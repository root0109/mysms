package com.mysms.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.ServiceTokenProvider;
import com.mysms.authclient.TokenVerifier;
import com.mysms.authclient.VerifiedToken;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.testkit.TestDatabase;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Service identity end to end: iam's client-credentials endpoint issues a service token to a
 * configured client (and to nobody else), the edge's own verifier reads it as that module's service principal, the
 * edge refuses it on the contract's operations (none is internal yet), and auth-client's provider obtains and keeps it
 * through the real endpoint.
 */
@SpringBootTest(classes = IamApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ServiceTokenComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String SECRET = "finance-client-secret-0123456789abcdefghij";
    private static final int PORT = freePort();

    @LocalServerPort
    private int port;

    @Autowired
    private TokenVerifier verifier;

    @Autowired
    private ServiceTokenProvider provider;

    private static int freePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        DATABASE.register(registry, "iam");
        registry.add("server.port", () -> PORT);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(SECRET.getBytes(StandardCharsets.UTF_8)));
        registry.add("mysms.iam.service-clients.finance.secret-sha256", () -> hash);
        registry.add("mysms.iam.service-clients.finance.permissions", () -> "org.institution.read,platform.card.read");
        registry.add("mysms.iam.token.service-token-ttl", () -> "PT2M");
        // the provider of a finance process, pointed at this iam
        registry.add("mysms.auth.token-uri", () -> "http://localhost:" + PORT + "/oauth2/token");
        registry.add("mysms.finance.service-client.secret", () -> SECRET);
    }

    private HttpResponse<String> token(String form) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String form(String clientId, String secret) {
        return "grant_type=client_credentials&client_id=" + clientId + "&client_secret="
                + URLEncoder.encode(secret, StandardCharsets.UTF_8);
    }

    @Test
    void issuesAServiceTokenToAConfiguredClient() throws Exception {
        HttpResponse<String> issued = token(form("finance", SECRET));
        assertThat(issued.statusCode()).as(issued.body()).isEqualTo(200);
        assertThat(issued.headers().firstValue("Cache-Control")).contains("no-store");
        JsonNode body = JSON.readTree(issued.body());
        assertThat(body.path("token_type").asString()).isEqualTo("Bearer");
        assertThat(body.path("expires_in").asLong()).isEqualTo(120);
        VerifiedToken token = verifier.verify(body.path("access_token").asString());
        assertThat(token.context().service()).contains("finance");
        assertThat(token.context().tenantId()).isEmpty();
        assertThat(token.context().permissions()).containsExactlyInAnyOrder("org.institution.read", "platform.card.read");
        assertThat(token.audiences()).containsExactly("mysms-internal");
        assertThat(token.expiresAt()).isBefore(Instant.now().plus(Duration.ofMinutes(3)));
    }

    @Test
    void refusesEveryOtherClientTheSameWay() throws Exception {
        for (String form : new String[] {form("finance", SECRET + "x"), form("platform", SECRET),
                "grant_type=client_credentials&client_id=finance", form("finance", SECRET) + "&client_id=finance"}) {
            HttpResponse<String> refused = token(form);
            assertThat(refused.statusCode()).as(form.replace(SECRET, "…")).isEqualTo(401);
            JsonNode body = JSON.readTree(refused.body());
            assertThat(body.path("code").asString()).isEqualTo("INVALID_CLIENT");
            assertThat(body.path("error").asString()).isEqualTo("invalid_client");
            assertThat(refused.body()).doesNotContain(SECRET);
        }
        HttpResponse<String> password = token("grant_type=password&client_id=finance&client_secret=" + SECRET);
        assertThat(password.statusCode()).isEqualTo(400);
        assertThat(JSON.readTree(password.body()).path("error").asString()).isEqualTo("unsupported_grant_type");
    }

    @Test
    void theEdgeRefusesAServiceTokenOnTheContractsOperations() throws Exception {
        String service = provider.token("finance");
        assertThat(provider.token("finance")).as("kept until shortly before it expires").isEqualTo(service);
        for (String path : new String[] {"/v1/iam/me", "/v1/iam/users:checkUsername?username=someone"}) {
            HttpResponse<String> refused = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Authorization", "Bearer " + service).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(refused.statusCode()).as(path + " " + refused.body()).isEqualTo(403);
            assertThat(JSON.readTree(refused.body()).path("code").asString()).isEqualTo("PERMISSION_DENIED");
        }
        assertThatThrownBy(() -> provider.token("platform")).isInstanceOf(UnavailableException.class);
    }
}
