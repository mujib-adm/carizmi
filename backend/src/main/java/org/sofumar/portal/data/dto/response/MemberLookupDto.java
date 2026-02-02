package org.sofumar.portal.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberLookupDto {
    private Integer memberID;
    private String firstName;
    private String lastName;
    private String phone;
}