package com.mysms.iam.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.AuthClientProperties;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.TokenVerifier;
import com.mysms.authclient.VerifiedToken;
import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.iam.config.IamProperties;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The issuer signs exactly what every module's verifier (auth-client) accepts and maps. */
class TokenIssuerTest {

    private final TestTokens keys = new TestTokens();
    private final TokenVerifier verifier = JwtTokenVerifier.from(new AuthClientProperties(TestTokens.ISSUER, null,
            keys.publicKeyPem(), null, List.of("iam"), null, false));

    private TokenIssuer issuer(String keyId) {
        return TokenIssuer.from(new IamProperties.Token(TestTokens.ISSUER, keys.privateKeyPem(), keyId, null, null,
                null),
                Clock.systemUTC());
    }

    @Test
    void issuedTokensVerifyAndCarryEveryClaim() {
        UUID user = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        TokenIssuer issuer = issuer(null);
        TokenIssuer.IssuedToken issued = issuer.issue(new AccessClaims(user, tenant, Optional.of(person), session,
                Set.of("iam.*", "people.person.read_mine"), Set.of(institution), List.of("teacher@tenant"), false,
                Set.of("people.person.read_mine"), new AccessClaims.Authorization(List.of(role + "@tenant"),
                        List.of("hr.staff.read"), List.of("iam.role.delete"), 7, Optional.of(Set.of("iam.role.read")))));
        VerifiedToken verified = verifier.verify(issued.token());
        AuthContext context = verified.context();
        assertThat(context.userId()).isEqualTo(user);
        assertThat(context.tenantId()).contains(tenant);
        assertThat(context.personId()).contains(person);
        assertThat(context.sessionId()).contains(session);
        assertThat(context.institutionIds()).containsExactly(institution);
        assertThat(context.tokenId()).isEqualTo(issued.tokenId());
        assertThat(context.permissions()).as("no permission list: the edge resolves the roles (SEC-0)").isEmpty();
        assertThat(context.tenantWidePermissions()).isEmpty();
        assertThat(verified.roleClaims().orElseThrow().roleIds()).containsExactly(role);
        assertThat(context.platformOperator()).isFalse();
        assertThat(verified.audiences()).containsExactly("mysms");
        assertThat(verified.authMethods()).containsExactly("pwd");
        assertThat(Duration.between(java.time.Instant.now(), issued.expiresAt())).isBetween(Duration.ofMinutes(14),
                Duration.ofMinutes(15));
        String header = new String(Base64.getUrlDecoder().decode(issued.token().split("\\.")[0]), StandardCharsets.UTF_8);
        assertThat(header).contains("\"kid\":\"" + issuer.keyId() + "\"").contains("RS256");
        String payload = new String(Base64.getUrlDecoder().decode(issued.token().split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).doesNotContain("\"perms\"").doesNotContain("\"tperms\"");
        assertThat(payload).as("the role-based claims (SEC-0)").contains("\"rgr\":[\"" + role + "@tenant\"]")
                .contains("\"dg\":[\"hr.staff.read\"]").contains("\"dd\":[\"iam.role.delete\"]")
                .contains("\"azv\":7").contains("\"abl\":[\"iam.role.read\"]");
    }

    @Test
    void aTokenWithoutAPersonHasNoPid() {
        TokenIssuer.IssuedToken issued = issuer("k1").issue(new AccessClaims(UUID.randomUUID(), UUID.randomUUID(),
                Optional.empty(), UUID.randomUUID(), Set.of("iam.role.read"), Set.of(), List.of(), true,
                Set.of("iam.role.read"), new AccessClaims.Authorization(List.of(), List.of(), List.of(), 0,
                        Optional.empty())));
        assertThat(verifier.verify(issued.token()).context().coversInstitution(UUID.randomUUID())).isTrue();
        assertThat(verifier.verify(issued.token()).context().personId()).isEmpty();
    }

    @Test
    void theJwksPublishesOnlyThePublicKey() {
        TokenIssuer issuer = issuer("key-2026");
        Map<String, Object> jwks = issuer.jwks();
        assertThat(issuer.keyId()).isEqualTo("key-2026");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keysList = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(keysList).hasSize(1);
        assertThat(keysList.get(0)).containsEntry("kid", "key-2026").containsEntry("kty", "RSA")
                .containsEntry("use", "sig").containsEntry("alg", "RS256").containsKeys("n", "e")
                .doesNotContainKeys("d", "p", "q", "dp", "dq", "qi");
        assertThat(issuer("").keyId()).isNotBlank();
    }

    @Test
    void duringRotationTheRetiringKeyStaysPublished() {
        TestTokens retiring = new TestTokens();
        TokenIssuer issuer = TokenIssuer.from(new IamProperties.Token(TestTokens.ISSUER, keys.privateKeyPem(), "new",
                null, null, retiring.publicKeyPem()), Clock.systemUTC());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> published = (List<Map<String, Object>>) issuer.jwks().get("keys");
        assertThat(published).hasSize(2);
        assertThat(published.get(0)).containsEntry("kid", "new");
        assertThat(published.get(1)).doesNotContainKey("d").containsKey("kid");
        assertThatThrownBy(() -> TokenIssuer.from(new IamProperties.Token(TestTokens.ISSUER, keys.privateKeyPem(), null,
                null, null, "garbage"), Clock.systemUTC())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("previous-public-key");
    }

    @Test
    void refusesToStartWithoutAUsableConfiguration() {
        Clock clock = Clock.systemUTC();
        assertThatThrownBy(() -> TokenIssuer.from(new IamProperties.Token(null, keys.privateKeyPem(), null, null, null, null),
                clock)).isInstanceOf(IllegalStateException.class).hasMessageContaining("issuer");
        assertThatThrownBy(() -> TokenIssuer.from(new IamProperties.Token("${MYSMS_AUTH_ISSUER}", keys.privateKeyPem(), null,
                null, null, null), clock)).isInstanceOf(IllegalStateException.class).hasMessageContaining("absolute");
        assertThatThrownBy(() -> TokenIssuer.from(new IamProperties.Token(TestTokens.ISSUER, " ", null, null, null, null),
                clock)).isInstanceOf(IllegalStateException.class).hasMessageContaining("signing-key");
        assertThatThrownBy(() -> TokenIssuer.from(new IamProperties.Token(TestTokens.ISSUER, "not a key", null, null,
                null, null), clock)).isInstanceOf(IllegalStateException.class).hasMessageContaining("PKCS#8");
        assertThatThrownBy(() -> TokenIssuer.from(new IamProperties.Token(TestTokens.ISSUER, keys.privateKeyPem(), null,
                Duration.ofHours(2), Duration.ofHours(1), null), clock)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lifetimes");
        assertThatThrownBy(() -> TokenIssuer.from(new IamProperties.Token(TestTokens.ISSUER, keys.privateKeyPem(), null,
                Duration.ZERO, null, null), clock)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void defaultsAndBootstrapSwitch() {
        IamProperties properties = new IamProperties(null, null);
        assertThat(properties.token().accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.token().refreshTokenTtl()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.bootstrap().enabled()).isFalse();
        assertThat(new IamProperties.Bootstrap("acme", "admin", " ").enabled()).isFalse();
        assertThat(new IamProperties.Bootstrap("acme", "admin", "long enough secret").enabled()).isTrue();
    }
}
