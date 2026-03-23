package org.sofumar.portal.framework.data.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Pagination metadata")
public class PaginationMeta {
    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;
    @Schema(description = "Page size", example = "10")
    private int pageSize;
    @Schema(description = "Total number of records", example = "42")
    private long totalRecords;
    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    public PaginationMeta(int page, int pageSize, long totalRecords) {
        this.page = page;
        this.pageSize = pageSize;
        this.totalRecords = totalRecords;
        this.totalPages = (int) Math.ceil((double) totalRecords / pageSize);
    }

    public static PaginationMeta of(int page, int pageSize, long totalRecords, int totalPages) {
        return new PaginationMeta(page, pageSize, totalRecords, totalPages);
    }
}