package io.carizmi.domain.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.carizmi.domain.finance.service.Expense;
import io.carizmi.domain.finance.service.Payment;
import io.carizmi.domain.membership.service.Member;
import io.carizmi.domain.platform.data.dto.response.QuarterlyCollectionDto;
import io.carizmi.domain.platform.model.DashboardSnapshotVO;
import io.carizmi.framework.projection.AbstractProjector;
import io.carizmi.shared.constants.QuarterStatus;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.shared.util.QuarterUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CQRS Projector for the Dashboard Read Model.
 *
 * <p>Triggered by {@link AbstractProjector} when an in-process Spring event
 * matches one of the supported aggregate types (Payment, Expense, or Member).
 * Rebuilds the {@code dashboard_metrics_snapshot} table on each invocation.</p>
 */
@Component
@RequiredArgsConstructor
public class DashboardProjector extends AbstractProjector {

    private static final Logger logger = LoggerFactory.getLogger(DashboardProjector.class);
    private static final Set<String> SUPPORTED_TYPES = Set.of("PaymentVO", "ExpenseVO", "MemberVO");

    private final DashboardSnapshot dashboardSnapshot;
    private final Member member;
    private final Payment payment;
    private final Expense expense;
    private final BaselineService baselineService;
    private final SystemSetting systemSetting;
    private final ObjectMapper objectMapper;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected Set<String> supportedAggregateTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rebuildProjection() {
        try {
            BigDecimal quarterlyFeeAmt = systemSetting.getQuarterlyFeeAmount();

            LocalDate now = LocalDate.now();
            int currentYear = now.getYear();
            int currentQuarter = QuarterUtils.quarterOf(now);
            LocalDate startOfYear = LocalDate.of(currentYear, 1, 1);

            // 1. Total Active Members
            long totalActiveMembers = member.countActiveMembers();

            // 2. Total Revenue: Yearly Baseline + Current Year Payments - Current Year Expenses
            BigDecimal yearlyBaseline = baselineService.getBaselineForYear(currentYear);
            BigDecimal currentYearPayments = payment.sumAmountByDateReceivedBetween(startOfYear, now);
            BigDecimal currentYearExpenses = expense.sumAmountByDateOfExpenseBetween(startOfYear, now);

            BigDecimal totalRevenue = yearlyBaseline.add(currentYearPayments).subtract(currentYearExpenses);

            // 3. Dues for Current Quarter
            BigDecimal expectedDues = BigDecimal.valueOf(totalActiveMembers).multiply(quarterlyFeeAmt);
            BigDecimal collectedCurrentQ = payment.sumAmountByYearAndQuarter(currentYear, currentQuarter);

            BigDecimal duesThisQuarter = expectedDues.subtract(collectedCurrentQ);
            if (duesThisQuarter.compareTo(BigDecimal.ZERO) < 0) {
                duesThisQuarter = BigDecimal.ZERO;
            }

            // 4. Overdue Total (single aggregate SQL — no entity loading)
            BigDecimal overdueTotal = payment.calculateTotalOverdue(
                    currentYear, currentQuarter, quarterlyFeeAmt,
                    ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                    ReferenceConstants.MEMBER_STATUS.ACTIVE);

            // 5. Quarterly Collections
            Map<Integer, BigDecimal> quarterlyTotalsMap = new HashMap<>();
            for (var qt : payment.findQuarterlyTotals(currentYear)) {
                quarterlyTotalsMap.put(qt.getQuarter(), qt.getTotalCollected());
            }

            List<QuarterlyCollectionDto> collections = new ArrayList<>();
            for (int q = 1; q <= 4; q++) {
                BigDecimal collected = quarterlyTotalsMap.getOrDefault(q, BigDecimal.ZERO);
                QuarterStatus status;
                if (q > currentQuarter) {
                    status = QuarterStatus.FUTURE;
                } else if (q == currentQuarter) {
                    status = QuarterStatus.CURRENT;
                } else {
                    status = QuarterStatus.PAST;
                }

                BigDecimal denom = BigDecimal.valueOf(totalActiveMembers).multiply(quarterlyFeeAmt);
                double pct = 0;
                if (denom.compareTo(BigDecimal.ZERO) > 0) {
                    pct = collected.divide(denom, 4, RoundingMode.HALF_UP).doubleValue();
                }

                String label = "Q" + q;
                if (QuarterStatus.CURRENT.equals(status)) {
                    label += " (Current)";
                }

                collections.add(QuarterlyCollectionDto.builder()
                        .quarterLabel(label)
                        .collectedAmount(collected)
                        .percentage(pct)
                        .status(status)
                        .build());
            }

            // Persist snapshot
            DashboardSnapshotVO snapshot = dashboardSnapshot.getSnapshot().orElse(new DashboardSnapshotVO());
            snapshot.setId(1);
            snapshot.setTotalActiveMembers(totalActiveMembers);
            snapshot.setTotalRevenue(totalRevenue);
            snapshot.setDuesThisQuarter(duesThisQuarter);
            snapshot.setOverdueTotal(overdueTotal);
            snapshot.setQuarterlyFeeAmt(quarterlyFeeAmt);
            snapshot.setQuarterlyCollections(objectMapper.writeValueAsString(collections));
            snapshot.setLastProjectedAt(LocalDateTime.now());

            dashboardSnapshot.saveSnapshot(snapshot);
            logger.info("Dashboard snapshot rebuilt successfully");

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize quarterly collections for dashboard snapshot", e);
        }
    }
}