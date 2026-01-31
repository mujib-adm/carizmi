package org.sofumar.portal.core.businesslogic.impl;

import org.sofumar.portal.core.vo.UserVO;
import org.sofumar.portal.framework.annotation.DomainLogicFor;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.core.repo.UserRepository;

@DomainLogicFor(UserVO.class)
public abstract sealed class UserAbstractBL extends AbstractBusinessLogic<UserVO, UserRepository> permits UserImpl {
    protected UserAbstractBL(UserRepository repo) {
        super(repo);
    }
}