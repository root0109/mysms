package com.mysms.demoseed.client;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Filters in the API's grammar ({@code field = "value" AND …}), quoting values safely. */
public final class Filter {

    private Filter() {
    }

    public static String eq(String field, Object value) {
        return field + " = \"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static String and(String... terms) {
        return Arrays.stream(terms).collect(Collectors.joining(" AND "));
    }
}
