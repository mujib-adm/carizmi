package org.sofumar.portal.core.businesslogic.impl

import org.sofumar.portal.constants.ReferenceConstants
import org.sofumar.portal.constants.FieldConstants
import org.sofumar.portal.constants.TableConstants
import org.sofumar.portal.data.dto.request.SortOrder
import org.sofumar.portal.data.dto.response.LatestPaymentDto
import org.sofumar.portal.data.dto.PaymentDto
import org.sofumar.portal.data.dto.request.PaymentSearchRequestDto
import org.sofumar.portal.data.transformer.PaymentDtoTransformer
import org.sofumar.portal.data.transformer.PaymentVOTransformer
import org.sofumar.portal.core.vo.MemberVO
import org.sofumar.portal.core.vo.PaymentVO
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.exception.DuplicateRecordException
import org.sofumar.portal.framework.exception.ValidationException
import org.sofumar.portal.framework.exception.RecordNotFoundException
import org.sofumar.portal.framework.util.MySQLConstraintResolver
import org.sofumar.portal.core.repo.PaymentRepository
import org.sofumar.portal.service.validation.PaymentValidator
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification as JpaSpecification
import org.springframework.http.ResponseEntity
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDate

import org.sofumar.portal.data.dto.response.PaymentSummary

class PaymentSpec extends BaseSpecification {

    PaymentRepository paymentRepo = Mock()
    PaymentVOTransformer voTransformer = Mock()
    PaymentDtoTransformer dtoTransformer = Mock()
    PaymentValidator validator = Mock()
    MySQLConstraintResolver constraintResolver = Mock()

    @Subject
    PaymentImpl paymentImpl = new PaymentImpl(paymentRepo, voTransformer, dtoTransformer, validator)

    void setup() {
        ReflectionTestUtils.setField(paymentImpl, "constraintResolver", constraintResolver)
    }

    def "test - addPayment: Success - Membership Fee"() {
        given: "A membership fee payment DTO and capture variables"
        Integer memberId = 1
        Integer paymentId = 1
        int year = LocalDate.now().year
        int quarter = 1
        String feeType = ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
        PaymentDto request = new PaymentDto(memberID: memberId, feeType: feeType, year: year, quarter: quarter)
        PaymentVO vo = new PaymentVO(paymentID: paymentId, member: new MemberVO(memberID: memberId), feeType: feeType, year: year, quarter: quarter)
        ResponseEntity<GlobalResponse<Integer>> response
        JpaSpecification capturedSpec

        when: "The target method executed"
        response = paymentImpl.addPayment(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * paymentRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; false }
        1 * paymentRepo.save(vo) >> vo
        0 * _

        and: "The expected result"
        response.body.responseData == 1
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        // Accessing root.get("member").get("memberID") results in both "member" and "memberID" being accessed.
        inspection.filters.containsAll([TableConstants.MEMBER_TABLE, FieldConstants.MEMBER_ID, FieldConstants.FEE_TYPE, FieldConstants.YEAR, FieldConstants.QUARTER])
        inspection.values.containsAll([memberId, feeType, year, quarter])
    }

    def "test - addPayment: Duplicate Membership Fee"() {
        given: "A duplicate membership fee scenario"
        Integer memberId = 1
        int year = LocalDate.now().year
        int quarter = 1
        String feeType = ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
        PaymentDto request = new PaymentDto(memberID: memberId, feeType: feeType, year: year, quarter: quarter)
        PaymentVO vo = new PaymentVO(member: new MemberVO(memberID: memberId), feeType: feeType, year: year, quarter: quarter)
        JpaSpecification capturedSpec
        ResponseEntity<GlobalResponse<Integer>> response

        when: "The target method executed"
        paymentImpl.addPayment(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * paymentRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; true }
        0 * _

        and: "VO has validation errors"
        vo.hasErrors()
        thrown(ValidationException)
        capturedSpec != null

        and: "The expected specification details"
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([TableConstants.MEMBER_TABLE, FieldConstants.MEMBER_ID, FieldConstants.FEE_TYPE, FieldConstants.YEAR, FieldConstants.QUARTER])
        inspection.values.containsAll([memberId, feeType, year, quarter])
    }

