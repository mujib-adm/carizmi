package org.sofumar.portal.service.businesslogic;

import org.sofumar.portal.data.dto.LatestPaymentDto;
import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.data.vo.PaymentVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService extends BusinessLogic<PaymentVO> {
    ResponseEntity<GlobalResponse<Integer>> addPayment(PaymentDto requestDto);

    ResponseEntity<GlobalResponse<Void>> updatePayment(PaymentDto requestDto);

    ResponseEntity<GlobalResponse<Void>> deletePayment(Integer paymentID);

    ResponseEntity<GlobalResponse<PaymentDto>> getPayment(Integer paymentID);

    ResponseEntity<GlobalResponse<List<PaymentDto>>> searchPayments(
            Integer memberID, String feeType, Integer year, Integer quarter,
            LocalDate dateFrom, LocalDate dateTo,
            int page, int size, String sortField, String sortOrder);

    ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> getLatestPayments(int limit);
}