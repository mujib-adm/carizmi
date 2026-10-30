package io.carizmi.domain.membership.repository.spec;

import io.carizmi.domain.membership.model.MemberMembershipPeriodVO;
import io.carizmi.shared.constants.FieldConstants;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

public class MemberMembershipPeriodSpecifications {

    @NonNull
    public static Specification<MemberMembershipPeriodVO> hasMemberID(Integer memberID) {
        return (root, query, cb) -> memberID == null ? null : cb.equal(root.get(FieldConstants.MEMBER_ID), memberID);
    }
}