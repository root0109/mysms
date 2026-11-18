package com.mysms.assessment.repository;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.HomeworkSubmissionEntity;
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
import java.util.Collection;
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
 * Plain-JDBC access to assessment.homework_submission: live rows only, keyset paging, and the row_version guard on
 * every update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class HomeworkSubmissionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("homeworkId", "r.homework_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("submittedOn", "r.submitted_on", ValueType.DATE),
            field("status", "r.status", ValueType.STRING),
            field("checkedByUserId", "r.checked_by_user_id", ValueType.UUID),
            field("checkedOn", "r.checked_on", ValueType.DATE),
            field("teacherComments", "r.teacher_comments", ValueType.STRING),
            field("studentReply", "r.student_reply", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<HomeworkSubmissionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", HomeworkSubmissionEntity::getCreatedAt),
            Map.entry("updateTime", HomeworkSubmissionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.homework_id, r.student_id, r.submitted_on, r.status, r.checked_by_user_id,
                   r.checked_on, r.teacher_comments, r.student_reply, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM assessment.homework_submission r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String FIND_BY_ID_FOR_UPDATE = FIND_BY_ID + " FOR UPDATE";
    /** assessment.CheckHomeworkSubmission: checked, by whom and when; comments are kept when the call gives none. */
    private static final String CHECK = """
            UPDATE assessment.homework_submission
               SET status = 'checked',
                   checked_by_user_id = :actor,
                   checked_on = :checkedOn,
                   teacher_comments = COALESCE(CAST(:comments AS text), teacher_comments),
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** assessment.ReplyToHomeworkFeedback: the student's answer to the teacher's comments. */
    private static final String REPLY = """
            UPDATE assessment.homework_submission
               SET student_reply = :reply,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String INSERT = """
            INSERT INTO assessment.homework_submission
            (id, tenant_id, homework_id, student_id, submitted_on, checked_by_user_id, checked_on, teacher_comments,
             student_reply, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :homeworkId, :studentId, :submittedOn, :checkedByUserId, :checkedOn,
                    :teacherComments, :studentReply, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, homework_id, student_id, submitted_on, status, checked_by_user_id, checked_on,
                      teacher_comments, student_reply, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.homework_submission
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.homework_submission
               SET homework_id = :homeworkId,
                   submitted_on = :submittedOn,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_HOMEWORK_SUBMISSION_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.homework_submission
                            WHERE tenant_id = :tenantId
                              AND homework_id = :homeworkId AND student_id = :studentId AND id <> :id AND ((deleted_at
                                  IS NULL)))
            """;
    private static final String EXISTS_HOMEWORK_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.homework WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM assessment.homework p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.homework_id IN (SELECT p0.id FROM assessment.homework p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private static final String REACHABLE_OR_OWN = """
             (:reachAll OR r.homework_id IN (SELECT p0.id FROM assessment.homework p0 WHERE p0.tenant_id = :tenantId
                 AND p0.institution_id = ANY(:reachIds))
              OR r.student_id = ANY(:ownIds))
            """;

    private final JdbcClient jdbc;

    public HomeworkSubmissionRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<HomeworkSubmissionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(HomeworkSubmissionRepository::map).optional();
    }

    public Optional<HomeworkSubmissionEntity> findByIdForUpdate(UUID id) {
        return jdbc.sql(FIND_BY_ID_FOR_UPDATE).param("id", id).query(HomeworkSubmissionRepository::map).optional();
    }

    /** Marks the submission checked; 0 rows means the row changed (or vanished) since it was read. */
    public int check(UUID id, int expected, String comments, UUID actor, LocalDate checkedOn, OffsetDateTime now) {
        return jdbc.sql(CHECK).param("id", id).param("expected", expected).param("comments", comments)
                .param("actor", actor).param("checkedOn", checkedOn).param("now", now).update();
    }

    /** Stores the student's reply; 0 rows means the row changed (or vanished) since it was read. */
    public int reply(UUID id, int expected, String reply, UUID actor, OffsetDateTime now) {
        return jdbc.sql(REPLY).param("id", id).param("expected", expected).param("reply", reply)
                .param("actor", actor).param("now", now).update();
    }

    public Page<HomeworkSubmissionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<HomeworkSubmissionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(HomeworkSubmissionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), HomeworkSubmissionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public HomeworkSubmissionEntity insert(HomeworkSubmissionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("homeworkId", e.getHomeworkId())
                .param("studentId", e.getStudentId())
                .param("submittedOn", e.getSubmittedOn())
                .param("checkedByUserId", e.getCheckedByUserId())
                .param("checkedOn", e.getCheckedOn())
                .param("teacherComments", e.getTeacherComments())
                .param("studentReply", e.getStudentReply())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(HomeworkSubmissionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(HomeworkSubmissionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("homeworkId", e.getHomeworkId())
                .param("submittedOn", e.getSubmittedOn())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(HomeworkSubmissionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_homework_submission_1: homework_id, student_id where (deleted_at IS NULL). */
    public boolean takenHomeworkIdStudentId1(HomeworkSubmissionEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_HOMEWORK_SUBMISSION_1_TAKEN)
                .param("homeworkId", e.getHomeworkId())
                .param("studentId", e.getStudentId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced assessment.homework row exists (live). */
    public boolean existsHomeworkId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_HOMEWORK_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through homework_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the institutions in the caller's staff reach, and rows of the caller's own students anywhere. */
    public static SqlFragment reachableOrOwn(DataScope reach, Collection<UUID> ownStudents) {
        return new SqlFragment(REACHABLE_OR_OWN, Map.of("reachAll", reach.all(), "reachIds", reach.institutionArray(),
                "ownIds", ownStudents.toArray(UUID[]::new)));
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, HomeworkSubmissionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static HomeworkSubmissionEntity map(ResultSet rs, int row) throws SQLException {
        return HomeworkSubmissionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .homeworkId(rs.getObject("homework_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .submittedOn(rs.getObject("submitted_on", LocalDate.class))
                .status(rs.getString("status"))
                .checkedByUserId(rs.getObject("checked_by_user_id", UUID.class))
                .checkedOn(rs.getObject("checked_on", LocalDate.class))
                .teacherComments(rs.getString("teacher_comments"))
                .studentReply(rs.getString("student_reply"))
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
