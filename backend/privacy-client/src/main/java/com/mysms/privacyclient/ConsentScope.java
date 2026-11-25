package com.mysms.privacyclient;

/**
 * How far the processing reaches: the width a consent names (class &lt; institution &lt; inter_school &lt; public)
 * for a width purpose, or {@link #TENANT} for a purpose decided once for the whole school.
 */
public enum ConsentScope {
    TENANT,
    CLASS,
    INSTITUTION,
    INTER_SCHOOL,
    PUBLIC
}
