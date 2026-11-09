package com.mysms.authclient;

/**
 * Verifies a bearer access token locally (signature against the issuer's keys, issuer, expiry, audience) — no network
 * call per request. The interface is MySMS's; the implementation behind it is replaceable with the issuer.
 */
public interface TokenVerifier {

    /** The verified token, or {@link InvalidTokenException}. */
    VerifiedToken verify(String token);
}
