package com.skalez.order.service;

import com.skalez.claim.domain.ClaimItem;
import com.skalez.claim.domain.ClaimType;
import com.skalez.claim.domain.OrderClaim;
import com.skalez.global.mail.MailClient;
import com.skalez.global.mail.MailClient.MailMessage;
import com.skalez.global.mail.SiteProperties;
import com.skalez.member.repository.MemberRepository;
import com.skalez.order.domain.Order;
import com.skalez.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주문 알림 메일 (결제 완료 · 발송 · 결제 취소 · 교환/반품 승인 · 거절 · 완료).
 * - 메일 내용은 주문을 처리하는 중에 만들고, 보내기는 DB 저장(커밋)이 끝난 뒤에 한다 → 저장이 실패하면 메일도 안 간다
 * - 메일이 실패해도 주문 처리는 그대로 성공 (로그만 남김)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifier {

    /** 메일마다 다른 글 (제목의 %s 는 주문번호) */
    private record Copy(String subject, String title, String body, String button) {}

    /** 메일 표에 쓰는 항목 이름 */
    private record Labels(String order, String total, String tracking, String refund, String reason) {}

    private static final Map<String, Labels> LABELS = Map.of(
            "ko", new Labels("주문번호", "결제 금액", "송장번호", "환불 금액", "안내"),
            "en", new Labels("Order number", "Total paid", "Tracking number", "Refund amount", "Note"),
            "ja", new Labels("注文番号", "お支払い金額", "追跡番号", "返金額", "ご案内"),
            "zh-CN", new Labels("订单号", "支付金额", "运单号", "退款金额", "说明")
    );

    private static final Map<String, Copy> PAID = Map.of(
            "ko", new Copy("[SKALEZ] 주문이 완료됐어요 (%s)", "주문해 주셔서 감사해요", "결제가 확인됐어요. 상품을 준비해서 보내 드릴게요.", "주문 내역 보기"),
            "en", new Copy("[SKALEZ] Order confirmed (%s)", "Thank you for your order", "Your payment has been confirmed. We'll prepare and ship your items soon.", "View my order"),
            "ja", new Copy("[SKALEZ] ご注文ありがとうございます (%s)", "ご注文ありがとうございます", "お支払いを確認しました。商品を準備して発送いたします。", "注文履歴を見る"),
            "zh-CN", new Copy("[SKALEZ] 订单已完成 (%s)", "感谢您的订购", "已确认付款，我们会尽快为您备货发货。", "查看订单")
    );

    private static final Map<String, Copy> SHIPPED = Map.of(
            "ko", new Copy("[SKALEZ] 상품이 출발했어요 (%s)", "상품이 출발했어요", "주문하신 상품을 택배사에 넘겼어요. 아래 송장번호로 배송을 조회할 수 있어요.", "배송 조회하기"),
            "en", new Copy("[SKALEZ] Your order has shipped (%s)", "Your order is on its way", "We've handed your order to the courier. You can track it with the number below.", "Track my order"),
            "ja", new Copy("[SKALEZ] 商品を発送しました (%s)", "商品を発送しました", "ご注文の商品を配送業者に引き渡しました。下記の追跡番号で配送状況を確認できます。", "配送状況を確認"),
            "zh-CN", new Copy("[SKALEZ] 商品已发货 (%s)", "商品已发货", "您订购的商品已交给快递公司，可通过下方运单号查询物流。", "查询物流")
    );

    private static final Map<String, Copy> CANCELLED = Map.of(
            "ko", new Copy("[SKALEZ] 주문이 취소됐어요 (%s)", "주문이 취소됐어요", "결제하신 금액을 환불했어요. 카드사에 따라 영업일 기준 3~7일 뒤에 카드 내역에서 취소가 보여요.", "주문 내역 보기"),
            "en", new Copy("[SKALEZ] Order cancelled (%s)", "Your order has been cancelled", "We've refunded your payment. Depending on your card issuer, it may take 3-7 business days to appear.", "View my order"),
            "ja", new Copy("[SKALEZ] ご注文をキャンセルしました (%s)", "ご注文をキャンセルしました", "お支払い金額を返金しました。カード会社により、反映まで3〜7営業日かかる場合があります。", "注文履歴を見る"),
            "zh-CN", new Copy("[SKALEZ] 订单已取消 (%s)", "订单已取消", "已为您退款。根据发卡机构不同，退款到账可能需要 3-7 个工作日。", "查看订单")
    );

    private static final Map<String, Copy> RETURN_APPROVED = Map.of(
            "ko", new Copy("[SKALEZ] 반품 신청이 접수됐어요 (%s)", "반품 신청이 승인됐어요", "보내 주신 상품을 확인한 뒤 환불해 드릴게요. 상품 회수 방법은 고객센터에서 따로 안내해 드려요.", "신청 내역 보기"),
            "en", new Copy("[SKALEZ] Return approved (%s)", "Your return has been approved", "We'll refund you after we receive and check the items. Our customer service will tell you how to send them back.", "View my request"),
            "ja", new Copy("[SKALEZ] 返品申請を承認しました (%s)", "返品申請を承認しました", "商品の到着・確認後に返金いたします。返送方法はカスタマーセンターから別途ご案内します。", "申請内容を見る"),
            "zh-CN", new Copy("[SKALEZ] 退货申请已通过 (%s)", "退货申请已通过", "收到并确认商品后为您退款。寄回方式将由客服另行通知。", "查看申请")
    );

    private static final Map<String, Copy> EXCHANGE_APPROVED = Map.of(
            "ko", new Copy("[SKALEZ] 교환 신청이 접수됐어요 (%s)", "교환 신청이 승인됐어요", "보내 주신 상품을 확인한 뒤 새 상품을 보내 드릴게요. 상품 회수 방법은 고객센터에서 따로 안내해 드려요.", "신청 내역 보기"),
            "en", new Copy("[SKALEZ] Exchange approved (%s)", "Your exchange has been approved", "We'll send the new items after we receive and check the originals. Our customer service will tell you how to send them back.", "View my request"),
            "ja", new Copy("[SKALEZ] 交換申請を承認しました (%s)", "交換申請を承認しました", "商品の到着・確認後に新しい商品をお送りします。返送方法はカスタマーセンターから別途ご案内します。", "申請内容を見る"),
            "zh-CN", new Copy("[SKALEZ] 换货申请已通过 (%s)", "换货申请已通过", "收到并确认商品后为您寄出新商品。寄回方式将由客服另行通知。", "查看申请")
    );

    private static final Map<String, Copy> CLAIM_REJECTED = Map.of(
            "ko", new Copy("[SKALEZ] 교환·반품 신청 결과 안내 (%s)", "교환·반품 신청을 진행하지 못했어요", "신청하신 교환·반품을 처리할 수 없어요. 자세한 내용은 아래 안내를 확인하시거나 1:1 문의로 연락해 주세요.", "신청 내역 보기"),
            "en", new Copy("[SKALEZ] Update on your request (%s)", "We couldn't process your request", "We're unable to process your exchange or return. Please see the note below or contact us via inquiry.", "View my request"),
            "ja", new Copy("[SKALEZ] 交換・返品申請の結果 (%s)", "交換・返品申請をお受けできませんでした", "お申し込みの交換・返品は承れません。詳しくは下記のご案内をご確認いただくか、お問い合わせください。", "申請内容を見る"),
            "zh-CN", new Copy("[SKALEZ] 换货/退货申请结果 (%s)", "无法处理您的换货/退货申请", "很抱歉，无法处理您的换货/退货申请。详情请查看下方说明或通过在线咨询联系我们。", "查看申请")
    );

    private static final Map<String, Copy> RETURN_COMPLETED = Map.of(
            "ko", new Copy("[SKALEZ] 반품 환불이 완료됐어요 (%s)", "반품이 완료됐어요", "상품을 확인하고 아래 금액을 환불했어요. 카드사에 따라 영업일 기준 3~7일 뒤에 카드 내역에서 보여요.", "주문 내역 보기"),
            "en", new Copy("[SKALEZ] Return refunded (%s)", "Your return is complete", "We've checked the items and refunded the amount below. It may take 3-7 business days to appear, depending on your card issuer.", "View my order"),
            "ja", new Copy("[SKALEZ] 返品の返金が完了しました (%s)", "返品が完了しました", "商品を確認し、下記の金額を返金しました。カード会社により、反映まで3〜7営業日かかる場合があります。", "注文履歴を見る"),
            "zh-CN", new Copy("[SKALEZ] 退货退款已完成 (%s)", "退货已完成", "已确认商品并退还以下金额。根据发卡机构不同，可能需要 3-7 个工作日到账。", "查看订单")
    );

    private static final Map<String, Copy> EXCHANGE_COMPLETED = Map.of(
            "ko", new Copy("[SKALEZ] 교환 상품이 출발했어요 (%s)", "교환 상품이 출발했어요", "교환해 드릴 상품을 보냈어요. 아래 송장번호로 배송을 조회할 수 있어요.", "배송 조회하기"),
            "en", new Copy("[SKALEZ] Your exchange has shipped (%s)", "Your exchange is on its way", "We've sent your replacement items. You can track them with the number below.", "Track my order"),
            "ja", new Copy("[SKALEZ] 交換商品を発送しました (%s)", "交換商品を発送しました", "交換商品を発送しました。下記の追跡番号で配送状況を確認できます。", "配送状況を確認"),
            "zh-CN", new Copy("[SKALEZ] 换货商品已发出 (%s)", "换货商品已发出", "已为您寄出换货商品，可通过下方运单号查询物流。", "查询物流")
    );

    private final MailClient mailClient;
    private final MemberRepository memberRepository;
    private final SiteProperties site;

    /** 메일 표의 한 줄 */
    private record Row(String label, String value) {}

    /** 주문 상품 한 줄 (상품명 · 사이즈 · 수량) */
    private record Line(String name, String size, int quantity) {}

    public void paid(Order order) {
        send(order, order.getMemberId(), PAID, (labels) -> List.of(
                new Row(labels.order(), order.getOrderNumber()),
                new Row(labels.total(), money(order.getTotalAmount(), order.getCurrency()))), lines(order), null);
    }

    public void shipped(Order order) {
        String tracking = order.getTrackingNumber() == null ? null : order.getCourier().displayName() + " " + order.getTrackingNumber();
        send(order, order.getMemberId(), SHIPPED, (labels) -> withOptional(List.of(
                new Row(labels.order(), order.getOrderNumber()),
                new Row(labels.total(), money(order.getTotalAmount(), order.getCurrency()))), labels.tracking(), tracking),
                lines(order), order.trackingUrl());
    }

    /** 결제됐던 주문을 취소(전액 환불)했을 때. 결제 전 주문 취소는 메일을 보내지 않는다 */
    public void cancelled(Order order) {
        send(order, order.getMemberId(), CANCELLED, (labels) -> List.of(
                new Row(labels.order(), order.getOrderNumber()),
                new Row(labels.refund(), money(order.getTotalAmount(), order.getCurrency()))), lines(order), null);
    }

    public void claimApproved(OrderClaim claim) {
        Map<String, Copy> copies = claim.getType() == ClaimType.RETURN ? RETURN_APPROVED : EXCHANGE_APPROVED;
        send(claim.getOrder(), claim.getMemberId(), copies, (labels) -> withOptional(List.of(
                new Row(labels.order(), claim.getOrder().getOrderNumber())), labels.reason(), claim.getAdminMemo()),
                lines(claim), null);
    }

    public void claimRejected(OrderClaim claim) {
        send(claim.getOrder(), claim.getMemberId(), CLAIM_REJECTED, (labels) -> withOptional(List.of(
                new Row(labels.order(), claim.getOrder().getOrderNumber())), labels.reason(), claim.getAdminMemo()),
                lines(claim), null);
    }

    public void claimCompleted(OrderClaim claim) {
        Order order = claim.getOrder();
        if (claim.getType() == ClaimType.RETURN) {
            long refund = claim.getRefundAmount() == null ? 0 : claim.getRefundAmount();
            send(order, claim.getMemberId(), RETURN_COMPLETED, (labels) -> List.of(
                    new Row(labels.order(), order.getOrderNumber()),
                    new Row(labels.refund(), money(refund, order.getCurrency()))), lines(claim), null);
            return;
        }
        String tracking = claim.getTrackingNumber() == null || claim.getCourier() == null
                ? null : claim.getCourier().displayName() + " " + claim.getTrackingNumber();
        send(order, claim.getMemberId(), EXCHANGE_COMPLETED, (labels) -> withOptional(List.of(
                new Row(labels.order(), order.getOrderNumber())), labels.tracking(), tracking), lines(claim), claim.trackingUrl());
    }

    private interface RowsFor {
        List<Row> rows(Labels labels);
    }

    private static List<Row> withOptional(List<Row> rows, String label, String value) {
        if (value == null || value.isBlank()) return rows;
        List<Row> all = new ArrayList<>(rows);
        all.add(new Row(label, value));
        return all;
    }

    private static List<Line> lines(Order order) {
        return order.getItems().stream().map(item -> new Line(item.getProductName(), item.getSize(), item.getQuantity())).toList();
    }

    private static List<Line> lines(OrderClaim claim) {
        Map<Long, OrderItem> byId = claim.getOrder().getItems().stream().collect(Collectors.toMap(OrderItem::getId, item -> item));
        List<Line> lines = new ArrayList<>();
        for (ClaimItem item : claim.getItems()) {
            OrderItem orderItem = byId.get(item.getOrderItemId());
            if (orderItem == null) continue;
            String size = item.getExchangeSize() == null ? orderItem.getSize() : orderItem.getSize() + " → " + item.getExchangeSize();
            lines.add(new Line(orderItem.getProductName(), size, item.getQuantity()));
        }
        return lines;
    }

    /** link 가 없으면 마이페이지로 */
    private void send(Order order, Long memberId, Map<String, Copy> copies, RowsFor rows, List<Line> lines, String link) {
        try {
            memberRepository.findById(memberId).ifPresent(member -> {
                MailMessage message = build(member.getEmail(), member.getLanguage(), copies, order, rows, lines, link);
                afterCommit(() -> deliver(message, order.getOrderNumber()));
            });
        } catch (RuntimeException e) {
            log.warn("주문 알림 메일을 만들지 못했어요: {}", order.getOrderNumber(), e);
        }
    }

    private void deliver(MailMessage message, String orderNumber) {
        try {
            mailClient.send(message);
        } catch (RuntimeException e) {
            log.warn("주문 알림 메일을 보내지 못했어요: {}", orderNumber, e);
        }
    }

    private static void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private MailMessage build(String to, String language, Map<String, Copy> copies, Order order, RowsFor rowsFor, List<Line> lines, String trackingLink) {
        String lang = copies.containsKey(language) ? language : "ko";
        Copy copy = copies.get(lang);
        List<Row> rows = rowsFor.rows(LABELS.get(lang));
        String link = trackingLink != null ? trackingLink : site.url() + "/" + lang + "/mypage";

        String text = copy.title() + "\n\n" + copy.body() + "\n\n"
                + rows.stream().map(row -> row.label() + ": " + row.value()).collect(Collectors.joining("\n")) + "\n\n"
                + lines.stream().map(line -> "- " + line.name() + " (" + line.size() + ") x " + line.quantity()).collect(Collectors.joining("\n"))
                + "\n\n" + link + "\n\nSKALEZ";

        String rowsHtml = rows.stream().map(row -> row(row.label(), row.value())).collect(Collectors.joining());
        String linesHtml = lines.stream().map(OrderNotifier::lineRow).collect(Collectors.joining());
        String html = """
                <div style="max-width:480px;margin:0 auto;padding:32px 24px;font-family:-apple-system,'Noto Sans KR',sans-serif;color:#1c1e21">
                  <p style="margin:0 0 24px;font-weight:700;letter-spacing:.2em">SKALEZ</p>
                  <h1 style="margin:0 0 12px;font-size:20px">%s</h1>
                  <p style="margin:0 0 20px;line-height:1.7">%s</p>
                  <table style="width:100%%;border-collapse:collapse;font-size:14px;margin:0 0 16px">%s</table>
                  <table style="width:100%%;border-collapse:collapse;font-size:13px;margin:0 0 24px;border-top:1px solid #e5e5e5">%s</table>
                  <p style="margin:0"><a href="%s" style="display:inline-block;padding:14px 22px;color:#fff;background:#1c1e21;border-radius:6px;text-decoration:none">%s</a></p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(copy.title()), HtmlUtils.htmlEscape(copy.body()),
                rowsHtml, linesHtml, HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(copy.button()));
        return new MailMessage(to, copy.subject().formatted(order.getOrderNumber()), text, html);
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"padding:4px 0;color:#75787d\">" + HtmlUtils.htmlEscape(label)
                + "</td><td style=\"padding:4px 0;text-align:right;font-weight:600\">" + HtmlUtils.htmlEscape(value) + "</td></tr>";
    }

    private static String lineRow(Line line) {
        return "<tr><td style=\"padding:8px 0;border-bottom:1px solid #f0f0f0\">" + HtmlUtils.htmlEscape(line.name())
                + " <span style=\"color:#75787d\">(" + HtmlUtils.htmlEscape(line.size()) + ")</span></td>"
                + "<td style=\"padding:8px 0;border-bottom:1px solid #f0f0f0;text-align:right\">x " + line.quantity() + "</td></tr>";
    }

    /** KRW는 원 단위, 그 외(USD 등)는 센트 단위로 저장돼 있다 */
    static String money(long amount, String currency) {
        if ("KRW".equals(currency)) {
            return "₩" + String.format("%,d", amount);
        }
        String value = String.format("%,d.%02d", amount / 100, amount % 100);
        return "USD".equals(currency) ? "$" + value : value + " " + currency;
    }
}
