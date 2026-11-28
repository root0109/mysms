package com.mysms.hrms.timeattendance;

import com.mysms.hrms.HrmsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.ModuleChecks;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.hrms.timeattendance.config.TimeAttendanceDatabaseConfig;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
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
 * The hrms service's time_attendance area on its own database: it migrates schema "time_attendance" with its own
 * Flyway history, reports its database in health, its edge refuses undeclared routes and missing tokens, and the
 * leave link stays the workflow's — no create or update request writes {@code leave_application_id}.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoint.health.show-components=always")
class TimeAttendanceServiceComponentTest {

    /** CREATE TABLE statements in database/migrations/time_attendance (partitions included). */
    private static final int TABLES = 32;
    private static final String FIRST_VERSION = "\"1\"";
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(TimeAttendanceDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "time_attendance", "hr", "payroll");
        registry.add("mysms.auth.api-docs", () -> true);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private HttpResponse<String> send(String method, String path, String token, String body, String... headers)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void migratesItsOwnSchemaOnly() {
        Integer tables = database.jdbc().sql("SELECT count(*) FROM information_schema.tables"
                        + " WHERE table_schema = 'time_attendance' AND table_name <> 'flyway_schema_history'")
                .query(Integer.class).single();
        assertThat(tables).isEqualTo(TABLES);
        Integer moduleSchemas = database.jdbc().sql("SELECT count(*) FROM pg_namespace WHERE nspname = ANY(:modules)")
                .param("modules", ModuleChecks.SCHEMAS.toArray(String[]::new)).query(Integer.class).single();
        assertThat(moduleSchemas).as("only its module's schemas exist in its database").isEqualTo(3);
        Boolean ownsSchema = database.jdbc().sql(
                "SELECT pg_get_userbyid(nspowner) = current_user FROM pg_namespace WHERE nspname = 'time_attendance'")
                .query(Boolean.class).single();
        assertThat(ownsSchema).as("the application role owns nothing").isFalse();
    }

    @Test
    void reportsItsDatabaseInHealth() throws Exception {
        HttpResponse<String> health = send("GET", "/actuator/health", null, null);
        assertThat(health.statusCode()).isEqualTo(200);
        JsonNode body = JSON.readTree(health.body());
        assertThat(body.path("components").path("timeAttendanceDatabase").path("status").asString()).isEqualTo("UP");
    }

    @Test
    void edgeRefusesUndeclaredRoutesAndMissingTokens() throws Exception {
        assertThat(send("GET", "/v1/time-attendance/no-such-resource", null, null).statusCode()).isEqualTo(404);
        JsonNode protectedOperation = firstOperationRequiringAToken();
        String path = protectedOperation.path("path").asString().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        String method = protectedOperation.path("method").asString();
        HttpResponse<String> anonymous = send(method, path, null, null);
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(anonymous.body()).contains("UNAUTHENTICATED");
        String noPermissions = TOKENS.tenantUser(UUID.randomUUID(), UUID.randomUUID());
        String operator = TOKENS.platformOperator(UUID.randomUUID());
        int withoutPermission = send(method, path, noPermissions, null).statusCode();
        int asOperator = send(method, path, operator, null).statusCode();
        assertThat(withoutPermission).isEqualTo(403);
        assertThat(asOperator).isEqualTo(403);
    }

    /** The leave application behind an on_leave day is linked by the leave workflow, never by a request. */
    @Test
    void noRequestLinksALeaveApplicationToAStaffAttendanceDay() throws Exception {
        UUID tenant = UUID.randomUUID();
        String admin = admin(tenant);
        String path = "/v1/time-attendance/staff-attendance-days";
        HttpResponse<String> created = send("POST", path, admin, "{\"staffId\":\"" + UUID.randomUUID()
                + "\",\"attendanceDate\":\"2026-01-12\",\"status\":\"on_leave\",\"leaveApplicationId\":\""
                + UUID.randomUUID() + "\"}");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        JsonNode day = JSON.readTree(created.body());
        assertThat(day.has("leaveApplicationId")).as("ignored on create").isFalse();
        String id = day.path("id").asString();
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT leave_application_id IS NULL"
                        + " FROM time_attendance.staff_attendance WHERE tenant_id = :tenantId AND id = '" + id + "'")
                .query(Boolean.class).single())).isTrue();

        // a PATCH carrying only that field is like an empty body: the row comes back unchanged, at its version
        HttpResponse<String> patched = send("PATCH", path + "/" + id, admin,
                "{\"leaveApplicationId\":\"" + UUID.randomUUID() + "\"}", "If-Match", FIRST_VERSION);
        assertThat(patched.statusCode()).as(patched.body()).isEqualTo(200);
        JsonNode row = JSON.readTree(patched.body());
        assertThat(row.path("etag").asString()).as("keeps its version").isEqualTo(FIRST_VERSION);
        assertThat(row.has("leaveApplicationId")).as("not part of the update body").isFalse();
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        return send(method, path, token, body, new String[0]);
    }

    private <T> T read(UUID tenant, Supplier<T> work) {
        CallContext context = CallContext.anonymous(TRACE).withIdentity(tenant, null);
        return CallContextHolder.callAs(context, () -> TestTransactions.of(database).read(work::get));
    }

    /** A tenant-wide administrator holding the area's permissions. */
    private static String admin(UUID tenant) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("time_attendance.*"));
        claims.put("tperms", List.of("time_attendance.*"));
        claims.put("ins", List.of("*"));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private static JsonNode firstOperationRequiringAToken() throws Exception {
        try (InputStream in = TimeAttendanceServiceComponentTest.class.getClassLoader()
                .getResourceAsStream("META-INF/mysms/operations/time_attendance.json")) {
            for (JsonNode operation : JSON.readTree(in)) {
                if (!operation.path("anonymous").asBoolean()) {
                    return operation;
                }
            }
        }
        throw new IllegalStateException("time_attendance has no operation that requires a token");
    }
}
