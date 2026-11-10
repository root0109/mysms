package com.mysms.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.BadgeEntity;
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
 * Plain-JDBC access to lms.badge (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class BadgeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("imageFileId", "r.image_file_id", ValueType.UUID),
            field("targetEntity", "r.target_entity", ValueType.STRING),
            field("triggerEvent", "r.trigger_event", ValueType.STRING),
            field("conditionExpr", "r.condition_expr", ValueType.STRING),
            field("fieldToCheck", "r.field_to_check", ValueType.STRING),
            field("userField", "r.user_field", ValueType.STRING),
            field("grantOnlyOnce", "r.grant_only_once", ValueType.BOOLEAN),
            field("isEnabled", "r.is_enabled", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<BadgeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", BadgeEntity::getCreatedAt),
            Map.entry("updateTime", BadgeEntity::getUpdatedAt),
            Map.entry("title", BadgeEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.title, r.description, r.image_file_id, r.target_entity, r.trigger_event,
                   r.condition_expr, r.field_to_check, r.user_field, r.grant_only_once, r.is_enabled, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.badge r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.badge
            (id, tenant_id, title, description, image_file_id, target_entity, trigger_event, condition_expr,
             field_to_check, user_field, grant_only_once, is_enabled, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :title, :description, :imageFileId, :targetEntity, :triggerEvent, :conditionExpr,
                    :fieldToCheck, :userField, COALESCE(:grantOnlyOnce, false), COALESCE(:isEnabled, true),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, title, description, image_file_id, target_entity, trigger_event, condition_expr,
                      field_to_check, user_field, grant_only_once, is_enabled, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.badge
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.badge
               SET title = :title,
                   description = :description,
                   image_file_id = :imageFileId,
                   target_entity = :targetEntity,
                   trigger_event = :triggerEvent,
                   condition_expr = :conditionExpr,
                   field_to_check = :fieldToCheck,
                   user_field = :userField,
                   grant_only_once = :grantOnlyOnce,
                   is_enabled = :isEnabled,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_BADGE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.badge
                            WHERE tenant_id = :tenantId
                              AND title = :title AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_BADGE_AWARD_BADGE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.badge_award WHERE tenant_id = :tenantId
                AND badge_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public BadgeRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<BadgeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(BadgeRepository::map).optional();
    }

    public Page<BadgeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<BadgeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(BadgeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), BadgeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public BadgeEntity insert(BadgeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("imageFileId", e.getImageFileId())
                .param("targetEntity", e.getTargetEntity())
                .param("triggerEvent", e.getTriggerEvent())
                .param("conditionExpr", e.getConditionExpr())
                .param("fieldToCheck", e.getFieldToCheck())
                .param("userField", e.getUserField())
                .param("grantOnlyOnce", e.getGrantOnlyOnce())
                .param("isEnabled", e.getIsEnabled())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(BadgeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(BadgeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("imageFileId", e.getImageFileId())
                .param("targetEntity", e.getTargetEntity())
                .param("triggerEvent", e.getTriggerEvent())
                .param("conditionExpr", e.getConditionExpr())
                .param("fieldToCheck", e.getFieldToCheck())
                .param("userField", e.getUserField())
                .param("grantOnlyOnce", e.getGrantOnlyOnce())
                .param("isEnabled", e.getIsEnabled())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(BadgeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_badge_1: title where (deleted_at IS NULL). */
    public boolean takenTitle1(BadgeEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_BADGE_1_TAKEN).param("title", e.getTitle()).param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live lms.badge_award rows still reference the row. */
    public boolean referencedByBadgeAwardBadgeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_BADGE_AWARD_BADGE_ID).param("id", id).query(Boolean.class)
                .single());
    }

    private static List<Object> sortValues(SortSpec sort, BadgeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static BadgeEntity map(ResultSet rs, int row) throws SQLException {
        return BadgeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .imageFileId(rs.getObject("image_file_id", UUID.class))
                .targetEntity(rs.getString("target_entity"))
                .triggerEvent(rs.getString("trigger_event"))
                .conditionExpr(rs.getString("condition_expr"))
                .fieldToCheck(rs.getString("field_to_check"))
                .userField(rs.getString("user_field"))
                .grantOnlyOnce(rs.getObject("grant_only_once", Boolean.class))
                .isEnabled(rs.getObject("is_enabled", Boolean.class))
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
