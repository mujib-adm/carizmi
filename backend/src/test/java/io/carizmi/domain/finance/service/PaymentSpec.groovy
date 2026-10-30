package io.carizmi.domain.finance.service

import io.carizmi.domain.finance.data.dto.projection.FeeTypeQuarterlyTotalProjection
import io.carizmi.domain.finance.data.dto.projection.PaymentMethodTotalProjection
import io.carizmi.framework.event.DomainEventPublisher
import io.carizmi.shared.constants.ReferenceConstants
import io.carizmi.shared.constants.FieldConstants
import io.carizmi.shared.constants.TableConstants
import io.carizmi.shared.data.dto.QuarterlyTotalProjection
import io.carizmi.shared.data.dto.SortOrder
import io.carizmi.domain.finance.data.dto.response.LatestPaymentDto
import io.carizmi.domain.finance.data.dto.LatestPaymentProjection
import io.carizmi.domain.finance.data.dto.PaymentDto
import io.carizmi.domain.finance.data.dto.request.PaymentSearchRequestDto
import io.carizmi.domain.finance.data.dto.response.PaymentSearchResultDto
import io.carizmi.domain.finance.data.transformer.PaymentDtoTransformer
import io.carizmi.domain.finance.data.transformer.PaymentVOTransformer
import io.carizmi.domain.membership.model.MemberVO
import io.carizmi.domain.finance.model.PaymentVO
import io.carizmi.framework.data.response.SinglePagedResult
import io.carizmi.framework.data.response.PagedResult
import io.carizmi.framework.exception.DuplicateRecordException
import io.carizmi.framework.exception.ValidationException
import io.carizmi.framework.exception.RecordNotFoundException
import io.carizmi.shared.message.ValidationMessages
import io.carizmi.framework.util.MySQLConstraintResolver
import io.carizmi.domain.finance.repository.PaymentRepository
import io.carizmi.domain.finance.validation.PaymentValidator
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

import io.carizmi.shared.data.dto.PaymentSummary

class PaymentSpec extends BaseSpecification {

    PaymentRepository paymentRepo = Mock()
    PaymentVOTransformer voTransformer = Mock()
    PaymentDtoTransformer dtoTransformer = Mock()
    PaymentValidator validator = Mock()
    MySQLConstraintResolver constraintResolver = Mock()
    DomainEventPublisher domainEventPublisher = Mock()

    @Subject
    PaymentImpl paymentImpl = new PaymentImpl(paymentRepo, voTransformer, dtoTransformer, validator)

    void setup() {
        ReflectionTestUtils.setField(paymentImpl, "constraintResolver", constraintResolver)
        ReflectionTestUtils.setField(paymentImpl, "domainEventPublisher", domainEventPublisher)
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
        JpaSpecification capturedSpec

        when: "The target method executed"
        Integer result = paymentImpl.addPayment(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * paymentRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; false }
        1 * paymentRepo.save(vo) >> vo
        1 * domainEventPublisher.publish("CREATED", vo, paymentId)
        0 * _

        and: "The expected result"
        result == 1
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

        when: "The target method executed"
        Integer result = paymentImpl.addPayment(request)

        then: "The expected calls are made"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * paymentRepo.save(vo) >> vo
        1 * domainEventPublisher.publish("CREATED", vo, paymentId)
        0 * _

        and: "The expected result"
        result == 1
        noExceptionThrown()
    }

    def "test - addPayment: Success - Registration Fee publishes DomainEvent via framework"() {
        given: "A registration fee payment request"
        Integer memberId = 99
        Integer paymentId = 15
        LocalDate dateReceived = LocalDate.of(2026, 4, 15)
        String feeType = ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
        PaymentDto request = new PaymentDto(memberID: memberId, feeType: feeType, dateReceived: dateReceived)
        PaymentVO vo = new PaymentVO(paymentID: paymentId, member: new MemberVO(memberID: memberId), feeType: feeType, dateReceived: dateReceived)

        when: "addPayment is executed"
        Integer result = paymentImpl.addPayment(request)

        then: "Registration fee is validated, saved, and DomainEvent is published"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * paymentRepo.exists(_ as JpaSpecification) >> false
        1 * paymentRepo.save(vo) >> vo
        1 * domainEventPublisher.publish("CREATED", vo, paymentId)
        0 * _

        and:
        result == paymentId
        noExceptionThrown()
    }

