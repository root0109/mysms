package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.authclient.testing.TestTokens;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenVerifierTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID SESSION = UUID.randomUUID();
    private final TestTokens tokens = new TestTokens();
    private final TokenVerifier verifier = JwtTokenVerifier.from(
            new AuthClientProperties(TestTokens.ISSUER, null, tokens.publicKeyPem(), null, List.of("master"), null, false));

    @Test
    void mapsAServiceTokenToAServicePrincipal() {
        AuthContext service = verifier.verify(tokens.service("finance", "org.institution.read")).context();
        assertThat(service.isService()).isTrue();
        assertThat(service.service()).contains("finance");
        assertThat(service.tenantId()).isEmpty();
        assertThat(service.permissions()).containsExactly("org.institution.read");
        assertThat(service.hasTenantWidePermission("org.institution.read")).isTrue();
        assertThat(service.userId()).isEqualTo(AuthContext.serviceUserId("finance"))
                .isNotEqualTo(AuthContext.serviceUserId("platform"));
        assertThat(service.withTenant(TENANT).tenantId()).contains(TENANT);
        AuthContext person = verifier.verify(tokens.tenantUser(TENANT, USER)).context();
        assertThat(person.isService()).isFalse();
        assertThatThrownBy(() -> person.withTenant(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesTokensThatMixAServiceAndAPerson() {
        Instant expiry = Instant.now().plusSeconds(300);
        for (Map<String, Object> claims : List.of(
                Map.<String, Object>of("sub", "service:finance", "aud", "mysms-internal", "tid", TENANT.toString()),
                Map.<String, Object>of("sub", "service:finance", "aud", "mysms"),
                Map.<String, Object>of("sub", "service:finance", "aud", List.of("mysms-internal", "mysms")),
                Map.<String, Object>of("sub", USER.toString(), "aud", "mysms-internal"),
                Map.<String, Object>of("sub", "service:Finance-1", "aud", "mysms-internal"),
                Map.<String, Object>of("sub", "service:finance", "aud", "mysms-internal", "pid", PERSON.toString()))) {
            assertThatThrownBy(() -> verifier.verify(tokens.custom(claims, expiry))).as(claims.toString())
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Test
    void mapsTheMySmsClaims() {
        UUID institution = UUID.randomUUID();
        UUID admin = UUID.randomUUID();
        String token = tokens.custom(Map.of("sub", USER.toString(), "tid", TENANT.toString(), "aud", "mysms",
                "perms", List.of("master.country.read"), "ins", List.of(institution.toString()),
                "act", Map.of("sub", admin.toString()), "pid", PERSON.toString(), "sid", SESSION.toString()),
                Instant.now().plusSeconds(300));
        VerifiedToken verified = verifier.verify(token);
        AuthContext context = verified.context();
        assertThat(context.userId()).isEqualTo(USER);
        assertThat(context.tenantId()).contains(TENANT);
        assertThat(context.institutionIds()).containsExactly(institution);
        assertThat(context.impersonatorId()).contains(admin);
        assertThat(context.personId()).contains(PERSON);
        assertThat(context.sessionId()).contains(SESSION);
        assertThat(context.activeInstitutionId()).isEmpty();
        assertThat(context.withActiveInstitution(institution).activeInstitutionId()).contains(institution);
        assertThat(context.permissions()).as("a tenant user's permissions come from roles, never from the token")
                .isEmpty();
        assertThat(verified.roleClaims()).as("the test token's permissions travel as a role").isPresent();
        assertThat(context.platformOperator()).isFalse();
        assertThat(verified.audiences()).containsExactly("mysms");
        assertThat(verified.expiresAt()).isAfter(Instant.now());
    }

    /** SEC-0: a tenant user's token is never read for a permission list; only service and platform tokens keep one. */
    @Test
    void aTenantUsersOwnPermissionListIsIgnored() {
        VerifiedToken raw = verifier.verify(tokens.asGiven(Map.of("sub", USER.toString(), "tid", TENANT.toString(),
                "aud", "mysms", "perms", List.of("*"), "tperms", List.of("*"), "ins", List.of("*")),
                Instant.now().plusSeconds(60)));
        assertThat(raw.context().permissions()).isEmpty();
        assertThat(raw.context().tenantWidePermissions()).isEmpty();
        assertThat(raw.roleClaims()).as("no rgr: no role, so no permission at all").isEmpty();
        assertThat(verifier.verify(tokens.platformOperator(USER, "master.country.create")).context().permissions())
                .containsExactly("master.country.create");
    }

    @Test
    void personAndSessionAreOptional() {
        AuthContext context = verifier.verify(tokens.tenantUser(TENANT, USER)).context();
        assertThat(context.personId()).isEmpty();
        assertThat(context.sessionId()).isEmpty();
    }

    @Test
    void recognisesPlatformOperatorsOnlyWithMfa() {
        assertThat(verifier.verify(tokens.platformOperator(USER, "master.country.create")).context().platformOperator()).isTrue();
        String noMfa = tokens.custom(Map.of("sub", USER.toString(), "aud", "mysms-platform-admin", "amr", List.of("pwd")),
                Instant.now().plusSeconds(60));
        assertThat(verifier.verify(noMfa).context().platformOperator()).isFalse();
    }

    @Test
    void rejectsExpiredForgedForeignAndMalformedTokens() {
        String expired = tokens.custom(Map.of("sub", USER.toString(), "aud", "mysms"), Instant.now().minusSeconds(3600));
        assertThatThrownBy(() -> verifier.verify(expired)).isInstanceOf(InvalidTokenException.class)
                .extracting("code").isEqualTo(InvalidTokenException.TOKEN_EXPIRED);
        assertThatThrownBy(() -> verifier.verify(TestTokens.forged(TENANT, USER))).isInstanceOf(InvalidTokenException.class)
                .extracting("code").isEqualTo(InvalidTokenException.UNAUTHENTICATED);
        String wrongAudience = tokens.custom(Map.of("sub", USER.toString(), "aud", "other"), Instant.now().plusSeconds(60));
        assertThatThrownBy(() -> verifier.verify(wrongAudience)).isInstanceOf(InvalidTokenException.class);
        String badSubject = tokens.custom(Map.of("sub", "not-a-uuid", "aud", "mysms"), Instant.now().plusSeconds(60));
        assertThatThrownBy(() -> verifier.verify(badSubject)).isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("claims");
        assertThatThrownBy(() -> verifier.verify("garbage")).isInstanceOf(InvalidTokenException.class);
        String eternal = tokens.withoutExpiry(Map.of("sub", USER.toString(), "tid", TENANT.toString(), "aud", "mysms"));
        assertThatThrownBy(() -> verifier.verify(eternal)).isInstanceOf(InvalidTokenException.class);
        String otherIssuer = new TestTokens().custom(Map.of("sub", USER.toString(), "aud", "mysms"),
                Instant.now().plusSeconds(60));
        assertThatThrownBy(() -> verifier.verify(otherIssuer)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void requiresAnIssuerAndKeys() {
        assertThatThrownBy(() -> JwtTokenVerifier.from(new AuthClientProperties(null, null, null, null, null, null, false)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> JwtTokenVerifier.from(new AuthClientProperties("https://x", null, null, null, null, null, false)))
                .isInstanceOf(IllegalStateException.class);
        for (String issuer : new String[] {"${MYSMS_AUTH_ISSUER}", "iam.example.com", "ftp://x", "https://", "http://a b"}) {
            assertThatThrownBy(() -> JwtTokenVerifier.from(new AuthClientProperties(issuer, "https://x/jwks", null, null,
                    null, null, false))).isInstanceOf(IllegalStateException.class).hasMessageContaining("MYSMS_AUTH_ISSUER");
        }
        AuthClientProperties badKey = new AuthClientProperties("https://x", null, "not a key", null, null, null, false);
        assertThatThrownBy(() -> JwtTokenVerifier.from(badKey)).isInstanceOf(IllegalStateException.class);
        assertThat(JwtTokenVerifier.from(new AuthClientProperties("https://x", "https://x/jwks", null, null, null, null, false)))
                .isNotNull();
        AuthClientProperties defaults = new AuthClientProperties("https://x", null, null, null, null, null, false);
        assertThat(defaults.audiences()).containsExactly("mysms", "mysms-platform-admin", "mysms-internal");
        assertThat(defaults.infrastructurePaths()).contains("/actuator/health");
        assertThat(defaults.modules()).isEmpty();
    }
}
