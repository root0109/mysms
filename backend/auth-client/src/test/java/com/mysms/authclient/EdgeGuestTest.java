package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.internal.JwtTokenVerifier;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnavailableException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * The edge's handling of {@code X-Tenant-Code}: resolved only on operations callable without a token and never next
 * to a token; it binds the tenant alone, and an unknown school is 404 and an unreachable iam 503.
 */
class EdgeGuestTest {

    private static final TestTokens TOKENS = new TestTokens();
    private static final OperationTable OPERATIONS = OperationTable.load(List.of("authtest"));
    private static final UUID SCHOOL = UUID.randomUUID();
    private static final String LIST = "/v1/authtest/widgets";

    private final List<String> asked = new ArrayList<>();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-12T08:00:00Z"));
    private boolean iamDown;
    private final TenantCodeResolver iam = code -> {
        asked.add(code);
        if (iamDown) {
            throw new UnavailableException("iam is down");
        }
        return "green-valley".equalsIgnoreCase(code) ? Optional.of(SCHOOL) : Optional.empty();
    };
    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };
    private final AuthFilter filter = new AuthFilter(OPERATIONS, new EdgeAuthorizer(
            JwtTokenVerifier.from(new AuthClientProperties(TestTokens.ISSUER, null, TOKENS.publicKeyPem(), null,
                    List.of("authtest"), null, false)), new ClaimsPermissionChecker(), new TenantCodes(iam, clock)),
            new ProblemResponseWriter(JsonMapper.builder().build()), List.of());
    private final AtomicReference<Optional<UUID>> boundTenant = new AtomicReference<>();
    private final AtomicReference<Boolean> guest = new AtomicReference<>();

    @AfterEach
    void clear() {
        CallContextHolder.clear();
    }

    private MockHttpServletResponse get(String path, String code, String token) throws Exception {
        CallContextHolder.set(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (code != null) {
            request.addHeader(AuthFilter.TENANT_CODE_HEADER, code);
        }
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        boundTenant.set(null);
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                boundTenant.set(CallContextHolder.require().tenantId());
                guest.set(Guest.isGuest());
            }
        });
        return response;
    }

    @Test
    void aGuestReadsTheSchoolItsCodeNames() throws Exception {
        assertThat(get(LIST, " Green-Valley ", null).getStatus()).isEqualTo(200);
        assertThat(boundTenant.get()).contains(SCHOOL);
        assertThat(guest.get()).isTrue();
        assertThat(CallContextHolder.require().tenantId()).as("the binding ends with the request").isEmpty();
    }

    @Test
    void anUnknownSchoolIs404AndAMissingCodeBindsNoTenant() throws Exception {
        MockHttpServletResponse unknown = get(LIST, "nowhere", null);
        assertThat(unknown.getStatus()).isEqualTo(404);
        assertThat(unknown.getContentAsString()).contains("NOT_FOUND").doesNotContain("nowhere");
        assertThat(boundTenant.get()).as("the operation never ran").isNull();
        assertThat(get(LIST, null, null).getStatus()).isEqualTo(200);
        assertThat(boundTenant.get()).isEmpty();
        assertThatThrownBy(Guest::requireTenant).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aCodeNoSchoolCanHaveIsRefusedWithoutAskingIam() throws Exception {
        assertThat(get(LIST, "x".repeat(61), null).getStatus()).isEqualTo(404);
        assertThat(get(LIST, "a b", null).getStatus()).isEqualTo(404);
        assertThat(asked).isEmpty();
    }

    @Test
    void anIdentityServiceThatCannotAnswerIs503() throws Exception {
        iamDown = true;
        assertThat(get(LIST, "green-valley", null).getStatus()).isEqualTo(503);
    }

    @Test
    void answersAreKeptBrieflyAndCaseInsensitively() throws Exception {
        get(LIST, "green-valley", null);
        get(LIST, "GREEN-VALLEY", null);
        get(LIST, "nowhere", null);
        get(LIST, "nowhere", null);
        assertThat(asked).containsExactly("green-valley", "nowhere");
        now.set(now.get().plus(TenantCodes.TTL));
        get(LIST, "green-valley", null);
        assertThat(asked).hasSize(3);
    }

    @Test
    void aCodeNamingNoSchoolIsAskedAgainSoonerThanASchool() throws Exception {
        get(LIST, "green-valley", null);
        get(LIST, "nowhere", null);
        now.set(now.get().plus(TenantCodes.NEGATIVE_TTL));
        get(LIST, "green-valley", null);
        get(LIST, "nowhere", null);
        assertThat(asked).containsExactly("green-valley", "nowhere", "nowhere");
    }

    @Test
    void manyCodesPushOutOnlyTheLeastRecentlyUsed() {
        TenantCodes codes = new TenantCodes(iam, clock);
        codes.activeTenant("green-valley");
        for (int i = 0; i < TenantCodes.MAX_ENTRIES; i++) {
            if (i % 1000 == 0) {
                codes.activeTenant("green-valley");
            }
            codes.activeTenant("random-" + i);
        }
        asked.clear();
        assertThat(codes.activeTenant("green-valley")).contains(SCHOOL);
        assertThat(asked).as("a school in use stays cached").isEmpty();
        codes.activeTenant("random-0");
        assertThat(asked).as("the least recently used code left").containsExactly("random-0");
    }

    @Test
    void theCodeIsIgnoredNextToATokenAndOnOperationsNeedingOne() throws Exception {
        UUID tenant = UUID.randomUUID();
        String token = TOKENS.custom(Map.of("sub", UUID.randomUUID().toString(), "tid", tenant.toString(),
                "aud", "mysms", "perms", List.of(), "ins", List.of()), Instant.now().plusSeconds(600));
        assertThat(get(LIST, "green-valley", token).getStatus()).isEqualTo(200);
        assertThat(boundTenant.get()).as("a signed-in caller reads its own tenant").contains(tenant);
        assertThat(guest.get()).isFalse();
        assertThat(get("/v1/authtest/widgets/" + UUID.randomUUID(), "green-valley", null).getStatus())
                .as("a code is no credential").isEqualTo(401);
        assertThat(asked).isEmpty();
    }
}
