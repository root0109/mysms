package com.mysms.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.InboxProcessor;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.events.GuardianLinkSync;
import com.mysms.privacy.events.SubjectSync;
import java.sql.Array;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Real PostgreSQL as the non-owner role: privacy keeps its subjects and guardian links from people's and sis's events
 * through the real handler beans, inside each event's tenant — a person event sets the birth date, status and
 * majority date, an older one changes nothing, student events fill and empty the subject's records, a guardian link
 * turns inactive when deleted, and one tenant's events never show in another's.
 */
@SpringBootTest(classes = PrivacyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PrivacyProjectionsComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

    @Autowired
    @Qualifier(PrivacyDatabaseConfig.DATABASE)
    private ModuleDatabase database;

    @Autowired
    private SubjectSync subjects;

    @Autowired
    private GuardianLinkSync links;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "privacy");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    private static EventEnvelope event(UUID tenant, String type, UUID id, String data) {
        String aggregate = type.substring(0, type.lastIndexOf('.'));
        return new EventEnvelope(UUID.randomUUID(), aggregate.substring(0, aggregate.indexOf('.')), type,
                Optional.of(tenant), aggregate, id, Instant.now(), data, Optional.of(TRACE));
    }

    private static EventEnvelope person(UUID tenant, String verb, UUID person, LocalDate birthDate, int version) {
        return event(tenant, "people.person." + verb, person, "{\"id\":\"" + person + "\",\"dateOfBirth\":"
                + (birthDate == null ? "null" : "\"" + birthDate + "\"") + ",\"rowVersion\":" + version + "}");
    }

    private static EventEnvelope student(UUID tenant, String verb, UUID student, UUID person, UUID user,
            UUID institution, int version) {
        return event(tenant, "sis.student." + verb, student, "{\"id\":\"" + student + "\",\"userId\":"
                + (user == null ? "null" : "\"" + user + "\"") + ",\"personId\":\"" + person
                + "\",\"institutionId\":\"" + institution + "\",\"rowVersion\":" + version + "}");
    }

    private static EventEnvelope link(UUID tenant, String verb, UUID link, UUID student, UUID guardian, int version) {
        return event(tenant, "sis.student_guardian." + verb, link, "{\"id\":\"" + link + "\",\"studentPersonId\":\""
                + student + "\",\"guardianPersonId\":\"" + guardian + "\",\"guardianUserId\":null,"
                + "\"isLegalGuardian\":true,\"hasCustody\":true,\"status\":\"active\",\"rowVersion\":" + version + "}");
    }

    private <T> T as(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null), work);
    }

    private boolean deliverSubject(EventEnvelope event) {
        return as(event.tenantId().orElseThrow(), () -> TestTransactions.of(database).write(() -> subjects.apply(event)));
    }

    private boolean deliverLink(EventEnvelope event) {
        return as(event.tenantId().orElseThrow(), () -> TestTransactions.of(database).write(() -> links.apply(event)));
    }

    private Optional<Map<String, Object>> subject(UUID tenant, UUID person) {
        return as(tenant, () -> TestTransactions.of(database).read(() -> database.jdbc().sql("""
                SELECT s.status, s.birth_date, s.majority_date, s.student_ids, s.user_ids, s.institution_ids
                  FROM privacy.subject s
                 WHERE s.tenant_id = :tenantId AND s.person_id = :person
                """).param("person", person).query((rs, row) -> Map.<String, Object>of(
                        "status", rs.getString("status"),
                        "birthDate", Optional.ofNullable(rs.getObject("birth_date", LocalDate.class)),
                        "majorityDate", Optional.ofNullable(rs.getObject("majority_date", LocalDate.class)),
                        "studentIds", uuids(rs.getArray("student_ids")),
                        "userIds", uuids(rs.getArray("user_ids")),
                        "institutionIds", uuids(rs.getArray("institution_ids")))).optional()));
    }

    private Optional<String> linkStatus(UUID tenant, UUID link) {
        return as(tenant, () -> TestTransactions.of(database).read(() -> database.jdbc().sql("""
                SELECT g.status FROM privacy.guardian_link g WHERE g.tenant_id = :tenantId AND g.link_id = :link
                """).param("link", link).query(String.class).optional()));
    }

    private static List<UUID> uuids(Array array) throws SQLException {
        return List.of((UUID[]) array.getArray());
    }

    @Test
    void aPersonEventSetsTheMajorityAndAnOlderOneChangesNothing() {
        UUID tenant = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        LocalDate minor = TODAY.minusYears(10);
        InboxProcessor inbox = new InboxProcessor(database, Clock.systemUTC());
        EventEnvelope created = person(tenant, "created", person, minor, 1);
        assertThat(inbox.deliver(created, subjects::apply)).isEqualTo(InboxProcessor.Outcome.PROCESSED);
        assertThat(inbox.deliver(created, subjects::apply)).isEqualTo(InboxProcessor.Outcome.DUPLICATE);
        Map<String, Object> row = subject(tenant, person).orElseThrow();
        assertThat(row).containsEntry("status", "minor").containsEntry("birthDate", Optional.of(minor))
                .containsEntry("majorityDate", Optional.of(minor.plusYears(18)));

        LocalDate adult = TODAY.minusYears(30);
        assertThat(deliverSubject(person(tenant, "updated", person, adult, 3))).isTrue();
        assertThat(deliverSubject(person(tenant, "updated", person, minor, 2))).as("a late version").isFalse();
        assertThat(deliverSubject(person(tenant, "updated", person, minor, 3))).as("a repeated version").isFalse();
        assertThat(subject(tenant, person).orElseThrow()).containsEntry("status", "adult")
                .containsEntry("birthDate", Optional.of(adult));

        assertThat(deliverSubject(person(tenant, "deleted", person, adult, 4))).isTrue();
        assertThat(subject(tenant, person).orElseThrow()).containsEntry("status", "unknown")
                .containsEntry("birthDate", Optional.empty()).containsEntry("majorityDate", Optional.empty());
    }

    @Test
    void studentEventsFillTheSubjectsRecordsEvenBeforeItsPersonEvent() {
        UUID tenant = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID otherInstitution = UUID.randomUUID();
        assertThat(deliverSubject(student(tenant, "created", first, person, null, institution, 1))).isTrue();
        assertThat(subject(tenant, person).orElseThrow()).as("known before its person event")
                .containsEntry("status", "unknown").containsEntry("studentIds", List.of(first));

        deliverSubject(student(tenant, "updated", first, person, user, institution, 2));
        deliverSubject(student(tenant, "created", second, person, user, otherInstitution, 1));
        deliverSubject(person(tenant, "created", person, TODAY.minusYears(12), 1));
        Map<String, Object> row = subject(tenant, person).orElseThrow();
        assertThat(row).containsEntry("status", "minor").containsEntry("userIds", List.of(user));
        assertThat(row.get("studentIds")).asInstanceOf(InstanceOfAssertFactories.list(UUID.class))
                .containsExactlyInAnyOrder(first, second);
        assertThat(row.get("institutionIds")).asInstanceOf(InstanceOfAssertFactories.list(UUID.class))
                .containsExactlyInAnyOrder(institution, otherInstitution);

        // a deleted record leaves the arrays, and a late "updated" cannot bring it back
        assertThat(deliverSubject(student(tenant, "deleted", second, person, user, otherInstitution, 2))).isTrue();
        assertThat(deliverSubject(student(tenant, "updated", second, person, user, otherInstitution, 1))).isFalse();
        row = subject(tenant, person).orElseThrow();
        assertThat(row).containsEntry("studentIds", List.of(first)).containsEntry("institutionIds", List.of(institution));

        // a record moved to another person leaves this one
        UUID other = UUID.randomUUID();
        assertThat(deliverSubject(student(tenant, "updated", first, other, user, institution, 3))).isTrue();
        assertThat(subject(tenant, person).orElseThrow()).containsEntry("studentIds", List.of())
                .containsEntry("userIds", List.of());
        assertThat(subject(tenant, other).orElseThrow()).containsEntry("studentIds", List.of(first));
    }

    @Test
    void aGuardianLinkIsActiveUntilDeleted() {
        UUID tenant = UUID.randomUUID();
        UUID link = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID guardian = UUID.randomUUID();
        assertThat(deliverLink(link(tenant, "created", link, student, guardian, 1))).isTrue();
        assertThat(linkStatus(tenant, link)).contains("active");
        assertThat(deliverLink(link(tenant, "deleted", link, student, guardian, 2))).isTrue();
        assertThat(deliverLink(link(tenant, "updated", link, student, guardian, 1))).as("a late version").isFalse();
        assertThat(linkStatus(tenant, link)).contains("inactive");
    }

    @Test
    void oneTenantsEventsNeverShowInAnother() {
        UUID tenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        UUID person = UUID.randomUUID();
        UUID link = UUID.randomUUID();
        deliverSubject(person(tenant, "created", person, TODAY.minusYears(10), 1));
        // the same ids, newer versions, in another tenant
        deliverSubject(person(otherTenant, "updated", person, TODAY.minusYears(40), 7));
        deliverSubject(student(otherTenant, "created", UUID.randomUUID(), person, null, UUID.randomUUID(), 1));
        deliverLink(link(otherTenant, "created", link, person, UUID.randomUUID(), 1));

        assertThat(subject(tenant, person).orElseThrow()).containsEntry("status", "minor")
                .containsEntry("studentIds", List.of());
        assertThat(linkStatus(tenant, link)).isEmpty();
        assertThat(subject(otherTenant, person).orElseThrow()).containsEntry("status", "adult");
    }
}
