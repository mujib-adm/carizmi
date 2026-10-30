package io.carizmi.integration.test.platform

import io.carizmi.domain.finance.model.ExpenseVO
import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.domain.finance.repository.ExpenseRepository
import io.carizmi.domain.finance.repository.PaymentRepository
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.domain.membership.repository.MemberRepository
import io.carizmi.domain.platform.constants.BaselineConstants
import io.carizmi.domain.platform.model.SystemSettingsVO
import io.carizmi.domain.platform.repository.SystemSettingRepository
import io.carizmi.domain.platform.service.BaselineService
import io.carizmi.integration.base.BaseIntegrationSpecification
import io.carizmi.shared.constants.ReferenceConstants
import org.springframework.beans.factory.annotation.Autowired

import java.math.BigDecimal
import java.time.LocalDate

class BaselineServiceITSpec extends BaseIntegrationSpecification {

    @Autowired
    BaselineService baselineService

    @Autowired
    MemberRepository memberRepository

    @Autowired
    PaymentRepository paymentRepository

    @Autowired
    ExpenseRepository expenseRepository

    @Autowired
    SystemSettingRepository systemSettingRepository

    @Autowired(required = false)
    org.springframework.cache.CacheManager cacheManager

    def setup() {
        cleanTestData()
        ensureInauguralBaseline()
    }

    def cleanup() {
        cleanTestData()
    }

    private void clearCaches() {
        if (cacheManager != null) {
            cacheManager.cacheNames.each { name ->
                cacheManager.getCache(name)?.clear()
            }
        }
    }

    private void cleanTestData() {
        paymentRepository.deleteAll()
        expenseRepository.deleteAll()
        memberRepository.deleteAll()
        systemSettingRepository.findAll().each { vo ->
            if (vo.settingKey.startsWith(BaselineConstants.KEY_PREFIX_YEARLY)) {
                systemSettingRepository.delete(vo)
            }
        }
        clearCaches()
    }

    private void ensureInauguralBaseline() {
        String key2026 = BaselineConstants.getYearlyBaselineKey(2026)
        systemSettingRepository.save(new SystemSettingsVO(
                settingName: BaselineConstants.NAME_BASELINE,
                settingKey: key2026,
                settingValue: "55221.68",
                effectiveDate: LocalDate.of(2026, 1, 1),
                active: true
        ))
        clearCaches()
    }

    def "Integration Test: getBaselineForYear returns seeded baseline for inaugural year 2026"() {
        when: "getBaselineForYear is called for 2026"
        BigDecimal baseline2026 = baselineService.getBaselineForYear(2026)

        then: "Retrieves the exact database seed value"
        baseline2026 == new BigDecimal("55221.68")
    }

    def "Integration Test: closeYear(2026) calculates bounded 2026 totals and persists YEARLY_BASELINE_2027"() {
        given: "A registered active member"
        MemberVO member = memberRepository.save(new MemberVO(
                firstName: "Audit",
                lastName: "Tester",
                phone: "612-555-7788",
                email: "audit.tester@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(2026, 1, 1),
                state: "MN"
        ))

        and: "Payments in 2026 totaling 10,000.00"
        paymentRepository.save(new PaymentVO(
                member: member,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("6000.00"),
                dateReceived: LocalDate.of(2026, 3, 15),
                year: 2026, quarter: 1,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))
        paymentRepository.save(new PaymentVO(
                member: member,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("4000.00"),
                dateReceived: LocalDate.of(2026, 9, 20),
                year: 2026, quarter: 3,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))

        and: "Expenses in 2026 totaling 3,200.00"
        expenseRepository.save(new ExpenseVO(
                dateOfExpense: LocalDate.of(2026, 4, 10),
                category: "OFC",
                description: "Office Supplies",
                amount: new BigDecimal("1200.00")
        ))
        expenseRepository.save(new ExpenseVO(
                dateOfExpense: LocalDate.of(2026, 11, 5),
                category: "UTL",
                description: "Utilities",
                amount: new BigDecimal("2000.00")
        ))

        and: "Out-of-bound transactions in 2025 and 2027 to verify strict date isolation"
        paymentRepository.save(new PaymentVO(
                member: member,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("999.00"),
                dateReceived: LocalDate.of(2025, 12, 31),
                year: 2025, quarter: 4,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))
        expenseRepository.save(new ExpenseVO(
                dateOfExpense: LocalDate.of(2027, 1, 15),
                category: "OFC",
                description: "Future Expense",
                amount: new BigDecimal("888.00")
        ))

        when: "closeYear(2026) is executed"
        baselineService.closeYear(2026)

        then: "YEARLY_BASELINE_2027 is saved with exact roll-forward ending balance"
        // 55221.68 (inaugural) + 10000.00 (2026 payments) - 3200.00 (2026 expenses) = 62021.68
        BigDecimal baseline2027 = baselineService.getBaselineForYear(2027)
        baseline2027.compareTo(new BigDecimal("62021.68")) == 0

        and: "Persisted in database with correct metadata"
        SystemSettingsVO dbRecord = systemSettingRepository.findAll().find { it.settingKey == "YEARLY_BASELINE_2027" }
        dbRecord != null
        new BigDecimal(dbRecord.settingValue).compareTo(new BigDecimal("62021.68")) == 0
        dbRecord.effectiveDate == LocalDate.of(2027, 1, 1)
        dbRecord.active
    }

