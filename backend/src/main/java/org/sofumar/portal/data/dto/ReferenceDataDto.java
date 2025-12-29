package org.sofumar.portal.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceDataDto {
    private String referenceCode;
    private String referenceDisplay;
}