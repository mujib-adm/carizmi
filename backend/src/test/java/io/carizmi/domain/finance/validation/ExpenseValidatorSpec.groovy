package io.carizmi.domain.finance.validation

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

    @Subject
    ExpenseValidator expenseValidator = new ExpenseValidator(referenceValidator)

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
        0 * _

        and: "The expected result"
        noExceptionThrown()
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.EXPENSE_ID)
    }
}