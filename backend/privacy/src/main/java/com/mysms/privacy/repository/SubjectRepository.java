package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.model.Majority;
import com.mysms.privacy.model.StudentRecord;
import com.mysms.privacy.model.StudentRecords;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * privacy.subject: each person of a tenant as privacy knows them, kept from people's and sis's events. The person's
 * fields change only to a newer people.person row version; the student records are jsonb (written through
 * {@code CAST(:x AS jsonb)}, read back as JSON text) and their derived arrays are written with them. No statement
 * deletes a row: a deleted person keeps the row with its fields cleared. Staff records are kept from hr.staff events in
 * the same shape as student records. The row is also the lock of the person's
 * consent changes, and carries the version of their effective consent state.
 */
@Repository
public class SubjectRepository {

    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_GUARDIAN = "guardian";
    public static final String ROLE_STAFF = "staff";

    private static final TypeReference<Map<UUID, StudentRecord>> RECORDS = new TypeReference<>() {
    };

    private static final String APPLY_PERSON = """
            INSERT INTO privacy.subject (tenant_id, person_id, status, birth_date, majority_date, person_row_version,
                                         updated_at)
            VALUES (:tenantId, :personId, :status, :birthDate, :majorityDate, :version, :now)
            ON CONFLICT (tenant_id, person_id) DO UPDATE
               SET status = EXCLUDED.status, birth_date = EXCLUDED.birth_date,
                   majority_date = EXCLUDED.majority_date, person_row_version = EXCLUDED.person_row_version,
                   updated_at = EXCLUDED.updated_at
             WHERE privacy.subject.person_row_version < EXCLUDED.person_row_version
            """;

    /** An erased principal: the birth date and majority are forgotten, the status becomes unknown. */
    private static final String ANONYMISE = """
            UPDATE privacy.subject
               SET status = 'unknown', birth_date = NULL, majority_date = NULL, updated_at = :now
             WHERE tenant_id = :tenantId AND person_id = :personId
            """;

    /** A subject named by a student event before its person event: unknown until the person event arrives. */
    private static final String ENSURE = """
            INSERT INTO privacy.subject (tenant_id, person_id, updated_at)
            VALUES (:tenantId, :personId, :now)
            ON CONFLICT (tenant_id, person_id) DO NOTHING
            """;

    /** The other subjects with an entry (live or a tombstone) of the student record, in a fixed lock order. */
    private static final String HOLDERS = """
            SELECT s.person_id
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId
               AND s.person_id <> :personId AND s.student_records @> CAST(:probe AS jsonb)
             ORDER BY s.person_id
            """;

    private static final String LOCK_STUDENT_RECORDS = """
            SELECT s.student_records::text
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId AND s.person_id = :personId
               FOR UPDATE
            """;

    private static final String WRITE_STUDENT_RECORDS = """
            UPDATE privacy.subject
               SET student_records = CAST(:records AS jsonb), student_ids = CAST(:studentIds AS uuid[]),
                   user_ids = CAST(:userIds AS uuid[]), institution_ids = CAST(:institutionIds AS uuid[]),
                   updated_at = :now
             WHERE tenant_id = :tenantId AND person_id = :personId
            """;

    /** The other subjects with an entry (live or a tombstone) of the staff record, in a fixed lock order. */
    private static final String STAFF_HOLDERS = """
            SELECT s.person_id
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId
               AND s.person_id <> :personId AND s.staff_records @> CAST(:probe AS jsonb)
             ORDER BY s.person_id
            """;

    private static final String LOCK_STAFF_RECORDS = """
            SELECT s.staff_records::text
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId AND s.person_id = :personId
               FOR UPDATE
            """;

    private static final String WRITE_STAFF_RECORDS = """
            UPDATE privacy.subject
               SET staff_records = CAST(:records AS jsonb), updated_at = :now
             WHERE tenant_id = :tenantId AND person_id = :personId
            """;

    /**
     * The person's roles at the school as privacy knows them: student (a live student record), guardian (an active
     * link as a student's guardian) and staff (a live staff record); no row = no role known.
     */
    private static final String ROLES = """
            SELECT cardinality(s.student_ids) > 0 AS student,
                   EXISTS (SELECT 1
                             FROM privacy.guardian_link g
                            WHERE g.tenant_id = :tenantId
                              AND g.guardian_person_id = :personId AND g.status = 'active') AS guardian,
                   EXISTS (SELECT 1
                             FROM jsonb_each(s.staff_records) r
                            WHERE (r.value ->> 'live')::boolean) AS staff
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId AND s.person_id = :personId
            """;

