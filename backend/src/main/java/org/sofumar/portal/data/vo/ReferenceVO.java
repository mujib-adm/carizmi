package org.sofumar.portal.data.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.TableConstants;
import org.sofumar.portal.framework.vo.ValueObject;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableConstants.REFERENCE_TABLE, uniqueConstraints = {
        @UniqueConstraint(columnNames = { FieldConstants.REFERENCE_NAME, FieldConstants.REFERENCE_CODE })
})
public class ReferenceVO extends ValueObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = FieldConstants.REFERENCE_ID)
    private Integer referenceID;

    @NotBlank
    @Column(name = FieldConstants.REFERENCE_NAME, nullable = false)
    private String referenceName;

    @NotBlank
    @Size(max = 3)
    @Column(name = FieldConstants.REFERENCE_CODE, nullable = false, length = 3)
    private String referenceCode;

    @NotBlank
    @Column(name = FieldConstants.REFERENCE_DISPLAY, nullable = false)
    private String referenceDisplay;

    @Column(name = FieldConstants.ACTIVE, nullable = false)
    private boolean active = true;

    @Override
    public String getTableName() {
        return TableConstants.REFERENCE_TABLE;
    }
}