package com.mysms.core.paging;

import com.mysms.core.error.InvalidArgumentException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * A validated list request: page size (1-500, default 50), order, filter text and the keyset cursor.
 * Repositories call {@link #keysetPredicate()}, {@link #orderByClause()} and {@link #limit()} and hand the fetched
 * rows back to {@link #toPage}, which trims the look-ahead row and issues the next token.
 */
public final class PageRequest {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 500;

    private final int pageSize;
    private final SortSpec sort;
    private final String filter;
    private final Optional<PageToken.Cursor> cursor;
    private final Clock clock;

    private PageRequest(int pageSize, SortSpec sort, String filter, Optional<PageToken.Cursor> cursor, Clock clock) {
        this.pageSize = pageSize;
        this.sort = sort;
        this.filter = filter;
        this.cursor = cursor;
        this.clock = clock;
    }

    public static PageRequest of(Integer pageSize, String pageToken, SortSpec sort, String filter, Clock clock) {
        int size = pageSize == null ? DEFAULT_SIZE : pageSize;
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidArgumentException("pageSize must be between 1 and " + MAX_SIZE + ".");
        }
        Optional<PageToken.Cursor> cursor = pageToken == null || pageToken.isBlank()
                ? Optional.empty()
                : Optional.of(PageToken.decode(pageToken, sort, filter, clock));
        return new PageRequest(size, sort, filter, cursor, clock);
    }

    public int pageSize() {
        return pageSize;
    }

    /** Rows to fetch: one more than the page, to know whether another page exists. */
    public int limit() {
        return pageSize + 1;
    }

    public SortSpec sort() {
        return sort;
    }

    public String orderByClause() {
        return sort.orderByClause();
    }

    /**
     * Rows strictly after the cursor in the sort order: for keys k0..kn and the id tie-breaker,
     * (k0 > v0) OR (k0 = v0 AND k1 > v1) OR ... (with &lt; for descending keys).
     */
    public SqlFragment keysetPredicate() {
        if (cursor.isEmpty()) {
            return SqlFragment.TRUE;
        }
        List<SortSpec.Order> orders = sort.orders();
        List<Object> values = cursor.get().sortValues();
        Map<String, Object> params = new LinkedHashMap<>();
        List<String> disjuncts = new ArrayList<>();
        for (int k = 0; k <= orders.size(); k++) {
            List<String> conjuncts = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                conjuncts.add(orders.get(j).field().column() + " = :ks" + j);
            }
            if (k < orders.size()) {
                SortSpec.Order order = orders.get(k);
                conjuncts.add(order.field().column() + (order.descending() ? " < :ks" : " > :ks") + k);
            } else {
                conjuncts.add(sort.idColumn() + " > :ksid");
            }
            disjuncts.add("(" + String.join(" AND ", conjuncts) + ")");
        }
        for (int i = 0; i < orders.size(); i++) {
            params.put("ks" + i, values.get(i));
        }
        params.put("ksid", cursor.get().lastId());
        return new SqlFragment(String.join(" OR ", disjuncts), params);
    }

    /**
     * Builds the page from up to {@link #limit()} fetched rows.
     *
     * @param sortValues the row's values of the sort keys, in sort order
     * @param id         the row's id
     */
    public <R> Page<R> toPage(List<R> rows, Function<R, List<Object>> sortValues, Function<R, UUID> id) {
        if (rows.size() <= pageSize) {
            return new Page<>(rows, "");
        }
        List<R> items = rows.subList(0, pageSize);
        R last = items.get(items.size() - 1);
        return new Page<>(items, PageToken.encode(sort, filter, sortValues.apply(last), id.apply(last), clock));
    }
}
