package com.mysms.attendance.repository;
import com.mysms.attendance.config.AttendanceDatabaseConfig;
import com.mysms.attendance.domain.AbsenceReasonEntity;
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
 * Plain-JDBC access to attendance.absence_reason (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class AbsenceReasonRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("title", "r.title", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AbsenceReasonEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AbsenceReasonEntity::getCreatedAt),
            Map.entry("updateTime", AbsenceReasonEntity::getUpdatedAt),
            Map.entry("code", AbsenceReasonEntity::getCode),
            Map.entry("sortOrder", AbsenceReasonEntity::getSortOrder),
            Map.entry("title", AbsenceReasonEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.code, r.sort_order, r.title, r.is_active, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM attendance.absence_reason r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO attendance.absence_reason
            (id, tenant_id, code, sort_order, title, is_active, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :code, COALESCE(:sortOrder, 0), :title, COALESCE(:isActive, true), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, code, sort_order, title, is_active, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE attendance.absence_reason
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE attendance.absence_reason
               SET code = :code,
                   sort_order = :sortOrder,
                   title = :title,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ABSENCE_REASON_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM attendance.absence_reason
                            WHERE tenant_id = :tenantId
                              AND title = :title AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String UQ_ABSENCE_REASON_SCOPE_CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM attendance.absence_reason
                            WHERE tenant_id = :tenantId
                              AND code = :code AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_ATTENDANCE_RECORD_ABSENCE_REASON_ID = """
            SELECT EXISTS (SELECT 1 FROM attendance.attendance_record WHERE tenant_id = :tenantId
                AND absence_reason_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public AbsenceReasonRepository(@Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AbsenceReasonEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AbsenceReasonRepository::map).optional();
    }

    public Page<AbsenceReasonEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AbsenceReasonEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AbsenceReasonRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AbsenceReasonEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AbsenceReasonEntity insert(AbsenceReasonEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("code", e.getCode())
                .param("sortOrder", e.getSortOrder())
                .param("title", e.getTitle())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AbsenceReasonRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AbsenceReasonEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("sortOrder", e.getSortOrder())
                .param("title", e.getTitle())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AbsenceReasonEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_absence_reason_1: title where (deleted_at IS NULL). */
    public boolean takenTitle1(AbsenceReasonEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ABSENCE_REASON_1_TAKEN).param("title", e.getTitle())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** uq_absence_reason_scope_code: code where (deleted_at IS NULL). */
    public boolean takenCodeCode(AbsenceReasonEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ABSENCE_REASON_SCOPE_CODE_TAKEN).param("code", e.getCode())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live attendance.attendance_record rows still reference the row. */
    public boolean referencedByAttendanceRecordAbsenceReasonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ATTENDANCE_RECORD_ABSENCE_REASON_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, AbsenceReasonEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AbsenceReasonEntity map(ResultSet rs, int row) throws SQLException {
        return AbsenceReasonEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .code(rs.getString("code"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .title(rs.getString("title"))
                .isActive(rs.getObject("is_active", Boolean.class))
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
