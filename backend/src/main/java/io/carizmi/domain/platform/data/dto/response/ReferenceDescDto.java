package io.carizmi.domain.platform.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceDescDto {
    private String referenceCode;
    private String referenceDisplay;
}