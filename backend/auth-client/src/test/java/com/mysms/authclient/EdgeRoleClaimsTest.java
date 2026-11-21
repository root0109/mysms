package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.error.UnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * A tenant user's token: the permissions come from its role claims through iam's role data, never from a list in
 * the token, and the call is refused (503) when iam cannot answer.
 */
class EdgeRoleClaimsTest {

    private static final TestTokens TOKENS = new TestTokens();
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ROLE = UUID.randomUUID();
    private static final OperationRule READ_STUDENTS = new OperationRule("authtest.ListThings", "GET",
            "/v1/authtest/things", "sis.student.read", Set.of(OperationRule.BEARER), false);
    private static final OperationRule DELETE_STUDENTS = new OperationRule("authtest.DeleteThing", "DELETE",
            "/v1/authtest/things/{id}", "sis.student.delete", Set.of(OperationRule.BEARER), false);

    private boolean iamDown;

    private final RolePermissionSource iam = (tenant, roleIds, token) -> {
        if (iamDown) {
            throw new UnavailableException("iam is down");
        }
        return new RolePermissionSource.RolePermissions(1, Map.of(ROLE, Set.of("sis.student.read")), Set.of());
    };

    private final EdgeAuthorizer authorizer = new EdgeAuthorizer(JwtTokenVerifier.from(new AuthClientProperties(
            TestTokens.ISSUER, null, TOKENS.publicKeyPem(), null, List.of("authtest"), null, false)),
            new ClaimsPermissionChecker(), null, new RolePermissionResolver(iam, Duration.ofMinutes(1)));

    /** A tenant user's token whose own permission list says the opposite of its role (no role at all without roles). */
    private static String token(boolean withRoles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("aud", "mysms");
        claims.put("tid", TENANT.toString());
        claims.put("amr", List.of("pwd"));
        claims.put("ins", List.of("*"));
        claims.put("perms", List.of("sis.student.delete"));
        claims.put("rgr", withRoles ? List.of(ROLE + "@tenant") : List.of());
        claims.put("azv", 1);
        return TOKENS.custom(claims, Instant.now().plus(Duration.ofMinutes(5)));
    }

    private EdgeDecision decide(OperationRule rule, String token) {
        return authorizer.decide(rule, EdgeAuthorizer.BEARER_PREFIX + token, null, null);
    }

    @Test
    void theRolesDecideNotTheTokensOwnList() {
        String token = token(true);
        assertThat(decide(READ_STUDENTS, token)).isInstanceOf(EdgeDecision.Admitted.class);
        assertThat(decide(DELETE_STUDENTS, token)).isInstanceOfSatisfying(EdgeDecision.Refused.class,
                refused -> assertThat(refused.status()).isEqualTo(HttpStatus.FORBIDDEN));
        EdgeDecision.Admitted admitted = (EdgeDecision.Admitted) decide(READ_STUDENTS, token);
        assertThat(admitted.caller().orElseThrow().hasTenantWidePermission("sis.student.read")).isTrue();
        assertThat(admitted.roleClaims()).isPresent();
    }

    @Test
    void aTenantUsersOwnPermissionListGrantsNothing() {
        assertThat(decide(DELETE_STUDENTS, token(false))).isInstanceOfSatisfying(EdgeDecision.Refused.class,
                refused -> assertThat(refused.status()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void whenIamCannotAnswerTheCallIsRefusedNeverAdmitted() {
        iamDown = true;
        assertThat(decide(READ_STUDENTS, token(true))).isInstanceOfSatisfying(EdgeDecision.Refused.class,
                refused -> assertThat(refused.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void iamsRoleDataServiceAdmitsAPersonsOwnTokenOnly() {
        assertThat(authorizer.authenticate(null)).isInstanceOf(EdgeDecision.Refused.class);
        assertThat(authorizer.authenticate(EdgeAuthorizer.BEARER_PREFIX + TOKENS.service("lms")))
                .isInstanceOfSatisfying(EdgeDecision.Refused.class,
                        refused -> assertThat(refused.status()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(authorizer.authenticate(EdgeAuthorizer.BEARER_PREFIX + token(true)))
                .isInstanceOf(EdgeDecision.Admitted.class);
    }
}
