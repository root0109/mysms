package com.mysms.core.context;

import java.util.Optional;
import java.util.UUID;

/** The user account a change is attributed to (audit columns, audit events): the caller of the current request. */
public final class Actor {

    private Actor() {
    }

    public static Optional<UUID> current() {
        return CallContextHolder.current().flatMap(CallContext::userId);
    }
}
