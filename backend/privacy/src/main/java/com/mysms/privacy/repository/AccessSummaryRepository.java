package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.model.AccessSummarySnapshot;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plain-JDBC read of what an access summary holds: the processing register (a global catalogue, no tenant column)
 * with the caller's tenant's purpose settings and the subject's effective consent. The tenant tables filter the
 * caller's tenant themselves; row-level security is the backstop.
 */
@Repository
public class AccessSummaryRepository {

    /**
     * Unless the subject is an adult, only an activity that processes data of minors (targets_minors); when the
     * subject's roles are known, only an activity whose audiences include one of them; of those, every activity not on
     * the consent basis, and a consent-based one only while the tenant has its purpose switched on and, unless the
     * subject is an adult, never one whose purpose is forbidden for minors.
     */
    private static final String APPLIED = """
            SELECT a.code, a.module, p.code AS purpose_code, a.legal_basis, a.recipients::text AS recipients,
                   a.cross_border, a.retention_policy_code,
                   ARRAY(SELECT c.code
                           FROM privacy.processing_activity_category ac
                           JOIN privacy.data_category c ON c.id = ac.category_id
                          WHERE ac.activity_id = a.id
                          ORDER BY c.code) AS categories,
                   ARRAY(SELECT s.scope
                           FROM privacy.consent_state s
                          WHERE s.tenant_id = :tenantId
                            AND s.subject_person_id = :subjectPersonId AND s.purpose_code = p.code
                            AND s.result = 'allowed') AS allowed_scopes
              FROM privacy.processing_activity a
              JOIN privacy.purpose p ON p.id = a.purpose_id
              LEFT JOIN privacy.tenant_purpose t ON t.tenant_id = :tenantId AND t.purpose_id = p.id
             WHERE (:adult OR a.targets_minors)
               AND (cardinality(CAST(:roles AS varchar[])) = 0 OR a.audiences && CAST(:roles AS varchar[]))
               AND (a.legal_basis <> 'consent' OR (t.enabled AND (:adult OR NOT p.forbidden_for_minors)))
             ORDER BY a.code
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public AccessSummaryRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper json) {
        this.jdbc = database.jdbc();
        this.json = json;
    }

    /**
     * The caller's tenant's register activities that may concern the subject, by activity code; {@code adult} is
     * false for a minor and for a person of unknown age; {@code roles} are the subject's roles at the school (student,
     * guardian, staff), empty when none is known — then no activity is left out for its audience.
     */
    public List<AppliedActivityView> applied(UUID subjectPersonId, boolean adult, List<String> roles) {
        return jdbc.sql(APPLIED).param("subjectPersonId", subjectPersonId).param("adult", adult)
                .param("roles", roles.toArray(String[]::new)).query(this::map).list();
    }

    private AppliedActivityView map(ResultSet rs, int row) throws SQLException {
        return new AppliedActivityView(
                rs.getString("code"),
                rs.getString("module"),
                rs.getString("purpose_code"),
                rs.getString("legal_basis"),
                strings(rs.getArray("categories")),
                List.of(json.readValue(rs.getString("recipients"), AccessSummarySnapshot.Recipient[].class)),
                rs.getBoolean("cross_border"),
                rs.getString("retention_policy_code"),
                strings(rs.getArray("allowed_scopes")));
    }

    private static List<String> strings(Array array) throws SQLException {
        return List.of((String[]) array.getArray());
    }
}
