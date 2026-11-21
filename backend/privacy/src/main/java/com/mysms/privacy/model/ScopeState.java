package com.mysms.privacy.model;

/**
 * The effective consent answer for one width of a subject and purpose: a row of privacy.consent_state without its
 * keys and version.
 *
 * @param scope   the width (or tenant) answered
 * @param allowed whether processing at that width is allowed
 * @param reason  why
 */
public record ScopeState(ConsentScope scope, boolean allowed, ConsentReason reason) {

    /** The answer that allows: a counting grant covers the width. */
    public static ScopeState allowed(ConsentScope scope) {
        return new ScopeState(scope, true, ConsentReason.CONSENT);
    }

    /** An answer that denies, for the reason given. */
    public static ScopeState denied(ConsentScope scope, ConsentReason reason) {
        return new ScopeState(scope, false, reason);
    }
}