    def "test - addPayment: Success - Other Fee"() {
        given: "A non-membership fee payment request"
        Integer memberId = 1
        Integer paymentId = 1
        String feeType = "Other"
        PaymentDto request = new PaymentDto(memberID: memberId, feeType: feeType)
        PaymentVO vo = new PaymentVO(paymentID: paymentId)
        ResponseEntity<GlobalResponse<Integer>> response

        when: "The target method executed"
        response = paymentImpl.addPayment(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * paymentRepo.save(vo) >> vo
        0 * _

        and: "The expected result"
        response.body.responseData == 1
        noExceptionThrown()
    }

    def "test - addPayment: Duplicate Handling"() {
        given: "A duplicate payment scenario"
        PaymentVO vo = new PaymentVO()
        PaymentDto request = new PaymentDto(feeType: "Other")

        when: "The target method executed"
        paymentImpl.addPayment(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * paymentRepo.save(_) >> { throw new DataIntegrityViolationException("Dup", new RuntimeException("Duplicate entry '1' for key 'PRIMARY'")) }
        1 * constraintResolver.resolveFields(_) >> ["paymentID"]
        0 * _

        and: "The expected result"
        thrown(DuplicateRecordException)
    }

    def "test - addPayment: General DB Error"() {
        given: "A DB error scenario"
        PaymentVO vo = new PaymentVO()
        PaymentDto request = new PaymentDto(feeType: "Other")

        when: "The target method executed"
        paymentImpl.addPayment(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(_) >> vo
        1 * validator.validate(_)
        1 * paymentRepo.save(_) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - getLatestPayments: Success"() {
        given: "A list of payments in the repository"
        Integer memberID = 1
        String firstName = "John"
        String lastName = "Doe"
        Integer paymentID = 1
        String feeType = "FEE"
        BigDecimal amount = 100.0
        LocalDate dateReceived = LocalDate.now()

        Page<PaymentVO> mockPage = Mock(Page)
        MemberVO member = new MemberVO(memberID: memberID, firstName: firstName, lastName: lastName)
        PaymentVO payment = new PaymentVO(
                paymentID: paymentID,
                member: member,
                feeType: feeType,
                amount: amount,
                dateReceived: dateReceived
        )
        ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> response

        when: "The target method executed"
        response = paymentImpl.getLatestPayments(5)

        then: "The expected calls are made"
        1 * paymentRepo.findAll(_ as PageRequest) >> mockPage
        1 * mockPage.getContent() >> [payment]
        0 * _

        and: "The expected result"
        response.body.responseData.size() == 1
        with(response.body.responseData[0]) {
            it.paymentID == paymentID
            it.memberID == memberID
            it.memberName == "${firstName} ${lastName}"
            it.feeType == feeType
            it.amount == amount
            it.paymentDate == dateReceived
        }
        noExceptionThrown()
    }

    def "test - getLatestPayments: Handling empty results"() {
        given: "An empty payment repository"
        Page<PaymentVO> mockPage = Mock(Page)
        ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> response

        when: "The target method executed"
        response = paymentImpl.getLatestPayments(5)

        then: "The expected calls are made"
        1 * paymentRepo.findAll(_ as PageRequest) >> mockPage
        1 * mockPage.getContent() >> []
        0 * _

        and: "The expected result"
        response.body.responseData.isEmpty()
        noExceptionThrown()
    }

    def "test - updatePayment: Success"() {
        given: "An existing payment update request"
        Integer paymentId = 1
        PaymentVO vo = new PaymentVO()
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = paymentImpl.updatePayment(new PaymentDto(paymentID: paymentId))

        then: "The expected calls are made"
        1 * paymentRepo.findById(1) >> Optional.of(vo)
        1 * voTransformer.transformForUpdate(_, _) >> vo
        1 * validator.validateForUpdate(_)
        1 * paymentRepo.save(vo) >> vo
        0 * _

        and: "The expected result"
        response.statusCode.value() == 200
        noExceptionThrown()
    }

    def "test - updatePayment: DB Error"() {
        given: "A DB error during update"
        Integer id = 1
        PaymentDto dto = new PaymentDto(paymentID: id)
        PaymentVO vo = new PaymentVO(paymentID: id)

        when: "The target method executed"
        paymentImpl.updatePayment(dto)

        then: "The expected calls are made"
        1 * paymentRepo.findById(1) >> Optional.of(vo)
        1 * voTransformer.transformForUpdate(dto, vo) >> vo
        1 * validator.validateForUpdate(vo)
        1 * paymentRepo.save(vo) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - updatePayment: Not Found"() {
        given: "A missing payment ID for update"
        Integer paymentId = 99

        when: "The target method executed"
        paymentImpl.updatePayment(new PaymentDto(paymentID: paymentId))

        then: "The expected calls are made"
        1 * paymentRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - deletePayment: Success"() {
        given: "An existing payment VO"
        Integer paymentId = 1
        PaymentVO vo = new PaymentVO()
        ResponseEntity<GlobalResponse<Void>> response

        when: "The target method executed"
        response = paymentImpl.deletePayment(paymentId)

        then: "The expected calls are made"
        1 * paymentRepo.findById(1) >> Optional.of(vo)
        1 * paymentRepo.delete(vo)
        0 * _

        and: "The expected result"
        response.statusCode.value() == 200
        noExceptionThrown()
    }

    def "test - deletePayment: DB Error"() {
        given: "A DB error during deletion"
        Integer id = 1
        PaymentVO vo = new PaymentVO(paymentID: id)

        when: "The target method executed"
        paymentImpl.deletePayment(id)

        then: "The expected calls are made"
        1 * paymentRepo.findById(1) >> Optional.of(vo)
        1 * paymentRepo.delete(vo) >> { throw new DataAccessException("error", new RuntimeException("root")) {} }
        0 * _

        and: "The expected result"
        vo.hasErrors()
        noExceptionThrown()
    }

    def "test - deletePayment: Not Found"() {
        given: "A missing payment ID for deletion"
        Integer paymentId = 99

        when: "The target method executed"
        paymentImpl.deletePayment(paymentId)

        then: "The expected calls are made"
        1 * paymentRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - getPayment: Success"() {
        given: "An existing payment ID"
        Integer paymentId = 1
        PaymentVO vo = new PaymentVO(paymentID: paymentId)
        PaymentDto dto = new PaymentDto(paymentID: paymentId)
        ResponseEntity<GlobalResponse<PaymentDto>> response

        when: "The target method executed"
        response = paymentImpl.getPayment(paymentId)

        then: "The expected calls are made"
        1 * paymentRepo.findById(1) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> dto
        0 * _

        and: "The expected result"
        response.body.responseData == dto
        noExceptionThrown()
    }

    def "test - getPayment: Not Found"() {
        given: "A missing payment ID"
        Integer paymentId = 99

        when: "The target method executed"
        paymentImpl.getPayment(paymentId)

        then: "The expected calls are made"
        1 * paymentRepo.findById(99) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    @Unroll
    def "test - searchPayments: Checking filters #desc"() {
        given: "Search params using dynamic current year"
        int currentYear = LocalDate.now().year
        Integer yearValue = hasYearVal ? currentYear : null
        Page<PaymentVO> mockPage = Mock(Page)
        JpaSpecification capturedSpec

        PaymentSearchRequestDto request = new PaymentSearchRequestDto(memberID: memberID, feeType: feeType, year: yearValue, quarter: quarter, dateFrom: dateFrom, dateTo: dateTo)
        request.setPage(0)
        request.setSize(10)
        request.setSortField(FieldConstants.AMOUNT)
        request.setSortOrder(SortOrder.desc)

        when: "The target method executed"
        paymentImpl.searchPayments(request)

        then: "The expected calls are made"
        1 * paymentRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> { JpaSpecification spec, PageRequest page -> capturedSpec = spec; mockPage }
        1 * dtoTransformer.transformList([]) >> []
        1 * mockPage.toList() >> []
        // Metadata calls
        _ * mockPage.getContent() >> []
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 0
        _ * mockPage.getTotalPages() >> 0

        0 * _

        and: "The expected result"
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        if (hasFilters) {
            inspection.filters.containsAll([TableConstants.MEMBER_TABLE, FieldConstants.MEMBER_ID, FieldConstants.FEE_TYPE, FieldConstants.YEAR, FieldConstants.QUARTER, FieldConstants.DATE_RECEIVED])
            inspection.values.containsAll([memberID, feeType, currentYear, quarter, dateFrom, dateTo])
        } else {
            inspection.filters.isEmpty()
        }

        where:
        desc          | memberID | feeType | hasYearVal | quarter | dateFrom        | dateTo          || hasFilters
        "All filters" | 1        | "FEE"   | true       | 1       | LocalDate.now() | LocalDate.now() || true
        "No filters"  | null     | null    | false      | null    | null            | null            || false
    }

    def "test - findPaymentsForMemberQuarter: Should delegate to repo with criteria"() {
        given: "Criteria parameters"
        Integer memberID = 1
        int currentYear = LocalDate.now().year
        Integer year = currentYear
        Integer quarter = 1
        String feeType = "FEE"
        PaymentVO vo = new PaymentVO()
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        List<PaymentVO> result = paymentImpl.findPaymentsForMemberQuarter(memberID, year, quarter, feeType)

        then: "The expected calls are made"
        1 * paymentRepo.findAll(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; [vo] }
        0 * _

        and: "The expected result"
        result == [vo]
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([FieldConstants.MEMBER_ID, FieldConstants.YEAR, FieldConstants.QUARTER, FieldConstants.FEE_TYPE])
        inspection.values.containsAll([memberID, year, quarter, feeType])
        noExceptionThrown()
    }

    def "test - sumAmountByDateReceivedBefore: Should return sum"() {
        given: "A date"
        LocalDate date = LocalDate.now()
        BigDecimal expectedSum = new BigDecimal("100.00")

        when: "The target method executed"
        BigDecimal result = paymentImpl.sumAmountByDateReceivedBefore(date)

        then: "The expected calls are made"
        1 * paymentRepo.sumAmountByDateReceivedBefore(date) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }

    def "test - findPaymentSummaries: Should return list"() {
        given: "Criteria using dynamic year"
        String feeType = "FEE"
        int currentYear = LocalDate.now().year
        Integer year = currentYear
        List<PaymentSummary> summaries = []

        when: "The target method executed"
        List<PaymentSummary> result = paymentImpl.findPaymentSummaries(feeType, year)

        then: "The expected calls are made"
        1 * paymentRepo.findPaymentSummaries(feeType, year) >> summaries
        0 * _

        and: "The expected result"
        result == summaries
        noExceptionThrown()
    }

    def "test - sumAmountByDateReceivedBetween: Should return sum"() {
        given: "Date range"
        LocalDate start = LocalDate.now().minusDays(10)
        LocalDate end = LocalDate.now()
        BigDecimal expectedSum = new BigDecimal("150.00")

        when: "The target method executed"
        BigDecimal result = paymentImpl.sumAmountByDateReceivedBetween(start, end)

        then: "The expected calls are made"
        1 * paymentRepo.sumAmountByDateReceivedBetween(start, end) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }

    def "test - sumAmountByYearAndQuarter: Should return sum"() {
        given: "Year and Quarter using dynamic year"
        int currentYear = LocalDate.now().year
        Integer year = currentYear
        Integer quarter = 2
        BigDecimal expectedSum = new BigDecimal("200.00")

        when: "The target method executed"
        BigDecimal result = paymentImpl.sumAmountByYearAndQuarter(year, quarter)

        then: "The expected calls are made"
        1 * paymentRepo.sumAmountByYearAndQuarter(year, quarter) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }

    def "test - sumAmountByMemberID: Should return sum"() {
        given: "Member ID"
        Integer memberID = 100
        BigDecimal expectedSum = new BigDecimal("300.00")

        when: "The target method executed"
        BigDecimal result = paymentImpl.sumAmountByMemberID(memberID)

        then: "The expected calls are made"
        1 * paymentRepo.sumAmountByMemberID(memberID) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }

    def "test - sumAmountByMemberIDAndYearAndQuarter: Should return sum"() {
        given: "Parameters using dynamic year"
        Integer memberID = 100
        int currentYear = LocalDate.now().year
        Integer year = currentYear
        Integer quarter = 3
        BigDecimal expectedSum = new BigDecimal("400.00")

        when: "The target method executed"
        BigDecimal result = paymentImpl.sumAmountByMemberIDAndYearAndQuarter(memberID, year, quarter)

        then: "The expected calls are made"
        1 * paymentRepo.sumAmountByMemberIDAndYearAndQuarter(memberID, year, quarter) >> expectedSum
        0 * _

        and: "The expected result"
        result == expectedSum
        noExceptionThrown()
    }

    @Unroll
    def "test - findMemberPaymentSummaries: Should return summaries [size: #expectedList.size()]"() {
        given: "Parameters"
        Integer memberID = 100
        String feeType = "FEE"

        when: "The target method executed"
        List<PaymentSummary> result = paymentImpl.findMemberPaymentSummaries(memberID, feeType)

        then: "The expected calls are made"
        1 * paymentRepo.findMemberPaymentSummaries(memberID, feeType) >> expectedList
        0 * _

        and: "The expected result"
        result == expectedList
        noExceptionThrown()

        where:
        expectedList << [
                [],
                [Mock(PaymentSummary)],
                [Mock(PaymentSummary), Mock(PaymentSummary)]
        ]
    }

    @Unroll
    def "test - findPaymentsByCriteria: Should delegate to repo with spec [size: #expectedList.size()]"() {
        given: "A specification"
        JpaSpecification spec = Mock(JpaSpecification)
        JpaSpecification capturedSpec = null

        when: "The target method executed"
        List<PaymentVO> result = paymentImpl.findPaymentsByCriteria(spec)

        then: "The expected calls are made"
        1 * paymentRepo.findAll(_ as JpaSpecification) >> { JpaSpecification s ->
            capturedSpec = s
            expectedList
        }
        0 * _

        and: "The expected result"
        result == expectedList
        capturedSpec == spec
        noExceptionThrown()

        where:
        expectedList << [
                [],
                [new PaymentVO(paymentID: 1)],
                [new PaymentVO(paymentID: 1), new PaymentVO(paymentID: 2)]
        ]
    }
}