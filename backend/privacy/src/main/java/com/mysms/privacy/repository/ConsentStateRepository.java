package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.model.ConsentReason;
import com.mysms.privacy.model.ConsentScope;
import com.mysms.privacy.model.ScopeState;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * privacy.consent_state: the effective consent answer per subject, purpose and width, written only by the
 * recompute of the subject and purpose (one upsert per width, all carrying the subject's new state version). Every
 * statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ConsentStateRepository {

    /** One allowed width of a subject's purpose (the consent centre's "currently shared with"). */
    public record Allowed(UUID subjectPersonId, String purposeCode, ConsentScope scope) {
    }

    private static final String FIND = """
            SELECT s.scope, s.result, s.reason
              FROM privacy.consent_state s
             WHERE s.tenant_id = :tenantId
               AND s.subject_person_id = :subjectPersonId AND s.purpose_code = :purposeCode
             ORDER BY s.scope
            """;
    private static final String ALLOWED = """
            SELECT s.subject_person_id, s.purpose_code, s.scope
              FROM privacy.consent_state s
             WHERE s.tenant_id = :tenantId
               AND s.subject_person_id = ANY(CAST(:subjectPersonIds AS uuid[])) AND s.result = 'allowed'
            """;
    private static final String WRITE = """
            INSERT INTO privacy.consent_state (tenant_id, subject_person_id, purpose_code, scope, result, reason,
                                               state_version, computed_at)
            VALUES (:tenantId, :subjectPersonId, :purposeCode, :scope, :result, :reason, :stateVersion, :computedAt)
            ON CONFLICT (tenant_id, subject_person_id, purpose_code, scope) DO UPDATE
               SET result = EXCLUDED.result, reason = EXCLUDED.reason, state_version = EXCLUDED.state_version,
                   computed_at = EXCLUDED.computed_at
            """;

    private final JdbcClient jdbc;

    public ConsentStateRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The stored answers of the subject's purpose, one per width (empty when never computed). */
    public List<ScopeState> find(UUID subjectPersonId, String purposeCode) {
        return jdbc.sql(FIND).param("subjectPersonId", subjectPersonId).param("purposeCode", purposeCode)
                .query((rs, row) -> new ScopeState(ConsentScope.of(rs.getString("scope")),
                        "allowed".equals(rs.getString("result")), ConsentReason.valueOf(rs.getString("reason"))))
                .list();
    }

    /** Every allowed width of the given subjects' purposes. */
    public List<Allowed> allowed(List<UUID> subjectPersonIds) {
        return jdbc.sql(ALLOWED).param("subjectPersonIds", subjectPersonIds.toArray(UUID[]::new))
                .query((rs, row) -> new Allowed(rs.getObject("subject_person_id", UUID.class),
                        rs.getString("purpose_code"), ConsentScope.of(rs.getString("scope")))).list();
    }

    /** Writes the subject's purpose's answers, every one with the subject's new state version. */
    public void write(UUID subjectPersonId, String purposeCode, List<ScopeState> states, long stateVersion,
            OffsetDateTime computedAt) {
        for (ScopeState state : states) {
            jdbc.sql(WRITE).param("subjectPersonId", subjectPersonId).param("purposeCode", purposeCode)
                    .param("scope", state.scope().value()).param("result", state.allowed() ? "allowed" : "denied")
                    .param("reason", state.reason().name()).param("stateVersion", stateVersion)
                    .param("computedAt", computedAt).update();
        }
    }
}
