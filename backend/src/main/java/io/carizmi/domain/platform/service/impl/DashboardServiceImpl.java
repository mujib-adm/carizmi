package io.carizmi.domain.platform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.carizmi.domain.platform.data.dto.response.DashboardMetricsDto;
import io.carizmi.domain.platform.data.dto.response.QuarterlyCollectionDto;
import io.carizmi.domain.platform.model.DashboardSnapshotVO;
import io.carizmi.domain.platform.service.DashboardProjector;
import io.carizmi.domain.platform.service.DashboardService;
import io.carizmi.domain.platform.service.DashboardSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * CQRS Read Path for dashboard metrics.
 *
 * <p>Reads pre-computed metrics from the {@code dashboard_metrics_snapshot} table.
 * This helps to avoid performing a cross-domain queries synchronously on each request.</p>
 *
 * <p>The write path (metric computation) is handled by
 * {@link DashboardProjector}, which listens to in-process Spring events and
 * rebuilds the snapshot whenever a Payment, Expense, or Member change occurs.</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final DashboardSnapshot dashboardSnapshot;
    private final DashboardProjector dashboardProjector;
    private final ObjectMapper objectMapper;

    @Override
    public DashboardMetricsDto getMetrics() {
        DashboardSnapshotVO snapshotVO = dashboardSnapshot.getSnapshot().orElse(null);

        if (snapshotVO == null) {
            logger.info("Dashboard snapshot not found — triggering initial projection");
            dashboardProjector.rebuildProjection();
            snapshotVO = dashboardSnapshot.getSnapshot().orElse(null);
        }

        if (snapshotVO == null) {
            logger.warn("Dashboard snapshot could not be built — returning empty metrics");
            return DashboardMetricsDto.builder()
                    .quarterlyCollections(Collections.emptyList())
                    .build();
        }

        return DashboardMetricsDto.builder()
                .totalMembers(snapshotVO.getTotalActiveMembers())
                .totalRevenue(snapshotVO.getTotalRevenue())
                .duesThisQuarter(snapshotVO.getDuesThisQuarter())
                .overdueTotal(snapshotVO.getOverdueTotal())
                .quarterlyFeeAmt(snapshotVO.getQuarterlyFeeAmt())
                .quarterlyCollections(deserializeCollections(snapshotVO.getQuarterlyCollections()))
                .build();
    }

    private List<QuarterlyCollectionDto> deserializeCollections(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize quarterly collections from dashboard snapshot", e);
            return Collections.emptyList();
        }
    }
}