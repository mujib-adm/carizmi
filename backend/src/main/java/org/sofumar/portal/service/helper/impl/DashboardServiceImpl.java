package org.sofumar.portal.service.helper.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.QuarterStatus;
import org.sofumar.portal.constants.ReferenceCodeConstants;
import org.sofumar.portal.data.dto.DashboardMetricsDto;
import org.sofumar.portal.data.dto.PaymentSummary;
import org.sofumar.portal.data.dto.QuarterlyCollectionDto;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.core.businesslogic.Expense;
import org.sofumar.portal.core.businesslogic.Member;
import org.sofumar.portal.core.businesslogic.Payment;
import org.sofumar.portal.service.helper.BaselineService;
import org.sofumar.portal.service.helper.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private static final BigDecimal quarterlyFeeAmt = new BigDecimal("60");

    private final Member member;
    private final Payment payment;
    private final Expense expense;
    private final BaselineService baselineService;

    @Override
    public ResponseEntity<GlobalResponse<DashboardMetricsDto>> getMetrics() {
        logger.info("Fetching dashboard metrics");

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
        LocalDate startOfYear = LocalDate.of(currentYear, 1, 1);

        // 1. Total Active Members
        long totalActiveMembers = member.countActiveMembers();

        // 2. Total Revenue: Yearly Baseline + Current Year Payments - Current Year Expenses
        BigDecimal yearlyBaseline = baselineService.getBaselineForYear(currentYear);

        BigDecimal currentYearPayments = payment.sumAmountByDateReceivedBetween(startOfYear, now);
        BigDecimal currentYearExpenses = expense.sumAmountByDateOfExpenseBetween(startOfYear, now);

        if (currentYearPayments == null)
            currentYearPayments = BigDecimal.ZERO;
        if (currentYearExpenses == null)
            currentYearExpenses = BigDecimal.ZERO;

        BigDecimal totalRevenue = yearlyBaseline.add(currentYearPayments).subtract(currentYearExpenses);

        // 3. Dues for Current Quarter
        // Expected: Active Members * quarterlyFeeAmt
        BigDecimal expectedDues = new BigDecimal(totalActiveMembers).multiply(quarterlyFeeAmt);
        // Collected in current quarter
        BigDecimal collectedCurrentQ = payment.sumAmountByYearAndQuarter(currentYear, currentQuarter);
        if (collectedCurrentQ == null)
            collectedCurrentQ = BigDecimal.ZERO;

        BigDecimal duesThisQuarter = expectedDues.subtract(collectedCurrentQ);
        if (duesThisQuarter.compareTo(BigDecimal.ZERO) < 0) {
            duesThisQuarter = BigDecimal.ZERO;
        }

        // 4. Overdues (Excluding Current Quarter)
        // Fetch Membership Fee payments aggregated for the current year only
        List<PaymentSummary> summaries = payment.findPaymentSummaries(ReferenceCodeConstants.FEE_TYPE.MEMBERSHIP_FEE, currentYear);

        // Map: MemberID -> Quarter -> Amount
        Map<Integer, Map<Integer, BigDecimal>> paymentMap = new HashMap<>();
        for (PaymentSummary s : summaries) {
            paymentMap.computeIfAbsent(s.getMemberID(), k -> new HashMap<>())
                    .merge(s.getQuarter(), s.getTotalPaid(), BigDecimal::add);
        }

        BigDecimal overdueTotal = BigDecimal.ZERO;

        // Fetch active members
        List<MemberVO> activeMembers = member.findAllActiveMembers();

        for (MemberVO m : activeMembers) {
            LocalDate joinDate = m.getJoinDate();
            if (joinDate == null)
                joinDate = LocalDate.now();

            // We only care about overdues in the current year
            // Start assessment from Q1 of current year, or the member's join quarter (if
            // joined this year)
            int startQ = 1;
            if (joinDate.getYear() == currentYear) {
                startQ = (joinDate.getMonthValue() - 1) / 3 + 1;
            } else if (joinDate.getYear() > currentYear) {
                continue; // Joined in the future? Skip.
            }

            // Iterate through "Past" quarters of the current year
            for (int q = startQ; q < currentQuarter; q++) {
                BigDecimal paid = paymentMap.getOrDefault(m.getMemberID(), Collections.emptyMap())
                        .getOrDefault(q, BigDecimal.ZERO);

                BigDecimal due = quarterlyFeeAmt.subtract(paid);
                if (due.compareTo(BigDecimal.ZERO) > 0) {
                    overdueTotal = overdueTotal.add(due);
                }
            }
        }

        // 5. Quarterly Collections (Calendar Year: Q1, Q2, Q3, Q4)
        List<QuarterlyCollectionDto> collections = new ArrayList<>();

        for (int q = 1; q <= 4; q++) {
            collections.add(computeQuarterData(currentYear, q, currentQuarter, totalActiveMembers));
        }

        DashboardMetricsDto metrics = DashboardMetricsDto.builder()
                .totalMembers(totalActiveMembers)
                .totalRevenue(totalRevenue)
                .duesThisQuarter(duesThisQuarter)
                .overdueTotal(overdueTotal)
                .quarterlyFeeAmt(quarterlyFeeAmt)
                .quarterlyCollections(collections)
                .build();

        return ResponseUtils.okWithData(metrics);
    }

    private QuarterlyCollectionDto computeQuarterData(int year, int quarter, int currentQuarter, long totalActiveMembers) {
        BigDecimal collected = BigDecimal.ZERO;
        QuarterStatus status;

        if (quarter > currentQuarter) {
            status = QuarterStatus.FUTURE;
        } else if (quarter == currentQuarter) {
            status = QuarterStatus.CURRENT;
            collected = payment.sumAmountByYearAndQuarter(year, quarter);
        } else {
            status = QuarterStatus.PAST;
            collected = payment.sumAmountByYearAndQuarter(year, quarter);
        }

        if (collected == null)
            collected = BigDecimal.ZERO;

        // Denom: Active * quarterlyFeeAmt
        BigDecimal denom = new BigDecimal(totalActiveMembers).multiply(quarterlyFeeAmt);
        double pct = 0;
        if (denom.compareTo(BigDecimal.ZERO) > 0) {
            pct = collected.divide(denom, 4, RoundingMode.HALF_UP).doubleValue();
        }

        String label = "Q" + quarter;
        if (QuarterStatus.CURRENT.equals(status)) {
            label += " (Current)";
        }

        return QuarterlyCollectionDto.builder()
                .quarterLabel(label)
                .collectedAmount(collected)
                .percentage(pct)
                .status(status)
                .build();
    }
}