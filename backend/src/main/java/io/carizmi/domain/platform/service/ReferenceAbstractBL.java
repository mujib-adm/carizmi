package io.carizmi.domain.platform.service;

import io.carizmi.domain.platform.model.ReferenceVO;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;
import io.carizmi.domain.platform.repository.ReferenceRepository;

@DomainLogicFor(ReferenceVO.class)
public abstract sealed class ReferenceAbstractBL extends AbstractBusinessLogic<ReferenceVO, ReferenceRepository> permits ReferenceImpl {
    protected ReferenceAbstractBL(ReferenceRepository repo) {
        super(repo);
    }
}