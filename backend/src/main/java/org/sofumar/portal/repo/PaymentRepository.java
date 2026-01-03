package org.sofumar.portal.repo;

import org.sofumar.portal.data.vo.PaymentVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<PaymentVO, Integer>, JpaSpecificationExecutor<PaymentVO> {

    @org.springframework.data.jpa.repository.Query("select sum(p.amount) from PaymentVO p")
    java.math.BigDecimal sumTotalAmount();

    @org.springframework.data.jpa.repository.Query("select sum(p.amount) from PaymentVO p where p.year = :year and p.quarter = :quarter")
    java.math.BigDecimal sumAmountByYearAndQuarter(
            @org.springframework.data.repository.query.Param("year") Integer year,
            @org.springframework.data.repository.query.Param("quarter") Integer quarter);

    @org.springframework.data.jpa.repository.Query("select p.member.memberID as memberID, p.year as year, p.quarter as quarter, sum(p.amount) as totalPaid from PaymentVO p where p.feeType = :feeType group by p.member.memberID, p.year, p.quarter")
    java.util.List<PaymentSummary> findPaymentSummaries(
            @org.springframework.data.repository.query.Param("feeType") String feeType);

    interface PaymentSummary {
        Integer getMemberID();

        Integer getYear();

        Integer getQuarter();

        java.math.BigDecimal getTotalPaid();
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