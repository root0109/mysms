package com.mysms.timetable.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.timetable.config.TimetableDatabaseConfig;
import com.mysms.timetable.domain.SessionEntity;
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
 * Plain-JDBC access to timetable.session: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class SessionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("periodId", "r.period_id", ValueType.UUID),
            field("startAt", "r.start_at", ValueType.TIMESTAMP),
            field("endAt", "r.end_at", ValueType.TIMESTAMP),
            field("weekday", "r.weekday", ValueType.INTEGER),
            field("classSectionId", "r.class_section_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("batchId", "r.batch_id", ValueType.UUID),
            field("subjectId", "r.subject_id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("roomId", "r.room_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("sessionType", "r.session_type", ValueType.STRING),
            field("color", "r.color", ValueType.INTEGER),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    /** The order of a timetable (timetable.GetTimetable): by start. Not offered to the orderBy of the standard list. */
    public static final Map<String, FieldSpec> TIMETABLE_SORT_FIELDS = fields(
            field("startAt", "r.start_at", ValueType.TIMESTAMP));

    public static final String TIMETABLE_ORDER = "startAt";

    private static final Map<String, Function<SessionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("startAt", SessionEntity::getStartAt),
            Map.entry("createTime", SessionEntity::getCreatedAt),
            Map.entry("updateTime", SessionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.period_id, r.start_at, r.end_at, r.weekday,
                   r.class_section_id, r.course_id, r.batch_id, r.subject_id, r.staff_id, r.room_id, r.title,
                   r.session_type, r.color, r.status, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM timetable.session r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO timetable.session
            (id, tenant_id, institution_id, period_id, start_at, end_at, weekday, class_section_id, course_id,
             batch_id, subject_id, staff_id, room_id, title, session_type, color, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :periodId, :startAt, :endAt, :weekday, :classSectionId,
                    :courseId, :batchId, :subjectId, :staffId, :roomId, :title, :sessionType, :color, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, period_id, start_at, end_at, weekday, class_section_id, course_id,
                      batch_id, subject_id, staff_id, room_id, title, session_type, color, status, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE timetable.session
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE timetable.session
               SET institution_id = :institutionId,
                   period_id = :periodId,
                   start_at = :startAt,
                   end_at = :endAt,
                   weekday = :weekday,
                   class_section_id = :classSectionId,
                   course_id = :courseId,
                   batch_id = :batchId,
                   subject_id = :subjectId,
                   staff_id = :staffId,
                   room_id = :roomId,
                   title = :title,
                   session_type = :sessionType,
                   color = :color,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_PERIOD_ID = """
            SELECT EXISTS (SELECT 1 FROM timetable.period WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_SESSION_STUDENT_SESSION_ID = """
            SELECT EXISTS (SELECT 1 FROM timetable.session_student WHERE tenant_id = :tenantId
                AND session_id = :id AND deleted_at IS NULL)
            """;

    /** timetable.ConfirmSession: allowed while status is draft. */
    private static final String TRANSITION_CONFIRM_SESSION = """
            UPDATE timetable.session
               SET status = 'confirmed',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** timetable.CompleteSession: allowed while status is confirmed. */
    private static final String TRANSITION_COMPLETE_SESSION = """
            UPDATE timetable.session
               SET status = 'done',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** timetable.CancelSession: allowed while status is draft or confirmed. */
    private static final String TRANSITION_CANCEL_SESSION = """
            UPDATE timetable.session
               SET status = 'cancelled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** timetable.ResetSession: allowed while status is cancelled. */
    private static final String TRANSITION_RESET_SESSION = """
            UPDATE timetable.session
               SET status = 'draft',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String TIMETABLE_BETWEEN = """
             r.start_at >= :timetableFrom AND r.start_at < :timetableTo AND r.status <> 'draft'
            """;

    private static final String LISTING_STUDENT = """
             EXISTS (SELECT 1 FROM timetable.session_student ss
                      WHERE ss.tenant_id = :tenantId AND ss.session_id = r.id
                        AND ss.student_id = :timetableStudentId AND ss.deleted_at IS NULL)
            """;

    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public SessionRepository(@Qualifier(TimetableDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SessionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(SessionRepository::map).optional();
    }

    public Page<SessionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<SessionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(SessionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), SessionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public SessionEntity insert(SessionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("periodId", e.getPeriodId())
                .param("startAt", e.getStartAt())
                .param("endAt", e.getEndAt())
                .param("weekday", e.getWeekday())
                .param("classSectionId", e.getClassSectionId())
                .param("courseId", e.getCourseId())
                .param("batchId", e.getBatchId())
                .param("subjectId", e.getSubjectId())
                .param("staffId", e.getStaffId())
                .param("roomId", e.getRoomId())
                .param("title", e.getTitle())
                .param("sessionType", e.getSessionType())
                .param("color", e.getColor())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(SessionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(SessionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("periodId", e.getPeriodId())
                .param("startAt", e.getStartAt())
                .param("endAt", e.getEndAt())
                .param("weekday", e.getWeekday())
                .param("classSectionId", e.getClassSectionId())
                .param("courseId", e.getCourseId())
                .param("batchId", e.getBatchId())
                .param("subjectId", e.getSubjectId())
                .param("staffId", e.getStaffId())
                .param("roomId", e.getRoomId())
                .param("title", e.getTitle())
                .param("sessionType", e.getSessionType())
                .param("color", e.getColor())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(SessionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced timetable.period row exists (live). */
    public boolean existsPeriodId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PERIOD_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live timetable.session_student rows still reference the row. */
    public boolean referencedBySessionStudentSessionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_SESSION_STUDENT_SESSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** timetable.ConfirmSession; 0 rows means the row changed (or vanished) since it was read. */
    public int confirmSession(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CONFIRM_SESSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** timetable.CompleteSession; 0 rows means the row changed (or vanished) since it was read. */
    public int completeSession(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_COMPLETE_SESSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** timetable.CancelSession; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelSession(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_SESSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** timetable.ResetSession; 0 rows means the row changed (or vanished) since it was read. */
    public int resetSession(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_RESET_SESSION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /**
     * The sessions of a timetable: those that start in [from, to) and are part of the published timetable — a draft
     * is the builder's working copy and is not shown; a cancelled session is, so that who expected it sees it
     * cancelled.
     */
    public static SqlFragment timetableBetween(OffsetDateTime from, OffsetDateTime to) {
        return new SqlFragment(TIMETABLE_BETWEEN, Map.of("timetableFrom", from, "timetableTo", to));
    }

    public static SqlFragment ofClassSection(UUID classSectionId) {
        return new SqlFragment("r.class_section_id = :timetableClassSectionId",
                Map.of("timetableClassSectionId", classSectionId));
    }

    public static SqlFragment ofBatch(UUID batchId) {
        return new SqlFragment("r.batch_id = :timetableBatchId", Map.of("timetableBatchId", batchId));
    }

    public static SqlFragment ofStaff(UUID staffId) {
        return new SqlFragment("r.staff_id = :timetableStaffId", Map.of("timetableStaffId", staffId));
    }

    public static SqlFragment ofRoom(UUID roomId) {
        return new SqlFragment("r.room_id = :timetableRoomId", Map.of("timetableRoomId", roomId));
    }

    /** Sessions that list the student themselves (an elective, a remedial class); not the student's class. */
    public static SqlFragment listingStudent(UUID studentId) {
        return new SqlFragment(LISTING_STUDENT, Map.of("timetableStudentId", studentId));
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, SessionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static SessionEntity map(ResultSet rs, int row) throws SQLException {
        return SessionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .periodId(rs.getObject("period_id", UUID.class))
                .startAt(rs.getObject("start_at", OffsetDateTime.class))
                .endAt(rs.getObject("end_at", OffsetDateTime.class))
                .weekday(rs.getObject("weekday", Integer.class))
                .classSectionId(rs.getObject("class_section_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .batchId(rs.getObject("batch_id", UUID.class))
                .subjectId(rs.getObject("subject_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
                .roomId(rs.getObject("room_id", UUID.class))
                .title(rs.getString("title"))
                .sessionType(rs.getString("session_type"))
                .color(rs.getObject("color", Integer.class))
                .status(rs.getString("status"))
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
