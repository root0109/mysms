package com.mysms.privacyclient;

/**
 * Why a decision is what it is: privacy's reasons (design §4.2), and {@link #UNAVAILABLE} when privacy gave no
 * answer, which never allows.
 */
public enum ConsentReason {
    /** A consent that counts allows it. */
    CONSENT,
    /** The activity rests on another legal basis (legitimate use, contract…): no consent is asked. */
    NON_CONSENT_BASIS,
    REFUSED,
    NARROWER_GRANT,
    NOT_ANSWERED,
    EXPIRED,
    RECONSENT_REQUIRED,
    FORBIDDEN_FOR_MINOR,
    PURPOSE_DISABLED,
    /** privacy's register does not know the activity. */
    UNKNOWN_ACTIVITY,
    /** privacy could not be asked (down, too slow, or an answer that could not be read): fail closed. */
    UNAVAILABLE
}