    def "test - addPayment: Duplicate Registration Fee triggers validation error"() {
        given: "A member who already has a registration fee recorded"
        Integer memberId = 99
        String feeType = ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
        PaymentDto request = new PaymentDto(memberID: memberId, feeType: feeType)
        PaymentVO vo = new PaymentVO(member: new MemberVO(memberID: memberId), feeType: feeType)

        when: "addPayment is executed"
        paymentImpl.addPayment(request)

        then: "Duplicate is detected and ValidationException is thrown"
        1 * voTransformer.transform(request) >> vo
        1 * validator.validate(vo)
        1 * paymentRepo.exists(_ as JpaSpecification) >> true
        0 * _

        and:
        vo.hasErrors()
        thrown(ValidationException)
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

        LatestPaymentProjection projection = Mock(LatestPaymentProjection)

        when: "The target method executed"
        List<LatestPaymentDto> result = paymentImpl.getLatestPayments(5)

        then: "The expected calls are made"
        1 * paymentRepo.findLatestPayments(_ as PageRequest) >> [projection]
        1 * projection.getPaymentID() >> paymentID
        1 * projection.getMemberID() >> memberID
        1 * projection.getMemberName() >> "${firstName} ${lastName}"
        1 * projection.getFeeType() >> feeType
        1 * projection.getAmount() >> amount
        1 * projection.getPaymentDate() >> dateReceived
        0 * _

        and: "The expected result"
        result.size() == 1
        with(result[0]) {
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

        when: "The target method executed"
        List<LatestPaymentDto> result = paymentImpl.getLatestPayments(5)

        then: "The expected calls are made"
        1 * paymentRepo.findLatestPayments(_ as PageRequest) >> []
        0 * _

        and: "The expected result"
        result.isEmpty()
        noExceptionThrown()
    }

    def "test - updatePayment: Success"() {
        given: "An existing payment update request"
        Integer paymentId = 1
        PaymentVO vo = new PaymentVO(feeType: "Other")

        when: "The target method executed"
        paymentImpl.updatePayment(new PaymentDto(paymentID: paymentId))

        then: "The expected calls are made"
        1 * paymentRepo.findById(paymentId) >> Optional.of(vo)
        1 * voTransformer.transformForUpdate(_, _) >> vo
        1 * validator.validateForUpdate(_)
        1 * paymentRepo.save(vo) >> vo
        1 * domainEventPublisher.publish("UPDATED", vo, _)
        and: "The expected result"
        noExceptionThrown()
    }

    def "test - updatePayment: Success - Registration Fee publishes DomainEvent via framework"() {
        given: "An existing registration fee payment update request"
        Integer paymentId = 1
        Integer memberId = 99
        LocalDate newDate = LocalDate.of(2026, 7, 10)
        PaymentDto dto = new PaymentDto(paymentID: paymentId, memberID: memberId, feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, dateReceived: newDate)
        PaymentVO existingVO = new PaymentVO(paymentID: paymentId, member: new MemberVO(memberID: memberId), feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, dateReceived: LocalDate.of(2026, 4, 1))
        PaymentVO updatedVO = new PaymentVO(paymentID: paymentId, member: new MemberVO(memberID: memberId), feeType: ReferenceConstants.FEE_TYPE.REGISTRATION_FEE, dateReceived: newDate)

        when: "updatePayment is executed"
        paymentImpl.updatePayment(dto)

        then: "Payment updated and DomainEvent published with UPDATED"
        1 * paymentRepo.findById(paymentId) >> Optional.of(existingVO)
        1 * voTransformer.transformForUpdate(dto, existingVO) >> updatedVO
        1 * validator.validateForUpdate(updatedVO)
        1 * paymentRepo.exists(_ as JpaSpecification) >> false
        1 * paymentRepo.save(updatedVO) >> updatedVO
        1 * domainEventPublisher.publish("UPDATED", updatedVO, paymentId)
        0 * _

        and:
        noExceptionThrown()
    }

    def "test - updatePayment: Blocked if existing record in closed fiscal year"() {
        given: "An existing payment in a closed year"
        Integer paymentId = 1
        PaymentDto dto = new PaymentDto(paymentID: paymentId)
        PaymentVO existingVO = new PaymentVO(paymentID: paymentId, feeType: "Other")
        PaymentVO updatedVO = new PaymentVO(paymentID: paymentId, feeType: "Other")

        when: "The target method executed"
        paymentImpl.updatePayment(dto)

        then: "Validator marks error and exception is thrown"
        1 * paymentRepo.findById(1) >> Optional.of(existingVO)
        1 * voTransformer.transformForUpdate(dto, existingVO) >> updatedVO
        1 * validator.validateForUpdate(updatedVO) >> { PaymentVO p -> p.addFieldMessage("dateReceived", ValidationMessages.ERR_TRANSACTION_CLOSED_YEAR_MUTATION) }
        0 * _

        and:
        thrown(ValidationException)
    }

    def "test - updatePayment: DB Error"() {
        given: "A DB error during update"
        Integer id = 1
        PaymentDto dto = new PaymentDto(paymentID: id)
        PaymentVO vo = new PaymentVO(paymentID: id, feeType: "Other")

        when: "The target method executed"
        paymentImpl.updatePayment(dto)

        then: "The expected calls are made"
        1 * paymentRepo.findById(id) >> Optional.of(vo)
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
        1 * paymentRepo.findById(paymentId) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - updatePayment: Duplicate Membership Fee"() {
        given: "An update that would create a duplicate membership fee"
        Integer paymentId = 2
        Integer memberId = 1
        int year = LocalDate.now().year
        int quarter = 1
        String feeType = ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
        PaymentDto request = new PaymentDto(paymentID: paymentId, memberID: memberId, feeType: feeType, year: year, quarter: quarter)
        PaymentVO existingVO = new PaymentVO(paymentID: paymentId, member: new MemberVO(memberID: memberId), feeType: feeType, year: year, quarter: 2)
        PaymentVO updatedVO = new PaymentVO(paymentID: paymentId, member: new MemberVO(memberID: memberId), feeType: feeType, year: year, quarter: quarter)
        JpaSpecification capturedSpec

        when: "The target method executed"
        paymentImpl.updatePayment(request)

        then: "The expected calls are made"
        1 * paymentRepo.findById(paymentId) >> Optional.of(existingVO)
        1 * voTransformer.transformForUpdate(request, existingVO) >> updatedVO
        1 * validator.validateForUpdate(updatedVO)
        1 * paymentRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; true }
        0 * _

        and: "VO has validation errors and exception is thrown"
        updatedVO.hasErrors()
        thrown(ValidationException)
        capturedSpec != null

        and: "The specification includes self-exclusion"
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([TableConstants.MEMBER_TABLE, FieldConstants.MEMBER_ID, FieldConstants.FEE_TYPE, FieldConstants.YEAR, FieldConstants.QUARTER, FieldConstants.PAYMENT_ID])
        inspection.values.containsAll([memberId, feeType, year, quarter, paymentId])
    }

