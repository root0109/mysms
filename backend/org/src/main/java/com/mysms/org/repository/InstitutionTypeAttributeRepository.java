package com.mysms.org.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.org.config.OrgDatabaseConfig;
import com.mysms.org.domain.InstitutionTypeAttributeEntity;
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
 * Plain-JDBC access to org.institution_type_attribute: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class InstitutionTypeAttributeRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionTypeId", "r.institution_type_id", ValueType.UUID),
            field("attributeDefinitionId", "r.attribute_definition_id", ValueType.UUID),
            field("isRequired", "r.is_required", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<InstitutionTypeAttributeEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", InstitutionTypeAttributeEntity::getCreatedAt),
            Map.entry("updateTime", InstitutionTypeAttributeEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_type_id, r.attribute_definition_id, r.is_required, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM org.institution_type_attribute r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO org.institution_type_attribute
            (id, tenant_id, institution_type_id, attribute_definition_id, is_required, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionTypeId, :attributeDefinitionId, COALESCE(:isRequired, false),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_type_id, attribute_definition_id, is_required, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE org.institution_type_attribute
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE org.institution_type_attribute
               SET attribute_definition_id = :attributeDefinitionId,
                   is_required = :isRequired,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_INSTITUTION_TYPE_ATTRIBUTE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM org.institution_type_attribute
                            WHERE tenant_id = :tenantId
                              AND institution_type_id = :institutionTypeId AND attribute_definition_id =
                                  :attributeDefinitionId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_ATTRIBUTE_DEFINITION_ID = """
            SELECT EXISTS (SELECT 1 FROM org.attribute_definition WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_INSTITUTION_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM org.institution_type WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public InstitutionTypeAttributeRepository(@Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<InstitutionTypeAttributeEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(InstitutionTypeAttributeRepository::map).optional();
    }

    public Page<InstitutionTypeAttributeEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<InstitutionTypeAttributeEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(InstitutionTypeAttributeRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), InstitutionTypeAttributeEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public InstitutionTypeAttributeEntity insert(InstitutionTypeAttributeEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionTypeId", e.getInstitutionTypeId())
                .param("attributeDefinitionId", e.getAttributeDefinitionId())
                .param("isRequired", e.getIsRequired())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(InstitutionTypeAttributeRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(InstitutionTypeAttributeEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("attributeDefinitionId", e.getAttributeDefinitionId())
                .param("isRequired", e.getIsRequired())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(InstitutionTypeAttributeEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_institution_type_attribute_1: institution_type_id, attribute_definition_id where (deleted_at IS NULL). */
    public boolean takenInstitutionTypeIdAttributeDefinitionId1(InstitutionTypeAttributeEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_INSTITUTION_TYPE_ATTRIBUTE_1_TAKEN)
                .param("institutionTypeId", e.getInstitutionTypeId())
                .param("attributeDefinitionId", e.getAttributeDefinitionId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced org.attribute_definition row exists (live). */
    public boolean existsAttributeDefinitionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ATTRIBUTE_DEFINITION_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced org.institution_type row exists (live). */
    public boolean existsInstitutionTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_INSTITUTION_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.institution_type_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, InstitutionTypeAttributeEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static InstitutionTypeAttributeEntity map(ResultSet rs, int row) throws SQLException {
        return InstitutionTypeAttributeEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionTypeId(rs.getObject("institution_type_id", UUID.class))
                .attributeDefinitionId(rs.getObject("attribute_definition_id", UUID.class))
                .isRequired(rs.getObject("is_required", Boolean.class))
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
