package com.mysms.campus.facility.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.campus.facility.config.FacilityDatabaseConfig;
import com.mysms.campus.facility.domain.AssetItemEntity;
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
 * Plain-JDBC access to facility.asset_item: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class AssetItemRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("unitOfMeasure", "r.unit_of_measure", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AssetItemEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AssetItemEntity::getCreatedAt),
            Map.entry("updateTime", AssetItemEntity::getUpdatedAt),
            Map.entry("sortOrder", AssetItemEntity::getSortOrder),
            Map.entry("name", AssetItemEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.sort_order, r.is_active, r.institution_id, r.code, r.name, r.unit_of_measure,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM facility.asset_item r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO facility.asset_item
            (id, tenant_id, sort_order, is_active, institution_id, code, name, unit_of_measure, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, COALESCE(:sortOrder, 0), COALESCE(:isActive, true), :institutionId, :code, :name,
                    COALESCE(:unitOfMeasure, 'unit'::character varying), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, sort_order, is_active, institution_id, code, name, unit_of_measure, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE facility.asset_item
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE facility.asset_item
               SET sort_order = :sortOrder,
                   is_active = :isActive,
                   institution_id = :institutionId,
                   code = :code,
                   name = :name,
                   unit_of_measure = :unitOfMeasure,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ASSET_ITEM_SCOPE_CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM facility.asset_item
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND code = :code AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String REFERENCED_BY_ROOM_ASSET_ASSET_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM facility.room_asset WHERE tenant_id = :tenantId
                AND asset_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public AssetItemRepository(@Qualifier(FacilityDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AssetItemEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AssetItemRepository::map).optional();
    }

    public Page<AssetItemEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AssetItemEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AssetItemRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AssetItemEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AssetItemEntity insert(AssetItemEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("unitOfMeasure", e.getUnitOfMeasure())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AssetItemRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AssetItemEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("unitOfMeasure", e.getUnitOfMeasure())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AssetItemEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_asset_item_scope_code: institution_id, code where (deleted_at IS NULL). */
    public boolean takenInstitutionIdCodeCode(AssetItemEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ASSET_ITEM_SCOPE_CODE_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("code", e.getCode())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live facility.room_asset rows still reference the row. */
    public boolean referencedByRoomAssetAssetItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ROOM_ASSET_ASSET_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, AssetItemEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AssetItemEntity map(ResultSet rs, int row) throws SQLException {
        return AssetItemEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .isActive(rs.getObject("is_active", Boolean.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .unitOfMeasure(rs.getString("unit_of_measure"))
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
