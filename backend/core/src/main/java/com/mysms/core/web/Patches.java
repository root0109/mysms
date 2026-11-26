package com.mysms.core.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.lang.Nullable;

/**
 * Applies a PATCH's {@code <Resource>Update} to a loaded entity, one explicit line per field (§7.2): a field that is
 * {@code null} (property absent from the request) leaves the column unchanged, {@code Optional.empty()} (JSON
 * {@code null}) clears it, a value sets it. Collects the names it applied, in call order, for the audit record. No
 * reflection: the service names each field and its setter. One instance per update; not thread-safe.
 */
public final class Patches {

    private final List<String> applied = new ArrayList<>();

    /** Sets {@code field}'s value (null when cleared) through {@code setter} when the request carries it. */
    public <T> void apply(String name, @Nullable Optional<T> field, Consumer<? super T> setter) {
        if (field != null) {
            setter.accept(field.orElse(null));
            applied.add(name);
        }
    }

    /**
     * The value a field carries — null when it is absent or cleared — for the shape checks a service runs on the
     * request before it applies it (a wire time, a 64-bit integer in a string).
     */
    @Nullable
    public static <T> T value(@Nullable Optional<T> field) {
        return field == null ? null : field.orElse(null);
    }

    /** The names of the fields applied so far, in call order. */
    public List<String> applied() {
        return List.copyOf(applied);
    }
}
