package io.carizmi.domain.membership.service;

import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import io.carizmi.domain.membership.repository.MemberMembershipPeriodRepository;
import io.carizmi.framework.annotation.DomainLogicFor;
import io.carizmi.framework.annotation.RepositoryOwnerFor;
import io.carizmi.framework.bl.AbstractBusinessLogic;

@DomainLogicFor(MemberMembershipPeriodVO.class)
@RepositoryOwnerFor(MemberMembershipPeriodRepository.class)
public abstract sealed class MemberMembershipPeriodAbstractBL extends AbstractBusinessLogic<MemberMembershipPeriodVO, MemberMembershipPeriodRepository> permits MemberMembershipPeriodImpl {

    protected MemberMembershipPeriodAbstractBL(MemberMembershipPeriodRepository repo) {
        super(repo);
    }

    @Override
    protected Integer getId(MemberMembershipPeriodVO vo) {
        return vo.getPeriodID();
    }
}