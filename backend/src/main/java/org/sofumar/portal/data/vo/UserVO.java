package org.sofumar.portal.data.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Column(name = FieldConstants.ROLE, nullable = false)
    private String role; // ADMIN, MANAGER, MEMBER, ANONYMOUS

    @Column(name = FieldConstants.ACTIVE)
    private boolean active = true;

    @Override
    public String getTableName() {
        return TableConstants.USER_TABLE;
    }
}