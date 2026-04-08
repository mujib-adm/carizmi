package org.sofumar.portal.core.repo.jpaspec;

import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.TableConstants;
import org.sofumar.portal.core.vo.PaymentVO;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import java.time.LocalDate;

public class PaymentSpecifications {

    @NonNull
    public static Specification<PaymentVO> hasMemberID(Integer memberID) {
        return (root, query, cb) -> memberID == null ? null : cb.equal(root.get(TableConstants.MEMBER_TABLE).get(FieldConstants.MEMBER_ID), memberID);
    }

    @NonNull
    public static Specification<PaymentVO> hasFeeType(String feeType) {
        return (root, query, cb) -> feeType == null ? null : cb.equal(root.get(FieldConstants.FEE_TYPE), feeType);
    }

    @NonNull
    public static Specification<PaymentVO> hasYear(Integer year) {
        return (root, query, cb) -> year == null ? null : cb.equal(root.get(FieldConstants.YEAR), year);
    }

    @NonNull
    public static Specification<PaymentVO> hasQuarter(Integer quarter) {
        return (root, query, cb) -> quarter == null ? null : cb.equal(root.get(FieldConstants.QUARTER), quarter);
    }

    @NonNull
    public static Specification<PaymentVO> dateReceivedBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null)
                return null;
            if (from != null && to != null)
                return cb.between(root.get(FieldConstants.DATE_RECEIVED), from, to);
            if (from != null)
                return cb.greaterThanOrEqualTo(root.get(FieldConstants.DATE_RECEIVED), from);
            return cb.lessThanOrEqualTo(root.get(FieldConstants.DATE_RECEIVED), to);
        };
    }

    @NonNull
    public static Specification<PaymentVO> notPaymentId(Integer paymentID) {
        return (root, query, cb) -> {
            if (paymentID == null) {
                return cb.conjunction();
            }
            return cb.notEqual(root.get(FieldConstants.PAYMENT_ID), paymentID);
        };
    }
}