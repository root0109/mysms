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
 * @param apiDocs        serve the OpenAPI document and Swagger UI without a token ({@code /v3/api-docs*},
 *                       {@code /swagger-ui*}). Off by default: the document maps every operation and permission, so
 *                       production keeps it closed (data security first); the component tests and local development
 *                       turn it on (MYSMS_API_DOCS)
 */
@ConfigurationProperties("mysms.auth")
public record AuthClientProperties(String issuer, String jwkSetUri, String publicKey, List<String> audiences,
                                   List<String> modules, List<String> infrastructurePaths, boolean apiDocs) {

    /** The springdoc paths {@link #apiDocs} opens: the document and the Swagger UI it feeds. */
    public static final List<String> API_DOC_PATHS = List.of("/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**",
            "/swagger-ui.html", "/swagger-ui/**");

    public static final String TENANT_AUDIENCE = "mysms";
    public static final String PLATFORM_AUDIENCE = "mysms-platform-admin";
    /** The issuer's JSON Web Key Set (public keys only), served by iam. */
    public static final String JWKS_PATH = "/oauth2/jwks";

    public AuthClientProperties {
        audiences = audiences == null || audiences.isEmpty()
                ? List.of(TENANT_AUDIENCE, PLATFORM_AUDIENCE)
                : List.copyOf(audiences);
        modules = modules == null ? List.of() : List.copyOf(modules);
        List<String> paths = infrastructurePaths == null
                ? List.of("/actuator/health", "/actuator/health/**", "/actuator/info", JWKS_PATH)
                : List.copyOf(infrastructurePaths);
        infrastructurePaths = apiDocs
                ? List.copyOf(java.util.stream.Stream.concat(paths.stream(), API_DOC_PATHS.stream()).toList())
                : paths;
    }
}
