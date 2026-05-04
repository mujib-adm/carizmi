package io.carizmi.domain.platform.service

import io.carizmi.domain.platform.model.DashboardSnapshotVO
import io.carizmi.domain.platform.repository.DashboardSnapshotRepository
import io.carizmi.testbase.BaseSpecification

class DashboardSnapshotSpec extends BaseSpecification {

    DashboardSnapshotRepository dashboardSnapshotRepo = Mock()

    DashboardSnapshotImpl snapshotService = new DashboardSnapshotImpl(dashboardSnapshotRepo)

    def "test getSnapshot - Should return snapshot when present"() {
        given:
        DashboardSnapshotVO snapshot = new DashboardSnapshotVO(id: 1, totalActiveMembers: 5L)

        when: "The target method executed"
        Optional<DashboardSnapshotVO> result = snapshotService.getSnapshot()

        then: "The expected calls are made"
        1 * dashboardSnapshotRepo.findById(1) >> Optional.of(snapshot)
        0 * _

        and: "The expected result"
        result.isPresent()
        result.get().totalActiveMembers == 5L
        noExceptionThrown()
    }

    def "test getSnapshot - Should return empty when no snapshot exists"() {
        when: "The target method executed"
        Optional<DashboardSnapshotVO> result = snapshotService.getSnapshot()

        then: "The expected calls are made"
        1 * dashboardSnapshotRepo.findById(1) >> Optional.empty()
        0 * _

        and: "The expected result"
        result.isEmpty()
        noExceptionThrown()
    }

    def "test saveSnapshot - Should persist the snapshot"() {
        given: "A snapshot to save"
        DashboardSnapshotVO snapshot = new DashboardSnapshotVO(id: 1, totalActiveMembers: 3L)

        when: "The target method executed"
        snapshotService.saveSnapshot(snapshot)

        then: "The expected calls are made"
        1 * dashboardSnapshotRepo.save(snapshot)
        0 * _

        and: "The expected result"
        noExceptionThrown()
    }
}