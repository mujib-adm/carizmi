package org.sofumar.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.request.ChecklistSearchRequestDto;
import org.sofumar.portal.data.dto.response.QuarterlyChecklistDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.security.annotation.IsAuthenticated;
import org.sofumar.portal.service.helper.QuarterlyFeeChecklistService;
import org.sofumar.portal.framework.data.response.SinglePagedResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checklist")
@Tag(name = "Checklist", description = "Membership fee checklist APIs")
@RequiredArgsConstructor
@IsAuthenticated
public class QuarterlyFeeChecklistController {

    private final QuarterlyFeeChecklistService checklistService;

    @PostMapping("/quarterly-fee")
    @Operation(summary = "Search quarterly membership fee checklist")
    public ResponseEntity<GlobalResponse<QuarterlyChecklistDto>> getQuarterlyChecklist(
            @Valid @RequestBody ChecklistSearchRequestDto request) {
        SinglePagedResult<QuarterlyChecklistDto> result = checklistService.getQuarterlyChecklist(request);
        return ResponseUtils.okWithDataPageable(result.data(), result.meta());
    }
}