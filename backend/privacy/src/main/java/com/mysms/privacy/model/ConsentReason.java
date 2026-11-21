package com.mysms.privacy.model;

/**
 * Why the effective consent rule answered as it did for one width (privacy.consent_state.reason): CONSENT is the only
 * reason that allows; every other one denies.
 */
public enum ConsentReason {
    /** A counting grant covers the width. */
    CONSENT,
    /** A counting refusal reaches the width. */
    REFUSED,
    /** Another legal guardian of the minor granted only a narrower width (the most restrictive guardian decides). */
    NARROWER_GRANT,
    /** Nobody entitled has decided: silence never allows. */
    NOT_ANSWERED,
    /** The only decision there was has passed its valid_until. */
    EXPIRED,
    /** The grant predates a notice version that asks everyone again. */
    RECONSENT_REQUIRED,
    /** The purpose is never allowed for a minor (an unknown age counts as a minor). */
    FORBIDDEN_FOR_MINOR,
    /** The tenant does not process data for the purpose, or not at this width. */
    PURPOSE_DISABLED
}
