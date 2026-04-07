package org.sofumar.portal.framework.data.response;

/**
 * HTTP-agnostic container for a single result whose inner content is paginated, returned by BL/service methods.
 *
 * @param data the single result object
 * @param meta pagination metadata (page, size, total elements, total pages) for the inner paginated content
 */
public record SinglePagedResult<T>(T data, PaginationMeta meta) {

    public static <T> SinglePagedResult<T> of(T data, PaginationMeta meta) {
        return new SinglePagedResult<>(data, meta);
    }
}