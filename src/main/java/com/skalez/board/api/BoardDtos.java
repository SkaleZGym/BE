package com.skalez.board.api;

import com.skalez.board.domain.BoardText;
import com.skalez.board.domain.Faq;
import com.skalez.board.domain.FaqCategory;
import com.skalez.board.domain.Notice;
import com.skalez.board.domain.NoticeCategory;
import com.skalez.global.i18n.SupportedLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/** 공지·FAQ 요청·응답. 언어 키는 ko, en, ja, zh-CN */
public final class BoardDtos {

    private BoardDtos() {
    }

    public record BoardTextDto(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 10000) String content
    ) {
    }

    // ---------- 공지 ----------

    public record NoticeSaveRequest(
            @NotNull NoticeCategory category,
            boolean pinned,
            boolean published,
            @NotEmpty Map<String, @Valid BoardTextDto> texts
    ) {
    }

    /** 고객 목록용 (내용 제외) */
    public record NoticeSummaryResponse(Long id, String category, boolean pinned, String title, long viewCount, Instant createdAt) {
        public static NoticeSummaryResponse of(Notice notice, SupportedLanguage language) {
            BoardText text = notice.textFor(language);
            return new NoticeSummaryResponse(notice.getId(), notice.getCategory().name(), notice.isPinned(),
                    text == null ? null : text.getTitle(), notice.getViewCount(), notice.getCreatedAt());
        }
    }

    public record NoticeResponse(Long id, String category, boolean pinned, String title, String content, long viewCount, Instant createdAt) {
        public static NoticeResponse of(Notice notice, SupportedLanguage language) {
            BoardText text = notice.textFor(language);
            return new NoticeResponse(notice.getId(), notice.getCategory().name(), notice.isPinned(),
                    text == null ? null : text.getTitle(), text == null ? null : text.getContent(),
                    notice.getViewCount(), notice.getCreatedAt());
        }
    }

    public record AdminNoticeResponse(Long id, String category, boolean pinned, boolean published, long viewCount,
                                      Map<String, BoardTextDto> texts, Instant createdAt, Instant updatedAt) {
        public static AdminNoticeResponse from(Notice notice) {
            return new AdminNoticeResponse(notice.getId(), notice.getCategory().name(), notice.isPinned(), notice.isPublished(),
                    notice.getViewCount(), toDtos(notice.getTexts()), notice.getCreatedAt(), notice.getUpdatedAt());
        }
    }

    // ---------- FAQ ----------

    public record FaqSaveRequest(
            @NotNull FaqCategory category,
            int sortOrder,
            boolean published,
            @NotEmpty Map<String, @Valid BoardTextDto> texts
    ) {
    }

    public record FaqResponse(Long id, String category, String question, String answer) {
        public static FaqResponse of(Faq faq, SupportedLanguage language) {
            BoardText text = faq.textFor(language);
            return new FaqResponse(faq.getId(), faq.getCategory().name(),
                    text == null ? null : text.getTitle(), text == null ? null : text.getContent());
        }
    }

    public record AdminFaqResponse(Long id, String category, int sortOrder, boolean published,
                                   Map<String, BoardTextDto> texts, Instant updatedAt) {
        public static AdminFaqResponse from(Faq faq) {
            return new AdminFaqResponse(faq.getId(), faq.getCategory().name(), faq.getSortOrder(), faq.isPublished(),
                    toDtos(faq.getTexts()), faq.getUpdatedAt());
        }
    }

    private static Map<String, BoardTextDto> toDtos(Map<String, BoardText> texts) {
        Map<String, BoardTextDto> result = new TreeMap<>();
        texts.forEach((tag, text) -> result.put(tag, new BoardTextDto(text.getTitle(), text.getContent())));
        return result;
    }
}
