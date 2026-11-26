package com.mysms.learning.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.PlagiarismCohortLinkEntity;
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
 * Plain-JDBC access to lms.plagiarism_cohort_link: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PlagiarismCohortLinkRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("plagiarismAccountId", "r.plagiarism_account_id", ValueType.UUID),
            field("accountRef", "r.account_ref", ValueType.STRING),
            field("providerClassRef", "r.provider_class_ref", ValueType.STRING),
            field("providerClassTitle", "r.provider_class_title", ValueType.STRING),
            field("providerClassEndAt", "r.provider_class_end_at", ValueType.TIMESTAMP),
            field("providerAssignmentRef", "r.provider_assignment_ref", ValueType.STRING),
            field("providerAssignmentTitle", "r.provider_assignment_title", ValueType.STRING),
            field("providerAssignmentStartAt", "r.provider_assignment_start_at", ValueType.TIMESTAMP),
            field("providerAssignmentDueAt", "r.provider_assignment_due_at", ValueType.TIMESTAMP),
            field("providerAssignmentInstructions", "r.provider_assignment_instructions", ValueType.STRING),
            field("studentReportAccess", "r.student_report_access", ValueType.BOOLEAN),
            field("teacherEmail", "r.teacher_email", ValueType.STRING),
            field("teacherFirstName", "r.teacher_first_name", ValueType.STRING),
            field("teacherLastName", "r.teacher_last_name", ValueType.STRING),
            field("classCreated", "r.class_created", ValueType.BOOLEAN),
            field("assignmentCreated", "r.assignment_created", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PlagiarismCohortLinkEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PlagiarismCohortLinkEntity::getCreatedAt),
            Map.entry("updateTime", PlagiarismCohortLinkEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.cohort_id, r.plagiarism_account_id, r.account_ref, r.provider_class_ref,
                   r.provider_class_title, r.provider_class_end_at, r.provider_assignment_ref,
                   r.provider_assignment_title, r.provider_assignment_start_at, r.provider_assignment_due_at,
                   r.provider_assignment_instructions, r.student_report_access, r.teacher_email, r.teacher_first_name,
                   r.teacher_last_name, r.class_created, r.assignment_created, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.plagiarism_cohort_link r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.plagiarism_cohort_link
            (id, tenant_id, cohort_id, plagiarism_account_id, account_ref, provider_class_ref, provider_class_title,
             provider_class_end_at, provider_assignment_ref, provider_assignment_title, provider_assignment_start_at,
             provider_assignment_due_at, provider_assignment_instructions, student_report_access, teacher_email,
             teacher_first_name, teacher_last_name, class_created, assignment_created, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :cohortId, :plagiarismAccountId, :accountRef, :providerClassRef,
                    :providerClassTitle, :providerClassEndAt, :providerAssignmentRef, :providerAssignmentTitle,
                    :providerAssignmentStartAt, :providerAssignmentDueAt, :providerAssignmentInstructions,
                    COALESCE(:studentReportAccess, false), :teacherEmail, :teacherFirstName, :teacherLastName,
                    COALESCE(:classCreated, false), COALESCE(:assignmentCreated, false), :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, cohort_id, plagiarism_account_id, account_ref, provider_class_ref,
                      provider_class_title, provider_class_end_at, provider_assignment_ref, provider_assignment_title,
                      provider_assignment_start_at, provider_assignment_due_at, provider_assignment_instructions,
                      student_report_access, teacher_email, teacher_first_name, teacher_last_name, class_created,
                      assignment_created, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.plagiarism_cohort_link
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.plagiarism_cohort_link
               SET plagiarism_account_id = :plagiarismAccountId,
                   account_ref = :accountRef,
                   provider_class_ref = :providerClassRef,
                   provider_class_title = :providerClassTitle,
                   provider_class_end_at = :providerClassEndAt,
                   provider_assignment_ref = :providerAssignmentRef,
                   provider_assignment_title = :providerAssignmentTitle,
                   provider_assignment_start_at = :providerAssignmentStartAt,
                   provider_assignment_due_at = :providerAssignmentDueAt,
                   provider_assignment_instructions = :providerAssignmentInstructions,
                   student_report_access = :studentReportAccess,
                   teacher_email = :teacherEmail,
                   teacher_first_name = :teacherFirstName,
                   teacher_last_name = :teacherLastName,
                   class_created = :classCreated,
                   assignment_created = :assignmentCreated,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PLAGIARISM_COHORT_LINK_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.plagiarism_cohort_link
                            WHERE tenant_id = :tenantId
                              AND cohort_id = :cohortId AND plagiarism_account_id = :plagiarismAccountId AND id <> :id
                                  AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PLAGIARISM_ACCOUNT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.plagiarism_account WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public PlagiarismCohortLinkRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PlagiarismCohortLinkEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PlagiarismCohortLinkRepository::map).optional();
    }

    public Page<PlagiarismCohortLinkEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PlagiarismCohortLinkEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PlagiarismCohortLinkRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PlagiarismCohortLinkEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PlagiarismCohortLinkEntity insert(PlagiarismCohortLinkEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("cohortId", e.getCohortId())
                .param("plagiarismAccountId", e.getPlagiarismAccountId())
                .param("accountRef", e.getAccountRef())
                .param("providerClassRef", e.getProviderClassRef())
                .param("providerClassTitle", e.getProviderClassTitle())
                .param("providerClassEndAt", e.getProviderClassEndAt())
                .param("providerAssignmentRef", e.getProviderAssignmentRef())
                .param("providerAssignmentTitle", e.getProviderAssignmentTitle())
                .param("providerAssignmentStartAt", e.getProviderAssignmentStartAt())
                .param("providerAssignmentDueAt", e.getProviderAssignmentDueAt())
                .param("providerAssignmentInstructions", e.getProviderAssignmentInstructions())
                .param("studentReportAccess", e.getStudentReportAccess())
                .param("teacherEmail", e.getTeacherEmail())
                .param("teacherFirstName", e.getTeacherFirstName())
                .param("teacherLastName", e.getTeacherLastName())
                .param("classCreated", e.getClassCreated())
                .param("assignmentCreated", e.getAssignmentCreated())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PlagiarismCohortLinkRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PlagiarismCohortLinkEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("plagiarismAccountId", e.getPlagiarismAccountId())
                .param("accountRef", e.getAccountRef())
                .param("providerClassRef", e.getProviderClassRef())
                .param("providerClassTitle", e.getProviderClassTitle())
                .param("providerClassEndAt", e.getProviderClassEndAt())
                .param("providerAssignmentRef", e.getProviderAssignmentRef())
                .param("providerAssignmentTitle", e.getProviderAssignmentTitle())
                .param("providerAssignmentStartAt", e.getProviderAssignmentStartAt())
                .param("providerAssignmentDueAt", e.getProviderAssignmentDueAt())
                .param("providerAssignmentInstructions", e.getProviderAssignmentInstructions())
                .param("studentReportAccess", e.getStudentReportAccess())
                .param("teacherEmail", e.getTeacherEmail())
                .param("teacherFirstName", e.getTeacherFirstName())
                .param("teacherLastName", e.getTeacherLastName())
                .param("classCreated", e.getClassCreated())
                .param("assignmentCreated", e.getAssignmentCreated())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PlagiarismCohortLinkEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_plagiarism_cohort_link_1: cohort_id, plagiarism_account_id where (deleted_at IS NULL). */
    public boolean takenCohortIdPlagiarismAccountId1(PlagiarismCohortLinkEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PLAGIARISM_COHORT_LINK_1_TAKEN)
                .param("cohortId", e.getCohortId())
                .param("plagiarismAccountId", e.getPlagiarismAccountId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.plagiarism_account row exists (live). */
    public boolean existsPlagiarismAccountId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PLAGIARISM_ACCOUNT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** The institution owning the row reached through cohort_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.cohort_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, PlagiarismCohortLinkEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PlagiarismCohortLinkEntity map(ResultSet rs, int row) throws SQLException {
        return PlagiarismCohortLinkEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .plagiarismAccountId(rs.getObject("plagiarism_account_id", UUID.class))
                .accountRef(rs.getString("account_ref"))
                .providerClassRef(rs.getString("provider_class_ref"))
                .providerClassTitle(rs.getString("provider_class_title"))
                .providerClassEndAt(rs.getObject("provider_class_end_at", OffsetDateTime.class))
                .providerAssignmentRef(rs.getString("provider_assignment_ref"))
                .providerAssignmentTitle(rs.getString("provider_assignment_title"))
                .providerAssignmentStartAt(rs.getObject("provider_assignment_start_at", OffsetDateTime.class))
                .providerAssignmentDueAt(rs.getObject("provider_assignment_due_at", OffsetDateTime.class))
                .providerAssignmentInstructions(rs.getString("provider_assignment_instructions"))
                .studentReportAccess(rs.getObject("student_report_access", Boolean.class))
                .teacherEmail(rs.getString("teacher_email"))
                .teacherFirstName(rs.getString("teacher_first_name"))
                .teacherLastName(rs.getString("teacher_last_name"))
                .classCreated(rs.getObject("class_created", Boolean.class))
                .assignmentCreated(rs.getObject("assignment_created", Boolean.class))
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
