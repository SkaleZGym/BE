package com.skalez.order.service;

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

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주문 알림 메일 (결제 완료 · 발송).
 * - 메일 내용은 주문을 처리하는 중에 만들고, 보내기는 DB 저장(커밋)이 끝난 뒤에 한다 → 저장이 실패하면 메일도 안 간다
 * - 메일이 실패해도 주문 처리는 그대로 성공 (로그만 남김)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifier {

    private record Copy(String subject, String title, String body, String orderLabel, String totalLabel, String trackingLabel, String button) {}

    private static final Map<String, Copy> PAID = Map.of(
            "ko", new Copy("[SKALEZ] 주문이 완료됐어요 (%s)", "주문해 주셔서 감사해요", "결제가 확인됐어요. 상품을 준비해서 보내 드릴게요.", "주문번호", "결제 금액", "송장번호", "주문 내역 보기"),
            "en", new Copy("[SKALEZ] Order confirmed (%s)", "Thank you for your order", "Your payment has been confirmed. We'll prepare and ship your items soon.", "Order number", "Total paid", "Tracking number", "View my order"),
            "ja", new Copy("[SKALEZ] ご注文ありがとうございます (%s)", "ご注文ありがとうございます", "お支払いを確認しました。商品を準備して発送いたします。", "注文番号", "お支払い金額", "追跡番号", "注文履歴を見る"),
            "zh-CN", new Copy("[SKALEZ] 订单已完成 (%s)", "感谢您的订购", "已确认付款，我们会尽快为您备货发货。", "订单号", "支付金额", "运单号", "查看订单")
    );

    private static final Map<String, Copy> SHIPPED = Map.of(
            "ko", new Copy("[SKALEZ] 상품이 출발했어요 (%s)", "상품이 출발했어요", "주문하신 상품을 택배사에 넘겼어요. 아래 송장번호로 배송을 조회할 수 있어요.", "주문번호", "결제 금액", "송장번호", "배송 조회하기"),
            "en", new Copy("[SKALEZ] Your order has shipped (%s)", "Your order is on its way", "We've handed your order to the courier. You can track it with the number below.", "Order number", "Total paid", "Tracking number", "Track my order"),
            "ja", new Copy("[SKALEZ] 商品を発送しました (%s)", "商品を発送しました", "ご注文の商品を配送業者に引き渡しました。下記の追跡番号で配送状況を確認できます。", "注文番号", "お支払い金額", "追跡番号", "配送状況を確認"),
            "zh-CN", new Copy("[SKALEZ] 商品已发货 (%s)", "商品已发货", "您订购的商品已交给快递公司，可通过下方运单号查询物流。", "订单号", "支付金额", "运单号", "查询物流")
    );

    private final MailClient mailClient;
    private final MemberRepository memberRepository;
    private final SiteProperties site;

    public void paid(Order order) {
        notify(order, PAID, false);
    }

    public void shipped(Order order) {
        notify(order, SHIPPED, true);
    }

    private void notify(Order order, Map<String, Copy> copies, boolean shipped) {
        try {
            memberRepository.findById(order.getMemberId()).ifPresent(member -> {
                MailMessage message = build(member.getEmail(), member.getLanguage(), copies, order, shipped);
                afterCommit(() -> send(message, order.getOrderNumber()));
            });
        } catch (RuntimeException e) {
            log.warn("주문 알림 메일을 만들지 못했어요: {}", order.getOrderNumber(), e);
        }
    }

    private void send(MailMessage message, String orderNumber) {
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

    private MailMessage build(String to, String language, Map<String, Copy> copies, Order order, boolean shipped) {
        String lang = copies.containsKey(language) ? language : "ko";
        Copy copy = copies.get(lang);
        String total = money(order.getTotalAmount(), order.getCurrency());
        String link = shipped && order.trackingUrl() != null ? order.trackingUrl() : site.url() + "/" + lang + "/mypage";
        String tracking = shipped && order.getTrackingNumber() != null
                ? order.getCourier().displayName() + " " + order.getTrackingNumber()
                : null;

        String itemsText = order.getItems().stream()
                .map(item -> "- " + item.getProductName() + " (" + item.getSize() + ") x " + item.getQuantity())
                .collect(Collectors.joining("\n"));
        String text = copy.title() + "\n\n" + copy.body() + "\n\n"
                + copy.orderLabel() + ": " + order.getOrderNumber() + "\n"
                + copy.totalLabel() + ": " + total + "\n"
                + (tracking == null ? "" : copy.trackingLabel() + ": " + tracking + "\n")
                + "\n" + itemsText + "\n\n" + link + "\n\n— SKALEZ";

        String itemsHtml = order.getItems().stream()
                .map(OrderNotifier::itemRow)
                .collect(Collectors.joining());
        String trackingHtml = tracking == null ? "" : row(copy.trackingLabel(), tracking);
        String html = """
                <div style="max-width:480px;margin:0 auto;padding:32px 24px;font-family:-apple-system,'Noto Sans KR',sans-serif;color:#1c1e21">
                  <p style="margin:0 0 24px;font-weight:700;letter-spacing:.2em">SKALEZ</p>
                  <h1 style="margin:0 0 12px;font-size:20px">%s</h1>
                  <p style="margin:0 0 20px;line-height:1.7">%s</p>
                  <table style="width:100%%;border-collapse:collapse;font-size:14px;margin:0 0 16px">%s%s%s</table>
                  <table style="width:100%%;border-collapse:collapse;font-size:13px;margin:0 0 24px;border-top:1px solid #e5e5e5">%s</table>
                  <p style="margin:0"><a href="%s" style="display:inline-block;padding:14px 22px;color:#fff;background:#1c1e21;border-radius:6px;text-decoration:none">%s</a></p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(copy.title()), HtmlUtils.htmlEscape(copy.body()),
                row(copy.orderLabel(), order.getOrderNumber()), row(copy.totalLabel(), total), trackingHtml,
                itemsHtml, HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(copy.button()));
        return new MailMessage(to, copy.subject().formatted(order.getOrderNumber()), text, html);
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"padding:4px 0;color:#75787d\">" + HtmlUtils.htmlEscape(label)
                + "</td><td style=\"padding:4px 0;text-align:right;font-weight:600\">" + HtmlUtils.htmlEscape(value) + "</td></tr>";
    }

    private static String itemRow(OrderItem item) {
        return "<tr><td style=\"padding:8px 0;border-bottom:1px solid #f0f0f0\">" + HtmlUtils.htmlEscape(item.getProductName())
                + " <span style=\"color:#75787d\">(" + HtmlUtils.htmlEscape(item.getSize()) + ")</span></td>"
                + "<td style=\"padding:8px 0;border-bottom:1px solid #f0f0f0;text-align:right\">x " + item.getQuantity() + "</td></tr>";
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
