package com.mysms.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.OperationTable;
import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
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
import java.util.HashMap;
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
 * migrated by its own module, all 635 tables present, all 2049 operations routed at the edge — and a real
 * cross-module flow: sign-in at iam, GetMe reading the person from people over HTTP with the caller's token, the
 * profile update at people, the caller's settings.
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
        // attendance reads sessions, rosters and time zones from the modules that own them, over HTTP
        registry.add("mysms.attendance.timetable-base-url", () -> "http://localhost:" + PORT);
        registry.add("mysms.attendance.sis-base-url", () -> "http://localhost:" + PORT);
        registry.add("mysms.attendance.org-base-url", () -> "http://localhost:" + PORT);
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

    @Test
    void everyModuleOwnsItsDatabaseAndSchema() {
        assertThat(databases).hasSize(21);
        ModuleDatabase any = databases.get("masterDatabase");
        Integer tables = any.jdbc().sql(
                "SELECT count(*) FROM information_schema.tables WHERE table_name <> 'flyway_schema_history'"
                        + " AND table_schema = ANY(:schemas)")
                .param("schemas", MODULES).query(Integer.class).single();
        assertThat(tables).isEqualTo(636);
        Integer histories = any.jdbc().sql(
                // the catalog, not information_schema: the application role has no privilege on the Flyway history
                "SELECT count(*) FROM pg_catalog.pg_class WHERE relname = 'flyway_schema_history' AND relkind = 'r'")
                .query(Integer.class).single();
        assertThat(histories).isEqualTo(21);
        assertThat(operations.size()).isEqualTo(2049);
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
        assertThat(settings.path("items")).hasSize(10);
        assertThat(call("PUT", "/v1/iam/me/settings/ui.theme", token, "{\"value\":\"dark\"}", 200).path("sourceScope")
                .asString()).isEqualTo("user");
        assertThat(call("GET", "/oauth2/jwks", null, null, 200).path("keys")).hasSize(1);
        // the administrator's token from iam carries its tenant-wide permissions (tperms), so org's data scope lets
        // it create an institution; the module's standard operations are served in the assembled application
        JsonNode school = call("POST", "/v1/org/institutions", token, "{\"code\":\"MAIN\",\"name\":\"Main school\"}",
                201);
        JsonNode listed = call("GET", "/v1/org/institutions", token, null, 200);
        assertThat(listed.path("data").get(0).path("id").asString()).isEqualTo(school.path("id").asString());
        String id = school.path("id").asString();
        call("POST", "/v1/people/persons:sendGreetings", token, "{\"personIds\":[\"" + id + "\"],\"templateId\":\""
                + id + "\",\"channel\":\"sms\"}", 501);
    }

    /**
     * Attendance for a timetable session crosses four modules with the caller's own token: the session and its
     * students from timetable, the class roster from sis, the institution's time zone from org — and the sheet, its
     * marks and the summary in attendance.
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
}
