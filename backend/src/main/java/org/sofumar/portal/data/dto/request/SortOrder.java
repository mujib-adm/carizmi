package org.sofumar.portal.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum SortOrder {
    asc,
    desc
}