package com.mysms.platform.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.ReservedKeywordEntity;
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
 * Plain-JDBC access to platform.reserved_keyword (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. The table is global (no
 * tenant column, no row-level security): it is written only by
 * platform staff, through the operations whose scheme the edge enforces.
 */
@Repository
public class ReservedKeywordRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("keyword", "r.keyword", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ReservedKeywordEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ReservedKeywordEntity::getCreatedAt),
            Map.entry("updateTime", ReservedKeywordEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.keyword, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM platform.reserved_keyword r
             WHERE r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO platform.reserved_keyword
            (id, keyword, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :keyword, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, keyword, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE platform.reserved_keyword
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE platform.reserved_keyword
               SET keyword = :keyword,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_RESERVED_KEYWORD_KEYWORD_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM platform.reserved_keyword
                            WHERE keyword = :keyword AND id <> :id)
            """;

    private final JdbcClient jdbc;

    public ReservedKeywordRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ReservedKeywordEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ReservedKeywordRepository::map).optional();
    }

    public Page<ReservedKeywordEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ReservedKeywordEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ReservedKeywordRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ReservedKeywordEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ReservedKeywordEntity insert(ReservedKeywordEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("keyword", e.getKeyword())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ReservedKeywordRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ReservedKeywordEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("keyword", e.getKeyword())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ReservedKeywordEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_reserved_keyword_keyword: keyword. */
    public boolean takenKeywordKeyword(ReservedKeywordEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_RESERVED_KEYWORD_KEYWORD_TAKEN).param("keyword", e.getKeyword())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, ReservedKeywordEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ReservedKeywordEntity map(ResultSet rs, int row) throws SQLException {
        return ReservedKeywordEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .keyword(rs.getString("keyword"))
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
