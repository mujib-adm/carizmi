package io.carizmi.domain.platform.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.domain.platform.service.BaselineService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class YearEndScheduler {
    private static final Logger logger = LoggerFactory.getLogger(YearEndScheduler.class);

    private final BaselineService baselineService;

    /**
     * Runs on Jan 1st at 00:01 AM every year.
     * Captures the final revenue of the previous year and saves it as a baseline snapshot.
     */
    @Scheduled(cron = "0 1 0 1 1 ?")
    public void captureYearEndBaseline() {
        int previousYear = LocalDate.now().minusDays(1).getYear();
        logger.info("Executing Year-End Baseline Capture for year {}", previousYear);
        try {
            baselineService.closeYear(previousYear);
            logger.info("Successfully captured year-end baseline for {}", previousYear);
        } catch (Exception e) {
            logger.error("Failed to capture year-end baseline for {}: {}", previousYear, e.getMessage(), e);
        }
    }
}