    def "test - deletePayment: Success"() {
        given: "An existing payment VO"
        Integer paymentId = 1
        PaymentVO vo = new PaymentVO()

        when: "The target method executed"
        paymentImpl.deletePayment(paymentId)

        then: "The expected calls are made"
        1 * paymentRepo.findById(paymentId) >> Optional.of(vo)
        1 * validator.validateForDelete(vo)
        1 * paymentRepo.delete(vo)
        1 * domainEventPublisher.publish("DELETED", vo, _)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }

    def "test - deletePayment: Blocked if in closed fiscal year"() {
        given: "An existing payment in a closed year"
        Integer paymentId = 1
        PaymentVO vo = new PaymentVO()

        when: "The target method executed"
        paymentImpl.deletePayment(paymentId)

        then: "Validator marks error and exception is thrown"
        1 * paymentRepo.findById(paymentId) >> Optional.of(vo)
        1 * validator.validateForDelete(vo) >> { PaymentVO p -> p.addFieldMessage("dateReceived", ValidationMessages.ERR_TRANSACTION_CLOSED_YEAR_DELETE) }
        0 * _

        and:
        thrown(ValidationException)
    }

    def "test - deletePayment: DB Error"() {
        given: "A DB error during deletion"
        Integer id = 1
        PaymentVO vo = new PaymentVO(paymentID: id)

        when: "The target method executed"
        paymentImpl.deletePayment(id)

        then: "The expected calls are made"
        1 * paymentRepo.findById(id) >> Optional.of(vo)
        1 * validator.validateForDelete(vo)
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
        1 * paymentRepo.findById(paymentId) >> Optional.empty()
        0 * _

        and: "The expected result"
        thrown(RecordNotFoundException)
    }

