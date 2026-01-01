package org.sofumar.portal.service.businesslogic.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.data.dto.DashboardMetricsDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.repo.MemberRepository;
import org.sofumar.portal.service.businesslogic.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final MemberRepository memberRepo;

    @Override
    public ResponseEntity<GlobalResponse<DashboardMetricsDto>> getMetrics() {
        logger.info("Fetching dashboard metrics");

        long total = memberRepo.count();

        DashboardMetricsDto metrics = DashboardMetricsDto.builder()
                .totalMembers(total)
//                .totalRevenue(0L) // Placeholder for now
//                .duesThisQuarter(0L) // Placeholder for now
//                .overdueTotal(0L) // Placeholder for now
                .build();

        return ResponseUtils.okWithData(metrics);
    }
}
