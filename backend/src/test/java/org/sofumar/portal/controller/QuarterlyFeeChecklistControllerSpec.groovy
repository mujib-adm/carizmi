package org.sofumar.portal.controller

import org.sofumar.portal.data.dto.request.ChecklistSearchRequestDto
import org.sofumar.portal.data.dto.response.MemberQuarterlyRowDto
import org.sofumar.portal.data.dto.response.QuarterlyChecklistDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.framework.data.response.PaginationMeta
import org.sofumar.portal.framework.data.response.SinglePagedResult
import org.sofumar.portal.service.helper.QuarterlyFeeChecklistService
import org.sofumar.portal.testbase.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class QuarterlyFeeChecklistControllerSpec extends BaseSpecification {

    QuarterlyFeeChecklistService checklistService = Mock()

    @Subject
    QuarterlyFeeChecklistController controller = new QuarterlyFeeChecklistController(checklistService)

    def "test - getQuarterlyChecklist: Should delegate to service and wrap result with pagination"() {
        given: "A checklist search request"
        int year = 2026
        int currentQuarter = 2
        BigDecimal feeAmount = 60.00
        Integer memberID1 = 1
        String memberName1 = "John Doe"
        Integer memberID2 = 2
        String memberName2 = "Jane Smith"
        int page = 0
        int pageSize = 10
        long totalRecords = 2
        int totalPages = 1

        ChecklistSearchRequestDto request = new ChecklistSearchRequestDto(year: year)
        List<MemberQuarterlyRowDto> rows = [
                new MemberQuarterlyRowDto(memberID: memberID1, memberName: memberName1),
                new MemberQuarterlyRowDto(memberID: memberID2, memberName: memberName2)
        ]
        QuarterlyChecklistDto checklistDto = new QuarterlyChecklistDto(
                year: year, currentQuarter: currentQuarter, quarterlyFeeAmount: feeAmount, rows: rows
        )
        PaginationMeta meta = PaginationMeta.of(page, pageSize, totalRecords, totalPages)
        SinglePagedResult<QuarterlyChecklistDto> serviceResult = SinglePagedResult.of(checklistDto, meta)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<QuarterlyChecklistDto>> result = controller.getQuarterlyChecklist(request)

        then: "The expected calls are made"
        1 * checklistService.getQuarterlyChecklist(request) >> serviceResult
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.year == year
        result.body.responseData.currentQuarter == currentQuarter
        result.body.responseData.quarterlyFeeAmount == feeAmount
        result.body.responseData.rows.size() == totalRecords
        result.body.responseData.rows[0].memberName == memberName1
        result.body.responseData.rows[1].memberName == memberName2
        result.body.meta.totalRecords == totalRecords
        result.body.meta.totalPages == totalPages
        result.body.meta.page == page
        result.body.meta.pageSize == pageSize
        noExceptionThrown()
    }

    def "test - getQuarterlyChecklist: Should return empty rows when no members found"() {
        given: "A checklist search request with no matching members"
        int year = 2026
        int currentQuarter = 2
        BigDecimal feeAmount = 60.00

        ChecklistSearchRequestDto request = new ChecklistSearchRequestDto(year: year)
        QuarterlyChecklistDto checklistDto = new QuarterlyChecklistDto(
                year: year, currentQuarter: currentQuarter, quarterlyFeeAmount: feeAmount, rows: []
        )
        PaginationMeta meta = PaginationMeta.of(0, 10, 0, 0)
        SinglePagedResult<QuarterlyChecklistDto> serviceResult = SinglePagedResult.of(checklistDto, meta)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<QuarterlyChecklistDto>> result = controller.getQuarterlyChecklist(request)

        then: "The expected calls are made"
        1 * checklistService.getQuarterlyChecklist(request) >> serviceResult
        0 * _

        and: "The expected result"
        result.statusCode == HttpStatus.OK
        result.body.responseData.year == year
        result.body.responseData.rows.isEmpty()
        result.body.meta.totalRecords == 0
        result.body.meta.totalPages == 0
        noExceptionThrown()
    }

    def "test - getQuarterlyChecklist: Should pass pagination parameters through to service"() {
        given: "A checklist search request with specific pagination"
        int year = 2025
        int currentQuarter = 4
        BigDecimal feeAmount = 50.00
        Integer memberID = 11
        String memberName = "Alice Brown"
        int page = 2
        int pageSize = 5
        long totalRecords = 11
        int totalPages = 3

        ChecklistSearchRequestDto request = new ChecklistSearchRequestDto(year: year, page: page, size: pageSize)
        List<MemberQuarterlyRowDto> rows = [
                new MemberQuarterlyRowDto(memberID: memberID, memberName: memberName)
        ]
        QuarterlyChecklistDto checklistDto = new QuarterlyChecklistDto(
                year: year, currentQuarter: currentQuarter, quarterlyFeeAmount: feeAmount, rows: rows
        )
        PaginationMeta meta = PaginationMeta.of(page, pageSize, totalRecords, totalPages)
        SinglePagedResult<QuarterlyChecklistDto> serviceResult = SinglePagedResult.of(checklistDto, meta)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<QuarterlyChecklistDto>> result = controller.getQuarterlyChecklist(request)

        then: "The expected calls are made"
        1 * checklistService.getQuarterlyChecklist(request) >> serviceResult
        0 * _

        and: "The expected result reflects the requested page"
        result.statusCode == HttpStatus.OK
        result.body.responseData.year == year
        result.body.responseData.rows.size() == 1
        result.body.meta.page == page
        result.body.meta.pageSize == pageSize
        result.body.meta.totalRecords == totalRecords
        result.body.meta.totalPages == totalPages
        noExceptionThrown()
    }

    def "test - getQuarterlyChecklist: Should propagate RuntimeException from service"() {
        given: "A checklist search request that causes a service failure"
        ChecklistSearchRequestDto request = new ChecklistSearchRequestDto(year: 2026)

        when: "The target method executed"
        controller.getQuarterlyChecklist(request)

        then: "The expected calls are made"
        1 * checklistService.getQuarterlyChecklist(request) >> { throw new RuntimeException("Database error") }
        0 * _

        and: "The exception is propagated"
        thrown(RuntimeException)
    }
}