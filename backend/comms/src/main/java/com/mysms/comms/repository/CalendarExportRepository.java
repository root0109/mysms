package com.mysms.comms.repository;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.CalendarExportEntity;
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
 * Plain-JDBC access to comms.calendar_export: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CalendarExportRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("provider", "r.provider", ValueType.STRING),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("entityType", "r.entity_type", ValueType.STRING),
            field("entityId", "r.entity_id", ValueType.UUID),
            field("propertyName", "r.property_name", ValueType.STRING),
            field("propertyValue", "r.property_value", ValueType.STRING),
            field("isDeleted", "r.is_deleted", ValueType.BOOLEAN),
            field("lastExportedAt", "r.last_exported_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CalendarExportEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CalendarExportEntity::getCreatedAt),
            Map.entry("updateTime", CalendarExportEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_id, r.provider, r.cohort_id, r.entity_type, r.entity_id, r.property_name,
                   r.property_value, r.is_deleted, r.last_exported_at, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM comms.calendar_export r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.calendar_export
            (id, tenant_id, user_id, provider, cohort_id, entity_type, entity_id, property_name, property_value,
             is_deleted, last_exported_at, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :provider, :cohortId, :entityType, :entityId, :propertyName,
                    :propertyValue, COALESCE(:isDeleted, false), :lastExportedAt, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_id, provider, cohort_id, entity_type, entity_id, property_name,
                      property_value, is_deleted, last_exported_at, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.calendar_export
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.calendar_export
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_CALENDAR_EXPORT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM comms.calendar_export
                            WHERE tenant_id = :tenantId
                              AND user_id = :userId AND provider = :provider AND entity_type = :entityType AND entity_id
                                  = :entityId AND property_name = :propertyName AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;

    private final JdbcClient jdbc;

    public CalendarExportRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CalendarExportEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CalendarExportRepository::map).optional();
    }

    public Page<CalendarExportEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CalendarExportEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CalendarExportRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CalendarExportEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CalendarExportEntity insert(CalendarExportEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("provider", e.getProvider())
                .param("cohortId", e.getCohortId())
                .param("entityType", e.getEntityType())
                .param("entityId", e.getEntityId())
                .param("propertyName", e.getPropertyName())
                .param("propertyValue", e.getPropertyValue())
                .param("isDeleted", e.getIsDeleted())
                .param("lastExportedAt", e.getLastExportedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CalendarExportRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CalendarExportEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CalendarExportEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_calendar_export_1: user_id, provider, entity_type, entity_id, property_name where (deleted_at IS NULL). */
    public boolean takenUserIdProviderEntityTypeEntityIdPropertyName1(CalendarExportEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_CALENDAR_EXPORT_1_TAKEN)
                .param("userId", e.getUserId())
                .param("provider", e.getProvider())
                .param("entityType", e.getEntityType())
                .param("entityId", e.getEntityId())
                .param("propertyName", e.getPropertyName())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, CalendarExportEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CalendarExportEntity map(ResultSet rs, int row) throws SQLException {
        return CalendarExportEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .provider(rs.getString("provider"))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .entityType(rs.getString("entity_type"))
                .entityId(rs.getObject("entity_id", UUID.class))
                .propertyName(rs.getString("property_name"))
                .propertyValue(rs.getString("property_value"))
                .isDeleted(rs.getObject("is_deleted", Boolean.class))
                .lastExportedAt(rs.getObject("last_exported_at", OffsetDateTime.class))
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
