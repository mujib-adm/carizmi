package org.sofumar.portal.service.helper;

import org.sofumar.portal.data.dto.response.DashboardMetricsDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

public interface DashboardService {
    ResponseEntity<GlobalResponse<DashboardMetricsDto>> getMetrics();
}