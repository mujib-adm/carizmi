package io.carizmi.domain.platform.service

import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import io.carizmi.domain.platform.data.dto.response.DashboardMetricsDto
import io.carizmi.domain.platform.data.dto.response.QuarterlyCollectionDto
import io.carizmi.domain.platform.model.DashboardSnapshotVO
import io.carizmi.domain.platform.service.impl.DashboardServiceImpl
import io.carizmi.shared.constants.QuarterStatus
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject

import java.time.LocalDateTime

class DashboardServiceSpec extends BaseSpecification {

    DashboardSnapshot dashboardSnapshot = Mock()
    DashboardProjector dashboardProjector = Mock()
    ObjectMapper objectMapper = Mock()

    @Subject
    DashboardServiceImpl dashboardService = new DashboardServiceImpl(dashboardSnapshot, dashboardProjector, objectMapper)

    def "test - getMetrics: Should return pre-computed metrics from snapshot"() {
        given: "A pre-computed dashboard snapshot"
        List<QuarterlyCollectionDto> collections = [
                QuarterlyCollectionDto.builder()
                        .quarterLabel("Q1")
                        .collectedAmount(new BigDecimal("180.00"))
                        .percentage(1.0)
                        .status(QuarterStatus.PAST)
                        .build(),
                QuarterlyCollectionDto.builder()
                        .quarterLabel("Q2 (Current)")
                        .collectedAmount(new BigDecimal("60.00"))
                        .percentage(0.3333)
                        .status(QuarterStatus.CURRENT)
                        .build(),
                QuarterlyCollectionDto.builder()
                        .quarterLabel("Q3")
                        .collectedAmount(BigDecimal.ZERO)
                        .percentage(0)
                        .status(QuarterStatus.FUTURE)
                        .build()
        ]

        DashboardSnapshotVO snapshot = new DashboardSnapshotVO(
                id: 1,
                totalActiveMembers: 3L,
                totalRevenue: new BigDecimal("5000.00"),
                duesThisQuarter: new BigDecimal("120.00"),
                overdueTotal: new BigDecimal("30.00"),
                quarterlyFeeAmt: new BigDecimal("60.00"),
                quarterlyCollections: '[]',
                lastProjectedAt: LocalDateTime.now()
        )

        when: "The target method executed"
        DashboardMetricsDto result = dashboardService.getMetrics()

        then: "The expected calls are made"
        1 * dashboardSnapshot.getSnapshot() >> Optional.of(snapshot)
        1 * objectMapper.readValue('[]', _ as TypeReference) >> collections
        0 * _

        and: "The expected result"
        result != null
        result.totalMembers == 3L
        result.totalRevenue == new BigDecimal("5000.00")
        result.duesThisQuarter == new BigDecimal("120.00")
        result.overdueTotal == new BigDecimal("30.00")
        result.quarterlyCollections.size() == 3
        result.quarterlyCollections[0].status == QuarterStatus.PAST
        result.quarterlyCollections[1].status == QuarterStatus.CURRENT
        result.quarterlyCollections[2].status == QuarterStatus.FUTURE
        noExceptionThrown()
    }

    def "test - getMetrics: Handling zero members"() {
        given: "A snapshot with zero members"
        DashboardSnapshotVO snapshot = new DashboardSnapshotVO(
                id: 1,
                totalActiveMembers: 0L,
                totalRevenue: BigDecimal.ZERO,
                duesThisQuarter: BigDecimal.ZERO,
                overdueTotal: BigDecimal.ZERO,
                quarterlyFeeAmt: new BigDecimal("60.00"),
                quarterlyCollections: '[]',
                lastProjectedAt: LocalDateTime.now()
        )

        List<QuarterlyCollectionDto> collections = [
                QuarterlyCollectionDto.builder()
                        .quarterLabel("Q1 (Current)")
                        .collectedAmount(BigDecimal.ZERO)
                        .percentage(0)
                        .status(QuarterStatus.CURRENT)
                        .build()
        ]

        when: "The target method executed"
        DashboardMetricsDto result = dashboardService.getMetrics()

        then: "The expected calls are made"
        1 * dashboardSnapshot.getSnapshot() >> Optional.of(snapshot)
        1 * objectMapper.readValue('[]', _ as TypeReference) >> collections
        0 * _

        and: "The expected result"
        result.duesThisQuarter == BigDecimal.ZERO
        result.totalMembers == 0L
        result.quarterlyCollections[0].percentage == 0.0
        noExceptionThrown()
    }

