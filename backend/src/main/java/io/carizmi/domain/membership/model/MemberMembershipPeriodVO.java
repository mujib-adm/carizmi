package io.carizmi.domain.membership.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.carizmi.shared.constants.FieldConstants;
import io.carizmi.shared.constants.TableConstants;
import io.carizmi.framework.vo.ValueObject;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableConstants.MEMBER_MEMBERSHIP_PERIOD_TABLE)
public class MemberMembershipPeriodVO extends ValueObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = FieldConstants.PERIOD_ID)
    private Integer periodID;

    @NotNull
    @Column(name = FieldConstants.MEMBER_ID, nullable = false)
    private Integer memberID;

    @NotNull
    @Column(name = FieldConstants.START_DATE, nullable = false)
    private LocalDate startDate;

    @Column(name = FieldConstants.END_DATE)
    private LocalDate endDate;

    @Override
    public String getTableName() {
        return TableConstants.MEMBER_MEMBERSHIP_PERIOD_TABLE;
    }
}