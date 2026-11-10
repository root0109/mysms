package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.PermissionEntity;
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

/** Reads of the global permission catalogue iam.permission (no tenant column, no row-level security). */
@Repository
public class PermissionCatalogRepository {

    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "p.id", ValueType.UUID),
            field("code", "p.code", ValueType.STRING),
            field("displayName", "p.display_name", ValueType.STRING),
            field("moduleCode", "p.module_code", ValueType.STRING),
            field("isNavigation", "p.is_navigation", ValueType.BOOLEAN),
            field("isModifiable", "p.is_modifiable", ValueType.BOOLEAN),
            field("appliesToAllRoles", "p.applies_to_all_roles", ValueType.BOOLEAN));
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("code", "p.code", ValueType.STRING),
            field("createTime", "p.created_at", ValueType.TIMESTAMP));
    public static final String DEFAULT_ORDER = "code";
    public static final String ID_COLUMN = "p.id";

    private static final Map<String, Function<PermissionEntity, Object>> SORT_VALUES = Map.of(
            "code", PermissionEntity::getCode,
            "createTime", PermissionEntity::getCreatedAt);
    private static final String SELECT_LIVE = """
            SELECT p.id, p.code, p.display_name, p.description, p.module_code, p.is_navigation, p.is_modifiable,
                   p.applies_to_all_roles, p.is_object_level, p.created_at, p.created_by, p.updated_at, p.updated_by,
                   p.deleted_at, p.row_version
              FROM iam.permission p
             WHERE p.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND p.id = :id";

    private final JdbcClient jdbc;

    public PermissionCatalogRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PermissionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PermissionCatalogRepository::map).optional();
    }

    public Page<PermissionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PermissionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PermissionCatalogRepository::map).list();
        return page.toPage(rows, e -> page.sort().orders().stream()
                .map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList(), PermissionEntity::getId);
    }

    private static PermissionEntity map(ResultSet rs, int row) throws SQLException {
        return PermissionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .code(rs.getString("code"))
                .displayName(rs.getString("display_name"))
                .description(rs.getString("description"))
                .moduleCode(rs.getString("module_code"))
                .isNavigation(rs.getObject("is_navigation", Boolean.class))
                .isModifiable(rs.getObject("is_modifiable", Boolean.class))
                .appliesToAllRoles(rs.getObject("applies_to_all_roles", Boolean.class))
                .isObjectLevel(rs.getObject("is_object_level", Boolean.class))
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
