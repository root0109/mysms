package com.mysms.campus.library;

import com.mysms.campus.CampusApplication;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.events.LibraryPrivacyTaskHandler;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.privacyclient.testing.TestLegalHolds;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
 * library's privacy tasks against real PostgreSQL as the application role, with privacy's legal holds from
 * {@link TestLegalHolds} (design PRIVACY-AND-CONSENT-CENTRE §9.1): an erasure clears and soft-deletes the subject's
 * reservation, clears the requester of their purchase request and the borrower of their returned loan, and
 * deactivates and anonymises their card, once per task id; an open loan keeps itself and its card, counted; a held
 * subject's rows are kept and counted; every answer is on the outbox.
 */
@SpringBootTest(classes = CampusApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LibraryPrivacyTaskComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final LocalDate ISSUED = LocalDate.parse("2026-03-02");

    @Autowired
    @Qualifier(LibraryDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private LibraryPrivacyTaskHandler handler;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "library", "transport", "frontoffice", "facility");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /**
     * A student's library card with a returned loan (and an issued one when {@code open}), a reservation and a
     * purchase request of the student; the person's id.
     */
    private UUID member(UUID tenant, boolean open) {
        UUID person = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID cardType = UUID.randomUUID();
        UUID card = UUID.randomUUID();
        UUID media = UUID.randomUUID();
        write(tenant, () -> {
            database.jdbc().sql("INSERT INTO library.library_card_type (id, tenant_id, name, max_items, loan_days,"
                    + " code, row_version) VALUES (:id, :tenantId, 'Student', 2, 14, 'STU', 1)").param("id", cardType)
                    .update();
            database.jdbc().sql("INSERT INTO library.library_card (id, tenant_id, institution_id, card_number,"
                    + " card_type_id, holder_type, person_id, student_id, issue_date, row_version) VALUES (:id,"
                    + " :tenantId, :institution, 'LC-1', :type, 'student', :person, :student, :issued, 1)")
                    .param("id", card).param("institution", institution).param("type", cardType)
                    .param("person", person).param("student", UUID.randomUUID()).param("issued", ISSUED).update();
            database.jdbc().sql("INSERT INTO library.media (id, tenant_id, institution_id, title, row_version)"
                    + " VALUES (:id, :tenantId, :institution, 'Kidnapped', 1)").param("id", media)
                    .param("institution", institution).update();
            loan(media, card, "returned", 1);
            if (open) {
                loan(media, card, "issued", 2);
            }
            database.jdbc().sql("INSERT INTO library.reservation (id, tenant_id, media_id, requested_by_person_id,"
                    + " date_from, date_to, row_version) VALUES (:id, :tenantId, :media, :person, :from, :from, 1)")
                    .param("id", UUID.randomUUID()).param("media", media).param("person", person)
                    .param("from", ISSUED).update();
            database.jdbc().sql("INSERT INTO library.purchase_request (id, tenant_id, title, author_text,"
                    + " requested_by_person_id, row_version) VALUES (:id, :tenantId, 'Catriona', 'R. L. Stevenson',"
                    + " :person, 1)").param("id", UUID.randomUUID()).param("person", person).update();
            return null;
        });
        return person;
    }

    /** A copy of the title and its loan on the card, in the given state, borrowed by an account. */
    private void loan(UUID media, UUID card, String status, int copy) {
        UUID unit = UUID.randomUUID();
        database.jdbc().sql("INSERT INTO library.media_unit (id, tenant_id, media_id, accession_number, row_version)"
                + " VALUES (:id, :tenantId, :media, :accession, 1)").param("id", unit).param("media", media)
                .param("accession", "ACC-" + copy).update();
        database.jdbc().sql("INSERT INTO library.loan (id, tenant_id, media_unit_id, media_id, library_card_id,"
                + " borrower_user_id, issued_on, due_on, status, row_version) VALUES (:id, :tenantId, :unit, :media,"
                + " :card, :user, :issued, :due, :status, 1)").param("id", UUID.randomUUID()).param("unit", unit)
                .param("media", media).param("card", card).param("user", UUID.randomUUID()).param("issued", ISSUED)
                .param("due", ISSUED.plusDays(14)).param("status", status).update();
    }

    private void erase(UUID tenant, UUID taskId, UUID subject) {
        EventEnvelope event = new EventEnvelope(UUID.randomUUID(), "privacy", "privacy.rights_request.task_assigned",
                Optional.of(tenant), "privacy.rights_request_task", taskId, Instant.now(),
                "{\"requestId\":\"" + UUID.randomUUID() + "\",\"taskId\":\"" + taskId + "\",\"type\":\"erasure\","
                + "\"action\":\"erase\",\"subjectPersonId\":\"" + subject + "\",\"module\":\"library\",\"wave\":2,"
                + "\"deadline\":\"2027-01-31T00:00:00Z\"}", Optional.of(TRACE));
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        assertThat(inbox.deliver(event, handler::receive)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), () -> {
            handler.run();
            return null;
        });
    }

    private List<JsonNode> answers(UUID tenant, UUID taskId) {
        return read(tenant, () -> database.jdbc().sql("SELECT payload FROM library.outbox_event WHERE tenant_id ="
                + " :tenantId AND aggregate_id = :id AND event_type = 'library.privacy_task.completed' ORDER BY id")
                .param("id", taskId).query(String.class).list()).stream().map(JSON::readTree).toList();
    }

    /** What a count statement of the tenant finds for the person. */
    private int count(UUID tenant, String sql, UUID person) {
        return read(tenant, () -> database.jdbc().sql(sql).param("person", person).query(Integer.class).single());
    }

    private int cards(UUID tenant, UUID person) {
        return count(tenant, "SELECT count(*) FROM library.library_card WHERE tenant_id = :tenantId"
                + " AND person_id = :person AND status = 'active' AND deleted_at IS NULL", person);
    }

    /** The tenant's loans that still name their borrower's account. */
    private int borrowed(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM library.loan WHERE tenant_id = :tenantId"
                + " AND borrower_user_id IS NOT NULL").query(Integer.class).single());
    }

    private int requested(UUID tenant, UUID person) {
        return count(tenant, "SELECT count(*) FROM library.reservation WHERE tenant_id = :tenantId"
                + " AND requested_by_person_id = :person", person)
                + count(tenant, "SELECT count(*) FROM library.purchase_request WHERE tenant_id = :tenantId"
                + " AND requested_by_person_id = :person", person);
    }

    private int audited(UUID tenant) {
        return read(tenant, () -> database.jdbc().sql("SELECT count(*) FROM library.outbox_event WHERE tenant_id ="
                + " :tenantId AND event_type = 'audit.recorded'").query(Integer.class).single());
    }

    @Test
    void anErasureAnonymisesTheCardAndItsHistoryOncePerTask() {
        UUID tenant = UUID.randomUUID();
        UUID person = member(tenant, false);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(cards(tenant, person)).as("the card is the person's no more").isZero();
        assertThat(borrowed(tenant)).isZero();
        assertThat(requested(tenant, person)).isZero();
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("done");
        assertThat(answer.path("counts").path("library.library_card").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("library.loan").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("library.reservation").asInt()).isEqualTo(1);
        assertThat(answer.path("counts").path("library.purchase_request").asInt()).isEqualTo(1);
        int audits = audited(tenant);

        erase(tenant, taskId, person);

        assertThat(answers(tenant, taskId)).hasSize(2).allSatisfy(again -> assertThat(again).isEqualTo(answer));
        assertThat(audited(tenant)).as("nothing is erased twice").isEqualTo(audits);
    }

    @Test
    void anOpenLoanIsKeptWithItsCardAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = member(tenant, true);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(cards(tenant, person)).as("the card of the open loan stays").isEqualTo(1);
        assertThat(borrowed(tenant)).as("the open loan's borrower stays").isEqualTo(1);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("counts").path("library.loan").asInt()).as("the returned loan").isEqualTo(1);
        assertThat(answer.path("counts").has("library.library_card")).isFalse();
        assertThat(answer.path("heldItems").asInt()).as("the open loan and its card").isEqualTo(2);
    }

    @Test
    void aHeldSubjectsRowsAreKeptAndCounted() {
        UUID tenant = UUID.randomUUID();
        UUID person = member(tenant, true);
        TestLegalHolds.holdSubject(person);
        UUID taskId = UUID.randomUUID();

        erase(tenant, taskId, person);

        assertThat(cards(tenant, person)).isEqualTo(1);
        assertThat(requested(tenant, person)).isEqualTo(2);
        JsonNode answer = answers(tenant, taskId).get(0);
        assertThat(answer.path("status").asString()).isEqualTo("partial");
        assertThat(answer.path("heldItems").asInt())
                .as("the reservation, the purchase request, both loans and the card, each once").isEqualTo(5);
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
