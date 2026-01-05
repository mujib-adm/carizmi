package org.sofumar.portal.repo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.sofumar.portal.data.vo.PaymentVO;
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

    @Query("select p.member.memberID as memberID, p.year as year, p.quarter as quarter, sum(p.amount) as totalPaid from PaymentVO p where p.feeType = :feeType and p.year = :year group by p.member.memberID, p.year, p.quarter")
    List<PaymentSummary> findPaymentSummaries(@Param("feeType") String feeType, @Param("year") Integer year);

    interface PaymentSummary {
        Integer getMemberID();

        Integer getYear();

        Integer getQuarter();

        BigDecimal getTotalPaid();
    }
}
//    Page<Payment> findByMemberID(Integer memberID, Pageable pageable);

//    @Query("select p from Payment p where p.feeType = 'Membership fee' and p.period = :period and p.member.memberID = :memberID")
//    List<Payment> findQuarterPayment(@Param("memberID") Integer memberID, @Param("period") String period);

//    @Query("select p from Payment p join p.member m where m.memberID = :memberID and p.period = :period and p.feeType = :feeType")
//    List<Payment> findQuarterPayment(@Param("memberID") Integer memberID, @Param("period") String period, @Param("feeType") String feeType);

//    List<Payment> findByMemberMemberIDAndPeriodAndFeeType(Integer memberID, String period, String feeType);

//
//    @Query("select sum(p.amount) from Payment p where p.dateReceived between :from and :to")
//    BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);