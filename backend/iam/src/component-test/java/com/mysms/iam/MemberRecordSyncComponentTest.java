package com.mysms.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.events.MemberRecordSync;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.messaging.events.RejectedEventException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Real PostgreSQL: iam keeps each account's and person's staff and student records from hr's and sis's events, and
 * iam.GetMe names them — the newest row version wins, a late or repeated event changes nothing, a deleted record is
 * gone for good, another tenant's records are never seen, and an event that does not belong to its record is rejected.
 */
@SpringBootTest(classes = IamApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberRecordSyncComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    @Qualifier(IamDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private MemberRecordSync sync;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "iam");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
        registry.add("mysms.iam.token.signing-key", TOKENS::privateKeyPem);
    }

    private static EventEnvelope event(UUID tenant, String type, UUID record, UUID user, UUID person, int version) {
        String data = "{\"id\":\"" + record + "\",\"userId\":" + (user == null ? "null" : "\"" + user + "\"")
                + ",\"personId\":\"" + person + "\",\"institutionId\":\"" + UUID.randomUUID() + "\",\"rowVersion\":"
                + version + "}";
        String aggregate = type.substring(0, type.lastIndexOf('.'));
        return new EventEnvelope(UUID.randomUUID(), aggregate.substring(0, aggregate.indexOf('.')), type,
                Optional.of(tenant), aggregate, record, Instant.now(), data, Optional.of(TRACE));
    }

    private boolean deliver(EventEnvelope event) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(event.tenantId().orElseThrow(), null),
                () -> TestTransactions.of(database).write(() -> sync.apply(event)));
    }

    private <T> T as(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), work);
    }

    private UUID account(UUID tenant, UUID person) {
        UUID id = UUID.randomUUID();
        as(tenant, () -> TestTransactions.of(database).write(() -> database.jdbc().sql("""
                INSERT INTO iam.user_account (id, tenant_id, person_id, username, status, user_type)
                VALUES (:id, :tenantId, :person, :username, 'active', 'teacher')
                """).param("id", id).param("person", person).param("username", "u" + id).update()));
        return id;
    }

    private JsonNode me(UUID tenant, UUID user) throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.toString());
        claims.put("tid", tenant.toString());
        claims.put("aud", "mysms");
        claims.put("perms", List.of("iam.user_account.get_me"));
        claims.put("ins", List.of("*"));
        HttpResponse<String> response = HTTP.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/v1/iam/me")).GET().header("Authorization", "Bearer "
                        + TOKENS.custom(claims, Instant.now().plusSeconds(300))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private static List<String> ids(JsonNode me, String field) {
        List<String> ids = new ArrayList<>();
        me.path(field).forEach(id -> ids.add(id.asString()));
        return ids;
    }

    @Test
    void getMeNamesTheStaffAndStudentRecordsOfTheAccountAndOfItsPerson() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID user = account(tenant, person);
        UUID byAccount = UUID.randomUUID();
        UUID byPerson = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID someoneElse = UUID.randomUUID();
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        EventEnvelope created = event(tenant, "hr.staff.created", byAccount, user, UUID.randomUUID(), 1);
        assertThat(inbox.deliver(created, sync::apply)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(created, sync::apply)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);
        deliver(event(tenant, "hr.staff.created", byPerson, null, person, 1));
        deliver(event(tenant, "sis.student.created", student, user, person, 1));
        deliver(event(tenant, "hr.staff.created", someoneElse, UUID.randomUUID(), UUID.randomUUID(), 1));
        // the same ids in another tenant are not this tenant's
        deliver(event(UUID.randomUUID(), "hr.staff.created", UUID.randomUUID(), user, person, 1));

        JsonNode me = me(tenant, user);
        assertThat(ids(me, "staffIds")).containsExactlyInAnyOrder(byAccount.toString(), byPerson.toString());
        assertThat(ids(me, "studentIds")).containsExactly(student.toString());

        // the account leaves the first record (version 3); the late version 2 changes nothing
        assertThat(deliver(event(tenant, "hr.staff.updated", byAccount, null, UUID.randomUUID(), 3))).isTrue();
        assertThat(deliver(event(tenant, "hr.staff.updated", byAccount, user, UUID.randomUUID(), 2))).isFalse();
        assertThat(ids(me(tenant, user), "staffIds")).containsExactly(byPerson.toString());

        // a deleted record is gone, and a late "updated" cannot bring it back
        assertThat(deliver(event(tenant, "sis.student.deleted", student, user, person, 2))).isTrue();
        assertThat(deliver(event(tenant, "sis.student.updated", student, user, person, 1))).isFalse();
        assertThat(ids(me(tenant, user), "studentIds")).isEmpty();
    }

    @Test
    void anEventWithoutTheOwnerChangesNothingAndAForeignOneIsRejected() {
        UUID tenant = UUID.randomUUID();
        UUID record = UUID.randomUUID();
        EventEnvelope cleanup = new EventEnvelope(UUID.randomUUID(), "hr", "hr.staff.updated", Optional.of(tenant),
                "hr.staff", record, Instant.now(), "{\"id\":\"" + record + "\"}", Optional.of(TRACE));
        assertThat(deliver(cleanup)).as("a reference cleanup's announcement").isFalse();
        EventEnvelope foreign = new EventEnvelope(UUID.randomUUID(), "hr", "hr.staff.updated", Optional.of(tenant),
                "hr.staff", record, Instant.now(), "{\"id\":\"" + UUID.randomUUID() + "\",\"rowVersion\":1}",
                Optional.of(TRACE));
        assertThatThrownBy(() -> deliver(foreign)).isInstanceOf(RejectedEventException.class);
        EventEnvelope malformed = new EventEnvelope(UUID.randomUUID(), "sis", "sis.student.created",
                Optional.of(tenant), "sis.student", record, Instant.now(),
                "{\"id\":\"" + record + "\",\"userId\":\"not-a-uuid\",\"rowVersion\":1}", Optional.of(TRACE));
        assertThatThrownBy(() -> deliver(malformed)).isInstanceOf(RejectedEventException.class);
    }
}
