package io.carizmi.shared.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.NonNull;

@Data
@Schema(description = "Pagination parameters")
public class PaginationDto {
    @Schema(description = "Page number (0-based)", defaultValue = "0", example = "0")
    private Integer page = 0;
    @Schema(description = "Page size", defaultValue = "10", example = "10")
    private Integer size = 10;
    @Schema(description = "Sort field name", example = "firstName")
    private String sortField;
    @Schema(description = "Sort direction", example = "asc")
    private SortOrder sortOrder;

    @NonNull
    @JsonIgnore
    public Pageable toPageable() {
        Sort sort = Sort.unsorted();
        if (StringUtils.isNotBlank(sortField)) {
            Sort.Direction direction = sortOrder == SortOrder.desc ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, sortField);
        }
        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? 10 : size;
        return PageRequest.of(Math.max(0, safePage), Math.max(1, Math.min(safeSize, 100)), sort);
    }
}