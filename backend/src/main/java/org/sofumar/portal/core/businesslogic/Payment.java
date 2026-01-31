package org.sofumar.portal.core.businesslogic;

import org.sofumar.portal.data.dto.LatestPaymentDto;
import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.core.vo.PaymentVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.sofumar.portal.data.dto.PaymentSummary;
import org.springframework.data.jpa.domain.Specification;

public interface Payment extends BusinessLogic<PaymentVO> {
    ResponseEntity<GlobalResponse<Integer>> addPayment(PaymentDto requestDto);

    ResponseEntity<GlobalResponse<Void>> updatePayment(PaymentDto requestDto);

    ResponseEntity<GlobalResponse<Void>> deletePayment(Integer paymentID);

    ResponseEntity<GlobalResponse<PaymentDto>> getPayment(Integer paymentID);

    ResponseEntity<GlobalResponse<List<PaymentDto>>> searchPayments(
            Integer memberID, String feeType, Integer year, Integer quarter,
            LocalDate dateFrom, LocalDate dateTo,
            int page, int size, String sortField, String sortOrder);

    ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> getLatestPayments(int limit);

    BigDecimal sumAmountByDateReceivedBefore(LocalDate date);

    BigDecimal sumAmountByDateReceivedBetween(LocalDate start, LocalDate end);

    BigDecimal sumAmountByYearAndQuarter(Integer year, Integer quarter);

    BigDecimal sumAmountByMemberID(Integer memberID);

    BigDecimal sumAmountByMemberIDAndYearAndQuarter(Integer memberID, Integer year, Integer quarter);

    List<PaymentSummary> findMemberPaymentSummaries(Integer memberID, String feeType);

    List<PaymentSummary> findPaymentSummaries(String feeType, Integer year);

    List<PaymentVO> findPaymentsByCriteria(Specification<PaymentVO> spec);

    List<PaymentVO> findPaymentsForMemberQuarter(Integer memberID, Integer year, Integer quarter, String feeType);
}