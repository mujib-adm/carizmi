package io.carizmi.domain.platform.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.carizmi.domain.platform.constants.BaselineConstants;
import io.carizmi.domain.platform.model.SystemSettingsVO;
import io.carizmi.domain.finance.service.Expense;
import io.carizmi.domain.finance.service.Payment;
import io.carizmi.domain.platform.service.SystemSetting;
import io.carizmi.domain.platform.service.BaselineService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BaselineServiceImpl implements BaselineService {
    private static final Logger logger = LoggerFactory.getLogger(BaselineServiceImpl.class);

    private final SystemSetting systemSetting;
    private final Payment payment;
    private final Expense expense;

    @Override
    @Transactional
    public BigDecimal getBaselineForYear(int year) {
        String key = BaselineConstants.getYearlyBaselineKey(year);

        // 1. Check if snapshot exists
        Optional<SystemSettingsVO> snapshot = systemSetting.findBySettingKey(key);

        if (snapshot.isPresent()) {
            return new BigDecimal(snapshot.get().getSettingValue());
        }

        // 2. Fallback: Dynamic Roll-Forward from prior year
        logger.warn("Baseline snapshot for {} not found. Attempting dynamic roll-forward from year {}.", year, year - 1);

        Optional<SystemSettingsVO> prevSnapshot = systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year - 1));
        if (prevSnapshot.isPresent()) {
            BigDecimal prevBaseline = new BigDecimal(prevSnapshot.get().getSettingValue());
            LocalDate startOfPrevYear = LocalDate.of(year - 1, 1, 1);
            LocalDate endOfPrevYear = LocalDate.of(year - 1, 12, 31);

            BigDecimal payments = payment.sumAmountByDateReceivedBetween(startOfPrevYear, endOfPrevYear);
            if (payments == null) payments = BigDecimal.ZERO;

            BigDecimal expenses = expense.sumAmountByDateOfExpenseBetween(startOfPrevYear, endOfPrevYear);
            if (expenses == null) expenses = BigDecimal.ZERO;

            BigDecimal calculatedBaseline = prevBaseline.add(payments).subtract(expenses);
            saveBaselineSnapshot(year, calculatedBaseline);
            return calculatedBaseline;
        }

        logger.info("No prior baseline found for year {}. Defaulting to ZERO.", year);
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional
    @CacheEvict(value = "systemSettings", allEntries = true)
    public void closeYear(int year) {
        logger.info("Closing year {} and capturing final baseline snapshot for year {}.", year, year + 1);

        // 1. Beginning baseline of the closed year (e.g. YEARLY_BASELINE_2026)
        BigDecimal beginningBalance = getBaselineForYear(year);

        // 2. Receipts strictly within the closed year (Jan 1 to Dec 31)
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        BigDecimal payments = payment.sumAmountByDateReceivedBetween(startOfYear, endOfYear);
        if (payments == null) payments = BigDecimal.ZERO;

        // 3. Disbursements strictly within the closed year (Jan 1 to Dec 31)
        BigDecimal expenses = expense.sumAmountByDateOfExpenseBetween(startOfYear, endOfYear);
        if (expenses == null) expenses = BigDecimal.ZERO;

        // 4. Ending Cash Balance of year Y becomes Beginning Baseline for year Y + 1
        BigDecimal endingBalance = beginningBalance.add(payments).subtract(expenses);

        // 5. Persist snapshot for year Y + 1
        saveBaselineSnapshot(year + 1, endingBalance);
        logger.info("Successfully closed year {}. Saved baseline for year {} with value {}.", year, year + 1, endingBalance);
    }

    private void saveBaselineSnapshot(int year, BigDecimal amount) {
        String key = BaselineConstants.getYearlyBaselineKey(year);
        SystemSettingsVO vo = systemSetting.findBySettingKey(key)
                .orElse(new SystemSettingsVO());

        vo.setSettingName(BaselineConstants.NAME_BASELINE);
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