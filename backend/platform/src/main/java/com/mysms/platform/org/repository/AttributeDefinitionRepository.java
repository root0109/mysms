package com.mysms.platform.org.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.domain.AttributeDefinitionEntity;
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
 * Plain-JDBC access to org.attribute_definition: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class AttributeDefinitionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("dataType", "r.data_type", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AttributeDefinitionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AttributeDefinitionEntity::getCreatedAt),
            Map.entry("updateTime", AttributeDefinitionEntity::getUpdatedAt),
            Map.entry("name", AttributeDefinitionEntity::getName),
            Map.entry("sortOrder", AttributeDefinitionEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.name, r.data_type, r.sort_order, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM org.attribute_definition r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO org.attribute_definition
            (id, tenant_id, name, data_type, sort_order, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :name, COALESCE(:dataType, 'text'::character varying), COALESCE(:sortOrder, 0),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, name, data_type, sort_order, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE org.attribute_definition
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE org.attribute_definition
               SET name = :name,
                   data_type = :dataType,
                   sort_order = :sortOrder,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ATTRIBUTE_DEFINITION_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM org.attribute_definition
                            WHERE tenant_id = :tenantId
                              AND name = :name AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_INSTITUTION_ATTRIBUTE_ATTRIBUTE_DEFINITION_ID = """
            SELECT EXISTS (SELECT 1 FROM org.institution_attribute WHERE tenant_id = :tenantId
                AND attribute_definition_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_INSTITUTION_TYPE_ATTRIBUTE_ATTRIBUTE_DEFINITION_ID = """
            SELECT EXISTS (SELECT 1 FROM org.institution_type_attribute WHERE tenant_id = :tenantId
                AND attribute_definition_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public AttributeDefinitionRepository(@Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AttributeDefinitionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AttributeDefinitionRepository::map).optional();
    }

    public Page<AttributeDefinitionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AttributeDefinitionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AttributeDefinitionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AttributeDefinitionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AttributeDefinitionEntity insert(AttributeDefinitionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("name", e.getName())
                .param("dataType", e.getDataType())
                .param("sortOrder", e.getSortOrder())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AttributeDefinitionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AttributeDefinitionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("name", e.getName())
                .param("dataType", e.getDataType())
                .param("sortOrder", e.getSortOrder())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AttributeDefinitionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_attribute_definition_1: name where (deleted_at IS NULL). */
    public boolean takenName1(AttributeDefinitionEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ATTRIBUTE_DEFINITION_1_TAKEN).param("name", e.getName())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live org.institution_attribute rows still reference the row. */
    public boolean referencedByInstitutionAttributeAttributeDefinitionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_INSTITUTION_ATTRIBUTE_ATTRIBUTE_DEFINITION_ID)
                .param("id", id).query(Boolean.class).single());
    }

    /** Whether live org.institution_type_attribute rows still reference the row. */
    public boolean referencedByInstitutionTypeAttributeAttributeDefinitionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_INSTITUTION_TYPE_ATTRIBUTE_ATTRIBUTE_DEFINITION_ID)
                .param("id", id).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, AttributeDefinitionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AttributeDefinitionEntity map(ResultSet rs, int row) throws SQLException {
        return AttributeDefinitionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .name(rs.getString("name"))
                .dataType(rs.getString("data_type"))
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
