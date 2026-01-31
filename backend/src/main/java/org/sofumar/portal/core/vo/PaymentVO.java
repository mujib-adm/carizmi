package org.sofumar.portal.core.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.TableConstants;
import org.sofumar.portal.framework.vo.ValueObject;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableConstants.PAYMENT_TABLE)
public class PaymentVO extends ValueObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = FieldConstants.PAYMENT_ID)
    private Integer paymentID;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = FieldConstants.MEMBER_ID, nullable = false, foreignKey = @ForeignKey(name = "fk_payment_member"))
    private MemberVO member;

    @NotBlank
    @Column(name = FieldConstants.FEE_TYPE, nullable = false)
    private String feeType;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    @Column(name = FieldConstants.AMOUNT, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Column(name = FieldConstants.DATE_RECEIVED, nullable = false)
    private LocalDate dateReceived;

    @Column(name = FieldConstants.YEAR)
    private Integer year;

    @Column(name = FieldConstants.QUARTER)
    private Integer quarter;

    @NotBlank
    @Column(name = FieldConstants.METHOD_OF_PAYMENT, nullable = false)
    private String methodOfPayment;

    @Override
    public String getTableName() {
        return TableConstants.PAYMENT_TABLE;
    }
}