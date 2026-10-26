package com.skalez.dashboard.api;

import com.skalez.dashboard.api.DashboardDtos.DashboardResponse;
import com.skalez.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminDashboardController {
    private final DashboardService dashboardService;
    @GetMapping("/api/admin/dashboard")
    public DashboardResponse get() {return dashboardService.get();}
}
