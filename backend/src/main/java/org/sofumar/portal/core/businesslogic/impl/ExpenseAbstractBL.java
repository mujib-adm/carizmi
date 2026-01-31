package org.sofumar.portal.core.businesslogic.impl;

import org.sofumar.portal.core.vo.ExpenseVO;
import org.sofumar.portal.framework.annotation.DomainLogicFor;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.core.repo.ExpenseRepository;

@DomainLogicFor(ExpenseVO.class)
public abstract sealed class ExpenseAbstractBL extends AbstractBusinessLogic<ExpenseVO, ExpenseRepository> permits ExpenseImpl {
    protected ExpenseAbstractBL(ExpenseRepository repo) {
        super(repo);
    }
}