package org.sofumar.portal.core.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.sofumar.portal.constants.Role;
import org.sofumar.portal.constants.FieldConstants;
import org.sofumar.portal.constants.TableConstants;
import org.sofumar.portal.framework.vo.ValueObject;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableConstants.USER_TABLE)
public class UserVO extends ValueObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = FieldConstants.USER_ID)
    private Integer userID;

    @NotBlank
    @Column(name = FieldConstants.FIRST_NAME, nullable = false)
    private String firstName;

    @NotBlank
    @Column(name = FieldConstants.LAST_NAME, nullable = false)
    private String lastName;

    @Email
    @NotBlank
    @Column(name = FieldConstants.EMAIL, unique = true, nullable = false)
    private String email;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9._-]{4,}$")
    @Column(name = FieldConstants.USERNAME, unique = true, nullable = false)
    private String username;

    @NotBlank
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d\\W]).{5,}$")
    @Column(name = FieldConstants.PASSWORD, nullable = false)
    private String password;

    @Column(name = FieldConstants.PASSWORD_UPDATED_AT)
    private LocalDateTime passwordUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = FieldConstants.ROLE, nullable = false)
    private Role role;

    @Column(name = FieldConstants.ACTIVE)
    private boolean active = true;

    @Column(name = "failedLoginAttempts")
    private int failedLoginAttempts = 0;

    @Column(name = "lockoutTime")
    private LocalDateTime lockoutTime;
    
    @Transient
    @Setter(AccessLevel.NONE)
    private String persistedPassword;

    @PostLoad
    protected void postLoad() {
        this.persistedPassword = this.password;
    }

    @Override
    public String getTableName() {
        return TableConstants.USER_TABLE;
    }
}