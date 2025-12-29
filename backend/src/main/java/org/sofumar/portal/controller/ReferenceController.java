package org.sofumar.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.ReferenceDataDto;
import org.sofumar.portal.data.dto.ReferenceDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.service.businesslogic.ReferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reference")
@Tag(name = "Reference Data", description = "Reference data management APIs")
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceService referenceService;

    @GetMapping("/get/{referenceID}")
    @Operation(summary = "Get reference by ID")
    public ResponseEntity<GlobalResponse<ReferenceDto>> getReference(@PathVariable Integer referenceID) {
        return referenceService.getReference(referenceID);
    }

    @GetMapping("/search")
    @Operation(summary = "Search references")
    public ResponseEntity<GlobalResponse<List<ReferenceDto>>> searchReferences(
            @RequestParam(required = false) String referenceName,
            @RequestParam(required = false) String referenceCode,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return referenceService.searchReferences(referenceName, referenceCode, active, page, size, sortField,
                sortOrder);
    }

    @GetMapping("/list/{referenceName}")
    @Operation(summary = "Get list of references by name (e.g. feeType)")
    public ResponseEntity<GlobalResponse<List<ReferenceDataDto>>> getReferencesByName(
            @PathVariable String referenceName) {
        return referenceService.getReferencesByName(referenceName);
    }
}