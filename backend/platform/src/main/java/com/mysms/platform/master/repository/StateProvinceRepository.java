package com.mysms.platform.master.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.master.config.MasterDatabaseConfig;
import com.mysms.platform.master.domain.StateProvinceEntity;
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
 * Plain-JDBC access to master.state_province: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Master data is global: there is no tenant column.
 */
@Repository
public class StateProvinceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("countryId", "s.country_id", ValueType.UUID),
            field("code", "s.code", ValueType.STRING),
            field("name", "s.name", ValueType.STRING),
            field("isActive", "s.is_active", ValueType.BOOLEAN),
            field("createTime", "s.created_at", ValueType.TIMESTAMP),
            field("updateTime", "s.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; expressions are NOT NULL so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("name", "s.name", ValueType.STRING),
            field("createTime", "s.created_at", ValueType.TIMESTAMP),
            field("updateTime", "s.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "name";
    public static final String ID_COLUMN = "s.id";

    private static final Map<String, Function<StateProvinceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("name", StateProvinceEntity::getName),
            Map.entry("createTime", StateProvinceEntity::getCreatedAt),
            Map.entry("updateTime", StateProvinceEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT s.id, s.country_id, s.code, s.name, s.is_active, s.created_at, s.created_by, s.updated_at,
                   s.updated_by, s.deleted_at, s.row_version
              FROM master.state_province s
             WHERE s.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND s.id = :id";
    private static final String EXISTS_LIVE = """
            SELECT EXISTS (SELECT 1 FROM master.state_province WHERE id = :id AND deleted_at IS NULL)
            """;
    private static final String INSERT = """
            INSERT INTO master.state_province
                   (id, country_id, code, name, is_active, created_at, created_by, updated_at, updated_by,
                    row_version)
            VALUES (:id, :countryId, :code, :name, :active, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            """;
    private static final String UPDATE = """
            UPDATE master.state_province
               SET country_id = :countryId,
                   code = :code,
                   name = :name,
                   is_active = :active,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE master.state_province
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public StateProvinceRepository(@Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StateProvinceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StateProvinceRepository::map).optional();
    }

    public boolean existsLive(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LIVE).param("id", id).query(Boolean.class).single());
    }

    public Page<StateProvinceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StateProvinceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StateProvinceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StateProvinceEntity::getId);
    }

    public void insert(StateProvinceEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("countryId", e.getCountryId())
                .param("code", e.getCode())
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
    public int update(StateProvinceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("countryId", e.getCountryId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("active", e.isActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StateProvinceEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static final String NAME_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM master.state_province
                           WHERE country_id = :countryId AND name = :name AND id <> :id AND deleted_at IS NULL)
            """;
    private static final String CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM master.state_province
                           WHERE country_id = :countryId AND code = :code AND id <> :id AND deleted_at IS NULL)
            """;
    private static final String HAS_LIVE_CITIES = """
            SELECT EXISTS (SELECT 1 FROM master.city WHERE state_province_id = :id AND deleted_at IS NULL)
            """;

    /** uq_state_province_1: (country, name) among live rows. */
    public boolean nameTaken(UUID countryId, String name, UUID excludingId) {
        return Boolean.TRUE.equals(jdbc.sql(NAME_TAKEN).param("countryId", countryId).param("name", name)
                .param("id", excludingId).query(Boolean.class).single());
    }

    /** uq_state_province_code: (country, code) among live rows. */
    public boolean codeTaken(UUID countryId, String code, UUID excludingId) {
        return Boolean.TRUE.equals(jdbc.sql(CODE_TAKEN).param("countryId", countryId).param("code", code)
                .param("id", excludingId).query(Boolean.class).single());
    }

    public boolean hasLiveCities(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(HAS_LIVE_CITIES).param("id", id).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, StateProvinceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StateProvinceEntity map(ResultSet rs, int row) throws SQLException {
        return StateProvinceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .countryId(rs.getObject("country_id", UUID.class))
                .code(rs.getString("code"))
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
