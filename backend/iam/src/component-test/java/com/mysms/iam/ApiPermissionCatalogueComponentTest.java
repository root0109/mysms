package com.mysms.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.events.ApiPermissionSync;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real PostgreSQL and the real edge: a module's published API-permission catalogue becomes iam's — operations and
 * tenant-user permission codes added, a repeated catalogue harmless, what no catalogue carries any more retired (never
 * deleted; here at once, the retire-after window set to zero) and published again when it comes back, a malformed
 * catalogue rejected — and only platform staff read it.
 */
@SpringBootTest(classes = IamApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiPermissionCatalogueComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String MODULE = "catalogtest";
    private static final String READ = "{\"operationId\":\"catalogtest.ListItems\",\"method\":\"GET\","
            + "\"path\":\"/v1/catalogtest/items\",\"permission\":\"catalogtest.item.read\",\"schemes\":[\"bearerAuth\"],"
            + "\"anonymous\":false,\"audience\":\"public\"}";
    private static final String WRITE = "{\"operationId\":\"catalogtest.CreateItem\",\"method\":\"POST\","
            + "\"path\":\"/v1/catalogtest/items\",\"permission\":\"catalogtest.item.create\","
            + "\"schemes\":[\"bearerAuth\"],\"anonymous\":false,\"audience\":\"public\"}";
    /** A provider's signed callback: no token scheme at all. */
    private static final String CALLBACK = "{\"operationId\":\"catalogtest.ReceiveItemWebhook\",\"method\":\"POST\","
            + "\"path\":\"/v1/catalogtest/webhooks/items\",\"permission\":\"catalogtest.item.receive_webhook\","
            + "\"schemes\":[],\"anonymous\":true,\"audience\":\"public\"}";
    private static final String PURGE = "{\"operationId\":\"catalogtest.PurgeItems\",\"method\":\"POST\","
            + "\"path\":\"/v1/catalogtest/items:purge\",\"permission\":\"catalogtest.item.purge\","
            + "\"schemes\":[\"platformAdminAuth\"],\"anonymous\":false,\"audience\":\"platform\"}";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private ApiPermissionSync sync;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.api-permission.retire-after", () -> "PT0S");
    }

    private static EventEnvelope catalogue(String module, String sha256, String... entries) {
        String data = "{\"module\":\"" + MODULE + "\",\"release\":\"1.0.0\",\"sha256\":\"" + sha256
                + "\",\"entries\":[" + String.join(",", entries) + "]}";
        return new EventEnvelope(UUID.randomUUID(), module, module + ".api_permissions.published", Optional.empty(),
                module + ".api_permissions", UUID.randomUUID(), Instant.now(), data, Optional.empty());
    }

    private static String sha(char digit) {
        return String.valueOf(digit).repeat(64);
    }

    @Test
    void aPublishedCatalogueBecomesIamsAndRetiresWhatNoCatalogueCarriesAnyMore() throws Exception {
        sync.apply(catalogue(MODULE, sha('a'), READ, PURGE, CALLBACK));
        sync.apply(catalogue(MODULE, sha('a'), READ, PURGE, CALLBACK));

        JsonNode first = list("moduleCode = \"catalogtest\"");
        assertThat(operationIds(first)).as("a repeated catalogue changes nothing").containsExactly(
                "catalogtest.ListItems", "catalogtest.PurgeItems", "catalogtest.ReceiveItemWebhook");
        JsonNode read = first.get(0);
        assertThat(read.path("permissionCode").asString()).isEqualTo("catalogtest.item.read");
        assertThat(read.path("schemes").get(0).asString()).isEqualTo("bearerAuth");
        assertThat(read.path("audience").asString()).isEqualTo("public");
        assertThat(read.path("release").asString()).isEqualTo("1.0.0");
        assertThat(read.has("retireTime")).isFalse();
        assertThat(first.get(2).path("schemes").isEmpty()).as("a signed callback has no scheme").isTrue();
        assertThat(permission("catalogtest.item.read")).as("a tenant-user code joins with its derived id")
                .isEqualTo("published");
        assertThat(permission("catalogtest.item.purge")).as("platform-staff codes are not role permissions")
                .isEqualTo("absent");

        sync.apply(catalogue(MODULE, sha('b'), WRITE, PURGE));
        JsonNode second = list("moduleCode = \"catalogtest\"");
        assertThat(operationIds(second)).containsExactly("catalogtest.CreateItem", "catalogtest.ListItems",
                "catalogtest.PurgeItems", "catalogtest.ReceiveItemWebhook");
        assertThat(second.get(1).has("retireTime")).as("the dropped operation is retired, not deleted").isTrue();
        assertThat(second.get(0).has("retireTime")).isFalse();
        assertThat(permission("catalogtest.item.read")).isEqualTo("retired");
        assertThat(permission("catalogtest.item.create")).isEqualTo("published");

        sync.apply(catalogue(MODULE, sha('a'), READ, PURGE, CALLBACK));
        JsonNode third = list("moduleCode = \"catalogtest\"");
        assertThat(third.get(1).has("retireTime")).as("published again").isFalse();
        assertThat(third.get(0).has("retireTime")).isTrue();
        assertThat(permission("catalogtest.item.read")).isEqualTo("published");
        assertThat(permission("catalogtest.item.create")).isEqualTo("retired");
    }

    @Test
    void aCatalogueThatDoesNotNameItsOwnModuleOrHoldsAMalformedOperationIsRejected() {
        assertThatThrownBy(() -> sync.apply(catalogue("hr", sha('c'), READ)))
                .isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(catalogue(MODULE, "short", READ)))
                .isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(catalogue(MODULE, sha('d'), READ.replace("GET", "FETCH"))))
                .isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(catalogue(MODULE, sha('d'), READ.replace("catalogtest.ListItems",
                "hr.ListItems")))).as("an operation of another module").isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(catalogue(MODULE, sha('d'), READ.replace("catalogtest.item.read",
                "hr.staff.read")))).as("a permission code of another module").isInstanceOf(RejectedEventException.class);
        assertThat(sync.handlers()).containsKeys("iam.api_permissions.published", "lms.api_permissions.published");
    }

    @Test
    void onlyPlatformStaffReadTheCatalogue() throws Exception {
        String path = "/v1/iam/api-permissions";
        assertThat(get(path, null).statusCode()).isEqualTo(401);
        assertThat(get(path, TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID(), "*")).statusCode())
                .as("a tenant user, even one holding every permission").isEqualTo(403);
        assertThat(get(path, TOKENS.platformOperator(UUID.randomUUID())).statusCode())
                .as("platform staff without the permission").isEqualTo(403);
        assertThat(get(path, staff()).statusCode()).isEqualTo(200);
    }

    private String staff() {
        return TOKENS.platformOperator(UUID.randomUUID(), "iam.api_permission.read");
    }

    private JsonNode list(String filter) throws Exception {
        HttpResponse<String> response = get("/v1/iam/api-permissions?size=50&filter="
                + URLEncoder.encode(filter, StandardCharsets.UTF_8), staff());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JSON.readTree(response.body()).path("data");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static List<String> operationIds(JsonNode rows) {
        List<String> ids = new ArrayList<>();
        rows.forEach(row -> ids.add(row.path("operationId").asString()));
        return ids;
    }

    /** absent, published or retired: the state of a code in iam.permission, whose id must be the derived one. */
    private String permission(String code) {
        return TestTransactions.of(database).readWithoutTenant(() -> database.jdbc().sql("""
                SELECT CASE WHEN p.retired_at IS NULL THEN 'published' ELSE 'retired' END
                  FROM iam.permission p
                 WHERE p.code = :code AND p.id = CAST(md5('mysms.permission:' || p.code) AS uuid)
                """).param("code", code).query(String.class).optional().orElse("absent"));
    }
}
