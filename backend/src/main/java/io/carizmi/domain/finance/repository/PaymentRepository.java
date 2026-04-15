package io.carizmi.domain.finance.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.shared.data.dto.PaymentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<PaymentVO, Integer>, JpaSpecificationExecutor<PaymentVO> {

    @Query("select sum(p.amount) from PaymentVO p where p.dateReceived < :date")
    BigDecimal sumAmountByDateReceivedBefore(@Param("date") LocalDate date);

    @Query("select sum(p.amount) from PaymentVO p where p.dateReceived >= :start and p.dateReceived <= :end")
    BigDecimal sumAmountByDateReceivedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select sum(p.amount) from PaymentVO p where p.year = :year and p.quarter = :quarter")
    BigDecimal sumAmountByYearAndQuarter(@Param("year") Integer year, @Param("quarter") Integer quarter);

    @Query("select sum(p.amount) from PaymentVO p where p.member.memberID = :memberID")
    BigDecimal sumAmountByMemberID(@Param("memberID") Integer memberID);

    @Query("select sum(p.amount) from PaymentVO p where p.member.memberID = :memberID and p.year = :year and p.quarter = :quarter")
    BigDecimal sumAmountByMemberIDAndYearAndQuarter(@Param("memberID") Integer memberID, @Param("year") Integer year, @Param("quarter") Integer quarter);

    @Query("select p.year as year, p.quarter as quarter, sum(p.amount) as totalPaid from PaymentVO p where p.member.memberID = :memberID and p.feeType = :feeType group by p.year, p.quarter")
    List<PaymentSummary> findMemberPaymentSummaries(@Param("memberID") Integer memberID, @Param("feeType") String feeType);

    @Query("select p.member.memberID as memberID, p.year as year, p.quarter as quarter, sum(p.amount) as totalPaid from PaymentVO p where p.member.memberID in :memberIds and p.feeType = :feeType and p.year = :year group by p.member.memberID, p.year, p.quarter")
    List<PaymentSummary> findMembersPaymentSummaries(@Param("memberIds") List<Integer> memberIds, @Param("feeType") String feeType, @Param("year") Integer year);

    @Query("select p.member.memberID as memberID, p.year as year, p.quarter as quarter, sum(p.amount) as totalPaid from PaymentVO p where p.feeType = :feeType and p.year = :year group by p.member.memberID, p.year, p.quarter")
    List<PaymentSummary> findPaymentSummaries(@Param("feeType") String feeType, @Param("year") Integer year);

}