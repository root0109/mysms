package com.mysms.authclient.internal;

import com.mysms.authclient.AuthClientProperties;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.InvalidTokenException;
import com.mysms.authclient.IssuerUrls;
import com.mysms.authclient.TokenVerifier;
import com.mysms.authclient.VerifiedToken;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * {@link TokenVerifier} for JWT access tokens of an OIDC issuer (today the iam module; later any issuer). Verifies
 * the signature against the issuer's JWKS (cached, rotated), the issuer, expiry (60 s skew) and audience, then maps
 * the MySMS claims: sub, tid, perms, ins, jti, act, amr, pid, sid. Implementation detail of auth-client.
 */
public final class JwtTokenVerifier implements TokenVerifier {

    private static final String MFA = "mfa";
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);
    private static final Duration JWKS_TIMEOUT = Duration.ofSeconds(5);

    private final JwtDecoder decoder;

    public JwtTokenVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    /** Builds the verifier for the configured issuer (JWKS in production; a PEM key only for development/tests). */
    public static JwtTokenVerifier from(AuthClientProperties properties) {
        IssuerUrls.require("mysms.auth.issuer (MYSMS_AUTH_ISSUER)", properties.issuer());
        NimbusJwtDecoder decoder;
        if (properties.jwkSetUri() != null && !properties.jwkSetUri().isBlank()) {
            // bounded network waits: a slow issuer must not hang request threads (keys are cached between fetches)
            SimpleClientHttpRequestFactory http = new SimpleClientHttpRequestFactory();
            http.setConnectTimeout(JWKS_TIMEOUT);
            http.setReadTimeout(JWKS_TIMEOUT);
            decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).restOperations(new RestTemplate(http)).build();
        } else if (properties.publicKey() != null && !properties.publicKey().isBlank()) {
            decoder = NimbusJwtDecoder.withPublicKey(rsaPublicKey(properties.publicKey())).build();
        } else {
            throw new IllegalStateException("mysms.auth.jwk-set-uri (or, for development, mysms.auth.public-key) is required");
        }
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null
                && jwt.getAudience().stream().anyMatch(properties.audiences()::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "The token audience is not accepted.", null));
        // every token must expire: tokens without exp are rejected (the default validator would accept them)
        JwtTimestampValidator expiry = new JwtTimestampValidator(CLOCK_SKEW);
        expiry.setAllowEmptyExpiryClaim(false);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtIssuerValidator(properties.issuer()), expiry,
                audience));
        return new JwtTokenVerifier(decoder);
    }

    @Override
    public VerifiedToken verify(String token) {
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtValidationException e) {
            boolean expired = e.getErrors().stream()
                    .map(error -> Optional.ofNullable(error.getDescription()).orElse(""))
                    .anyMatch(description -> description.toLowerCase(java.util.Locale.ROOT).contains("expired"));
            throw expired
                    ? new InvalidTokenException(InvalidTokenException.TOKEN_EXPIRED, "The access token has expired.", e)
                    : new InvalidTokenException(InvalidTokenException.UNAUTHENTICATED, "The access token is not valid.", e);
        } catch (JwtException e) {
            throw new InvalidTokenException(InvalidTokenException.UNAUTHENTICATED, "The access token is not valid.", e);
        }
        return toVerified(jwt);
    }

    private static VerifiedToken toVerified(Jwt jwt) {
        try {
            Set<String> audiences = Set.copyOf(Optional.ofNullable(jwt.getAudience()).orElse(List.of()));
            Set<String> amr = Set.copyOf(Optional.ofNullable(jwt.getClaimAsStringList("amr")).orElse(List.of()));
            boolean platform = audiences.contains(AuthClientProperties.PLATFORM_AUDIENCE) && amr.contains(MFA);
            Set<UUID> institutions = new HashSet<>();
            List<String> ins = Optional.ofNullable(jwt.getClaimAsStringList("ins")).orElse(List.of());
            boolean allInstitutions = ins.contains(AuthContext.ALL);
            ins.stream().filter(i -> !AuthContext.ALL.equals(i)).forEach(i -> institutions.add(UUID.fromString(i)));
            Optional<UUID> impersonator = Optional.ofNullable(jwt.getClaimAsMap("act"))
                    .map(act -> act.get("sub"))
                    .map(Object::toString)
                    .map(UUID::fromString);
            String tokenId = jwt.getId();
            String subject = jwt.getSubject();
            if (tokenId == null || tokenId.isBlank() || subject == null || jwt.getExpiresAt() == null) {
                throw new IllegalArgumentException("sub, jti and exp are required");
            }
            AuthContext context = new AuthContext(
                    UUID.fromString(subject),
                    Optional.ofNullable(jwt.getClaimAsString("tid")).map(UUID::fromString),
                    institutions,
                    Optional.empty(),
                    Set.copyOf(Optional.ofNullable(jwt.getClaimAsStringList("perms")).orElse(List.of())),
                    tokenId,
                    impersonator,
                    platform,
                    Optional.ofNullable(jwt.getClaimAsString("pid")).map(UUID::fromString),
                    Optional.ofNullable(jwt.getClaimAsString("sid")).map(UUID::fromString),
                    allInstitutions,
                    // absent claim: no tenant-wide permission at all (fail closed)
                    Set.copyOf(Optional.ofNullable(jwt.getClaimAsStringList("tperms")).orElse(List.of())));
            return new VerifiedToken(context, jwt.getExpiresAt(), audiences, amr);
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException(InvalidTokenException.UNAUTHENTICATED, "The access token claims are not valid.", e);
        }
    }

    static RSAPublicKey rsaPublicKey(String pem) {
        String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new IllegalStateException("mysms.auth.public-key is not a PEM RSA public key", e);
        }
    }

    /** Visible for tests: claims-to-context mapping without a signature. */
    static VerifiedToken map(Map<String, Object> headers, Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("t").headers(h -> h.putAll(headers)).claims(c -> c.putAll(claims));
        return toVerified(builder.build());
    }
}
