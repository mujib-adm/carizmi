package io.carizmi.domain.finance.data.transformer;

import io.carizmi.domain.finance.data.dto.PaymentDto;
import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.framework.data.transformer.Transformer;
import org.springframework.stereotype.Service;

@Service
public class PaymentDtoTransformer implements Transformer<PaymentVO, PaymentDto> {

    @Override
    public PaymentDto transform(PaymentVO vo) {
        if (vo == null) return null;
        return PaymentDto.builder()
                .paymentID(vo.getPaymentID())
                .memberID(vo.getMember() != null ? vo.getMember().getMemberID() : null)
                .memberFullName(vo.getMember() != null ? vo.getMember().getFirstName() + " " + vo.getMember().getLastName() : "")
                .feeType(vo.getFeeType())
                .amount(vo.getAmount())
                .dateReceived(vo.getDateReceived())
                .methodOfPayment(vo.getMethodOfPayment())
                .year(vo.getYear())
                .quarter(vo.getQuarter())
                .build();
    }

}