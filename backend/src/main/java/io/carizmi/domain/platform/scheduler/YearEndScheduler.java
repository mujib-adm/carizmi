package io.carizmi.domain.platform.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.domain.platform.service.BaselineService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Setter
public class YearEndScheduler {
    private static final Logger logger = LoggerFactory.getLogger(YearEndScheduler.class);

    private final BaselineService baselineService;

    @Value("${app.timezone:America/Chicago}")
    private String timezone;

    /**
     * Runs on Jan 1st at 00:01 AM every year in the organization's business timezone.
     * Captures the final revenue of the previous year and saves it as a baseline snapshot.
     */
    @Scheduled(cron = "0 1 0 1 1 ?", zone = "${app.timezone:America/Chicago}")
    public void captureYearEndBaseline() {
        ZoneId zoneId = ZoneId.of(timezone != null ? timezone : "America/Chicago");
        int previousYear = LocalDate.now(zoneId).minusDays(1).getYear();
        logger.info("Executing Year-End Baseline Capture for year {} in timezone {}", previousYear, zoneId);
        try {
            baselineService.closeYear(previousYear);
            logger.info("Successfully captured year-end baseline for {}", previousYear);
        } catch (Exception e) {
            logger.error("Failed to capture year-end baseline for {}: {}", previousYear, e.getMessage(), e);
        }
    }
}