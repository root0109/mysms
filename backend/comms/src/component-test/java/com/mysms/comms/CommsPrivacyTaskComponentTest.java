package com.mysms.comms;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.events.CommsPrivacyTaskHandler;
import com.mysms.comms.events.StudentRecordSync;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.InboxProcessor;
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
 * comms' privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1, §10): the person and account of a student record
 * arrive with sis's event; an erasure cancels and clears the messages to the student, clears what the student wrote,
 * removes their memberships and closes the feedback about them, once per task id, leaving others' messages alone; a
 * held subject's rows are kept and counted; a person comms does not know is not applicable;
 * comms.media_after_withdrawal is not swept; every answer is on the outbox.
 */
@SpringBootTest(classes = CommsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommsPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    @Qualifier(CommsDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private CommsPrivacyTaskHandler handler;

    @Autowired
    private StudentRecordSync records;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "comms");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** What comms holds of one student and of the teacher talking to them. */
    private record Student(UUID person, UUID user, UUID student, UUID conversation) {
    }

    /**
     * A student (made known by sis's event) with a queued SMS, a chat with a teacher (both wrote), a group membership
     * and a feedback thread about them with the teacher's answer.
     */
    private Student student(UUID tenant) {
        Student s = new Student(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        deliver(tenant, "sis", "sis.student.created", "sis.student", s.student(), studentEvent(s, 1), records::apply);
        UUID teacher = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID group = UUID.randomUUID();
        UUID thread = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO comms.outbound_message (id, tenant_id, institution_id, message_kind,"
                    + " channel, sender, recipient, recipient_user_id, body, row_version) VALUES (:id, :tenantId,"
                    + " :institution, 'notification', 'sms', 'SCHOOL', '9800000000', :user, 'Bus is late', 1)")
                    .param("id", UUID.randomUUID()).param("institution", institution).param("user", s.user())
                    .update();
            database.jdbc().sql("INSERT INTO comms.conversation (id, tenant_id, conversation_type, row_version)"
                    + " VALUES (:id, :tenantId, 'direct', 1)").param("id", s.conversation()).update();
            for (UUID user : List.of(s.user(), teacher)) {
                database.jdbc().sql("INSERT INTO comms.conversation_participant (id, tenant_id, conversation_id,"
                        + " user_id, row_version) VALUES (:id, :tenantId, :conversation, :user, 1)")
                        .param("id", UUID.randomUUID()).param("conversation", s.conversation()).param("user", user)
                        .update();
                database.jdbc().sql("INSERT INTO comms.conversation_message (id, tenant_id, conversation_id,"
                        + " sender_user_id, body, row_version) VALUES (:id, :tenantId, :conversation, :user, 'Hello',"
                        + " 1)").param("id", UUID.randomUUID()).param("conversation", s.conversation())
                        .param("user", user).update();
            }
            database.jdbc().sql("INSERT INTO comms.audience_group (id, tenant_id, name, row_version) VALUES (:id,"
                    + " :tenantId, 'Chess club', 1)").param("id", group).update();
            database.jdbc().sql("INSERT INTO comms.audience_group_member (id, tenant_id, group_id, user_id,"
                    + " row_version) VALUES (:id, :tenantId, :group, :user, 1)").param("id", UUID.randomUUID())
                    .param("group", group).param("user", s.user()).update();
            database.jdbc().sql("INSERT INTO comms.feedback_thread (id, tenant_id, institution_id, guardian_id,"
                    + " student_id, assigned_to_user_id, row_version) VALUES (:id, :tenantId, :institution, :guardian,"
                    + " :student, :teacher, 1)").param("id", thread).param("institution", institution)
                    .param("guardian", UUID.randomUUID()).param("student", s.student()).param("teacher", teacher)
                    .update();
            database.jdbc().sql("INSERT INTO comms.feedback_message (id, tenant_id, thread_id, sender_user_id,"
                    + " category, message, row_version) VALUES (:id, :tenantId, :thread, :teacher, 'complaint',"
                    + " 'Seen to it', 1)").param("id", UUID.randomUUID()).param("thread", thread)
                    .param("teacher", teacher).update();
            return null;
        });
        return s;
    }

    private static String studentEvent(Student s, int version) {
        return "{\"id\":\"" + s.student() + "\",\"userId\":\"" + s.user() + "\",\"personId\":\"" + s.person()
                + "\",\"institutionId\":\"" + UUID.randomUUID() + "\",\"rowVersion\":" + version + "}";
    }

    private void deliver(UUID tenant, String source, String type, String aggregate, UUID id, String data,
            EventHandler receiver) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), source, type, Optional.of(tenant), aggregate, id,
                Instant.now(), data, Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, receiver)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
    }

    private void privacy(UUID tenant, String type, String aggregate, UUID id, String data) {
        deliver(tenant, "privacy", type, aggregate, id, data, handler::receive);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        privacy(tenant, "privacy.rights_request.task_assigned", "privacy.rights_request_task", taskId,
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"comms\",\"wave\":1,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}");
    }

    private List<JsonNode> answers(UUID tenant, String type, UUID id) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM comms.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = :type ORDER BY id").param("id", id)
                .param("type", type).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    /** The bodies of the conversation's live messages, in sender order (the student's first when kept). */
    private List<String> liveMessages(UUID tenant, UUID conversation) {
        return read(tenant, () -> database.jdbc().sql("SELECT body FROM comms.conversation_message WHERE tenant_id ="
                + " :tenantId AND conversation_id = :conversation AND deleted_at IS NULL ORDER BY created_at, id")
                .param("conversation", conversation).query(String.class).list());
    }

    private String smsStatus(UUID tenant, UUID user) {
        return read(tenant, () -> database.jdbc().sql("SELECT status || ':' || coalesce(body, '') FROM comms.outbound_message"
                + " WHERE tenant_id = :tenantId AND recipient_user_id = :user").param("user", user)
                .query(String.class).single());
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM comms.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureStopsAndClearsWhatConcernsTheStudentOncePerTask() {
        UUID tenant = UUID.randomUUID();
        Student s = student(tenant);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, s.person());

        assertThat(smsStatus(tenant, s.user())).isEqualTo("cancelled:Erased");
        assertThat(liveMessages(tenant, s.conversation())).as("the teacher's message stays").containsExactly("Hello");
        assertThat(read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM comms.feedback_message WHERE"
                + " tenant_id = :tenantId AND message = 'Erased' AND deleted_at IS NOT NULL")
                .query(Integer.class).single())).as("the feedback about the student").isEqualTo(1);
        JsonNode answer = answers(tenant, "comms.privacy_task.completed", taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        JsonNode counts = answer.path("counts");
        assertThat(counts.path("comms.outbound_message").asInt()).isEqualTo(1);
        assertThat(counts.path("comms.conversation_message").asInt()).isEqualTo(1);
        assertThat(counts.path("comms.conversation_participant").asInt()).isEqualTo(1);
        assertThat(counts.path("comms.audience_group_member").asInt()).isEqualTo(1);
        assertThat(counts.path("comms.feedback_message").asInt()).isEqualTo(1);
        assertThat(counts.path("comms.feedback_thread").asInt()).isEqualTo(1);
        int audits = audited(tenant);

        erase(tenant, taskId, s.person());

        assertThat(answers(tenant, "comms.privacy_task.completed", taskId)).hasSize(2)
                .allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        Student s = student(tenant);
        TestLegalHolds.holdSubject(s.person());
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, s.person());

        assertThat(smsStatus(tenant, s.user())).isEqualTo("queued:Bus is late");
        assertThat(liveMessages(tenant, s.conversation())).containsExactly("Hello", "Hello");
        JsonNode answer = answers(tenant, "comms.privacy_task.completed", taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt())
                .as("the SMS, the chat place and message, the membership, the feedback thread and message")
                .isEqualTo(6);
    }

    @Test
    void anErasureOfAPersonCommsDoesNotKnowIsNotApplicable() {
        UUID tenant = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, UUID.randomUUID());

        assertThat(answers(tenant, "comms.privacy_task.completed", taskId).get(0).path("status").asString())
                .isEqualTo("not_applicable");
    }

    @Test
    void aStudentRecordChangesOnlyToANewerVersion() {
        UUID tenant = UUID.randomUUID();
        Student s = student(tenant);
        UUID event = UUID.randomUUID();

        boolean older = CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(() -> records.apply(new EventEnvelope(event, "sis",
                        "sis.student.updated", Optional.of(tenant), "sis.student", s.student(), Instant.now(),
                        studentEvent(s, 1), Optional.of(TRACE)))));

        assertThat(older).as("a repeated version changes nothing").isFalse();
    }

    @Test
    void mediaAfterWithdrawalIsNotSwept() {
        UUID tenant = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        privacy(tenant, "privacy.retention.due", "privacy.retention_run", runId, "{\"runId\":\"" + runId
                + "\",\"policyCode\":\"comms.media_after_withdrawal\",\"categoryCode\":\"photo_media\","
                + "\"module\":\"comms\",\"cutoff\":\"2026-06-01T00:00:00Z\",\"action\":\"delete\"}");

        JsonNode answer = answers(tenant, "comms.privacy_retention.completed", runId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("failed");
        assertThat(answer.path("errorCode").asString()).isEqualTo("RETENTION_POLICY_UNSUPPORTED");
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
