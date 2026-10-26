package com.skalez.board.api;

import com.skalez.board.api.BoardDtos.AdminFaqResponse;
import com.skalez.board.api.BoardDtos.AdminNoticeResponse;
import com.skalez.board.api.BoardDtos.FaqSaveRequest;
import com.skalez.board.api.BoardDtos.NoticeSaveRequest;
import com.skalez.board.service.BoardService;
import com.skalez.global.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 관리자 공지사항·FAQ 관리 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminBoardController {

    private final BoardService boardService;

    // ---------- 공지 ----------

    @GetMapping("/notices")
    public PageResponse<AdminNoticeResponse> getNotices(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return boardService.getAllNotices(pageable);
    }

    @GetMapping("/notices/{noticeId}")
    public AdminNoticeResponse getNotice(@PathVariable Long noticeId) {
        return boardService.getNoticeForAdmin(noticeId);
    }

    @PostMapping("/notices")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminNoticeResponse createNotice(@Valid @RequestBody NoticeSaveRequest request) {
        return boardService.createNotice(request);
    }

    @PutMapping("/notices/{noticeId}")
    public AdminNoticeResponse updateNotice(@PathVariable Long noticeId, @Valid @RequestBody NoticeSaveRequest request) {
        return boardService.updateNotice(noticeId, request);
    }

    @DeleteMapping("/notices/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotice(@PathVariable Long noticeId) {
        boardService.deleteNotice(noticeId);
    }

    // ---------- FAQ ----------

    @GetMapping("/faqs")
    public List<AdminFaqResponse> getFaqs() {
        return boardService.getAllFaqs();
    }

    @PostMapping("/faqs")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminFaqResponse createFaq(@Valid @RequestBody FaqSaveRequest request) {
        return boardService.createFaq(request);
    }

    @PutMapping("/faqs/{faqId}")
    public AdminFaqResponse updateFaq(@PathVariable Long faqId, @Valid @RequestBody FaqSaveRequest request) {
        return boardService.updateFaq(faqId, request);
    }

    @DeleteMapping("/faqs/{faqId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFaq(@PathVariable Long faqId) {
        boardService.deleteFaq(faqId);
    }
}
