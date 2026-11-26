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
import com.mysms.learning.lms.domain.RubricEntity;
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
 * Plain-JDBC access to lms.rubric: live rows only, keyset paging, and the row_version guard on every update and delete.
 * Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RubricRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("version", "r.version", ValueType.INTEGER),
            field("supersededById", "r.superseded_by_id", ValueType.UUID),
            field("publishedAt", "r.published_at", ValueType.TIMESTAMP),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("isDefault", "r.is_default", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RubricEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", RubricEntity::getCreatedAt),
            Map.entry("updateTime", RubricEntity::getUpdatedAt),
            Map.entry("title", RubricEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.version, r.superseded_by_id, r.published_at, r.institution_id, r.title,
                   r.is_default, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.rubric r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.rubric
            (id, tenant_id, institution_id, title, is_default, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :institutionId, :title, COALESCE(:isDefault, false), :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, version, superseded_by_id, published_at, institution_id, title, is_default,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.rubric
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.rubric
               SET institution_id = :institutionId,
                   title = :title,
                   is_default = :isDefault,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_SUPERSEDED_BY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_RUBRIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND rubric_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RUBRIC_SUPERSEDED_BY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric WHERE tenant_id = :tenantId
                AND superseded_by_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RUBRIC_CELL_RUBRIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_cell WHERE tenant_id = :tenantId
                AND rubric_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RUBRIC_CRITERION_RUBRIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_criterion WHERE tenant_id = :tenantId
                AND rubric_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RUBRIC_LEVEL_RUBRIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_level WHERE tenant_id = :tenantId
                AND rubric_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public RubricRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RubricEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RubricRepository::map).optional();
    }

    public Page<RubricEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RubricEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RubricRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RubricEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public RubricEntity insert(RubricEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("title", e.getTitle())
                .param("isDefault", e.getIsDefault())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(RubricRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(RubricEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("title", e.getTitle())
                .param("isDefault", e.getIsDefault())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(RubricEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.rubric row exists (live). */
    public boolean existsSupersededById(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SUPERSEDED_BY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemRubricId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_RUBRIC_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.rubric rows still reference the row. */
    public boolean referencedByRubricSupersededById(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RUBRIC_SUPERSEDED_BY_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.rubric_cell rows still reference the row. */
    public boolean referencedByRubricCellRubricId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RUBRIC_CELL_RUBRIC_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.rubric_criterion rows still reference the row. */
    public boolean referencedByRubricCriterionRubricId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RUBRIC_CRITERION_RUBRIC_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.rubric_level rows still reference the row. */
    public boolean referencedByRubricLevelRubricId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RUBRIC_LEVEL_RUBRIC_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, RubricEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static RubricEntity map(ResultSet rs, int row) throws SQLException {
        return RubricEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .version(rs.getObject("version", Integer.class))
                .supersededById(rs.getObject("superseded_by_id", UUID.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .title(rs.getString("title"))
                .isDefault(rs.getObject("is_default", Boolean.class))
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
