package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.ApiPermissionEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to iam.api_permission. The table is global (no tenant column, no row-level security): it is written
 * only from the modules' published catalogues and read only by platform staff, through the operation whose scheme the
 * edge enforces. A catalogue is applied in two set-based statements, never one per operation; schemes are stored
 * space-separated (empty for a provider's signed callback).
 */
@Repository
public class ApiPermissionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "a.id", ValueType.UUID),
            field("moduleCode", "a.module_code", ValueType.STRING),
            field("operationId", "a.operation_id", ValueType.STRING),
            field("httpMethod", "a.http_method", ValueType.STRING),
            field("permissionCode", "a.permission_code", ValueType.STRING),
            field("audience", "a.audience", ValueType.STRING),
            field("isAnonymous", "a.is_anonymous", ValueType.BOOLEAN));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("operationId", "a.operation_id", ValueType.STRING),
            field("lastSeenTime", "a.last_seen_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "operationId";
    public static final String ID_COLUMN = "a.id";

    private static final Map<String, Function<ApiPermissionEntity, Object>> SORT_VALUES = Map.of(
            "operationId", ApiPermissionEntity::getOperationId,
            "lastSeenTime", ApiPermissionEntity::getLastSeenAt);

    private static final String SELECT_LIVE = """
            SELECT a.id, a.module_code, a.operation_id, a.http_method, a.path_template, a.permission_code, a.schemes,
                   a.is_anonymous, a.audience, a.release, a.catalogue_sha256, a.first_seen_at, a.last_seen_at,
                   a.retired_at, a.created_at, a.created_by, a.updated_at, a.updated_by, a.deleted_at, a.row_version
              FROM iam.api_permission a
             WHERE a.deleted_at IS NULL
            """;
    /** New operations are inserted; known ones take the catalogue's values and are published again. */
    private static final String UPSERT = """
            INSERT INTO iam.api_permission
            (id, module_code, operation_id, http_method, path_template, permission_code, schemes, is_anonymous,
             audience, release, catalogue_sha256, first_seen_at, last_seen_at, created_at, updated_at, row_version)
            SELECT op.id, :module, op.operation_id, op.http_method, op.path_template, op.permission_code, op.schemes,
                   op.is_anonymous, op.audience, :release, :sha256, :now, :now, :now, :now, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:operationIds AS varchar[]), CAST(:methods AS varchar[]),
                          CAST(:paths AS varchar[]), CAST(:permissions AS varchar[]), CAST(:schemes AS varchar[]),
                          CAST(:anonymous AS boolean[]), CAST(:audiences AS varchar[]))
                   AS op (id, operation_id, http_method, path_template, permission_code, schemes, is_anonymous,
                          audience)
            ON CONFLICT (operation_id) WHERE deleted_at IS NULL DO UPDATE
               SET module_code = EXCLUDED.module_code,
                   http_method = EXCLUDED.http_method,
                   path_template = EXCLUDED.path_template,
                   permission_code = EXCLUDED.permission_code,
                   schemes = EXCLUDED.schemes,
                   is_anonymous = EXCLUDED.is_anonymous,
                   audience = EXCLUDED.audience,
                   release = EXCLUDED.release,
                   catalogue_sha256 = EXCLUDED.catalogue_sha256,
                   last_seen_at = EXCLUDED.last_seen_at,
                   retired_at = NULL,
                   updated_at = EXCLUDED.updated_at,
                   row_version = iam.api_permission.row_version + 1
            """;
    /**
     * The module's operations no catalogue has carried since the cutoff: retired, never deleted. Retiring by age, not
     * by one catalogue, keeps an older replica's catalogue (a rolling deploy, a late start) from retiring what the
     * newer release still serves.
     */
    private static final String RETIRE_UNSEEN = """
            UPDATE iam.api_permission
               SET retired_at = :now,
                   updated_at = :now,
                   row_version = row_version + 1
             WHERE module_code = :module AND last_seen_at < :cutoff
               AND retired_at IS NULL AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public ApiPermissionRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Page<ApiPermissionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ApiPermissionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ApiPermissionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ApiPermissionEntity::getId);
    }

    /**
     * Applies one module's catalogue: every operation upserted as seen now (ids are used for new rows only), then the
     * module's operations not seen since the cutoff retired.
     *
     * @return the number of operations retired
     */
    public int apply(String module, String release, String sha256, List<ApiPermissionEntity> operations,
                     OffsetDateTime now, OffsetDateTime cutoff) {
        jdbc.sql(UPSERT)
                .param("module", module)
                .param("release", release)
                .param("sha256", sha256)
                .param("now", now)
                .param("ids", operations.stream().map(ApiPermissionEntity::getId).toArray(UUID[]::new))
                .param("operationIds", column(operations, ApiPermissionEntity::getOperationId))
                .param("methods", column(operations, ApiPermissionEntity::getHttpMethod))
                .param("paths", column(operations, ApiPermissionEntity::getPathTemplate))
                .param("permissions", column(operations, ApiPermissionEntity::getPermissionCode))
                .param("schemes", column(operations, ApiPermissionEntity::getSchemes))
                .param("anonymous", operations.stream().map(ApiPermissionEntity::getIsAnonymous)
                        .toArray(Boolean[]::new))
                .param("audiences", column(operations, ApiPermissionEntity::getAudience))
                .update();
        return jdbc.sql(RETIRE_UNSEEN).param("module", module).param("cutoff", cutoff).param("now", now).update();
    }

    private static String[] column(List<ApiPermissionEntity> operations, Function<ApiPermissionEntity, String> value) {
        return operations.stream().map(value).toArray(String[]::new);
    }

    private static List<Object> sortValues(SortSpec sort, ApiPermissionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ApiPermissionEntity map(ResultSet rs, int row) throws SQLException {
        return ApiPermissionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .moduleCode(rs.getString("module_code"))
                .operationId(rs.getString("operation_id"))
                .httpMethod(rs.getString("http_method"))
                .pathTemplate(rs.getString("path_template"))
                .permissionCode(rs.getString("permission_code"))
                .schemes(rs.getString("schemes"))
                .isAnonymous(rs.getBoolean("is_anonymous"))
                .audience(rs.getString("audience"))
                .release(rs.getString("release"))
                .catalogueSha256(rs.getString("catalogue_sha256"))
                .firstSeenAt(rs.getObject("first_seen_at", OffsetDateTime.class))
                .lastSeenAt(rs.getObject("last_seen_at", OffsetDateTime.class))
                .retiredAt(rs.getObject("retired_at", OffsetDateTime.class))
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
