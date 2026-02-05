package org.sofumar.portal.core.businesslogic.impl

import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.data.dto.ExpenseDto
import org.sofumar.portal.data.dto.request.ExpenseSearchRequestDto
import org.sofumar.portal.data.transformer.ExpenseDtoTransformer
import org.sofumar.portal.data.transformer.ExpenseVOTransformer
import org.sofumar.portal.core.vo.ExpenseVO
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.exception.DuplicateRecordException
import org.sofumar.portal.framework.exception.RecordNotFoundException
import org.sofumar.portal.framework.util.MySQLConstraintResolver
import org.sofumar.portal.core.repo.ExpenseRepository
import org.sofumar.portal.service.validation.ExpenseValidator
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.http.ResponseEntity
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject

import java.time.LocalDate

class ExpenseSpec extends BaseSpecification {

    ExpenseRepository expenseRepo = Mock()
    ExpenseVOTransformer voTransformer = Mock()
    ExpenseDtoTransformer dtoTransformer = Mock()
    ExpenseValidator validator = Mock()
    MySQLConstraintResolver constraintResolver = Mock()

    @Subject
    ExpenseImpl expenseService = new ExpenseImpl(expenseRepo, voTransformer, dtoTransformer, validator)

    void setup() {
        ReflectionTestUtils.setField(expenseService, "constraintResolver", constraintResolver)
    }

