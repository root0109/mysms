package com.mysms.core.paging;

import com.mysms.core.error.InvalidArgumentException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A validated `orderBy` (api/README "Filtering and ordering"): comma-separated fields with optional ` desc`, each
 * checked against the aggregate's allowlist and mapped to its column (JAVA-CODING-RULES §6). The row id is always the
 * final tie-breaker, so the order is total and keyset paging is exact.
 */
public final class SortSpec {

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
     * Parses `orderBy` (blank → `defaultOrderBy`) against the allowlist; unknown fields or bad syntax → 400.
     *
     * @param idColumn the aggregate's id column, appended as the final ascending tie-breaker
     */
    public static SortSpec parse(String orderBy, Map<String, FieldSpec> allowed, String defaultOrderBy, String idColumn) {
        String text = orderBy == null || orderBy.isBlank() ? defaultOrderBy : orderBy;
        List<Order> orders = new ArrayList<>();
        for (String part : text.split(",", -1)) {
            String[] tokens = part.trim().split("\\s+");
            if (tokens.length == 0 || tokens[0].isEmpty() || tokens.length > 2
                    || tokens.length == 2 && !"desc".equals(tokens[1]) && !"asc".equals(tokens[1])) {
                throw new InvalidArgumentException("orderBy must be comma-separated fields with an optional ' desc'.");
            }
            FieldSpec field = allowed.get(tokens[0]);
            if (field == null) {
                throw new InvalidArgumentException("orderBy field '" + tokens[0] + "' is not sortable.");
            }
            if (orders.stream().anyMatch(o -> o.field().name().equals(field.name()))) {
                throw new InvalidArgumentException("orderBy lists '" + field.name() + "' twice.");
            }
            orders.add(new Order(field, tokens.length == 2 && "desc".equals(tokens[1])));
        }
        return new SortSpec(orders, idColumn);
    }

    public List<Order> orders() {
        return orders;
    }

    public String idColumn() {
        return idColumn;
    }

    /** Canonical text, stored in page tokens so a token cannot be replayed under a different order. */
    public String canonical() {
        return orders.stream()
                .map(o -> o.field().name() + (o.descending() ? " desc" : ""))
                .collect(Collectors.joining(","));
    }

    /** The ORDER BY list (without the keyword). */
    public String orderByClause() {
        return orders.stream()
                .map(o -> o.field().column() + (o.descending() ? " DESC" : " ASC"))
                .collect(Collectors.joining(", ")) + ", " + idColumn + " ASC";
    }
}