    def "test - hasPaymentsForMember: Returns correct boolean based on repository"() {
        given: "A target member ID"
        Integer memberId = 123
        JpaSpecification capturedSpec = null

        when: "hasPaymentsForMember is called"
        boolean hasPayments = paymentImpl.hasPaymentsForMember(memberId)

        then: "Repository method is queried"
        1 * paymentRepo.exists(_ as JpaSpecification) >> { JpaSpecification spec -> capturedSpec = spec; expected }
        0 * _

        and:
        hasPayments == expected
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        inspection.filters.containsAll([TableConstants.MEMBER_TABLE, FieldConstants.MEMBER_ID])
        inspection.values.contains(memberId)

        where:
        expected << [true, false]
    }

    def "test - getPayment: Success"() {
        given: "An existing payment ID"
        Integer paymentId = 1
        PaymentVO vo = new PaymentVO(paymentID: paymentId)
        PaymentDto dto = new PaymentDto(paymentID: paymentId)

        when: "The target method executed"
        PaymentDto result = paymentImpl.getPayment(paymentId)

        then: "The expected calls are made"
        1 * paymentRepo.findById(paymentId) >> Optional.of(vo)
        1 * dtoTransformer.transform(vo) >> dto
        0 * _

        and: "The expected result"
        result == dto
        noExceptionThrown()
    }

    def "test - getPayment: Not Found"() {
        given: "A missing payment ID"
        Integer paymentId = 99

        when: "The target method executed"
        paymentImpl.getPayment(paymentId)

        then: "The expected calls are made"
        1 * paymentRepo.findById(paymentId) >> Optional.empty()
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

        PaymentSearchRequestDto request = new PaymentSearchRequestDto(memberID: memberID, feeType: feeType, methodOfPayment: methodOfPayment, year: yearValue, quarter: quarter, dateFrom: dateFrom, dateTo: dateTo)
        request.setPage(0)
        request.setSize(10)
        request.setSortField(FieldConstants.AMOUNT)
        request.setSortOrder(SortOrder.desc)

        when: "The target method executed"
        SinglePagedResult<PaymentSearchResultDto> result = paymentImpl.searchPayments(request)

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
        result != null
        result.data().payments == []
        result.data().summary.totalAmount == BigDecimal.ZERO
        noExceptionThrown()
        capturedSpec != null
        Map<String, List> inspection = inspectSpecification(capturedSpec)
        if (hasFilters) {
            inspection.filters.containsAll([TableConstants.MEMBER_TABLE, FieldConstants.MEMBER_ID, FieldConstants.FEE_TYPE, FieldConstants.METHOD_OF_PAYMENT, FieldConstants.YEAR, FieldConstants.QUARTER, FieldConstants.DATE_RECEIVED])
            inspection.values.containsAll([memberID, feeType, methodOfPayment, currentYear, quarter, dateFrom, dateTo])
        } else {
            inspection.filters.isEmpty()
        }

        where:
        desc          | memberID | feeType | methodOfPayment | hasYearVal | quarter | dateFrom        | dateTo          || hasFilters
        "All filters" | 1        | "FEE"   | "01"            | true       | 1       | LocalDate.now() | LocalDate.now() || true
        "No filters"  | null     | null    | null            | false      | null    | null            | null            || false
    }

