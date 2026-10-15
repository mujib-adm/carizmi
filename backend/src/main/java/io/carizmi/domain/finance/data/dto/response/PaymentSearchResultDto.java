package io.carizmi.domain.finance.data.dto.response;

import io.carizmi.domain.finance.data.dto.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSearchResultDto {
    private List<PaymentDto> payments;
    private PaymentSummaryDto summary;
}