package com.mysms.authclient;

import java.time.Instant;
import java.util.Set;

/**
 * What a valid access token says, in MySMS's own types.
 *
 * @param audiences   `aud`
 * @param authMethods `amr` (e.g. pwd, mfa)
 */
public record VerifiedToken(AuthContext context, Instant expiresAt, Set<String> audiences, Set<String> authMethods) {

    public VerifiedToken {
        audiences = Set.copyOf(audiences);
        authMethods = Set.copyOf(authMethods);
    }
}
