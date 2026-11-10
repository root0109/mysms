package com.mysms.assessment.repository;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.AssignmentSubmissionEntity;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to assessment.assignment_submission (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class AssignmentSubmissionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("assignmentId", "r.assignment_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("content", "r.content", ValueType.STRING),
            field("submittedAt", "r.submitted_at", ValueType.TIMESTAMP),
            field("status", "r.status", ValueType.STRING),
            field("marksObtained", "r.marks_obtained", ValueType.DECIMAL),
            field("gradedByUserId", "r.graded_by_user_id", ValueType.UUID),
            field("gradedOn", "r.graded_on", ValueType.DATE),
            field("feedback", "r.feedback", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AssignmentSubmissionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AssignmentSubmissionEntity::getCreatedAt),
            Map.entry("updateTime", AssignmentSubmissionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.assignment_id, r.student_id, r.content, r.submitted_at, r.status,
                   r.marks_obtained, r.graded_by_user_id, r.graded_on, r.feedback, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM assessment.assignment_submission r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO assessment.assignment_submission
            (id, tenant_id, assignment_id, student_id, content, submitted_at, marks_obtained, graded_by_user_id,
             graded_on, feedback, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :assignmentId, :studentId, :content, :submittedAt, :marksObtained,
                    :gradedByUserId, :gradedOn, :feedback, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, assignment_id, student_id, content, submitted_at, status, marks_obtained,
                      graded_by_user_id, graded_on, feedback, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.assignment_submission
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.assignment_submission
               SET assignment_id = :assignmentId,
                   student_id = :studentId,
                   content = :content,
                   submitted_at = :submittedAt,
                   marks_obtained = :marksObtained,
                   graded_by_user_id = :gradedByUserId,
                   graded_on = :gradedOn,
                   feedback = :feedback,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ASSIGNMENT_SUBMISSION_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.assignment_submission
                            WHERE tenant_id = :tenantId
                              AND assignment_id = :assignmentId AND student_id = :studentId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.assignment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM assessment.assignment p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** assessment.SubmitAssignmentSubmission: allowed while status is draft or change_requested. */
    private static final String TRANSITION_SUBMIT_ASSIGNMENT_SUBMISSION = """
            UPDATE assessment.assignment_submission
               SET status = 'submitted',
                   submitted_at = :now,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.AcceptAssignmentSubmission: allowed while status is submitted. */
    private static final String TRANSITION_ACCEPT_ASSIGNMENT_SUBMISSION = """
            UPDATE assessment.assignment_submission
               SET status = 'accepted',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** assessment.RejectAssignmentSubmission: allowed while status is submitted. */
    private static final String TRANSITION_REJECT_ASSIGNMENT_SUBMISSION = """
            UPDATE assessment.assignment_submission
               SET status = 'rejected',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.assignment_id IN (SELECT p0.id FROM assessment.assignment p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public AssignmentSubmissionRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AssignmentSubmissionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AssignmentSubmissionRepository::map).optional();
    }

    public Page<AssignmentSubmissionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AssignmentSubmissionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AssignmentSubmissionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AssignmentSubmissionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AssignmentSubmissionEntity insert(AssignmentSubmissionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("assignmentId", e.getAssignmentId())
                .param("studentId", e.getStudentId())
                .param("content", e.getContent())
                .param("submittedAt", e.getSubmittedAt())
                .param("marksObtained", e.getMarksObtained())
                .param("gradedByUserId", e.getGradedByUserId())
                .param("gradedOn", e.getGradedOn())
                .param("feedback", e.getFeedback())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AssignmentSubmissionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AssignmentSubmissionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("assignmentId", e.getAssignmentId())
                .param("studentId", e.getStudentId())
                .param("content", e.getContent())
                .param("submittedAt", e.getSubmittedAt())
                .param("marksObtained", e.getMarksObtained())
                .param("gradedByUserId", e.getGradedByUserId())
                .param("gradedOn", e.getGradedOn())
                .param("feedback", e.getFeedback())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AssignmentSubmissionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_assignment_submission_1: assignment_id, student_id where (deleted_at IS NULL). */
    public boolean takenAssignmentIdStudentId1(AssignmentSubmissionEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ASSIGNMENT_SUBMISSION_1_TAKEN)
                .param("assignmentId", e.getAssignmentId())
                .param("studentId", e.getStudentId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced assessment.assignment row exists (live). */
    public boolean existsAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ASSIGNMENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through assignment_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** assessment.SubmitAssignmentSubmission; 0 rows means the row changed (or vanished) since it was read. */
    public int submitAssignmentSubmission(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_SUBMIT_ASSIGNMENT_SUBMISSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.AcceptAssignmentSubmission; 0 rows means the row changed (or vanished) since it was read. */
    public int acceptAssignmentSubmission(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_ACCEPT_ASSIGNMENT_SUBMISSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** assessment.RejectAssignmentSubmission; 0 rows means the row changed (or vanished) since it was read. */
    public int rejectAssignmentSubmission(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REJECT_ASSIGNMENT_SUBMISSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, AssignmentSubmissionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AssignmentSubmissionEntity map(ResultSet rs, int row) throws SQLException {
        return AssignmentSubmissionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .assignmentId(rs.getObject("assignment_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .content(rs.getString("content"))
                .submittedAt(rs.getObject("submitted_at", OffsetDateTime.class))
                .status(rs.getString("status"))
                .marksObtained(rs.getBigDecimal("marks_obtained"))
                .gradedByUserId(rs.getObject("graded_by_user_id", UUID.class))
                .gradedOn(rs.getObject("graded_on", LocalDate.class))
                .feedback(rs.getString("feedback"))
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
