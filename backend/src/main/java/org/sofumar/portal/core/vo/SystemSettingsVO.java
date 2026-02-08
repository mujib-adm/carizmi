package org.sofumar.portal.core.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.TableConstants;
import org.sofumar.portal.framework.vo.ValueObject;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableConstants.SYSTEM_SETTINGS_TABLE)
public class SystemSettingsVO extends ValueObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = FieldConstants.SYSTEM_SETTINGS_ID)
    private Integer systemSettingsID;

    @NotBlank
    @Column(name = FieldConstants.SETTING_NAME, nullable = false)
    private String settingName;

    @NotBlank
    @Column(name = FieldConstants.SETTING_KEY, unique = true, nullable = false)
    private String settingKey;

    @NotBlank
    @Column(name = FieldConstants.SETTING_VALUE, nullable = false)
    private String settingValue;

    @Column(name = FieldConstants.EFFECTIVE_DATE)
    private LocalDate effectiveDate;

    @Column(name = FieldConstants.ACTIVE)
    private boolean active = true;

    @Override
    public String getTableName() {
        return TableConstants.SYSTEM_SETTINGS_TABLE;
    }
}