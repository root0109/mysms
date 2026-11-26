package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain-JDBC access of the plagiarism workflows: the account used, the viewer's provider identity and the report. */
@Repository
public class PlagiarismWorkflowRepository {

    /** A plagiarism account with its encrypted key. */
    public record AccountRow(UUID id, String provider, String accountRef, String apiUrl, String secretEnc) {
    }

    /** A report as the workflows need it, with the record its file was checked for and who sent it. */
    public record ReportRow(UUID id, UUID accountId, String submissionRef, String status, int rowVersion,
                            String entityType, UUID entityId, UUID createdBy) {
    }

    private static final String ACCOUNT = """
            SELECT id, provider, account_ref, api_url, shared_secret_enc FROM lms.plagiarism_account
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String DEFAULT_ACCOUNT = """
            SELECT id, provider, account_ref, api_url, shared_secret_enc FROM lms.plagiarism_account
             WHERE tenant_id = :tenantId AND is_default = true AND deleted_at IS NULL ORDER BY created_at, id LIMIT 1
            """;
    private static final String USER_REF = """
            SELECT provider_user_ref FROM lms.plagiarism_user_link
             WHERE tenant_id = :tenantId AND plagiarism_account_id = :accountId AND user_id = :userId
               AND deleted_at IS NULL ORDER BY created_at, id LIMIT 1
            """;
    private static final String INSERT = """
            INSERT INTO lms.plagiarism_report (id, tenant_id, plagiarism_account_id, entity_attachment_id, file_id,
                   item_kind, entity_type, entity_id, provider_submission_ref, submitted_at, status, created_at,
                   created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :accountId, :attachmentId, :fileId, :itemKind, :entityType, :entityId, :ref, :now,
                    'uploaded', :now, :actor, :now, :actor, 1)
            """;
    private static final String REPORT = """
            SELECT id, plagiarism_account_id, provider_submission_ref, status, row_version, entity_type, entity_id,
                   created_by FROM lms.plagiarism_report
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String REPORT_BY_REF = """
            SELECT id, plagiarism_account_id, provider_submission_ref, status, row_version, entity_type, entity_id,
                   created_by FROM lms.plagiarism_report
             WHERE tenant_id = :tenantId AND provider_submission_ref = :ref AND deleted_at IS NULL
            """;
    private static final String REPORT_BY_REF_FOR_UPDATE = REPORT_BY_REF + " FOR UPDATE";
    private static final String RECORD_EVENT = """
            UPDATE lms.plagiarism_report
               SET status = :status, similarity_score = COALESCE(:similarity, similarity_score), error_code = :errorCode,
                   provider_submitted_at = COALESCE(provider_submitted_at, :now), received_at = :now,
                   last_checked_at = :now, updated_at = :now, updated_by = NULL, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public PlagiarismWorkflowRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AccountRow> account(UUID id) {
        return jdbc.sql(ACCOUNT).param("id", id).query((rs, n) -> account(rs)).optional();
    }

    public Optional<AccountRow> defaultAccount() {
        return jdbc.sql(DEFAULT_ACCOUNT).query((rs, n) -> account(rs)).optional();
    }

    public Optional<String> userRef(UUID accountId, UUID userId) {
        return jdbc.sql(USER_REF).param("accountId", accountId).param("userId", userId).query(String.class)
                .optional();
    }

    public void insert(UUID id, UUID accountId, UUID attachmentId, UUID fileId, String itemKind, String entityType,
            UUID entityId, String ref, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT).param("id", id).param("accountId", accountId).param("attachmentId", attachmentId)
                .param("fileId", fileId).param("itemKind", itemKind).param("entityType", entityType)
                .param("entityId", entityId).param("ref", ref).param("now", now)
                .param("actor", actor).update();
    }

    public Optional<ReportRow> report(UUID id) {
        return jdbc.sql(REPORT).param("id", id).query((rs, n) -> report(rs)).optional();
    }

    public Optional<ReportRow> byRef(String ref) {
        return jdbc.sql(REPORT_BY_REF).param("ref", ref).query((rs, n) -> report(rs)).optional();
    }

    /** The report of a provider submission, locked. */
    public Optional<ReportRow> lockByRef(String ref) {
        return jdbc.sql(REPORT_BY_REF_FOR_UPDATE).param("ref", ref).query((rs, n) -> report(rs)).optional();
    }

    public int recordEvent(UUID id, int expected, String status, String similarity, String errorCode,
            OffsetDateTime now) {
        return jdbc.sql(RECORD_EVENT).param("id", id).param("expected", expected).param("status", status)
                .param("similarity", similarity).param("errorCode", errorCode).param("now", now).update();
    }

    private static AccountRow account(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AccountRow(rs.getObject("id", UUID.class), rs.getString("provider"), rs.getString("account_ref"),
                rs.getString("api_url"), rs.getString("shared_secret_enc"));
    }

    private static ReportRow report(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReportRow(rs.getObject("id", UUID.class), rs.getObject("plagiarism_account_id", UUID.class),
                rs.getString("provider_submission_ref"), rs.getString("status"), rs.getInt("row_version"),
                rs.getString("entity_type"), rs.getObject("entity_id", UUID.class),
                rs.getObject("created_by", UUID.class));
    }
}
