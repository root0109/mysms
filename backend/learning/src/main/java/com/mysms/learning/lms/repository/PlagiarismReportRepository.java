package com.mysms.learning.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.PlagiarismReportEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Plain-JDBC access to lms.plagiarism_report: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PlagiarismReportRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("plagiarismAccountId", "r.plagiarism_account_id", ValueType.UUID),
            field("entityAttachmentId", "r.entity_attachment_id", ValueType.UUID),
            field("fileId", "r.file_id", ValueType.UUID),
            field("itemKind", "r.item_kind", ValueType.STRING),
            field("providerSubmissionRef", "r.provider_submission_ref", ValueType.STRING),
            field("providerObjectRef", "r.provider_object_ref", ValueType.STRING),
            field("submittedAt", "r.submitted_at", ValueType.TIMESTAMP),
            field("providerSubmittedAt", "r.provider_submitted_at", ValueType.TIMESTAMP),
            field("status", "r.status", ValueType.STRING),
            field("similarityScore", "r.similarity_score", ValueType.STRING),
            field("reportUrl", "r.report_url", ValueType.STRING),
            field("tolerance", "r.tolerance", ValueType.DECIMAL),
            field("grademarkStatus", "r.grademark_status", ValueType.STRING),
            field("errorCode", "r.error_code", ValueType.STRING),
            field("errorText", "r.error_text", ValueType.STRING),
            field("lastCheckedAt", "r.last_checked_at", ValueType.TIMESTAMP),
            field("receivedAt", "r.received_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PlagiarismReportEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PlagiarismReportEntity::getCreatedAt),
            Map.entry("updateTime", PlagiarismReportEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.plagiarism_account_id, r.entity_attachment_id, r.file_id, r.item_kind,
                   r.provider_submission_ref, r.provider_object_ref, r.submitted_at, r.provider_submitted_at,
                   r.status, r.similarity_score, r.report_url, r.tolerance, r.grademark_status, r.error_code,
                   r.error_text, r.last_checked_at, r.received_at, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM lms.plagiarism_report r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.plagiarism_report
            (id, tenant_id, plagiarism_account_id, entity_attachment_id, file_id, item_kind, provider_submission_ref,
             provider_object_ref, submitted_at, provider_submitted_at, status, similarity_score, report_url,
             tolerance, grademark_status, error_code, error_text, last_checked_at, received_at, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :plagiarismAccountId, :entityAttachmentId, :fileId, :itemKind,
                    :providerSubmissionRef, :providerObjectRef, :submittedAt, :providerSubmittedAt, :status,
                    :similarityScore, :reportUrl, :tolerance, :grademarkStatus, :errorCode, :errorText,
                    :lastCheckedAt, :receivedAt, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, plagiarism_account_id, entity_attachment_id, file_id, item_kind,
                      provider_submission_ref, provider_object_ref, submitted_at, provider_submitted_at, status,
                      similarity_score, report_url, tolerance, grademark_status, error_code, error_text,
                      last_checked_at, received_at, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.plagiarism_report
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.plagiarism_report
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_PLAGIARISM_ACCOUNT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.plagiarism_account WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public PlagiarismReportRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PlagiarismReportEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PlagiarismReportRepository::map).optional();
    }

    public Page<PlagiarismReportEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PlagiarismReportEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PlagiarismReportRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PlagiarismReportEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PlagiarismReportEntity insert(PlagiarismReportEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("plagiarismAccountId", e.getPlagiarismAccountId())
                .param("entityAttachmentId", e.getEntityAttachmentId())
                .param("fileId", e.getFileId())
                .param("itemKind", e.getItemKind())
                .param("providerSubmissionRef", e.getProviderSubmissionRef())
                .param("providerObjectRef", e.getProviderObjectRef())
                .param("submittedAt", e.getSubmittedAt())
                .param("providerSubmittedAt", e.getProviderSubmittedAt())
                .param("status", e.getStatus())
                .param("similarityScore", e.getSimilarityScore())
                .param("reportUrl", e.getReportUrl())
                .param("tolerance", e.getTolerance())
                .param("grademarkStatus", e.getGrademarkStatus())
                .param("errorCode", e.getErrorCode())
                .param("errorText", e.getErrorText())
                .param("lastCheckedAt", e.getLastCheckedAt())
                .param("receivedAt", e.getReceivedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PlagiarismReportRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PlagiarismReportEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PlagiarismReportEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.plagiarism_account row exists (live). */
    public boolean existsPlagiarismAccountId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PLAGIARISM_ACCOUNT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    private static List<Object> sortValues(SortSpec sort, PlagiarismReportEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PlagiarismReportEntity map(ResultSet rs, int row) throws SQLException {
        return PlagiarismReportEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .plagiarismAccountId(rs.getObject("plagiarism_account_id", UUID.class))
                .entityAttachmentId(rs.getObject("entity_attachment_id", UUID.class))
                .fileId(rs.getObject("file_id", UUID.class))
                .itemKind(rs.getString("item_kind"))
                .providerSubmissionRef(rs.getString("provider_submission_ref"))
                .providerObjectRef(rs.getString("provider_object_ref"))
                .submittedAt(rs.getObject("submitted_at", OffsetDateTime.class))
                .providerSubmittedAt(rs.getObject("provider_submitted_at", OffsetDateTime.class))
                .status(rs.getString("status"))
                .similarityScore(rs.getString("similarity_score"))
                .reportUrl(rs.getString("report_url"))
                .tolerance(rs.getBigDecimal("tolerance"))
                .grademarkStatus(rs.getString("grademark_status"))
                .errorCode(rs.getString("error_code"))
                .errorText(rs.getString("error_text"))
                .lastCheckedAt(rs.getObject("last_checked_at", OffsetDateTime.class))
                .receivedAt(rs.getObject("received_at", OffsetDateTime.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
