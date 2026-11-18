package com.mysms.sample.api.model;

import org.springframework.lang.Nullable;

/** A contract DTO of a pretend module "sample", in the shape of the modules' {@code api.model} classes. */
public class Sample {

    private String name;

    private @Nullable String note;

    public String getName() {
        return name;
    }

    public @Nullable String getNote() {
        return note;
    }
}
