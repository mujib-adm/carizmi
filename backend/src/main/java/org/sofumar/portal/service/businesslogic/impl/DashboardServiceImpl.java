package org.sofumar.portal.service.businesslogic.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.QuarterStatus;
import org.sofumar.portal.constants.ReferenceCodeConstants;
import org.sofumar.portal.data.dto.DashboardMetricsDto;
import org.sofumar.portal.data.dto.QuarterlyCollectionDto;
import org.sofumar.portal.data.vo.MemberVO;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.repo.ExpenseRepository;
import org.sofumar.portal.repo.MemberRepository;
import org.sofumar.portal.repo.PaymentRepository;
import org.sofumar.portal.repo.PaymentRepository.PaymentSummary;
import org.sofumar.portal.service.businesslogic.DashboardService;
import org.springframework.data.jpa.domain.Specification;
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

    // Baseline revenue amount hardcoded temporarily
    private static final BigDecimal baseline = new BigDecimal("59863.68");
    private static final BigDecimal quarterlyFeeAmt = new BigDecimal("60");

    private final MemberRepository memberRepo;
    private final PaymentRepository paymentRepo;
    private final ExpenseRepository expenseRepo;

    @Override
    public ResponseEntity<GlobalResponse<DashboardMetricsDto>> getMetrics() {
        logger.info("Fetching dashboard metrics");

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;

        // 1. Total Active Members
        long totalActiveMembers = memberRepo.countByStatus(ReferenceCodeConstants.MEMBER_STATUS.ACTIVE);

        // 2. Total Revenue: Baseline + Payments - Expenses
        BigDecimal totalPayments = paymentRepo.sumTotalAmount();
        BigDecimal totalExpenses = expenseRepo.sumTotalAmount();

        if (totalPayments == null)
            totalPayments = BigDecimal.ZERO;
        if (totalExpenses == null)
            totalExpenses = BigDecimal.ZERO;

        BigDecimal totalRevenue = baseline.add(totalPayments).subtract(totalExpenses);

        // 3. Dues for Current Quarter
        // Expected: Active Members * quarterlyFeeAmt
        BigDecimal expectedDues = new BigDecimal(totalActiveMembers).multiply(quarterlyFeeAmt);
        // Collected in current quarter
        BigDecimal collectedCurrentQ = paymentRepo.sumAmountByYearAndQuarter(currentYear, currentQuarter);
        if (collectedCurrentQ == null)
            collectedCurrentQ = BigDecimal.ZERO;

        BigDecimal duesThisQuarter = expectedDues.subtract(collectedCurrentQ);
        if (duesThisQuarter.compareTo(BigDecimal.ZERO) < 0) {
            duesThisQuarter = BigDecimal.ZERO;
        }

        // 4. Overdues (Excluding Current Quarter)
        // Fetch Membership Fee payments grouped by member/year/quarter
        List<PaymentSummary> summaries = paymentRepo.findPaymentSummaries(ReferenceCodeConstants.FEE_TYPE.MEMBERSHIP_FEE);

        // Map: MemberID -> "Year-Quarter" -> Amount
        Map<Integer, Map<String, BigDecimal>> paymentMap = new HashMap<>();
        for (PaymentSummary s : summaries) {
            paymentMap.computeIfAbsent(s.getMemberID(), k -> new HashMap<>())
                    .put(s.getYear() + "-" + s.getQuarter(), s.getTotalPaid());
        }

        BigDecimal overdueTotal = BigDecimal.ZERO;

        // Fetch active members
        List<MemberVO> activeMembers = listActiveMembers();

        for (MemberVO m : activeMembers) {
            LocalDate startDate = m.getJoinDate();
            if (startDate == null)
                startDate = LocalDate.now(); // Fallback

            // Iterate from startDate to Quarter Before Current
            LocalDate iterDate = startDate;

            // Safety brake: don't go back too far if data is bad (the app was launched in 2026)
            if (iterDate.getYear() < 2026)
                iterDate = LocalDate.of(2026, 1, 1);

            // Important: We need to normalize iterDate to the start of its quarter to avoid
            // issues with day-of-month logic
            iterDate = getQuarterStart(iterDate);

            while (isBeforeCurrentQuarter(iterDate, currentYear, currentQuarter)) {
                int y = iterDate.getYear();
                int q = (iterDate.getMonthValue() - 1) / 3 + 1;
                String key = y + "-" + q;

                BigDecimal paid = paymentMap.getOrDefault(m.getMemberID(), Collections.emptyMap()).getOrDefault(key, BigDecimal.ZERO);
                BigDecimal due = quarterlyFeeAmt.subtract(paid);
                if (due.compareTo(BigDecimal.ZERO) > 0) {
                    overdueTotal = overdueTotal.add(due);
                }

                // Move to next quarter
                iterDate = iterDate.plusMonths(3);
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

    private List<MemberVO> listActiveMembers() {
        Specification<MemberVO> spec = (root, query, cb)
                -> cb.equal(root.get(FieldConstants.STATUS), ReferenceCodeConstants.MEMBER_STATUS.ACTIVE);
        return memberRepo.findAll(spec);
    }

    private LocalDate getQuarterStart(LocalDate date) {
        int qMonth = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDate.of(date.getYear(), qMonth, 1);
    }

    private boolean isBeforeCurrentQuarter(LocalDate date, int currentYear, int currentQuarter) {
        int y = date.getYear();
        int q = (date.getMonthValue() - 1) / 3 + 1;
        if (y < currentYear)
            return true;
        return y == currentYear && q < currentQuarter;
    }

    private QuarterlyCollectionDto computeQuarterData(int year, int quarter, int currentQuarter, long totalActiveMembers) {
        BigDecimal collected = BigDecimal.ZERO;
        QuarterStatus status;

        if (quarter > currentQuarter) {
            status = QuarterStatus.FUTURE;
        } else if (quarter == currentQuarter) {
            status = QuarterStatus.CURRENT;
            collected = paymentRepo.sumAmountByYearAndQuarter(year, quarter);
        } else {
            status = QuarterStatus.PAST;
            collected = paymentRepo.sumAmountByYearAndQuarter(year, quarter);
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
