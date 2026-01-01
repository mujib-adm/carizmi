package org.sofumar.portal.controller;

import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.DashboardMetricsDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.service.businesslogic.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    public ResponseEntity<GlobalResponse<DashboardMetricsDto>> getMetrics() {
        return dashboardService.getMetrics();
    }
}