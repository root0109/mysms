package com.mysms.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.events.PlatformPrivacyTaskHandler;
import com.mysms.privacyclient.testing.TestLegalHolds;
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
 * platform's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): an erasure soft-deletes the notes, attachments and
 * tags on the person's record and the files it owns or has attached, their text and names cleared, once per task id; a
 * held subject's rows are kept and counted; an export counts them; every answer is on the outbox.
 */
@SpringBootTest(classes = PlatformApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlatformPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PERSON = "people.person";

    @Autowired
    @Qualifier(PlatformDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private PlatformPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "platform");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** A tenant whose person has a note, a tag, an owned photo and an attached document; the tenant. */
    private UUID tenant(UUID person) {
        UUID tenant = UUID.randomUUID();
        UUID tag = UUID.randomUUID();
        UUID document = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO platform.tenant (id, code, name, locale, timezone) VALUES (:tenantId,"
                    + " :code, 'School group', 'hi', 'Asia/Kolkata')")
                    .param("code", "p-" + tenant.toString().substring(0, 8)).update();
            note(person, "Prefers mornings");
            note(UUID.randomUUID(), "Another person's note");
            database.jdbc().sql("INSERT INTO platform.tag (id, tenant_id, name, slug) VALUES (:id, :tenantId,"
                    + " 'Scholarship', :slug)").param("id", tag).param("slug", "s-" + tag).update();
            database.jdbc().sql("INSERT INTO platform.tag_assignment (id, tenant_id, tag_id, entity_type, entity_id)"
                    + " VALUES (:id, :tenantId, :tag, :type, :person)").param("id", UUID.randomUUID())
                    .param("tag", tag).param("type", PERSON).param("person", person).update();
            file(UUID.randomUUID(), "asha-photo.jpg", ", owner_entity_type, owner_entity_id", ", :type, :person",
                    person);
            file(document, "asha-certificate.pdf", "", "", person);
            database.jdbc().sql("INSERT INTO platform.entity_attachment (id, tenant_id, entity_type, entity_id,"
                    + " file_id, title) VALUES (:id, :tenantId, :type, :person, :file, 'Birth certificate')")
                    .param("id", UUID.randomUUID()).param("type", PERSON).param("person", person)
                    .param("file", document).update();
            return null;
        });
        return tenant;
    }

    private void note(UUID entity, String text) {
        database.jdbc().sql("INSERT INTO platform.entity_note (id, tenant_id, institution_id, entity_type, entity_id,"
                + " note_text) VALUES (:id, :tenantId, :institution, :type, :entity, :text)")
                .param("id", UUID.randomUUID()).param("institution", UUID.randomUUID()).param("type", PERSON)
                .param("entity", entity).param("text", text).update();
    }

    /** A file, owned by the person's record when the owner columns and values are given. */
    private void file(UUID id, String name, String ownerColumns, String ownerValues, UUID person) {
        database.jdbc().sql("INSERT INTO platform.file_object (id, tenant_id, storage_disk, storage_key, file_name,"
                + " original_name, size_bytes, status" + ownerColumns + ") VALUES (:id, :tenantId, 'local', :key,"
                + " :name, :name, 1, 'available'" + ownerValues + ")").param("id", id).param("key", "files/" + id)
                .param("name", name).param("type", PERSON).param("person", person).update();
    }

    private void task(UUID tenant, UUID taskId, String action, UUID subject) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", "privacy.rights_request.task_assigned",
                Optional.of(tenant), "privacy.rights_request_task", taskId, Instant.now(),
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"" + action + "\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"platform\","
                + "\"wave\":2,\"deadline\":\"2027-01-31T00:00:00Z\"}", Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM platform.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'platform.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    private int count(UUID tenant, String sql) {
        return read(tenant, () -> database.jdbc().sql(sql).query(Integer.class).single());
    }

    @Test
    void anErasureSoftDeletesWhatIsOnThePersonsRecordOncePerTask() {
        UUID person = UUID.randomUUID();
        UUID tenant = tenant(person);
        UUID taskId = UUID.randomUUID();

        task(tenant, taskId, "erase", person);

        assertThat(count(tenant, "SELECT count(*) FROM platform.entity_note WHERE tenant_id = :tenantId"
                + " AND note_text = 'Erased' AND deleted_at IS NOT NULL")).isEqualTo(1);
        assertThat(count(tenant, "SELECT count(*) FROM platform.entity_note WHERE tenant_id = :tenantId"
                + " AND deleted_at IS NULL")).as("another person's note stays").isEqualTo(1);
        assertThat(count(tenant, "SELECT count(*) FROM platform.file_object WHERE tenant_id = :tenantId"
                + " AND file_name = 'erased' AND original_name IS NULL AND status = 'deleted'"
                + " AND deleted_at IS NOT NULL")).as("the owned and the attached file").isEqualTo(2);
        assertThat(count(tenant, "SELECT count(*) FROM platform.entity_attachment WHERE tenant_id = :tenantId"
                + " AND title IS NULL AND deleted_at IS NOT NULL")).isEqualTo(1);
        assertThat(count(tenant, "SELECT count(*) FROM platform.outbox_event WHERE tenant_id = :tenantId"
                + " AND event_type = 'platform.file_object.deleted'")).as("the files' owners clean up").isEqualTo(2);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("platform.entity_note").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("platform.entity_attachment").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("platform.tag_assignment").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("platform.file_object").asInt()).isEqualTo(2);
        String audits = "SELECT count(*) FROM platform.outbox_event WHERE tenant_id = :tenantId"
                + " AND event_type = 'audit.recorded'";
        int audited = count(tenant, audits);

        task(tenant, taskId, "erase", person);

        assertThat(answers(tenant, taskId)).hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(count(tenant, audits)).as("nothing is erased twice").isEqualTo(audited);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCountedAndAnExportCountsThem() {
        UUID person = UUID.randomUUID();
        UUID tenant = tenant(person);
        TestLegalHolds.holdSubject(person);
        UUID erase = UUID.randomUUID();
        UUID export = UUID.randomUUID();

        task(tenant, erase, "erase", person);
        task(tenant, export, "export", person);

        assertThat(count(tenant, "SELECT count(*) FROM platform.entity_note WHERE tenant_id = :tenantId"
                + " AND note_text = 'Prefers mornings' AND deleted_at IS NULL")).isEqualTo(1);
        JsonNode held = answers(tenant, erase).get(0);
        assertThat(held.path("status").asString()).isEqualTo("partial");
        assertThat(held.path("heldItems").asInt()).as("note, attachment, tag and two files").isEqualTo(5);
        JsonNode exported = answers(tenant, export).get(0);
        assertThat(exported.path("status").asString()).isEqualTo("done");
        assertThat(exported.path("counts").path("platform.file_object").asInt()).isEqualTo(2);
        assertThat(exported.path("counts").path("platform.entity_note").asInt()).isEqualTo(1);
    }

    @Test
    void aPersonPlatformKnowsNothingOfIsNotApplicable() {
        UUID tenant = tenant(UUID.randomUUID());
        UUID taskId = UUID.randomUUID();

        task(tenant, taskId, "erase", UUID.randomUUID());

        assertThat(answers(tenant, taskId).get(0).path("status").asString()).isEqualTo("not_applicable");
    }

    private void write(UUID tenant, Supplier<Object> work) {
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(work::get));
    }

    private <T> T read(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).read(work::get));
    }
}
