package com.mysms.finance.repository;

import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * finance.student_record: the person, login account and institution of each student record, kept from sis's student
 * events. A row changes only to a newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class StudentRecordRepository {

    private static final String APPLY = """
            INSERT INTO finance.student_record (tenant_id, student_id, person_id, user_id, institution_id,
                                                source_version, synced_at)
            VALUES (:tenantId, :studentId, :personId, :userId, :institutionId, :version, :now)
            ON CONFLICT (tenant_id, student_id) DO UPDATE
               SET person_id = EXCLUDED.person_id, user_id = EXCLUDED.user_id,
                   institution_id = EXCLUDED.institution_id, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE finance.student_record.source_version < EXCLUDED.source_version
            """;
    private static final String STUDENT_IN = """
            SELECT EXISTS (SELECT 1 FROM finance.student_record r
                            WHERE r.tenant_id = :tenantId AND r.student_id = :studentId
                              AND r.institution_id = :institutionId)
            """;
    private static final String ACCOUNT_IN = """
            SELECT EXISTS (SELECT 1 FROM finance.student_record r
                            WHERE r.tenant_id = :tenantId AND r.user_id = :userId
                              AND r.institution_id = :institutionId)
            """;

    private final JdbcClient jdbc;

    public StudentRecordRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /**
     * Applies one event of a student record ({@code userId} null while it has no login account); whether the stored
     * entry changed (false for an older or repeated one).
     */
    public boolean apply(UUID studentId, UUID personId, UUID userId, UUID institutionId, int version,
            OffsetDateTime now) {
        return jdbc.sql(APPLY).param("studentId", studentId).param("personId", personId).param("userId", userId)
                .param("institutionId", institutionId).param("version", version).param("now", now).update() == 1;
    }

    /** Whether finance knows the student as one of the institution's. */
    public boolean studentIn(UUID studentId, UUID institutionId) {
        return Boolean.TRUE.equals(jdbc.sql(STUDENT_IN).param("studentId", studentId)
                .param("institutionId", institutionId).query(Boolean.class).single());
    }

    /** Whether the account is the login of one of the institution's students. */
    public boolean accountIn(UUID userId, UUID institutionId) {
        return Boolean.TRUE.equals(jdbc.sql(ACCOUNT_IN).param("userId", userId)
                .param("institutionId", institutionId).query(Boolean.class).single());
    }
}
