package com.mysms.platform.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.ThemeEntity;
import com.mysms.platform.domain.ThemeTokenEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
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
 * Plain-JDBC access to platform.theme and its design tokens, platform.theme_token: one repository for the aggregate,
 * constant schema-qualified SQL, live rows only, keyset paging and the row_version guard. Every statement filters the
 * caller's tenant itself; row-level security is the backstop. Tokens are read for a list of themes at once (no N+1) and
 * replaced as a set in one statement each way.
 */
@Repository
public class ThemeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("isDefault", "r.is_default", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("name", "r.name", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "name";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ThemeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("name", ThemeEntity::getName),
            Map.entry("createTime", ThemeEntity::getCreatedAt),
            Map.entry("updateTime", ThemeEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.name, r.description, r.thumbnail_file_id, r.thumbnail_alt_file_id,
                   r.logo_file_id, r.is_default, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM platform.theme r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String FIND_DEFAULT = SELECT_LIVE + " AND r.is_default";
    private static final String INSERT = """
            INSERT INTO platform.theme
            (id, tenant_id, name, description, thumbnail_file_id, thumbnail_alt_file_id, logo_file_id, is_default,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :name, :description, :thumbnailFileId, :thumbnailAltFileId, :logoFileId,
                    :isDefault, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, name, description, thumbnail_file_id, thumbnail_alt_file_id, logo_file_id,
                      is_default, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String UPDATE = """
            UPDATE platform.theme
               SET name = :name,
                   description = :description,
                   thumbnail_file_id = :thumbnailFileId,
                   thumbnail_alt_file_id = :thumbnailAltFileId,
                   logo_file_id = :logoFileId,
                   is_default = :isDefault,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE platform.theme
               SET deleted_at = :deletedAt,
                   is_default = false,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** Takes the default mark off every other live theme of the tenant (uq_theme_default allows one). */
    private static final String CLEAR_OTHER_DEFAULTS = """
            UPDATE platform.theme
               SET is_default = false,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND is_default AND deleted_at IS NULL AND id <> :id
            RETURNING id
            """;
    private static final String TOKENS_OF = """
            SELECT t.id, t.tenant_id, t.theme_id, t.token_key, t.value, t.created_at, t.created_by, t.updated_at,
                   t.updated_by, t.deleted_at, t.row_version
              FROM platform.theme_token t
             WHERE t.tenant_id = :tenantId
               AND t.deleted_at IS NULL
               AND t.theme_id = ANY(:themeIds)
             ORDER BY t.theme_id, t.token_key
            """;
    private static final String RETIRE_TOKENS = """
            UPDATE platform.theme_token
               SET deleted_at = :now,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND theme_id = :themeId AND deleted_at IS NULL
            """;
    private static final String INSERT_TOKENS = """
            INSERT INTO platform.theme_token
            (id, tenant_id, theme_id, token_key, value, created_at, created_by, updated_at, updated_by, row_version)
            SELECT u.id, :tenantId, :themeId, u.token_key, u.value, :now, :actor, :now, :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:keys AS varchar[]), CAST(:values AS varchar[]))
                   AS u(id, token_key, value)
            """;
    /** A file the theme may show: a live, uploaded file of the tenant. */
    private static final String FILE_AVAILABLE = """
            SELECT EXISTS (SELECT 1 FROM platform.file_object f
                            WHERE f.tenant_id = :tenantId AND f.deleted_at IS NULL
                              AND f.id = :id AND f.status = 'available')
            """;

    private final JdbcClient jdbc;

    public ThemeRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ThemeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ThemeRepository::map).optional();
    }

    /** The tenant's default theme, when it has marked one. */
    public Optional<ThemeEntity> findDefault() {
        return jdbc.sql(FIND_DEFAULT).query(ThemeRepository::map).optional();
    }

    public Page<ThemeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ThemeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ThemeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ThemeEntity::getId);
    }

    public ThemeEntity insert(ThemeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("name", e.getName())
                .param("description", e.getDescription())
                .param("thumbnailFileId", e.getThumbnailFileId())
                .param("thumbnailAltFileId", e.getThumbnailAltFileId())
                .param("logoFileId", e.getLogoFileId())
                .param("isDefault", e.getIsDefault())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ThemeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ThemeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("name", e.getName())
                .param("description", e.getDescription())
                .param("thumbnailFileId", e.getThumbnailFileId())
                .param("thumbnailAltFileId", e.getThumbnailAltFileId())
                .param("logoFileId", e.getLogoFileId())
                .param("isDefault", e.getIsDefault())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ThemeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Removes the default mark from every other live theme; returns the themes that lost it. */
    public List<UUID> clearOtherDefaults(UUID keep, OffsetDateTime now, UUID actor) {
        return jdbc.sql(CLEAR_OTHER_DEFAULTS).param("id", keep).param("now", now).param("actor", actor)
                .query(UUID.class).list();
    }

    /** The live tokens of these themes, by theme then key. */
    public List<ThemeTokenEntity> tokensOf(Collection<UUID> themeIds) {
        if (themeIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(TOKENS_OF).param("themeIds", themeIds.toArray(UUID[]::new))
                .query(ThemeRepository::mapToken).list();
    }

    /**
     * Replaces the theme's live tokens with these: the current set is retired (soft-deleted) and the new one inserted.
     *
     * @param ids one new UUIDv7 per token, in the order of {@code tokens}
     */
    public void replaceTokens(UUID themeId, Map<String, String> tokens, List<UUID> ids, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(RETIRE_TOKENS).param("themeId", themeId).param("now", now).param("actor", actor).update();
        if (tokens.isEmpty()) {
            return;
        }
        jdbc.sql(INSERT_TOKENS)
                .param("themeId", themeId)
                .param("ids", ids.toArray(UUID[]::new))
                .param("keys", tokens.keySet().toArray(String[]::new))
                .param("values", tokens.values().toArray(String[]::new))
                .param("now", now)
                .param("actor", actor)
                .update();
    }

    /** Whether the file is a live, uploaded file of the tenant (a theme may only show such files). */
    public boolean fileAvailable(UUID fileId) {
        return Boolean.TRUE.equals(jdbc.sql(FILE_AVAILABLE).param("id", fileId).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, ThemeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ThemeEntity map(ResultSet rs, int row) throws SQLException {
        return ThemeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .thumbnailFileId(rs.getObject("thumbnail_file_id", UUID.class))
                .thumbnailAltFileId(rs.getObject("thumbnail_alt_file_id", UUID.class))
                .logoFileId(rs.getObject("logo_file_id", UUID.class))
                .isDefault(rs.getBoolean("is_default"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static ThemeTokenEntity mapToken(ResultSet rs, int row) throws SQLException {
        return ThemeTokenEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .themeId(rs.getObject("theme_id", UUID.class))
                .tokenKey(rs.getString("token_key"))
                .value(rs.getString("value"))
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
