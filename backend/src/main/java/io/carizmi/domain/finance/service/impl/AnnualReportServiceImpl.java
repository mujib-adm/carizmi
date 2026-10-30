package io.carizmi.domain.finance.service.impl;

import io.carizmi.domain.finance.data.dto.projection.ExpenseCategoryTotalProjection;
import io.carizmi.domain.finance.data.dto.projection.FeeTypeReceiptBreakdownProjection;
import io.carizmi.domain.finance.data.dto.projection.PaymentMethodTotalProjection;
import io.carizmi.domain.finance.data.dto.response.AnnualCombinedComplianceDto;
import io.carizmi.domain.finance.data.dto.response.AnnualComplianceSummaryDto;
import io.carizmi.domain.finance.data.dto.response.AnnualExpenseCategoryDto;
import io.carizmi.domain.finance.data.dto.response.AnnualFeeTypeReceiptDto;
import io.carizmi.domain.finance.data.dto.response.AnnualFinancialReportDto;
import io.carizmi.domain.finance.data.dto.response.AnnualMembershipFeeComplianceDto;
import io.carizmi.domain.finance.data.dto.response.AnnualMethodReceiptDto;
import io.carizmi.domain.finance.data.dto.response.AnnualRegistrationFeeComplianceDto;
import io.carizmi.domain.finance.service.AnnualReportService;
import io.carizmi.domain.finance.service.Expense;
import io.carizmi.domain.finance.service.Payment;
import io.carizmi.domain.membership.service.Member;
import io.carizmi.domain.platform.constants.BaselineConstants;
import io.carizmi.domain.platform.service.BaselineService;
import io.carizmi.domain.finance.model.AnnualReportSnapshotVO;
import io.carizmi.domain.finance.service.AnnualReportSnapshot;
import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.domain.platform.service.SystemSetting;
import io.carizmi.shared.constants.ReferenceConstants;
import io.carizmi.shared.util.QuarterUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public final class AnnualReportServiceImpl implements AnnualReportService {

    private static final Logger logger = LoggerFactory.getLogger(AnnualReportServiceImpl.class);

    private final BaselineService baselineService;
    private final Payment payment;
    private final Expense expense;
    private final Member member;
    private final SystemSetting systemSetting;
    private final AnnualReportSnapshot annualReportSnapshot;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AnnualFinancialReportDto getAnnualReport(Integer inputYear) {
        int reportYear = (inputYear != null) ? inputYear : LocalDate.now().getYear();

        // 1. Check if frozen snapshot exists in CQRS read model
        Optional<AnnualReportSnapshotVO> snapshotOpt = annualReportSnapshot.findByFiscalYear(reportYear);
        if (snapshotOpt.isPresent()) {
            logger.info("Serving frozen Annual Financial Report snapshot for closed year: {}", reportYear);
            try {
                return objectMapper.readValue(snapshotOpt.get().getReportData(), AnnualFinancialReportDto.class);
            } catch (Exception e) {
                logger.error("Failed to deserialize report snapshot for year {}: {}", reportYear, e.getMessage(), e);
                return AnnualFinancialReportDto.builder().build();
            }
        }

        // 2. Check if the year is closed (YEARLY_BASELINE_{reportYear + 1} exists)
        String closingKey = BaselineConstants.getYearlyBaselineKey(reportYear + 1);
        boolean isClosedYear = systemSetting.findBySettingKey(closingKey).isPresent();

        // 3. If closed year but snapshot missing, ensure delinquent members are deactivated and freeze snapshot
        if (isClosedYear) {
            logger.info("Fiscal year {} is closed but snapshot not found. Ensuring delinquent members are deactivated and freezing snapshot.", reportYear);
            member.deactivateDelinquentMembers(reportYear);
            return createAndPersistSnapshot(reportYear, null);
        }

        // 4. Live projection for open year
        return computeAnnualReport(reportYear, false);
    }

    @Override
    @Transactional
    public AnnualFinancialReportDto generateAndFreezeSnapshot(int reportYear) {
        Optional<AnnualReportSnapshotVO> existingOpt = annualReportSnapshot.findByFiscalYear(reportYear);
        if (existingOpt.isPresent()) {
            logger.info("Frozen Annual Financial Report snapshot already exists for year {}. Returning existing snapshot.", reportYear);
            try {
                return objectMapper.readValue(existingOpt.get().getReportData(), AnnualFinancialReportDto.class);
            } catch (Exception e) {
                logger.warn("Existing snapshot for year {} could not be deserialized; regenerating snapshot: {}", reportYear, e.getMessage());
            }
        }
        return createAndPersistSnapshot(reportYear, existingOpt.orElse(null));
    }

    private AnnualFinancialReportDto createAndPersistSnapshot(int reportYear, AnnualReportSnapshotVO existingSnapshot) {
        AnnualFinancialReportDto reportDto = computeAnnualReport(reportYear, true);
        reportDto.setIsClosed(true);
        reportDto.setSnapshotStatus("OFFICIAL_CLOSED_SNAPSHOT");

        try {
            String json = objectMapper.writeValueAsString(reportDto);
            BigDecimal beginningCashBalance = reportDto.getBeginningCashBalance();
            BigDecimal totalReceipts = reportDto.getTotalReceipts();
            BigDecimal totalDisbursements = reportDto.getTotalDisbursements();
            BigDecimal endingCashBalance = reportDto.getEndingCashBalance();
            long activeMembersCount = reportDto.getComplianceSummary() != null
                    ? reportDto.getComplianceSummary().getActiveMembersCount() : 0L;

            if (existingSnapshot != null) {
                existingSnapshot.setReportData(json);
                existingSnapshot.setBeginningCashBalance(beginningCashBalance);
                existingSnapshot.setTotalReceipts(totalReceipts);
                existingSnapshot.setTotalDisbursements(totalDisbursements);
                existingSnapshot.setEndingCashBalance(endingCashBalance);
                existingSnapshot.setActiveMembersCount(activeMembersCount);
                existingSnapshot.setClosedAt(LocalDateTime.now());
                existingSnapshot.setClosedBy("SYSTEM_SCHEDULER");
                annualReportSnapshot.update(existingSnapshot);
            } else {
                AnnualReportSnapshotVO snapshotVO = AnnualReportSnapshotVO.builder()
                        .fiscalYear(reportYear)
                        .reportData(json)
                        .beginningCashBalance(beginningCashBalance)
                        .totalReceipts(totalReceipts)
                        .totalDisbursements(totalDisbursements)
                        .endingCashBalance(endingCashBalance)
                        .activeMembersCount(activeMembersCount)
                        .closedAt(LocalDateTime.now())
                        .closedBy("SYSTEM_SCHEDULER")
                        .build();
                annualReportSnapshot.add(snapshotVO);
            }
            logger.info("Successfully froze and persisted Annual Financial Report snapshot for year {}", reportYear);
        } catch (Exception e) {
            logger.error("Failed to serialize or save report snapshot for year {}: {}", reportYear, e.getMessage(), e);
            throw new IllegalStateException("Failed to freeze annual report snapshot for year " + reportYear, e);
        }

        return reportDto;
    }

    private AnnualFinancialReportDto computeAnnualReport(int reportYear, boolean isClosedYear) {
        LocalDate startOfYear = LocalDate.of(reportYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(reportYear, 12, 31);

        logger.info("Computing Annual Financial Report for year: {}", reportYear);

        // 1. Cash Balance Summary (Reconciliation)
        BigDecimal beginningCashBalance = baselineService.getBaselineForYear(reportYear);
        if (beginningCashBalance == null) {
            beginningCashBalance = BigDecimal.ZERO;
        }

        BigDecimal totalReceipts = payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear);
        if (totalReceipts == null) {
            totalReceipts = BigDecimal.ZERO;
        }

        BigDecimal totalDisbursements = expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear);
        if (totalDisbursements == null) {
            totalDisbursements = BigDecimal.ZERO;
        }

        BigDecimal netOperatingSurplus = totalReceipts.subtract(totalDisbursements);
        BigDecimal endingCashBalance = beginningCashBalance.add(netOperatingSurplus);

        // 2. Receipts by Fee Type and Quarter
        Map<String, BigDecimal[]> feeQuarterMap = new LinkedHashMap<>();
        feeQuarterMap.put(ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        feeQuarterMap.put(ReferenceConstants.FEE_TYPE.REGISTRATION_FEE,
                new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        List<FeeTypeReceiptBreakdownProjection> feeProjections = payment.findFeeTypeReceiptBreakdownsBetween(startOfYear, endOfYear);
        if (feeProjections != null) {
            for (FeeTypeReceiptBreakdownProjection proj : feeProjections) {
                String rawFeeType = proj.getFeeType() != null ? proj.getFeeType() : "OTHER";
                Integer obYear = proj.getObligationYear();
                int receivedQ = proj.getReceivedQuarter() != null ? proj.getReceivedQuarter() : 1;
                int feeQ = proj.getFeeQuarter() != null ? proj.getFeeQuarter() : receivedQ;
                BigDecimal amount = proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO;

                String displayKey;
                int targetQuarter;

                if (ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE.equals(rawFeeType)) {
                    if (obYear == null || obYear == reportYear) {
                        displayKey = ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE;
                        targetQuarter = (feeQ >= 1 && feeQ <= 4) ? feeQ : receivedQ;
                    } else if (obYear < reportYear) {
                        displayKey = "Prior Year Arrears";
                        targetQuarter = receivedQ;
                    } else {
                        displayKey = "Prepaid Dues";
                        targetQuarter = receivedQ;
                    }
                } else {
                    displayKey = rawFeeType;
                    targetQuarter = receivedQ;
                }

                if (targetQuarter >= 1 && targetQuarter <= 4) {
                    feeQuarterMap.putIfAbsent(displayKey,
                            new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                    BigDecimal[] quarters = feeQuarterMap.get(displayKey);
                    quarters[targetQuarter - 1] = quarters[targetQuarter - 1].add(amount);
                }
            }
        }

        List<AnnualFeeTypeReceiptDto> receiptsByFeeType = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : feeQuarterMap.entrySet()) {
            BigDecimal[] quarters = entry.getValue();
            BigDecimal total = quarters[0].add(quarters[1]).add(quarters[2]).add(quarters[3]);
            receiptsByFeeType.add(AnnualFeeTypeReceiptDto.builder()
                    .feeType(entry.getKey())
                    .q1(quarters[0])
                    .q2(quarters[1])
                    .q3(quarters[2])
                    .q4(quarters[3])
                    .total(total)
                    .build());
        }

        // 3. Receipts by Payment Channel
        List<AnnualMethodReceiptDto> receiptsByPaymentMethod = new ArrayList<>();
        List<PaymentMethodTotalProjection> methodProjections = payment.findPaymentMethodTotalsBetween(startOfYear, endOfYear);
        if (methodProjections != null) {
            for (PaymentMethodTotalProjection proj : methodProjections) {
                BigDecimal amt = proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO;
                double pct = 0.0;
                if (totalReceipts.compareTo(BigDecimal.ZERO) > 0) {
                    pct = amt.multiply(BigDecimal.valueOf(100))
                            .divide(totalReceipts, 1, RoundingMode.HALF_UP)
                            .doubleValue();
                }
                receiptsByPaymentMethod.add(AnnualMethodReceiptDto.builder()
                        .methodOfPayment(proj.getMethodOfPayment())
                        .totalAmount(amt)
                        .percentage(pct)
                        .build());
            }
        }

        // 4. Disbursements by Expense Category
        List<AnnualExpenseCategoryDto> disbursementsByCategory = new ArrayList<>();
        List<ExpenseCategoryTotalProjection> expenseProjections = expense.findExpenseCategoryTotalsBetween(startOfYear, endOfYear);
        if (expenseProjections != null) {
            for (ExpenseCategoryTotalProjection proj : expenseProjections) {
                BigDecimal amt = proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO;
                double pct = 0.0;
                if (totalDisbursements.compareTo(BigDecimal.ZERO) > 0) {
                    pct = amt.multiply(BigDecimal.valueOf(100))
                            .divide(totalDisbursements, 1, RoundingMode.HALF_UP)
                            .doubleValue();
                }
                disbursementsByCategory.add(AnnualExpenseCategoryDto.builder()
                        .category(proj.getCategory())
                        .totalAmount(amt)
                        .percentage(pct)
                        .build());
            }
        }

        // 5. Membership Health & Compliance Indicators
        List<MemberVO> activeMembersInReportYear = member.findAllMembersActiveInYear(reportYear);
        if (activeMembersInReportYear == null) {
            activeMembersInReportYear = Collections.emptyList();
        }
        long activeMembersCount = activeMembersInReportYear.size();
        BigDecimal quarterlyFeeRate = systemSetting.getQuarterlyFeeAmount();
        if (quarterlyFeeRate == null) {
            quarterlyFeeRate = BigDecimal.ZERO;
        }

        // 5a. Membership Fee: Quarter Eligibility & Proration across episodes
        List<Integer> memberIds = activeMembersInReportYear.stream()
                .map(MemberVO::getMemberID)
                .filter(Objects::nonNull)
                .toList();
        Map<Integer, List<MemberMembershipPeriodVO>> periodsByMember;
        try {
            periodsByMember = memberIds.isEmpty() ? Collections.emptyMap() : member.findMembershipPeriodsForMembers(memberIds);
        } catch (Exception e) {
            periodsByMember = Collections.emptyMap();
        }

        long[] eligibleMemberCounts = new long[4];
        for (MemberVO m : activeMembersInReportYear) {
            LocalDate joinDate = QuarterUtils.resolveJoinDate(m.getJoinDate());
            int joinYear = joinDate.getYear();
            int joinQuarter = QuarterUtils.quarterOf(joinDate);
            List<MemberMembershipPeriodVO> periods =
                    m.getMemberID() != null ? periodsByMember.getOrDefault(m.getMemberID(), Collections.emptyList()) : Collections.emptyList();

            for (int q = 1; q <= 4; q++) {
                LocalDate qStart = QuarterUtils.firstDayOfQuarter(reportYear, q);
                LocalDate qEnd = QuarterUtils.lastDayOfQuarter(reportYear, q);

                boolean isActive;
                if (!periods.isEmpty()) {
                    isActive = periods.stream().anyMatch(p ->
                            !p.getStartDate().isAfter(qEnd) && (p.getEndDate() == null || !p.getEndDate().isBefore(qStart)));
                } else {
                    isActive = (reportYear > joinYear || (reportYear == joinYear && q >= joinQuarter));
                }

                if (isActive) {
                    eligibleMemberCounts[q - 1]++;
                }
            }
        }

        BigDecimal annualExpectedDues = BigDecimal.ZERO;
        for (int q = 0; q < 4; q++) {
            annualExpectedDues = annualExpectedDues.add(BigDecimal.valueOf(eligibleMemberCounts[q]).multiply(quarterlyFeeRate));
        }

        BigDecimal actualDuesCollected = receiptsByFeeType.stream()
                .filter(r -> ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE.equals(r.getFeeType()))
                .map(AnnualFeeTypeReceiptDto::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double collectionEfficiencyRate = 0.0;
        if (annualExpectedDues.compareTo(BigDecimal.ZERO) > 0) {
            collectionEfficiencyRate = actualDuesCollected.multiply(BigDecimal.valueOf(100))
                    .divide(annualExpectedDues, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        BigDecimal totalOverdueDues = annualExpectedDues.subtract(actualDuesCollected);
        if (totalOverdueDues.compareTo(BigDecimal.ZERO) < 0) {
            totalOverdueDues = BigDecimal.ZERO;
        }

        AnnualMembershipFeeComplianceDto membershipSummary = AnnualMembershipFeeComplianceDto.builder()
                .activeMembersCount(activeMembersCount)
                .quarterlyFeeRate(quarterlyFeeRate)
                .annualExpectedDues(annualExpectedDues)
                .actualDuesCollected(actualDuesCollected)
                .collectionEfficiencyRate(collectionEfficiencyRate)
                .totalOverdueDues(totalOverdueDues)
                .q1EligibleMembers(eligibleMemberCounts[0])
                .q2EligibleMembers(eligibleMemberCounts[1])
                .q3EligibleMembers(eligibleMemberCounts[2])
                .q4EligibleMembers(eligibleMemberCounts[3])
                .build();

        // 5b. Registration Fee: Newly Joined Members & One-Time Fee Compliance
        BigDecimal registrationFeeRate;
        try {
            registrationFeeRate = systemSetting.getRegistrationFeeAmount();
        } catch (Exception e) {
            logger.warn("Could not retrieve REGISTRATION_FEE system setting: {}", e.getMessage());
            registrationFeeRate = BigDecimal.ZERO;
        }
        if (registrationFeeRate == null) {
            registrationFeeRate = BigDecimal.ZERO;
        }

        Set<Integer> membersWithRegistrationFee = Collections.emptySet();
        try {
            List<Integer> regMemberIds = payment.findMemberIDsByFeeTypeAndDateReceivedBetween(
                    ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, startOfYear, endOfYear);
            if (regMemberIds != null) {
                membersWithRegistrationFee = new HashSet<>(regMemberIds);
            }
        } catch (Exception e) {
            logger.warn("Could not query registration fee member IDs: {}", e.getMessage());
        }

        long[] newMembersByQuarter = new long[4];
        for (MemberVO m : activeMembersInReportYear) {
            LocalDate joinDate = m.getJoinDate();
            if (joinDate != null && joinDate.getYear() == reportYear) {
                // Charter / legacy members migrated with default Jan 1 join date and no registration fee
                // in the report year are excluded from one-time registration fee obligations.
                if (joinDate.isEqual(startOfYear) && !membersWithRegistrationFee.contains(m.getMemberID())) {
                    continue;
                }
                int q = QuarterUtils.quarterOf(joinDate);
                if (q >= 1 && q <= 4) {
                    newMembersByQuarter[q - 1]++;
                }
            }
        }
        long totalNewMembers = newMembersByQuarter[0] + newMembersByQuarter[1] + newMembersByQuarter[2] + newMembersByQuarter[3];
        BigDecimal expectedRegistrationFees = BigDecimal.valueOf(totalNewMembers).multiply(registrationFeeRate);

        BigDecimal actualRegistrationCollected = receiptsByFeeType.stream()
                .filter(r -> ReferenceConstants.FEE_TYPE.REGISTRATION_FEE.equals(r.getFeeType()))
                .map(AnnualFeeTypeReceiptDto::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double regEfficiencyRate = 0.0;
        if (expectedRegistrationFees.compareTo(BigDecimal.ZERO) > 0) {
            regEfficiencyRate = actualRegistrationCollected.multiply(BigDecimal.valueOf(100))
                    .divide(expectedRegistrationFees, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        } else if (actualRegistrationCollected.compareTo(BigDecimal.ZERO) > 0) {
            regEfficiencyRate = 100.0;
        }

        BigDecimal totalOverdueRegistrationFees = expectedRegistrationFees.subtract(actualRegistrationCollected);
        if (totalOverdueRegistrationFees.compareTo(BigDecimal.ZERO) < 0) {
            totalOverdueRegistrationFees = BigDecimal.ZERO;
        }

        AnnualRegistrationFeeComplianceDto registrationSummary = AnnualRegistrationFeeComplianceDto.builder()
                .registrationFeeRate(registrationFeeRate)
                .newMembersCount(totalNewMembers)
                .q1NewMembers(newMembersByQuarter[0])
                .q2NewMembers(newMembersByQuarter[1])
                .q3NewMembers(newMembersByQuarter[2])
                .q4NewMembers(newMembersByQuarter[3])
                .expectedRegistrationFees(expectedRegistrationFees)
                .actualRegistrationFeesCollected(actualRegistrationCollected)
                .collectionEfficiencyRate(regEfficiencyRate)
                .totalOverdueRegistrationFees(totalOverdueRegistrationFees)
                .build();

        // 5c. Combined Summary: All Operating Fee Inflows
        BigDecimal totalExpectedAmount = annualExpectedDues.add(expectedRegistrationFees);
        BigDecimal totalActualCollected = actualDuesCollected.add(actualRegistrationCollected);

        double overallEfficiencyRate = 0.0;
        if (totalExpectedAmount.compareTo(BigDecimal.ZERO) > 0) {
            overallEfficiencyRate = totalActualCollected.multiply(BigDecimal.valueOf(100))
                    .divide(totalExpectedAmount, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        BigDecimal totalOverdueAmount = totalOverdueDues.add(totalOverdueRegistrationFees);

        AnnualCombinedComplianceDto combinedSummary = AnnualCombinedComplianceDto.builder()
                .totalExpectedAmount(totalExpectedAmount)
                .totalActualCollected(totalActualCollected)
                .overallCollectionEfficiencyRate(overallEfficiencyRate)
                .totalOverdueAmount(totalOverdueAmount)
                .build();

        AnnualComplianceSummaryDto complianceSummary = AnnualComplianceSummaryDto.builder()
                .activeMembersCount(activeMembersCount)
                .quarterlyFeeRate(quarterlyFeeRate)
                .annualExpectedDues(annualExpectedDues)
                .actualDuesCollected(actualDuesCollected)
                .collectionEfficiencyRate(collectionEfficiencyRate)
                .totalOverdueDues(totalOverdueDues)
                .membershipFeeSummary(membershipSummary)
                .registrationFeeSummary(registrationSummary)
                .combinedSummary(combinedSummary)
                .build();

        return AnnualFinancialReportDto.builder()
                .year(reportYear)
                .generatedDate(LocalDate.now())
                .baselineKey(BaselineConstants.getYearlyBaselineKey(reportYear))
                .beginningCashBalance(beginningCashBalance)
                .totalReceipts(totalReceipts)
                .totalDisbursements(totalDisbursements)
                .netOperatingSurplus(netOperatingSurplus)
                .endingCashBalance(endingCashBalance)
                .receiptsByFeeType(receiptsByFeeType)
                .receiptsByPaymentMethod(receiptsByPaymentMethod)
                .disbursementsByCategory(disbursementsByCategory)
                .complianceSummary(complianceSummary)
                .isClosed(isClosedYear)
                .snapshotStatus(isClosedYear ? "OFFICIAL_CLOSED_SNAPSHOT" : "LIVE_PROJECTION")
                .build();
    }
}