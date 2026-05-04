package io.carizmi.domain.finance.service

import io.carizmi.framework.event.DomainEventPublisher
import io.carizmi.shared.constants.FieldConstants
import io.carizmi.domain.finance.data.dto.ExpenseDto
import io.carizmi.domain.finance.data.dto.request.ExpenseSearchRequestDto
import io.carizmi.shared.data.dto.SortOrder
import io.carizmi.domain.finance.data.transformer.ExpenseDtoTransformer
import io.carizmi.domain.finance.data.transformer.ExpenseVOTransformer
import io.carizmi.domain.finance.model.ExpenseVO
import io.carizmi.framework.data.response.PagedResult
import io.carizmi.framework.exception.DuplicateRecordException
import io.carizmi.framework.exception.RecordNotFoundException
import io.carizmi.framework.util.MySQLConstraintResolver
import io.carizmi.domain.finance.repository.ExpenseRepository
import io.carizmi.domain.finance.validation.ExpenseValidator
import io.carizmi.testbase.BaseSpecification
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDate

class ExpenseSpec extends BaseSpecification {

    ExpenseRepository expenseRepo = Mock()
    ExpenseVOTransformer voTransformer = Mock()
    ExpenseDtoTransformer dtoTransformer = Mock()
    ExpenseValidator validator = Mock()
    MySQLConstraintResolver constraintResolver = Mock()
    DomainEventPublisher domainEventPublisher = Mock()

    @Subject
    ExpenseImpl expenseImpl = new ExpenseImpl(expenseRepo, voTransformer, dtoTransformer, validator)

    void setup() {
        ReflectionTestUtils.setField(expenseImpl, "constraintResolver", constraintResolver)
        ReflectionTestUtils.setField(expenseImpl, "domainEventPublisher", domainEventPublisher)
    }

