package org.sofumar.portal.service.businesslogic;

import org.sofumar.portal.data.dto.DashboardMetricsDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

public interface DashboardService {
    ResponseEntity<GlobalResponse<DashboardMetricsDto>> getMetrics();
}
