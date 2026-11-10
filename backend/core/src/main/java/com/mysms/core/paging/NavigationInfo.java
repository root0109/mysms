package com.mysms.core.paging;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The navigation part of a {@link PagingInfo}: where the caller is and the links to move on (relative URLs that keep
 * the request's filter, sort and field mask). A link that does not apply — {@code previous} on the first page,
 * {@code next} on the last — is omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NavigationInfo(
        @Schema(example = "3", description = "The current page number (one-based).")
        int page,
        @Schema(example = "10", description = "The count of items in the current page.")
        int size,
        @Schema(example = "/v1/hr/staff?page=1&size=10", description = "URL of the first page.")
        String first,
        @Schema(example = "/v1/hr/staff?page=2&size=10", description = "URL of the previous page.")
        String previous,
        @Schema(example = "/v1/hr/staff?page=4&size=10&cursor=eyJ2Ijo…",
                description = "URL of the next page; absent on the last page. It carries the cursor that continues "
                        + "exactly after the last item of this page.")
        String next) {
}
