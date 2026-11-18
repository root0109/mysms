package com.mysms.academics.repository;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.domain.GradeLevelEntity;
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
 * Plain-JDBC access to academics.grade_level: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class GradeLevelRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("slug", "r.slug", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
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

    private static final Map<String, Function<GradeLevelEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", GradeLevelEntity::getCreatedAt),
            Map.entry("updateTime", GradeLevelEntity::getUpdatedAt),
            Map.entry("name", GradeLevelEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.name, r.slug, r.sort_order, r.is_active, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM academics.grade_level r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO academics.grade_level
            (id, tenant_id, institution_id, name, slug, sort_order, is_active, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :name, :slug, :sortOrder, COALESCE(:isActive, true), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, name, slug, sort_order, is_active, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE academics.grade_level
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE academics.grade_level
               SET institution_id = :institutionId,
                   name = :name,
                   slug = :slug,
                   sort_order = :sortOrder,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_GRADE_LEVEL_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM academics.grade_level
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND slug = :slug AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String REFERENCED_BY_CLASS_SECTION_GRADE_LEVEL_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.class_section WHERE tenant_id = :tenantId
                AND grade_level_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public GradeLevelRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<GradeLevelEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(GradeLevelRepository::map).optional();
    }

    public Page<GradeLevelEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<GradeLevelEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(GradeLevelRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), GradeLevelEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public GradeLevelEntity insert(GradeLevelEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("name", e.getName())
                .param("slug", e.getSlug())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(GradeLevelRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(GradeLevelEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("name", e.getName())
                .param("slug", e.getSlug())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(GradeLevelEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_grade_level_1: institution_id, slug where (deleted_at IS NULL). */
    public boolean takenInstitutionIdSlug1(GradeLevelEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_GRADE_LEVEL_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("slug", e.getSlug())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live academics.class_section rows still reference the row. */
    public boolean referencedByClassSectionGradeLevelId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CLASS_SECTION_GRADE_LEVEL_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, GradeLevelEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static GradeLevelEntity map(ResultSet rs, int row) throws SQLException {
        return GradeLevelEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .name(rs.getString("name"))
                .slug(rs.getString("slug"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
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
