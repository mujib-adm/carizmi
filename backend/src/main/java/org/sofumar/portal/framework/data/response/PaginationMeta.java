package org.sofumar.portal.framework.data.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaginationMeta {
    private int page;
    private int pageSize;
    private long totalRecords;
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