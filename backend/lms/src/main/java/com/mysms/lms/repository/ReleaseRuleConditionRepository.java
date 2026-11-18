package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.ReleaseRuleConditionEntity;
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
 * Plain-JDBC access to lms.release_rule_condition: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ReleaseRuleConditionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("conditionType", "r.condition_type", ValueType.STRING),
            field("releaseRuleId", "r.release_rule_id", ValueType.UUID),
            field("operator", "r.operator", ValueType.STRING),
            field("value", "r.value", ValueType.STRING),
            field("value2", "r.value2", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ReleaseRuleConditionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ReleaseRuleConditionEntity::getCreatedAt),
            Map.entry("updateTime", ReleaseRuleConditionEntity::getUpdatedAt),
            Map.entry("sortOrder", ReleaseRuleConditionEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.condition_type, r.release_rule_id, r.operator, r.value, r.value2,
                   r.sort_order, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.release_rule_condition r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** The conditions of several rules, in order. */
    private static final String OF_RULES = SELECT_LIVE
            + " AND r.release_rule_id = ANY(:ruleIds) ORDER BY r.release_rule_id, r.sort_order, r.id";
    private static final String INSERT = """
            INSERT INTO lms.release_rule_condition
            (id, tenant_id, condition_type, release_rule_id, operator, value, value2, sort_order, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :conditionType, :releaseRuleId, :operator, :value, :value2,
                    COALESCE(:sortOrder, 0), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, condition_type, release_rule_id, operator, value, value2, sort_order, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.release_rule_condition
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.release_rule_condition
               SET condition_type = :conditionType,
                   operator = :operator,
                   value = :value,
                   value2 = :value2,
                   sort_order = :sortOrder,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_RELEASE_RULE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_rule WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.release_rule p0 JOIN lms.course p1 ON p1.tenant_id = :tenantId AND p1.id =
                p0.course_id WHERE p0.tenant_id = :tenantId
                AND p0.id =
            :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.release_rule_id IN (SELECT p0.id FROM lms.release_rule p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.course_id IS NULL OR
             p0.course_id IN (SELECT p1.id FROM lms.course p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR
             p1.institution_id IS NULL)))))
            """;

    private final JdbcClient jdbc;

    public ReleaseRuleConditionRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ReleaseRuleConditionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ReleaseRuleConditionRepository::map).optional();
    }

    public Page<ReleaseRuleConditionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ReleaseRuleConditionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ReleaseRuleConditionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ReleaseRuleConditionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    /** The live conditions of the rules, ordered within each rule. */
    public List<ReleaseRuleConditionEntity> ofRules(List<UUID> ruleIds) {
        return jdbc.sql(OF_RULES).param("ruleIds", ruleIds.toArray(UUID[]::new))
                .query(ReleaseRuleConditionRepository::map).list();
    }

    public ReleaseRuleConditionEntity insert(ReleaseRuleConditionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("conditionType", e.getConditionType())
                .param("releaseRuleId", e.getReleaseRuleId())
                .param("operator", e.getOperator())
                .param("value", e.getValue())
                .param("value2", e.getValue2())
                .param("sortOrder", e.getSortOrder())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ReleaseRuleConditionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ReleaseRuleConditionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("conditionType", e.getConditionType())
                .param("operator", e.getOperator())
                .param("value", e.getValue())
                .param("value2", e.getValue2())
                .param("sortOrder", e.getSortOrder())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ReleaseRuleConditionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.release_rule row exists (live). */
    public boolean existsReleaseRuleId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_RELEASE_RULE_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through release_rule_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.release_rule_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ReleaseRuleConditionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ReleaseRuleConditionEntity map(ResultSet rs, int row) throws SQLException {
        return ReleaseRuleConditionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .conditionType(rs.getString("condition_type"))
                .releaseRuleId(rs.getObject("release_rule_id", UUID.class))
                .operator(rs.getString("operator"))
                .value(rs.getString("value"))
                .value2(rs.getString("value2"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
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
