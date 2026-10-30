package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.data.dto.response.LatestPaymentDto;
import io.carizmi.domain.finance.data.dto.PaymentDto;
import io.carizmi.domain.finance.data.dto.projection.FeeTypeReceiptBreakdownProjection;
import io.carizmi.domain.finance.data.dto.projection.FeeTypeQuarterlyTotalProjection;
import io.carizmi.domain.finance.data.dto.projection.PaymentMethodTotalProjection;
import io.carizmi.domain.finance.data.dto.request.PaymentSearchRequestDto;
import io.carizmi.domain.finance.data.dto.response.PaymentSearchResultDto;
import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.framework.bl.BusinessLogic;
import io.carizmi.framework.data.response.SinglePagedResult;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.carizmi.shared.data.dto.PaymentSummary;
import io.carizmi.shared.data.dto.QuarterlyTotalProjection;
import org.springframework.data.jpa.domain.Specification;

public interface Payment extends BusinessLogic<PaymentVO> {
    Integer addPayment(PaymentDto requestDto);

    void updatePayment(PaymentDto requestDto);

    void deletePayment(@NonNull Integer paymentID);

    PaymentDto getPayment(@NonNull Integer paymentID);

    SinglePagedResult<PaymentSearchResultDto> searchPayments(PaymentSearchRequestDto request);

    List<LatestPaymentDto> getLatestPayments(int limit);

    BigDecimal sumAmountByDateReceivedBetween(LocalDate start, LocalDate end);

    BigDecimal sumAmountByYearAndQuarter(@NonNull Integer year, @NonNull Integer quarter);

    BigDecimal sumAmountByMemberID(@NonNull Integer memberID);

    boolean hasPaymentsForMember(@NonNull Integer memberID);

    BigDecimal sumAmountByMemberIDAndYearAndQuarter(@NonNull Integer memberID, @NonNull Integer year, @NonNull Integer quarter);

    List<PaymentSummary> findMemberPaymentSummaries(@NonNull Integer memberID, String feeType);

    List<PaymentSummary> findMembersPaymentSummaries(List<Integer> memberIds, String feeType, @NonNull Integer year);

    List<PaymentSummary> findPaymentSummaries(String feeType, @NonNull Integer year);

    List<QuarterlyTotalProjection> findQuarterlyTotals(@NonNull Integer year);

    List<FeeTypeQuarterlyTotalProjection> findFeeTypeQuarterlyTotals(@NonNull Integer year);

    List<FeeTypeReceiptBreakdownProjection> findFeeTypeReceiptBreakdownsBetween(@NonNull LocalDate start, @NonNull LocalDate end);

    List<PaymentMethodTotalProjection> findPaymentMethodTotalsBetween(@NonNull LocalDate start, @NonNull LocalDate end);

    List<Integer> findMemberIDsByFeeTypeAndDateReceivedBetween(@NonNull String feeType, @NonNull LocalDate start, @NonNull LocalDate end);

    BigDecimal calculateTotalOverdue(@NonNull Integer year, @NonNull Integer currentQuarter,
                                     @NonNull BigDecimal feeAmt, @NonNull String feeType,
                                     @NonNull String status);

    List<PaymentVO> findPaymentsByCriteria(Specification<PaymentVO> spec);

    List<PaymentVO> findPaymentsForMemberQuarter(@NonNull Integer memberID, @NonNull Integer year, @NonNull Integer quarter, String feeType);
}