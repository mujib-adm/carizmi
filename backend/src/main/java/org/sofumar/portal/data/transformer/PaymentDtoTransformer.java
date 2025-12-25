package org.sofumar.portal.data.transformer;

import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.data.vo.PaymentVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentDtoTransformer implements Transformer<PaymentVO, PaymentDto> {

    @Override
    public PaymentDto transform(PaymentVO vo) {
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

    public List<PaymentDto> transformList(List<PaymentVO> list) {
        return list.stream().map(this::transform).collect(Collectors.toList());
    }
}