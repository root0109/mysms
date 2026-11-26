package com.mysms.identity.iam.business;

import com.mysms.identity.iam.config.IamProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Signs MySMS access tokens (RS256) and publishes the public key as a JWKS — the issuer half of the token contract
 * every module's auth-client verifies (iss, aud "mysms", exp required; sub, tid, perms, ins, roles, pid, sid, jti,
 * amr). The private key exists only in memory, loaded from the environment; startup fails without a usable key.
 */
public final class TokenIssuer {

    public static final String TENANT_AUDIENCE = "mysms";
    /** Service tokens (client credentials) are for MySMS services only. */
    public static final String SERVICE_AUDIENCE = com.mysms.authclient.AuthClientProperties.INTERNAL_AUDIENCE;
    private static final String SERVICE_SUBJECT_PREFIX = com.mysms.authclient.AuthContext.SERVICE_SUBJECT_PREFIX;
    private static final int MIN_KEY_BITS = 2048;

    private final String issuer;
    private final RSAKey publicJwk;
    private final JWKSet published;
    private final RSASSASigner signer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final Clock clock;

    private TokenIssuer(String issuer, RSAKey publicJwk, JWKSet published, RSASSASigner signer, Duration accessTokenTtl,
                        Duration refreshTokenTtl, Clock clock) {
        this.issuer = issuer;
        this.publicJwk = publicJwk;
        this.published = published;
        this.signer = signer;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.clock = clock;
    }

