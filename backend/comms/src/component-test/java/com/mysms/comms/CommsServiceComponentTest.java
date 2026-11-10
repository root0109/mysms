package com.mysms.comms;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.ModuleChecks;
import com.mysms.core.testkit.OpenApiDocuments;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.comms.config.CommsDatabaseConfig;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * The comms microservice runs alone on its own database (JAVA-CODING-RULES §2, §12):
 * it migrates schema "comms" with its own Flyway history, reports its database in health,
 * and its edge refuses undeclared routes and missing tokens.
 */
@SpringBootTest(classes = CommsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoint.health.show-components=always")
class CommsServiceComponentTest {

    /** CREATE TABLE statements in database/migrations/comms (partitions included). */
    private static final int TABLES = 96;
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(CommsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "comms");
        registry.add("mysms.auth.api-docs", () -> true);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private HttpResponse<String> send(String method, String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void migratesItsOwnSchemaOnly() {
        Integer tables = database.jdbc().sql("SELECT count(*) FROM information_schema.tables"
                        + " WHERE table_schema = 'comms' AND table_name <> 'flyway_schema_history'")
                .query(Integer.class).single();
        assertThat(tables).isEqualTo(TABLES);
        Integer moduleSchemas = database.jdbc().sql("SELECT count(*) FROM pg_namespace WHERE nspname = ANY(:modules)")
                .param("modules", ModuleChecks.MODULES.toArray(String[]::new)).query(Integer.class).single();
        assertThat(moduleSchemas).as("only its own schema exists in its database").isEqualTo(1);
        Boolean ownsSchema = database.jdbc().sql(
                "SELECT pg_get_userbyid(nspowner) = current_user FROM pg_namespace WHERE nspname = 'comms'")
                .query(Boolean.class).single();
        assertThat(ownsSchema).as("the application role owns nothing").isFalse();
    }

    @Test
    void reportsItsDatabaseInHealth() throws Exception {
        HttpResponse<String> health = send("GET", "/actuator/health", null);
        assertThat(health.statusCode()).isEqualTo(200);
        JsonNode body = JsonMapper.builder().build().readTree(health.body());
        assertThat(body.path("components").path("commsDatabase").path("status").asString()).isEqualTo("UP");
    }

    @Test
    void edgeRefusesUndeclaredRoutesAndMissingTokens() throws Exception {
        assertThat(send("GET", "/v1/comms/no-such-resource", null).statusCode()).isEqualTo(404);
        JsonNode protectedOperation = firstOperationRequiringAToken();
        String path = protectedOperation.path("path").asString().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        String method = protectedOperation.path("method").asString();
        HttpResponse<String> anonymous = send(method, path, null);
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.body()).contains("UNAUTHENTICATED");
        String noPermissions = TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID());
        String operator = TOKENS.platformOperator(UUID.randomUUID());
        int withoutPermission = send(method, path, noPermissions).statusCode();
        int asOperator = send(method, path, operator).statusCode();
        assertThat(withoutPermission).isEqualTo(403);
        assertThat(asOperator).isEqualTo(403);
    }

    private static JsonNode firstOperationRequiringAToken() throws Exception {
        try (InputStream in = CommsServiceComponentTest.class.getClassLoader()
                .getResourceAsStream("META-INF/mysms/operations/comms.json")) {
            for (JsonNode operation : JsonMapper.builder().build().readTree(in)) {
                if (!operation.path("anonymous").asBoolean()) {
                    return operation;
                }
            }
        }
        throw new IllegalStateException("comms has no operation that requires a token");
    }

    /** The OpenAPI document is written from the contract interfaces by springdoc and kept as a build artifact. */
    @Test
    void writesItsOpenApiDocumentFromTheCode() throws Exception {
        HttpResponse<String> doc = send("GET", "/v3/api-docs.yaml", null);
        assertThat(doc.statusCode()).isEqualTo(200);
        String document = OpenApiDocuments.write("comms", doc.body());
        assertThat(document).contains("operationId: comms.ActivateEvent").contains("x-permission:");
    }
}
