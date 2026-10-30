package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.model.AnnualReportSnapshotVO;
import io.carizmi.framework.bl.BusinessLogic;

import java.util.Optional;

public interface AnnualReportSnapshot extends BusinessLogic<AnnualReportSnapshotVO> {

    Optional<AnnualReportSnapshotVO> findByFiscalYear(Integer fiscalYear);

    boolean existsByFiscalYear(Integer fiscalYear);
}