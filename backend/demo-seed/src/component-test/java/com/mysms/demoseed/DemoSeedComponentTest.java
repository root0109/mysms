package com.mysms.demoseed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.app.MySmsApplication;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.demoseed.client.ApiClient;
import com.mysms.demoseed.client.ApiException;
import com.mysms.demoseed.client.Bearer;
import com.mysms.demoseed.client.Filter;
import com.mysms.demoseed.client.SignIn;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The seeder against the real monolith (real PostgreSQL, RabbitMQ, signed tokens, iam's installation bootstrap making
 * the demo tenant): a small school is seeded, every module's rows are counted through the API, each persona signs in
 * to their own data, a second run changes nothing, and remove deletes the tenant so nobody can sign in to it.
 */
@SpringBootTest(classes = MySmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class DemoSeedComponentTest {

    private static final String[] MODULES = {"master", "platform", "iam", "org", "people", "privacy", "academics",
            "sis", "hr", "payroll", "admission", "attendance", "facility", "timetable", "assessment", "lms", "finance",
            "library", "transport", "comms", "frontoffice", "careers"};
    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TENANT = "demo-20261118";
    private static final String ADMIN_PASSWORD = "demo-admin-secret";
    private static final String PERSONA_PASSWORD = "demo-persona-secret";
    private static final String SERVICE_SECRET = "finance-service-secret-0123456789abcdef";
    private static final String IAM_SERVICE_SECRET = "iam-service-secret-0123456789abcdefghijkl";
    private static final int PORT = freePort();
    private static final int GRPC_PORT = freePort();
    private static final URI BASE = URI.create("http://localhost:" + PORT);
    private static URI broker;

    @TempDir
    private Path manifests;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, MODULES);
        registry.add("server.port", () -> PORT);
        registry.add("grpc.server.port", () -> GRPC_PORT);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.iam.bootstrap.tenant-code", () -> TENANT);
        registry.add("mysms.iam.bootstrap.admin-username", () -> "admin");
        registry.add("mysms.iam.bootstrap.admin-password", () -> ADMIN_PASSWORD);
        registry.add("mysms.events.bus", () -> "rabbitmq");
        registry.add("mysms.events.rabbitmq.uri", () -> broker().toString());
        registry.add("mysms.events.rabbitmq.exchange", () -> "mysms.events." + UUID.randomUUID());
        registry.add("mysms.auth.token-uri", () -> "http://localhost:" + PORT + "/oauth2/token");
        registry.add("mysms.finance.service-client.secret", () -> SERVICE_SECRET);
        registry.add("mysms.iam.service-clients.finance.secret-sha256", () -> sha256(SERVICE_SECRET));
        registry.add("mysms.iam.service-client.secret", () -> IAM_SERVICE_SECRET);
        registry.add("mysms.iam.service-clients.iam.secret-sha256", () -> sha256(IAM_SERVICE_SECRET));
        registry.add("mysms.iam.service-clients.iam.permissions[0]", () -> "org.institution.list_active");
    }

    @Test
    void seedsEveryModuleOnceSignsInEveryPersonaAndRemovesTheTenant() {
        DemoSeed seeder = new DemoSeed(settings(DemoSettings.Action.SEED), JSON);
        Manifest first = seeder.seed();

        assertThat(first.tenantCode()).isEqualTo(TENANT);
        assertThat(first.rows().get("org")).containsEntry("institution", 1).containsEntry("academicYear", 1)
                .containsEntry("academicTerm", 2).containsEntry("department", 5);
        assertThat(first.rows().get("academics")).containsEntry("gradeLevel", 10).containsEntry("classSection", 20)
                .containsEntry("subject", 8).containsEntry("classSubject", 120);
        assertThat(first.rows().get("hr")).containsEntry("staff", 27).containsEntry("designation", 7);
        assertThat(first.rows().get("sis")).containsEntry("student", 20).containsEntry("guardian", 20)
                .containsEntry("studentEnrollment", 20).containsEntry("studentGuardian", 20);
        assertThat(first.rows().get("iam")).containsEntry("userAccount", 8).containsEntry("role", 7);
        assertThat(first.rows().get("academics")).containsEntry("teachingAssignment", 120);
        // one past week and this one: 2 weeks x 6 days x 20 classes x 6 periods; a roll call on each school day
        // before 18 November (9-14 and 16-17 November) in every class
        assertThat(first.rows().get("timetable")).containsEntry("period", 6).containsEntry("session", 1440);
        assertThat(first.rows().get("attendance")).containsEntry("attendanceRegister", 20)
                .containsEntry("attendanceSheet", 160).containsEntry("absenceReason", 3);
        assertThat(first.personas()).hasSize(8);
        Map<String, Integer> counted = countThroughTheApi(first);
        assertThat(counted).containsEntry("institutions", 1).containsEntry("classSections", 20)
                .containsEntry("staff", 27).containsEntry("students", 20).containsEntry("enrollments", 20)
                .containsEntry("teachingAssignments", 120).containsEntry("attendanceSheets", 160);

        personasSignInToTheirOwnData();

        Manifest second = seeder.seed();
        assertThat(second).as("a second run finds every row and creates none").isEqualTo(first);
        assertThat(countThroughTheApi(second)).isEqualTo(counted);

        remove(first);
    }

    @Test
    void refusesTenantsThatAreNotDemoTenants() {
        assertThatThrownBy(() -> new DemoSettings(BASE, DemoSettings.Action.SEED, "school", "admin", ADMIN_PASSWORD,
                PERSONA_PASSWORD, "", 1, 0, 1L, manifests, LocalDate.of(2026, 11, 18)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("demo-");
        DemoSettings unknown = new DemoSettings(BASE, DemoSettings.Action.REMOVE, "demo-unknown", "", "", "",
                TOKENS.platformOperator(UUID.randomUUID(), "platform.tenant.read", "platform.tenant.delete"), 1, 0, 1L,
                manifests, LocalDate.of(2026, 11, 18));
        assertThatThrownBy(() -> new DemoSeed(unknown, JSON).run()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no manifest");
        DemoSettings unbootstrapped = new DemoSettings(BASE, DemoSettings.Action.SEED, "demo-unknown", "admin",
                ADMIN_PASSWORD, PERSONA_PASSWORD, "", 1, 0, 1L, manifests, LocalDate.of(2026, 11, 18));
        assertThatThrownBy(() -> new DemoSeed(unbootstrapped, JSON).run()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MYSMS_IAM_BOOTSTRAP_TENANT_CODE");
    }

    private Map<String, Integer> countThroughTheApi(Manifest manifest) {
        ApiClient admin = signedIn("admin", ADMIN_PASSWORD);
        String school = Filter.eq("institutionId", manifest.institutionId());
        return Map.of("institutions", admin.list("/v1/org/institutions", null).size(),
                "classSections", admin.list("/v1/academics/class-sections", school).size(),
                "staff", admin.list("/v1/hr/staff", school).size(),
                "students", admin.list("/v1/sis/students", school).size(),
                "enrollments", admin.list("/v1/sis/student-enrollments", null).size(),
                "teachingAssignments", admin.list("/v1/academics/teaching-assignments", null).size(),
                "attendanceSheets", admin.list("/v1/attendance/attendance-sheets", Filter.eq("status", "done")).size());
    }

    private void personasSignInToTheirOwnData() {
        ApiClient student = signedIn("student", PERSONA_PASSWORD);
        JsonNode me = student.get("/v1/iam/me");
        assertThat(me.path("studentIds")).hasSize(1);
        JsonNode summary = student.get("/v1/sis/students/" + me.path("studentIds").get(0).asString() + ":summary");
        assertThat(summary.isObject()).isTrue();

        ApiClient guardian = signedIn("parent", PERSONA_PASSWORD);
        assertThat(guardian.list("/v1/sis/me/children", null)).hasSize(1);

        for (String staff : List.of("schooladmin", "principal", "teacher", "classteacher", "accountant",
                "librarian")) {
            JsonNode staffMe = signedIn(staff, PERSONA_PASSWORD).get("/v1/iam/me");
            assertThat(staffMe.path("staffIds")).as(staff).hasSize(1);
            assertThat(staffMe.path("person").path("displayName").asString()).as(staff).isNotBlank();
        }
        assertThatThrownBy(() -> signedIn("teacher", PERSONA_PASSWORD).get("/v1/iam/roles"))
                .isInstanceOf(ApiException.class).extracting(e -> ((ApiException) e).status()).isEqualTo(403);
    }

    private void remove(Manifest manifest) {
        String operator = TOKENS.platformOperator(UUID.randomUUID(), "platform.tenant.read", "platform.tenant.delete");
        ApiClient platform = ApiClient.anonymous(BASE, JSON).as(Bearer.fixed(operator));
        // the platform registers the bootstrap tenant from iam's announcement, over the broker
        await(() -> exists(platform, manifest.tenantId()));

        new DemoSeed(settings(DemoSettings.Action.REMOVE, operator), JSON).run();

        assertThat(exists(platform, manifest.tenantId())).isFalse();
        assertThat(Files.exists(Manifest.file(manifests, TENANT))).isFalse();
        await(() -> {
            try {
                signedIn("admin", ADMIN_PASSWORD);
                return false;
            } catch (ApiException e) {
                return e.status() == 401;
            }
        });
    }

    private static boolean exists(ApiClient platform, UUID tenant) {
        try {
            platform.get("/v1/platform/tenants/" + tenant);
            return true;
        } catch (ApiException e) {
            if (e.status() == 404) {
                return false;
            }
            throw e;
        }
    }

    private static void await(BooleanSupplier condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (!condition.getAsBoolean()) {
            assertThat(Instant.now()).as("condition within 30 s").isBefore(deadline);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private static ApiClient signedIn(String username, String password) {
        ApiClient anonymous = ApiClient.anonymous(BASE, JSON);
        return anonymous.as(SignIn.of(anonymous, TENANT, username, password));
    }

    private DemoSettings settings(DemoSettings.Action action) {
        return settings(action, "");
    }

    private DemoSettings settings(DemoSettings.Action action, String platformToken) {
        return new DemoSettings(BASE, action, TENANT, "admin", ADMIN_PASSWORD, PERSONA_PASSWORD, platformToken, 1,
                1, 20261118L, manifests, LocalDate.of(2026, 11, 18));
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(String secret) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** RabbitMQ: the broker at MYSMS_TEST_AMQP_URI (its module queues of earlier runs removed), or a container. */
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

    private static void forgetEarlierRuns(URI uri) {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(uri);
            try (Connection connection = factory.newConnection("mysms-demo-seed-test-cleanup")) {
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
}
