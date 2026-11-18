package com.mysms.lms.business;

import java.net.URI;

/** Port to an external calendar provider an evaluator links over OAuth; adapters are chosen by configuration. */
public interface CalendarProvider {

    /** The provider code this adapter serves (lms.evaluator.calendar_provider). */
    String provider();

    /** Whether the deployment configured this adapter; an unconfigured one is never called. */
    boolean configured();

    /** The provider's authorization URL for one link attempt, carrying the state nonce. */
    URI authorizeUrl(String redirectUri, String state);
}
