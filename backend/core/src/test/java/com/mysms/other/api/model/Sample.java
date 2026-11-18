package com.mysms.other.api.model;

import org.springframework.lang.Nullable;

/**
 * A contract DTO of a second pretend module "other" with the same simple name as {@code sample}'s but other required
 * fields: in one process with both, neither may decide the other's schema.
 */
public class Sample {

    private @Nullable String name;

    private String note;

    public @Nullable String getName() {
        return name;
    }

    public String getNote() {
        return note;
    }
}
