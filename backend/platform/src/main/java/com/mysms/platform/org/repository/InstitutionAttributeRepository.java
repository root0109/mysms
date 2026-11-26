package com.mysms.platform.org.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.domain.InstitutionAttributeEntity;
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
 * Plain-JDBC access to org.institution_attribute: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class InstitutionAttributeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("attrKey", "r.attr_key", ValueType.STRING),
            field("attrValue", "r.attr_value", ValueType.STRING),
            field("attributeDefinitionId", "r.attribute_definition_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<InstitutionAttributeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", InstitutionAttributeEntity::getCreatedAt),
            Map.entry("updateTime", InstitutionAttributeEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.attr_key, r.attr_value, r.attribute_definition_id,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM org.institution_attribute r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO org.institution_attribute
            (id, tenant_id, institution_id, attr_key, attr_value, attribute_definition_id, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :attrKey, :attrValue, :attributeDefinitionId, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, attr_key, attr_value, attribute_definition_id, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE org.institution_attribute
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE org.institution_attribute
               SET attr_key = :attrKey,
                   attr_value = :attrValue,
                   attribute_definition_id = :attributeDefinitionId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_INSTITUTION_ATTRIBUTE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM org.institution_attribute
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND attr_key = :attrKey AND id <> :id AND ((deleted_at
                                  IS NULL)))
            """;
    private static final String EXISTS_ATTRIBUTE_DEFINITION_ID = """
            SELECT EXISTS (SELECT 1 FROM org.attribute_definition WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_INSTITUTION_ID = """
            SELECT EXISTS (SELECT 1 FROM org.institution WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public InstitutionAttributeRepository(@Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<InstitutionAttributeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(InstitutionAttributeRepository::map).optional();
    }

    public Page<InstitutionAttributeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<InstitutionAttributeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(InstitutionAttributeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), InstitutionAttributeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public InstitutionAttributeEntity insert(InstitutionAttributeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("attrKey", e.getAttrKey())
                .param("attrValue", e.getAttrValue())
                .param("attributeDefinitionId", e.getAttributeDefinitionId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(InstitutionAttributeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(InstitutionAttributeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("attrKey", e.getAttrKey())
                .param("attrValue", e.getAttrValue())
                .param("attributeDefinitionId", e.getAttributeDefinitionId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(InstitutionAttributeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_institution_attribute_1: institution_id, attr_key where (deleted_at IS NULL). */
    public boolean takenInstitutionIdAttrKey1(InstitutionAttributeEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_INSTITUTION_ATTRIBUTE_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("attrKey", e.getAttrKey())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced org.attribute_definition row exists (live). */
    public boolean existsAttributeDefinitionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ATTRIBUTE_DEFINITION_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced org.institution row exists (live). */
    public boolean existsInstitutionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_INSTITUTION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.institution_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, InstitutionAttributeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static InstitutionAttributeEntity map(ResultSet rs, int row) throws SQLException {
        return InstitutionAttributeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .attrKey(rs.getString("attr_key"))
                .attrValue(rs.getString("attr_value"))
                .attributeDefinitionId(rs.getObject("attribute_definition_id", UUID.class))
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
