package com.mysms.privacy.model;

import java.util.Arrays;
import java.util.List;

/**
 * How far a consent reaches (privacy.consent_record.scope, privacy.consent_state.scope). A width purpose is decided at
 * the widths class &lt; institution &lt; inter_school &lt; public, where a grant covers every narrower width and a
 * refusal every wider one; a tenant-scoped purpose has the single scope tenant.
 */
public enum ConsentScope {

    TENANT("tenant", 0),
    CLASS("class", 1),
    INSTITUTION("institution", 2),
    INTER_SCHOOL("inter_school", 3),
    PUBLIC("public", 4);

    /** privacy.purpose.scope_kind of a purpose decided at widths. */
    public static final String WIDTH_KIND = "width";

    private static final List<ConsentScope> WIDTHS = List.of(CLASS, INSTITUTION, INTER_SCHOOL, PUBLIC);
    private static final List<ConsentScope> TENANT_ONLY = List.of(TENANT);

    private final String value;
    private final int width;

    ConsentScope(String value, int width) {
        this.value = value;
        this.width = width;
    }

    /** The value stored and sent: class, institution, inter_school, public or tenant. */
    public String value() {
        return value;
    }

    /** Whether this scope reaches no further than {@code other} (a scope is within itself). */
    public boolean within(ConsentScope other) {
        return width <= other.width;
    }

    /** The scope of a stored or sent value. */
    public static ConsentScope of(String value) {
        return Arrays.stream(values()).filter(s -> s.value.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown consent scope " + value));
    }

    /** The scopes a purpose of this scope_kind is decided at, narrowest first. */
    public static List<ConsentScope> forKind(String scopeKind) {
        return WIDTH_KIND.equals(scopeKind) ? WIDTHS : TENANT_ONLY;
    }
}
