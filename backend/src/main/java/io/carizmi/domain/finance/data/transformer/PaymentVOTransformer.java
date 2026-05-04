package io.carizmi.domain.finance.data.transformer;

import io.carizmi.domain.finance.data.dto.PaymentDto;
import io.carizmi.domain.membership.model.MemberVO;
import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.framework.data.transformer.Transformer;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentVOTransformer implements Transformer<PaymentDto, PaymentVO> {

    private final EntityManager entityManager;

    @Override
    public PaymentVO transform(PaymentDto dto) {
        if (dto == null) return null;
        PaymentVO vo = new PaymentVO();
        vo.setFeeType(dto.getFeeType());
        vo.setAmount(dto.getAmount());
        vo.setDateReceived(dto.getDateReceived());
        vo.setMethodOfPayment(dto.getMethodOfPayment());
        vo.setYear(dto.getYear());
        vo.setQuarter(dto.getQuarter());

        if (dto.getMemberID() != null) {
            vo.setMember(entityManager.getReference(MemberVO.class, dto.getMemberID()));
        }
        return vo;
    }

    public PaymentVO transformForUpdate(PaymentDto dto, PaymentVO existing) {
        if (dto == null || existing == null) return existing;
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