    /**
     * The login accounts of the persons' student and staff records, tombstones included (privacy.subject.user_ids holds
     * only the live ones, so it adds nothing here).
     */
    private static final String RECORD_ACCOUNTS = """
            SELECT CAST(r.value ->> 'userId' AS uuid)
              FROM privacy.subject s
             CROSS JOIN LATERAL jsonb_each(s.student_records || s.staff_records) r
             WHERE s.tenant_id = :tenantId AND s.person_id = ANY(CAST(:personIds AS uuid[]))
               AND r.value ->> 'userId' IS NOT NULL
            """;

    /** The login accounts of the persons' guardian links. */
    private static final String GUARDIAN_ACCOUNTS = """
            SELECT g.guardian_user_id
              FROM privacy.guardian_link g
             WHERE g.tenant_id = :tenantId
               AND g.guardian_person_id = ANY(CAST(:personIds AS uuid[])) AND g.guardian_user_id IS NOT NULL
            """;
    private static final String MAJORITY = """
            SELECT s.status, s.majority_date
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId AND s.person_id = :personId
            """;

    /** The subject's row is the lock every consent change of the subject takes first (then the evidence chain). */
    private static final String LOCK_STATUS = """
            SELECT s.status
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId AND s.person_id = :personId
               FOR UPDATE
            """;

