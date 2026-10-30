package io.carizmi.domain.platform.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.domain.finance.data.dto.response.AnnualFinancialReportDto;
import io.carizmi.domain.finance.service.AnnualReportService;
import io.carizmi.domain.finance.service.AnnualReportSnapshot;
import io.carizmi.domain.membership.service.Member;
import io.carizmi.domain.platform.constants.BaselineConstants;
import io.carizmi.domain.platform.service.BaselineService;
import io.carizmi.domain.platform.service.DashboardProjector;
import io.carizmi.domain.platform.service.SystemSetting;
import io.carizmi.framework.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;

import static io.carizmi.shared.message.ValidationMessages.ERR_FISCAL_YEAR_NOT_ENDED;
import static io.carizmi.shared.message.ValidationMessages.ERR_FISCAL_YEAR_NO_BASELINE;

@Component
@RequiredArgsConstructor
@Setter
public class YearEndScheduler {
    private static final Logger logger = LoggerFactory.getLogger(YearEndScheduler.class);
    private static final String DEFAULT_TIMEZONE = "America/Chicago";
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final BaselineService baselineService;
    private final AnnualReportService annualReportService;
    private final Member member;
    private final SystemSetting systemSetting;
    private final AnnualReportSnapshot annualReportSnapshot;
    private final DashboardProjector dashboardProjector;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.timezone:America/Chicago}")
    private String timezone;

    @Value("${app.scheduler.year-end.max-attempts:3}")
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    /**
     * Runs on Jan 1st at 00:01 AM every year in the organization's business timezone.
     * Deactivates delinquent members, closes the fiscal year, freezes the annual report snapshot,
     * and refreshes the dashboard read model.
     */
    @Scheduled(cron = "${app.scheduler.year-end.cron:0 1 0 1 1 ?}", zone = "${app.timezone:America/Chicago}")
    public void executeYearEndClosingProcess() {
        ZoneId zoneId = resolveZoneId();
        int previousYear = LocalDate.now(zoneId).getYear() - 1;
        logger.info("Executing scheduled Year-End job for year {} in timezone {}", previousYear, zoneId);
        try {
            if (isYearFullyClosed(previousYear)) {
                logger.info("Fiscal year {} is already closed and frozen. Skipping scheduled year-end execution.", previousYear);
                return;
            }
            executeWithRetry(previousYear);
        } catch (Exception e) {
            logger.error("Scheduled year-end job failed for year {}: {}", previousYear, e.getMessage(), e);
        }
    }

    /**
     * One-time self-healing check triggered on application startup.
     * Ensures that if the container was down, scaling, or experienced a database outage
     * at 00:01 AM on Jan 1st, any unclosed prior fiscal year is automatically closed on boot.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileUnclosedYears() {
        ZoneId zoneId = resolveZoneId();
        int previousYear = LocalDate.now(zoneId).getYear() - 1;
        try {
            if (isYearEligibleForClosing(previousYear)) {
                logger.warn("Detected unclosed prior fiscal year {} during startup check. Triggering catch-up closing process.", previousYear);
                executeWithRetry(previousYear);
            } else {
                logger.debug("Startup year-end check complete: prior fiscal year {} does not require closing.", previousYear);
            }
        } catch (Exception e) {
            logger.error("Startup year-end catch-up failed for year {}: {}", previousYear, e.getMessage(), e);
        }
    }

    /**
     * Manually triggers or retries the year-end closing process for a completed past fiscal year.
     * Restricted to Admin users via the controller layer.
     *
     * @param targetYear the completed fiscal year to close and freeze
     * @return the official frozen AnnualFinancialReportDto
     */
    public AnnualFinancialReportDto closeFiscalYearManually(int targetYear) {
        ZoneId zoneId = resolveZoneId();
        int currentYear = LocalDate.now(zoneId).getYear();
        if (targetYear >= currentYear) {
            throw new ValidationException(
                    ERR_FISCAL_YEAR_NOT_ENDED.addMessageArgs(String.valueOf(targetYear)).getMessageString());
        }
        String openingBaselineKey = BaselineConstants.getYearlyBaselineKey(targetYear);
        if (systemSetting.findBySettingKey(openingBaselineKey).isEmpty()) {
            throw new ValidationException(
                    ERR_FISCAL_YEAR_NO_BASELINE.addMessageArgs(String.valueOf(targetYear)).getMessageString());
        }
        executeYearEndProcess(targetYear);
        return annualReportService.getAnnualReport(targetYear);
    }

    /**
     * Checks whether the specified fiscal year has been completely closed and frozen
     * (both next year's opening baseline and the target year's frozen report snapshot exist).
     */
    public boolean isYearFullyClosed(int year) {
        String closingBaselineKey = BaselineConstants.getYearlyBaselineKey(year + 1);
        return systemSetting.findBySettingKey(closingBaselineKey).isPresent()
                && annualReportSnapshot.existsByFiscalYear(year);
    }

    /**
     * Checks whether a completed past year has an active opening baseline (i.e., is on or after
     * the organization's inaugural year) and has not yet been fully closed.
     */
    public boolean isYearEligibleForClosing(int year) {
        String openingBaselineKey = BaselineConstants.getYearlyBaselineKey(year);
        return systemSetting.findBySettingKey(openingBaselineKey).isPresent()
                && !isYearFullyClosed(year);
    }

    /**
     * Executes the complete year-end rollover sequence for the specified fiscal year:
     * 1. Deactivates members who were active all 4 quarters and made zero membership payments.
     * 2. Calculates closing balance for the target year and creates/persists YEARLY_BASELINE for (targetYear + 1).
     * 3. Freezes and persists the official Annual Financial Report snapshot.
     * 4. Rebuilds the Dashboard CQRS projection after the rollover transaction commits.
     *
     * @param targetYear the fiscal year being closed (e.g., 2026)
     */
    public void executeYearEndProcess(int targetYear) {
        logger.info("Starting year-end closing process for fiscal year {}", targetYear);
        Runnable coreClosingSteps = () -> {
            int deactivated = member.deactivateDelinquentMembers(targetYear);
            logger.info("Deactivated {} delinquent members for year {}", deactivated, targetYear);
            baselineService.closeYear(targetYear);
            annualReportService.generateAndFreezeSnapshot(targetYear);
        };

        if (transactionTemplate != null) {
            transactionTemplate.executeWithoutResult(status -> coreClosingSteps.run());
        } else {
            coreClosingSteps.run();
        }

        try {
            dashboardProjector.rebuildProjection();
        } catch (Exception e) {
            logger.warn("Post-closing dashboard projection rebuild failed after closing year {}: {}", targetYear, e.getMessage(), e);
        }
        logger.info("Successfully completed year-end closing process and frozen snapshot for {}", targetYear);
    }

    private void executeWithRetry(int targetYear) {
        int attempts = Math.max(1, maxAttempts);
        Exception lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                executeYearEndProcess(targetYear);
                return;
            } catch (Exception e) {
                lastException = e;
                logger.warn("Year-end closing attempt {}/{} failed for fiscal year {}: {}",
                        attempt, attempts, targetYear, e.getMessage());
            }
        }
        throw new IllegalStateException("Year-end closing process failed after " + attempts + " attempts for year " + targetYear, lastException);
    }

    private ZoneId resolveZoneId() {
        return ZoneId.of(StringUtils.isNotBlank(timezone) ? timezone.trim() : DEFAULT_TIMEZONE);
    }
}