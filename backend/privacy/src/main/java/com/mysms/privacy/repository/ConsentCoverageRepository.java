package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC counts of the effective consent state (privacy.consent_state) over the tenant's data principals
 * (privacy.subject), optionally those with a student record of one institution: counts only, never a person id.
 */
@Repository
public class ConsentCoverageRepository {

    /** How many subjects have the stored answer with this reason at this width. */
    public record Count(String scope, String reason, long subjects) {
    }

    private static final String SUBJECTS = """
            SELECT count(*)
              FROM privacy.subject p
             WHERE p.tenant_id = :tenantId
            """;
    private static final String SUBJECTS_OF_INSTITUTION = SUBJECTS + " AND :institutionId = ANY(p.institution_ids)";
    private static final String COUNTS = """
            SELECT s.scope, s.reason, count(*) AS subjects
              FROM privacy.consent_state s
             WHERE s.tenant_id = :tenantId
               AND s.purpose_code = :purposeCode
             GROUP BY s.scope, s.reason
            """;
    private static final String COUNTS_OF_INSTITUTION = """
            SELECT s.scope, s.reason, count(*) AS subjects
              FROM privacy.consent_state s
              JOIN privacy.subject p ON p.tenant_id = :tenantId AND p.person_id = s.subject_person_id
                   AND :institutionId = ANY(p.institution_ids)
             WHERE s.tenant_id = :tenantId
               AND s.purpose_code = :purposeCode
             GROUP BY s.scope, s.reason
            """;

    private final JdbcClient jdbc;

    public ConsentCoverageRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** How many data principals privacy knows in the tenant (null institution) or with a student record of it. */
    public long subjects(UUID institutionId) {
        return institutionId == null ? jdbc.sql(SUBJECTS).query(Long.class).single()
                : jdbc.sql(SUBJECTS_OF_INSTITUTION).param("institutionId", institutionId).query(Long.class).single();
    }

    /** The stored answers of the purpose counted by width and reason, over the same subjects as {@link #subjects}. */
    public List<Count> counts(String purposeCode, UUID institutionId) {
        JdbcClient.StatementSpec statement = institutionId == null
                ? jdbc.sql(COUNTS).param("purposeCode", purposeCode)
                : jdbc.sql(COUNTS_OF_INSTITUTION).param("purposeCode", purposeCode)
                        .param("institutionId", institutionId);
        return statement.query((rs, row) -> new Count(rs.getString("scope"), rs.getString("reason"),
                rs.getLong("subjects"))).list();
    }
}
