package com.mysms.master.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.master.config.MasterDatabaseConfig;
import com.mysms.master.domain.CityEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to master.city (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only, keyset paging over an allowlisted order, and the row_version guard on every update and delete.
 * Master data is global: there is no tenant column.
 */
@Repository
public class CityRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("countryId", "t.country_id", ValueType.UUID),
            field("stateProvinceId", "t.state_province_id", ValueType.UUID),
            field("name", "t.name", ValueType.STRING),
            field("isActive", "t.is_active", ValueType.BOOLEAN),
            field("createTime", "t.created_at", ValueType.TIMESTAMP),
            field("updateTime", "t.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; expressions are NOT NULL so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("name", "t.name", ValueType.STRING),
            field("createTime", "t.created_at", ValueType.TIMESTAMP),
            field("updateTime", "t.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "name";
    public static final String ID_COLUMN = "t.id";

    private static final Map<String, Function<CityEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("name", CityEntity::getName),
            Map.entry("createTime", CityEntity::getCreatedAt),
            Map.entry("updateTime", CityEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT t.id, t.country_id, t.state_province_id, t.name, t.is_active, t.created_at, t.created_by,
                   t.updated_at, t.updated_by, t.deleted_at, t.row_version
              FROM master.city t
             WHERE t.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND t.id = :id";
    private static final String EXISTS_LIVE = """
            SELECT EXISTS (SELECT 1 FROM master.city WHERE id = :id AND deleted_at IS NULL)
            """;
    private static final String INSERT = """
            INSERT INTO master.city
                   (id, country_id, state_province_id, name, is_active, created_at, created_by, updated_at,
                    updated_by, row_version)
            VALUES (:id, :countryId, :stateProvinceId, :name, :active, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            """;
    private static final String UPDATE = """
            UPDATE master.city
               SET country_id = :countryId,
                   state_province_id = :stateProvinceId,
                   name = :name,
                   is_active = :active,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE master.city
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** totalSize is exact up to 10,000 (api/README "Pagination"); the count never scans past that. */
    private static final int COUNT_CAP = 10_001;

    private final JdbcClient jdbc;

    public CityRepository(@Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CityEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CityRepository::map).optional();
    }

    public boolean existsLive(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LIVE).param("id", id).query(Boolean.class).single());
    }

    public Page<CityEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + " LIMIT :limit";
        List<CityEntity> rows = jdbc.sql(sql).params(where.params()).param("limit", page.limit())
                .query(CityRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CityEntity::getId);
    }

    public int count(SqlFragment filter) {
        String sql = "SELECT count(*) FROM (" + SELECT_LIVE + " AND (" + filter.sql() + ") LIMIT :cap) counted";
        return jdbc.sql(sql).params(filter.params()).param("cap", COUNT_CAP).query(Integer.class).single();
    }

    public void insert(CityEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("countryId", e.getCountryId())
                .param("stateProvinceId", e.getStateProvinceId())
                .param("name", e.getName())
                .param("active", e.isActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CityEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("countryId", e.getCountryId())
                .param("stateProvinceId", e.getStateProvinceId())
                .param("name", e.getName())
                .param("active", e.isActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CityEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static List<Object> sortValues(SortSpec sort, CityEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CityEntity map(ResultSet rs, int row) throws SQLException {
        return CityEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .countryId(rs.getObject("country_id", UUID.class))
                .stateProvinceId(rs.getObject("state_province_id", UUID.class))
                .name(rs.getString("name"))
                .active(rs.getBoolean("is_active"))
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
        return java.util.Arrays.stream(specs).collect(java.util.stream.Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
