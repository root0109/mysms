package com.mysms.iam.config;

import java.time.Duration;

/**
 * `mysms.iam.*`: the token issuer, where GetMe reads the caller's person record, and the one-time bootstrap of a new
 * installation. Secrets (the signing key, the bootstrap password) come only from the environment. Built by
 * {@link IamSecurityConfig}, which reads each value from its property or, failing that, from the environment
 * variable of DEPLOYMENT.md §3.
 *
 * @param token         the access/refresh token issuer
 * @param peopleBaseUrl base URL of the people service (or the monolith) for the person in iam.GetMe; empty = omitted
 * @param bootstrap     first tenant and administrator of a new installation
 */
public record IamProperties(Token token, String peopleBaseUrl, Bootstrap bootstrap) {

    private static final Duration DEFAULT_ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration DEFAULT_REFRESH_TTL = Duration.ofDays(30);

    public IamProperties {
        token = token == null ? new Token(null, null, null, null, null, null) : token;
        bootstrap = bootstrap == null ? new Bootstrap(null, null, null) : bootstrap;
    }

    /**
     * @param issuer          `iss` of every token (the same value every module verifies: mysms.auth.issuer)
     * @param signingKey      PEM PKCS#8 RSA private key (2048 bits or more)
     * @param keyId           `kid` published in the JWKS; empty = the key's RFC 7638 thumbprint
     * @param accessTokenTtl  access-token lifetime (default 15 minutes)
     * @param refreshTokenTtl refresh-token lifetime (default 30 days; every use rotates it)
     * @param previousPublicKey PEM public key of the key being retired, still published in the JWKS so tokens it
     *                          signed stay valid until they expire (key rotation); empty when not rotating
     */
    public record Token(String issuer, String signingKey, String keyId, Duration accessTokenTtl,
                        Duration refreshTokenTtl, String previousPublicKey) {

        public Token {
            accessTokenTtl = accessTokenTtl == null ? DEFAULT_ACCESS_TTL : accessTokenTtl;
            refreshTokenTtl = refreshTokenTtl == null ? DEFAULT_REFRESH_TTL : refreshTokenTtl;
        }
    }

    /** All three set = create the tenant directory entry, an administrator role and its user once, if missing. */
    public record Bootstrap(String tenantCode, String adminUsername, String adminPassword) {

        public boolean enabled() {
            return notBlank(tenantCode) && notBlank(adminUsername) && notBlank(adminPassword);
        }

        private static boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }
}
