package com.mysms.identity.iam.config;

import java.time.Duration;

/**
 * `mysms.iam.*`: the token issuer and the one-time bootstrap of a new installation. Secrets (the signing key, the
 * bootstrap password) come only from the environment; {@link IamSecurityConfig} reads each value from its property or,
 * failing that, its environment variable.
 */
public record IamProperties(Token token, Bootstrap bootstrap) {

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
