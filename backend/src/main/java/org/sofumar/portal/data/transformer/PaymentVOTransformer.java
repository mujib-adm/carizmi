package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.core.vo.MemberVO;
import org.sofumar.portal.core.vo.PaymentVO;
import org.springframework.stereotype.Service;

@Service
public class PaymentVOTransformer implements Transformer<PaymentDto, PaymentVO> {

    @Override
    public PaymentVO transform(PaymentDto dto) {
        PaymentVO vo = new PaymentVO();
        vo.setFeeType(dto.getFeeType());
        vo.setAmount(dto.getAmount());
        vo.setDateReceived(dto.getDateReceived());
        vo.setMethodOfPayment(dto.getMethodOfPayment());
        vo.setYear(dto.getYear());
        vo.setQuarter(dto.getQuarter());

        if (dto.getMemberID() != null) {
            MemberVO member = new MemberVO();
            member.setMemberID(dto.getMemberID());
            vo.setMember(member);
        }
        return vo;
    }

    public PaymentVO transformForUpdate(PaymentDto dto, PaymentVO existing) {
        existing.setFeeType(dto.getFeeType());
        existing.setAmount(dto.getAmount());
        existing.setDateReceived(dto.getDateReceived());
        existing.setMethodOfPayment(dto.getMethodOfPayment());
        existing.setYear(dto.getYear());
        existing.setQuarter(dto.getQuarter());
        // Member usually shouldn't change on update, but if needed logic goes here.
        return existing;
    }
}