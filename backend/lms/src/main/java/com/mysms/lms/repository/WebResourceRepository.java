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
import com.mysms.lms.domain.WebResourceEntity;
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
 * Plain-JDBC access to lms.web_resource (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class WebResourceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("resourceType", "r.resource_type", ValueType.STRING),
            field("url", "r.url", ValueType.STRING),
            field("launchFile", "r.launch_file", ValueType.STRING),
            field("windowFeatures", "r.window_features", ValueType.STRING),
            field("categoryId", "r.category_id", ValueType.UUID),
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

    private static final Map<String, Function<WebResourceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", WebResourceEntity::getCreatedAt),
            Map.entry("updateTime", WebResourceEntity::getUpdatedAt),
            Map.entry("title", WebResourceEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.title, r.description, r.resource_type, r.url, r.launch_file,
                   r.window_features, r.category_id, r.is_in_repository, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM lms.web_resource r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.web_resource
            (id, tenant_id, institution_id, title, description, resource_type, url, launch_file, window_features,
             category_id, is_in_repository, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :title, :description, :resourceType, :url, :launchFile,
                    :windowFeatures, :categoryId, COALESCE(:isInRepository, false), :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, title, description, resource_type, url, launch_file,
                      window_features, category_id, is_in_repository, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.web_resource
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.web_resource
               SET institution_id = :institutionId,
                   title = :title,
                   description = :description,
                   resource_type = :resourceType,
                   url = :url,
                   launch_file = :launchFile,
                   window_features = :windowFeatures,
                   category_id = :categoryId,
                   is_in_repository = :isInRepository,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CATEGORY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.resource_category WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_WEB_RESOURCE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND web_resource_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public WebResourceRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<WebResourceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(WebResourceRepository::map).optional();
    }

    public Page<WebResourceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<WebResourceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(WebResourceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), WebResourceEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public WebResourceEntity insert(WebResourceEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("resourceType", e.getResourceType())
                .param("url", e.getUrl())
                .param("launchFile", e.getLaunchFile())
                .param("windowFeatures", e.getWindowFeatures())
                .param("categoryId", e.getCategoryId())
                .param("isInRepository", e.getIsInRepository())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(WebResourceRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(WebResourceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("resourceType", e.getResourceType())
                .param("url", e.getUrl())
                .param("launchFile", e.getLaunchFile())
                .param("windowFeatures", e.getWindowFeatures())
                .param("categoryId", e.getCategoryId())
                .param("isInRepository", e.getIsInRepository())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(WebResourceEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.resource_category row exists (live). */
    public boolean existsCategoryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CATEGORY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemWebResourceId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_WEB_RESOURCE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, WebResourceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static WebResourceEntity map(ResultSet rs, int row) throws SQLException {
        return WebResourceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .resourceType(rs.getString("resource_type"))
                .url(rs.getString("url"))
                .launchFile(rs.getString("launch_file"))
                .windowFeatures(rs.getString("window_features"))
                .categoryId(rs.getObject("category_id", UUID.class))
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
