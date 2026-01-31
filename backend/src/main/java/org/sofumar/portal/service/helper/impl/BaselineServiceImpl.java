package org.sofumar.portal.service.helper.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.constants.BaselineConstants;
import org.sofumar.portal.core.vo.SystemSettingsVO;
import org.sofumar.portal.core.businesslogic.Expense;
import org.sofumar.portal.core.businesslogic.Payment;
import org.sofumar.portal.core.businesslogic.SystemSetting;
import org.sofumar.portal.service.helper.BaselineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class BaselineServiceImpl implements BaselineService {
    private static final Logger logger = LoggerFactory.getLogger(BaselineServiceImpl.class);

    // Baseline revenue amount hardcoded for app launch year
    private static final BigDecimal YEARLY_BASELINE_2026 = new BigDecimal("59863.68");

    private final SystemSetting systemSetting;
    private final Payment payment;
    private final Expense expense;

    @Autowired
    public BaselineServiceImpl(final SystemSetting systemSetting,
                               final Payment payment,
                               final Expense expense) {
        this.systemSetting = systemSetting;
        this.payment = payment;
        this.expense = expense;
    }

    @Override
    @Transactional
    public BigDecimal getBaselineForYear(int year) {
        String key = BaselineConstants.getYearlyBaselineKey(year);

        // 1. Check if snapshot exists
        Optional<SystemSettingsVO> snapshot = systemSetting.findBySettingKey(key);

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
        BigDecimal payments = payment.sumAmountByDateReceivedBefore(firstDayOfTargetYear);
        if (payments == null)
            payments = BigDecimal.ZERO;

        // Expenses incurred BEFORE firstDayOfTargetYear
        BigDecimal expenses = expense.sumAmountByDateOfExpenseBefore(firstDayOfTargetYear);
        if (expenses == null)
            expenses = BigDecimal.ZERO;

        return seed.add(payments).subtract(expenses);
    }

    private BigDecimal getSeedAmount() {
        return systemSetting.findBySettingKey(BaselineConstants.KEY_SEED)
                .map(vo -> new BigDecimal(vo.getSettingValue()))
                .orElse(BigDecimal.ZERO);
    }

    private void saveBaselineSnapshot(int year, BigDecimal amount) {
        String key = BaselineConstants.getYearlyBaselineKey(year);
        SystemSettingsVO vo = systemSetting.findBySettingKey(key)
                .orElse(new SystemSettingsVO());

        vo.setSettingType(BaselineConstants.TYPE_BASELINE);
        vo.setSettingKey(key);
        vo.setSettingValue(amount.toString());
        vo.setActive(true);
        vo.setEffectiveDate(LocalDate.of(year, 1, 1));

        if (vo.getSystemSettingsID() == null) {
            systemSetting.add(vo);
        } else {
            systemSetting.update(vo);
        }
        logger.info("Saved baseline snapshot for year {} with value {}", year, amount);
    }
}