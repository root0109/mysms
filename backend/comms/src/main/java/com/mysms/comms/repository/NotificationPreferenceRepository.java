package com.mysms.comms.repository;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.NotificationPreferenceEntity;
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
 * Plain-JDBC access to comms.notification_preference: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class NotificationPreferenceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("preferenceSetId", "r.preference_set_id", ValueType.UUID),
            field("notificationTypeId", "r.notification_type_id", ValueType.UUID),
            field("propertyName", "r.property_name", ValueType.STRING),
            field("propertyValue", "r.property_value", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<NotificationPreferenceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", NotificationPreferenceEntity::getCreatedAt),
            Map.entry("updateTime", NotificationPreferenceEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.preference_set_id, r.notification_type_id, r.property_name, r.property_value,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.notification_preference r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.notification_preference
            (id, tenant_id, preference_set_id, notification_type_id, property_name, property_value, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :preferenceSetId, :notificationTypeId, :propertyName, :propertyValue, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, preference_set_id, notification_type_id, property_name, property_value, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.notification_preference
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.notification_preference
               SET notification_type_id = :notificationTypeId,
                   property_name = :propertyName,
                   property_value = :propertyValue,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_NOTIFICATION_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.notification_type WHERE id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PREFERENCE_SET_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.notification_preference_set WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public NotificationPreferenceRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<NotificationPreferenceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(NotificationPreferenceRepository::map).optional();
    }

    public Page<NotificationPreferenceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<NotificationPreferenceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(NotificationPreferenceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), NotificationPreferenceEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public NotificationPreferenceEntity insert(NotificationPreferenceEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("preferenceSetId", e.getPreferenceSetId())
                .param("notificationTypeId", e.getNotificationTypeId())
                .param("propertyName", e.getPropertyName())
                .param("propertyValue", e.getPropertyValue())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(NotificationPreferenceRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(NotificationPreferenceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("notificationTypeId", e.getNotificationTypeId())
                .param("propertyName", e.getPropertyName())
                .param("propertyValue", e.getPropertyValue())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(NotificationPreferenceEntity e, int expectedVersion) {
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

    /** Whether the referenced comms.notification_preference_set row exists (live). */
    public boolean existsPreferenceSetId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PREFERENCE_SET_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.preference_set_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, NotificationPreferenceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static NotificationPreferenceEntity map(ResultSet rs, int row) throws SQLException {
        return NotificationPreferenceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .preferenceSetId(rs.getObject("preference_set_id", UUID.class))
                .notificationTypeId(rs.getObject("notification_type_id", UUID.class))
                .propertyName(rs.getString("property_name"))
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
