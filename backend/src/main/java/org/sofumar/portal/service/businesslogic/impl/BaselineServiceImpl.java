package org.sofumar.portal.service.businesslogic.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.BaselineConstants;
import org.sofumar.portal.data.vo.SystemSettingsVO;
import org.sofumar.portal.repo.ExpenseRepository;
import org.sofumar.portal.repo.PaymentRepository;
import org.sofumar.portal.repo.SystemSettingsRepository;
import org.sofumar.portal.repo.jpaspec.SystemSettingsSpecifications;
import org.sofumar.portal.service.businesslogic.BaselineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BaselineServiceImpl implements BaselineService {
    private static final Logger logger = LoggerFactory.getLogger(BaselineServiceImpl.class);

    // Baseline revenue amount hardcoded for 2026
    private static final BigDecimal YEARLY_BASELINE_2026 = new BigDecimal("59863.68");

    private final SystemSettingsRepository settingsRepo;
    private final PaymentRepository paymentRepo;
    private final ExpenseRepository expenseRepo;

    @Override
    @Transactional
    public BigDecimal getBaselineForYear(int year) {
        String key = BaselineConstants.getYearlyBaselineKey(year);

        // 1. Check if snapshot exists
        Optional<SystemSettingsVO> snapshot = settingsRepo.findOne(SystemSettingsSpecifications.isSettingKey(key));

        if (snapshot.isPresent()) {
            return new BigDecimal(snapshot.get().getSettingValue());
        }

        // 2. Fallback: Dynamic Calculation
        logger.warn("Baseline snapshot for {} not found. Calculating dynamically.", year);
        BigDecimal baseline;
        if (year == 2026) {
            baseline = YEARLY_BASELINE_2026;
        } else {
            baseline = calculateRevenueAsOf(LocalDate.of(year, 1, 1));
        }

        // 3. "Soft-Cache": Persist for future use
        saveBaselineSnapshot(year, baseline);

        return baseline;
    }

    @Override
    @Transactional
    public void closeYear(int year) {
        logger.info("Closing year {} and capturing final baseline.", year);
        // Baseline for year Y+1 is revenue as of Dec 31 of year Y (exclusive of Jan 1 of Y+1)
        BigDecimal finalRevenue = calculateRevenueAsOf(LocalDate.of(year + 1, 1, 1));
        saveBaselineSnapshot(year + 1, finalRevenue);
    }

    private BigDecimal calculateRevenueAsOf(LocalDate firstDayOfTargetYear) {
        // Seed from System Settings
        BigDecimal seed = getSeedAmount();

        // Payments received BEFORE firstDayOfTargetYear
        BigDecimal payments = paymentRepo.sumAmountByDateReceivedBefore(firstDayOfTargetYear);
        if (payments == null)
            payments = BigDecimal.ZERO;

        // Expenses incurred BEFORE firstDayOfTargetYear
        BigDecimal expenses = expenseRepo.sumAmountByDateOfExpenseBefore(firstDayOfTargetYear);
        if (expenses == null)
            expenses = BigDecimal.ZERO;

        return seed.add(payments).subtract(expenses);
    }

    private BigDecimal getSeedAmount() {
        return settingsRepo.findOne(SystemSettingsSpecifications.isSettingKey(BaselineConstants.KEY_SEED))
                .map(vo -> new BigDecimal(vo.getSettingValue()))
                .orElse(BigDecimal.ZERO);
    }

    private void saveBaselineSnapshot(int year, BigDecimal amount) {
        String key = BaselineConstants.getYearlyBaselineKey(year);
        SystemSettingsVO vo = settingsRepo.findOne(SystemSettingsSpecifications.isSettingKey(key))
                .orElse(new SystemSettingsVO());

        vo.setSettingType(BaselineConstants.TYPE_BASELINE);
        vo.setSettingKey(key);
        vo.setSettingValue(amount.toString());
        vo.setActive(true);
        vo.setEffectiveDate(LocalDate.of(year, 1, 1));

        settingsRepo.save(vo);
        logger.info("Saved baseline snapshot for year {} with value {}", year, amount);
    }
}