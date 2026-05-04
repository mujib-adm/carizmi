package io.carizmi.domain.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CQRS Read Model for dashboard metrics.
 *
 * <p>This is a singleton entity (id = 1) that holds pre-computed dashboard
 * metrics. It is automatically rebuilt by {@code DashboardProjector}
 * via in-process Spring events whenever a relevant domain change occurs
 * (Payment, Expense, or Member).</p>
 *
 * <p>The dashboard API reads from this table instead of performing multiple cross-domain
 * queries on every request, reducing the read path to a single-row lookup.</p>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "dashboard_metrics_snapshot")
public class DashboardSnapshotVO {

    @Id
    @Column(name = "id")
    @ColumnDefault("1")
    private Integer id;

    @Column(name = "total_active_members", nullable = false)
    @ColumnDefault("0")
    private Long totalActiveMembers;

    @Column(name = "total_revenue", nullable = false)
    @ColumnDefault("0.00")
    private BigDecimal totalRevenue;

    @Column(name = "dues_this_quarter", nullable = false)
    @ColumnDefault("0.00")
    private BigDecimal duesThisQuarter;

    @Column(name = "overdue_total", nullable = false)
    @ColumnDefault("0.00")
    private BigDecimal overdueTotal;

    @Column(name = "quarterly_fee_amt", nullable = false)
    @ColumnDefault("0.00")
    private BigDecimal quarterlyFeeAmt;

    @Column(name = "quarterly_collections", columnDefinition = "JSON")
    private String quarterlyCollections;

    @Column(name = "last_projected_at",
            nullable = false,
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime lastProjectedAt;
}