package org.sofumar.portal.repo.jpaspec;

import org.sofumar.portal.data.vo.PaymentVO;
import org.springframework.data.jpa.domain.Specification;

public class PaymentSpecifications {

    public static Specification<PaymentVO> hasMemberId(Integer memberID) {
        return (root, query, cb) -> cb.equal(root.get("member").get("memberID"), memberID);
    }

    public static Specification<PaymentVO> hasPeriod(String period) {
        return (root, query, cb) -> cb.equal(root.get("period"), period);
    }

    public static Specification<PaymentVO> hasFeeType(String feeType) {
        return (root, query, cb) -> cb.equal(root.get("feeType"), feeType);
    }
}

