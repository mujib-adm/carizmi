package org.sofumar.portal.core.businesslogic.impl;

import org.sofumar.portal.core.vo.ReferenceVO;
import org.sofumar.portal.framework.annotation.DomainLogicFor;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.core.repo.ReferenceRepository;

@DomainLogicFor(ReferenceVO.class)
public abstract sealed class ReferenceAbstractBL extends AbstractBusinessLogic<ReferenceVO, ReferenceRepository> permits ReferenceImpl {
    protected ReferenceAbstractBL(ReferenceRepository repo) {
        super(repo);
    }
}