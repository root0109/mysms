package com.mysms.comms.repository;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.NotificationTypeEntity;
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
 * Plain-JDBC access to comms.notification_type: schema-qualified constant SQL, named parameters, live rows only, keyset
 * paging over an allowlisted order and the row_version guard on every update and delete. The table is global (no tenant
 * column, no row-level security): it is written only by platform staff, through the operations whose scheme the edge
 * enforces.
 */
@Repository
public class NotificationTypeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("moduleCode", "r.module_code", ValueType.STRING),
            field("roleCode", "r.role_code", ValueType.STRING),
            field("isEnabled", "r.is_enabled", ValueType.BOOLEAN),
            field("popupEnabled", "r.popup_enabled", ValueType.BOOLEAN),
            field("teacherPopupEnabled", "r.teacher_popup_enabled", ValueType.BOOLEAN),
            field("validParameters", "r.valid_parameters", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<NotificationTypeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", NotificationTypeEntity::getCreatedAt),
            Map.entry("updateTime", NotificationTypeEntity::getUpdatedAt),
            Map.entry("code", NotificationTypeEntity::getCode));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.code, r.description, r.module_code, r.role_code, r.is_enabled, r.popup_enabled,
                   r.teacher_popup_enabled, r.valid_parameters, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM comms.notification_type r
             WHERE r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.notification_type
            (id, code, description, module_code, role_code, is_enabled, popup_enabled, teacher_popup_enabled,
             valid_parameters, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :code, :description, :moduleCode, :roleCode, COALESCE(:isEnabled, true),
                    COALESCE(:popupEnabled, false), COALESCE(:teacherPopupEnabled, false), :validParameters,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, code, description, module_code, role_code, is_enabled, popup_enabled, teacher_popup_enabled,
                      valid_parameters, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.notification_type
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.notification_type
               SET code = :code,
                   description = :description,
                   module_code = :moduleCode,
                   role_code = :roleCode,
                   is_enabled = :isEnabled,
                   popup_enabled = :popupEnabled,
                   teacher_popup_enabled = :teacherPopupEnabled,
                   valid_parameters = :validParameters,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_NOTIFICATION_TYPE_CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM comms.notification_type
                            WHERE code = :code AND id <> :id)
            """;
    private static final String REFERENCED_BY_NOTIFICATION_TYPE_PROPERTY_NOTIFICATION_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.notification_type_property WHERE notification_type_id = :id AND
                deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public NotificationTypeRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<NotificationTypeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(NotificationTypeRepository::map).optional();
    }

    public Page<NotificationTypeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<NotificationTypeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(NotificationTypeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), NotificationTypeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public NotificationTypeEntity insert(NotificationTypeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("description", e.getDescription())
                .param("moduleCode", e.getModuleCode())
                .param("roleCode", e.getRoleCode())
                .param("isEnabled", e.getIsEnabled())
                .param("popupEnabled", e.getPopupEnabled())
                .param("teacherPopupEnabled", e.getTeacherPopupEnabled())
                .param("validParameters", e.getValidParameters())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(NotificationTypeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(NotificationTypeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("description", e.getDescription())
                .param("moduleCode", e.getModuleCode())
                .param("roleCode", e.getRoleCode())
                .param("isEnabled", e.getIsEnabled())
                .param("popupEnabled", e.getPopupEnabled())
                .param("teacherPopupEnabled", e.getTeacherPopupEnabled())
                .param("validParameters", e.getValidParameters())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(NotificationTypeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_notification_type_code: code. */
    public boolean takenCodeCode(NotificationTypeEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_NOTIFICATION_TYPE_CODE_TAKEN).param("code", e.getCode())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live comms.notification_type_property rows still reference the row. */
    public boolean referencedByNotificationTypePropertyNotificationTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_NOTIFICATION_TYPE_PROPERTY_NOTIFICATION_TYPE_ID)
                .param("id", id).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, NotificationTypeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static NotificationTypeEntity map(ResultSet rs, int row) throws SQLException {
        return NotificationTypeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .code(rs.getString("code"))
                .description(rs.getString("description"))
                .moduleCode(rs.getString("module_code"))
                .roleCode(rs.getString("role_code"))
                .isEnabled(rs.getObject("is_enabled", Boolean.class))
                .popupEnabled(rs.getObject("popup_enabled", Boolean.class))
                .teacherPopupEnabled(rs.getObject("teacher_popup_enabled", Boolean.class))
                .validParameters(rs.getString("valid_parameters"))
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
