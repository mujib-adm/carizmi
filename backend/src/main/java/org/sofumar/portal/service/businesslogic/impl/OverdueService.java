package org.sofumar.portal.service.businesslogic.impl;

import org.sofumar.portal.constants.ReferenceCodeConstants;
import org.sofumar.portal.data.vo.MemberVO;
import org.sofumar.portal.data.vo.PaymentVO;
import org.sofumar.portal.repo.MemberRepository;
import org.sofumar.portal.repo.PaymentRepository;
import org.sofumar.portal.repo.jpaspec.PaymentSpecifications;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class OverdueService {
    private final MemberRepository members;
    private final PaymentRepository payments;

    public OverdueService(MemberRepository members, PaymentRepository payments) {
        this.members = members;
        this.payments = payments;
    }

    public List<MemberVO> overdueMembers(LocalDate asOfDate) {
        // Determine current quarter and due date
        int year = asOfDate.getYear();
        int month = asOfDate.getMonthValue();
        int quarter = ((month - 1) / 3) + 1;

        LocalDate dueDate = switch (quarter) {
            case 1 -> LocalDate.of(year, 1, 15);
            case 2 -> LocalDate.of(year, 4, 15);
            case 3 -> LocalDate.of(year, 7, 15);
            default -> LocalDate.of(year, 10, 15);
        };

        List<MemberVO> all = members.findAll().stream()
                .filter(m -> ReferenceCodeConstants.MEMBER_STATUS.ACTIVE.equalsIgnoreCase(m.getStatus()))
                .toList();

        return all.stream().filter(m -> {
            List<PaymentVO> paymentVOList = payments.findAll(PaymentSpecifications.hasMemberID(m.getMemberID())
                    .and(PaymentSpecifications.hasYear(year))
                    .and(PaymentSpecifications.hasQuarter(quarter))
                    .and(PaymentSpecifications.hasFeeType(ReferenceCodeConstants.FEE_TYPE.MEMBERSHIP_FEE)));
            boolean paidOnTime = paymentVOList.stream().anyMatch(p -> !p.getDateReceived().isAfter(dueDate));
            return !paidOnTime && asOfDate.isAfter(dueDate);
        }).toList();
    }
}