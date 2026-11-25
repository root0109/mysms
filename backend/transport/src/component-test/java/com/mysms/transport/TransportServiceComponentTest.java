package com.mysms.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.ModuleChecks;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.transport.config.TransportDatabaseConfig;
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
 * The transport microservice runs alone on its own database: it migrates schema "transport" with its own Flyway
 * history, reports its database in health, and its edge refuses undeclared routes and missing tokens.
 */
@SpringBootTest(classes = TransportApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoint.health.show-components=always")
class TransportServiceComponentTest {

    /** CREATE TABLE statements in database/migrations/transport (partitions included). */
    private static final int TABLES = 9;
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(TransportDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "transport");
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
                        + " WHERE table_schema = 'transport' AND table_name <> 'flyway_schema_history'")
                .query(Integer.class).single();
        assertThat(tables).isEqualTo(TABLES);
        Integer moduleSchemas = database.jdbc().sql("SELECT count(*) FROM pg_namespace WHERE nspname = ANY(:modules)")
                .param("modules", ModuleChecks.MODULES.toArray(String[]::new)).query(Integer.class).single();
        assertThat(moduleSchemas).as("only its own schema exists in its database").isEqualTo(1);
        Boolean ownsSchema = database.jdbc().sql(
                "SELECT pg_get_userbyid(nspowner) = current_user FROM pg_namespace WHERE nspname = 'transport'")
                .query(Boolean.class).single();
        assertThat(ownsSchema).as("the application role owns nothing").isFalse();
    }

    @Test
    void reportsItsDatabaseInHealth() throws Exception {
        HttpResponse<String> health = send("GET", "/actuator/health", null);
        assertThat(health.statusCode()).isEqualTo(200);
        JsonNode body = JsonMapper.builder().build().readTree(health.body());
        assertThat(body.path("components").path("transportDatabase").path("status").asString()).isEqualTo("UP");
    }

    @Test
    void edgeRefusesUndeclaredRoutesAndMissingTokens() throws Exception {
        assertThat(send("GET", "/v1/transport/no-such-resource", null).statusCode()).isEqualTo(404);
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
        try (InputStream in = TransportServiceComponentTest.class.getClassLoader()
                .getResourceAsStream("META-INF/mysms/operations/transport.json")) {
            for (JsonNode operation : JsonMapper.builder().build().readTree(in)) {
                if (!operation.path("anonymous").asBoolean()) {
                    return operation;
                }
            }
        }
        throw new IllegalStateException("transport has no operation that requires a token");
    }
}
