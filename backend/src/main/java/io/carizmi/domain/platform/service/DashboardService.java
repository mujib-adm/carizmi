package io.carizmi.domain.platform.service;

import io.carizmi.domain.platform.data.dto.response.DashboardMetricsDto;

public interface DashboardService {
    DashboardMetricsDto getMetrics();
}