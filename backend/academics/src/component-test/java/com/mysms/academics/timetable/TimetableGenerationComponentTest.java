package com.mysms.academics.timetable;

import com.mysms.academics.AcademicsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.academics.timetable.config.TimetableDatabaseConfig;
import com.mysms.academics.timetable.events.InstitutionZoneSync;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * timetable.GenerateSessions and timetable.BatchConfirmSessions over HTTP against real PostgreSQL as the application
 * role: meetings at the period's local time, idempotent re-runs, clashes reported and not written, the operation
 * recorded, and the caller's institutions and permissions. Each test works in a tenant of its own.
 */
@SpringBootTest(classes = AcademicsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimetableGenerationComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String GENERATE = "timetable.session.generate_sessions";
    private static final String CONFIRM = "timetable.session.batch_confirm_sessions";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(TimetableDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private InstitutionZoneSync zones;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "timetable", "academics", "attendance", "assessment");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {

        JsonNode outcome() {
            return body.path("result");
        }
    }

    /** One school of its own tenant with a 45-minute first period at 09:00, a teacher, a class and a room. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID period = UUID.randomUUID();
        final UUID teacher = UUID.randomUUID();
        final UUID classSection = UUID.randomUUID();
        final UUID room = UUID.randomUUID();
        final String admin = token(tenant, institution.toString(), GENERATE, CONFIRM, "timetable.session.read",
                "timetable.operation.read");

        School() {
            sql("INSERT INTO timetable.period (id, tenant_id, institution_id, name, start_time, duration_minutes,"
                    + " row_version) VALUES ('" + period + "', :tenantId, '" + institution + "', 'First', '09:00', 45,"
                    + " 1)");
        }

        /** Mondays and Wednesdays of two weeks (4 meetings) of the school's class with its teacher in its room. */
        String pattern(UUID ofClass, UUID staff, UUID inRoom) {
            return "{\"institutionId\":\"" + institution + "\",\"classSectionId\":\"" + ofClass
                    + "\",\"startDate\":\"2026-03-02\",\"endDate\":\"2026-03-15\",\"timeZone\":\"Asia/Kolkata\","
                    + "\"slots\":[" + slot(1, staff, inRoom) + "," + slot(3, staff, inRoom) + "]}";
        }

        String slot(int weekday, UUID staff, UUID inRoom) {
            return "{\"weekday\":" + weekday + ",\"periodId\":\"" + period + "\",\"subjectId\":\"" + classSection
                    + "\",\"staffId\":\"" + staff + "\"" + (inRoom == null ? "" : ",\"roomId\":\"" + inRoom + "\"")
                    + "}";
        }

        List<String> column(String query) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(query).query(String.class)
                            .list()));
        }

        void sql(String statement) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> database.jdbc().sql(statement).update()));
        }

        /** org's institution event (OrgEvents.institution) giving the school a time zone of its own. */
        boolean zone(String timeZone, int version) {
            String data = "{\"id\":\"" + institution + "\"" + (timeZone == null ? "" : ",\"timezone\":\"" + timeZone
                    + "\"") + ",\"rowVersion\":" + version + "}";
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> zones.apply(new EventEnvelope(UUID.randomUUID(),
                            "org", "org.institution.updated", Optional.of(tenant), "org.institution", institution,
                            Instant.now(), data, Optional.empty()))));
        }
    }

    private Reply call(String method, String path, String body, String token) throws Exception {
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token).build(),
                HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private Reply generate(String body, String token) throws Exception {
        return call("POST", "/v1/timetable/sessions:generate", body, token);
    }

    private Reply confirm(List<String> ids, String token) throws Exception {
        List<String> quoted = new ArrayList<>();
        ids.forEach(id -> quoted.add("\"" + id + "\""));
        return call("POST", "/v1/timetable/sessions:batchConfirm", "{\"sessionIds\":[" + String.join(",", quoted)
                + "]}", token);
    }

    private static String token(UUID tenant, String institution, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", "*".equals(institution) ? List.of(permissions) : List.of());
        claims.put("ins", List.of(institution));
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    @Test
    void aWeeklyPatternBecomesDraftSessionsAtTheLocalPeriodTimeOnce() throws Exception {
        School school = new School();
        Reply run = generate(school.pattern(school.classSection, school.teacher, school.room), school.admin);
        assertThat(run.status()).as(run.text()).isEqualTo(202);
        assertThat(run.body().path("status").asString()).isEqualTo("succeeded");
        assertThat(run.body().path("kind").asString()).isEqualTo("timetable.GenerateSessions");
        assertThat(run.outcome().path("created").asInt()).isEqualTo(4);
        assertThat(run.outcome().path("clashCount").asInt()).isZero();
        assertThat(school.column("SELECT to_char(start_at AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI') || '/' || status"
                + " || '/' || to_char(end_at - start_at, 'MI') FROM timetable.session WHERE tenant_id = :tenantId"
                + " ORDER BY start_at")).as("09:00 in Kolkata is 03:30 UTC; 45 minutes; drafts")
                .containsExactly("2026-03-02 03:30/draft/45", "2026-03-04 03:30/draft/45", "2026-03-09 03:30/draft/45",
                        "2026-03-11 03:30/draft/45");

        Reply again = generate(school.pattern(school.classSection, school.teacher, school.room), school.admin);
        assertThat(again.outcome().path("created").asInt()).as("a re-run writes nothing").isZero();
        assertThat(again.outcome().path("kept").asInt()).isEqualTo(4);
        assertThat(school.column("SELECT count(*)::text FROM timetable.session WHERE tenant_id = :tenantId"))
                .containsExactly("4");
        Reply read = call("GET", "/v1/timetable/operations/" + run.body().path("operationId").asString(), null,
                school.admin);
        assertThat(read.status()).isEqualTo(200);
        assertThat(read.outcome().path("created").asInt()).as("the run is kept with its outcome").isEqualTo(4);
    }

    @Test
    void theSchoolsOwnTimeZoneAppliesAndARunInAnotherZoneMovesItsDraftsNeverDuplicates() throws Exception {
        School school = new School();
        String inUtc = school.pattern(school.classSection, school.teacher, school.room).replace("Asia/Kolkata", "UTC");
        Reply wrong = generate(inUtc, school.admin);
        assertThat(wrong.status()).as("no zone of its own yet: the request's applies; " + wrong.text()).isEqualTo(202);
        assertThat(wrong.outcome().path("created").asInt()).isEqualTo(4);

        assertThat(school.zone("Asia/Kolkata", 2)).isTrue();
        assertThat(school.zone(null, 1)).as("a late, older event changes nothing").isFalse();
        Reply other = generate(inUtc, school.admin);
        assertThat(other.status()).as("another zone than the school's").isEqualTo(422);
        assertThat(other.body().path("errors").get(0).path("field").asString()).isEqualTo("timeZone");
        String ownZone = inUtc.replace("\"timeZone\":\"UTC\",", "");
        Reply moved = generate(ownZone, school.admin);
        assertThat(moved.status()).as(moved.text()).isEqualTo(202);
        assertThat(moved.outcome().path("created").asInt()).as("the same meetings, not new ones").isZero();
        assertThat(moved.outcome().path("replaced").asInt()).isEqualTo(4);
        assertThat(moved.outcome().path("clashCount").asInt()).isZero();
        assertThat(school.column("SELECT to_char(start_at AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:MI') || '/' || status"
                + " FROM timetable.session WHERE tenant_id = :tenantId AND deleted_at IS NULL ORDER BY start_at"))
                .as("09:00 in the school's zone").containsExactly("2026-03-02 03:30/draft", "2026-03-04 03:30/draft",
                        "2026-03-09 03:30/draft", "2026-03-11 03:30/draft");

        Reply named = generate(inUtc.replace("UTC", "Asia/Kolkata"), school.admin);
        assertThat(named.outcome().path("kept").asInt()).as("naming the school's own zone is fine").isEqualTo(4);
        school.sql("UPDATE timetable.session SET status = 'confirmed', start_at = start_at + interval '1 hour',"
                + " end_at = end_at + interval '1 hour' WHERE start_at = '2026-03-02 03:30+00'");
        Reply confirmedMoved = generate(ownZone, school.admin);
        assertThat(confirmedMoved.outcome().path("kept").asInt()).isEqualTo(3);
        assertThat(confirmedMoved.outcome().path("created").asInt()).as("a confirmed session is not duplicated")
                .isZero();
        assertThat(confirmedMoved.outcome().path("clashCount").asInt()).as("nor moved: reported").isEqualTo(1);
        assertThat(confirmedMoved.outcome().path("clashes").get(0).path("on").asString()).isEqualTo("class");
        assertThat(school.column("SELECT count(*)::text FROM timetable.session WHERE tenant_id = :tenantId"))
                .containsExactly("4");
    }

    @Test
    void meetingsThatClashWithTheTeacherRoomOrClassAreReportedNotWritten() throws Exception {
        School school = new School();
        assertThat(generate(school.pattern(school.classSection, school.teacher, school.room), school.admin).status())
                .isEqualTo(202);

        Reply sameTeacher = generate(school.pattern(UUID.randomUUID(), school.teacher, null), school.admin);
        assertThat(sameTeacher.outcome().path("created").asInt()).isZero();
        assertThat(sameTeacher.outcome().path("clashCount").asInt()).isEqualTo(4);
        JsonNode clash = sameTeacher.outcome().path("clashes").get(0);
        assertThat(clash.path("on").asString()).isEqualTo("staff");
        assertThat(clash.path("date").asString()).isEqualTo("2026-03-02");
        assertThat(clash.path("periodId").asString()).isEqualTo(school.period.toString());
        assertThat(generate(school.pattern(UUID.randomUUID(), UUID.randomUUID(), school.room), school.admin).outcome()
                .path("clashes").get(0).path("on").asString()).isEqualTo("room");
        assertThat(generate(school.pattern(school.classSection, UUID.randomUUID(), null), school.admin).outcome()
                .path("clashes").get(0).path("on").asString()).isEqualTo("class");
        school.sql("UPDATE timetable.session SET status = 'cancelled' WHERE tenant_id = :tenantId");
        assertThat(generate(school.pattern(UUID.randomUUID(), school.teacher, null), school.admin).outcome()
                .path("created").asInt()).as("cancelled sessions free the time").isEqualTo(4);
    }

    @Test
    void thePatternIsCheckedBeforeAnyRunStarts() throws Exception {
        School school = new School();
        String good = school.pattern(school.classSection, school.teacher, null);

        Reply noZone = generate(good.replace("\"timeZone\":\"Asia/Kolkata\",", ""), school.admin);
        assertThat(noZone.status()).as("a school without a zone of its own needs one named").isEqualTo(422);
        assertThat(noZone.body().path("errors").get(0).path("field").asString()).isEqualTo("timeZone");
        Reply zone = generate(good.replace("Asia/Kolkata", "Mars/Olympus"), school.admin);
        assertThat(zone.status()).as(zone.text()).isEqualTo(422);
        assertThat(zone.body().path("errors").get(0).path("field").asString()).isEqualTo("timeZone");
        assertThat(generate(good.replace("2026-03-15", "2026-03-01"), school.admin).status()).isEqualTo(422);
        assertThat(generate(good.replace("2026-03-15", "2027-03-15"), school.admin).status()).as("a year at most")
                .isEqualTo(422);
        Reply noClass = generate(good.replace("\"classSectionId\":\"" + school.classSection + "\",", ""),
                school.admin);
        assertThat(noClass.body().path("errors").get(0).path("field").asString()).isEqualTo("classSectionId");
        Reply unknownPeriod = generate(good.replace(school.period.toString(), UUID.randomUUID().toString()),
                school.admin);
        assertThat(unknownPeriod.status()).isEqualTo(422);
        assertThat(unknownPeriod.body().path("errors").get(0).path("field").asString()).isEqualTo("slots[0].periodId");
        assertThat(generate(good.replace("\"weekday\":1", "\"weekday\":8"), school.admin).status()).isEqualTo(422);
        school.sql("UPDATE timetable.period SET duration_minutes = NULL WHERE id = '" + school.period + "'");
        assertThat(generate(good, school.admin).status()).as("a period without a length").isEqualTo(422);
        assertThat(school.column("SELECT count(*)::text FROM timetable.operation WHERE tenant_id = :tenantId"))
                .as("no run was recorded").containsExactly("0");
    }

    @Test
    void generatingNeedsItsPermissionForTheInstitution() throws Exception {
        School school = new School();
        String good = school.pattern(school.classSection, school.teacher, null);

        assertThat(generate(good, token(school.tenant, school.institution.toString(), "timetable.session.create"))
                .status()).isEqualTo(403);
        assertThat(generate(good, token(school.tenant, UUID.randomUUID().toString(), GENERATE)).status())
                .as("another school's administrator").isEqualTo(403);
        assertThat(generate(good, token(UUID.randomUUID(), "*", GENERATE)).status())
                .as("another tenant's periods do not exist").isEqualTo(422);
    }

    @Test
    void manySessionsAreConfirmedAtOnceAllOrNothing() throws Exception {
        School school = new School();
        generate(school.pattern(school.classSection, school.teacher, null), school.admin);
        List<String> ids = school.column("SELECT id::text FROM timetable.session WHERE tenant_id = :tenantId"
                + " ORDER BY start_at");

        Reply confirmed = confirm(ids.subList(0, 2), school.admin);
        assertThat(confirmed.status()).as(confirmed.text()).isEqualTo(200);
        assertThat(confirmed.body().path("items").findValuesAsString("id")).isEqualTo(ids.subList(0, 2));
        assertThat(confirmed.body().path("items").findValuesAsString("status")).containsOnly("confirmed");
        assertThat(confirm(ids.subList(0, 3), school.admin).body().path("items").findValuesAsString("status"))
                .as("a confirmed session stays as it is").containsOnly("confirmed");

        school.sql("UPDATE timetable.session SET status = 'cancelled' WHERE id = '" + ids.get(3) + "'");
        assertThat(confirm(List.of(ids.get(3)), school.admin).status()).isEqualTo(409);
        Reply unknown = confirm(List.of(ids.get(0), UUID.randomUUID().toString()), school.admin);
        assertThat(unknown.status()).isEqualTo(422);
        assertThat(unknown.body().path("errors").get(0).path("field").asString()).isEqualTo("sessionIds[1]");
        assertThat(confirm(List.of(ids.get(0), ids.get(0)), school.admin).status()).isEqualTo(422);
        assertThat(confirm(List.of(), school.admin).status()).isEqualTo(422);
        assertThat(confirm(List.of(ids.get(0)), token(school.tenant, UUID.randomUUID().toString(), CONFIRM))
                .status()).as("another school's session").isEqualTo(422);
        assertThat(confirm(List.of(ids.get(0)), token(school.tenant, school.institution.toString(),
                "timetable.session.confirm_session")).status()).isEqualTo(403);
        assertThat(school.column("SELECT count(*)::text FROM timetable.outbox_event WHERE tenant_id = :tenantId"
                + " AND event_type = 'timetable.session.confirm'")).containsExactly("3");
    }
}
