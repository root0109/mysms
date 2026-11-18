package com.mysms.library.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.domain.LibraryCardTypeEntity;
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
 * Plain-JDBC access to library.library_card_type: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class LibraryCardTypeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("name", "r.name", ValueType.STRING),
            field("maxItems", "r.max_items", ValueType.INTEGER),
            field("loanDays", "r.loan_days", ValueType.INTEGER),
            field("finePerDay", "r.fine_per_day", ValueType.DECIMAL),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LibraryCardTypeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LibraryCardTypeEntity::getCreatedAt),
            Map.entry("updateTime", LibraryCardTypeEntity::getUpdatedAt),
            Map.entry("code", LibraryCardTypeEntity::getCode),
            Map.entry("sortOrder", LibraryCardTypeEntity::getSortOrder),
            Map.entry("name", LibraryCardTypeEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.code, r.sort_order, r.is_active, r.name, r.max_items, r.loan_days,
                   r.fine_per_day, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM library.library_card_type r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO library.library_card_type
            (id, tenant_id, code, sort_order, is_active, name, max_items, loan_days, fine_per_day, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :code, COALESCE(:sortOrder, 0), COALESCE(:isActive, true), :name, :maxItems,
                    :loanDays, COALESCE(:finePerDay, 0), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, code, sort_order, is_active, name, max_items, loan_days, fine_per_day, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE library.library_card_type
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE library.library_card_type
               SET code = :code,
                   sort_order = :sortOrder,
                   is_active = :isActive,
                   name = :name,
                   max_items = :maxItems,
                   loan_days = :loanDays,
                   fine_per_day = :finePerDay,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_LIBRARY_CARD_TYPE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM library.library_card_type
                            WHERE tenant_id = :tenantId
                              AND name = :name AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String UQ_LIBRARY_CARD_TYPE_SCOPE_CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM library.library_card_type
                            WHERE tenant_id = :tenantId
                              AND code = :code AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_LIBRARY_CARD_CARD_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM library.library_card WHERE tenant_id = :tenantId
                AND card_type_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public LibraryCardTypeRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LibraryCardTypeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LibraryCardTypeRepository::map).optional();
    }

    public Page<LibraryCardTypeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LibraryCardTypeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LibraryCardTypeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LibraryCardTypeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LibraryCardTypeEntity insert(LibraryCardTypeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("code", e.getCode())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("name", e.getName())
                .param("maxItems", e.getMaxItems())
                .param("loanDays", e.getLoanDays())
                .param("finePerDay", e.getFinePerDay())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LibraryCardTypeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LibraryCardTypeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("name", e.getName())
                .param("maxItems", e.getMaxItems())
                .param("loanDays", e.getLoanDays())
                .param("finePerDay", e.getFinePerDay())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LibraryCardTypeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_library_card_type_1: name where (deleted_at IS NULL). */
    public boolean takenName1(LibraryCardTypeEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_LIBRARY_CARD_TYPE_1_TAKEN).param("name", e.getName())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** uq_library_card_type_scope_code: code where (deleted_at IS NULL). */
    public boolean takenCodeCode(LibraryCardTypeEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_LIBRARY_CARD_TYPE_SCOPE_CODE_TAKEN).param("code", e.getCode())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live library.library_card rows still reference the row. */
    public boolean referencedByLibraryCardCardTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LIBRARY_CARD_CARD_TYPE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, LibraryCardTypeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LibraryCardTypeEntity map(ResultSet rs, int row) throws SQLException {
        return LibraryCardTypeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .code(rs.getString("code"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .isActive(rs.getObject("is_active", Boolean.class))
                .name(rs.getString("name"))
                .maxItems(rs.getObject("max_items", Integer.class))
                .loanDays(rs.getObject("loan_days", Integer.class))
                .finePerDay(rs.getBigDecimal("fine_per_day"))
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
