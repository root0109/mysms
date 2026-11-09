package com.mysms.core.paging;

import java.util.List;
import java.util.function.Function;

/** One page of a list: the items and the opaque token of the next page (empty on the last page). */
public record Page<T>(List<T> items, String nextPageToken) {

    public Page {
        items = List.copyOf(items);
        nextPageToken = nextPageToken == null ? "" : nextPageToken;
    }

    public <U> Page<U> map(Function<T, U> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), nextPageToken);
    }
}
