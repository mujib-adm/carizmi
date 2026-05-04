package io.carizmi.domain.finance.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.domain.finance.constants.QuarterCellStatus;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.domain.membership.service.Member;
import io.carizmi.domain.finance.service.Payment;
import io.carizmi.domain.platform.service.SystemSetting;
import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.domain.finance.data.dto.request.ChecklistSearchRequestDto;
import io.carizmi.domain.finance.data.dto.response.ChecklistSummaryDto;
import io.carizmi.shared.data.dto.MemberJoinDateProjection;
import io.carizmi.domain.finance.data.dto.response.MemberQuarterlyRowDto;
import io.carizmi.shared.data.dto.PaymentSummary;
import io.carizmi.domain.finance.data.dto.response.QuarterCellDto;
import io.carizmi.domain.finance.data.dto.response.QuarterSummaryDto;
import io.carizmi.domain.finance.data.dto.response.QuarterlyChecklistDto;
import io.carizmi.framework.data.response.PaginationMeta;
import io.carizmi.framework.data.response.SinglePagedResult;
import io.carizmi.domain.finance.service.QuarterlyFeeChecklistService;
import io.carizmi.shared.util.QuarterUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuarterlyFeeChecklistServiceImpl implements QuarterlyFeeChecklistService {

    private static final Logger logger = LoggerFactory.getLogger(QuarterlyFeeChecklistServiceImpl.class);

    private final Member member;
    private final Payment payment;
    private final SystemSetting systemSetting;

    @Override
    public SinglePagedResult<QuarterlyChecklistDto> getQuarterlyChecklist(ChecklistSearchRequestDto request) {
        int year = (request.getYear() != null) ? request.getYear() : LocalDate.now().getYear();
        BigDecimal quarterlyFeeAmount = systemSetting.getQuarterlyFeeAmount();

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentQuarter = QuarterUtils.quarterOf(now);

        // For past years, all 4 quarters are assessable; for current year, only up to currentQuarter
        int assessableQuarter = (year < currentYear) ? 4 : (year == currentYear) ? currentQuarter : 0;

        // 1. Fetch paginated active members, sorted by name at the DB level
        Pageable requestedPageable = request.toPageable();
        Pageable pageable = PageRequest.of(requestedPageable.getPageNumber(), requestedPageable.getPageSize(),
                Sort.by(FieldConstants.FIRST_NAME, FieldConstants.LAST_NAME));
        Page<MemberVO> memberPage = member.findActiveMembers(pageable);
        List<MemberVO> activeMembers = memberPage.getContent();

        // 2. Fetch payment summaries scoped to only the members on the current page
        List<Integer> memberIds = activeMembers.stream().map(MemberVO::getMemberID).toList();
        List<PaymentSummary> summaries = memberIds.isEmpty()
                ? Collections.emptyList()
                : payment.findMembersPaymentSummaries(memberIds, ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year);

        // 3. Build payment lookup: memberID → quarter → totalPaid
        Map<Integer, Map<Integer, BigDecimal>> paymentMap = buildPaymentMap(summaries);

        // 4. Build rows for each active member (already sorted and paginated by DB)
        List<MemberQuarterlyRowDto> rows = buildRows(activeMembers, paymentMap, quarterlyFeeAmount, year, currentYear, currentQuarter);

        // 5. Compute global summary across ALL members (not just current page)
        ChecklistSummaryDto summary = computeGlobalSummary(quarterlyFeeAmount, year, currentYear, currentQuarter, assessableQuarter);

        QuarterlyChecklistDto checklistDto = QuarterlyChecklistDto.builder()
                .year(year)
                .currentQuarter(assessableQuarter)
                .quarterlyFeeAmount(quarterlyFeeAmount)
                .rows(List.copyOf(rows))
                .summary(summary)
                .build();

        PaginationMeta meta = PaginationMeta.of(memberPage.getNumber(), memberPage.getSize(),
                memberPage.getTotalElements(), memberPage.getTotalPages());
        return SinglePagedResult.of(checklistDto, meta);
    }

    /**
     * Builds the payment lookup map: memberID → quarter → totalPaid.
     */
    private Map<Integer, Map<Integer, BigDecimal>> buildPaymentMap(List<PaymentSummary> summaries) {
        Map<Integer, Map<Integer, BigDecimal>> paymentMap = new HashMap<>();
        for (PaymentSummary s : summaries) {
            paymentMap.computeIfAbsent(s.getMemberID(), k -> new HashMap<>())
                    .merge(s.getQuarter(), s.getTotalPaid(), BigDecimal::add);
        }
        return paymentMap;
    }

    /**
     * Builds the per-member rows for the current page.
     */
    private List<MemberQuarterlyRowDto> buildRows(List<MemberVO> activeMembers,
                                                   Map<Integer, Map<Integer, BigDecimal>> paymentMap,
                                                   BigDecimal quarterlyFeeAmount,
                                                   int year, int currentYear, int currentQuarter) {
        List<MemberQuarterlyRowDto> rows = new ArrayList<>();

        for (MemberVO m : activeMembers) {
            LocalDate joinDate = QuarterUtils.resolveJoinDate(m.getJoinDate());
            int joinYear = joinDate.getYear();
            int joinQuarter = QuarterUtils.quarterOf(joinDate);

            List<QuarterCellDto> quarterCells = new ArrayList<>();
            BigDecimal totalPaid = BigDecimal.ZERO;
            int eligibleQuarters = 0;

            for (int q = 1; q <= 4; q++) {
                BigDecimal amountPaid = BigDecimal.ZERO;
                QuarterCellStatus status = resolveQuarterStatus(q, year, currentYear, currentQuarter,
                        joinYear, joinQuarter, BigDecimal.ZERO, quarterlyFeeAmount);

                if (status == QuarterCellStatus.PAID || status == QuarterCellStatus.UNPAID) {
                    BigDecimal paid = paymentMap.getOrDefault(m.getMemberID(), Collections.emptyMap())
                            .getOrDefault(q, BigDecimal.ZERO);
                    amountPaid = paid;
                    status = resolveQuarterStatus(q, year, currentYear, currentQuarter,
                            joinYear, joinQuarter, paid, quarterlyFeeAmount);
                    totalPaid = totalPaid.add(paid);
                    eligibleQuarters++;
                }

                quarterCells.add(QuarterCellDto.builder()
                        .quarter(q)
                        .status(status)
                        .amountPaid(amountPaid)
                        .build());
            }

            rows.add(MemberQuarterlyRowDto.builder()
                    .memberID(m.getMemberID())
                    .memberName(m.getFirstName() + " " + m.getLastName())
                    .quarters(quarterCells)
                    .totalPaid(totalPaid)
                    .balance(computeBalance(eligibleQuarters, totalPaid, quarterlyFeeAmount))
                    .build());
        }

        return rows;
    }

    /**
     * Computes the global summary across ALL active members using lightweight projections.
     * - Fetches only memberID + joinDate (2 columns) instead of full MemberVO entities
     * - Reuses the existing findPaymentSummaries SQL aggregate query
     */
    private ChecklistSummaryDto computeGlobalSummary(BigDecimal quarterlyFeeAmount, int year, int currentYear, int currentQuarter, int assessableQuarter) {
        // Lightweight projection: only memberID + joinDate
        List<MemberJoinDateProjection> allJoinDates = member.findActiveMemberJoinDates();

        // Reuse existing SQL aggregate: memberID × quarter → totalPaid
        List<PaymentSummary> allSummaries = payment.findPaymentSummaries(
                ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE, year);
        Map<Integer, Map<Integer, BigDecimal>> globalPaymentMap = buildPaymentMap(allSummaries);

        BigDecimal grandTotalPaid = BigDecimal.ZERO;
        BigDecimal grandTotalBalance = BigDecimal.ZERO;

        // Per-quarter accumulators
        int[] paidCounts = new int[4];
        int[] unpaidCounts = new int[4];

        for (MemberJoinDateProjection proj : allJoinDates) {
            LocalDate joinDate = QuarterUtils.resolveJoinDate(proj.getJoinDate());
            int joinYear = joinDate.getYear();
            int joinQuarter = QuarterUtils.quarterOf(joinDate);

            BigDecimal memberTotalPaid = BigDecimal.ZERO;
            int eligibleQuarters = 0;

            for (int q = 1; q <= 4; q++) {
                BigDecimal paid = globalPaymentMap.getOrDefault(proj.getMemberID(), Collections.emptyMap())
                        .getOrDefault(q, BigDecimal.ZERO);

                QuarterCellStatus status = resolveQuarterStatus(q, year, currentYear, currentQuarter,
                        joinYear, joinQuarter, paid, quarterlyFeeAmount);

                if (status == QuarterCellStatus.PAID) {
                    paidCounts[q - 1]++;
                    memberTotalPaid = memberTotalPaid.add(paid);
                    eligibleQuarters++;
                } else if (status == QuarterCellStatus.UNPAID) {
                    unpaidCounts[q - 1]++;
                    memberTotalPaid = memberTotalPaid.add(paid);
                    eligibleQuarters++;
                }
                // FUTURE and NOT_APPLICABLE don't count toward paid/unpaid
            }

            grandTotalPaid = grandTotalPaid.add(memberTotalPaid);
            grandTotalBalance = grandTotalBalance.add(
                    computeBalance(eligibleQuarters, memberTotalPaid, quarterlyFeeAmount));
        }

        // Build quarter summaries
        List<QuarterSummaryDto> quarterSummaries = new ArrayList<>();
        for (int q = 1; q <= 4; q++) {
            boolean isFuture = q > assessableQuarter;
            quarterSummaries.add(QuarterSummaryDto.builder()
                    .quarter(q)
                    .paidCount(isFuture ? 0 : paidCounts[q - 1])
                    .unpaidCount(isFuture ? 0 : unpaidCounts[q - 1])
                    .future(isFuture)
                    .build());
        }

        return ChecklistSummaryDto.builder()
                .totalPaid(grandTotalPaid)
                .totalBalance(grandTotalBalance)
                .quarterSummaries(quarterSummaries)
                .build();
    }

    /**
     * Resolves the payment status for a specific quarter based on eligibility and payment data.
     */
    private QuarterCellStatus resolveQuarterStatus(int quarter, int year, int currentYear, int currentQuarter,
                                                    int joinYear, int joinQuarter,
                                                    BigDecimal amountPaid, BigDecimal quarterlyFeeAmount) {
        if (year > currentYear || (year == currentYear && quarter > currentQuarter)) {
            return QuarterCellStatus.FUTURE;
        }
        if (year > joinYear || (year == joinYear && quarter >= joinQuarter)) {
            return amountPaid.compareTo(quarterlyFeeAmount) >= 0
                    ? QuarterCellStatus.PAID : QuarterCellStatus.UNPAID;
        }
        return QuarterCellStatus.NOT_APPLICABLE;
    }

    /**
     * Computes the balance owed, clamped to zero (no negative balances).
     */
    private BigDecimal computeBalance(int eligibleQuarters, BigDecimal totalPaid, BigDecimal quarterlyFeeAmount) {
        BigDecimal expectedTotal = quarterlyFeeAmount.multiply(BigDecimal.valueOf(eligibleQuarters));
        return expectedTotal.subtract(totalPaid).max(BigDecimal.ZERO);
    }
}