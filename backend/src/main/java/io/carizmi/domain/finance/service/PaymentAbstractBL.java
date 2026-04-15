package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;
import io.carizmi.domain.finance.repository.PaymentRepository;

@DomainLogicFor(PaymentVO.class)
public abstract sealed class PaymentAbstractBL extends AbstractBusinessLogic<PaymentVO, PaymentRepository> permits PaymentImpl {
    protected PaymentAbstractBL(PaymentRepository repo) {
        super(repo);
    }
}