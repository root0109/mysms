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
import com.mysms.learning.lms.domain.TimetableEntryEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Plain-JDBC access to lms.timetable_entry: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class TimetableEntryRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("timetableTemplateId", "r.timetable_template_id", ValueType.UUID),
            field("lessonId", "r.lesson_id", ValueType.UUID),
            field("quizId", "r.quiz_id", ValueType.UUID),
            field("assignmentId", "r.assignment_id", ValueType.UUID),
            field("exerciseId", "r.exercise_id", ValueType.UUID),
            field("liveClassId", "r.live_class_id", ValueType.UUID),
            field("entryDate", "r.entry_date", ValueType.DATE),
            field("dayNumber", "r.day_number", ValueType.INTEGER),
            field("isMilestone", "r.is_milestone", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<TimetableEntryEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", TimetableEntryEntity::getCreatedAt),
            Map.entry("updateTime", TimetableEntryEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.cohort_id, r.timetable_template_id, r.lesson_id, r.quiz_id, r.assignment_id,
                   r.exercise_id, r.live_class_id, r.entry_date, r.day_number, r.start_time, r.end_time,
                   r.duration_minutes, r.is_milestone, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM lms.timetable_entry r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.timetable_entry
            (id, tenant_id, cohort_id, timetable_template_id, lesson_id, quiz_id, assignment_id, exercise_id,
             live_class_id, entry_date, day_number, start_time, end_time, duration_minutes, is_milestone, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :cohortId, :timetableTemplateId, :lessonId, :quizId, :assignmentId, :exerciseId,
                    :liveClassId, :entryDate, :dayNumber, :startTime, :endTime, :durationMinutes,
                    COALESCE(:isMilestone, false), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, cohort_id, timetable_template_id, lesson_id, quiz_id, assignment_id, exercise_id,
                      live_class_id, entry_date, day_number, start_time, end_time, duration_minutes, is_milestone,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.timetable_entry
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.timetable_entry
               SET cohort_id = :cohortId,
                   timetable_template_id = :timetableTemplateId,
                   lesson_id = :lessonId,
                   quiz_id = :quizId,
                   assignment_id = :assignmentId,
                   exercise_id = :exerciseId,
                   live_class_id = :liveClassId,
                   entry_date = :entryDate,
                   day_number = :dayNumber,
                   start_time = :startTime,
                   end_time = :endTime,
                   duration_minutes = :durationMinutes,
                   is_milestone = :isMilestone,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.programming_exercise WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_LESSON_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_LIVE_CLASS_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.live_class WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_TIMETABLE_TEMPLATE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_template WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.cohort_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public TimetableEntryRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<TimetableEntryEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(TimetableEntryRepository::map).optional();
    }

    public Page<TimetableEntryEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<TimetableEntryEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(TimetableEntryRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), TimetableEntryEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public TimetableEntryEntity insert(TimetableEntryEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("cohortId", e.getCohortId())
                .param("timetableTemplateId", e.getTimetableTemplateId())
                .param("lessonId", e.getLessonId())
                .param("quizId", e.getQuizId())
                .param("assignmentId", e.getAssignmentId())
                .param("exerciseId", e.getExerciseId())
                .param("liveClassId", e.getLiveClassId())
                .param("entryDate", e.getEntryDate())
                .param("dayNumber", e.getDayNumber())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("durationMinutes", e.getDurationMinutes())
                .param("isMilestone", e.getIsMilestone())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(TimetableEntryRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(TimetableEntryEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("cohortId", e.getCohortId())
                .param("timetableTemplateId", e.getTimetableTemplateId())
                .param("lessonId", e.getLessonId())
                .param("quizId", e.getQuizId())
                .param("assignmentId", e.getAssignmentId())
                .param("exerciseId", e.getExerciseId())
                .param("liveClassId", e.getLiveClassId())
                .param("entryDate", e.getEntryDate())
                .param("dayNumber", e.getDayNumber())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("durationMinutes", e.getDurationMinutes())
                .param("isMilestone", e.getIsMilestone())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(TimetableEntryEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.assignment row exists (live). */
    public boolean existsAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ASSIGNMENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.programming_exercise row exists (live). */
    public boolean existsExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXERCISE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.lesson row exists (live). */
    public boolean existsLessonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LESSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.live_class row exists (live). */
    public boolean existsLiveClassId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LIVE_CLASS_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.quiz row exists (live). */
    public boolean existsQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUIZ_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.timetable_template row exists (live). */
    public boolean existsTimetableTemplateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_TIMETABLE_TEMPLATE_ID).param("id", id).query(Boolean.class)
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

    private static List<Object> sortValues(SortSpec sort, TimetableEntryEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static TimetableEntryEntity map(ResultSet rs, int row) throws SQLException {
        return TimetableEntryEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .timetableTemplateId(rs.getObject("timetable_template_id", UUID.class))
                .lessonId(rs.getObject("lesson_id", UUID.class))
                .quizId(rs.getObject("quiz_id", UUID.class))
                .assignmentId(rs.getObject("assignment_id", UUID.class))
                .exerciseId(rs.getObject("exercise_id", UUID.class))
                .liveClassId(rs.getObject("live_class_id", UUID.class))
                .entryDate(rs.getObject("entry_date", LocalDate.class))
                .dayNumber(rs.getObject("day_number", Integer.class))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .endTime(rs.getObject("end_time", LocalTime.class))
                .durationMinutes(rs.getObject("duration_minutes", Integer.class))
                .isMilestone(rs.getObject("is_milestone", Boolean.class))
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
