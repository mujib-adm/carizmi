package org.sofumar.portal.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceDto {
    private Integer referenceID;
    private String referenceName;
    private String referenceCode;
    private String referenceDisplay;
    private boolean active;
}