    private static final String COMING_OF_AGE = """
            SELECT s.person_id
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId
               AND s.status = 'minor' AND s.majority_date <= :today
             ORDER BY s.person_id
             LIMIT :limit
            """;
    private static final String COME_OF_AGE = """
            UPDATE privacy.subject
               SET status = 'adult', updated_at = :now
             WHERE tenant_id = :tenantId AND person_id = :personId AND status = 'minor' AND majority_date <= :today
            RETURNING majority_date
            """;
    private static final String STATE_VERSION = """
            SELECT s.state_version
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId AND s.person_id = :personId
            """;
    private static final String NEXT_STATE_VERSION = """
            UPDATE privacy.subject
               SET state_version = state_version + 1
             WHERE tenant_id = :tenantId AND person_id = :personId
            RETURNING state_version
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public SubjectRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database, JsonMapper json) {
        this.jdbc = database.jdbc();
        this.json = json;
    }

    /**
     * Sets the person's birth date and majority from one person event; whether the row changed (false for an older or
     * repeated version).
     */
    public boolean applyPerson(UUID personId, String status, LocalDate birthDate, LocalDate majorityDate, int version,
            OffsetDateTime now) {
        return jdbc.sql(APPLY_PERSON).param("personId", personId).param("status", status)
                .param("birthDate", birthDate).param("majorityDate", majorityDate).param("version", version)
                .param("now", now).update() == 1;
    }

    /**
     * Forgets the erased person's birth date and majority (status unknown); a later person event of a newer row version
     * would set them again, which the person's own erasure in people prevents.
     */
    public void anonymise(UUID personId, OffsetDateTime now) {
        jdbc.sql(ANONYMISE).param("personId", personId).param("now", now).update();
    }

    /** Makes sure the person has a row (status unknown when it is new). */
    public void ensure(UUID personId, OffsetDateTime now) {
        jdbc.sql(ENSURE).param("personId", personId).param("now", now).update();
    }

    /**
     * The person's status (minor, adult, unknown) with the day they come of age (null without a birth date); empty
     * while no event has named the person.
     */
    public Optional<Majority> majority(UUID personId) {
        return jdbc.sql(MAJORITY).param("personId", personId)
                .query((rs, row) -> new Majority(rs.getString("status"), rs.getObject("majority_date", LocalDate.class)))
                .optional();
    }

    /**
     * The person's status with their row locked until the transaction ends; the row is created (status unknown) when
     * no event has named the person yet, so a consent can be recorded before the person's events arrive.
     */
    public String lockStatus(UUID personId, OffsetDateTime now) {
        ensure(personId, now);
        return jdbc.sql(LOCK_STATUS).param("personId", personId).query(String.class).single();
    }

    /** Up to {@code limit} minors whose majority date has come by {@code today}, in person order. */
    public List<UUID> comingOfAge(LocalDate today, int limit) {
        return jdbc.sql(COMING_OF_AGE).param("today", today).param("limit", limit).query(UUID.class).list();
    }

    /**
     * Makes the minor an adult when the majority date has come by {@code today} (their row must be locked); answers the
     * majority date, empty when the person is not such a minor (any more).
     */
    public Optional<LocalDate> comeOfAge(UUID personId, LocalDate today, OffsetDateTime now) {
        return jdbc.sql(COME_OF_AGE).param("personId", personId).param("today", today).param("now", now)
                .query(LocalDate.class).optional();
    }

    /** The person's consent state version (0 while no answer was ever written). */
    public long stateVersion(UUID personId) {
        return jdbc.sql(STATE_VERSION).param("personId", personId).query(Long.class).single();
    }

    /** Increments the person's consent state version (their row must be locked) and answers the new value. */
    public long nextStateVersion(UUID personId) {
        return jdbc.sql(NEXT_STATE_VERSION).param("personId", personId).query(Long.class).single();
    }

    /** The subjects other than this person's that hold an entry of the student record. */
    public List<UUID> holdersOf(UUID studentId, UUID personId) {
        String probe = json.writeValueAsString(Map.of(studentId.toString(), Map.of()));
        return jdbc.sql(HOLDERS).param("personId", personId).param("probe", probe).query(UUID.class).list();
    }

    /** The subjects other than this person's that hold an entry of the staff record. */
    public List<UUID> staffHoldersOf(UUID staffId, UUID personId) {
        String probe = json.writeValueAsString(Map.of(staffId.toString(), Map.of()));
        return jdbc.sql(STAFF_HOLDERS).param("personId", personId).param("probe", probe).query(UUID.class).list();
    }

    /** The person's staff records, its row locked until the transaction ends (same shape as student records). */
    public StudentRecords lockStaffRecords(UUID personId) {
        String records = jdbc.sql(LOCK_STAFF_RECORDS).param("personId", personId).query(String.class).single();
        return StudentRecords.of(json.readValue(records, RECORDS));
    }

    /** Writes the person's staff records. */
    public void writeStaffRecords(UUID personId, StudentRecords records, OffsetDateTime now) {
        jdbc.sql(WRITE_STAFF_RECORDS).param("personId", personId)
                .param("records", json.writeValueAsString(records.records())).param("now", now).update();
    }

    /** The person's roles at the school (student, guardian, staff, in that order); empty when none is known. */
    public List<String> roles(UUID personId) {
        return jdbc.sql(ROLES).param("personId", personId).query((rs, row) -> {
            List<String> roles = new ArrayList<>();
            if (rs.getBoolean("student")) {
                roles.add(ROLE_STUDENT);
            }
            if (rs.getBoolean("guardian")) {
                roles.add(ROLE_GUARDIAN);
            }
            if (rs.getBoolean("staff")) {
                roles.add(ROLE_STAFF);
            }
            return List.copyOf(roles);
        }).optional().orElse(List.of());
    }

    /** Every login account privacy knows for the persons (see {@link #RECORD_ACCOUNTS}, {@link #GUARDIAN_ACCOUNTS}). */
    public Set<UUID> accountsOf(Collection<UUID> personIds) {
        UUID[] persons = personIds.toArray(UUID[]::new);
        Set<UUID> accounts = new HashSet<>(jdbc.sql(RECORD_ACCOUNTS).param("personIds", persons).query(UUID.class)
                .list());
        accounts.addAll(jdbc.sql(GUARDIAN_ACCOUNTS).param("personIds", persons).query(UUID.class).list());
        return Set.copyOf(accounts);
    }

    /** The person's student records, its row locked until the transaction ends. */
    public StudentRecords lockStudentRecords(UUID personId) {
        String records = jdbc.sql(LOCK_STUDENT_RECORDS).param("personId", personId).query(String.class).single();
        return StudentRecords.of(json.readValue(records, RECORDS));
    }

    /** Writes the person's student records and the arrays derived from them. */
    public void writeStudentRecords(UUID personId, StudentRecords records, OffsetDateTime now) {
        jdbc.sql(WRITE_STUDENT_RECORDS).param("personId", personId)
                .param("records", json.writeValueAsString(records.records()))
                .param("studentIds", records.studentIds().toArray(UUID[]::new))
                .param("userIds", records.userIds().toArray(UUID[]::new))
                .param("institutionIds", records.institutionIds().toArray(UUID[]::new))
                .param("now", now).update();
    }
}
