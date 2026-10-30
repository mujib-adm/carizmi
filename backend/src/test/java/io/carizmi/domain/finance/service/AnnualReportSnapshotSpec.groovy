package io.carizmi.domain.finance.service

import io.carizmi.domain.finance.model.AnnualReportSnapshotVO
import io.carizmi.domain.finance.repository.AnnualReportSnapshotRepository
import io.carizmi.domain.finance.validation.AnnualReportSnapshotValidator
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject

import java.time.LocalDate
import java.time.LocalDateTime

class AnnualReportSnapshotSpec extends BaseSpecification {

    AnnualReportSnapshotRepository repo = Mock()
    AnnualReportSnapshotValidator validator = Mock()

    @Subject
    AnnualReportSnapshotImpl annualReportSnapshot = new AnnualReportSnapshotImpl(repo, validator)

    def "test - findByFiscalYear: Returns snapshot when present"() {
        given: "A fiscal year with an existing snapshot"
        int fiscalYear = LocalDate.now().getYear()
        AnnualReportSnapshotVO snapshot = AnnualReportSnapshotVO.builder()
                .fiscalYear(fiscalYear)
                .reportData('{"isClosed":true}')
                .closedAt(LocalDateTime.now())
                .closedBy("SYSTEM_SCHEDULER")
                .build()

        when: "findByFiscalYear is invoked"
        Optional<AnnualReportSnapshotVO> result = annualReportSnapshot.findByFiscalYear(fiscalYear)

        then: "The repository is queried and the snapshot is returned"
        1 * repo.findById(fiscalYear) >> Optional.of(snapshot)
        0 * _

        and:
        result.isPresent()
        result.get().getFiscalYear() == fiscalYear
        result.get().getReportData() == '{"isClosed":true}'
    }

    def "test - findByFiscalYear: Returns empty when not found"() {
        given: "A fiscal year with no snapshot"
        int fiscalYear = LocalDate.now().getYear() + 1

        when: "findByFiscalYear is invoked"
        Optional<AnnualReportSnapshotVO> result = annualReportSnapshot.findByFiscalYear(fiscalYear)

        then: "Repository returns empty"
        1 * repo.findById(fiscalYear) >> Optional.empty()
        0 * _

        and:
        result.isEmpty()
    }

    def "test - existsByFiscalYear: Returns true when snapshot exists"() {
        given: "A fiscal year"
        int fiscalYear = LocalDate.now().getYear()

        when: "existsByFiscalYear is invoked"
        boolean exists = annualReportSnapshot.existsByFiscalYear(fiscalYear)

        then: "Repository returns true"
        1 * repo.existsById(fiscalYear) >> true
        0 * _

        and:
        exists
    }

    def "test - existsByFiscalYear: Returns false when snapshot does not exist"() {
        given: "A fiscal year"
        int fiscalYear = LocalDate.now().getYear() + 1

        when: "existsByFiscalYear is invoked"
        boolean exists = annualReportSnapshot.existsByFiscalYear(fiscalYear)

        then: "Repository returns false"
        1 * repo.existsById(fiscalYear) >> false
        0 * _

        and:
        !exists
    }

    def "test - add executes AbstractBusinessLogic lifecycle"() {
        given: "A snapshot to add"
        int fiscalYear = LocalDate.now().getYear()
        AnnualReportSnapshotVO snapshot = AnnualReportSnapshotVO.builder()
                .fiscalYear(fiscalYear)
                .reportData('{"isClosed":true}')
                .closedAt(LocalDateTime.now())
                .closedBy("SYSTEM_SCHEDULER")
                .build()

        when: "add is invoked"
        AnnualReportSnapshotVO added = annualReportSnapshot.add(snapshot)

        then: "Validator and repository save are invoked via AbstractBusinessLogic"
        1 * validator.validate(snapshot)
        1 * repo.save(snapshot) >> snapshot
        0 * _

        and:
        added == snapshot
        annualReportSnapshot.getId(snapshot) == fiscalYear
        annualReportSnapshot.getLogger() != null
        noExceptionThrown()
    }

    def "test - update executes AbstractBusinessLogic lifecycle"() {
        given: "An existing snapshot to update"
        int fiscalYear = LocalDate.now().getYear()
        AnnualReportSnapshotVO snapshot = AnnualReportSnapshotVO.builder()
                .fiscalYear(fiscalYear)
                .reportData('{"isClosed":true}')
                .closedAt(LocalDateTime.now())
                .closedBy("SYSTEM_SCHEDULER")
                .build()

        when: "update is invoked"
        AnnualReportSnapshotVO updated = annualReportSnapshot.update(snapshot)

        then: "Validator validateForUpdate and repository save are invoked via AbstractBusinessLogic"
        1 * validator.validateForUpdate(snapshot)
        1 * repo.save(snapshot) >> snapshot
        0 * _

        and:
        updated == snapshot
        noExceptionThrown()
    }
}
