package io.carizmi.domain.membership.data.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MemberDto {
    private Integer memberID;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String status;
    private LocalDate joinDate;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String zip;
}