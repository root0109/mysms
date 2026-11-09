package com.mysms.authclient.testing;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Signed access tokens for component tests: a throwaway RSA key pair whose public half is configured as
 * {@code mysms.auth.public-key}. Tokens carry the MySMS claims (sub, tid, perms, ins, jti, amr, act).
 */
public final class TestTokens {

    public static final String ISSUER = "https://iam.test.mysms";

    private final KeyPair keys;

    public TestTokens() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keys = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The PEM public key for {@code mysms.auth.public-key}. */
    public String publicKeyPem() {
        return "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder().encodeToString(keys.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
    }

    /**
     * The PEM (PKCS#8) private key, so a component test can configure the iam token issuer with the same key pair the
     * verifier trusts. Test keys only — production keys come from the environment, never from code.
     */
    public String privateKeyPem() {
        return "-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder().encodeToString(keys.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }

    /** A tenant user's token. */
    public String tenantUser(UUID tenant, UUID user, String... permissions) {
        return sign(claims(user, "mysms", List.of("pwd"), permissions).claim("tid", tenant.toString()).build());
    }

    /** A platform operator's token (audience mysms-platform-admin, MFA). */
    public String platformOperator(UUID user, String... permissions) {
        return sign(claims(user, "mysms-platform-admin", List.of("pwd", "mfa"), permissions).build());
    }

    /** A token with any claims, for negative tests. */
    public String custom(Map<String, Object> claims, Instant expiresAt) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(ISSUER).expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(15)))).jwtID(UUID.randomUUID().toString());
        claims.forEach(builder::claim);
        return sign(builder.build());
    }

    /** A correctly signed token that never expires (no exp claim) — must be rejected. */
    public String withoutExpiry(Map<String, Object> claims) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(ISSUER).issueTime(new Date())
                .jwtID(UUID.randomUUID().toString());
        claims.forEach(builder::claim);
        return sign(builder.build());
    }

    /** A token signed by another key (forged). */
    public static String forged(UUID tenant, UUID user, String... permissions) {
        return new TestTokens().tenantUser(tenant, user, permissions);
    }

    private static JWTClaimsSet.Builder claims(UUID user, String audience, List<String> amr, String... permissions) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(user.toString())
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofMinutes(15))))
                .jwtID(UUID.randomUUID().toString())
                .claim("amr", amr)
                .claim("perms", List.of(permissions));
    }

    private String sign(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) keys.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
