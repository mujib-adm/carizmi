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

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableConstants.MEMBER_TABLE)
public class MemberVO extends ValueObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = FieldConstants.MEMBER_ID)
    private Integer memberID;

    @NotBlank
    @Column(name = FieldConstants.FIRST_NAME)
    private String firstName;

    @NotBlank
    @Column(name = FieldConstants.LAST_NAME)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}$")
    @Column(name = FieldConstants.PHONE)
    private String phone;

    @Email
    @Column(name = FieldConstants.EMAIL)
    private String email;

    @NotBlank
    @Column(name = FieldConstants.STATUS, nullable = false)
    private String status; // MemberStatus from system settings

    @Column(name = FieldConstants.JOIN_DATE)
    private LocalDate joinDate;

    @Column(name = FieldConstants.ADDRESS1)
    private String address1;

    @Column(name = FieldConstants.ADDRESS2)
    private String address2;

    @Column(name = FieldConstants.CITY)
    private String city;

    @NotBlank
    @Column(name = FieldConstants.STATE, nullable = false)
    private String state = "MN";

    @Column(name = FieldConstants.ZIP)
    private String zip;

    @Override
    public String getTableName() {
        return TableConstants.MEMBER_TABLE;
    }
}