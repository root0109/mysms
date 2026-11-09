package com.mysms.authclient;

import java.net.URI;
import java.net.URISyntaxException;

/** The one check for a configured token issuer, shared by the verifier of every service and by the iam token issuer. */
public final class IssuerUrls {

    private IssuerUrls() {
    }

    /**
     * Refuses anything but an absolute http(s) URL, so an unset environment variable (which Spring leaves as the literal
     * {@code ${...}} placeholder) stops the service instead of silently becoming the issuer.
     */
    public static void require(String property, String issuer) {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException(property + " is required");
        }
        URI uri;
        try {
            uri = new URI(issuer);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(property + " must be an absolute http(s) URL, got: " + issuer, e);
        }
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null) {
            throw new IllegalStateException(property + " must be an absolute http(s) URL, got: " + issuer);
        }
    }
}
