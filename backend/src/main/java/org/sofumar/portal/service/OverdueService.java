package org.sofumar.portal.service;

import org.sofumar.portal.constants.ReferenceConstants;
import org.sofumar.portal.core.businesslogic.Member;
import org.sofumar.portal.core.businesslogic.Payment;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.core.vo.PaymentVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class OverdueService {
    private final Member members;
    private final Payment payments;

    public OverdueService(Member members, Payment payments) {
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

        List<MemberVO> all = members.findAllActiveMembers();

        return all.stream().filter(m -> {
            List<PaymentVO> paymentVOList = payments.findPaymentsForMemberQuarter(
                    m.getMemberID(), year, quarter, ReferenceConstants.FEE_TYPE.MEMBERSHIP_FEE);
            boolean paidOnTime = paymentVOList.stream().anyMatch(p -> !p.getDateReceived().isAfter(dueDate));
            return !paidOnTime && asOfDate.isAfter(dueDate);
        }).toList();
    }
}