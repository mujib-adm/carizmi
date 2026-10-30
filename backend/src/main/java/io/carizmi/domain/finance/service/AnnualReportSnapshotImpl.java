package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.model.AnnualReportSnapshotVO;
import io.carizmi.domain.finance.repository.AnnualReportSnapshotRepository;
import io.carizmi.domain.finance.validation.AnnualReportSnapshotValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public final class AnnualReportSnapshotImpl extends AnnualReportSnapshotAbstractBL implements AnnualReportSnapshot {

    private static final Logger logger = LoggerFactory.getLogger(AnnualReportSnapshotImpl.class);

    private final AnnualReportSnapshotValidator validator;

    @Autowired
    public AnnualReportSnapshotImpl(AnnualReportSnapshotRepository repo, AnnualReportSnapshotValidator validator) {
        super(repo);
        this.validator = validator;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void performDomainValidation(AnnualReportSnapshotVO vo, boolean isUpdate) {
        if (isUpdate) {
            validator.validateForUpdate(vo);
        } else {
            validator.validate(vo);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnnualReportSnapshotVO> findByFiscalYear(Integer fiscalYear) {
        return getRepo().findById(fiscalYear);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByFiscalYear(Integer fiscalYear) {
        return getRepo().existsById(fiscalYear);
    }
}