package io.carizmi.domain.identity.service;

import io.carizmi.domain.identity.model.UserVO;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.annotation.RepositoryOwnerFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;
import io.carizmi.domain.identity.repository.UserRepository;

@DomainLogicFor(UserVO.class)
@RepositoryOwnerFor(UserRepository.class)
public abstract sealed class UserAbstractBL extends AbstractBusinessLogic<UserVO, UserRepository> permits UserImpl {
    protected UserAbstractBL(UserRepository repo) {
        super(repo);
    }

    @Override
    protected Integer getId(UserVO vo) {
        return vo.getUserID();
    }
}