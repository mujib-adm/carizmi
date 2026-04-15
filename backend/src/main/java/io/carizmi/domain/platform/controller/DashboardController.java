package io.carizmi.domain.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.carizmi.domain.platform.data.dto.response.DashboardMetricsDto;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.util.ResponseUtils;
import io.carizmi.domain.platform.service.DashboardService;
import io.carizmi.infrastructure.security.annotation.IsAuthenticated;
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