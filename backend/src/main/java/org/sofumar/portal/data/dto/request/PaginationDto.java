package org.sofumar.portal.data.dto.request;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.NonNull;

@Data
public class PaginationDto {
    private int page = 0;
    private int size = 10;
    private String sortField;
    private String sortOrder;

    @NonNull
    @JsonIgnore
    public Pageable toPageable() {
        Sort sort = Sort.unsorted();
        if (StringUtils.isNotBlank(sortField)) {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, sortField);
        }
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), sort);
    }
}