    def "test - addExpense: Should transform, validate, and save expense"() {
        given: "An expense DTO and setup"
        BigDecimal amount = 100.00
        Integer id = 1
        ExpenseDto requestDto = new ExpenseDto(amount: amount)
        ExpenseVO transformedVo = new ExpenseVO(amount: amount)
        ExpenseVO savedVo = new ExpenseVO(expenseID: id, amount: amount)
        ExpenseDto capturedDto = null
        ExpenseVO capturedVo = null
        ResponseEntity<GlobalResponse<Integer>> response

        when: "The target method executed"
        response = expenseService.addExpense(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> { ExpenseDto dto -> capturedDto = dto; transformedVo }
        1 * validator.validate(transformedVo)
        1 * expenseRepo.save(_) >> { ExpenseVO vo -> capturedVo = vo; savedVo }
        0 * _

        and: "The expected result"
        response.body.responseData == 1
        capturedDto == requestDto
        capturedVo == transformedVo
        noExceptionThrown()
    }

    def "test - addExpense: Should handle DataIntegrityViolationException - Duplicate"() {
        given: "A request that causes a duplicate entry"
        BigDecimal amount = 100.00
        ExpenseDto requestDto = new ExpenseDto(amount: amount)
        ExpenseVO transformedVo = new ExpenseVO(amount: amount)

        when: "The target method executed"
        expenseService.addExpense(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> transformedVo
        1 * validator.validate(_)
        1 * expenseRepo.save(_) >> { throw new DataIntegrityViolationException("Duplicate", new RuntimeException("Duplicate entry '1' for key 'PRIMARY'")) }
        1 * constraintResolver.resolveFields(_) >> ["expenseID"]
        0 * _

        and: "The expected result"
        thrown(DuplicateRecordException)
    }

    def "test - addExpense: Should handle general DataAccessException"() {
        given: "A request that causes a DB error"
        BigDecimal amount = 100.00
        ExpenseDto requestDto = new ExpenseDto(amount: amount)
        ExpenseVO vo = new ExpenseVO(amount: amount)

        when: "The target method executed"
        expenseService.addExpense(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * expenseRepo.save(_) >> { throw new DataAccessException("DB error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - updateExpense: Success"() {
        given: "A valid update request"
        Integer id = 1
        BigDecimal amount = 200.00
        ExpenseDto dto = new ExpenseDto(expenseID: id, amount: amount)
        ExpenseVO existingVo = new ExpenseVO(expenseID: id)
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = expenseService.updateExpense(dto)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(existingVo)
        1 * voTransformer.transformForUpdate(dto, existingVo) >> existingVo
        1 * validator.validateForUpdate(existingVo)
        1 * expenseRepo.save(existingVo) >> existingVo
        0 * _

        and: "The expected result"
        response.statusCode.value() == 200
        noExceptionThrown()
    }

    def "test - updateExpense: Not Found"() {
        given: "A non-existent expense ID"
        Integer id = 99
        ExpenseDto dto = new ExpenseDto(expenseID: id)

        when: "The target method executed"
        expenseService.updateExpense(dto)

        then: "The expected calls are made"
        1 * expenseRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - deleteExpense: Success"() {
        given: "An existing expense VO"
        Integer id = 1
        ExpenseVO vo = new ExpenseVO(expenseID: id)

        when: "The target method executed"
        expenseService.deleteExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(vo)
        1 * expenseRepo.delete(vo)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - deleteExpense: Not Found"() {
        given: "A missing expense ID"
        Integer id = 99

        when: "The target method executed"
        expenseService.deleteExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - deleteExpense: DB Error"() {
        given: "A DB error during deletion"
        Integer id = 1
        ExpenseVO vo = new ExpenseVO(expenseID: id)

        when: "The target method executed"
        expenseService.deleteExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(vo)
        1 * expenseRepo.delete(vo) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - getExpense: Success"() {
        given: "An existing expense ID"
        Integer id = 1
        ExpenseVO vo = new ExpenseVO(expenseID: id)
        ExpenseDto dto = new ExpenseDto(expenseID: id)
        ResponseEntity<GlobalResponse<ExpenseDto>> response

        when: "The target method executed"
        response = expenseService.getExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> dto
        0 * _

        and: "The expected result"
        response.body.responseData == dto
        noExceptionThrown()
    }

    def "test - getExpense: Handling record not found"() {
        given: "A non-existent ID"
        Integer id = 99

        when: "The target method executed"
        expenseService.getExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - searchExpenses: Exhausting logic branches and sorting"() {
        given: "Null parameters and sorting setup"
        Page<ExpenseVO> mockPage = Mock(Page)

        when: "The target method executed"

        ExpenseSearchRequestDto request1 = new ExpenseSearchRequestDto()
        request1.setPage(0)
        request1.setSize(10)
        expenseService.searchExpenses(request1)

        ExpenseSearchRequestDto request2 = new ExpenseSearchRequestDto(category: "Transportation", dateFrom: LocalDate.now(), dateTo: LocalDate.now())
        request2.setPage(0)
        request2.setSize(10)
        request2.setSortField(FieldConstants.AMOUNT)
        request2.setSortOrder("DESC")
        expenseService.searchExpenses(request2)

        then: "The expected calls are made"
        2 * expenseRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> mockPage
        _ * mockPage.toList() >> []
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 0
        _ * mockPage.getTotalPages() >> 0
        2 * dtoTransformer.transformList(_) >> []
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - deleteMultiple: AbstractBusinessLogic branch coverage"() {
        given: "A list of VOs"
        Integer id = 1
        List<ExpenseVO> vos = [new ExpenseVO(expenseID: id)]

        when: "The target method executed"
        expenseService.delete(vos)

        then: "The expected calls are made"
        1 * expenseRepo.deleteAll(vos)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - deleteMultiple: Empty list branch"() {
        given: "An empty list"

        when: "The target method executed"
        expenseService.delete([])

        then: "The expected calls are made"
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - sumAmountByDateOfExpenseBefore: Should return sum"() {
        given: "A date"
        LocalDate date = LocalDate.now()
        BigDecimal expectedSum = new BigDecimal("100.00")

        when: "The target method executed"
        BigDecimal result = expenseService.sumAmountByDateOfExpenseBefore(date)

        then: "The expected calls are made"
        1 * expenseRepo.sumAmountByDateOfExpenseBefore(date) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }

    def "test - sumAmountByDateOfExpenseBetween: Should return sum"() {
        given: "A date range"
        LocalDate start = LocalDate.now().minusDays(10)
        LocalDate end = LocalDate.now()
        BigDecimal expectedSum = new BigDecimal("200.00")

        when: "The target method executed"
        BigDecimal result = expenseService.sumAmountByDateOfExpenseBetween(start, end)

        then: "The expected calls are made"
        1 * expenseRepo.sumAmountByDateOfExpenseBetween(start, end) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }
}