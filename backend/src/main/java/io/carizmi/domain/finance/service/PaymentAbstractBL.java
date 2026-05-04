package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.annotation.RepositoryOwnerFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;
import io.carizmi.domain.finance.repository.PaymentRepository;

@DomainLogicFor(PaymentVO.class)
@RepositoryOwnerFor(PaymentRepository.class)
public abstract sealed class PaymentAbstractBL extends AbstractBusinessLogic<PaymentVO, PaymentRepository> permits PaymentImpl {
    protected PaymentAbstractBL(PaymentRepository repo) {
        super(repo);
    }

    @Override
    protected Integer getId(PaymentVO vo) {
        return vo.getPaymentID();
    }

    @Override
    protected boolean publishesDomainEvents() {
        return true;
    }
}