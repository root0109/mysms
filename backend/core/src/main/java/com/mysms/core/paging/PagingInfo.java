package com.mysms.core.paging;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The response of every list operation: the items of the requested page and the {@link NavigationInfo} to move
 * between pages. Built by {@link PagingUtil#pagingInfo(Page)}.
 *
 * @param <T> the contract DTO listed
 */
public record PagingInfo<T>(
        @Schema(description = "Data retrieved in the paginated request.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<T> data,
        @Schema(description = "Navigation info of the paginated request.", requiredMode = Schema.RequiredMode.REQUIRED)
        NavigationInfo paging) {

    public PagingInfo {
        data = List.copyOf(data);
    }
}
