package com.mysms.core.db;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The database role the current thread asks for while it opens a connection. It is set only around opening a
 * transaction and always restored, so nothing leaks to the next task on a pooled thread; without one, the primary.
 */
public final class DatabaseRoleHolder {

    private static final ThreadLocal<DatabaseRole> ROLE = new ThreadLocal<>();

    private DatabaseRoleHolder() {
    }

    /** The role asked for, if any. */
    public static Optional<DatabaseRole> current() {
        return Optional.ofNullable(ROLE.get());
    }

    /** Runs {@code work} with {@code role} asked for, then restores what was asked for before. */
    public static <T> T callWith(DatabaseRole role, Supplier<T> work) {
        DatabaseRole previous = ROLE.get();
        ROLE.set(role);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                ROLE.remove();
            } else {
                ROLE.set(previous);
            }
        }
    }
}
