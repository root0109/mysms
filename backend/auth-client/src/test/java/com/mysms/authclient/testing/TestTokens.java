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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Signed access tokens for component tests: a throwaway RSA key pair whose public half is configured as
 * {@code mysms.auth.public-key}. Tokens carry the MySMS claims (sub, tid, ins, jti, amr, act); a tenant user's
 * permissions given as {@code perms}/{@code tperms} are signed as role claims ({@code rgr}, {@code azv}), as iam
 * issues them, and answered by {@link TestRoleData}. Platform staff and service tokens keep {@code perms}.
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

    /** A module's service token (iam's client-credentials grant: sub service:&lt;module&gt;, audience mysms-internal). */
    public String service(String module, String... permissions) {
        Instant now = Instant.now();
        return sign(new JWTClaimsSet.Builder().issuer(ISSUER).subject("service:" + module).audience("mysms-internal")
                .issueTime(Date.from(now)).expirationTime(Date.from(now.plus(Duration.ofMinutes(5))))
                .jwtID(UUID.randomUUID().toString()).claim("perms", List.of(permissions)).build());
    }

    /** A token with any claims, for negative tests. */
    public String custom(Map<String, Object> claims, Instant expiresAt) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(ISSUER).expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(15)))).jwtID(UUID.randomUUID().toString());
        claims.forEach(builder::claim);
        return sign(builder.build());
    }

    /**
     * A token signed exactly as given — a tenant user's {@code perms} are not turned into roles — for tests that the
     * verifier itself ignores a permission list in a tenant user's token.
     */
    public String asGiven(Map<String, Object> claims, Instant expiresAt) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().issuer(ISSUER).expirationTime(Date.from(expiresAt))
                .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(15)))).jwtID(UUID.randomUUID().toString());
        claims.forEach(builder::claim);
        return signAsGiven(builder.build());
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

    /**
     * A tenant user's permissions as iam states them (JAVA-CODING-RULES §16.2): the test's {@code perms} become role
     * grants — the {@code tperms} codes one tenant-wide role, the rest one role scoped to units — which
     * {@link TestRoleData} answers for. A token that names its roles itself, or no tenant, is signed as given.
     */
    private static JWTClaimsSet roleBased(JWTClaimsSet claims) {
        if (claims.getClaim("tid") == null || claims.getClaim("perms") == null || claims.getClaim("rgr") != null) {
            return claims;
        }
        List<String> listed = strings(claims, "perms");
        List<String> listedTenantWide = strings(claims, "tperms");
        // tenant-wide is what both lists grant, as AuthContext read them (a tenant-wide code beyond perms granted nothing)
        Set<String> tenantWide = new TreeSet<>();
        listedTenantWide.stream().filter(code -> covers(listed, code)).forEach(tenantWide::add);
        listed.stream().filter(code -> covers(listedTenantWide, code)).forEach(tenantWide::add);
        Set<String> units = new TreeSet<>(listed);
        units.removeAll(tenantWide);
        List<String> grants = new ArrayList<>();
        if (!tenantWide.isEmpty()) {
            grants.add(TestRoles.role(tenantWide) + "@tenant");
        }
        if (!units.isEmpty()) {
            grants.add(TestRoles.role(units).toString());
        }
        return new JWTClaimsSet.Builder(claims).claim("perms", null).claim("tperms", null).claim("rgr", grants)
                .claim("azv", 0L).build();
    }

    /** {@code *}, {@code <module>.*} and {@code <module>.<resource>.*} cover what they name, as in AuthContext. */
    private static boolean covers(List<String> codes, String code) {
        if (codes.contains(code) || codes.contains("*")) {
            return true;
        }
        for (int dot = code.indexOf('.'); dot > 0; dot = code.indexOf('.', dot + 1)) {
            if (codes.contains(code.substring(0, dot + 1) + "*")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> strings(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        List<String> strings = new ArrayList<>();
        if (value instanceof Collection<?> values) {
            values.forEach(v -> strings.add(String.valueOf(v)));
        }
        return strings;
    }

    private String sign(JWTClaimsSet given) {
        return signAsGiven(roleBased(given));
    }

    private String signAsGiven(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner((RSAPrivateKey) keys.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
