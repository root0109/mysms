package com.mysms.academics.repository;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.domain.CourseEntity;
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
 * Plain-JDBC access to academics.course: live rows only, keyset paging over an allowlisted order and the row_version
 * guard on every update and delete. Every statement filters the caller's tenant itself; row-level security is the
 * backstop.
 */
@Repository
public class CourseRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("programId", "r.program_id", ValueType.UUID),
            field("departmentId", "r.department_id", ValueType.UUID),
            field("parentCourseId", "r.parent_course_id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("evaluationType", "r.evaluation_type", ValueType.STRING),
            field("minUnitLoad", "r.min_unit_load", ValueType.DECIMAL),
            field("maxUnitLoad", "r.max_unit_load", ValueType.DECIMAL),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CourseEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CourseEntity::getCreatedAt),
            Map.entry("updateTime", CourseEntity::getUpdatedAt),
            Map.entry("code", CourseEntity::getCode),
            Map.entry("name", CourseEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.program_id, r.department_id, r.parent_course_id, r.code,
                   r.name, r.evaluation_type, r.min_unit_load, r.max_unit_load, r.is_active, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM academics.course r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO academics.course
            (id, tenant_id, institution_id, program_id, department_id, parent_course_id, code, name, evaluation_type,
             min_unit_load, max_unit_load, is_active, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :programId, :departmentId, :parentCourseId, :code, :name,
                    COALESCE(:evaluationType, 'normal'::character varying), :minUnitLoad, :maxUnitLoad,
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, program_id, department_id, parent_course_id, code, name,
                      evaluation_type, min_unit_load, max_unit_load, is_active, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE academics.course
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE academics.course
               SET institution_id = :institutionId,
                   program_id = :programId,
                   department_id = :departmentId,
                   parent_course_id = :parentCourseId,
                   code = :code,
                   name = :name,
                   evaluation_type = :evaluationType,
                   min_unit_load = :minUnitLoad,
                   max_unit_load = :maxUnitLoad,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_COURSE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM academics.course
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND code = :code AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_PARENT_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.course WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PROGRAM_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.program WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_BATCH_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.batch WHERE tenant_id = :tenantId
                AND course_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COURSE_PARENT_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.course WHERE tenant_id = :tenantId
                AND parent_course_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COURSE_SUBJECT_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.course_subject WHERE tenant_id = :tenantId
                AND course_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public CourseRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CourseEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CourseRepository::map).optional();
    }

    public Page<CourseEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CourseEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CourseRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CourseEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CourseEntity insert(CourseEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("programId", e.getProgramId())
                .param("departmentId", e.getDepartmentId())
                .param("parentCourseId", e.getParentCourseId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("evaluationType", e.getEvaluationType())
                .param("minUnitLoad", e.getMinUnitLoad())
                .param("maxUnitLoad", e.getMaxUnitLoad())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CourseRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CourseEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("programId", e.getProgramId())
                .param("departmentId", e.getDepartmentId())
                .param("parentCourseId", e.getParentCourseId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("evaluationType", e.getEvaluationType())
                .param("minUnitLoad", e.getMinUnitLoad())
                .param("maxUnitLoad", e.getMaxUnitLoad())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CourseEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_course_1: institution_id, code where (deleted_at IS NULL). */
    public boolean takenInstitutionIdCode1(CourseEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_COURSE_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced academics.course row exists (live). */
    public boolean existsParentCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PARENT_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced academics.program row exists (live). */
    public boolean existsProgramId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PROGRAM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live academics.batch rows still reference the row. */
    public boolean referencedByBatchCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_BATCH_COURSE_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live academics.course rows still reference the row. */
    public boolean referencedByCourseParentCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COURSE_PARENT_COURSE_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live academics.course_subject rows still reference the row. */
    public boolean referencedByCourseSubjectCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COURSE_SUBJECT_COURSE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, CourseEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CourseEntity map(ResultSet rs, int row) throws SQLException {
        return CourseEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .programId(rs.getObject("program_id", UUID.class))
                .departmentId(rs.getObject("department_id", UUID.class))
                .parentCourseId(rs.getObject("parent_course_id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .evaluationType(rs.getString("evaluation_type"))
                .minUnitLoad(rs.getBigDecimal("min_unit_load"))
                .maxUnitLoad(rs.getBigDecimal("max_unit_load"))
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
