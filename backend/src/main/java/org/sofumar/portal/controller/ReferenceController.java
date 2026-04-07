package org.sofumar.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.response.ReferenceDescDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.data.dto.request.ReferenceSearchRequestDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.data.response.PagedResult;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.core.businesslogic.Reference;
import org.sofumar.portal.security.annotation.IsAuthenticated;
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