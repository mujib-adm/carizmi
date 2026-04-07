package org.sofumar.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.response.DashboardMetricsDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.service.helper.DashboardService;
import org.sofumar.portal.security.annotation.IsAuthenticated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Dashboard analytics APIs")
@RequiredArgsConstructor
@IsAuthenticated
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/metrics")
    @Operation(summary = "Get dashboard metrics")
    public ResponseEntity<GlobalResponse<DashboardMetricsDto>> getMetrics() {
        return ResponseUtils.okWithData(dashboardService.getMetrics());
    }
}