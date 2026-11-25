package com.mysms.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.mysms.authclient.OperationTable;
import com.mysms.authclient.ServiceTokenProvider;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.master.v1.CountryServiceGrpc;
import com.mysms.master.v1.ListCountriesRequest;
import com.mysms.org.v1.InstitutionServiceGrpc;
import com.mysms.org.v1.ListInstitutionsRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.MetadataUtils;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.testkit.GrpcParity;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.iam.v1.ListSignUpInstitutionsRequest;
import com.mysms.iam.v1.SignUpRequest;
import com.mysms.iam.v1.SignUpResponse;
import com.mysms.iam.v1.UserAccountServiceGrpc;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
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
 * The monolith assembles all 22 modules, each on its own database with its own migrations, all 743 tables present,
 * every operation routed at the edge — and a real cross-module flow: sign-in at iam, GetMe reading the person from
 * people over in-process gRPC with the caller's token, a profile update at people.
 */
@SpringBootTest(classes = MySmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "management.endpoint.health.show-components=always")
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
            "careers",
            "privacy"};
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String ADMIN_PASSWORD = "monolith-admin-secret";
    private static final String SERVICE_SECRET = "finance-service-secret-0123456789abcdef";
    /** iam's own service identity: sign-up reads a tenant's active schools from org. */
    private static final String IAM_SERVICE_SECRET = "iam-service-secret-0123456789abcdefghijkl";
    /** A mailed one-time secret: {@code <tenant>.<256 random bits>}. */
    private static final Pattern SECRET = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-"
            + "[0-9a-f]{12}\\.[A-Za-z0-9_-]{43}");
    private static final GreenMail SMTP = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
    /** A fixed free port for REST, so iam issues finance's service token inside the same process over HTTP. */
    private static final int PORT = freePort();
    /** The one gRPC server's port: the monolith serves every module on it. */
    private static final int GRPC_PORT = freePort();

    @Autowired
    private Map<String, ModuleDatabase> databases;

    @Autowired
    private OperationTable operations;

    @Autowired
    private ServiceTokenProvider serviceTokens;

    @Autowired
    private com.mysms.authclient.RolePermissionSource roleData;

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
        registry.add("mysms.iam.bootstrap.tenant-code", () -> "monolith");
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> ADMIN_PASSWORD);
        // events cross modules on a real RabbitMQ, as in production
        registry.add("mysms.events.bus", () -> "rabbitmq");
        registry.add("mysms.events.rabbitmq.uri", () -> broker().toString());
        registry.add("mysms.events.rabbitmq.exchange", () -> "mysms.events." + UUID.randomUUID());
        // one real gRPC port for the process, and finance's service identity issued by iam in the same process
        registry.add("grpc.server.port", () -> GRPC_PORT);
        registry.add("mysms.auth.token-uri", () -> "http://localhost:" + PORT + "/oauth2/token");
        registry.add("mysms.finance.service-client.secret", () -> SERVICE_SECRET);
        registry.add("mysms.iam.service-clients.finance.secret-sha256", () -> sha256(SERVICE_SECRET));
        registry.add("mysms.iam.service-client.secret", () -> IAM_SERVICE_SECRET);
        registry.add("mysms.iam.service-clients.iam.secret-sha256", () -> sha256(IAM_SERVICE_SECRET));
        registry.add("mysms.iam.service-clients.iam.permissions[0]", () -> "org.institution.list_active");
        // a real SMTP server for the codes iam mails
        SMTP.start();
        registry.add("mysms.iam.smtp.host", () -> "127.0.0.1");
        registry.add("mysms.iam.smtp.port", () -> SMTP.getSmtp().getPort());
        registry.add("mysms.iam.smtp.from", () -> "no-reply@mysms.test");
        registry.add("mysms.iam.smtp.starttls-required", () -> "false");
        registry.add("mysms.iam.mail.app-url", () -> "https://app.mysms.test/");
    }

    @AfterAll
    static void stopSmtp() {
        SMTP.stop();
    }

    private static String sha256(String secret) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
     * A shared broker keeps an earlier run's module queues, bound to that run's exchanges, and a service refuses a
     * queue with other arguments, so the test starts from none.
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
     * An owner's deletion crosses modules over RabbitMQ: hr's {@code hr.staff.deleted} is relayed, academics clears the
     * class teacher who named that staff member, and the platform audits the change.
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
        CallContextHolder.callAs(context, () -> TestTransactions.of(platform).write(() -> platform.jdbc().sql(
                "INSERT INTO platform.tenant (id, code, name) VALUES (:tenant, 'e' || substr(md5(CAST(:tenant AS text)), "
                        + "1, 12), 'Events tenant')").param("tenant", tenant).update()));
        CallContextHolder.callAs(context, () -> TestTransactions.of(academics).write(() -> {
            cleanup.path("fixtures").forEach(f -> academics.jdbc().sql(f.asString()).param("tenant", tenant).update());
            return academics.jdbc().sql(cleanup.path("row").asString()).param("tenant", tenant).update();
        }));
        OutboxWriter hrOutbox = new OutboxWriter("hr", hr.jdbc(), JSON);
        CallContextHolder.callAs(context, () -> TestTransactions.of(hr).write(() -> {
            hrOutbox.publish("hr.staff.deleted", "hr.staff", staff, Map.of("id", staff.toString()));
            return null;
        }));

        long until = System.currentTimeMillis() + 30_000;
        boolean cleared = false;
        Integer audited = 0;
        while (System.currentTimeMillis() < until && (!cleared || audited == 0)) {
            Thread.sleep(200);
            cleared = CallContextHolder.callAs(context, () -> TestTransactions.of(academics).read(() -> academics.jdbc().sql(
                    "SELECT class_teacher_staff_id IS NULL FROM academics.class_section WHERE tenant_id = :tenantId "
                            + "AND id = :id").param("id", section).query(Boolean.class).single()));
            audited = CallContextHolder.callAs(context, () -> TestTransactions.of(platform).read(() -> platform.jdbc().sql(
                    "SELECT count(*) FROM platform.audit_log WHERE tenant_id = :tenantId AND subject_id = :id "
                            + "AND event = 'update'").param("id", section).query(Integer.class).single()));
        }
        assertThat(cleared).as("academics cleared the reference to the deleted staff member").isTrue();
        assertThat(audited).as("the platform recorded the change in the tenant's trail").isEqualTo(1);
    }

    /** The bootstrap tenant's id, from iam's directory. */
    private UUID bootstrapTenant() {
        ModuleDatabase iam = databases.get("iamDatabase");
        return CallContextHolder.callAs(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736"),
                () -> TestTransactions.of(iam).readWithoutTenant(() -> iam.jdbc().sql(
                        "SELECT tenant_id FROM iam.tenant_directory WHERE tenant_code = 'monolith'").query(UUID.class)
                        .single()));
    }

    /** Waits until the platform has registered the tenant iam announced (events cross RabbitMQ asynchronously). */
    private void awaitAtThePlatform(UUID tenant) throws InterruptedException {
        long until = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < until && !DATABASE.adminJdbc().sql(
                "SELECT EXISTS (SELECT 1 FROM platform.tenant WHERE id = :id)").param("id", tenant)
                .query(Boolean.class).single()) {
            Thread.sleep(200);
        }
    }

    /**
     * A fresh start: the platform registers the bootstrap tenant from iam's announcement, so the audit records of the
     * bootstrap itself land in that tenant's trail instead of failing their foreign key, and iam's directory follows the
     * platform row.
     */
    @Test
    void aFreshStartRegistersTheBootstrapTenantAndWritesItsAuditTrail() throws Exception {
        UUID tenant = bootstrapTenant();
        ModuleDatabase platform = databases.get("platformDatabase");
        CallContext context = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null);
        UUID role = DATABASE.adminJdbc().sql("SELECT id FROM iam.role WHERE tenant_id = :tenant "
                + "AND code = 'administrator'").param("tenant", tenant).query(UUID.class).single();
        long until = System.currentTimeMillis() + 30_000;
        Integer audited = 0;
        while (System.currentTimeMillis() < until && audited == 0) {
            Thread.sleep(200);
            audited = CallContextHolder.callAs(context, () -> TestTransactions.of(platform).read(() -> platform.jdbc()
                    .sql("SELECT count(*) FROM platform.audit_log WHERE tenant_id = :tenantId AND log_name = 'iam' "
                            + "AND subject_type = 'iam.role' AND subject_id = :role AND event = 'create'")
                    .param("role", role).query(Integer.class).single()));
        }
        assertThat(audited).as("the bootstrap's administrator role in the tenant's trail").isEqualTo(1);
        assertThat(DATABASE.adminJdbc().sql("SELECT code FROM platform.tenant WHERE id = :id").param("id", tenant)
                .query(String.class).single()).isEqualTo("monolith");
        Integer directory = 0;
        while (System.currentTimeMillis() < until && directory == 0) {
            directory = DATABASE.adminJdbc().sql("SELECT source_version FROM iam.tenant_directory WHERE tenant_id = :id")
                    .param("id", tenant).query(Integer.class).single();
            Thread.sleep(directory == 0 ? 200 : 0);
        }
        assertThat(directory).as("iam's directory follows platform.tenant.created").isOne();
    }

    /** In one process every module's permissions filter reads iam's role data directly, never over gRPC. */
    @Test
    void everyFilterReadsIamsRoleDataInProcess() {
        assertThat(roleData).isInstanceOfSatisfying(com.mysms.authclient.testing.TestRoleData.class, test ->
                assertThat(test.iam()).as("behind the component tests' role fixtures")
                        .isInstanceOf(com.mysms.iam.business.LocalRolePermissions.class));
    }

    @Test
    void everyModuleOwnsItsDatabaseAndSchema() {
        assertThat(databases).hasSize(22);
        ModuleDatabase any = databases.get("masterDatabase");
        Integer tables = any.jdbc().sql(
                "SELECT count(*) FROM information_schema.tables WHERE table_name <> 'flyway_schema_history'"
                        + " AND table_schema = ANY(:schemas)")
                .param("schemas", MODULES).query(Integer.class).single();
        assertThat(tables).as("every CREATE TABLE of database/migrations").isEqualTo(743);
        Integer histories = any.jdbc().sql(
                // the catalog, not information_schema: the application role has no privilege on the Flyway history
                "SELECT count(*) FROM pg_catalog.pg_class WHERE relname = 'flyway_schema_history' AND relkind = 'r'")
                .query(Integer.class).single();
        assertThat(histories).isEqualTo(22);
        assertThat(operations.size()).isEqualTo(2123);
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
        UUID tenant = CallContextHolder.callAs(system, () -> TestTransactions.of(iam).readWithoutTenant(() -> iam.jdbc()
                .sql("SELECT tenant_id FROM iam.tenant_directory WHERE tenant_code = 'monolith'").query(UUID.class)
                .single()));
        UUID person = UUID.randomUUID();
        CallContext asTenant = system.withIdentity(tenant, null);
        CallContextHolder.callAs(asTenant, () -> TestTransactions.of(people).write(() -> people.jdbc().sql(
                "INSERT INTO people.person (id, tenant_id, display_name, row_version) VALUES (:id, :tenant, 'Asha Rao', 1)")
                .param("id", person).param("tenant", tenant).update()));
        CallContextHolder.callAs(asTenant, () -> TestTransactions.of(iam).write(() -> iam.jdbc().sql(
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
        assertThat(settings.path("items")).as("every setting definition of iam's catalogue").hasSize(13);
        assertThat(call("PUT", "/v1/iam/me/settings/ui.theme", token, "{\"value\":\"dark\"}", 200).path("sourceScope")
                .asString()).isEqualTo("user");
        assertThat(call("GET", "/oauth2/jwks", null, null, 200).path("keys")).hasSize(1);
        // tenant-wide permissions (tperms) let the administrator create an institution in org
        JsonNode school = call("POST", "/v1/org/institutions", token, "{\"code\":\"MAIN\",\"name\":\"Main school\"}",
                201);
        JsonNode listed = call("GET", "/v1/org/institutions", token, null, 200);
        assertThat(listed.path("data").findValuesAsString("id")).contains(school.path("id").asString());
        String id = school.path("id").asString();
        call("POST", "/v1/people/persons:sendGreetings", token, "{\"personIds\":[\"" + id + "\"],\"templateId\":\""
                + id + "\",\"channel\":\"sms\"}", 422);
    }

    /**
     * A guest names its school with X-Tenant-Code: lms and careers resolve it through iam over in-process gRPC;
     * an unknown code is 404 before any module reads.
     */
    @Test
    void guestsReadPublicPagesOfTheSchoolTheirCodeNames() throws Exception {
        JsonNode resolved = call("GET", "/v1/iam/tenant-codes/MONOLITH", null, null, 200);
        assertThat(resolved.path("tenantId").asString()).as("codes compare case-insensitively")
                .isEqualTo(call("GET", "/v1/iam/tenant-codes/monolith", null, null, 200).path("tenantId").asString());
        call("GET", "/v1/iam/tenant-codes/nowhere", null, null, 404);

        JsonNode catalog = call("GET", "/v1/lms/catalog", null, null, 200, "X-Tenant-Code", "monolith");
        assertThat(catalog.path("courses").isArray()).isTrue();
        call("GET", "/v1/lms/public-settings", null, null, 200, "X-Tenant-Code", "monolith");
        call("GET", "/v1/careers/job-openings", null, null, 200, "X-Tenant-Code", "monolith");
        call("GET", "/v1/lms/catalog", null, null, 404, "X-Tenant-Code", "nowhere");
        call("GET", "/v1/lms/catalog", null, null, 404);
    }

    /**
     * A guest reads a school's open cohorts and verifies its certificates (both public operations), and a school that
     * keeps guests out (lms allowGuestAccess false) answers every guest read 404 while its sign-in page still gets the
     * settings it needs; signed-in members are unaffected.
     */
    @Test
    void guestsReadOpenCohortsAndCertificatesUnlessTheSchoolKeepsThemOut() throws Exception {
        ModuleDatabase iam = databases.get("iamDatabase");
        ModuleDatabase lms = databases.get("lmsDatabase");
        CallContext system = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736");
        UUID tenant = CallContextHolder.callAs(system, () -> TestTransactions.of(iam).readWithoutTenant(() -> iam.jdbc()
                .sql("SELECT tenant_id FROM iam.tenant_directory WHERE tenant_code = 'monolith'").query(UUID.class)
                .single()));
        UUID cohort = UUID.randomUUID();
        UUID certificate = UUID.randomUUID();
        UUID settings = UUID.randomUUID();
        CallContext asTenant = system.withIdentity(tenant, null);
        CallContextHolder.callAs(asTenant, () -> TestTransactions.of(lms).write(() -> {
            lms.jdbc().sql("INSERT INTO lms.cohort (id, tenant_id, title, start_date, end_date, timezone, is_published, "
                    + "allow_self_enrollment, external_ref) VALUES (:id, :tenant, 'Open cohort', DATE '2026-01-01', "
                    + "DATE '2099-12-31', 'UTC', true, true, 'INTERNAL-7')").param("id", cohort).param("tenant", tenant)
                    .update();
            return lms.jdbc().sql("INSERT INTO lms.certificate (id, tenant_id, user_id, cohort_id, issue_date, "
                    + "template_code, is_published) VALUES (:id, :tenant, :user, :cohort, DATE '2026-06-30', 'standard', "
                    + "true)").param("id", certificate).param("tenant", tenant).param("user", UUID.randomUUID())
                    .param("cohort", cohort).update();
        }));
        JsonNode open = call("GET", "/v1/lms/cohorts/" + cohort, null, null, 200, "X-Tenant-Code", "monolith");
        assertThat(open.path("title").asString()).isEqualTo("Open cohort");
        assertThat(open.has("externalRef")).as("guest projection").isFalse();
        assertThat(call("GET", "/v1/lms/cohorts", null, null, 200, "X-Tenant-Code", "monolith").path("data")
                .findValuesAsString("id")).contains(cohort.toString());
        call("GET", "/v1/lms/cohorts/" + cohort + "/courses", null, null, 200, "X-Tenant-Code", "monolith");
        JsonNode verified = call("GET", "/v1/lms/certificates/" + certificate + ":verify", null, null, 200,
                "X-Tenant-Code", "monolith");
        assertThat(verified.path("valid").asBoolean()).isTrue();
        assertThat(verified.has("userId")).isFalse();
        try {
            CallContextHolder.callAs(asTenant, () -> TestTransactions.of(lms).write(() -> lms.jdbc().sql(
                    "INSERT INTO lms.settings (id, tenant_id, allow_guest_access, contact_us_email) VALUES (:id, "
                            + ":tenant, false, 'office@example.org')").param("id", settings).param("tenant", tenant)
                    .update()));
            for (String path : List.of("/v1/lms/catalog", "/v1/lms/courses", "/v1/lms/cohorts",
                    "/v1/lms/cohorts/" + cohort, "/v1/lms/certificates/" + certificate + ":verify")) {
                call("GET", path, null, null, 404, "X-Tenant-Code", "monolith");
            }
            JsonNode signIn = call("GET", "/v1/lms/public-settings", null, null, 200, "X-Tenant-Code", "monolith");
            assertThat(signIn.path("allowGuestAccess").asBoolean()).isFalse();
            assertThat(signIn.has("contactUsEmail")).as("only what the sign-in page needs").isFalse();
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", UUID.randomUUID().toString());
            claims.put("tid", tenant.toString());
            claims.put("aud", "mysms");
            claims.put("perms", List.of("lms.*"));
            claims.put("tperms", List.of("lms.*"));
            claims.put("ins", List.of("*"));
            String member = TOKENS.custom(claims, Instant.now().plusSeconds(900));
            call("GET", "/v1/lms/cohorts/" + cohort, member, null, 200);
            assertThat(call("GET", "/v1/lms/public-settings", member, null, 200).path("contactUsEmail").asString())
                    .isEqualTo("office@example.org");
        } finally {
            CallContextHolder.callAs(asTenant, () -> TestTransactions.of(lms).write(() -> lms.jdbc().sql(
                    "UPDATE lms.settings SET deleted_at = now() WHERE id = :id").param("id", settings).update()));
        }
    }

    /**
     * A member profile shows enrolments only within the caller's data scope for the profile permission: the school
     * administrator reading their own profile with a grant for one school sees that school's enrolment, not another's.
     */
    @Test
    void aMemberProfileShowsOnlyEnrolmentsWithinTheCallersReach() throws Exception {
        ModuleDatabase iam = databases.get("iamDatabase");
        ModuleDatabase lms = databases.get("lmsDatabase");
        CallContext system = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736");
        UUID tenant = CallContextHolder.callAs(system, () -> TestTransactions.of(iam).readWithoutTenant(() -> iam.jdbc()
                .sql("SELECT tenant_id FROM iam.tenant_directory WHERE tenant_code = 'monolith'").query(UUID.class)
                .single()));
        UUID admin = CallContextHolder.callAs(system.withIdentity(tenant, null), () -> TestTransactions.of(iam)
                .read(() -> iam.jdbc().sql("SELECT id FROM iam.user_account WHERE username = 'admin'")
                        .query(UUID.class).single()));
        UUID mine = UUID.randomUUID();
        UUID theirs = UUID.randomUUID();
        UUID inReach = UUID.randomUUID();
        UUID outOfReach = UUID.randomUUID();
        CallContextHolder.callAs(system.withIdentity(tenant, null), () -> TestTransactions.of(lms).write(() -> {
            for (UUID[] course : new UUID[][] {{mine, inReach}, {theirs, outOfReach}}) {
                lms.jdbc().sql("INSERT INTO lms.course (id, tenant_id, institution_id, title) VALUES (:id, :tenant, "
                        + ":institution, 'Course')").param("id", course[0]).param("tenant", tenant)
                        .param("institution", course[1]).update();
                lms.jdbc().sql("INSERT INTO lms.enrollment (id, tenant_id, course_id, user_id) VALUES (:id, :tenant, "
                        + ":course, :user)").param("id", UUID.randomUUID()).param("tenant", tenant)
                        .param("course", course[0]).param("user", admin).update();
            }
            return null;
        }));
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", admin.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("lms.enrollment.get_member_profile", "iam.user_account.get_me"));
        claims.put("ins", List.of(inReach.toString(), outOfReach.toString()));
        String token = TOKENS.custom(claims, Instant.now().plusSeconds(900));
        JsonNode profile = call("GET", "/v1/lms/members/" + admin + ":profile", token, null, 200,
                "X-Institution-Id", inReach.toString());
        assertThat(profile.path("enrollments").findValuesAsString("courseId")).containsExactly(mine.toString());
        claims.put("perms", List.of("lms.enrollment.read", "iam.user_account.get_me"));
        call("GET", "/v1/lms/members/" + admin + ":profile", TOKENS.custom(claims, Instant.now().plusSeconds(900)),
                null, 403);
    }

    /**
     * Attendance for a timetable session crosses four modules with the caller's own token: session and students from
     * timetable, roster from sis, time zone from org, the sheet in attendance.
     */
    @Test
    void attendanceForATimetableSessionCrossesTimetableSisAndOrg() throws Exception {
        UUID tenant = UUID.randomUUID();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("*"));
        claims.put("tperms", List.of("*"));
        claims.put("ins", List.of("*"));
        String admin = TOKENS.custom(claims, Instant.now().plusSeconds(900));
        // as in production, the tenant exists at the platform: its audit trail refers to it
        ModuleDatabase platform = databases.get("platformDatabase");
        CallContextHolder.callAs(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(tenant, null),
                () -> TestTransactions.of(platform).write(() -> platform.jdbc().sql(
                        "INSERT INTO platform.tenant (id, code, name) VALUES (:tenant, 'a' || substr(md5(CAST(:tenant AS "
                                + "text)), 1, 12), 'Attendance tenant')").param("tenant", tenant).update()));

        // 19:00 on 2 March in Los Angeles is 3 March in UTC: the sheet must carry the school's date
        String institution = call("POST", "/v1/org/institutions", admin,
                "{\"code\":\"LA\",\"name\":\"Evening school\",\"timezone\":\"America/Los_Angeles\"}", 201)
                .path("id").asString();
        UUID classSection = UUID.randomUUID();
        List<String> roster = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String student = call("POST", "/v1/sis/students", admin, "{\"personId\":\"" + UUID.randomUUID()
                    + "\",\"institutionId\":\"" + institution + "\"}", 201).path("id").asString();
            call("POST", "/v1/sis/student-enrollments", admin, "{\"studentId\":\"" + student
                    + "\",\"academicYearId\":\"" + UUID.randomUUID() + "\",\"classSectionId\":\"" + classSection
                    + "\"}", 201);
            roster.add(student);
        }
        String session = call("POST", "/v1/timetable/sessions", admin, "{\"institutionId\":\"" + institution
                + "\",\"startAt\":\"2026-03-03T03:00:00Z\",\"endAt\":\"2026-03-03T03:45:00Z\",\"subjectId\":\""
                + UUID.randomUUID() + "\",\"staffId\":\"" + UUID.randomUUID() + "\",\"classSectionId\":\""
                + classSection + "\"}", 201).path("id").asString();
        String forSession = "{\"timetableSessionId\":\"" + session + "\"}";

        JsonNode noRegister = call("POST", "/v1/attendance/attendance-sheets:forSession", admin, forSession, 409);
        assertThat(noRegister.path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");
        call("POST", "/v1/attendance/attendance-registers", admin, "{\"institutionId\":\"" + institution
                + "\",\"code\":\"EVE\",\"name\":\"Evening class\",\"registerScope\":\"class_section\","
                + "\"classSectionId\":\"" + classSection + "\"}", 201);

        JsonNode sheet = call("POST", "/v1/attendance/attendance-sheets:forSession", admin, forSession, 200);
        String sheetId = sheet.path("id").asString();
        assertThat(sheet.path("status").asString()).isEqualTo("in_progress");
        assertThat(sheet.path("attendanceDate").asString()).isEqualTo("2026-03-02");
        assertThat(sheet.path("timetableSessionId").asString()).isEqualTo(session);
        JsonNode marks = call("GET", "/v1/attendance/attendance-sheets/" + sheetId + "/records?filter="
                + URLEncoder.encode("attendanceDate >= \"2026-03-02\" AND attendanceDate < \"2026-03-03\"",
                        StandardCharsets.UTF_8), admin, null, 200);
        assertThat(marks.path("data")).hasSize(2);
        for (JsonNode mark : marks.path("data")) {
            assertThat(roster).contains(mark.path("studentId").asString());
            assertThat(mark.path("status").asString()).isEqualTo("present");
        }
        assertThat(call("POST", "/v1/attendance/attendance-sheets:forSession", admin, forSession, 200).path("id")
                .asString()).as("the session's sheet is opened once").isEqualTo(sheetId);

        call("POST", "/v1/attendance/attendance-sheets/" + sheetId + ":recordAttendance", admin,
                "{\"marks\":[{\"studentId\":\"" + roster.get(0) + "\",\"status\":\"absent\"}],\"complete\":true}",
                200);
        JsonNode summary = call("GET", "/v1/attendance/summaries?institutionId=" + institution
                + "&fromDate=2026-03-02&toDate=2026-03-02&groupBy=class_section", admin, null, 200);
        assertThat(summary.path("data")).hasSize(1);
        assertThat(summary.path("data").get(0).path("present").asInt()).isEqualTo(1);
        assertThat(summary.path("data").get(0).path("absent").asInt()).isEqualTo(1);
        assertThat(summary.path("data").get(0).path("attendancePercent").asString()).isEqualTo("50.00");

        // a session that names its own students expects exactly those, not the class roster
        String elective = call("POST", "/v1/timetable/sessions", admin, "{\"institutionId\":\"" + institution
                + "\",\"startAt\":\"2026-03-04T18:00:00Z\",\"endAt\":\"2026-03-04T18:45:00Z\",\"subjectId\":\""
                + UUID.randomUUID() + "\",\"staffId\":\"" + UUID.randomUUID() + "\",\"classSectionId\":\""
                + classSection + "\"}", 201).path("id").asString();
        call("POST", "/v1/timetable/sessions/" + elective + "/students", admin,
                "{\"studentId\":\"" + roster.get(1) + "\"}", 201);
        JsonNode electiveSheet = call("POST", "/v1/attendance/attendance-sheets:forSession", admin,
                "{\"timetableSessionId\":\"" + elective + "\"}", 200);
        assertThat(electiveSheet.path("attendanceDate").asString()).isEqualTo("2026-03-04");
        JsonNode electiveMarks = call("GET", "/v1/attendance/attendance-sheets/" + electiveSheet.path("id").asString()
                + "/records?filter=" + URLEncoder.encode(
                        "attendanceDate >= \"2026-03-04\" AND attendanceDate < \"2026-03-05\"",
                        StandardCharsets.UTF_8), admin, null, 200);
        assertThat(electiveMarks.path("data")).hasSize(1);
        assertThat(electiveMarks.path("data").get(0).path("studentId").asString()).isEqualTo(roster.get(1));

        // a first mark names a student the sheet does not know: sis must know them as a student of this institution
        String record = "/v1/attendance/attendance-sheets/" + electiveSheet.path("id").asString() + ":recordAttendance";
        String newcomer = call("POST", "/v1/sis/students", admin, "{\"personId\":\"" + UUID.randomUUID()
                + "\",\"institutionId\":\"" + institution + "\"}", 201).path("id").asString();
        String otherSchool = call("POST", "/v1/org/institutions", admin, "{\"code\":\"NY\",\"name\":\"Other school\"}",
                201).path("id").asString();
        String outsider = call("POST", "/v1/sis/students", admin, "{\"personId\":\"" + UUID.randomUUID()
                + "\",\"institutionId\":\"" + otherSchool + "\"}", 201).path("id").asString();
        JsonNode strangers = call("POST", record, admin, "{\"marks\":[{\"studentId\":\"" + roster.get(0)
                + "\",\"status\":\"late\"},{\"studentId\":\"" + outsider + "\",\"status\":\"absent\"},{\"studentId\":\""
                + UUID.randomUUID() + "\",\"status\":\"absent\"}]}", 422);
        assertThat(strangers.path("errors")).hasSize(2);
        assertThat(strangers.path("errors").get(0).path("field").asString()).isEqualTo("marks[1].studentId");
        assertThat(strangers.path("errors").get(0).path("code").asString()).isEqualTo("NOT_FOUND");
        assertThat(strangers.path("errors").get(1).path("field").asString()).isEqualTo("marks[2].studentId");
        // a classmate is known from the roster, any other student of the school from sis itself
        JsonNode welcomed = call("POST", record, admin, "{\"marks\":[{\"studentId\":\"" + roster.get(0)
                + "\",\"status\":\"late\"},{\"studentId\":\"" + newcomer + "\",\"status\":\"present\"}]}", 200);
        assertThat(welcomed.path("items")).hasSize(2);
        // the roster is a teacher's permission; a student outside the class needs the permission to read students
        claims.put("perms", List.of("attendance.attendance_sheet.record_attendance",
                "sis.student_enrollment.list_class_roster"));
        claims.put("tperms", List.of("attendance.attendance_sheet.record_attendance",
                "sis.student_enrollment.list_class_roster"));
        String teacher = TOKENS.custom(claims, Instant.now().plusSeconds(900));
        String another = call("POST", "/v1/sis/students", admin, "{\"personId\":\"" + UUID.randomUUID()
                + "\",\"institutionId\":\"" + institution + "\"}", 201).path("id").asString();
        JsonNode needsStudents = call("POST", record, teacher, "{\"marks\":[{\"studentId\":\"" + another
                + "\",\"status\":\"present\"}]}", 403);
        assertThat(needsStudents.path("detail").asString()).contains("sis.student.read");

        // a caller who may not read sessions is refused, naming what is missing; an unknown session is a field error
        claims.put("perms", List.of("attendance.attendance_sheet.take_attendance_for_session"));
        claims.put("tperms", List.of("attendance.attendance_sheet.take_attendance_for_session"));
        String narrow = TOKENS.custom(claims, Instant.now().plusSeconds(900));
        JsonNode refused = call("POST", "/v1/attendance/attendance-sheets:forSession", narrow, forSession, 403);
        assertThat(refused.path("detail").asString()).contains("timetable.session.read");
        JsonNode unknown = call("POST", "/v1/attendance/attendance-sheets:forSession", admin,
                "{\"timetableSessionId\":\"" + UUID.randomUUID() + "\"}", 422);
        assertThat(unknown.path("errors").get(0).path("field").asString()).isEqualTo("timetableSessionId");
    }

    /**
     * Self sign-up names its school: the guest list offers the tenant's active schools only, an unknown, inactive or
     * other tenant's school is the same 422, and the new account's role reaches that one school — its token carries
     * no other institution, so another school's rows are 404. REST and gRPC answer alike.
     */
    @Test
    void aSelfSignedUpStudentReachesOnlyTheSchoolItSignedUpAt() throws Exception {
        ModuleDatabase iam = databases.get("iamDatabase");
        CallContext system = CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736");
        UUID tenant = bootstrapTenant();
        // the audit trail of every write below refers to the tenant the platform registered from the bootstrap
        awaitAtThePlatform(tenant);
        String admin = call("POST", "/v1/iam/auth:login", null, "{\"tenantCode\":\"monolith\",\"identifier\":"
                + "\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}", 200).path("tokens").path("accessToken")
                .asString();
        String north = call("POST", "/v1/org/institutions", admin, "{\"code\":\"NORTH\",\"name\":\"North school\"}",
                201).path("id").asString();
        String south = call("POST", "/v1/org/institutions", admin, "{\"code\":\"SOUTH\",\"name\":\"South school\"}",
                201).path("id").asString();
        JsonNode shut = call("POST", "/v1/org/institutions", admin, "{\"code\":\"SHUT\",\"name\":\"Shut school\"}",
                201);
        call("PATCH", "/v1/org/institutions/" + shut.path("id").asString(), admin, "{\"status\":\"inactive\"}", 200,
                "If-Match", shut.path("etag").asString());
        UUID otherTenant = UUID.randomUUID();
        UUID elsewhere = UUID.randomUUID();
        ModuleDatabase orgDatabase = databases.get("orgDatabase");
        CallContextHolder.callAs(system.withIdentity(otherTenant, null), () -> TestTransactions.of(orgDatabase)
                .write(() -> orgDatabase.jdbc().sql("INSERT INTO org.institution (id, tenant_id, code, name, "
                        + "row_version) VALUES (:id, :tenant, 'FAR', 'Far school', 1)").param("id", elsewhere)
                        .param("tenant", otherTenant).update()));

        String schools = "/v1/iam/tenant-codes/monolith/sign-up-institutions";
        call("GET", schools, null, null, 403);
        CallContextHolder.callAs(system.withIdentity(tenant, null), () -> TestTransactions.of(iam).write(() -> {
            UUID role = UUID.randomUUID();
            iam.jdbc().sql("INSERT INTO iam.role (id, tenant_id, code) VALUES (:id, :tenant, 'learner')")
                    .param("id", role).param("tenant", tenant).update();
            iam.jdbc().sql("INSERT INTO iam.role_permission (id, tenant_id, role_id, permission_id) SELECT :id, "
                    + ":tenant, :role, pm.id FROM iam.permission pm WHERE pm.code = 'org.institution.read'")
                    .param("id", UUID.randomUUID()).param("tenant", tenant).param("role", role).update();
            return iam.jdbc().sql("INSERT INTO iam.setting_value (id, tenant_id, scope_type, setting_key, value) "
                    + "VALUES (:a, :tenant, 'tenant', 'security.selfSignUp', 'true'), (:b, :tenant, 'tenant', "
                    + "'security.selfSignUpRole', 'learner')").param("a", UUID.randomUUID())
                    .param("b", UUID.randomUUID()).param("tenant", tenant).update();
        }));

        JsonNode offered = call("GET", schools, null, null, 200).path("items");
        List<String> offeredIds = offered.findValuesAsString("id");
        assertThat(offeredIds).contains(north, south).doesNotContain(shut.path("id").asString(), elsewhere.toString());
        assertThat(offered.get(0).propertyNames()).as("the public projection").containsExactlyInAnyOrder("id", "name");
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", GRPC_PORT).usePlaintext().build();
        try {
            UserAccountServiceGrpc.UserAccountServiceBlockingStub accounts = UserAccountServiceGrpc.newBlockingStub(
                    channel);
            assertThat(accounts.listSignUpInstitutions(ListSignUpInstitutionsRequest.newBuilder()
                    .setTenantCode("monolith").build()).getItemsList()).extracting(i -> i.getId())
                    .containsExactlyElementsOf(offeredIds);

            String form = "{\"tenantCode\":\"monolith\",\"institutionId\":\"%s\",\"email\":\"%s\","
                    + "\"fullName\":\"Asha Learner\",\"acceptTerms\":true,\"personaCategory\":\"student\"}";
            JsonNode unknown = call("POST", "/v1/iam/auth:signUp", null, String.format(form, UUID.randomUUID(),
                    "learner@school.test"), 422);
            assertThat(unknown.path("errors").get(0).path("field").asString()).isEqualTo("institutionId");
            for (String refused : List.of(elsewhere.toString(), shut.path("id").asString())) {
                JsonNode same = call("POST", "/v1/iam/auth:signUp", null, String.format(form, refused,
                        "learner@school.test"), 422);
                assertThat(same.path("errors")).as("another tenant's or an inactive school: no different answer")
                        .isEqualTo(unknown.path("errors"));
            }
            SignUpRequest.Builder overGrpc = SignUpRequest.newBuilder().setTenantCode("monolith")
                    .setInstitutionId(elsewhere.toString()).setEmail("learner@school.test").setFullName("Asha Learner")
                    .setAcceptTerms(true);
            StatusRuntimeException refused = org.junit.jupiter.api.Assertions.assertThrows(
                    StatusRuntimeException.class, () -> accounts.signUp(overGrpc.build()));
            GrpcParity.Answer refusal = GrpcParity.refusal(refused);
            assertThat(refusal.status()).isEqualTo(422);
            assertThat(refusal.code()).isEqualTo(unknown.path("code").asString());
            assertThat(SMTP.getReceivedMessages()).as("nothing was mailed for a refused school").isEmpty();

            JsonNode receipt = call("POST", "/v1/iam/auth:signUp", null, String.format(form, north,
                    "Learner@School.test"), 200);
            assertThat(receipt.has("id")).isFalse();
            SignUpResponse grpcReceipt = accounts.signUp(overGrpc.setInstitutionId(south)
                    .setEmail("grpc.learner@school.test").build());
            assertThat(GrpcParity.json(grpcReceipt.getUserAccount()).propertyNames()).as("the same receipt shape")
                    .containsExactlyInAnyOrderElementsOf(receipt.propertyNames());
            assertThat(SMTP.waitForIncomingEmail(10_000, 2)).as("both codes are mailed").isTrue();
        } finally {
            channel.shutdownNow();
        }

        String code = null;
        for (jakarta.mail.internet.MimeMessage mail : SMTP.getReceivedMessages()) {
            if ("Learner@School.test".equals(mail.getAllRecipients()[0].toString())) {
                Matcher m = SECRET.matcher(GreenMailUtil.getBody(mail));
                assertThat(m.find()).isTrue();
                code = m.group();
            }
        }
        assertThat(code).as("the code mailed to the student").isNotNull();
        call("POST", "/v1/iam/auth:resetPassword", null, "{\"token\":\"" + code + "\",\"newPassword\":"
                + "\"learner-strong-pass-1\"}", 204);
        String student = call("POST", "/v1/iam/auth:login", null, "{\"tenantCode\":\"monolith\",\"identifier\":"
                + "\"learner@school.test\",\"password\":\"learner-strong-pass-1\"}", 200).path("tokens")
                .path("accessToken").asString();
        JsonNode claims = JSON.readTree(new String(Base64.getUrlDecoder().decode(student.split("\\.")[1]),
                StandardCharsets.UTF_8));
        List<String> institutions = new ArrayList<>();
        claims.path("ins").forEach(i -> institutions.add(i.asString()));
        assertThat(institutions).as("the one school it signed up at, never every institution").containsExactly(north);
        assertThat(claims.path("rgr").toString()).as("no tenant-wide role").doesNotContain("@tenant");
        assertThat(claims.path("dg")).as("and no direct grant, which would reach every institution").isEmpty();
        assertThat(call("GET", "/v1/org/institutions/" + north, student, null, 200).path("name").asString())
                .isEqualTo("North school");
        call("GET", "/v1/org/institutions/" + south, student, null, 404);
        assertThat(call("GET", "/v1/org/institutions", student, null, 200).path("data").findValuesAsString("id"))
                .containsExactly(north);
    }

    /**
     * One gRPC server serves every module behind the REST edge's rules: public reads without a token, the rest refused
     * without one; a module's service token is refused on contract operations over both transports.
     */
    @Test
    void oneGrpcServerServesEveryModuleBehindTheRestEdgesRules() throws Exception {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", GRPC_PORT).usePlaintext().build();
        try {
            HealthGrpc.HealthBlockingStub health = HealthGrpc.newBlockingStub(channel);
            for (String service : List.of("mysms.master.v1.CountryService", "mysms.org.v1.InstitutionService",
                    "mysms.org.v1.AcademicYearService")) {
                assertThat(health.check(HealthCheckRequest.newBuilder().setService(service).build()).getStatus())
                        .as(service).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
            }
            assertThat(CountryServiceGrpc.newBlockingStub(channel).listCountries(ListCountriesRequest
                    .getDefaultInstance()).getPagination().getPage()).isEqualTo(1);
            StatusRuntimeException anonymous = org.junit.jupiter.api.Assertions.assertThrows(
                    StatusRuntimeException.class, () -> InstitutionServiceGrpc.newBlockingStub(channel)
                            .listInstitutions(ListInstitutionsRequest.getDefaultInstance()));
            assertThat(anonymous.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);

            String service = serviceTokens.token("finance");
            call("GET", "/v1/org/institutions", service, null, 403);
            Metadata headers = new Metadata();
            headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + service);
            StatusRuntimeException refused = org.junit.jupiter.api.Assertions.assertThrows(
                    StatusRuntimeException.class, () -> InstitutionServiceGrpc.newBlockingStub(channel)
                            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                            .listInstitutions(ListInstitutionsRequest.getDefaultInstance()));
            assertThat(refused.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
        } finally {
            channel.shutdownNow();
        }
    }
}
