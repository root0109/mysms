package com.mysms.core.db;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The database role the current thread asks for while it opens a connection (the {@code ConnectionIdHolder} of the
 * owner's {@code common} module). A role is set only around the call that opens a transaction — the connection is
 * bound to the transaction from then on — and is always restored, so nothing leaks to the next task a pooled or
 * virtual thread runs. Without a role, connections come from the primary.
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
