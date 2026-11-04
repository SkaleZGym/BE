package com.skalez.audit.api;

import com.skalez.audit.service.AdminAccessLogService;
import com.skalez.audit.service.AdminAccessLogService.Row;
import com.skalez.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/access-logs")
@RequiredArgsConstructor
public class AdminAccessLogController {
    private final AdminAccessLogService accessLogService;
    @GetMapping
    public PageResponse<Row> list(@PageableDefault(size = 50) Pageable pageable){
        return accessLogService.list(PageResponse.capped(pageable));
    }
}
