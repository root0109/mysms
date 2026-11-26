package com.mysms.identity.people;

import com.mysms.identity.IdentityApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.identity.people.config.PeopleDatabaseConfig;
import com.mysms.identity.people.events.GreetingTemplateSync;
import com.mysms.identity.people.events.MemberLinkSync;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * An institution's birthdays and greetings over HTTP against real PostgreSQL as the application role, with the members
 * of each school kept from sis's and hr's events (people.member_link): what a birthday listing shows, whom a greeting
 * may reach and how often. Each test works in a tenant of its own.
 */
@SpringBootTest(classes = IdentityApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PeopleBirthdayAndGreetingComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(PeopleDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private MemberLinkSync sync;

    @Autowired
    private GreetingTemplateSync templates;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "people", "iam");
        // identity also runs iam, whose token issuer needs a signing key
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * One school of its own tenant: a pupil, two guardians of theirs (one without a date of birth) and a teacher,
     * each known to people from sis's and hr's events.
     */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID pupil = person("Pupil One", today().minusYears(11));
        final UUID guardian = person("Guardian One", today().plusDays(1).minusYears(40));
        final UUID carer = person("Carer One", null);
        final UUID teacher = person("Teacher One", today().minusYears(35));
        final UUID guardianLink = UUID.randomUUID();

        School() {
            deliver("sis.student.created", UUID.randomUUID(), "\"personId\":\"" + pupil + "\",\"institutionId\":\""
                    + institution + "\"", 1);
            deliver("hr.staff.created", UUID.randomUUID(), "\"personId\":\"" + teacher + "\",\"institutionId\":\""
                    + institution + "\",\"joiningDate\":\"" + today().minusYears(3) + "\"", 1);
            deliver("sis.student_guardian.created", guardianLink, link(guardian, "active"), 1);
            deliver("sis.student_guardian.created", UUID.randomUUID(), link(carer, "active"), 1);
        }

        /** A guardian link's fields as sis publishes them (SisEvents.guardianLink). */
        String link(UUID guardianPerson, String status) {
            return "\"studentPersonId\":\"" + pupil + "\",\"guardianPersonId\":\"" + guardianPerson
                    + "\",\"guardianUserId\":null,\"isLegalGuardian\":true,\"hasCustody\":true,\"status\":\"" + status
                    + "\"";
        }

        UUID person(String name, LocalDate born) {
            UUID id = UUID.randomUUID();
            sql("INSERT INTO people.person (id, tenant_id, display_name, date_of_birth, primary_email, row_version)"
                    + " VALUES ('" + id + "', :tenantId, '" + name + "', " + (born == null ? "NULL" : "'" + born + "'")
                    + ", 'someone@example.org', 1)");
            return id;
        }

        boolean deliver(String type, UUID record, String fields, int version) {
            String aggregate = type.substring(0, type.lastIndexOf('.'));
            EventEnvelope event = new EventEnvelope(UUID.randomUUID(), aggregate.substring(0, aggregate.indexOf('.')),
                    type, Optional.of(tenant), aggregate, record, Instant.now(), "{\"id\":\"" + record + "\","
                    + fields + ",\"rowVersion\":" + version + "}", Optional.of(TRACE));
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> sync.apply(event)));
        }

        boolean template(UUID id, UUID institution, boolean active, int version, String verb) {
            EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "comms", "comms.message_template." + verb,
                    Optional.of(tenant), "comms.message_template", id, Instant.now(), "{\"id\":\"" + id
                    + "\",\"institutionId\":\"" + institution + "\",\"isActive\":" + active + ",\"rowVersion\":"
                    + version + "}", Optional.of(TRACE));
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> templates.apply(event)));
        }

        boolean rejected(UUID greeting) {
            EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "comms", "comms.greeting_delivery.rejected",
                    Optional.of(tenant), "comms.greeting_delivery", greeting, Instant.now(), "{\"id\":\"" + greeting
                    + "\",\"reason\":\"TEMPLATE_UNAVAILABLE\"}", Optional.of(TRACE));
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> templates.rejected(event)));
        }

        String as(UUID person, String institutionScope, List<String> permissions) {
            return token(tenant, person, institutionScope, permissions);
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

    private static String token(UUID tenant, UUID person, String institution, List<String> permissions) {
        return token(tenant, UUID.randomUUID(), person, institution, permissions);
    }

    private static String token(UUID tenant, UUID user, UUID person, String institution, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", permissions);
        claims.put("tperms", "*".equals(institution) ? permissions : List.of());
        claims.put("ins", List.of(institution));
        if (person != null) {
            claims.put("pid", person.toString());
        }
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    @Test
    void birthdaysShowTheDayAndNameOfTheSchoolsMembersOnly() throws Exception {
        School school = new School();
        String reader = school.as(null, school.institution.toString(), List.of("people.person.list_birthdays"));
        String window = "?institutionId=" + school.institution + "&fromDate=" + today() + "&toDate=" + today()
                .plusDays(5);

        Reply listed = call("GET", "/v1/people/persons:birthdays" + window, null, reader);
        assertThat(listed.status()).as(listed.text()).isEqualTo(200);
        List<String> seen = new ArrayList<>();
        listed.body().path("data").forEach(entry -> seen.add(entry.path("displayName").asString() + "/"
                + entry.path("kind").asString() + "/" + entry.path("role").asString() + "/" + entry.path("date")
                .asString() + "/" + entry.path("years").asInt(-1)));
        assertThat(seen).containsExactly(
                "Pupil One/birthday/student/" + today() + "/-1",
                "Teacher One/birthday/staff/" + today() + "/-1",
                "Teacher One/work_anniversary/staff/" + today() + "/3",
                "Guardian One/birthday/guardian/" + today().plusDays(1) + "/-1");
        JsonNode first = listed.body().path("data").get(0);
        assertThat(first.has("primaryEmail")).isFalse();
        assertThat(first.size()).as("the name and the day only").isEqualTo(5);

        assertThat(call("GET", "/v1/people/persons:birthdays" + window + "&kinds=work_anniversary", null, reader)
                .body().path("data")).hasSize(1);
        assertThat(call("GET", "/v1/people/persons:birthdays" + window + "&roles=student&roles=guardian", null,
                reader).body().path("data").findValuesAsString("displayName"))
                .containsExactly("Pupil One", "Guardian One");
        Reply paged = call("GET", "/v1/people/persons:birthdays" + window + "&size=3", null, reader);
        assertThat(paged.body().path("data")).hasSize(3);
        String next = paged.body().path("paging").path("next").asString();
        assertThat(call("GET", next, null, reader).body().path("data").findValuesAsString("displayName"))
                .containsExactly("Guardian One");
        assertThat(call("GET", "/v1/people/persons:birthdays?institutionId=" + school.institution + "&fromDate="
                + today() + "&toDate=" + today().plusDays(62), null, reader).status()).isEqualTo(422);
        assertThat(call("GET", "/v1/people/persons:birthdays" + window + "&kinds=funeral", null, reader).status())
                .isEqualTo(422);
        assertThat(call("GET", "/v1/people/persons:birthdays" + window, null, school.as(null,
                UUID.randomUUID().toString(), List.of("people.person.list_birthdays"))).status())
                .as("another school's staff").isEqualTo(404);
        assertThat(call("GET", "/v1/people/persons:birthdays" + window, null, school.as(null,
                school.institution.toString(), List.of("people.person.send_greetings"))).status())
                .as("without the permission").isEqualTo(403);
        assertThat(call("GET", "/v1/people/persons:birthdays" + window, null, token(UUID.randomUUID(), null, "*",
                List.of("people.person.list_birthdays"))).body().path("data")).as("another tenant").isEmpty();
    }

    @Test
    void aGuardianBelongsToTheSchoolOnlyWhileTheLinkToTheChildIsActive() throws Exception {
        School school = new School();
        String reader = school.as(null, school.institution.toString(), List.of("people.person.list_birthdays"));
        String guardians = "/v1/people/persons:birthdays?institutionId=" + school.institution + "&fromDate=" + today()
                + "&toDate=" + today().plusDays(5) + "&roles=guardian";
        assertThat(call("GET", guardians, null, reader).body().path("data").findValuesAsString("displayName"))
                .containsExactly("Guardian One");

        school.deliver("sis.student_guardian.updated", school.guardianLink, school.link(school.guardian, "inactive"),
                2);
        assertThat(call("GET", guardians, null, reader).body().path("data")).as("an inactive link").isEmpty();
        assertThat(school.deliver("sis.student_guardian.updated", school.guardianLink,
                school.link(school.guardian, "active"), 1)).as("a late event changes nothing").isFalse();
        assertThat(call("GET", guardians, null, reader).body().path("data")).isEmpty();
        String sender = school.as(null, school.institution.toString(), List.of("people.person.send_greetings"));
        assertThat(call("POST", "/v1/people/persons:sendGreetings", "{\"personIds\":[\"" + school.guardian
                + "\"],\"templateId\":\"" + UUID.randomUUID() + "\",\"channel\":\"sms\"}", sender).status())
                .as("no longer reached through the child").isEqualTo(422);
        school.deliver("sis.student_guardian.deleted", school.guardianLink, school.link(school.guardian, "active"), 3);
        assertThat(call("GET", guardians, null, reader).body().path("data")).as("a deleted link").isEmpty();
    }

    @Test
    void aGreetingNamesATemplateCommsDeliversAndARejectedOneCostsNothing() throws Exception {
        School school = new School();
        String sender = school.as(null, school.institution.toString(), List.of("people.person.send_greetings"));
        UUID template = UUID.randomUUID();
        UUID elsewhere = UUID.randomUUID();
        String greet = "{\"personIds\":[\"" + school.pupil + "\"],\"templateId\":\"" + template
                + "\",\"channel\":\"sms\"}";
        Reply unknown = call("POST", "/v1/people/persons:sendGreetings", greet, sender);
        assertThat(unknown.status()).as("a template comms never announced").isEqualTo(422);
        assertThat(unknown.body().path("errors").get(0).path("field").asString()).isEqualTo("templateId");

        assertThat(school.template(template, school.institution, true, 1, "created")).isTrue();
        assertThat(school.template(elsewhere, UUID.randomUUID(), true, 1, "created")).isTrue();
        Reply otherSchool = call("POST", "/v1/people/persons:sendGreetings", greet.replace(template.toString(),
                elsewhere.toString()), sender);
        assertThat(otherSchool.status()).as("another school's template").isEqualTo(422);
        assertThat(school.template(template, school.institution, false, 2, "updated")).isTrue();
        assertThat(call("POST", "/v1/people/persons:sendGreetings", greet, sender).status())
                .as("a template comms no longer uses").isEqualTo(422);
        assertThat(school.template(template, school.institution, true, 3, "updated")).isTrue();
        Reply sent = call("POST", "/v1/people/persons:sendGreetings", greet, sender);
        assertThat(sent.status()).as(sent.text()).isEqualTo(200);
        assertThat(sent.body().path("queued").asInt()).isEqualTo(1);

        UUID greeting = UUID.fromString(school.column("SELECT id::text FROM people.greeting WHERE tenant_id ="
                + " :tenantId AND deleted_at IS NULL").get(0));
        assertThat(school.rejected(greeting)).as("comms could not deliver it").isTrue();
        assertThat(school.rejected(greeting)).as("taken back once").isFalse();
        Reply resent = call("POST", "/v1/people/persons:sendGreetings", greet, sender);
        assertThat(resent.body().path("queued").asInt()).as("a rejected greeting is not counted as sent")
                .isEqualTo(1);
    }

    @Test
    void greetingsReachTheSchoolsMembersOnceADayWithinTheSendersAllowance() throws Exception {
        School school = new School();
        String sender = school.as(null, school.institution.toString(), List.of("people.person.send_greetings"));
        String template = UUID.randomUUID().toString();
        school.sql("INSERT INTO people.greeting_template (tenant_id, template_id, is_active, source_version,"
                + " synced_at) VALUES (:tenantId, '" + template + "', true, 1, now())");
        String greet = "{\"personIds\":[\"" + school.pupil + "\",\"" + school.teacher + "\"],\"templateId\":\""
                + template + "\",\"channel\":\"sms\"}";

        Reply sent = call("POST", "/v1/people/persons:sendGreetings", greet, sender);
        assertThat(sent.status()).as(sent.text()).isEqualTo(200);
        assertThat(sent.body().path("queued").asInt()).isEqualTo(2);
        Reply again = call("POST", "/v1/people/persons:sendGreetings", greet, sender);
        assertThat(again.body().path("skipped").asInt()).as("once a day per channel").isEqualTo(2);
        assertThat(school.column("SELECT payload FROM people.outbox_event WHERE tenant_id = :tenantId AND"
                + " event_type = 'people.greeting.requested'")).hasSize(2).allSatisfy(payload -> assertThat(payload)
                .contains(template).doesNotContain("Pupil One").doesNotContain("example.org"));

        UUID stranger = school.person("Stranger", today());
        Reply outside = call("POST", "/v1/people/persons:sendGreetings", "{\"personIds\":[\"" + stranger + "\"],"
                + "\"templateId\":\"" + template + "\",\"channel\":\"sms\"}", sender);
        assertThat(outside.status()).isEqualTo(422);
        assertThat(outside.body().path("errors").get(0).path("field").asString()).isEqualTo("personIds[0]");
        assertThat(call("POST", "/v1/people/persons:sendGreetings", greet, school.as(null,
                UUID.randomUUID().toString(), List.of("people.person.send_greetings"))).status()).isEqualTo(422);
        String tenantWide = school.as(null, "*", List.of("people.person.send_greetings"));
        assertThat(call("POST", "/v1/people/persons:sendGreetings", "{\"personIds\":[\"" + stranger + "\"],"
                + "\"templateId\":\"" + template + "\",\"channel\":\"email\"}", tenantWide).status())
                .as("a tenant-wide grant reaches every person").isEqualTo(200);
        Reply unknown = call("POST", "/v1/people/persons:sendGreetings", "{\"personIds\":[\"" + UUID.randomUUID()
                + "\"],\"templateId\":\"" + template + "\",\"channel\":\"sms\"}", tenantWide);
        assertThat(unknown.status()).as("but not an unknown one").isEqualTo(422);
        assertThat(unknown.body().path("errors").get(0).path("field").asString()).isEqualTo("personIds[0]");

        UUID busyUser = UUID.randomUUID();
        String busy = token(school.tenant, busyUser, null, school.institution.toString(),
                List.of("people.person.send_greetings"));
        school.sql("INSERT INTO people.person (id, tenant_id, display_name, row_version) SELECT gen_random_uuid(),"
                + " :tenantId, 'Bulk ' || n, 1 FROM generate_series(1, 300) n");
        school.sql("INSERT INTO people.greeting (id, tenant_id, person_id, greeted_on, channel, created_by,"
                + " row_version) SELECT gen_random_uuid(), :tenantId, p.id, '" + today() + "', 'email', '" + busyUser
                + "', 1 FROM people.person p WHERE p.tenant_id = :tenantId AND p.display_name LIKE 'Bulk %'");
        Reply limited = call("POST", "/v1/people/persons:sendGreetings", greet.replace("sms", "push"), busy);
        assertThat(limited.status()).as(limited.text()).isEqualTo(429);
        assertThat(limited.body().path("code").asString()).isEqualTo("RATE_LIMITED");
    }
}
