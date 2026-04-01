package org.sofumar.portal.service.helper;

import org.sofumar.portal.data.dto.request.ChecklistSearchRequestDto;
import org.sofumar.portal.data.dto.response.QuarterlyChecklistDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.springframework.http.ResponseEntity;

public interface QuarterlyFeeChecklistService {
    ResponseEntity<GlobalResponse<QuarterlyChecklistDto>> getQuarterlyChecklist(ChecklistSearchRequestDto request);
}