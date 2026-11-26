package com.mysms.learning.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.SettingsSidebarItemEntity;
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
 * Plain-JDBC access to lms.settings_sidebar_item: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class SettingsSidebarItemRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("settingsId", "r.settings_id", ValueType.UUID),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("itemType", "r.item_type", ValueType.STRING),
            field("itemKey", "r.item_key", ValueType.STRING),
            field("title", "r.title", ValueType.STRING),
            field("icon", "r.icon", ValueType.STRING),
            field("webPageRoute", "r.web_page_route", ValueType.STRING),
            field("route", "r.route", ValueType.STRING),
            field("url", "r.url", ValueType.STRING),
            field("isStandard", "r.is_standard", ValueType.BOOLEAN),
            field("isHidden", "r.is_hidden", ValueType.BOOLEAN),
            field("openInNewWindow", "r.open_in_new_window", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<SettingsSidebarItemEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", SettingsSidebarItemEntity::getCreatedAt),
            Map.entry("updateTime", SettingsSidebarItemEntity::getUpdatedAt),
            Map.entry("sortOrder", SettingsSidebarItemEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.settings_id, r.sort_order, r.item_type, r.item_key, r.title, r.icon,
                   r.web_page_route, r.route, r.url, r.is_standard, r.is_hidden, r.open_in_new_window, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.settings_sidebar_item r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** The visible navigation items of the settings, in order (the public catalogue's navigation). */
    private static final String VISIBLE = SELECT_LIVE
            + " AND r.settings_id = :settingsId AND NOT r.is_hidden ORDER BY r.sort_order, r.id";
    private static final String INSERT = """
            INSERT INTO lms.settings_sidebar_item
            (id, tenant_id, settings_id, sort_order, item_type, item_key, title, icon, web_page_route, route, url,
             is_standard, is_hidden, open_in_new_window, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :settingsId, COALESCE(:sortOrder, 0), :itemType, :itemKey, :title, :icon,
                    :webPageRoute, :route, :url, COALESCE(:isStandard, false), COALESCE(:isHidden, false),
                    COALESCE(:openInNewWindow, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, settings_id, sort_order, item_type, item_key, title, icon, web_page_route, route,
                      url, is_standard, is_hidden, open_in_new_window, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.settings_sidebar_item
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.settings_sidebar_item
               SET sort_order = :sortOrder,
                   item_type = :itemType,
                   item_key = :itemKey,
                   title = :title,
                   icon = :icon,
                   web_page_route = :webPageRoute,
                   route = :route,
                   url = :url,
                   is_standard = :isStandard,
                   is_hidden = :isHidden,
                   open_in_new_window = :openInNewWindow,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_SETTINGS_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.settings WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public SettingsSidebarItemRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The settings' visible navigation items in order. */
    public List<SettingsSidebarItemEntity> visible(UUID settingsId) {
        return jdbc.sql(VISIBLE).param("settingsId", settingsId).query(SettingsSidebarItemRepository::map).list();
    }

    public Optional<SettingsSidebarItemEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(SettingsSidebarItemRepository::map).optional();
    }

    public Page<SettingsSidebarItemEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<SettingsSidebarItemEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(SettingsSidebarItemRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), SettingsSidebarItemEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public SettingsSidebarItemEntity insert(SettingsSidebarItemEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("settingsId", e.getSettingsId())
                .param("sortOrder", e.getSortOrder())
                .param("itemType", e.getItemType())
                .param("itemKey", e.getItemKey())
                .param("title", e.getTitle())
                .param("icon", e.getIcon())
                .param("webPageRoute", e.getWebPageRoute())
                .param("route", e.getRoute())
                .param("url", e.getUrl())
                .param("isStandard", e.getIsStandard())
                .param("isHidden", e.getIsHidden())
                .param("openInNewWindow", e.getOpenInNewWindow())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(SettingsSidebarItemRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(SettingsSidebarItemEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("sortOrder", e.getSortOrder())
                .param("itemType", e.getItemType())
                .param("itemKey", e.getItemKey())
                .param("title", e.getTitle())
                .param("icon", e.getIcon())
                .param("webPageRoute", e.getWebPageRoute())
                .param("route", e.getRoute())
                .param("url", e.getUrl())
                .param("isStandard", e.getIsStandard())
                .param("isHidden", e.getIsHidden())
                .param("openInNewWindow", e.getOpenInNewWindow())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(SettingsSidebarItemEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.settings row exists (live). */
    public boolean existsSettingsId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SETTINGS_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.settings_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, SettingsSidebarItemEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static SettingsSidebarItemEntity map(ResultSet rs, int row) throws SQLException {
        return SettingsSidebarItemEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .settingsId(rs.getObject("settings_id", UUID.class))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .itemType(rs.getString("item_type"))
                .itemKey(rs.getString("item_key"))
                .title(rs.getString("title"))
                .icon(rs.getString("icon"))
                .webPageRoute(rs.getString("web_page_route"))
                .route(rs.getString("route"))
                .url(rs.getString("url"))
                .isStandard(rs.getObject("is_standard", Boolean.class))
                .isHidden(rs.getObject("is_hidden", Boolean.class))
                .openInNewWindow(rs.getObject("open_in_new_window", Boolean.class))
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