    def "test - addExpense: Success"() {
        given: "A valid expense request"
        Integer id = 1
        ExpenseDto requestDto = new ExpenseDto()
        ExpenseVO vo = new ExpenseVO(expenseID: id)

        when: "The target method executed"
        Integer result = expenseImpl.addExpense(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(requestDto) >> vo
        1 * validator.validate(vo)
        1 * expenseRepo.save(vo) >> vo
        1 * domainEventPublisher.publish("CREATED", vo, id)
        0 * _

        and: "The expected result"
        result == 1
        noExceptionThrown()
    }

    def "test - addExpense: Duplicate Handling"() {
        given: "A duplicate expense scenario"
        ExpenseVO vo = new ExpenseVO()
        ExpenseDto requestDto = new ExpenseDto()

        when: "The target method executed"
        expenseImpl.addExpense(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * expenseRepo.save(_) >> { throw new DataIntegrityViolationException("Dup", new RuntimeException("Duplicate entry '1' for key 'PRIMARY'")) }
        1 * constraintResolver.resolveFields(_) >> ["expenseID"]
        0 * _

        and: "The expected result"
        thrown(DuplicateRecordException)
    }

    def "test - addExpense: General DB Error"() {
        given: "A DB error scenario during add"
        ExpenseVO vo = new ExpenseVO()
        ExpenseDto requestDto = new ExpenseDto()

        when: "The target method executed"
        expenseImpl.addExpense(requestDto)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * expenseRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - updateExpense: Success"() {
        given: "A valid update request"
        Integer id = 1
        ExpenseDto dto = new ExpenseDto(expenseID: id)
        ExpenseVO vo = new ExpenseVO(expenseID: id)

        when: "The target method executed"
        expenseImpl.updateExpense(dto)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(vo)
        1 * voTransformer.transformForUpdate(dto, vo) >> vo
        1 * validator.validateForUpdate(vo)
        1 * expenseRepo.save(vo) >> vo
        1 * domainEventPublisher.publish("UPDATED", vo, id)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - updateExpense: DB Error"() {
        given: "A DB error during update"
        Integer id = 1
        ExpenseDto dto = new ExpenseDto(expenseID: id)
        ExpenseVO vo = new ExpenseVO(expenseID: id)

        when: "The target method executed"
        expenseImpl.updateExpense(dto)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(vo)
        1 * voTransformer.transformForUpdate(dto, vo) >> vo
        1 * validator.validateForUpdate(vo)
        1 * expenseRepo.save(vo) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - updateExpense: Not Found"() {
        given: "A missing expense ID for update"
        Integer id = 99
        ExpenseDto dto = new ExpenseDto(expenseID: id)

        when: "The target method executed"
        expenseImpl.updateExpense(dto)

        then: "The expected calls are made"
        1 * expenseRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - deleteExpense: Success"() {
        given: "An existing expense ID"
        Integer id = 1
        ExpenseVO vo = new ExpenseVO(expenseID: id)

        when: "The target method executed"
        expenseImpl.deleteExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(vo)
        1 * expenseRepo.delete(vo)
        1 * domainEventPublisher.publish("DELETED", vo, id)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - deleteExpense: DB Error"() {
        given: "A DB error during deletion"
        Integer id = 1
        ExpenseVO vo = new ExpenseVO(expenseID: id)

        when: "The target method executed"
        expenseImpl.deleteExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(vo)
        1 * expenseRepo.delete(vo) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - deleteExpense: Not Found"() {
        given: "A missing expense ID for deletion"
        Integer id = 99

        when: "The target method executed"
        expenseImpl.deleteExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - getExpense: Success"() {
        given: "An existing expense ID"
        Integer id = 1
        ExpenseVO vo = new ExpenseVO(expenseID: id)
        ExpenseDto expectedDto = new ExpenseDto(expenseID: id)

        when: "The target method executed"
        ExpenseDto result = expenseImpl.getExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(1) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> expectedDto
        0 * _

        and: "The expected result"
        result == expectedDto
        noExceptionThrown()
    }

    def "test - getExpense: Not Found"() {
        given: "A missing expense ID"
        Integer id = 99

        when: "The target method executed"
        expenseImpl.getExpense(id)

        then: "The expected calls are made"
        1 * expenseRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    @Unroll
    def "test - searchExpenses: Checking filter combinations #desc"() {
        given: "Search parameters and mock page"
        Page<ExpenseVO> mockPage = Mock(Page)
        JpaSpecification capturedSpec

        ExpenseSearchRequestDto request = new ExpenseSearchRequestDto(category: cat, dateFrom: dateFrom, dateTo: dateTo)
        request.setPage(0)
        request.setSize(10)
        request.setSortField(FieldConstants.AMOUNT)
        request.setSortOrder(SortOrder.desc)

        when: "The target method executed"
        PagedResult<ExpenseDto> result = expenseImpl.searchExpenses(request)

        then: "The expected calls are made"
        1 * expenseRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> { JpaSpecification spec, PageRequest page ->
            capturedSpec = spec; return mockPage
        }
        1 * mockPage.toList() >> []
        1 * dtoTransformer.transformList([]) >> []
        // Metadata calls
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 0
        _ * mockPage.getTotalPages() >> 0
        0 * _

        and: "The expected result"
        result != null
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        if (hasFilters) {
            inspection.filters.containsAll([FieldConstants.CATEGORY, FieldConstants.DATE_OF_EXPENSE])
            inspection.values.containsAll([cat, dateFrom, dateTo])
        } else {
            inspection.filters.isEmpty()
        }

        where:
        desc          | cat    | dateFrom        | dateTo          || hasFilters
        "All filters" | "FOOD" | LocalDate.now() | LocalDate.now() || true
        "No filters"  | null   | null            | null            || false
    }

    def "test - sumAmountByDateOfExpenseBetween: Should return sum"() {
        given: "Date range"
        LocalDate start = LocalDate.now().minusDays(10)
        LocalDate end = LocalDate.now()
        BigDecimal expectedSum = new BigDecimal("250.00")

        when: "The target method executed"
        BigDecimal result = expenseImpl.sumAmountByDateOfExpenseBetween(start, end)

        then: "The expected calls are made"
        1 * expenseRepo.sumAmountByDateOfExpenseBetween(start, end) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }

    def "test - sumAmountByDateOfExpenseBefore: Should return sum"() {
        given: "A date"
        LocalDate date = LocalDate.now()
        BigDecimal expectedSum = new BigDecimal("500.00")

        when: "The target method executed"
        BigDecimal result = expenseImpl.sumAmountByDateOfExpenseBefore(date)

        then: "The expected calls are made"
        1 * expenseRepo.sumAmountByDateOfExpenseBefore(date) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }
}