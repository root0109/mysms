package com.mysms.master.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.master.config.MasterDatabaseConfig;
import com.mysms.master.domain.CurrencyEntity;
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
 * Plain-JDBC access to master.currency (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only, keyset paging over an allowlisted order, and the row_version guard on every update and delete.
 * Master data is global: there is no tenant column.
 */
@Repository
public class CurrencyRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("symbol", "r.symbol", ValueType.STRING),
            field("minorUnits", "r.minor_units", ValueType.INTEGER),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; expressions are NOT NULL so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("name", "r.name", ValueType.STRING),
            field("code", "r.code", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "name";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CurrencyEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("name", CurrencyEntity::getName),
            Map.entry("code", CurrencyEntity::getCode),
            Map.entry("createTime", CurrencyEntity::getCreatedAt),
            Map.entry("updateTime", CurrencyEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.code, r.name, r.symbol, r.minor_units, r.is_active, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM master.currency r
             WHERE r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String EXISTS_LIVE = """
            SELECT EXISTS (SELECT 1 FROM master.currency WHERE id = :id AND deleted_at IS NULL)
            """;
    private static final String INSERT = """
            INSERT INTO master.currency
                   (id, code, name, symbol, minor_units, is_active, created_at, created_by, updated_at, updated_by,
                    row_version)
            VALUES (:id, :code, :name, :symbol, :minorUnits, :active, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            """;
    private static final String UPDATE = """
            UPDATE master.currency
               SET code = :code,
                   name = :name,
                   symbol = :symbol,
                   minor_units = :minorUnits,
                   is_active = :active,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE master.currency
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public CurrencyRepository(@Qualifier(MasterDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CurrencyEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CurrencyRepository::map).optional();
    }

    public boolean existsLive(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LIVE).param("id", id).query(Boolean.class).single());
    }

    public Page<CurrencyEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CurrencyEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CurrencyRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CurrencyEntity::getId);
    }

    public void insert(CurrencyEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("symbol", e.getSymbol())
                .param("minorUnits", e.getMinorUnits())
                .param("active", e.isActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CurrencyEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("symbol", e.getSymbol())
                .param("minorUnits", e.getMinorUnits())
                .param("active", e.isActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CurrencyEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static final String CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM master.currency WHERE code = :code AND id <> :id)
            """;

    /** The code unique key spans every row, soft-deleted ones included (uq_currency_code). */
    public boolean codeTaken(String code, UUID excludingId) {
        return Boolean.TRUE.equals(jdbc.sql(CODE_TAKEN).param("code", code).param("id", excludingId)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, CurrencyEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CurrencyEntity map(ResultSet rs, int row) throws SQLException {
        return CurrencyEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .symbol(rs.getString("symbol"))
                .minorUnits(rs.getObject("minor_units", Integer.class))
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
