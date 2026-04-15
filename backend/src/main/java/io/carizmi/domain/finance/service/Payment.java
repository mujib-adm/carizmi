package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.data.dto.response.LatestPaymentDto;
import io.carizmi.domain.finance.data.dto.PaymentDto;
import io.carizmi.domain.finance.data.dto.request.PaymentSearchRequestDto;
import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.framework.bl.BusinessLogic;
import io.carizmi.framework.data.response.PagedResult;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.carizmi.shared.data.dto.PaymentSummary;
import org.springframework.data.jpa.domain.Specification;

public interface Payment extends BusinessLogic<PaymentVO> {
    Integer addPayment(PaymentDto requestDto);

    void updatePayment(PaymentDto requestDto);

    void deletePayment(@NonNull Integer paymentID);

    PaymentDto getPayment(@NonNull Integer paymentID);

    PagedResult<PaymentDto> searchPayments(PaymentSearchRequestDto request);

    List<LatestPaymentDto> getLatestPayments(int limit);

    BigDecimal sumAmountByDateReceivedBefore(LocalDate date);

    BigDecimal sumAmountByDateReceivedBetween(LocalDate start, LocalDate end);

    BigDecimal sumAmountByYearAndQuarter(@NonNull Integer year, @NonNull Integer quarter);

    BigDecimal sumAmountByMemberID(@NonNull Integer memberID);

    BigDecimal sumAmountByMemberIDAndYearAndQuarter(@NonNull Integer memberID, @NonNull Integer year, @NonNull Integer quarter);

    List<PaymentSummary> findMemberPaymentSummaries(@NonNull Integer memberID, String feeType);

    List<PaymentSummary> findMembersPaymentSummaries(List<Integer> memberIds, String feeType, @NonNull Integer year);

    List<PaymentSummary> findPaymentSummaries(String feeType, @NonNull Integer year);

    List<PaymentVO> findPaymentsByCriteria(Specification<PaymentVO> spec);

    List<PaymentVO> findPaymentsForMemberQuarter(@NonNull Integer memberID, @NonNull Integer year, @NonNull Integer quarter, String feeType);
}