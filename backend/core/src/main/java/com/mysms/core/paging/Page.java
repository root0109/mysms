package com.mysms.core.paging;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a list, as the repository read it: the items, the one-based page number and the requested page size,
 * and the opaque cursor of the next page (empty on the last page). {@link PagingUtil} turns it into the
 * {@link PagingInfo} a list operation returns.
 */
public record Page<T>(List<T> items, int page, int size, String nextCursor) {

    public Page {
        items = List.copyOf(items);
        nextCursor = nextCursor == null ? "" : nextCursor;
    }

    /** Whether another page follows this one. */
    public boolean hasNext() {
        return !nextCursor.isEmpty();
    }

    /** The same page with every item converted, e.g. entities to contract DTOs. */
    public <U> Page<U> map(Function<T, U> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), page, size, nextCursor);
    }
}
