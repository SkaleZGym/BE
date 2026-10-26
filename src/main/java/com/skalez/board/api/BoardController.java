package com.skalez.board.api;

import com.skalez.board.api.BoardDtos.FaqResponse;
import com.skalez.board.api.BoardDtos.NoticeResponse;
import com.skalez.board.api.BoardDtos.NoticeSummaryResponse;
import com.skalez.board.domain.FaqCategory;
import com.skalez.board.domain.NoticeCategory;
import com.skalez.board.service.BoardService;
import com.skalez.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/** 고객 화면용 공지·FAQ (로그인 불필요) */
@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    /** 예: GET /api/notices?category=EVENT&page=0 */
    @GetMapping("/api/notices")
    public PageResponse<NoticeSummaryResponse> getNotices(@RequestParam(required = false) NoticeCategory category,
                                                          @PageableDefault(size = 10) Pageable pageable, Locale locale) {
        return boardService.getNotices(category, pageable, locale);
    }

    @GetMapping("/api/notices/{noticeId}")
    public NoticeResponse getNotice(@PathVariable Long noticeId, Locale locale) {
        return boardService.getNotice(noticeId, locale);
    }

    /** 예: GET /api/faqs?category=SHIPPING */
    @GetMapping("/api/faqs")
    public List<FaqResponse> getFaqs(@RequestParam(required = false) FaqCategory category, Locale locale) {
        return boardService.getFaqs(category, locale);
    }
}
