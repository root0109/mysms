package com.mysms.authclient;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * `mysms.auth.*`.
 *
 * @param issuer         the OIDC issuer URI every token must carry (`iss`)
 * @param jwkSetUri      the issuer's JWKS endpoint (production); keys are cached and rotated by the verifier
 * @param publicKey      a PEM RSA public key instead of a JWKS (local development and tests only)
 * @param audiences      accepted `aud` values
 * @param modules        the modules this process serves (one for a microservice, all 21 for the monolith)
 * @param infrastructurePaths non-contract paths served without a token (health, info, and the issuer's public keys,
 *                            which only iam serves)
 */
@ConfigurationProperties("mysms.auth")
public record AuthClientProperties(String issuer, String jwkSetUri, String publicKey, List<String> audiences,
                                   List<String> modules, List<String> infrastructurePaths) {

    public static final String TENANT_AUDIENCE = "mysms";
    public static final String PLATFORM_AUDIENCE = "mysms-platform-admin";
    /** The issuer's JSON Web Key Set (public keys only), served by iam. */
    public static final String JWKS_PATH = "/oauth2/jwks";

    public AuthClientProperties {
        audiences = audiences == null || audiences.isEmpty()
                ? List.of(TENANT_AUDIENCE, PLATFORM_AUDIENCE)
                : List.copyOf(audiences);
        modules = modules == null ? List.of() : List.copyOf(modules);
        infrastructurePaths = infrastructurePaths == null
                ? List.of("/actuator/health", "/actuator/health/**", "/actuator/info", JWKS_PATH)
                : List.copyOf(infrastructurePaths);
    }
}
