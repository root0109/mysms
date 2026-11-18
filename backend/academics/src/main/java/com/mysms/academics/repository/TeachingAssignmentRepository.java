package com.mysms.academics.repository;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.domain.TeachingAssignmentEntity;
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
 * Plain-JDBC access to academics.teaching_assignment: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class TeachingAssignmentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("classSubjectId", "r.class_subject_id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("periodsPerWeek", "r.periods_per_week", ValueType.INTEGER),
            field("periodsRemaining", "r.periods_remaining", ValueType.INTEGER),
            field("teacherRole", "r.teacher_role", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<TeachingAssignmentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", TeachingAssignmentEntity::getCreatedAt),
            Map.entry("updateTime", TeachingAssignmentEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.class_subject_id, r.staff_id, r.periods_per_week, r.periods_remaining,
                   r.teacher_role, r.is_active, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM academics.teaching_assignment r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO academics.teaching_assignment
            (id, tenant_id, class_subject_id, staff_id, periods_per_week, periods_remaining, teacher_role, is_active,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :classSubjectId, :staffId, :periodsPerWeek, :periodsRemaining, :teacherRole,
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, class_subject_id, staff_id, periods_per_week, periods_remaining, teacher_role,
                      is_active, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE academics.teaching_assignment
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE academics.teaching_assignment
               SET class_subject_id = :classSubjectId,
                   staff_id = :staffId,
                   periods_per_week = :periodsPerWeek,
                   periods_remaining = :periodsRemaining,
                   teacher_role = :teacherRole,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_TEACHING_ASSIGNMENT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM academics.teaching_assignment
                            WHERE tenant_id = :tenantId
                              AND class_subject_id = :classSubjectId AND staff_id = :staffId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_CLASS_SUBJECT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.class_subject WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LESSON_PLAN_TEACHING_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.lesson_plan WHERE tenant_id = :tenantId
                AND teaching_assignment_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM academics.class_subject p0 JOIN academics.class_section p1 ON p1.tenant_id =
                :tenantId AND p1.id =
            p0.class_section_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.class_subject_id IN (SELECT p0.id FROM academics.class_subject p0 WHERE p0.tenant_id =
                 :tenantId
                 AND
             (p0.class_section_id IN (SELECT p1.id FROM academics.class_section p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id =
             ANY(:scopeIds))))))
            """;

    private final JdbcClient jdbc;

    public TeachingAssignmentRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<TeachingAssignmentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(TeachingAssignmentRepository::map).optional();
    }

    public Page<TeachingAssignmentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<TeachingAssignmentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(TeachingAssignmentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), TeachingAssignmentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public TeachingAssignmentEntity insert(TeachingAssignmentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("classSubjectId", e.getClassSubjectId())
                .param("staffId", e.getStaffId())
                .param("periodsPerWeek", e.getPeriodsPerWeek())
                .param("periodsRemaining", e.getPeriodsRemaining())
                .param("teacherRole", e.getTeacherRole())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(TeachingAssignmentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(TeachingAssignmentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("classSubjectId", e.getClassSubjectId())
                .param("staffId", e.getStaffId())
                .param("periodsPerWeek", e.getPeriodsPerWeek())
                .param("periodsRemaining", e.getPeriodsRemaining())
                .param("teacherRole", e.getTeacherRole())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(TeachingAssignmentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_teaching_assignment_1: class_subject_id, staff_id where (deleted_at IS NULL). */
    public boolean takenClassSubjectIdStaffId1(TeachingAssignmentEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_TEACHING_ASSIGNMENT_1_TAKEN)
                .param("classSubjectId", e.getClassSubjectId())
                .param("staffId", e.getStaffId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced academics.class_subject row exists (live). */
    public boolean existsClassSubjectId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CLASS_SUBJECT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live academics.lesson_plan rows still reference the row. */
    public boolean referencedByLessonPlanTeachingAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LESSON_PLAN_TEACHING_ASSIGNMENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through class_subject_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, TeachingAssignmentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static TeachingAssignmentEntity map(ResultSet rs, int row) throws SQLException {
        return TeachingAssignmentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .classSubjectId(rs.getObject("class_subject_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
                .periodsPerWeek(rs.getObject("periods_per_week", Integer.class))
                .periodsRemaining(rs.getObject("periods_remaining", Integer.class))
                .teacherRole(rs.getString("teacher_role"))
                .isActive(rs.getObject("is_active", Boolean.class))
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
