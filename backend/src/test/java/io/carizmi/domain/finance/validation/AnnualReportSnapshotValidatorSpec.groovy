package io.carizmi.domain.finance.validation

import io.carizmi.domain.finance.model.AnnualReportSnapshotVO
import io.carizmi.shared.constants.FieldConstants
import io.carizmi.testbase.BaseSpecification
import spock.lang.Subject

import java.time.LocalDate

class AnnualReportSnapshotValidatorSpec extends BaseSpecification {

    @Subject
    AnnualReportSnapshotValidator validator = new AnnualReportSnapshotValidator()

    def "test - validate: Valid snapshot passes without errors"() {
        given: "A valid snapshot VO"
        int fiscalYear = LocalDate.now().getYear()
        AnnualReportSnapshotVO vo = AnnualReportSnapshotVO.builder()
                .fiscalYear(fiscalYear)
                .reportData('{"isClosed":true}')
                .build()

        when: "validate is executed"
        validator.validate(vo)

        then: "No errors are recorded"
        !vo.hasErrors()
        vo.getTableName() == "annual_report_snapshot"
    }

    def "test - validate: Missing required fields records field messages"() {
        given: "An empty snapshot VO"
        AnnualReportSnapshotVO vo = new AnnualReportSnapshotVO()

        when: "validate is executed"
        validator.validate(vo)

        then: "Field errors are accumulated for fiscalYear and reportData"
        vo.hasErrors()
        vo.getFieldMessages().containsKey(FieldConstants.FISCAL_YEAR)
        vo.getFieldMessages().containsKey(FieldConstants.REPORT_DATA)
    }
}