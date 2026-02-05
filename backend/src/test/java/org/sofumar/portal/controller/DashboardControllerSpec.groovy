package org.sofumar.portal.controller

import org.sofumar.portal.data.dto.response.DashboardMetricsDto
import org.sofumar.portal.framework.data.response.GlobalResponse
import org.sofumar.portal.service.helper.DashboardService
import org.sofumar.portal.testsupport.BaseSpecification
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Subject

class DashboardControllerSpec extends BaseSpecification {

    DashboardService dashboardService = Mock()

    @Subject
    DashboardController dashboardController = new DashboardController(dashboardService)

    def "test - getMetrics: Should delegate to dashboard service"() {
        given: "A metrics request"
        ResponseEntity<GlobalResponse<DashboardMetricsDto>> serviceResponse = new ResponseEntity<>(HttpStatus.OK)

        when: "The target method executed"
        ResponseEntity<GlobalResponse<DashboardMetricsDto>> result = dashboardController.getMetrics()

        then: "The expected calls are made"
        1 * dashboardService.getMetrics() >> serviceResponse
        0 * _

        and: "The expected result"
        result == serviceResponse
        result.statusCode == HttpStatus.OK
        noExceptionThrown()
    }
}