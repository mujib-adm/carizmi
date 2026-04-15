package io.carizmi.domain.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.carizmi.domain.finance.data.dto.request.ChecklistSearchRequestDto;
import io.carizmi.domain.finance.data.dto.response.QuarterlyChecklistDto;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.util.ResponseUtils;
import io.carizmi.infrastructure.security.annotation.IsAuthenticated;
import io.carizmi.domain.finance.service.QuarterlyFeeChecklistService;
import io.carizmi.framework.data.response.SinglePagedResult;
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