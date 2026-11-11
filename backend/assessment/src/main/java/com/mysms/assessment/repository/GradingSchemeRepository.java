package com.mysms.assessment.repository;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.GradingSchemeEntity;
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

/** Plain-JDBC access to grading schemes with tenant predicates, institution scoping and ETag guarded writes. */
@Repository
public class GradingSchemeRepository {

    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("schemeType", "r.scheme_type", ValueType.STRING),
            field("gradingMethod", "r.grading_method", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("isTemplate", "r.is_template", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));
    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<GradingSchemeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", GradingSchemeEntity::getCreatedAt),
            Map.entry("updateTime", GradingSchemeEntity::getUpdatedAt),
            Map.entry("name", GradingSchemeEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.name, r.scheme_type,
                   r.grading_method, r.criteria, r.is_active, r.is_template, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM assessment.grading_scheme r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO assessment.grading_scheme
            (id, tenant_id, institution_id, academic_year_id, name, scheme_type, grading_method, criteria, is_active,
             is_template, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :name, :schemeType,
                    COALESCE(:gradingMethod, 'percentage_band'::character varying), :criteria,
                    COALESCE(:isActive, true), COALESCE(:isTemplate, false), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, name, scheme_type, grading_method, criteria,
                      is_active, is_template, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String UPDATE = """
            UPDATE assessment.grading_scheme
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   name = :name,
                   scheme_type = :schemeType,
                   grading_method = :gradingMethod,
                   criteria = :criteria,
                   is_active = :isActive,
                   is_template = :isTemplate,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.grading_scheme
               SET deleted_at = :deletedAt, updated_at = :updatedAt, updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UNIQUE_NAME = """
            SELECT EXISTS (SELECT 1 FROM assessment.grading_scheme
                            WHERE tenant_id = :tenantId AND institution_id = :institutionId
                              AND academic_year_id IS NOT DISTINCT FROM :academicYearId
                              AND scheme_type = :schemeType AND name = :name
                              AND id <> :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_BAND = """
            SELECT EXISTS (SELECT 1 FROM assessment.grading_band
                            WHERE tenant_id = :tenantId AND grading_scheme_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TEMPLATE = """
            SELECT EXISTS (SELECT 1 FROM assessment.result_template
                            WHERE tenant_id = :tenantId AND grading_scheme_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = "(:scopeAll OR r.institution_id = ANY(:scopeIds))";

    private final JdbcClient jdbc;

    public GradingSchemeRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<GradingSchemeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(GradingSchemeRepository::map).optional();
    }

    public Page<GradingSchemeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<GradingSchemeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(GradingSchemeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), GradingSchemeEntity::getId);
    }

    public GradingSchemeEntity insert(GradingSchemeEntity entity) {
        return jdbc.sql(INSERT)
                .param("id", entity.getId())
                .param("tenantId", entity.getTenantId())
                .param("institutionId", entity.getInstitutionId())
                .param("academicYearId", entity.getAcademicYearId())
                .param("name", entity.getName())
                .param("schemeType", entity.getSchemeType())
                .param("gradingMethod", entity.getGradingMethod())
                .param("criteria", entity.getCriteria())
                .param("isActive", entity.getIsActive())
                .param("isTemplate", entity.getIsTemplate())
                .param("createdAt", entity.getCreatedAt())
                .param("createdBy", entity.getCreatedBy())
                .param("updatedAt", entity.getUpdatedAt())
                .param("updatedBy", entity.getUpdatedBy())
                .param("rowVersion", entity.getRowVersion())
                .query(GradingSchemeRepository::map).single();
    }

    public int update(GradingSchemeEntity entity, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", entity.getId())
                .param("institutionId", entity.getInstitutionId())
                .param("academicYearId", entity.getAcademicYearId())
                .param("name", entity.getName())
                .param("schemeType", entity.getSchemeType())
                .param("gradingMethod", entity.getGradingMethod())
                .param("criteria", entity.getCriteria())
                .param("isActive", entity.getIsActive())
                .param("isTemplate", entity.getIsTemplate())
                .param("updatedAt", entity.getUpdatedAt())
                .param("updatedBy", entity.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(GradingSchemeEntity entity, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", entity.getId())
                .param("deletedAt", entity.getDeletedAt())
                .param("updatedAt", entity.getUpdatedAt())
                .param("updatedBy", entity.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public boolean takenName(GradingSchemeEntity entity) {
        return Boolean.TRUE.equals(jdbc.sql(UNIQUE_NAME)
                .param("institutionId", entity.getInstitutionId())
                .param("academicYearId", entity.getAcademicYearId())
                .param("schemeType", entity.getSchemeType())
                .param("name", entity.getName())
                .param("id", entity.getId())
                .query(Boolean.class).single());
    }

    public boolean referencedByGradingBand(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_BAND).param("id", id).query(Boolean.class).single());
    }

    public boolean referencedByResultTemplate(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TEMPLATE).param("id", id).query(Boolean.class).single());
    }

    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, GradingSchemeEntity entity) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(entity)).toList();
    }

    private static GradingSchemeEntity map(ResultSet rs, int row) throws SQLException {
        return GradingSchemeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .name(rs.getString("name"))
                .schemeType(rs.getString("scheme_type"))
                .gradingMethod(rs.getString("grading_method"))
                .criteria(rs.getString("criteria"))
                .isActive(rs.getObject("is_active", Boolean.class))
                .isTemplate(rs.getObject("is_template", Boolean.class))
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