    def "Integration Test: Multi-year chaining from 2026 to 2027 and 2028"() {
        given: "Closing 2026 with no transactions, so 2027 starts with 55221.68"
        baselineService.closeYear(2026)
        assert baselineService.getBaselineForYear(2027).compareTo(new BigDecimal("55221.68")) == 0

        MemberVO member = memberRepository.save(new MemberVO(
                firstName: "MultiYear",
                lastName: "Tester",
                phone: "612-555-9900",
                email: "my.tester@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(2026, 1, 1),
                state: "MN"
        ))

        and: "Transactions in 2027: 8,000.00 receipts, 2,000.00 disbursements"
        paymentRepository.save(new PaymentVO(
                member: member,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("8000.00"),
                dateReceived: LocalDate.of(2027, 5, 1),
                year: 2027, quarter: 2,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))
        expenseRepository.save(new ExpenseVO(
                dateOfExpense: LocalDate.of(2027, 8, 1),
                category: "OFC",
                description: "2027 Event Supplies",
                amount: new BigDecimal("2000.00")
        ))

        when: "closeYear(2027) is executed"
        baselineService.closeYear(2027)

        then: "YEARLY_BASELINE_2028 is created: 55221.68 + 8000.00 - 2000.00 = 61221.68"
        BigDecimal baseline2028 = baselineService.getBaselineForYear(2028)
        baseline2028.compareTo(new BigDecimal("61221.68")) == 0
    }

    def "Integration Test: Re-closing a year is idempotent and updates snapshot in place"() {
        given: "Initial 2026 close"
        baselineService.closeYear(2026)
        BigDecimal initial2027 = baselineService.getBaselineForYear(2027)
        assert initial2027.compareTo(new BigDecimal("55221.68")) == 0

        MemberVO member = memberRepository.save(new MemberVO(
                firstName: "Idempotent",
                lastName: "Tester",
                phone: "612-555-1122",
                email: "idem.tester@test.com",
                status: ReferenceConstants.MEMBER_STATUS.ACTIVE,
                joinDate: LocalDate.of(2026, 1, 1),
                state: "MN"
        ))

        and: "A late-recorded 2026 receipt of 1,500.00"
        paymentRepository.save(new PaymentVO(
                member: member,
                feeType: ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE,
                amount: new BigDecimal("1500.00"),
                dateReceived: LocalDate.of(2026, 12, 30),
                year: 2026, quarter: 4,
                methodOfPayment: ReferenceConstants.PAYMENT_METHOD.CASH
        ))

        when: "closeYear(2026) is re-executed (reclosing/auditing)"
        baselineService.closeYear(2026)

        then: "YEARLY_BASELINE_2027 is updated in place without duplicate record errors"
        BigDecimal updated2027 = baselineService.getBaselineForYear(2027)
        updated2027.compareTo(new BigDecimal("56721.68")) == 0
    }
}
