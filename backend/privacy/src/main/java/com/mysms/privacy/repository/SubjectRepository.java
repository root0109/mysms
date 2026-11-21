package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.model.StudentRecord;
import com.mysms.privacy.model.StudentRecords;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * deletes a row: a deleted person keeps the row with its fields cleared. The row is also the lock of the person's
 * consent changes, and carries the version of their effective consent state.
 */
@Repository
public class SubjectRepository {

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

    private static final String STATUS = """
            SELECT s.status FROM privacy.subject s WHERE s.tenant_id = :tenantId AND s.person_id = :personId
            """;

    /** The subject's row is the lock every consent change of the subject takes first (then the evidence chain). */
    private static final String LOCK_STATUS = """
            SELECT s.status
              FROM privacy.subject s
             WHERE s.tenant_id = :tenantId AND s.person_id = :personId
               FOR UPDATE
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

    /** Makes sure the person has a row (status unknown when it is new). */
    public void ensure(UUID personId, OffsetDateTime now) {
        jdbc.sql(ENSURE).param("personId", personId).param("now", now).update();
    }

    /** The person's status (minor, adult, unknown); empty while no event has named the person. */
    public Optional<String> status(UUID personId) {
        return jdbc.sql(STATUS).param("personId", personId).query(String.class).optional();
    }

    /**
     * The person's status with their row locked until the transaction ends; the row is created (status unknown) when
     * no event has named the person yet, so a consent can be recorded before the person's events arrive.
     */
    public String lockStatus(UUID personId, OffsetDateTime now) {
        ensure(personId, now);
        return jdbc.sql(LOCK_STATUS).param("personId", personId).query(String.class).single();
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
