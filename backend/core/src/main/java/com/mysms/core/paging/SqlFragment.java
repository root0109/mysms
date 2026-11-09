package com.mysms.core.paging;

import java.util.LinkedHashMap;
import java.util.Map;

/** A piece of SQL with its named bind parameters. Values are always bound, never concatenated (§6). */
public record SqlFragment(String sql, Map<String, Object> params) {

    public static final SqlFragment TRUE = new SqlFragment("TRUE", Map.of());

    public SqlFragment {
        params = Map.copyOf(params);
    }

    public SqlFragment and(SqlFragment other) {
        if (this == TRUE || "TRUE".equals(sql)) {
            return other;
        }
        if ("TRUE".equals(other.sql())) {
            return this;
        }
        Map<String, Object> merged = new LinkedHashMap<>(params);
        other.params().forEach((name, value) -> {
            if (merged.containsKey(name)) {
                throw new IllegalStateException("duplicate bind parameter " + name);
            }
            merged.put(name, value);
        });
        return new SqlFragment("(" + sql + ") AND (" + other.sql() + ")", merged);
    }
}
