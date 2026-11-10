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
 * A validated list request (api/README "Pagination"): the one-based {@code page} (default 1), the page {@code size}
 * (1-500, default 50), the sort and, when the caller follows a {@code next} link, its cursor.
 *
 * <p>Two ways reach a page, and the repository SQL is the same for both — it appends {@link #keysetPredicate()} to
 * its WHERE, orders by {@link #orderByClause()}, ends with {@link #limitClause()} and binds {@link #limitParams()}:
 * <ul>
 * <li>With a cursor (every {@code next} link carries one) the page starts right after the cursor's row: a keyset
 * seek on the sort index, as fast on page 10,000 as on page 2, and stable while rows are added in front.</li>
 * <li>Without one (the first page, a {@code previous} link, a jump to page n) the page is read with OFFSET. OFFSET
 * reads and discards every skipped row, so it is capped at {@link #MAX_OFFSET} rows; deeper pages are reached by
 * following {@code next} links.</li>
 * </ul>
 * One row more than the page is fetched to know whether another page exists (no count query is ever run).
 */
public final class PageRequest {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 500;
    /** The most rows an OFFSET page may skip. */
    public static final int MAX_OFFSET = 10_000;

    private final int page;
    private final int size;
    private final SortSpec sort;
    private final String view;
    private final Optional<PageCursor.Position> cursor;
    private final Clock clock;

    private PageRequest(int page, int size, SortSpec sort, String view, Optional<PageCursor.Position> cursor,
            Clock clock) {
        this.page = page;
        this.size = size;
        this.sort = sort;
        this.view = view;
        this.cursor = cursor;
        this.clock = clock;
    }

    /**
     * Validates the request parameters.
     *
     * @param page   the one-based page number; null → 1
     * @param size   the page size; null → {@link #DEFAULT_SIZE}
     * @param cursor the cursor of a {@code next} link, or null/blank
     * @param view   what identifies the rows listed besides the sort — the filter text and any parent id — so a
     *               cursor cannot be replayed against another list
     */
    public static PageRequest of(Integer page, Integer size, String cursor, SortSpec sort, String view, Clock clock) {
        int number = page == null ? 1 : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (number < 1 || number == Integer.MAX_VALUE) {
            // the last int is no page: the page after it could not be numbered
            throw new InvalidArgumentException("page must be between 1 and " + (Integer.MAX_VALUE - 1) + ".");
        }
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw new InvalidArgumentException("size must be between 1 and " + MAX_SIZE + ".");
        }
        Optional<PageCursor.Position> position = cursor == null || cursor.isBlank()
                ? Optional.empty()
                : Optional.of(PageCursor.decode(cursor, number, sort, view, clock));
        if (position.isEmpty() && offset(number, pageSize) > MAX_OFFSET) {
            throw new InvalidArgumentException("page " + number + " is beyond the first " + MAX_OFFSET
                    + " rows; follow the next links to read further.");
        }
        return new PageRequest(number, pageSize, sort, view, position, clock);
    }

    private static long offset(int page, int size) {
        return (long) (page - 1) * size;
    }

    /** Whether page n of this size can be opened without a cursor (its OFFSET is within {@link #MAX_OFFSET}). */
    public static boolean reachableByOffset(int page, int size) {
        return page >= 1 && offset(page, size) <= MAX_OFFSET;
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public SortSpec sort() {
        return sort;
    }

    public String orderByClause() {
        return sort.orderByClause();
    }

    /** The SQL tail that bounds the read: {@code " LIMIT :limit OFFSET :offset"}. */
    public String limitClause() {
        return " LIMIT :limit OFFSET :offset";
    }

    /** The bind values of {@link #limitClause()}: one row more than the page, and the rows to skip (0 after a cursor). */
    public Map<String, Object> limitParams() {
        return Map.of("limit", size + 1, "offset", cursor.isPresent() ? 0L : offset(page, size));
    }

    /**
     * Rows strictly after the cursor in the sort order, or TRUE without a cursor. When every key runs the same way
     * the predicate is one row comparison, {@code (k0, k1, …, id) > (v0, v1, …, vid)} ({@code <} when descending),
     * which PostgreSQL answers with a range scan of a matching index; a mixed order needs the expanded form
     * {@code (k0 > v0) OR (k0 = v0 AND k1 > v1) OR …} (with {@code <} for descending keys).
     */
    public SqlFragment keysetPredicate() {
        if (cursor.isEmpty()) {
            return SqlFragment.TRUE;
        }
        List<SortSpec.Order> orders = sort.orders();
        List<Object> values = cursor.get().sortValues();
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < orders.size(); i++) {
            params.put("ks" + i, values.get(i));
        }
        params.put("ksid", cursor.get().lastId());
        if (sort.uniform()) {
            List<String> columns = new ArrayList<>();
            List<String> marks = new ArrayList<>();
            for (int i = 0; i < orders.size(); i++) {
                columns.add(orders.get(i).field().column());
                marks.add(":ks" + i);
            }
            columns.add(sort.idColumn());
            marks.add(":ksid");
            return new SqlFragment("(" + String.join(", ", columns) + (sort.allDescending() ? ") < (" : ") > (")
                    + String.join(", ", marks) + ")", params);
        }
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
        return new SqlFragment(String.join(" OR ", disjuncts), params);
    }

    /**
     * Builds the page from the rows fetched with {@link #limitParams()} (up to one more than the page).
     *
     * @param sortValues the row's values of the sort keys, in sort order
     * @param id         the row's id
     */
    public <R> Page<R> toPage(List<R> rows, Function<R, List<Object>> sortValues, Function<R, UUID> id) {
        if (rows.size() <= size) {
            return new Page<>(rows, page, size, "");
        }
        List<R> items = rows.subList(0, size);
        R last = items.get(items.size() - 1);
        return new Page<>(items, page, size,
                PageCursor.encode(page + 1, sort, view, sortValues.apply(last), id.apply(last), clock));
    }
}
