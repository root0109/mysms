package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The reads behind consent decisions: a processing activity with its purpose and the caller's tenant's setting of it,
 * the stored answers of many subjects at one width, and the subjects an answer allows. Every tenant table is filtered
 * by the caller's tenant; row-level security is the backstop.
 */
@Repository
public class ConsentDecisionRepository {

    /** An activity as a decision needs it: basis, purpose rules and the tenant's setting (switched off when unset). */
    public record Activity(String code, String legalBasis, String purposeCode, String scopeKind,
                           boolean forbiddenForMinors, boolean enabled, String maxScope, UUID noticeId) {
    }

    /** One subject asked about: their status and state version (unknown, 0 while privacy has no row) and the answer. */
    public record Answer(UUID subjectPersonId, String status, long stateVersion, String result, String reason) {
    }

    /** Subjects of an allowed answer are listed in person order only. */
    public static final Map<String, FieldSpec> SORT_FIELDS = Map.of("subjectPersonId",
            new FieldSpec("subjectPersonId", "s.subject_person_id", ValueType.UUID));
    public static final String DEFAULT_ORDER = "subjectPersonId";
    public static final String ID_COLUMN = "s.subject_person_id";

    private static final String ACTIVITY = """
            SELECT a.code, a.legal_basis, p.code AS purpose_code, p.scope_kind, p.forbidden_for_minors,
                   COALESCE(t.enabled, false) AS enabled, t.max_scope, t.notice_id
              FROM privacy.processing_activity a
              JOIN privacy.purpose p ON p.id = a.purpose_id
              LEFT JOIN privacy.tenant_purpose t ON t.purpose_id = p.id AND t.tenant_id = :tenantId
             WHERE a.code = :code
            """;
    private static final String ANSWERS = """
            SELECT ids.id AS subject_person_id, COALESCE(sub.status, 'unknown') AS status,
                   COALESCE(sub.state_version, 0) AS state_version, s.result, s.reason
              FROM unnest(CAST(:ids AS uuid[])) AS ids (id)
              LEFT JOIN privacy.subject sub ON sub.tenant_id = :tenantId AND sub.person_id = ids.id
              LEFT JOIN privacy.consent_state s ON s.tenant_id = :tenantId AND s.subject_person_id = ids.id
                   AND s.purpose_code = :purposeCode AND s.scope = :scope
            """;
    private static final String ACTIVE_NOTICE_VERSION = """
            SELECT v.id
              FROM privacy.notice_version v
             WHERE v.tenant_id = :tenantId
               AND v.notice_id = :noticeId AND v.status = 'active'
            """;
    private static final String ALLOWED = """
            SELECT s.subject_person_id
              FROM privacy.consent_state s
              JOIN privacy.subject sub ON sub.tenant_id = :tenantId AND sub.person_id = s.subject_person_id
             WHERE s.tenant_id = :tenantId
               AND s.purpose_code = :purposeCode AND s.scope = :scope AND s.result = 'allowed'
            """;
    private static final String IN_INSTITUTIONS = "sub.institution_ids && CAST(:institutionIds AS uuid[])";

    private final JdbcClient jdbc;

    public ConsentDecisionRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The activity of the catalogue with the code, as the caller's tenant has set its purpose. */
    public Optional<Activity> activity(String code) {
        return jdbc.sql(ACTIVITY).param("code", code).query((rs, row) -> new Activity(rs.getString("code"),
                rs.getString("legal_basis"), rs.getString("purpose_code"), rs.getString("scope_kind"),
                rs.getBoolean("forbidden_for_minors"), rs.getBoolean("enabled"), rs.getString("max_scope"),
                rs.getObject("notice_id", UUID.class))).optional();
    }

    /** Each subject's stored answer to the purpose at the width (absent when never computed), in the order asked. */
    public List<Answer> answers(List<UUID> subjectPersonIds, String purposeCode, String scope) {
        return jdbc.sql(ANSWERS).param("ids", subjectPersonIds.toArray(UUID[]::new)).param("purposeCode", purposeCode)
                .param("scope", scope).query((rs, row) -> new Answer(rs.getObject("subject_person_id", UUID.class),
                        rs.getString("status"), rs.getLong("state_version"), rs.getString("result"),
                        rs.getString("reason"))).list();
    }

    /** The active version of the tenant's notice, if one is published. */
    public Optional<UUID> activeNoticeVersion(UUID noticeId) {
        return jdbc.sql(ACTIVE_NOTICE_VERSION).param("noticeId", noticeId).query(UUID.class).optional();
    }

    /** The subjects whose answer to the purpose at the width allows, optionally only of the given institutions. */
    public Page<UUID> allowed(String purposeCode, String scope, List<UUID> institutionIds, PageRequest page) {
        SqlFragment where = institutionIds.isEmpty() ? SqlFragment.TRUE
                : new SqlFragment(IN_INSTITUTIONS, Map.of("institutionIds", institutionIds.toArray(UUID[]::new)));
        where = where.and(page.keysetPredicate());
        String sql = ALLOWED + " AND " + where.sql() + " ORDER BY " + page.orderByClause() + page.limitClause();
        List<UUID> rows = jdbc.sql(sql).param("purposeCode", purposeCode).param("scope", scope)
                .params(where.params()).params(page.limitParams()).query(UUID.class).list();
        return page.toPage(rows, id -> List.<Object>of(id), id -> id);
    }
}
