package io.carizmi.domain.finance.model;

import io.carizmi.framework.vo.ValueObject;
import io.carizmi.shared.constants.TableConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CQRS Read Model for frozen Annual Financial Statement snapshots.
 *
 * <p>When a fiscal year is closed by {@code YearEndScheduler} / {@code BaselineService},
 * the compiled {@code AnnualFinancialReportDto} is frozen into this table as an immutable JSON snapshot.
 * Subsequent queries for closed years are served directly from this snapshot in O(1) time,
 * guaranteeing 100% financial statement idempotency and zero drift.</p>
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableConstants.ANNUAL_REPORT_SNAPSHOT_TABLE)
public class AnnualReportSnapshotVO extends ValueObject {

    @Id
    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    @Column(name = "report_data", columnDefinition = "JSON", nullable = false)
    private String reportData;

    @Column(name = "beginning_cash_balance", nullable = false)
    private BigDecimal beginningCashBalance;

    @Column(name = "total_receipts", nullable = false)
    private BigDecimal totalReceipts;

    @Column(name = "total_disbursements", nullable = false)
    private BigDecimal totalDisbursements;

    @Column(name = "ending_cash_balance", nullable = false)
    private BigDecimal endingCashBalance;

    @Column(name = "active_members_count", nullable = false)
    private Long activeMembersCount;

    @Column(name = "closed_at", nullable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", nullable = false, length = 50)
    private String closedBy;

    @Override
    public String getTableName() {
        return TableConstants.ANNUAL_REPORT_SNAPSHOT_TABLE;
    }
}