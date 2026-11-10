package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.LmsAssignmentEntity;
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
 * Plain-JDBC access to lms.assignment (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class LmsAssignmentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("version", "r.version", ValueType.INTEGER),
            field("supersededById", "r.superseded_by_id", ValueType.UUID),
            field("publishedAt", "r.published_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING),
            field("question", "r.question", ValueType.STRING),
            field("submissionType", "r.submission_type", ValueType.STRING),
            field("courseId", "r.course_id", ValueType.UUID),
            field("isGraded", "r.is_graded", ValueType.BOOLEAN),
            field("showModelAnswer", "r.show_model_answer", ValueType.BOOLEAN),
            field("modelAnswer", "r.model_answer", ValueType.STRING),
            field("schedulingEnabled", "r.scheduling_enabled", ValueType.BOOLEAN),
            field("scheduleStart", "r.schedule_start", ValueType.TIMESTAMP),
            field("scheduleEnd", "r.schedule_end", ValueType.TIMESTAMP),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("submissionMode", "r.submission_mode", ValueType.STRING),
            field("minGroupSize", "r.min_group_size", ValueType.INTEGER),
            field("maxGroupSize", "r.max_group_size", ValueType.INTEGER),
            field("peerReviewPoints", "r.peer_review_points", ValueType.DECIMAL),
            field("usesInlineAuthoring", "r.uses_inline_authoring", ValueType.BOOLEAN),
            field("isInRepository", "r.is_in_repository", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LmsAssignmentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LmsAssignmentEntity::getCreatedAt),
            Map.entry("updateTime", LmsAssignmentEntity::getUpdatedAt),
            Map.entry("title", LmsAssignmentEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.version, r.superseded_by_id, r.published_at, r.title, r.question,
                   r.submission_type, r.course_id, r.is_graded, r.show_model_answer, r.model_answer,
                   r.scheduling_enabled, r.schedule_start, r.schedule_end, r.institution_id, r.submission_mode,
                   r.min_group_size, r.max_group_size, r.peer_review_points, r.uses_inline_authoring,
                   r.is_in_repository, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM lms.assignment r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.assignment
            (id, tenant_id, title, question, submission_type, course_id, is_graded, show_model_answer, model_answer,
             scheduling_enabled, schedule_start, schedule_end, institution_id, submission_mode, min_group_size,
             max_group_size, peer_review_points, uses_inline_authoring, is_in_repository, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :title, :question, :submissionType, :courseId, COALESCE(:isGraded, true),
                    COALESCE(:showModelAnswer, false), :modelAnswer, COALESCE(:schedulingEnabled, false),
                    :scheduleStart, :scheduleEnd, :institutionId,
                    COALESCE(:submissionMode, 'individual'::character varying), :minGroupSize, :maxGroupSize,
                    :peerReviewPoints, COALESCE(:usesInlineAuthoring, false), COALESCE(:isInRepository, false),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, version, superseded_by_id, published_at, title, question, submission_type,
                      course_id, is_graded, show_model_answer, model_answer, scheduling_enabled, schedule_start,
                      schedule_end, institution_id, submission_mode, min_group_size, max_group_size, peer_review_points,
                      uses_inline_authoring, is_in_repository, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.assignment
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.assignment
               SET title = :title,
                   question = :question,
                   submission_type = :submissionType,
                   course_id = :courseId,
                   is_graded = :isGraded,
                   show_model_answer = :showModelAnswer,
                   model_answer = :modelAnswer,
                   scheduling_enabled = :schedulingEnabled,
                   schedule_start = :scheduleStart,
                   schedule_end = :scheduleEnd,
                   institution_id = :institutionId,
                   submission_mode = :submissionMode,
                   min_group_size = :minGroupSize,
                   max_group_size = :maxGroupSize,
                   peer_review_points = :peerReviewPoints,
                   uses_inline_authoring = :usesInlineAuthoring,
                   is_in_repository = :isInRepository,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_SUPERSEDED_BY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_SUPERSEDED_BY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment WHERE tenant_id = :tenantId
                AND superseded_by_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_SUBMISSION_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_submission WHERE tenant_id = :tenantId
                AND assignment_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_AUTHOR_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_author WHERE tenant_id = :tenantId
                AND assignment_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND assignment_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LESSON_ACTIVITY_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson_activity WHERE tenant_id = :tenantId
                AND assignment_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TIMETABLE_ENTRY_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_entry WHERE tenant_id = :tenantId
                AND assignment_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public LmsAssignmentRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LmsAssignmentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LmsAssignmentRepository::map).optional();
    }

    public Page<LmsAssignmentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LmsAssignmentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LmsAssignmentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LmsAssignmentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LmsAssignmentEntity insert(LmsAssignmentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("title", e.getTitle())
                .param("question", e.getQuestion())
                .param("submissionType", e.getSubmissionType())
                .param("courseId", e.getCourseId())
                .param("isGraded", e.getIsGraded())
                .param("showModelAnswer", e.getShowModelAnswer())
                .param("modelAnswer", e.getModelAnswer())
                .param("schedulingEnabled", e.getSchedulingEnabled())
                .param("scheduleStart", e.getScheduleStart())
                .param("scheduleEnd", e.getScheduleEnd())
                .param("institutionId", e.getInstitutionId())
                .param("submissionMode", e.getSubmissionMode())
                .param("minGroupSize", e.getMinGroupSize())
                .param("maxGroupSize", e.getMaxGroupSize())
                .param("peerReviewPoints", e.getPeerReviewPoints())
                .param("usesInlineAuthoring", e.getUsesInlineAuthoring())
                .param("isInRepository", e.getIsInRepository())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LmsAssignmentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LmsAssignmentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("title", e.getTitle())
                .param("question", e.getQuestion())
                .param("submissionType", e.getSubmissionType())
                .param("courseId", e.getCourseId())
                .param("isGraded", e.getIsGraded())
                .param("showModelAnswer", e.getShowModelAnswer())
                .param("modelAnswer", e.getModelAnswer())
                .param("schedulingEnabled", e.getSchedulingEnabled())
                .param("scheduleStart", e.getScheduleStart())
                .param("scheduleEnd", e.getScheduleEnd())
                .param("institutionId", e.getInstitutionId())
                .param("submissionMode", e.getSubmissionMode())
                .param("minGroupSize", e.getMinGroupSize())
                .param("maxGroupSize", e.getMaxGroupSize())
                .param("peerReviewPoints", e.getPeerReviewPoints())
                .param("usesInlineAuthoring", e.getUsesInlineAuthoring())
                .param("isInRepository", e.getIsInRepository())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LmsAssignmentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.assignment row exists (live). */
    public boolean existsSupersededById(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SUPERSEDED_BY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.assignment rows still reference the row. */
    public boolean referencedByAssignmentSupersededById(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_SUPERSEDED_BY_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.assignment_submission rows still reference the row. */
    public boolean referencedByAssignmentSubmissionAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_SUBMISSION_ASSIGNMENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.content_author rows still reference the row. */
    public boolean referencedByContentAuthorAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_AUTHOR_ASSIGNMENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_ASSIGNMENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.lesson_activity rows still reference the row. */
    public boolean referencedByLessonActivityAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LESSON_ACTIVITY_ASSIGNMENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.timetable_entry rows still reference the row. */
    public boolean referencedByTimetableEntryAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TIMETABLE_ENTRY_ASSIGNMENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, LmsAssignmentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LmsAssignmentEntity map(ResultSet rs, int row) throws SQLException {
        return LmsAssignmentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .version(rs.getObject("version", Integer.class))
                .supersededById(rs.getObject("superseded_by_id", UUID.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
                .title(rs.getString("title"))
                .question(rs.getString("question"))
                .submissionType(rs.getString("submission_type"))
                .courseId(rs.getObject("course_id", UUID.class))
                .isGraded(rs.getObject("is_graded", Boolean.class))
                .showModelAnswer(rs.getObject("show_model_answer", Boolean.class))
                .modelAnswer(rs.getString("model_answer"))
                .schedulingEnabled(rs.getObject("scheduling_enabled", Boolean.class))
                .scheduleStart(rs.getObject("schedule_start", OffsetDateTime.class))
                .scheduleEnd(rs.getObject("schedule_end", OffsetDateTime.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .submissionMode(rs.getString("submission_mode"))
                .minGroupSize(rs.getObject("min_group_size", Integer.class))
                .maxGroupSize(rs.getObject("max_group_size", Integer.class))
                .peerReviewPoints(rs.getBigDecimal("peer_review_points"))
                .usesInlineAuthoring(rs.getObject("uses_inline_authoring", Boolean.class))
                .isInRepository(rs.getObject("is_in_repository", Boolean.class))
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
