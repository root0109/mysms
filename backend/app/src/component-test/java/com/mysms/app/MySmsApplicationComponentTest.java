package com.mysms.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.OperationTable;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.testkit.TestDatabase;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The monolith assembles all 21 modules: 21 separate databases (here one PostgreSQL, 21 pools), every schema
 * migrated by its own module, all 635 tables present, all 2052 operations routed at the edge — and a real
 * cross-module flow: sign-in at iam, GetMe reading the person from people over HTTP with the caller's token, the
 * profile update at people, the caller's settings.
 */
@SpringBootTest(classes = MySmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"spring.config.name=" + MySmsApplication.CONFIG_NAME,
                "management.endpoint.health.show-components=always"})
class MySmsApplicationComponentTest {

    private static final String[] MODULES = {
            "master",
            "platform",
            "iam",
            "org",
            "people",
            "academics",
            "sis",
            "hr",
            "payroll",
            "admission",
            "attendance",
            "facility",
            "timetable",
            "assessment",
            "lms",
            "finance",
            "library",
            "transport",
            "comms",
            "frontoffice",
            "careers"};
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String ADMIN_PASSWORD = "monolith-admin-secret";
    /** A fixed free port, so iam can call people inside the same process over HTTP. */
    private static final int PORT = freePort();

    @Autowired
    private Map<String, ModuleDatabase> databases;

