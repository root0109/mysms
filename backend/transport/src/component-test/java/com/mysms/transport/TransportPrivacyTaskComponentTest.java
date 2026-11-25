package com.mysms.transport;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.privacyclient.testing.TestLegalHolds;
import com.mysms.transport.config.TransportDatabaseConfig;
import com.mysms.transport.events.TransportPrivacyTaskHandler;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * transport's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): transport cannot map a person to its rows, so an
 * erasure, held or not, and an export answer not applicable, once per task id, and change nothing.
 */
@SpringBootTest(classes = TransportApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransportPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(TransportDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private TransportPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "transport");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A student's transport arrangement with notes. */
    private void arrangement(UUID tenant) {
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(() -> database.jdbc().sql("INSERT INTO"
                        + " transport.student_transport (id, tenant_id, student_id, academic_year_id, transport_mode,"
                        + " details) VALUES (:id, :tenantId, :student, :year, 'auto', 'Driver: 98450 00000')")
                        .param("id", UUID.randomUUID()).param("student", UUID.randomUUID())
                        .param("year", UUID.randomUUID()).update()));
    }

    private void task(UUID tenant, UUID taskId, String action, UUID subject) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", "privacy.rights_request.task_assigned",
                Optional.of(tenant), "privacy.rights_request_task", taskId, Instant.now(),
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"" + action + "\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"transport\","
                + "\"wave\":2,\"deadline\":\"2027-01-31T00:00:00Z\"}", Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM transport.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'transport.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    @Test
    void everyTaskAnswersNotApplicableOncePerTaskAndChangesNothing() {
        UUID tenant = UUID.randomUUID();
        arrangement(tenant);
        UUID person = UUID.randomUUID();
        UUID held = UUID.randomUUID();
        TestLegalHolds.holdSubject(held);
        UUID erase = UUID.randomUUID();
        UUID heldErase = UUID.randomUUID();
        UUID export = UUID.randomUUID();

        task(tenant, erase, "erase", person);
        task(tenant, heldErase, "erase", held);
        task(tenant, export, "export", person);
        task(tenant, erase, "erase", person);

        for (UUID taskId : List.of(heldErase, export)) {
            assertThat(answers(tenant, taskId).get(0).path("status").asString()).isEqualTo("not_applicable");
        }
        List<JsonNode> erased = answers(tenant, erase);
        assertThat(erased).as("answered again from the receipt").hasSize(2);
        assertThat(erased.get(1)).isEqualTo(erased.get(0));
        assertThat(erased.get(0).path("status").asString()).isEqualTo("not_applicable");
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT details FROM transport.student_transport"
                + " WHERE tenant_id = :tenantId").query(String.class).list().get(0)))
                .isEqualTo("Driver: 98450 00000");
    }
}
