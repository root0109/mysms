package com.mysms.core.paging;

import com.mysms.core.error.InvalidArgumentException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A validated {@code sort}: {@code field,asc|desc} criteria checked against the allowlist, with the row id as final
 * tie-breaker so OFFSET pages and keyset cursors are exact. Spring splits a single value at its commas but not
 * several, so all values are read as one token stream of field names, each optionally followed by a direction.
 */
public final class SortSpec {

    private static final Pattern SEPARATORS = Pattern.compile("[,\\s]+");

    /** One sort key. */
    public record Order(FieldSpec field, boolean descending) {
    }

    private final List<Order> orders;
    private final String idColumn;

    private SortSpec(List<Order> orders, String idColumn) {
        this.orders = List.copyOf(orders);
        this.idColumn = idColumn;
    }

    /**
     * Parses {@code sort} (absent or empty → {@code defaultSort}) against the allowlist; unknown fields, a field
     * listed twice or a stray direction → 400.
     *
     * @param defaultSort the aggregate's default order in the same syntax, e.g. {@code "name"} or {@code "createTime,desc"}
     * @param idColumn    the aggregate's id column, appended as the final tie-breaker
     */
    public static SortSpec parse(List<String> sort, Map<String, FieldSpec> allowed, String defaultSort,
            String idColumn) {
        List<String> tokens = tokens(sort);
        if (tokens.isEmpty()) {
            tokens = tokens(List.of(defaultSort));
        }
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            String name = tokens.get(i);
            if (isDirection(name)) {
                throw new InvalidArgumentException("sort must be field names, each optionally followed by asc or desc.");
            }
            FieldSpec field = allowed.get(name);
            if (field == null) {
                throw new InvalidArgumentException("sort field '" + name + "' is not sortable.");
            }
            if (orders.stream().anyMatch(o -> o.field().name().equals(field.name()))) {
                throw new InvalidArgumentException("sort lists '" + field.name() + "' twice.");
            }
            boolean descending = false;
            if (i + 1 < tokens.size() && isDirection(tokens.get(i + 1))) {
                descending = "desc".equalsIgnoreCase(tokens.get(++i));
            }
            orders.add(new Order(field, descending));
        }
        return new SortSpec(orders, idColumn);
    }

    private static List<String> tokens(List<String> values) {
        List<String> tokens = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    for (String token : SEPARATORS.split(value.trim())) {
                        if (!token.isEmpty()) {
                            tokens.add(token);
                        }
                    }
                }
            }
        }
        return tokens;
    }

    private static boolean isDirection(String token) {
        return "asc".equalsIgnoreCase(token) || "desc".equalsIgnoreCase(token);
    }

    public List<Order> orders() {
        return orders;
    }

    public String idColumn() {
        return idColumn;
    }

    /** Canonical text ({@code field,asc|desc;…}), stored in cursors so one cannot be replayed under another sort. */
    public String canonical() {
        return orders.stream()
                .map(o -> o.field().name() + (o.descending() ? ",desc" : ",asc"))
                .collect(Collectors.joining(";"));
    }

    /**
     * Whether every key sorts the same way, descending. The id tie-breaker then descends too, so the whole order runs
     * one way and the keyset predicate can be a single row comparison an index range scan serves.
     */
    public boolean allDescending() {
        return orders.stream().allMatch(Order::descending);
    }

    /** Whether every key sorts the same way (all ascending or all descending, the id tie-breaker following them). */
    public boolean uniform() {
        return allDescending() || orders.stream().noneMatch(Order::descending);
    }

    /** The ORDER BY list (without the keyword); the id breaks ties in the direction of a uniform order. */
    public String orderByClause() {
        return orders.stream()
                .map(o -> o.field().column() + (o.descending() ? " DESC" : " ASC"))
                .collect(Collectors.joining(", ")) + ", " + idColumn + (allDescending() ? " DESC" : " ASC");
    }
}
