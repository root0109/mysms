package com.mysms.comms.repository;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.NotificationTypePropertyEntity;
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
 * Plain-JDBC access to comms.notification_type_property (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. The table is global (no
 * tenant column, no row-level security): it is written only by
 * platform staff, through the operations whose scheme the edge enforces.
 */
@Repository
public class NotificationTypePropertyRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("notificationTypeId", "r.notification_type_id", ValueType.UUID),
            field("moduleCode", "r.module_code", ValueType.STRING),
            field("propertyValue", "r.property_value", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<NotificationTypePropertyEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", NotificationTypePropertyEntity::getCreatedAt),
            Map.entry("updateTime", NotificationTypePropertyEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.notification_type_id, r.module_code, r.property_value, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.notification_type_property r
             WHERE r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.notification_type_property
            (id, notification_type_id, module_code, property_value, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :notificationTypeId, :moduleCode, :propertyValue, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, notification_type_id, module_code, property_value, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.notification_type_property
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.notification_type_property
               SET module_code = :moduleCode,
                   property_value = :propertyValue,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_NOTIFICATION_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.notification_type WHERE id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public NotificationTypePropertyRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<NotificationTypePropertyEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(NotificationTypePropertyRepository::map).optional();
    }

    public Page<NotificationTypePropertyEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<NotificationTypePropertyEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(NotificationTypePropertyRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), NotificationTypePropertyEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public NotificationTypePropertyEntity insert(NotificationTypePropertyEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("notificationTypeId", e.getNotificationTypeId())
                .param("moduleCode", e.getModuleCode())
                .param("propertyValue", e.getPropertyValue())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(NotificationTypePropertyRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(NotificationTypePropertyEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("moduleCode", e.getModuleCode())
                .param("propertyValue", e.getPropertyValue())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(NotificationTypePropertyEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced comms.notification_type row exists (live). */
    public boolean existsNotificationTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_NOTIFICATION_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.notification_type_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, NotificationTypePropertyEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static NotificationTypePropertyEntity map(ResultSet rs, int row) throws SQLException {
        return NotificationTypePropertyEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .notificationTypeId(rs.getObject("notification_type_id", UUID.class))
                .moduleCode(rs.getString("module_code"))
                .propertyValue(rs.getString("property_value"))
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
