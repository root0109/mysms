package com.mysms.academics.repository;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.domain.ProgramLevelEntity;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
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
 * Plain-JDBC access to academics.program_level: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class ProgramLevelRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("name", "r.name", ValueType.STRING),
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

    private static final Map<String, Function<ProgramLevelEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ProgramLevelEntity::getCreatedAt),
            Map.entry("updateTime", ProgramLevelEntity::getUpdatedAt),
            Map.entry("code", ProgramLevelEntity::getCode),
            Map.entry("sortOrder", ProgramLevelEntity::getSortOrder),
            Map.entry("name", ProgramLevelEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.code, r.sort_order, r.is_active, r.name, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM academics.program_level r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO academics.program_level
            (id, tenant_id, code, sort_order, is_active, name, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :code, COALESCE(:sortOrder, 0), COALESCE(:isActive, true), :name, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, code, sort_order, is_active, name, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE academics.program_level
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE academics.program_level
               SET code = :code,
                   sort_order = :sortOrder,
                   is_active = :isActive,
                   name = :name,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PROGRAM_LEVEL_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM academics.program_level
                            WHERE tenant_id = :tenantId
                              AND name = :name AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String UQ_PROGRAM_LEVEL_SCOPE_CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM academics.program_level
                            WHERE tenant_id = :tenantId
                              AND code = :code AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_PROGRAM_PROGRAM_LEVEL_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.program WHERE tenant_id = :tenantId
                AND program_level_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public ProgramLevelRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ProgramLevelEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ProgramLevelRepository::map).optional();
    }

    public Page<ProgramLevelEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ProgramLevelEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ProgramLevelRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ProgramLevelEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ProgramLevelEntity insert(ProgramLevelEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("code", e.getCode())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("name", e.getName())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ProgramLevelRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ProgramLevelEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("name", e.getName())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ProgramLevelEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_program_level_1: name where (deleted_at IS NULL). */
    public boolean takenName1(ProgramLevelEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PROGRAM_LEVEL_1_TAKEN).param("name", e.getName()).param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** uq_program_level_scope_code: code where (deleted_at IS NULL). */
    public boolean takenCodeCode(ProgramLevelEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PROGRAM_LEVEL_SCOPE_CODE_TAKEN).param("code", e.getCode())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live academics.program rows still reference the row. */
    public boolean referencedByProgramProgramLevelId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PROGRAM_PROGRAM_LEVEL_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, ProgramLevelEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ProgramLevelEntity map(ResultSet rs, int row) throws SQLException {
        return ProgramLevelEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .code(rs.getString("code"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .isActive(rs.getObject("is_active", Boolean.class))
                .name(rs.getString("name"))
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
