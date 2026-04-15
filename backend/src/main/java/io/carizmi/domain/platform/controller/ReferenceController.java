package io.carizmi.domain.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.carizmi.domain.platform.data.dto.response.ReferenceDescDto;
import io.carizmi.domain.platform.data.dto.ReferenceDto;
import io.carizmi.domain.platform.data.dto.request.ReferenceSearchRequestDto;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.data.response.PagedResult;
import io.carizmi.framework.util.ResponseUtils;
import io.carizmi.domain.platform.service.Reference;
import io.carizmi.infrastructure.security.annotation.IsAuthenticated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reference")
@Tag(name = "Reference Data", description = "Reference data management APIs")
@RequiredArgsConstructor
public class ReferenceController {

    private final Reference reference;

    @GetMapping("/get/{referenceID}")
    @Operation(summary = "Get reference by ID")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<ReferenceDto>> getReference(@PathVariable Integer referenceID) {
        return ResponseUtils.okWithData(reference.getReference(referenceID));
    }

    @PostMapping("/search")
    @Operation(summary = "Search references")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<ReferenceDto>>> searchReferences(
            @RequestBody ReferenceSearchRequestDto request) {
        PagedResult<ReferenceDto> result = reference.searchReferences(request);
        return ResponseUtils.okWithDataPageable(result.items(), result.meta());
    }

    @GetMapping("/list/{referenceName}")
    @Operation(summary = "Get list of references by name (e.g. feeType)")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<ReferenceDescDto>>> getReferencesByName(
            @PathVariable String referenceName) {
        return ResponseUtils.okWithData(reference.getReferencesByName(referenceName));
    }
}