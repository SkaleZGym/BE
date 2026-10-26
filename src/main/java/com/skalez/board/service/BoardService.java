package com.skalez.board.service;

import com.skalez.board.api.BoardDtos.AdminFaqResponse;
import com.skalez.board.api.BoardDtos.AdminNoticeResponse;
import com.skalez.board.api.BoardDtos.BoardTextDto;
import com.skalez.board.api.BoardDtos.FaqResponse;
import com.skalez.board.api.BoardDtos.FaqSaveRequest;
import com.skalez.board.api.BoardDtos.NoticeResponse;
import com.skalez.board.api.BoardDtos.NoticeSaveRequest;
import com.skalez.board.api.BoardDtos.NoticeSummaryResponse;
import com.skalez.board.domain.BoardText;
import com.skalez.board.domain.Faq;
import com.skalez.board.domain.FaqCategory;
import com.skalez.board.domain.Notice;
import com.skalez.board.domain.NoticeCategory;
import com.skalez.board.repository.FaqRepository;
import com.skalez.board.repository.NoticeRepository;
import com.skalez.global.error.BusinessException;
import com.skalez.global.error.ErrorCode;
import com.skalez.global.i18n.SupportedLanguage;
import com.skalez.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 공지사항 · FAQ */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final NoticeRepository noticeRepository;
    private final FaqRepository faqRepository;

    // ---------- 고객 ----------

    public PageResponse<NoticeSummaryResponse> getNotices(NoticeCategory category, Pageable pageable, Locale locale) {
        SupportedLanguage language = SupportedLanguage.from(locale);
        Page<Notice> notices = category == null
                ? noticeRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc(pageable)
                : noticeRepository.findByPublishedTrueAndCategoryOrderByPinnedDescCreatedAtDesc(category, pageable);
        return PageResponse.from(notices.map(notice -> NoticeSummaryResponse.of(notice, language)));
    }

    /** 상세를 열 때마다 조회수 +1 */
    @Transactional
    public NoticeResponse getNotice(Long noticeId, Locale locale) {
        Notice notice = noticeRepository.findByIdAndPublishedTrue(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
        notice.increaseViewCount();
        return NoticeResponse.of(notice, SupportedLanguage.from(locale));
    }

    public List<FaqResponse> getFaqs(FaqCategory category, Locale locale) {
        SupportedLanguage language = SupportedLanguage.from(locale);
        List<Faq> faqs = category == null
                ? faqRepository.findByPublishedTrueOrderByCategoryAscSortOrderAsc()
                : faqRepository.findByPublishedTrueAndCategoryOrderBySortOrderAsc(category);
        return faqs.stream().map(faq -> FaqResponse.of(faq, language)).toList();
    }

    // ---------- 관리자: 공지 ----------

    public PageResponse<AdminNoticeResponse> getAllNotices(Pageable pageable) {
        return PageResponse.from(noticeRepository.findAll(pageable).map(AdminNoticeResponse::from));
    }

    public AdminNoticeResponse getNoticeForAdmin(Long noticeId) {
        return AdminNoticeResponse.from(findNotice(noticeId));
    }

    @Transactional
    public AdminNoticeResponse createNotice(NoticeSaveRequest request) {
        Notice notice = new Notice(request.category());
        notice.change(request.category(), request.pinned(), request.published(), toTexts(request.texts()));
        return AdminNoticeResponse.from(noticeRepository.save(notice));
    }

    @Transactional
    public AdminNoticeResponse updateNotice(Long noticeId, NoticeSaveRequest request) {
        Notice notice = findNotice(noticeId);
        notice.change(request.category(), request.pinned(), request.published(), toTexts(request.texts()));
        return AdminNoticeResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        noticeRepository.delete(findNotice(noticeId));
    }

    // ---------- 관리자: FAQ ----------

    public List<AdminFaqResponse> getAllFaqs() {
        return faqRepository.findAllByOrderByCategoryAscSortOrderAsc().stream().map(AdminFaqResponse::from).toList();
    }

    @Transactional
    public AdminFaqResponse createFaq(FaqSaveRequest request) {
        Faq faq = new Faq(request.category());
        faq.change(request.category(), request.sortOrder(), request.published(), toTexts(request.texts()));
        return AdminFaqResponse.from(faqRepository.save(faq));
    }

    @Transactional
    public AdminFaqResponse updateFaq(Long faqId, FaqSaveRequest request) {
        Faq faq = findFaq(faqId);
        faq.change(request.category(), request.sortOrder(), request.published(), toTexts(request.texts()));
        return AdminFaqResponse.from(faq);
    }

    @Transactional
    public void deleteFaq(Long faqId) {
        faqRepository.delete(findFaq(faqId));
    }

    /** 한국어 글은 필수. 언어 코드 확인 */
    private Map<SupportedLanguage, BoardText> toTexts(Map<String, BoardTextDto> dtos) {
        if (!dtos.containsKey(SupportedLanguage.DEFAULT.tag())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Map<SupportedLanguage, BoardText> texts = new EnumMap<>(SupportedLanguage.class);
        dtos.forEach((tag, dto) -> texts.put(SupportedLanguage.fromTag(tag), new BoardText(dto.title(), dto.content())));
        return texts;
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId).orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
    }

    private Faq findFaq(Long faqId) {
        return faqRepository.findById(faqId).orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
    }
}
