package org.sofumar.portal.service.helper;

import org.sofumar.portal.data.dto.request.ChecklistSearchRequestDto;
import org.sofumar.portal.data.dto.response.QuarterlyChecklistDto;
import org.sofumar.portal.framework.data.response.SinglePagedResult;

public interface QuarterlyFeeChecklistService {
    SinglePagedResult<QuarterlyChecklistDto> getQuarterlyChecklist(ChecklistSearchRequestDto request);
}