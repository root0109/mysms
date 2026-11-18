package com.mysms.platform.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.TagEntity;
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
 * Plain-JDBC access to platform.tag: live rows only, keyset paging over an allowlisted order and the row_version guard
 * on every update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class TagRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("tagType", "r.tag_type", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("slug", "r.slug", ValueType.STRING),
            field("code", "r.code", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("name", "r.name", ValueType.STRING),
            field("slug", "r.slug", ValueType.STRING),
            field("tagType", "r.tag_type", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "name";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<TagEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("name", TagEntity::getName),
            Map.entry("slug", TagEntity::getSlug),
            Map.entry("tagType", TagEntity::getTagType),
            Map.entry("createTime", TagEntity::getCreatedAt),
            Map.entry("updateTime", TagEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.tag_type, r.name, r.slug, r.sort_order, r.code, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM platform.tag r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO platform.tag
            (id, tenant_id, tag_type, name, slug, sort_order, code, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, COALESCE(:tagType, 'general'::character varying), :name, :slug, :sortOrder, :code,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, tag_type, name, slug, sort_order, code, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String UPDATE = """
            UPDATE platform.tag
               SET tag_type = :tagType,
                   name = :name,
                   slug = :slug,
                   sort_order = :sortOrder,
                   code = :code,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE platform.tag
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** uq_tag_1: (tenant, tag type, slug) among live rows. */
    private static final String SLUG_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM platform.tag r
                            WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL
                              AND r.tag_type = :tagType AND r.slug = :slug AND r.id <> :id)
            """;
    /** uq_tag_code: (tenant, tag type, code) among live rows with a code. */
    private static final String CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM platform.tag r
                            WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL
                              AND r.tag_type = :tagType AND r.code = :code AND r.id <> :id)
            """;
    private static final String ASSIGNED = """
            SELECT EXISTS (SELECT 1 FROM platform.tag_assignment a
                            WHERE a.tenant_id = :tenantId AND a.deleted_at IS NULL AND a.tag_id = :id)
            """;

    /** Tags whose name contains the text (pattern with LIKE wildcards escaped), by name. */
    private static final String SEARCH = SELECT_LIVE + """
             AND r.name ILIKE :pattern ESCAPE '\\'
            ORDER BY lower(r.name), r.id
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    public TagRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<TagEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(TagRepository::map).optional();
    }

    public Page<TagEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<TagEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(TagRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), TagEntity::getId);
    }

    /** Inserts the row and returns it as stored (the default tag type included). */
    public TagEntity insert(TagEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("tagType", e.getTagType())
                .param("name", e.getName())
                .param("slug", e.getSlug())
                .param("sortOrder", e.getSortOrder())
                .param("code", e.getCode())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(TagRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(TagEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("tagType", e.getTagType())
                .param("name", e.getName())
                .param("slug", e.getSlug())
                .param("sortOrder", e.getSortOrder())
                .param("code", e.getCode())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(TagEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** At most {@code limit} tags whose name contains the text, by name. */
    public List<TagEntity> search(String pattern, int limit) {
        return jdbc.sql(SEARCH).param("pattern", pattern).param("limit", limit).query(TagRepository::map).list();
    }

    /** Whether another live tag of the same type already uses the slug. */
    public boolean slugTaken(TagEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(SLUG_TAKEN).param("tagType", e.getTagType()).param("slug", e.getSlug())
                .param("id", e.getId()).query(Boolean.class).single());
    }

    /** Whether another live tag of the same type already uses the code. */
    public boolean codeTaken(TagEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(CODE_TAKEN).param("tagType", e.getTagType()).param("code", e.getCode())
                .param("id", e.getId()).query(Boolean.class).single());
    }

    /** Whether live tag assignments still apply the tag. */
    public boolean assigned(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(ASSIGNED).param("id", id).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, TagEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static TagEntity map(ResultSet rs, int row) throws SQLException {
        return TagEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .tagType(rs.getString("tag_type"))
                .name(rs.getString("name"))
                .slug(rs.getString("slug"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .code(rs.getString("code"))
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
