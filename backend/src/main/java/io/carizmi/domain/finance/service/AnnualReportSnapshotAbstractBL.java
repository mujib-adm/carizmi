package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.model.AnnualReportSnapshotVO;
import io.carizmi.domain.finance.repository.AnnualReportSnapshotRepository;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.annotation.RepositoryOwnerFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;

@DomainLogicFor(AnnualReportSnapshotVO.class)
@RepositoryOwnerFor(AnnualReportSnapshotRepository.class)
public abstract sealed class AnnualReportSnapshotAbstractBL
        extends AbstractBusinessLogic<AnnualReportSnapshotVO, AnnualReportSnapshotRepository>
        permits AnnualReportSnapshotImpl {

    protected AnnualReportSnapshotAbstractBL(AnnualReportSnapshotRepository repo) {
        super(repo);
    }

    @Override
    protected Integer getId(AnnualReportSnapshotVO vo) {
        return vo.getFiscalYear();
    }
}