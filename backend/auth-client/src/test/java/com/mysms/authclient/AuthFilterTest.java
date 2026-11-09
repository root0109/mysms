package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ProblemResponseWriter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import java.net.URI;
import tools.jackson.databind.json.JsonMapper;

class AuthFilterTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID INSTITUTION = UUID.randomUUID();

    private final TestTokens tokens = new TestTokens();
    private final AuthFilter filter = new AuthFilter(OperationTable.load(List.of("master", "org")),
            JwtTokenVerifier.from(new AuthClientProperties(TestTokens.ISSUER, null, tokens.publicKeyPem(), null,
                    List.of("master", "org"), null)),
            new ClaimsPermissionChecker(), new ProblemResponseWriter(JsonMapper.builder().build()),
            List.of("/actuator/health", "/actuator/health/**"));

    /** What the downstream chain saw. */
    private final AtomicReference<AuthContext> seen = new AtomicReference<>();
    private final AtomicReference<CallContext> seenCall = new AtomicReference<>();
    private final AtomicReference<Object> seenOperation = new AtomicReference<>();
    private final AtomicReference<java.util.Set<String>> seenMethods = new AtomicReference<>();

    @AfterEach
    void tearDown() {
        CallContextHolder.clear();
    }

    private MockHttpServletResponse call(String method, String path, String authorization, String institution) throws Exception {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        if (institution != null) {
            request.addHeader(AuthFilter.INSTITUTION_HEADER, institution);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        seen.set(null);
        seenOperation.set(null);
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                seen.set(AuthContext.current().orElse(null));
                seenMethods.set(AuthContext.currentAuthMethods());
                seenCall.set(CallContextHolder.current().orElse(null));
                seenOperation.set(req.getAttribute(com.mysms.core.error.Problems.OPERATION_ATTRIBUTE));
            }
        });
        return response;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void publicOperationsAndHealthNeedNoToken() throws Exception {
        assertThat(call("GET", "/v1/master/countries", null, null).getStatus()).isEqualTo(200);
        assertThat(seen.get()).isNull();
        // the matched contract operation is marked for the dispatcher (an unserved one is then 501, not 404)
        assertThat(seenOperation.get()).isEqualTo("master.ListCountries");
        assertThat(call("GET", "/actuator/health/readiness", null, null).getStatus()).isEqualTo(200);
    }

    @Test
    void undeclaredRoutesAreRefused() throws Exception {
        MockHttpServletResponse response = call("GET", "/v1/master/internal-dump", null, null);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).contains("NOT_FOUND");
    }

    @Test
    void protectedOperationsNeedAValidToken() throws Exception {
        MockHttpServletResponse none = call("POST", "/v1/master/countries", null, null);
        assertThat(none.getStatus()).isEqualTo(401);
        assertThat(none.getHeader("WWW-Authenticate")).startsWith("Bearer");
        assertThat(call("POST", "/v1/master/countries", "Basic abc", null).getStatus()).isEqualTo(401);
        assertThat(call("POST", "/v1/master/countries", "Bearer ", null).getStatus()).isEqualTo(401);
        MockHttpServletResponse forged = call("POST", "/v1/master/countries", bearer(TestTokens.forged(TENANT, USER)), null);
        assertThat(forged.getStatus()).isEqualTo(401);
        assertThat(forged.getContentAsString()).contains("UNAUTHENTICATED");
        assertThat(call("GET", "/v1/master/countries", bearer("garbage"), null).getStatus()).isEqualTo(401);
    }

    @Test
    void platformOperationsRefuseTenantTokensAndViceVersa() throws Exception {
        String tenantToken = bearer(tokens.tenantUser(TENANT, USER, "master.country.create", "org.institution.read"));
        assertThat(call("POST", "/v1/master/countries", tenantToken, null).getStatus()).isEqualTo(403);
        String operator = bearer(tokens.platformOperator(USER, "master.country.create"));
        assertThat(call("POST", "/v1/master/countries", operator, null).getStatus()).isEqualTo(200);
        assertThat(seen.get().platformOperator()).isTrue();
        assertThat(seenCall.get().userId()).contains(USER);
        assertThat(seenCall.get().tenantId()).isEmpty();
        UUID institution = UUID.randomUUID();
        assertThat(call("GET", "/v1/org/institutions/" + institution, operator, null).getStatus()).isEqualTo(403);
        // a platform-audience token without MFA that carries a tenant is still not a tenant user's token
        String platformAudienceWithTenant = bearer(tokens.custom(java.util.Map.of("sub", USER.toString(),
                "tid", TENANT.toString(), "aud", "mysms-platform-admin", "perms", List.of("org.institution.read")),
                java.time.Instant.now().plusSeconds(300)));
        assertThat(call("GET", "/v1/org/institutions/" + institution, platformAudienceWithTenant, null).getStatus())
                .isEqualTo(403);
    }

    @Test
    void permissionsAndInstitutionScopeAreEnforced() throws Exception {
        String reader = bearer(tokens.custom(java.util.Map.of("sub", USER.toString(), "tid", TENANT.toString(), "aud", "mysms",
                "perms", List.of("org.institution.read"), "ins", List.of(INSTITUTION.toString())),
                java.time.Instant.now().plusSeconds(300)));
        String path = "/v1/org/institutions/" + INSTITUTION;
        assertThat(call("GET", path, reader, null).getStatus()).isEqualTo(200);
        assertThat(seen.get().tenantId()).contains(TENANT);
        assertThat(seenCall.get().tenantId()).contains(TENANT);
        assertThat(seen.get().activeInstitutionId()).isEmpty();
        assertThat(call("GET", path, reader, INSTITUTION.toString()).getStatus()).isEqualTo(200);
        assertThat(seen.get().activeInstitutionId()).contains(INSTITUTION);
        assertThat(call("GET", path, reader, UUID.randomUUID().toString()).getStatus()).isEqualTo(403);
        assertThat(call("GET", path, reader, "x").getStatus()).isEqualTo(400);
        MockHttpServletResponse denied = call("DELETE", path, reader, null);
        assertThat(denied.getStatus()).isEqualTo(403);
        assertThat(denied.getContentAsString()).contains("PERMISSION_DENIED");
        // after the request the identity is gone again
        assertThat(AuthContext.current()).isEmpty();
    }

    @Test
    void theTokensAuthenticationMethodsReachTheRequestOnly() throws Exception {
        String mfa = bearer(tokens.custom(java.util.Map.of("sub", USER.toString(), "tid", TENANT.toString(),
                "aud", "mysms", "perms", List.of("org.institution.read"), "ins", List.of("*"),
                "amr", List.of("pwd", "mfa")), java.time.Instant.now().plusSeconds(300)));
        assertThat(call("GET", "/v1/org/institutions/" + INSTITUTION, mfa, null).getStatus()).isEqualTo(200);
        assertThat(seenMethods.get()).containsExactlyInAnyOrder("pwd", "mfa");
        assertThat(AuthContext.currentAuthMethods()).as("cleared after the request").isEmpty();
        assertThat(call("GET", "/v1/master/countries", null, null).getStatus()).isEqualTo(200);
        assertThat(seenMethods.get()).isEmpty();
    }

    @Test
    void aTenantWideGrantCoversEveryInstitution() throws Exception {
        String tenantWide = bearer(tokens.custom(java.util.Map.of("sub", USER.toString(), "tid", TENANT.toString(),
                "aud", "mysms", "perms", List.of("org.institution.read"), "ins", List.of("*")),
                java.time.Instant.now().plusSeconds(300)));
        UUID anyInstitution = UUID.randomUUID();
        assertThat(call("GET", "/v1/org/institutions/" + anyInstitution, tenantWide, anyInstitution.toString())
                .getStatus()).isEqualTo(200);
        assertThat(seen.get().activeInstitutionId()).contains(anyInstitution);
        assertThat(seen.get().allInstitutions()).isTrue();
        assertThat(seen.get().institutionIds()).isEmpty();
    }

    @Test
    void propagatesTheCallerTokenOnSdkCalls() throws Exception {
        String token = tokens.tenantUser(TENANT, USER, "org.institution.read");
        AtomicReference<String> forwarded = new AtomicReference<>();
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/org/institutions/" + INSTITUTION);
        request.addHeader("Authorization", bearer(token));
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                MockClientHttpRequest outbound = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://sis.local/v1/x"));
                try {
                    new BearerPropagationInterceptor().intercept(outbound, new byte[0], (r, b) -> {
                        forwarded.set(r.getHeaders().getFirst("Authorization"));
                        return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
                    });
                } catch (java.io.IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }
        });
        assertThat(forwarded.get()).isEqualTo("Bearer " + token);
        assertThat(filter.getOrder()).isEqualTo(AuthFilter.ORDER);
        org.assertj.core.api.Assertions.assertThatThrownBy(AuthContext::require).isInstanceOf(IllegalStateException.class);
    }
}
