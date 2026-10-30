package io.carizmi.domain.finance.validation

import io.carizmi.domain.platform.constants.BaselineConstants
import io.carizmi.domain.platform.model.SystemSettingsVO
import io.carizmi.domain.platform.service.SystemSetting
import io.carizmi.domain.platform.validation.ReferenceValidator

import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.domain.finance.model.ExpenseVO
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject
import spock.lang.Unroll
import java.time.LocalDate

class ExpenseValidatorSpec extends BaseSpecification {

    ReferenceValidator referenceValidator = Mock()
    SystemSetting systemSetting = Mock()

    @Subject
    ExpenseValidator expenseValidator = new ExpenseValidator(referenceValidator, systemSetting)

    def "test - validate: Should pass for valid VO"() {
        given: "A valid ExpenseVO"
        LocalDate date = LocalDate.now()
        String category = ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES
        String description = "Paper"
        BigDecimal amount = 50.0
        ExpenseVO vo = new ExpenseVO(dateOfExpense: date, category: category, description: description, amount: amount)

        String fieldName = FieldConstants.CATEGORY
        String referenceName = ReferenceConstants.EXPENSE_CATEGORY.NAME

        when: "The target method executed"
        expenseValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, fieldName, referenceName, category)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(date.year + 1)) >> Optional.empty()
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validate: With errors"() {
        given: "An invalid ExpenseVO (missing description)"
        String category = ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES
        ExpenseVO vo = new ExpenseVO(dateOfExpense: LocalDate.now(), category: category, amount: 50.0)

        String fieldName = FieldConstants.CATEGORY
        String referenceName = ReferenceConstants.EXPENSE_CATEGORY.NAME

        when: "The target method executed"
        expenseValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, fieldName, referenceName, category)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        0 * _

        and: "VO has validation errors"
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.DESCRIPTION)
        noExceptionThrown()
    }

    @Unroll
    def "test - validate: Handling field validations [field: #field, value: #value, isValid: #isValid]"() {
        given: "An ExpenseVO with a specific field variation"
        String defaultCategory = ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES
        ExpenseVO vo = new ExpenseVO(
                dateOfExpense: (LocalDate) (field == FieldConstants.DATE_OF_EXPENSE ? value : LocalDate.now()),
                category: field == FieldConstants.CATEGORY ? value : defaultCategory,
                description: field == FieldConstants.DESCRIPTION ? value : "Desc",
                amount: (BigDecimal) (field == FieldConstants.AMOUNT ? value : 10.0G)
        )

        String fieldName = FieldConstants.CATEGORY
        String referenceName = ReferenceConstants.EXPENSE_CATEGORY.NAME

        when: "The target method executed"
        expenseValidator.validate(vo)

        then: "The expected calls are made"
        if (field != fieldName || value) {
            String arg = (field == fieldName) ? value : defaultCategory
            1 * referenceValidator.validate(vo, fieldName, referenceName, arg)
        }
        if (vo.getDateOfExpense() != null) {
            1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        }
        0 * _

        and: "The expected result"
        if (isValid) {
            !vo.hasErrors()
        } else {
            vo.hasErrors()
            vo.getFieldMessages().containsKey(field)
        }
        noExceptionThrown()

        where:
        field                          | value           | isValid
        FieldConstants.DATE_OF_EXPENSE | null            | false
        FieldConstants.CATEGORY        | null            | false
        FieldConstants.CATEGORY        | ""              | false
        FieldConstants.DESCRIPTION     | null            | false
        FieldConstants.DESCRIPTION     | ""              | false
        FieldConstants.AMOUNT          | null            | false
        FieldConstants.DATE_OF_EXPENSE | LocalDate.now() | true
    }

    def "test - validateForUpdate: Should validate expenseID and call validate"() {
        given: "An ExpenseVO for update"
        Integer expenseID = 1
        String category = ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES
        ExpenseVO vo = new ExpenseVO(expenseID: expenseID, dateOfExpense: LocalDate.now(), category: category, description: "Desc", amount: 10.0)

        String fieldName = FieldConstants.CATEGORY
        String referenceName = ReferenceConstants.EXPENSE_CATEGORY.NAME

        when: "The target method executed"
        expenseValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, fieldName, referenceName, category)
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        0 * _

        and: "The expected result"
        !vo.hasErrors()
        noExceptionThrown()
    }

    def "test - validateForUpdate: Should catch missing expenseID"() {
        given: "An ExpenseVO for update without ID"
        String category = ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES
        ExpenseVO vo = new ExpenseVO(dateOfExpense: LocalDate.now(), category: category, description: "Desc", amount: 10.0)

        String fieldName = FieldConstants.CATEGORY
        String referenceName = ReferenceConstants.EXPENSE_CATEGORY.NAME

        when: "The target method executed"
        expenseValidator.validateForUpdate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, fieldName, referenceName, category)
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(LocalDate.now().year + 1)) >> Optional.empty()
        0 * _

        and: "The expected result"
        noExceptionThrown()
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.EXPENSE_ID)
    }

    def "test - validate: Fails if dateOfExpense is in a closed fiscal year"() {
        given:
        int year = LocalDate.now().year
        ExpenseVO vo = new ExpenseVO(
                category: ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES,
                description: "Paper",
                amount: new BigDecimal("50.00"),
                dateOfExpense: LocalDate.of(year, 6, 15)
        )

        when: "Validation is executed"
        expenseValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, FieldConstants.CATEGORY, ReferenceConstants.EXPENSE_CATEGORY.NAME, ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.of(new SystemSettingsVO())
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.DATE_OF_EXPENSE)
    }

    def "test - validate: Passes if dateOfExpense is in an open fiscal year"() {
        given:
        int year = LocalDate.now().year
        ExpenseVO vo = new ExpenseVO(
                category: ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES,
                description: "Paper",
                amount: new BigDecimal("50.00"),
                dateOfExpense: LocalDate.of(year + 1, 2, 10)
        )

        when: "Validation is executed"
        expenseValidator.validate(vo)

        then: "The expected calls are made"
        1 * referenceValidator.validate(vo, FieldConstants.CATEGORY, ReferenceConstants.EXPENSE_CATEGORY.NAME, ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES)
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 2)) >> Optional.empty()
        0 * _

        and:
        !vo.getFieldMessages().containsKey(FieldConstants.DATE_OF_EXPENSE)
    }

    def "test - validateForDelete: Fails if expense is in a closed fiscal year"() {
        given:
        int year = 2025
        ExpenseVO vo = new ExpenseVO(expenseID: 5, dateOfExpense: LocalDate.of(year, 3, 15))

        when: "validateForDelete is called"
        expenseValidator.validateForDelete(vo)

        then: "Error is attached to dateOfExpense"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.of(new SystemSettingsVO())
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.DATE_OF_EXPENSE)
    }

    def "test - validateForDelete: Passes if expense is in an open fiscal year"() {
        given:
        int year = 2026
        ExpenseVO vo = new ExpenseVO(expenseID: 5, dateOfExpense: LocalDate.of(year, 3, 15))

        when: "validateForDelete is called"
        expenseValidator.validateForDelete(vo)

        then: "No error is attached"
        1 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        0 * _

        and:
        !vo.hasErrors()
    }

    def "test - validateForUpdate: Fails if expense is in a closed fiscal year"() {
        given:
        int year = 2025
        ExpenseVO vo = new ExpenseVO(expenseID: 5, dateOfExpense: LocalDate.of(year, 3, 15))

        when: "validateForUpdate is called"
        expenseValidator.validateForUpdate(vo)

        then: "Error is attached to dateOfExpense"
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.of(new SystemSettingsVO())
        0 * _

        and:
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.DATE_OF_EXPENSE)
    }

    def "test - validateForUpdate: Passes if expense is in an open fiscal year"() {
        given:
        int year = 2026
        String category = ReferenceConstants.EXPENSE_CATEGORY.OFFICE_SUPPLIES
        ExpenseVO vo = new ExpenseVO(expenseID: 5, dateOfExpense: LocalDate.of(year, 3, 15), category: category, description: "Desc", amount: 10.0)

        when: "validateForUpdate is called"
        expenseValidator.validateForUpdate(vo)

        then: "No error is attached"
        2 * systemSetting.findBySettingKey(BaselineConstants.getYearlyBaselineKey(year + 1)) >> Optional.empty()
        1 * referenceValidator.validate(vo, FieldConstants.CATEGORY, ReferenceConstants.EXPENSE_CATEGORY.NAME, category)
        0 * _

        and:
        !vo.hasErrors()
    }
}