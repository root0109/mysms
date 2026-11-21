package com.mysms.authclient;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * What a valid access token says, in MySMS's own types.
 *
 * @param audiences   `aud`
 * @param authMethods `amr` (e.g. pwd, mfa)
 * @param roleClaims  a person's role-based claims ({@code rgr}, {@code dg}, {@code dd}, {@code azv}, {@code abl}),
 *                    when the issuer sent them; empty for service tokens
 */
public record VerifiedToken(AuthContext context, Instant expiresAt, Set<String> audiences, Set<String> authMethods,
                            Optional<RoleClaims> roleClaims) {

    public VerifiedToken {
        audiences = Set.copyOf(audiences);
        authMethods = Set.copyOf(authMethods);
        roleClaims = roleClaims == null ? Optional.empty() : roleClaims;
    }
}
