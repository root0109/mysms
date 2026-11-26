package com.mysms.sample.api.model;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.lang.Nullable;

/** A PATCH body of the pretend module "sample", in the shape of the modules' {@code <Resource>Update} classes (§7.2). */
public class SampleUpdate {

    private @Nullable Optional<@NotNull String> name;

    private @Nullable Optional<String> note;

    private @Nullable Optional<Sample> parent;

    public @Nullable Optional<String> getName() {
        return name;
    }

    public @Nullable Optional<String> getNote() {
        return note;
    }

    public @Nullable Optional<Sample> getParent() {
        return parent;
    }
}