    @Autowired
    private OperationTable operations;

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, MODULES);
        registry.add("server.port", () -> PORT);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.people-base-url", () -> "http://localhost:" + PORT);
        registry.add("mysms.iam.bootstrap.tenant-code", () -> "monolith");
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> ADMIN_PASSWORD);
        // events cross modules on a real RabbitMQ, as in production
        registry.add("mysms.events.bus", () -> "rabbitmq");
        registry.add("mysms.events.rabbitmq.uri", () -> broker().toString());
        registry.add("mysms.events.rabbitmq.exchange", () -> "mysms.events." + UUID.randomUUID());
    }

    private static URI broker;

    /** RabbitMQ: the broker at MYSMS_TEST_AMQP_URI, or a Testcontainers RabbitMQ 4.1. */
    private static synchronized URI broker() {
        if (broker == null) {
            String given = System.getenv("MYSMS_TEST_AMQP_URI");
            if (given != null && !given.isBlank()) {
                broker = URI.create(given);
                forgetEarlierRuns(broker);
            } else {
                GenericContainer<?> rabbit = new GenericContainer<>("rabbitmq:4.1-alpine").withExposedPorts(5672)
                        .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));
                rabbit.start();
                broker = URI.create("amqp://guest:guest@" + rabbit.getHost() + ":" + rabbit.getMappedPort(5672));
            }
        }
        return broker;
    }

    /**
     * A shared broker keeps the module queues of an earlier run, bound to that run's exchanges; a service refuses a
     * queue with other arguments (as it must in production), so the test starts from none.
     */
    private static void forgetEarlierRuns(URI uri) {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(uri);
            try (Connection connection = factory.newConnection("mysms-test-cleanup")) {
                for (String module : MODULES) {
                    for (String queue : List.of("mysms." + module + ".events", "mysms." + module + ".dead")) {
                        try (Channel channel = connection.createChannel()) {
                            channel.queueDelete(queue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("cannot reset the test broker", e);
        }
    }

    private JsonNode call(String method, String path, String token, String body, int expected, String... headers)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(method + " " + path + ": " + response.body()).isEqualTo(expected);
        return response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body());
    }

    /**
     * An owner's deletion crosses modules over RabbitMQ: hr's outbox event `hr.staff.deleted` is relayed, academics
     * clears the class teacher of the class section that named that staff member, and the platform records the
     * change in the tenant's audit trail.
     */
    @Test
    void anOwnersDeletionCleansUpItsReferencesInAnotherModuleOverRabbitMq() throws Exception {
        UUID tenant = UUID.randomUUID();
        JsonNode entry = null;
        for (JsonNode e : JSON.readTree(Files.readString(
                Path.of("../academics/src/component-test/resources/reference-cleanup.json"))).path("cleanup")) {
            if ("class_teacher_staff_id".equals(e.path("column").asString())) {
                entry = e;
            }
        }
        assertThat(entry).isNotNull();
        JsonNode cleanup = entry;
        UUID staff = UUID.fromString(cleanup.path("ownerId").asString());
        UUID section = UUID.fromString(cleanup.path("rowId").asString());
        CallContext context = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null);
        ModuleDatabase platform = databases.get("platformDatabase");
        ModuleDatabase academics = databases.get("academicsDatabase");
        ModuleDatabase hr = databases.get("hrDatabase");
        CallContextHolder.callAs(context, () -> platform.tx().write(() -> platform.jdbc().sql(
                "INSERT INTO platform.tenant (id, code, name) VALUES (:tenant, 'e' || substr(md5(CAST(:tenant AS text)), "
                        + "1, 12), 'Events tenant')").param("tenant", tenant).update()));
        CallContextHolder.callAs(context, () -> academics.tx().write(() -> {
            cleanup.path("fixtures").forEach(f -> academics.jdbc().sql(f.asString()).param("tenant", tenant).update());
            return academics.jdbc().sql(cleanup.path("row").asString()).param("tenant", tenant).update();
        }));
        OutboxWriter hrOutbox = new OutboxWriter("hr", hr.jdbc(), JSON);
        CallContextHolder.callAs(context, () -> hr.tx().write(() -> {
            hrOutbox.publish("hr.staff.deleted", "hr.staff", staff, Map.of("id", staff.toString()));
            return null;
        }));

        long until = System.currentTimeMillis() + 30_000;
        boolean cleared = false;
        Integer audited = 0;
        while (System.currentTimeMillis() < until && (!cleared || audited == 0)) {
            Thread.sleep(200);
            cleared = CallContextHolder.callAs(context, () -> academics.tx().read(() -> academics.jdbc().sql(
                    "SELECT class_teacher_staff_id IS NULL FROM academics.class_section WHERE tenant_id = :tenantId "
                            + "AND id = :id").param("id", section).query(Boolean.class).single()));
            audited = CallContextHolder.callAs(context, () -> platform.tx().read(() -> platform.jdbc().sql(
                    "SELECT count(*) FROM platform.audit_log WHERE tenant_id = :tenantId AND subject_id = :id "
                            + "AND event = 'update'").param("id", section).query(Integer.class).single()));
        }
        assertThat(cleared).as("academics cleared the reference to the deleted staff member").isTrue();
        assertThat(audited).as("the platform recorded the change in the tenant's trail").isEqualTo(1);
    }

    @Test
    void everyModuleOwnsItsDatabaseAndSchema() {
        assertThat(databases).hasSize(21);
        ModuleDatabase any = databases.get("masterDatabase");
        Integer tables = any.jdbc().sql(
                "SELECT count(*) FROM information_schema.tables WHERE table_name <> 'flyway_schema_history'"
                        + " AND table_schema = ANY(:schemas)")
                .param("schemas", MODULES).query(Integer.class).single();
        assertThat(tables).isEqualTo(635);
        Integer histories = any.jdbc().sql(
                // the catalog, not information_schema: the application role has no privilege on the Flyway history
                "SELECT count(*) FROM pg_catalog.pg_class WHERE relname = 'flyway_schema_history' AND relkind = 'r'")
                .query(Integer.class).single();
        assertThat(histories).isEqualTo(21);
        assertThat(operations.size()).isEqualTo(2052);
    }

    @Test
    void healthCoversEveryModuleDatabase() throws Exception {
        JsonNode components = call("GET", "/actuator/health", null, null, 200).path("components");
        for (String module : MODULES) {
            assertThat(components.path(module + "Database").path("status").asString()).as(module).isEqualTo("UP");
        }
    }

    @Test
    void signInProfileAndSettingsAcrossModules() throws Exception {
        ModuleDatabase iam = databases.get("iamDatabase");
        ModuleDatabase people = databases.get("peopleDatabase");
        CallContext system = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736");
        UUID tenant = CallContextHolder.callAs(system, () -> iam.tx().readWithoutTenant(() -> iam.jdbc()
                .sql("SELECT tenant_id FROM iam.tenant_directory WHERE tenant_code = 'monolith'").query(UUID.class)
                .single()));
        UUID person = UUID.randomUUID();
        CallContext asTenant = system.withIdentity(tenant, null);
        CallContextHolder.callAs(asTenant, () -> people.tx().write(() -> people.jdbc().sql(
                "INSERT INTO people.person (id, tenant_id, display_name, row_version) VALUES (:id, :tenant, 'Asha Rao', 1)")
                .param("id", person).param("tenant", tenant).update()));
        CallContextHolder.callAs(asTenant, () -> iam.tx().write(() -> iam.jdbc().sql(
                "UPDATE iam.user_account SET person_id = :person WHERE username = 'admin'")
                .param("person", person).update()));

        JsonNode login = call("POST", "/v1/iam/auth:login", null,
                "{\"tenantCode\":\"monolith\",\"identifier\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}", 200);
        String token = login.path("tokens").path("accessToken").asString();
        JsonNode me = call("GET", "/v1/iam/me", token, null, 200);
        assertThat(me.path("person").path("id").asString()).isEqualTo(person.toString());
        assertThat(me.path("person").path("displayName").asString()).isEqualTo("Asha Rao");
        assertThat(me.path("roles").get(0).path("roleCode").asString()).isEqualTo("administrator");

        JsonNode updated = call("PATCH", "/v1/people/me", token, "{\"aboutMe\":\"Principal\"}", 200, "If-Match", "\"1\"");
        assertThat(updated.path("aboutMe").asString()).isEqualTo("Principal");
        assertThat(call("GET", "/v1/iam/me", token, null, 200).path("person").path("aboutMe").asString())
                .isEqualTo("Principal");

        JsonNode settings = call("GET", "/v1/iam/me/settings", token, null, 200);
        assertThat(settings.path("items")).hasSize(10);
        assertThat(call("PUT", "/v1/iam/me/settings/ui.theme", token, "{\"value\":\"dark\"}", 200).path("sourceScope")
                .asString()).isEqualTo("user");
        assertThat(call("GET", "/oauth2/jwks", null, null, 200).path("keys")).hasSize(1);
        // the administrator's token from iam carries its tenant-wide permissions (tperms), so org's data scope lets
        // it create an institution; the module's standard operations are served in the assembled application
        JsonNode school = call("POST", "/v1/org/institutions", token, "{\"code\":\"MAIN\",\"name\":\"Main school\"}",
                201);
        JsonNode listed = call("GET", "/v1/org/institutions", token, null, 200);
        assertThat(listed.path("items").get(0).path("id").asString()).isEqualTo(school.path("id").asString());
        String id = school.path("id").asString();
        call("POST", "/v1/people/persons:sendGreetings", token, "{\"personIds\":[\"" + id + "\"],\"templateId\":\""
                + id + "\",\"channel\":\"sms\"}", 501);
    }
}
