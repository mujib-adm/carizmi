package org.sofumar.portal.repo;

import org.sofumar.portal.data.vo.PaymentVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<PaymentVO, Integer>, JpaSpecificationExecutor<PaymentVO> {

//    Page<Payment> findByMemberID(Integer memberID, Pageable pageable);

//    @Query("select p from Payment p where p.feeType = 'Membership fee' and p.period = :period and p.member.memberID = :memberID")
//    List<Payment> findQuarterPayment(@Param("memberID") Integer memberID, @Param("period") String period);

//    @Query("select p from Payment p join p.member m where m.memberID = :memberID and p.period = :period and p.feeType = :feeType")
//    List<Payment> findQuarterPayment(@Param("memberID") Integer memberID, @Param("period") String period, @Param("feeType") String feeType);

//    List<Payment> findByMemberMemberIDAndPeriodAndFeeType(Integer memberID, String period, String feeType);

//
//    @Query("select sum(p.amount) from Payment p where p.dateReceived between :from and :to")
//    BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}