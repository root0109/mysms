package com.mysms.sis;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.sis.config.SisDatabaseConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * sis.ListClassRoster over HTTP against real PostgreSQL as the application role: the roster is the enrolled
 * enrollments of one class section, for the holder of the roster permission, inside the caller's institutions and
 * tenant, paged with a cursor that belongs to that roster.
 */
@SpringBootTest(classes = SisApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SisClassRosterComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String ROSTER = "sis.student_enrollment.list_class_roster";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(SisDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "sis");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    private Reply roster(UUID classSection, String token, String query) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port
                + "/v1/sis/class-sections/" + classSection + ":roster" + query)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private static String token(UUID tenant, List<String> institutions, List<String> tenantWide,
            String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", UUID.randomUUID().toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", tenantWide);
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    /** A student of the institution placed in the class with the status; returns the student id. */
    private UUID placed(UUID tenant, UUID institution, UUID classSection, String status) {
        UUID student = UUID.randomUUID();
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(() -> {
                    database.jdbc().sql("INSERT INTO sis.student (id, tenant_id, person_id, institution_id, "
                            + "row_version) VALUES (:id, :tenantId, :person, :institution, 1)")
                            .param("id", student).param("person", UUID.randomUUID())
                            .param("institution", institution).update();
                    database.jdbc().sql("INSERT INTO sis.student_enrollment (tenant_id, student_id, "
                            + "academic_year_id, class_section_id, enrollment_status, row_version) "
                            + "VALUES (:tenantId, :student, :year, :classSection, :status, 1)")
                            .param("student", student).param("year", UUID.randomUUID())
                            .param("classSection", classSection).param("status", status).update();
                    return null;
                }));
        return student;
    }

    private static List<String> students(Reply reply) {
        List<String> students = new ArrayList<>();
        reply.body().path("data").forEach(item -> students.add(item.path("studentId").asString()));
        return students;
    }

    @Test
    void theRosterIsTheEnrolledStudentsOfTheClassInsideTheCallersScope() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID classSection = UUID.randomUUID();
        UUID first = placed(tenant, institution, classSection, "enrolled");
        UUID second = placed(tenant, institution, classSection, "enrolled");
        placed(tenant, institution, classSection, "left");
        placed(tenant, institution, classSection, "promoted");
        placed(tenant, institution, UUID.randomUUID(), "enrolled");
        String teacher = token(tenant, List.of(institution.toString()), List.of(), ROSTER);

        Reply roster = roster(classSection, teacher, "");

        assertThat(roster.status()).as(roster.text()).isEqualTo(200);
        assertThat(students(roster)).containsExactlyInAnyOrder(first.toString(), second.toString());
        assertThat(roster.body().path("paging").path("size").asInt()).isEqualTo(2);
        assertThat(roster.body().path("data").get(0).path("classSectionId").asString())
                .isEqualTo(classSection.toString());

        assertThat(roster(classSection, token(tenant, List.of("*"), List.of("*"), "sis.student_enrollment.read"),
                "").status()).as("the roster permission is its own").isEqualTo(403);
        assertThat(roster(classSection, null, "").status()).isEqualTo(401);
        assertThat(students(roster(classSection,
                token(tenant, List.of(UUID.randomUUID().toString()), List.of(), ROSTER), "")))
                .as("a class of another institution").isEmpty();
        assertThat(students(roster(classSection, token(UUID.randomUUID(), List.of("*"), List.of("*"), ROSTER), "")))
                .as("another tenant").isEmpty();
        assertThat(students(roster(UUID.randomUUID(), teacher, ""))).as("a class without students").isEmpty();
    }

    @Test
    void theRosterIsPagedAndACursorBelongsToItsClass() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID classSection = UUID.randomUUID();
        UUID otherClass = UUID.randomUUID();
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            expected.add(placed(tenant, institution, classSection, "enrolled").toString());
        }
        placed(tenant, institution, otherClass, "enrolled");
        String teacher = token(tenant, List.of("*"), List.of("*"), ROSTER);

        List<String> seen = new ArrayList<>();
        Reply page = roster(classSection, teacher, "?size=2");
        assertThat(page.body().path("data")).hasSize(2);
        seen.addAll(students(page));
        String next = page.body().path("paging").path("next").asString();
        assertThat(next).contains("page=2").contains("cursor=");
        String cursor = next.substring(next.indexOf("cursor=") + "cursor=".length());
        Reply last = roster(classSection, teacher, next.substring(next.indexOf('?')));
        assertThat(last.status()).as(last.text()).isEqualTo(200);
        seen.addAll(students(last));
        assertThat(last.body().path("paging").has("next")).isFalse();
        assertThat(seen).containsExactlyInAnyOrderElementsOf(expected);

        assertThat(roster(otherClass, teacher, "?page=2&size=2&cursor=" + cursor).status())
                .as("a cursor of another class").isEqualTo(400);
        assertThat(roster(classSection, teacher, "?size=501").status()).isEqualTo(400);
    }
}
