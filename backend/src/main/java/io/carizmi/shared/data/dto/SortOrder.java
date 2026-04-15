package io.carizmi.shared.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum SortOrder {
    asc,
    desc
}