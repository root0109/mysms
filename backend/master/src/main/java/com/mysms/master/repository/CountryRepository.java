package com.mysms.master.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.master.config.MasterDatabaseConfig;
import com.mysms.master.domain.CountryEntity;
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
 * Plain-JDBC access to master.country (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only, keyset paging over an allowlisted order, and the row_version guard on every update and delete.
 * Master data is global: there is no tenant column.
 */
@Repository
public class CountryRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("iso2", "c.iso2", ValueType.STRING),
            field("iso3", "c.iso3", ValueType.STRING),
            field("name", "c.name", ValueType.STRING),
            field("shortName", "c.short_name", ValueType.STRING),
            field("phonePrefix", "c.phone_prefix", ValueType.STRING),
            field("sortOrder", "c.sort_order", ValueType.INTEGER),
            field("isActive", "c.is_active", ValueType.BOOLEAN),
            field("createTime", "c.created_at", ValueType.TIMESTAMP),
            field("updateTime", "c.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; expressions are NOT NULL so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("name", "c.name", ValueType.STRING),
            field("sortOrder", "COALESCE(c.sort_order, 2147483647)", ValueType.INTEGER),
            field("iso2", "COALESCE(c.iso2, '')", ValueType.STRING),
            field("createTime", "c.created_at", ValueType.TIMESTAMP),
            field("updateTime", "c.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "sortOrder,name";
    public static final String ID_COLUMN = "c.id";

    private static final Map<String, Function<CountryEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("name", CountryEntity::getName),
            Map.entry("sortOrder", e -> (long) (e.getSortOrder() == null ? Integer.MAX_VALUE : e.getSortOrder())),
            Map.entry("iso2", e -> e.getIso2() == null ? "" : e.getIso2()),
            Map.entry("createTime", CountryEntity::getCreatedAt),
            Map.entry("updateTime", CountryEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT c.id, c.iso2, c.iso3, c.name, c.short_name, c.phone_prefix, c.sort_order, c.is_active,
                   c.created_at, c.created_by, c.updated_at, c.updated_by, c.deleted_at, c.row_version
              FROM master.country c
             WHERE c.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND c.id = :id";
    private static final String EXISTS_LIVE = """
            SELECT EXISTS (SELECT 1 FROM master.country WHERE id = :id AND deleted_at IS NULL)
            """;
    private static final String INSERT = """
            INSERT INTO master.country
                   (id, iso2, iso3, name, short_name, phone_prefix, sort_order, is_active, created_at, created_by,
                    updated_at, updated_by, row_version)
            VALUES (:id, :iso2, :iso3, :name, :shortName, :phonePrefix, :sortOrder, :active, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            """;
    private static final String UPDATE = """
            UPDATE master.country
               SET iso2 = :iso2,
                   iso3 = :iso3,
                   name = :name,
                   short_name = :shortName,
                   phone_prefix = :phonePrefix,
                   sort_order = :sortOrder,
                   is_active = :active,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE master.country
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public CountryRepository(@Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CountryEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CountryRepository::map).optional();
    }

    public boolean existsLive(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LIVE).param("id", id).query(Boolean.class).single());
    }

    public Page<CountryEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CountryEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CountryRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CountryEntity::getId);
    }

    public void insert(CountryEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("iso2", e.getIso2())
                .param("iso3", e.getIso3())
                .param("name", e.getName())
                .param("shortName", e.getShortName())
                .param("phonePrefix", e.getPhonePrefix())
                .param("sortOrder", e.getSortOrder())
                .param("active", e.isActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CountryEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("iso2", e.getIso2())
                .param("iso3", e.getIso3())
                .param("name", e.getName())
                .param("shortName", e.getShortName())
                .param("phonePrefix", e.getPhonePrefix())
                .param("sortOrder", e.getSortOrder())
                .param("active", e.isActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CountryEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static final String ISO2_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM master.country WHERE iso2 = :iso2 AND id <> :id)
            """;
    private static final String HAS_LIVE_CHILDREN = """
            SELECT EXISTS (SELECT 1 FROM master.state_province WHERE country_id = :id AND deleted_at IS NULL)
                OR EXISTS (SELECT 1 FROM master.city WHERE country_id = :id AND deleted_at IS NULL)
            """;

    /** The iso2 unique key spans every row, soft-deleted ones included (uq_country_iso2). */
    public boolean iso2Taken(String iso2, UUID excludingId) {
        return Boolean.TRUE.equals(jdbc.sql(ISO2_TAKEN).param("iso2", iso2).param("id", excludingId)
                .query(Boolean.class).single());
    }

    public boolean hasLiveStatesOrCities(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(HAS_LIVE_CHILDREN).param("id", id).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, CountryEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CountryEntity map(ResultSet rs, int row) throws SQLException {
        return CountryEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .iso2(rs.getString("iso2"))
                .iso3(rs.getString("iso3"))
                .name(rs.getString("name"))
                .shortName(rs.getString("short_name"))
                .phonePrefix(rs.getString("phone_prefix"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
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
