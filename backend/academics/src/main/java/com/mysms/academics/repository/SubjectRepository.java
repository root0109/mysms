package com.mysms.academics.repository;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.domain.SubjectEntity;
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
 * Plain-JDBC access to academics.subject: live rows only, keyset paging over an allowlisted order and the row_version
 * guard on every update and delete. Every statement filters the caller's tenant itself; row-level security is the
 * backstop.
 */
@Repository
public class SubjectRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("departmentId", "r.department_id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("deliveryType", "r.delivery_type", ValueType.STRING),
            field("defaultCategory", "r.default_category", ValueType.STRING),
            field("gradeWeightage", "r.grade_weightage", ValueType.DECIMAL),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<SubjectEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", SubjectEntity::getCreatedAt),
            Map.entry("updateTime", SubjectEntity::getUpdatedAt),
            Map.entry("name", SubjectEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.department_id, r.code, r.name, r.delivery_type,
                   r.default_category, r.grade_weightage, r.is_active, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM academics.subject r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO academics.subject
            (id, tenant_id, institution_id, department_id, code, name, delivery_type, default_category,
             grade_weightage, is_active, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :departmentId, :code, :name,
                    COALESCE(:deliveryType, 'theory'::character varying),
                    COALESCE(:defaultCategory, 'compulsory'::character varying), :gradeWeightage,
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, department_id, code, name, delivery_type, default_category,
                      grade_weightage, is_active, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE academics.subject
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE academics.subject
               SET institution_id = :institutionId,
                   department_id = :departmentId,
                   code = :code,
                   name = :name,
                   delivery_type = :deliveryType,
                   default_category = :defaultCategory,
                   grade_weightage = :gradeWeightage,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_CLASS_SUBJECT_SUBJECT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.class_subject WHERE tenant_id = :tenantId
                AND subject_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_COURSE_SUBJECT_SUBJECT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.course_subject WHERE tenant_id = :tenantId
                AND subject_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public SubjectRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SubjectEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(SubjectRepository::map).optional();
    }

    public Page<SubjectEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<SubjectEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(SubjectRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), SubjectEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public SubjectEntity insert(SubjectEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("departmentId", e.getDepartmentId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("deliveryType", e.getDeliveryType())
                .param("defaultCategory", e.getDefaultCategory())
                .param("gradeWeightage", e.getGradeWeightage())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(SubjectRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(SubjectEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("departmentId", e.getDepartmentId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("deliveryType", e.getDeliveryType())
                .param("defaultCategory", e.getDefaultCategory())
                .param("gradeWeightage", e.getGradeWeightage())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(SubjectEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live academics.class_subject rows still reference the row. */
    public boolean referencedByClassSubjectSubjectId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CLASS_SUBJECT_SUBJECT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live academics.course_subject rows still reference the row. */
    public boolean referencedByCourseSubjectSubjectId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_COURSE_SUBJECT_SUBJECT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, SubjectEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static SubjectEntity map(ResultSet rs, int row) throws SQLException {
        return SubjectEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .departmentId(rs.getObject("department_id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .deliveryType(rs.getString("delivery_type"))
                .defaultCategory(rs.getString("default_category"))
                .gradeWeightage(rs.getBigDecimal("grade_weightage"))
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
