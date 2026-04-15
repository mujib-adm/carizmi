package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.model.ExpenseVO;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;
import io.carizmi.domain.finance.repository.ExpenseRepository;

@DomainLogicFor(ExpenseVO.class)
public abstract sealed class ExpenseAbstractBL extends AbstractBusinessLogic<ExpenseVO, ExpenseRepository> permits ExpenseImpl {
    protected ExpenseAbstractBL(ExpenseRepository repo) {
        super(repo);
    }
}