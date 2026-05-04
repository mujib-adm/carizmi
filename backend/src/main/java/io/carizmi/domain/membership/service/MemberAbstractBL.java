package io.carizmi.domain.membership.service;

import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.annotation.RepositoryOwnerFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;
import io.carizmi.domain.membership.repository.MemberRepository;

@DomainLogicFor(MemberVO.class)
@RepositoryOwnerFor(MemberRepository.class)
public abstract sealed class MemberAbstractBL extends AbstractBusinessLogic<MemberVO, MemberRepository> permits MemberImpl {
    protected MemberAbstractBL(MemberRepository repo) {
        super(repo);
    }

    @Override
    protected Integer getId(MemberVO vo) {
        return vo.getMemberID();
    }

    @Override
    protected boolean publishesDomainEvents() {
        return true;
    }
}