    /** Validates the configuration and loads the key; any problem stops the service from starting. */
    public static TokenIssuer from(IamProperties.Token config, Clock clock) {
        com.mysms.authclient.IssuerUrls.require("mysms.iam.token.issuer (MYSMS_AUTH_ISSUER)",
                config.issuer());
        if (config.signingKey() == null || config.signingKey().isBlank()) {
            throw new IllegalStateException("mysms.iam.token.signing-key is required (MYSMS_IAM_SIGNING_KEY: a PEM PKCS#8 RSA"
                    + " private key)");
        }
        if (config.accessTokenTtl().isNegative() || config.accessTokenTtl().isZero()
                || config.refreshTokenTtl().compareTo(config.accessTokenTtl()) <= 0) {
            throw new IllegalStateException("token lifetimes must be positive and the refresh lifetime longer than the access"
                    + " lifetime");
        }
        RSAPrivateCrtKey privateKey = privateKey(config.signingKey());
        if (privateKey.getModulus().bitLength() < MIN_KEY_BITS) {
            throw new IllegalStateException("mysms.iam.token.signing-key must be at least " + MIN_KEY_BITS + " bits");
        }
        RSAPublicKey publicKey = publicKey(privateKey.getModulus(), privateKey.getPublicExponent());
        try {
            RSAKey withoutId = new RSAKey.Builder(publicKey).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).build();
            String keyId = config.keyId() == null || config.keyId().isBlank()
                    ? withoutId.computeThumbprint().toString()
                    : config.keyId();
            RSAKey publicJwk = new RSAKey.Builder(withoutId).keyID(keyId).build();
            List<com.nimbusds.jose.jwk.JWK> keys = new java.util.ArrayList<>(List.of(publicJwk));
            if (config.previousPublicKey() != null && !config.previousPublicKey().isBlank()) {
                RSAKey previous = new RSAKey.Builder(previousKey(config.previousPublicKey())).keyUse(KeyUse.SIGNATURE)
                        .algorithm(JWSAlgorithm.RS256).build();
                previous = new RSAKey.Builder(previous).keyID(previous.computeThumbprint().toString()).build();
                if (!previous.getKeyID().equals(keyId)) {
                    keys.add(previous);
                }
            }
            return new TokenIssuer(config.issuer(), publicJwk, new JWKSet(keys), new RSASSASigner(privateKey),
                    config.accessTokenTtl(), config.refreshTokenTtl(), clock);
        } catch (JOSEException e) {
            throw new IllegalStateException("cannot use mysms.iam.token.signing-key", e);
        }
    }

    /** A signed access token for the claims, valid for the access-token lifetime from now. */
    public IssuedToken issue(AccessClaims claims) {
        return issue(claims, List.of("pwd"));
    }

    /** As {@link #issue(AccessClaims)}, naming how the user authenticated (`amr`: pwd, mfa). */
    public IssuedToken issue(AccessClaims claims, List<String> authMethods) {
        return sign(claims, authMethods, accessTokenTtl, null);
    }

    /**
     * An access token for an administrator acting as the claims' user (iam.StartImpersonation): RFC 8693 `act`
     * names the administrator (`act.sub`), the lifetime is the given one (at most the access-token lifetime), and no
     * refresh token goes with it.
     */
    public IssuedToken issueActing(AccessClaims claims, List<String> authMethods, UUID actor, Duration lifetime) {
        Duration ttl = lifetime.compareTo(accessTokenTtl) < 0 ? lifetime : accessTokenTtl;
        return sign(claims, authMethods, ttl, actor);
    }

    private IssuedToken sign(AccessClaims claims, List<String> authMethods, Duration ttl, UUID actor) {
        Instant now = clock.instant();
        Instant expires = now.plus(ttl);
        String tokenId = UUID.randomUUID().toString();
        JWTClaimsSet.Builder set = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(claims.userId().toString())
                .audience(TENANT_AUDIENCE)
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(expires))
                .jwtID(tokenId)
                .claim("tid", claims.tenantId().toString())
                .claim("sid", claims.sessionId().toString())
                .claim("amr", List.copyOf(authMethods))
                .claim("ins", institutionsClaim(claims))
                .claim("roles", claims.roles())
                .claim("rgr", claims.authorization().roleGrants())
                .claim("dg", claims.authorization().directGrants())
                .claim("dd", claims.authorization().denies())
                .claim("azv", claims.authorization().version());
        claims.authorization().abilities().ifPresent(abilities -> set.claim("abl", abilities.stream().sorted().toList()));
        claims.personId().ifPresent(person -> set.claim("pid", person.toString()));
        if (actor != null) {
            set.claim("act", Map.of("sub", actor.toString()));
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(publicJwk.getKeyID()).build(),
                set.build());
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("cannot sign an access token", e);
        }
        return new IssuedToken(jwt.serialize(), tokenId, expires);
    }

    /**
     * A service token for a module's client (client credentials): {@code sub =
     * service:<module>}, audience {@code mysms-internal}, the client's permissions, no tenant and no person — the
     * edge admits it only on operations of audience internal, in the tenant the call names.
     */
    public IssuedToken issueService(String module, List<String> permissions, Duration ttl) {
        Instant now = clock.instant();
        Instant expires = now.plus(ttl);
        String tokenId = UUID.randomUUID().toString();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(SERVICE_SUBJECT_PREFIX + module)
                .audience(SERVICE_AUDIENCE)
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(expires))
                .jwtID(tokenId)
                .claim("perms", permissions.stream().sorted().toList())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(publicJwk.getKeyID()).build(),
                claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("cannot sign a service token", e);
        }
        return new IssuedToken(jwt.serialize(), tokenId, expires);
    }

    /** `ins`: the scoped institutions, led by `*` when a tenant-wide grant covers every institution. */
    private static List<String> institutionsClaim(AccessClaims claims) {
        List<String> ins = new java.util.ArrayList<>(claims.institutions().stream().map(UUID::toString).sorted().toList());
        if (claims.allInstitutions()) {
            ins.add(0, "*");
        }
        return ins;
    }

    /**
     * The public JSON Web Key Set (RFC 7517) every module's verifier reads from /oauth2/jwks: the signing key and,
     * during a rotation, the retiring key (verifiers pick by `kid`).
     */
    public Map<String, Object> jwks() {
        return published.toJSONObject(true);
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    public Duration refreshTokenTtl() {
        return refreshTokenTtl;
    }

    public String keyId() {
        return publicJwk.getKeyID();
    }

    private static RSAPrivateCrtKey privateKey(String pem) {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            if (KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)))
                    instanceof RSAPrivateCrtKey key) {
                return key;
            }
            throw new IllegalStateException("mysms.iam.token.signing-key must be an RSA private key with CRT parameters");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new IllegalStateException("mysms.iam.token.signing-key is not a PEM PKCS#8 RSA private key", e);
        }
    }

    private static RSAPublicKey previousKey(String pem) {
        String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalStateException("mysms.iam.token.previous-public-key is not a PEM RSA public key", e);
        }
    }

    private static RSAPublicKey publicKey(BigInteger modulus, BigInteger exponent) {
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("cannot derive the public key", e);
        }
    }

    /**
     * @param token     the serialized JWT
     * @param tokenId   its `jti`
     * @param expiresAt its `exp`
     */
    public record IssuedToken(String token, String tokenId, Instant expiresAt) {
    }
}
