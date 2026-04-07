package org.sofumar.portal.framework.data.response;

import java.util.List;

/**
 * HTTP-agnostic container for paginated results returned by BL/service methods.
 *
 * @param items the paginated list of items
 * @param meta pagination metadata (page, size, total elements, total pages)
 */
public record PagedResult<T>(List<T> items, PaginationMeta meta) {

    public static <T> PagedResult<T> of(List<T> items, PaginationMeta meta) {
        return new PagedResult<>(items, meta);
    }
}