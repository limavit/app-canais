package com.example.iptvmanager.dashboard.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.iptvmanager.auth.security.UserPrincipal;
import com.example.iptvmanager.dashboard.dto.DashboardDTO;
import com.example.iptvmanager.dashboard.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardDTO dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return dashboardService.dashboard(principal.getUser());
    }
}
