package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.SettingValueEntity;
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
 * Plain-JDBC access to iam.setting_value (JAVA-CODING-RULES §6): schema-qualified constant SQL, live rows only,
 * keyset paging, the row_version guard on update and delete. Row-level security limits every statement to the bound
 * tenant; the explicit tenant column is still written from the call context.
 */
@Repository
public class SettingValueRepository {

    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            new FieldSpec("scopeType", "r.scope_type", ValueType.STRING),
            new FieldSpec("scopeId", "r.scope_id", ValueType.UUID),
            new FieldSpec("settingKey", "r.setting_key", ValueType.STRING),
            new FieldSpec("isLocked", "r.is_locked", ValueType.BOOLEAN),
            new FieldSpec("createTime", "r.created_at", ValueType.TIMESTAMP),
            new FieldSpec("updateTime", "r.updated_at", ValueType.TIMESTAMP));
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            new FieldSpec("settingKey", "r.setting_key", ValueType.STRING),
            new FieldSpec("scopeType", "r.scope_type", ValueType.STRING),
            new FieldSpec("createTime", "r.created_at", ValueType.TIMESTAMP),
            new FieldSpec("updateTime", "r.updated_at", ValueType.TIMESTAMP));
    public static final String DEFAULT_ORDER = "settingKey,scopeType";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<SettingValueEntity, Object>> SORT_VALUES = Map.of(
            "settingKey", SettingValueEntity::getSettingKey,
            "scopeType", SettingValueEntity::getScopeType,
            "createTime", SettingValueEntity::getCreatedAt,
            "updateTime", SettingValueEntity::getUpdatedAt);
    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.scope_type, r.scope_id, r.setting_key, r.value, r.is_locked, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM iam.setting_value r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    /** The values that can apply to one user: the tenant's and those of the user's own units. */
    private static final String FOR_CHAIN = SELECT_LIVE + """
               AND (r.scope_type = 'tenant'
                    OR (r.scope_type = 'institution' AND r.scope_id = CAST(:institution AS uuid))
                    OR (r.scope_type = 'department' AND r.scope_id = CAST(:department AS uuid))
                    OR (r.scope_type = 'class_section' AND r.scope_id = CAST(:classSection AS uuid))
                    OR (r.scope_type = 'user' AND r.scope_id = CAST(:user AS uuid)))
            """;
    private static final String FIND_AT_SCOPE = SELECT_LIVE + """
               AND r.scope_type = :scopeType AND r.scope_id IS NOT DISTINCT FROM CAST(:scopeId AS uuid)
               AND r.setting_key = :key
            """;
    private static final String INSERT = """
            INSERT INTO iam.setting_value
                   (id, tenant_id, scope_type, scope_id, setting_key, value, is_locked, created_at, created_by, updated_at,
                    updated_by, row_version)
            VALUES (:id, :tenant, :scopeType, :scopeId, :key, :value, :locked, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            """;
    private static final String UPDATE = """
            UPDATE iam.setting_value
               SET value = :value,
                   is_locked = :locked,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE iam.setting_value
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final int COUNT_CAP = 10_001;

    private final JdbcClient jdbc;

    public SettingValueRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SettingValueEntity> findById(UUID id) {
        return jdbc.sql(SELECT_LIVE + " AND r.id = :id").param("id", id).query(SettingValueRepository::map).optional();
    }

    /** The live value of a key at one scope ({@code scopeId} null for the tenant scope). */
    public Optional<SettingValueEntity> findAtScope(String scopeType, UUID scopeId, String key) {
        return jdbc.sql(FIND_AT_SCOPE).param("scopeType", scopeType).param("scopeId", text(scopeId)).param("key", key)
                .query(SettingValueRepository::map).optional();
    }

    /** Every value on the user's resolution chain; absent units are passed as null and match nothing. */
    public List<SettingValueEntity> findForChain(UUID institution, UUID department, UUID classSection, UUID user) {
        return jdbc.sql(FOR_CHAIN).param("institution", text(institution)).param("department", text(department))
                .param("classSection", text(classSection)).param("user", text(user))
                .query(SettingValueRepository::map).list();
    }

    public Page<SettingValueEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + " LIMIT :limit";
        List<SettingValueEntity> rows = jdbc.sql(sql).params(where.params()).param("limit", page.limit())
                .query(SettingValueRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), SettingValueEntity::getId);
    }

    public int count(SqlFragment filter) {
        String sql = "SELECT count(*) FROM (" + SELECT_LIVE + " AND (" + filter.sql() + ") LIMIT :cap) counted";
        return jdbc.sql(sql).params(filter.params()).param("cap", COUNT_CAP).query(Integer.class).single();
    }

    public void insert(SettingValueEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenant", e.getTenantId())
                .param("scopeType", e.getScopeType())
                .param("scopeId", e.getScopeId())
                .param("key", e.getSettingKey())
                .param("value", e.getValue())
                .param("locked", e.isLocked())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(SettingValueEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("value", e.getValue())
                .param("locked", e.isLocked())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(SettingValueEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static String text(UUID id) {
        return id == null ? null : id.toString();
    }

    private static List<Object> sortValues(SortSpec sort, SettingValueEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static SettingValueEntity map(ResultSet rs, int row) throws SQLException {
        return SettingValueEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .scopeType(rs.getString("scope_type"))
                .scopeId(rs.getObject("scope_id", UUID.class))
                .settingKey(rs.getString("setting_key"))
                .value(rs.getString("value"))
                .locked(rs.getBoolean("is_locked"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