    def "test - searchPayments: Should calculate total amount for single-page results from payments list"() {
        given: "A search request and page with 2 payments"
        BigDecimal feeAmount = new BigDecimal("60.00")
        PaymentSearchRequestDto request = new PaymentSearchRequestDto()
        request.setPage(0)
        request.setSize(10)

        Page<PaymentVO> mockPage = Mock(Page)
        PaymentVO vo1 = new PaymentVO(amount: feeAmount)
        PaymentVO vo2 = new PaymentVO(amount: feeAmount)
        PaymentDto dto1 = new PaymentDto(paymentID: 1, amount: feeAmount)
        PaymentDto dto2 = new PaymentDto(paymentID: 2, amount: feeAmount)

        when: "searchPayments is executed"
        SinglePagedResult<PaymentSearchResultDto> result = paymentImpl.searchPayments(request)

        then: "Repository and transformer are invoked"
        1 * paymentRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> mockPage
        1 * mockPage.toList() >> [vo1, vo2]
        1 * dtoTransformer.transformList([vo1, vo2]) >> [dto1, dto2]
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 2
        _ * mockPage.getTotalPages() >> 1
        0 * _

        and: "Total amount is calculated as sum of payments on the page"
        result.data().payments.size() == 2
        result.data().summary.totalAmount == new BigDecimal(feeAmount * 2)
        result.meta().totalRecords == 2
    }

