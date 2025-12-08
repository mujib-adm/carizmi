package org.sofumar.portal.service.businesslogic.impl;

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
        int month = asOfDate.getMonthValue();
        String period = switch ((month - 1) / 3) {
            case 0 -> "1st quarter";
            case 1 -> "2nd quarter";
            case 2 -> "3rd quarter";
            default -> "4th quarter";
        };
        LocalDate dueDate = switch (period) {
            case "1st quarter" -> LocalDate.of(asOfDate.getYear(), 1, 15);
            case "2nd quarter" -> LocalDate.of(asOfDate.getYear(), 4, 15);
            case "3rd quarter" -> LocalDate.of(asOfDate.getYear(), 7, 15);
            default -> LocalDate.of(asOfDate.getYear(), 10, 15);
        };

        List<MemberVO> all = members.findAll().stream()
                .filter(m -> "Active".equalsIgnoreCase(m.getStatus()))
                .toList();

        return all.stream().filter(m -> {
            List<PaymentVO> paymentVOList = payments.findAll(PaymentSpecifications.hasMemberId(m.getMemberID())
                    .and(PaymentSpecifications.hasPeriod(period))
                    .and(PaymentSpecifications.hasFeeType("Membership fee"))
            );
            boolean paidOnTime = paymentVOList.stream().anyMatch(p -> !p.getDateReceived().isAfter(dueDate));
            return !paidOnTime && asOfDate.isAfter(dueDate);
        }).toList();
    }
}