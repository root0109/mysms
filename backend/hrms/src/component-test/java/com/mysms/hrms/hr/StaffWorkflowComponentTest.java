package com.mysms.hrms.hr;

import com.mysms.hrms.HrmsApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.repository.StaffRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * The staff workflow over HTTP against real PostgreSQL: hr.ChangeStaffStatus along its allowed moves, and what
 * hr.CreateStaffPortalAccount decides by itself. hr runs without iam here, so a request that passes hr's own checks
 * answers 503 and changes nothing; iam's answers are covered by StaffAccountLookupTest. Each test works in a tenant of
 * its own.
 */
@SpringBootTest(classes = HrmsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffWorkflowComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String CHANGE_STATUS = "hr.staff.change_staff_status";
    private static final String CREATE_ACCOUNT = "hr.staff.create_staff_portal_account";

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier(HrDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private StaffRepository staffRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "hr", "payroll");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private record Reply(int status, JsonNode body, String text) {
    }

    /** One school of one tenant with an HR officer and one staff member, who has no login account. */
    private final class School {
        final UUID tenant = UUID.randomUUID();
        final UUID institution = UUID.randomUUID();
        final UUID person = UUID.randomUUID();
        final UUID staff = UUID.randomUUID();
        final UUID officerUser = UUID.randomUUID();
        final String officer = token(officerUser, tenant, List.of(institution.toString()), "hr.staff.read",
                CHANGE_STATUS, CREATE_ACCOUNT);

        School() {
            sql("INSERT INTO hr.staff (id, tenant_id, institution_id, person_id, employee_code, row_version) VALUES ('"
                    + staff + "', :tenantId, '" + institution + "', '" + person + "', 'E-17', 1)");
        }

        void sql(String... statements) {
            CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).write(() -> {
                        for (String statement : statements) {
                            database.jdbc().sql(statement).update();
                        }
                        return null;
                    }));
        }

        <T> T read(TestTransactions.Work<T> work) {
            return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                    () -> TestTransactions.of(database).read(work));
        }

        Reply changeStatus(String token, String body) throws Exception {
            return call("POST", "/v1/hr/staff/" + staff + ":changeStatus", token, body);
        }

        Reply createAccount(String token, String body) throws Exception {
            return call("POST", "/v1/hr/staff/" + staff + ":createPortalAccount", token, body);
        }

        Reply get() throws Exception {
            return call("GET", "/v1/hr/staff/" + staff, officer, null);
        }

        /** The payloads of the staff member's events of one type, oldest first. */
        List<String> events(String type) {
            return read(() -> database.jdbc().sql("SELECT payload FROM hr.outbox_event WHERE tenant_id = :tenantId "
                    + "AND aggregate_id = :staff AND event_type = :type ORDER BY id")
                    .param("staff", staff).param("type", type).query(String.class).list());
        }
    }

    private Reply call(String method, String path, String token, String body, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Authorization", "Bearer " + token);
        if (headers.length > 0) {
            request.headers(headers);
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()), response.body());
    }

    private static String token(UUID user, UUID tenant, List<String> institutions, String... permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of(permissions));
        claims.put("tperms", institutions.contains("*") ? List.of("*") : List.of());
        claims.put("ins", institutions);
        return TOKENS.custom(claims, Instant.now().plusSeconds(900));
    }

    private static String status(String status) {
        return "{\"status\":\"" + status + "\"}";
    }

    @Test
    void aStaffMemberIsDeactivatedTakenBackExitedAndRejoins() throws Exception {
        School school = new School();

        Reply inactive = school.changeStatus(school.officer, status("inactive"));
        assertThat(inactive.status()).as(inactive.text()).isEqualTo(200);
        assertThat(inactive.body().path("status").asString()).isEqualTo("inactive");
        assertThat(inactive.body().path("etag").asString()).isEqualTo("\"2\"");
        assertThat(inactive.body().path("updatedBy").asString()).isEqualTo(school.officerUser.toString());

        assertThat(school.changeStatus(school.officer, status("active")).body().path("status").asString())
                .isEqualTo("active");
        assertThat(school.changeStatus(school.officer, status("inactive")).status()).isEqualTo(200);
        assertThat(school.changeStatus(school.officer, status("exited")).body().path("status").asString())
                .as("leaves while suspended").isEqualTo("exited");
        Reply rejoined = school.changeStatus(school.officer, status("active"));
        assertThat(rejoined.status()).as("one who left comes back on the same record: " + rejoined.text())
                .isEqualTo(200);
        Reply exited = school.changeStatus(school.officer, status("exited"));
        assertThat(exited.body().path("status").asString()).as("leaves while active").isEqualTo("exited");
        assertThat(exited.body().path("etag").asString()).isEqualTo("\"7\"");

        List<String> events = school.events("hr.staff.updated");
        assertThat(events).as("every change is announced with the member payload iam keeps its records from")
                .hasSize(6);
        JsonNode last = JSON.readTree(events.get(5));
        assertThat(last.path("id").asString()).isEqualTo(school.staff.toString());
        assertThat(last.path("personId").asString()).isEqualTo(school.person.toString());
        assertThat(last.path("institutionId").asString()).isEqualTo(school.institution.toString());
        assertThat(last.path("rowVersion").asInt()).isEqualTo(7);
        assertThat(school.read(() -> database.jdbc().sql("SELECT count(*) FROM hr.outbox_event WHERE tenant_id = "
                + ":tenantId AND event_type = 'audit.recorded' AND payload LIKE '%changeStaffStatus%'")
                .query(Integer.class).single())).as("and audited").isEqualTo(6);
    }

    @Test
    void aMoveTheStatusDoesNotAllowChangesNothing() throws Exception {
        School school = new School();

        Reply same = school.changeStatus(school.officer, status("active"));
        assertThat(same.status()).as("already active: " + same.text()).isEqualTo(409);
        assertThat(same.body().path("code").asString()).isEqualTo("INVALID_STATE_TRANSITION");

        assertThat(school.changeStatus(school.officer, status("exited")).status()).isEqualTo(200);
        Reply suspended = school.changeStatus(school.officer, status("inactive"));
        assertThat(suspended.status()).as("one who left is not suspended: " + suspended.text()).isEqualTo(409);
        assertThat(school.changeStatus(school.officer, status("exited")).status()).isEqualTo(409);

        assertThat(school.changeStatus(school.officer, "{}").status()).as("the status is required").isEqualTo(422);
        assertThat(school.changeStatus(school.officer, status("retired")).status()).as("not a status")
                .isEqualTo(400);
        Reply stale = school.changeStatus(school.officer, "{\"status\":\"active\",\"etag\":\"\\\"1\\\"\"}");
        assertThat(stale.status()).as(stale.text()).isEqualTo(412);

        assertThat(school.get().body().path("etag").asString()).as("refused calls change nothing")
                .isEqualTo("\"2\"");
        assertThat(school.events("hr.staff.updated")).hasSize(1);
        Reply current = school.changeStatus(school.officer, "{\"status\":\"active\",\"etag\":\"\\\"2\\\"\"}");
        assertThat(current.status()).as("the etag of the last read: " + current.text()).isEqualTo(200);
    }

    @Test
    void theStatusChangesOnlyForACallerWithThePermissionInTheStaffMembersInstitution() throws Exception {
        School school = new School();
        String reader = token(UUID.randomUUID(), school.tenant, List.of(school.institution.toString()),
                "hr.staff.read", "hr.staff.update");
        assertThat(school.changeStatus(reader, status("inactive")).status()).isEqualTo(403);

        String elsewhere = token(UUID.randomUUID(), school.tenant, List.of(UUID.randomUUID().toString()),
                CHANGE_STATUS, CREATE_ACCOUNT);
        assertThat(school.changeStatus(elsewhere, status("inactive")).status()).as("another institution")
                .isEqualTo(404);
        assertThat(school.createAccount(elsewhere, "{}").status()).isEqualTo(404);

        String otherTenant = token(UUID.randomUUID(), UUID.randomUUID(), List.of("*"), CHANGE_STATUS, CREATE_ACCOUNT);
        assertThat(school.changeStatus(otherTenant, status("inactive")).status()).as("another tenant")
                .isEqualTo(404);
        assertThat(school.createAccount(otherTenant, "{}").status()).isEqualTo(404);
        assertThat(school.createAccount(reader, "{}").status()).isEqualTo(403);

        assertThat(school.get().body().path("status").asString()).isEqualTo("active");
        assertThat(school.get().body().path("etag").asString()).isEqualTo("\"1\"");
    }

    @Test
    void aPortalAccountIsRefusedBeforeIamIsAskedAndWithoutIamTheAnswerIsUnavailable() throws Exception {
        School school = new School();

        Reply invite = school.createAccount(school.officer, "{\"sendInvite\":true}");
        assertThat(invite.status()).as("no invitation can be sent yet: " + invite.text()).isEqualTo(422);
        assertThat(invite.body().path("errors").path(0).path("field").asString()).isEqualTo("sendInvite");

        Reply unavailable = school.createAccount(school.officer, "{\"sendInvite\":false}");
        assertThat(unavailable.status()).as("hr alone has no iam to ask: " + unavailable.text()).isEqualTo(503);
        assertThat(school.get().body().has("userId")).as("nothing is stored").isFalse();
        assertThat(school.get().body().path("etag").asString()).isEqualTo("\"1\"");

        school.sql("UPDATE hr.staff SET employee_code = NULL WHERE id = '" + school.staff + "'");
        Reply nameless = school.createAccount(school.officer, "{}");
        assertThat(nameless.status()).as("the employee number is the login name: " + nameless.text()).isEqualTo(422);
        assertThat(nameless.body().path("errors").path(0).path("field").asString()).isEqualTo("employeeCode");

        school.sql("UPDATE hr.staff SET employee_code = 'E-17', status = 'exited' WHERE id = '" + school.staff + "'");
        Reply left = school.createAccount(school.officer, "{}");
        assertThat(left.status()).as("one who left gets no account: " + left.text()).isEqualTo(409);

        school.sql("UPDATE hr.staff SET status = 'active', user_id = '" + UUID.randomUUID() + "' WHERE id = '"
                + school.staff + "'");
        Reply already = school.createAccount(school.officer, "{}");
        assertThat(already.status()).as(already.text()).isEqualTo(409);
        assertThat(already.body().path("detail").asString()).contains("already has a login account");
        assertThat(school.events("hr.staff.updated")).isEmpty();
    }

    @Test
    void anAccountIsStoredOnlyOnTheRowAsReadOfAStaffMemberOfTheTenantWhoHasNoneAndHasNotLeft() {
        School school = new School();
        School other = new School();
        UUID account = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        assertThat(attach(other, school.staff, 1, account, now)).as("another tenant's staff member").isZero();
        // iam is asked between the read and the write: a change made meanwhile is never overwritten
        school.sql("UPDATE hr.staff SET status = 'exited', row_version = 2 WHERE id = '" + school.staff + "'");
        assertThat(attach(school, school.staff, 2, account, now)).as("one who left meanwhile").isZero();
        school.sql("UPDATE hr.staff SET status = 'inactive', row_version = 3 WHERE id = '" + school.staff + "'");
        assertThat(attach(school, school.staff, 2, account, now)).as("a row changed since it was read").isZero();
        assertThat(attach(school, school.staff, 3, account, now)).isEqualTo(1);
        assertThat(school.read(() -> staffRepository.findById(school.staff)).orElseThrow())
                .satisfies(stored -> {
                    assertThat(stored.getUserId()).isEqualTo(account);
                    assertThat(stored.getRowVersion()).isEqualTo(4);
                    assertThat(stored.getUpdatedBy()).isEqualTo(school.officerUser);
                });
        assertThat(attach(school, school.staff, 4, UUID.randomUUID(), now)).as("the account is never replaced")
                .isZero();

        school.sql("UPDATE hr.staff SET user_id = NULL, deleted_at = now() WHERE id = '" + school.staff + "'");
        assertThat(attach(school, school.staff, 4, account, now)).as("a deleted staff member").isZero();
    }

    private int attach(School as, UUID staff, int version, UUID account, OffsetDateTime now) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(as.tenant, null),
                () -> TestTransactions.of(database).write(
                        () -> staffRepository.attachAccount(staff, version, account, now, as.officerUser)));
    }

    @Test
    void theLoginAccountIsGivenOnlyByItsOperationNeverTypedIn() throws Exception {
        School school = new School();
        String office = token(school.officerUser, school.tenant, List.of(school.institution.toString()),
                "hr.staff.read", "hr.staff.create", "hr.staff.update");
        UUID someone = UUID.randomUUID();

        Reply created = call("POST", "/v1/hr/staff", office, "{\"personId\":\"" + UUID.randomUUID()
                + "\",\"institutionId\":\"" + school.institution + "\",\"userId\":\"" + someone + "\"}");
        assertThat(created.status()).as(created.text()).isEqualTo(422);
        assertThat(created.body().path("errors").path(0).path("field").asString()).isEqualTo("userId");

        String path = "/v1/hr/staff/" + school.staff;
        Reply typed = call("PATCH", path, office, "{\"userId\":\"" + someone + "\"}", "If-Match", "\"1\"");
        assertThat(typed.status()).as(typed.text()).isEqualTo(422);
        assertThat(school.get().body().has("userId")).isFalse();

        school.sql("UPDATE hr.staff SET user_id = '" + someone + "' WHERE id = '" + school.staff + "'");
        Reply same = call("PATCH", path, office, "{\"userId\":\"" + someone + "\",\"specialization\":\"Physics\"}",
                "If-Match", "\"1\"");
        assertThat(same.status()).as("the account it has is sent back unchanged: " + same.text()).isEqualTo(200);
        Reply cleared = call("PATCH", path, office, "{\"userId\":null}", "If-Match", "\"2\"");
        assertThat(cleared.status()).as("a wrong account is cleared: " + cleared.text()).isEqualTo(200);
        assertThat(cleared.body().has("userId")).isFalse();
        assertThat(school.events("hr.staff.updated")).as("each change carries the member payload").hasSize(2)
                .last().satisfies(payload -> assertThat(JSON.readTree(payload).path("userId").isNull()).isTrue());
    }
}