    def "test - searchPayments: Should calculate total amount for multi-page results via repository sumAmountByFilters"() {
        given: "A search request and page indicating multiple pages"
        BigDecimal feeAmount = new BigDecimal("60.00")
        BigDecimal total = new BigDecimal("1200.00")
        PaymentSearchRequestDto request = new PaymentSearchRequestDto()
        request.setPage(0)
        request.setSize(10)

        Page<PaymentVO> mockPage = Mock(Page)
        PaymentVO vo1 = new PaymentVO(amount: feeAmount)
        PaymentVO vo2 = new PaymentVO(amount: feeAmount)
        PaymentDto dto1 = new PaymentDto(paymentID: 1, amount: feeAmount)
        PaymentDto dto2 = new PaymentDto(paymentID: 2, amount: feeAmount)

        when: "searchPayments is executed"
        SinglePagedResult<PaymentSearchResultDto> result = paymentImpl.searchPayments(request)

        then: "Repository findAll and sumAmountByFilters are invoked"
        1 * paymentRepo.findAll(_ as JpaSpecification, _ as PageRequest) >> mockPage
        1 * mockPage.toList() >> [vo1, vo2]
        1 * dtoTransformer.transformList([vo1, vo2]) >> [dto1, dto2]
        _ * mockPage.getNumber() >> 0
        _ * mockPage.getSize() >> 10
        _ * mockPage.getTotalElements() >> 25
        _ * mockPage.getTotalPages() >> 3
        1 * paymentRepo.sumAmountByFilters(null, null, null, null, null, null, null) >> total
        0 * _

        and: "Result contains current page payments and multi-page total amount"
        result.data().payments.size() == 2
        result.data().summary.totalAmount == total
        result.meta().totalRecords == 25
        result.meta().totalPages == 3
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

    @Unroll
    def "test - findMembersPaymentSummaries: Should return summaries scoped to member IDs [size: #expectedList.size()]"() {
        given: "Parameters using dynamic year"
        List<Integer> memberIds = [1, 2, 3]
        String feeType = "FEE"
        int currentYear = LocalDate.now().year
        Integer year = currentYear

        when: "The target method executed"
        List<PaymentSummary> result = paymentImpl.findMembersPaymentSummaries(memberIds, feeType, year)

        then: "The expected calls are made"
        1 * paymentRepo.findMembersPaymentSummaries(memberIds, feeType, year) >> expectedList
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
    def "test findQuarterlyTotals - Should return quarterly totals for year [size: #expectedList.size()]"() {
        given:
        int currentYear = LocalDate.now().year

        when: "The target method executed"
        def result = paymentImpl.findQuarterlyTotals(currentYear)

        then: "The expected calls are made"
        1 * paymentRepo.findQuarterlyTotals(currentYear) >> expectedList
        0 * _

        and: "The expected result"
        result == expectedList
        noExceptionThrown()

        where:
        expectedList << [
                [],
                [Stub(QuarterlyTotalProjection)],
                [Stub(QuarterlyTotalProjection), Stub(QuarterlyTotalProjection)]
        ]
    }

    def "test calculateTotalOverdue - Should return overdue total from repo"() {
        given:
        int currentYear = LocalDate.now().year
        int currentQuarter = 2
        BigDecimal feeAmt = new BigDecimal("60.00")
        String feeType = ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
        String status = ReferenceConstants.MEMBER_STATUS.ACTIVE
        BigDecimal expectedOverdue = new BigDecimal("180.00")

        when: "The target method executed"
        BigDecimal result = paymentImpl.calculateTotalOverdue(currentYear, currentQuarter, feeAmt, feeType, status)

        then: "The expected calls are made"
        1 * paymentRepo.calculateTotalOverdue(currentYear, currentQuarter, feeAmt, feeType, status) >> expectedOverdue
        0 * _

        and: "The expected result"
        result == expectedOverdue
        noExceptionThrown()
    }

    def "test calculateTotalOverdue - Should return zero when no overdue"() {
        given:
        int currentYear = LocalDate.now().year
        int currentQuarter = 1
        BigDecimal feeAmt = new BigDecimal("60.00")
        String feeType = ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE
        String status = ReferenceConstants.MEMBER_STATUS.ACTIVE

        when: "The target method executed"
        BigDecimal result = paymentImpl.calculateTotalOverdue(currentYear, currentQuarter, feeAmt, feeType, status)

        then: "The expected calls are made"
        1 * paymentRepo.calculateTotalOverdue(currentYear, currentQuarter, feeAmt, feeType, status) >> BigDecimal.ZERO
        0 * _

        and: "The expected result"
        result == BigDecimal.ZERO
        noExceptionThrown()
    }

    def "test - findFeeTypeQuarterlyTotals: Should delegate to repo"() {
        given: "A year"
        Integer year = 2026
        def mockProj = Mock(FeeTypeQuarterlyTotalProjection)

        when: "The target method executed"
        def result = paymentImpl.findFeeTypeQuarterlyTotals(year)

        then: "The expected calls are made"
        1 * paymentRepo.findFeeTypeQuarterlyTotals(year) >> [mockProj]
        0 * _

        and: "The expected result"
        result == [mockProj]
        noExceptionThrown()
    }

    def "test - findPaymentMethodTotalsBetween: Should delegate to repo"() {
        given: "A date range"
        LocalDate start = LocalDate.of(2026, 1, 1)
        LocalDate end = LocalDate.of(2026, 12, 31)
        def mockProj = Mock(PaymentMethodTotalProjection)

        when: "The target method executed"
        def result = paymentImpl.findPaymentMethodTotalsBetween(start, end)

        then: "The expected calls are made"
        1 * paymentRepo.findPaymentMethodTotalsBetween(start, end) >> [mockProj]
        0 * _

        and: "The expected result"
        result == [mockProj]
        noExceptionThrown()
    }

    @Unroll
    def "test - findMemberIDsByFeeTypeAndDateReceivedBetween: Should delegate to repo [size: #expectedList.size()]"() {
        given: "Criteria parameters"
        String feeType = ReferenceConstants.FEE_TYPE.REGISTRATION_FEE
        LocalDate start = LocalDate.of(2026, 1, 1)
        LocalDate end = LocalDate.of(2026, 12, 31)

        when: "The target method executed"
        List<Integer> result = paymentImpl.findMemberIDsByFeeTypeAndDateReceivedBetween(feeType, start, end)

        then: "The expected calls are made"
        1 * paymentRepo.findMemberIDsByFeeTypeAndDateReceivedBetween(feeType, start, end) >> expectedList
        0 * _

        and: "The expected result"
        result == expectedList
        noExceptionThrown()

        where:
        expectedList << [
                [],
                [101],
                [101, 102, 105]
        ]
    }
}