package com.mysms.privacyclient;

import java.util.Objects;

/**
 * Whether personal data of one subject may be used for an activity at a scope.
 *
 * @param allowed       whether it may
 * @param reason        why
 * @param legalBasis    the activity's legal basis in privacy's register (consent, legitimate_use…); null when unknown
 * @param stateVersion  the subject's consent-state version the answer was read at; -1 for an answer privacy did not
 *                      give
 */
public record ConsentDecision(boolean allowed, ConsentReason reason, String legalBasis, long stateVersion) {

    /** What privacy's register calls a consent-based activity. */
    static final String CONSENT_BASIS = "consent";

    public ConsentDecision {
        Objects.requireNonNull(reason, "reason");
    }

    /** The fail-closed answer when privacy cannot be asked. */
    public static ConsentDecision unavailable() {
        return new ConsentDecision(false, ConsentReason.UNAVAILABLE, null, -1);
    }

    /** The answer for an activity on another legal basis, given without asking privacy. */
    static ConsentDecision nonConsent(String legalBasis) {
        return new ConsentDecision(true, ConsentReason.NON_CONSENT_BASIS, legalBasis, -1);
    }
}
