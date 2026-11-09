package com.mysms.core.paging;

/**
 * A field clients may filter or sort on: its contract (JSON) name, the SQL column expression and its type.
 * The expression comes from code, never from a request. Sort expressions must be NOT NULL (keyset paging compares
 * them); wrap a nullable column in COALESCE when it is sortable.
 */
public record FieldSpec(String name, String column, ValueType type) {
}
