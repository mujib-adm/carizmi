package org.sofumar.portal.core.businesslogic.impl;

import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.framework.annotation.DomainLogicFor;
import org.sofumar.portal.framework.bl.AbstractBusinessLogic;
import org.sofumar.portal.core.repo.MemberRepository;

@DomainLogicFor(MemberVO.class)
public abstract sealed class MemberAbstractBL extends AbstractBusinessLogic<MemberVO, MemberRepository> permits MemberImpl {
    protected MemberAbstractBL(MemberRepository repo) {
        super(repo);
    }
}