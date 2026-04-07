package org.sofumar.portal.core.businesslogic;

import org.sofumar.portal.data.dto.response.LatestPaymentDto;
import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.data.dto.request.PaymentSearchRequestDto;
import org.sofumar.portal.core.vo.PaymentVO;
import org.sofumar.portal.framework.bl.BusinessLogic;
import org.sofumar.portal.framework.data.response.PagedResult;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.sofumar.portal.data.dto.response.PaymentSummary;
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