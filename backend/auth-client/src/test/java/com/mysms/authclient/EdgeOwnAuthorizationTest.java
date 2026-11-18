package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ProblemResponseWriter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * An operation whose contract gives it an {@code Authorization} header of its own (a provider's signed callback, such
 * as lms.ReceiveToolOutcome's LTI 1.1 OAuth 1.0a signature) reaches the operation with that header untouched and no
 * identity, whatever the header holds; every other operation, anonymous ones included, still reads the header as a
 * bearer token and refuses anything else with 401. The REST edge is the real {@link AuthFilter}; the gRPC edge makes
 * the same {@link EdgeAuthorizer} decision; tokens are signed for real.
 */
class EdgeOwnAuthorizationTest {

    private static final TestTokens TOKENS = new TestTokens();
    private static final String OUTCOME = "/v1/authtest/widgets:outcome";
    private static final String OAUTH = "OAuth oauth_consumer_key=\"tool\", oauth_signature_method=\"HMAC-SHA1\","
            + " oauth_signature=\"c2lnbmVk\"";
    private static final OperationTable OPERATIONS = OperationTable.load(List.of("authtest"));
    private static final EdgeAuthorizer AUTHORIZER = new EdgeAuthorizer(
            JwtTokenVerifier.from(new AuthClientProperties(TestTokens.ISSUER, null, TOKENS.publicKeyPem(), null,
                    List.of("authtest"), null, false)), new ClaimsPermissionChecker());

    private final AuthFilter filter = new AuthFilter(OPERATIONS, AUTHORIZER,
            new ProblemResponseWriter(JsonMapper.builder().build()), List.of());
    private final AtomicReference<String> reached = new AtomicReference<>();

    @AfterEach
    void clear() {
        CallContextHolder.clear();
    }

    private MockHttpServletResponse post(String path, String authorization) throws Exception {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        reached.set(null);
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                assertThat(AuthContext.current()).as("no identity is bound").isEmpty();
                reached.set(((MockHttpServletRequest) req).getHeader("Authorization"));
            }
        });
        return response;
    }

    @Test
    void theOperationReceivesItsOwnAuthorizationHeaderUntouched() throws Exception {
        assertThat(post(OUTCOME, OAUTH).getStatus()).isEqualTo(200);
        assertThat(reached.get()).isEqualTo(OAUTH);
        // a tool's own JWT (LTI Advantage) is the operation's to verify, not a MySMS token the edge refuses
        String forged = "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0b29sIn0.c2ln";
        assertThat(post(OUTCOME, forged).getStatus()).isEqualTo(200);
        assertThat(reached.get()).isEqualTo(forged);
        assertThat(post(OUTCOME, null).getStatus()).as("the operation refuses a missing signature itself")
                .isEqualTo(200);
    }

    @Test
    void everyOtherOperationStillReadsTheHeaderAsABearerToken() throws Exception {
        MockHttpServletResponse anonymous = post("/v1/authtest/widgets:sync", OAUTH);
        assertThat(anonymous.getStatus()).isEqualTo(401);
        assertThat(reached.get()).isNull();
        OperationRule list = OPERATIONS.match("GET", "/v1/authtest/widgets").orElseThrow();
        assertThat(list.isPublic()).isTrue();
        assertThat(AUTHORIZER.decide(list, OAUTH, null, null)).isInstanceOf(EdgeDecision.Refused.class);
        String forged = "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0b29sIn0.c2ln";
        assertThat(AUTHORIZER.decide(list, forged, null, null)).isInstanceOf(EdgeDecision.Refused.class);
        String user = TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID(), "authtest.widget.read");
        assertThat(AUTHORIZER.decide(list, "Bearer " + user, null, null)).isInstanceOf(EdgeDecision.Admitted.class);
    }

    @Test
    void theGrpcEdgeDecidesTheSame() {
        OperationRule outcome = OPERATIONS.forGrpcMethod("mysms.authtest.v1.WidgetService/ReceiveWidgetOutcome")
                .orElseThrow();
        assertThat(outcome.ownAuthorization()).isTrue();
        EdgeDecision decision = AUTHORIZER.decide(outcome, OAUTH, "not-a-uuid", null);
        assertThat(decision).isEqualTo(new EdgeDecision.Admitted(Optional.empty(), null, Set.of()));
    }

    @Test
    void onlyAnAnonymousPublicOperationMayVerifyItsOwnHeader() {
        assertThatThrownBy(() -> new OperationRule("x.Y", "POST", "/v1/x", "x.y.receive", Set.of("bearerAuth"), false,
                null, OperationRule.PUBLIC, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationRule("x.Y", "POST", "/v1/x", "x.y.receive", Set.of(), true, null,
                OperationRule.INTERNAL, true)).isInstanceOf(IllegalArgumentException.class);
    }
}
