package io.carizmi.domain.identity.service;

import io.carizmi.domain.identity.model.UserVO;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;
import io.carizmi.domain.identity.repository.UserRepository;

@DomainLogicFor(UserVO.class)
public abstract sealed class UserAbstractBL extends AbstractBusinessLogic<UserVO, UserRepository> permits UserImpl {
    protected UserAbstractBL(UserRepository repo) {
        super(repo);
    }
}