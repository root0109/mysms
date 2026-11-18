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
import com.mysms.lms.domain.ExternalToolLinkEntity;
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
 * Plain-JDBC access to lms.external_tool_link: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ExternalToolLinkRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("externalToolResourceId", "r.external_tool_resource_id", ValueType.UUID),
            field("externalToolId", "r.external_tool_id", ValueType.UUID),
            field("linkRef", "r.link_ref", ValueType.STRING),
            field("linkTitle", "r.link_title", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ExternalToolLinkEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ExternalToolLinkEntity::getCreatedAt),
            Map.entry("updateTime", ExternalToolLinkEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.external_tool_resource_id, r.external_tool_id, r.link_ref, r.link_title,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.external_tool_link r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.external_tool_link
            (id, tenant_id, external_tool_resource_id, external_tool_id, link_ref, link_title, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :externalToolResourceId, :externalToolId, :linkRef, :linkTitle, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, external_tool_resource_id, external_tool_id, link_ref, link_title, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.external_tool_link
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.external_tool_link
               SET external_tool_id = :externalToolId,
                   link_ref = :linkRef,
                   link_title = :linkTitle,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_EXTERNAL_TOOL_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.external_tool WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_EXTERNAL_TOOL_RESOURCE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.external_tool_resource WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.external_tool_resource p0 WHERE p0.tenant_id = :tenantId
                AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.external_tool_resource_id IN (SELECT p0.id FROM lms.external_tool_resource p0 WHERE
                 p0.tenant_id = :tenantId
                 AND
             (p0.institution_id = ANY(:scopeIds) OR p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public ExternalToolLinkRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ExternalToolLinkEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ExternalToolLinkRepository::map).optional();
    }

    public Page<ExternalToolLinkEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ExternalToolLinkEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ExternalToolLinkRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ExternalToolLinkEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ExternalToolLinkEntity insert(ExternalToolLinkEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("externalToolResourceId", e.getExternalToolResourceId())
                .param("externalToolId", e.getExternalToolId())
                .param("linkRef", e.getLinkRef())
                .param("linkTitle", e.getLinkTitle())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ExternalToolLinkRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ExternalToolLinkEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("externalToolId", e.getExternalToolId())
                .param("linkRef", e.getLinkRef())
                .param("linkTitle", e.getLinkTitle())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ExternalToolLinkEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.external_tool row exists (live). */
    public boolean existsExternalToolId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXTERNAL_TOOL_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.external_tool_resource row exists (live). */
    public boolean existsExternalToolResourceId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXTERNAL_TOOL_RESOURCE_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /**
     * The institution owning the row reached through external_tool_resource_id (empty: a tenant-level row, or none).
     */
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

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.external_tool_resource_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ExternalToolLinkEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ExternalToolLinkEntity map(ResultSet rs, int row) throws SQLException {
        return ExternalToolLinkEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .externalToolResourceId(rs.getObject("external_tool_resource_id", UUID.class))
                .externalToolId(rs.getObject("external_tool_id", UUID.class))
                .linkRef(rs.getString("link_ref"))
                .linkTitle(rs.getString("link_title"))
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
