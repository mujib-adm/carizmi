package io.carizmi.domain.finance.validation;

import io.carizmi.domain.finance.model.AnnualReportSnapshotVO;
import io.carizmi.framework.bl.AbstractDomainValidator;
import io.carizmi.shared.constants.FieldConstants;
import org.springframework.stereotype.Service;

@Service
public class AnnualReportSnapshotValidator extends AbstractDomainValidator<AnnualReportSnapshotVO> {

    @Override
    public void validate(AnnualReportSnapshotVO vo) {
        validateRequired(vo, FieldConstants.FISCAL_YEAR, vo.getFiscalYear());
        validateRequired(vo, FieldConstants.REPORT_DATA, vo.getReportData());
    }
}