    def "test - getMetrics: Should trigger initial projection when snapshot not found"() {
        given: "No snapshot exists initially"
        DashboardSnapshotVO snapshot = new DashboardSnapshotVO(
                id: 1,
                totalActiveMembers: 0L,
                totalRevenue: BigDecimal.ZERO,
                duesThisQuarter: BigDecimal.ZERO,
                overdueTotal: BigDecimal.ZERO,
                quarterlyFeeAmt: new BigDecimal("60.00"),
                quarterlyCollections: null,
                lastProjectedAt: LocalDateTime.now()
        )

        when: "The target method executed"
        DashboardMetricsDto result = dashboardService.getMetrics()

        then: "Initial projection is triggered"
        1 * dashboardSnapshot.getSnapshot() >> Optional.empty()
        1 * dashboardProjector.rebuildProjection()
        1 * dashboardSnapshot.getSnapshot() >> Optional.of(snapshot)
        0 * _

        and: "The expected result"
        result != null
        result.totalMembers == 0L
        result.quarterlyCollections.isEmpty()
        noExceptionThrown()
    }

    def "test - getMetrics: Should return empty metrics when snapshot cannot be built"() {
        when: "The target method executed"
        DashboardMetricsDto result = dashboardService.getMetrics()

        then: "Projection is triggered but returns nothing"
        1 * dashboardSnapshot.getSnapshot() >> Optional.empty()
        1 * dashboardProjector.rebuildProjection()
        1 * dashboardSnapshot.getSnapshot() >> Optional.empty()
        0 * _

        and: "The expected result"
        result != null
        result.quarterlyCollections.isEmpty()
        noExceptionThrown()
    }

    def "test - getMetrics: Should rebuild projection when cached snapshot is from a prior year"() {
        given: "A snapshot projected in the previous year"
        DashboardSnapshotVO staleSnapshot = new DashboardSnapshotVO(
                id: 1,
                totalActiveMembers: 5L,
                totalRevenue: new BigDecimal("50000.00"),
                duesThisQuarter: new BigDecimal("300.00"),
                overdueTotal: new BigDecimal("240.00"),
                quarterlyFeeAmt: new BigDecimal("60.00"),
                quarterlyCollections: '[]',
                lastProjectedAt: LocalDateTime.now().minusYears(1)
        )
        DashboardSnapshotVO freshSnapshot = new DashboardSnapshotVO(
                id: 1,
                totalActiveMembers: 4L,
                totalRevenue: new BigDecimal("50165.00"),
                duesThisQuarter: new BigDecimal("240.00"),
                overdueTotal: BigDecimal.ZERO,
                quarterlyFeeAmt: new BigDecimal("60.00"),
                quarterlyCollections: null,
                lastProjectedAt: LocalDateTime.now()
        )

        when: "getMetrics is called in the new year"
        DashboardMetricsDto result = dashboardService.getMetrics()

        then: "Projection rebuild is triggered and fresh snapshot is returned"
        1 * dashboardSnapshot.getSnapshot() >> Optional.of(staleSnapshot)
        1 * dashboardProjector.rebuildProjection()
        1 * dashboardSnapshot.getSnapshot() >> Optional.of(freshSnapshot)
        0 * _

        and:
        result.totalMembers == 4L
        result.totalRevenue == new BigDecimal("50165.00")
        result.overdueTotal == BigDecimal.ZERO
    }

    def "test - getMetrics: Should not rebuild projection when lastProjectedAt is null"() {
        given: "A snapshot with null lastProjectedAt"
        DashboardSnapshotVO snapshot = new DashboardSnapshotVO(
                id: 1,
                totalActiveMembers: 2L,
                totalRevenue: new BigDecimal("1000.00"),
                duesThisQuarter: new BigDecimal("120.00"),
                overdueTotal: BigDecimal.ZERO,
                quarterlyFeeAmt: new BigDecimal("60.00"),
                quarterlyCollections: null,
                lastProjectedAt: null
        )

        when: "getMetrics is called"
        DashboardMetricsDto result = dashboardService.getMetrics()

        then: "Returns snapshot without rebuilding"
        1 * dashboardSnapshot.getSnapshot() >> Optional.of(snapshot)
        0 * _

        and:
        result.totalMembers == 2L
    }
}