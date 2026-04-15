package io.carizmi.domain.finance.service;

import io.carizmi.domain.finance.data.dto.request.ChecklistSearchRequestDto;
import io.carizmi.domain.finance.data.dto.response.QuarterlyChecklistDto;
import io.carizmi.framework.data.response.SinglePagedResult;

public interface QuarterlyFeeChecklistService {
    SinglePagedResult<QuarterlyChecklistDto> getQuarterlyChecklist(ChecklistSearchRequestDto request);
}