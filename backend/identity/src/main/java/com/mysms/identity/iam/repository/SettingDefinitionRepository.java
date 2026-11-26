package com.mysms.identity.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.domain.SettingDefinitionEntity;
import java.sql.Array;
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
 * Plain-JDBC reads of iam.setting_definition, the global setting catalogue (no tenant; maintained by migrations, so
 * there are no writes here). Keyset paging over an allowlisted order, filters on an allowlist of fields.
 */
@Repository
public class SettingDefinitionRepository {

    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            new FieldSpec("settingKey", "r.setting_key", ValueType.STRING),
            new FieldSpec("category", "r.category", ValueType.STRING),
            new FieldSpec("valueType", "r.value_type", ValueType.STRING),
            new FieldSpec("userEditable", "r.user_editable", ValueType.BOOLEAN));
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            new FieldSpec("settingKey", "r.setting_key", ValueType.STRING),
            new FieldSpec("category", "r.category", ValueType.STRING),
            new FieldSpec("sortOrder", "r.sort_order", ValueType.INTEGER));
    public static final String DEFAULT_ORDER = "category,sortOrder,settingKey";
    public static final String ID_COLUMN = "r.id";
    /**
     * Definitions the caller may see: settable by users themselves, or settable at a scope the caller administers
     * (the same rule as SettingRules.visibleTo, in SQL).
     */
    public static SqlFragment visibleFor(java.util.Set<String> administeredScopes) {
        return new SqlFragment("((r.user_editable AND 'user' = ANY (r.allowed_scopes))"
                + " OR CAST(r.allowed_scopes AS text[]) && CAST(:administeredScopes AS text[]))",
                Map.of("administeredScopes", administeredScopes.stream().sorted().toArray(String[]::new)));
    }

    private static final Map<String, Function<SettingDefinitionEntity, Object>> SORT_VALUES = Map.of(
            "settingKey", SettingDefinitionEntity::getSettingKey,
            "category", SettingDefinitionEntity::getCategory,
            "sortOrder", SettingDefinitionEntity::getSortOrder);
    private static final String SELECT_LIVE = """
            SELECT r.id, r.setting_key, r.category, r.value_type, r.allowed_values, r.min_value, r.max_value,
                   r.default_value, r.allowed_scopes, r.user_editable, r.description, r.sort_order, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM iam.setting_definition r
             WHERE r.deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public SettingDefinitionRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SettingDefinitionEntity> findById(UUID id) {
        return jdbc.sql(SELECT_LIVE + " AND r.id = :id").param("id", id).query(SettingDefinitionRepository::map)
                .optional();
    }

    public Optional<SettingDefinitionEntity> findByKey(String key) {
        return jdbc.sql(SELECT_LIVE + " AND r.setting_key = :key").param("key", key)
                .query(SettingDefinitionRepository::map).optional();
    }

    /** The whole catalogue, in display order. */
    public List<SettingDefinitionEntity> findAll() {
        return jdbc.sql(SELECT_LIVE + " ORDER BY r.category, r.sort_order, r.setting_key")
                .query(SettingDefinitionRepository::map).list();
    }

    public Page<SettingDefinitionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<SettingDefinitionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(SettingDefinitionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), SettingDefinitionEntity::getId);
    }

    private static List<Object> sortValues(SortSpec sort, SettingDefinitionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    static List<String> strings(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        try {
            return List.of((String[]) array.getArray());
        } finally {
            array.free();
        }
    }

    private static SettingDefinitionEntity map(ResultSet rs, int row) throws SQLException {
        return SettingDefinitionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .settingKey(rs.getString("setting_key"))
                .category(rs.getString("category"))
                .valueType(rs.getString("value_type"))
                .allowedValues(strings(rs.getArray("allowed_values")))
                .minValue(rs.getObject("min_value", Integer.class))
                .maxValue(rs.getObject("max_value", Integer.class))
                .defaultValue(rs.getString("default_value"))
                .allowedScopes(strings(rs.getArray("allowed_scopes")))
                .userEditable(rs.getBoolean("user_editable"))
                .description(rs.getString("description"))
                .sortOrder(rs.getInt("sort_order"))
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
