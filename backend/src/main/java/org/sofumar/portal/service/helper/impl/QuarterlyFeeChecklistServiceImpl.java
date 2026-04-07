package org.sofumar.portal.service.helper.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.QuarterCellStatus;
import org.sofumar.portal.constants.ReferenceConstants;
import org.sofumar.portal.core.businesslogic.Member;
import org.sofumar.portal.core.businesslogic.Payment;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.data.dto.request.ChecklistSearchRequestDto;
import org.sofumar.portal.data.dto.response.MemberQuarterlyRowDto;
import org.sofumar.portal.data.dto.response.PaymentSummary;
import org.sofumar.portal.data.dto.response.QuarterCellDto;
import org.sofumar.portal.data.dto.response.QuarterlyChecklistDto;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.data.response.SinglePagedResult;
import org.sofumar.portal.service.helper.QuarterlyFeeChecklistService;
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
    private static final BigDecimal QUARTERLY_FEE_AMOUNT = new BigDecimal("60.00");

    private final Member member;
    private final Payment payment;

    @Override
    public SinglePagedResult<QuarterlyChecklistDto> getQuarterlyChecklist(ChecklistSearchRequestDto request) {
        int year = (request.getYear() != null) ? request.getYear() : LocalDate.now().getYear();
        logger.info("Generating quarterly fee checklist for year: {}", year);

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;

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
        Map<Integer, Map<Integer, BigDecimal>> paymentMap = new HashMap<>();
        for (PaymentSummary s : summaries) {
            paymentMap.computeIfAbsent(s.getMemberID(), k -> new HashMap<>())
                    .merge(s.getQuarter(), s.getTotalPaid(), BigDecimal::add);
        }

        // 4. Build rows for each active member (already sorted and paginated by DB)
        List<MemberQuarterlyRowDto> rows = new ArrayList<>();

        for (MemberVO m : activeMembers) {
            LocalDate joinDate = m.getJoinDate();
            if (joinDate == null) {
                joinDate = LocalDate.now();
            }

            int joinYear = joinDate.getYear();
            int joinQuarter = (joinDate.getMonthValue() - 1) / 3 + 1;

            List<QuarterCellDto> quarterCells = new ArrayList<>();
            BigDecimal totalPaid = BigDecimal.ZERO;
            int eligibleQuarters = 0;

            for (int q = 1; q <= 4; q++) {
                QuarterCellStatus status;
                BigDecimal amountPaid = BigDecimal.ZERO;

                if (year > currentYear || (year == currentYear && q > currentQuarter)) {
                    // Future quarter
                    status = QuarterCellStatus.FUTURE;
                } else if (year > joinYear || (year == joinYear && q >= joinQuarter)) {
                    // Member was active during this quarter — check payment
                    BigDecimal paid = paymentMap.getOrDefault(m.getMemberID(), Collections.emptyMap())
                            .getOrDefault(q, BigDecimal.ZERO);
                    amountPaid = paid;

                    if (paid.compareTo(QUARTERLY_FEE_AMOUNT) >= 0) {
                        status = QuarterCellStatus.PAID;
                    } else {
                        status = QuarterCellStatus.UNPAID;
                    }

                    totalPaid = totalPaid.add(paid);
                    eligibleQuarters++;
                } else {
                    // Before member's join date
                    status = QuarterCellStatus.NOT_APPLICABLE;
                }

                quarterCells.add(QuarterCellDto.builder()
                        .quarter(q)
                        .status(status)
                        .amountPaid(amountPaid)
                        .build());
            }

            BigDecimal expectedTotal = QUARTERLY_FEE_AMOUNT.multiply(new BigDecimal(eligibleQuarters));
            BigDecimal balance = expectedTotal.subtract(totalPaid);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO;
            }

            rows.add(MemberQuarterlyRowDto.builder()
                    .memberID(m.getMemberID())
                    .memberName(m.getFirstName() + " " + m.getLastName())
                    .quarters(quarterCells)
                    .totalPaid(totalPaid)
                    .balance(balance)
                    .build());
        }

        QuarterlyChecklistDto checklistDto = QuarterlyChecklistDto.builder()
                .year(year)
                .currentQuarter(assessableQuarter)
                .quarterlyFeeAmount(QUARTERLY_FEE_AMOUNT)
                .rows(List.copyOf(rows))
                .build();

        PaginationMeta meta = PaginationMeta.of(memberPage.getNumber(), memberPage.getSize(),
                memberPage.getTotalElements(), memberPage.getTotalPages());
        return SinglePagedResult.of(checklistDto, meta);
    }
}