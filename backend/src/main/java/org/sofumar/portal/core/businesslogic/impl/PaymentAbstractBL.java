package org.sofumar.portal.core.businesslogic.impl;

import org.sofumar.portal.core.vo.PaymentVO;
import org.sofumar.portal.framework.annotation.DomainLogicFor;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.core.repo.PaymentRepository;

@DomainLogicFor(PaymentVO.class)
public abstract sealed class PaymentAbstractBL extends AbstractBusinessLogic<PaymentVO, PaymentRepository> permits PaymentImpl {
    protected PaymentAbstractBL(PaymentRepository repo) {
        super(repo);
    }
}