package io.carizmi.domain.finance.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.carizmi.domain.finance.model.PaymentVO;
import io.carizmi.domain.finance.data.dto.LatestPaymentProjection;
import io.carizmi.domain.finance.data.dto.projection.FeeTypeQuarterlyTotalProjection;
import io.carizmi.domain.finance.data.dto.projection.PaymentMethodTotalProjection;
import io.carizmi.shared.data.dto.PaymentSummary;
import io.carizmi.shared.data.dto.QuarterlyTotalProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<PaymentVO, Integer>, JpaSpecificationExecutor<PaymentVO> {

    @Query("SELECT p.paymentID as paymentID, m.memberID as memberID, " +
            "CONCAT(m.firstName, ' ', m.lastName) as memberName, " +
            "p.feeType as feeType, p.amount as amount, p.dateReceived as paymentDate " +
            "FROM PaymentVO p JOIN p.member m " +
            "ORDER BY p.dateReceived DESC, p.paymentID DESC")
    List<LatestPaymentProjection> findLatestPayments(Pageable pageable);

    @Query("select coalesce(sum(p.amount), 0) from PaymentVO p where p.dateReceived >= :start and p.dateReceived <= :end")
    BigDecimal sumAmountByDateReceivedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("select coalesce(sum(p.amount), 0) from PaymentVO p where p.year = :year and p.quarter = :quarter")
    BigDecimal sumAmountByYearAndQuarter(@Param("year") Integer year, @Param("quarter") Integer quarter);

    @Query("select coalesce(sum(p.amount), 0) from PaymentVO p where p.member.memberID = :memberID")
    BigDecimal sumAmountByMemberID(@Param("memberID") Integer memberID);

    @Query("select coalesce(sum(p.amount), 0) from PaymentVO p where p.member.memberID = :memberID and p.year = :year and p.quarter = :quarter")
    BigDecimal sumAmountByMemberIDAndYearAndQuarter(@Param("memberID") Integer memberID, @Param("year") Integer year, @Param("quarter") Integer quarter);

    @Query("select coalesce(sum(p.amount), 0) from PaymentVO p" +
            " where (:memberID is null or p.member.memberID = :memberID)" +
            " and (:feeType is null or p.feeType = :feeType)" +
            " and (:methodOfPayment is null or p.methodOfPayment = :methodOfPayment)" +
            " and (:year is null or p.year = :year)" +
            " and (:quarter is null or p.quarter = :quarter)" +
            " and (:dateFrom is null or p.dateReceived >= :dateFrom)" +
            " and (:dateTo is null or p.dateReceived <= :dateTo)")
    BigDecimal sumAmountByFilters(@Param("memberID") Integer memberID,
                                  @Param("feeType") String feeType,
                                  @Param("methodOfPayment") String methodOfPayment,
                                  @Param("year") Integer year,
                                  @Param("quarter") Integer quarter,
                                  @Param("dateFrom") LocalDate dateFrom,
                                  @Param("dateTo") LocalDate dateTo);

    @Query("select p.year as year, p.quarter as quarter, sum(p.amount) as totalPaid from PaymentVO p where p.member.memberID = :memberID and p.feeType = :feeType group by p.year, p.quarter")
    List<PaymentSummary> findMemberPaymentSummaries(@Param("memberID") Integer memberID, @Param("feeType") String feeType);

    @Query("select p.member.memberID as memberID, p.year as year, p.quarter as quarter, sum(p.amount) as totalPaid from PaymentVO p where p.member.memberID in :memberIds and p.feeType = :feeType and p.year = :year group by p.member.memberID, p.year, p.quarter")
    List<PaymentSummary> findMembersPaymentSummaries(@Param("memberIds") List<Integer> memberIds, @Param("feeType") String feeType, @Param("year") Integer year);

    @Query("select p.member.memberID as memberID, p.year as year, p.quarter as quarter, sum(p.amount) as totalPaid from PaymentVO p where p.feeType = :feeType and p.year = :year group by p.member.memberID, p.year, p.quarter")
    List<PaymentSummary> findPaymentSummaries(@Param("feeType") String feeType, @Param("year") Integer year);

    @Query("select p.quarter as quarter, coalesce(sum(p.amount), 0) as totalCollected from PaymentVO p where p.year = :year group by p.quarter")
    List<QuarterlyTotalProjection> findQuarterlyTotals(@Param("year") Integer year);

    @Query(value = """
            SELECT
                p.feeType AS feeType,
                COALESCE(p.quarter, QUARTER(p.dateReceived)) AS quarter,
                COALESCE(SUM(p.amount), 0) AS totalAmount
            FROM payment p
            WHERE COALESCE(p.year, YEAR(p.dateReceived)) = :year
            GROUP BY p.feeType, COALESCE(p.quarter, QUARTER(p.dateReceived))
            """, nativeQuery = true)
    List<FeeTypeQuarterlyTotalProjection> findFeeTypeQuarterlyTotals(@Param("year") Integer year);

    @Query("select p.methodOfPayment as methodOfPayment, coalesce(sum(p.amount), 0) as totalAmount from PaymentVO p where p.dateReceived >= :start and p.dateReceived <= :end group by p.methodOfPayment")
    List<PaymentMethodTotalProjection> findPaymentMethodTotalsBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
            SELECT COALESCE(SUM(GREATEST(0, :feeAmt - COALESCE(paid.total, 0))), 0)
            FROM member m
            CROSS JOIN (SELECT 1 AS q UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) quarters
            LEFT JOIN (
                SELECT p.memberID, p.quarter, SUM(p.amount) AS total
                FROM payment p
                WHERE p.feeType = :feeType AND p.year = :year
                GROUP BY p.memberID, p.quarter
            ) paid ON m.memberID = paid.memberID AND quarters.q = paid.quarter
            WHERE m.status = :status
              AND quarters.q >= CASE
                    WHEN YEAR(COALESCE(m.joinDate, CURDATE())) = :year
                      THEN QUARTER(COALESCE(m.joinDate, CURDATE()))
                    ELSE 1
                  END
              AND quarters.q < :currentQuarter
              AND (YEAR(COALESCE(m.joinDate, CURDATE())) < :year
                   OR (YEAR(COALESCE(m.joinDate, CURDATE())) = :year
                       AND QUARTER(COALESCE(m.joinDate, CURDATE())) < :currentQuarter))
            """, nativeQuery = true)
    BigDecimal calculateTotalOverdue(@Param("year") Integer year,
                                     @Param("currentQuarter") Integer currentQuarter,
                                     @Param("feeAmt") BigDecimal feeAmt,
                                     @Param("feeType") String feeType,
                                     @Param("status") String status);

}