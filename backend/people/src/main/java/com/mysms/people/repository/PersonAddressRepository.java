package com.mysms.people.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.domain.PersonAddressEntity;
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
 * Plain-JDBC access to people.person_address (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class PersonAddressRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("personId", "r.person_id", ValueType.UUID),
            field("addressType", "r.address_type", ValueType.STRING),
            field("line1", "r.line1", ValueType.STRING),
            field("line2", "r.line2", ValueType.STRING),
            field("cityId", "r.city_id", ValueType.UUID),
            field("cityName", "r.city_name", ValueType.STRING),
            field("stateProvinceId", "r.state_province_id", ValueType.UUID),
            field("countryId", "r.country_id", ValueType.UUID),
            field("postalCode", "r.postal_code", ValueType.STRING),
            field("isPrimary", "r.is_primary", ValueType.BOOLEAN),
            field("stateName", "r.state_name", ValueType.STRING),
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

    private static final Map<String, Function<PersonAddressEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PersonAddressEntity::getCreatedAt),
            Map.entry("updateTime", PersonAddressEntity::getUpdatedAt),
            Map.entry("sortOrder", PersonAddressEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.person_id, r.address_type, r.line1, r.line2, r.city_id, r.city_name,
                   r.state_province_id, r.country_id, r.postal_code, r.is_primary, r.state_name, r.sort_order,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM people.person_address r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO people.person_address
            (id, tenant_id, person_id, address_type, line1, line2, city_id, city_name, state_province_id, country_id,
             postal_code, is_primary, state_name, sort_order, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :personId, :addressType, :line1, :line2, :cityId, :cityName, :stateProvinceId,
                    :countryId, :postalCode, COALESCE(:isPrimary, false), :stateName, COALESCE(:sortOrder, 0),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, person_id, address_type, line1, line2, city_id, city_name, state_province_id,
                      country_id, postal_code, is_primary, state_name, sort_order, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE people.person_address
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE people.person_address
               SET address_type = :addressType,
                   line1 = :line1,
                   line2 = :line2,
                   city_id = :cityId,
                   city_name = :cityName,
                   state_province_id = :stateProvinceId,
                   country_id = :countryId,
                   postal_code = :postalCode,
                   is_primary = :isPrimary,
                   state_name = :stateName,
                   sort_order = :sortOrder,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.person WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public PersonAddressRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PersonAddressEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PersonAddressRepository::map).optional();
    }

    public Page<PersonAddressEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PersonAddressEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PersonAddressRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PersonAddressEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PersonAddressEntity insert(PersonAddressEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("personId", e.getPersonId())
                .param("addressType", e.getAddressType())
                .param("line1", e.getLine1())
                .param("line2", e.getLine2())
                .param("cityId", e.getCityId())
                .param("cityName", e.getCityName())
                .param("stateProvinceId", e.getStateProvinceId())
                .param("countryId", e.getCountryId())
                .param("postalCode", e.getPostalCode())
                .param("isPrimary", e.getIsPrimary())
                .param("stateName", e.getStateName())
                .param("sortOrder", e.getSortOrder())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PersonAddressRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PersonAddressEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("addressType", e.getAddressType())
                .param("line1", e.getLine1())
                .param("line2", e.getLine2())
                .param("cityId", e.getCityId())
                .param("cityName", e.getCityName())
                .param("stateProvinceId", e.getStateProvinceId())
                .param("countryId", e.getCountryId())
                .param("postalCode", e.getPostalCode())
                .param("isPrimary", e.getIsPrimary())
                .param("stateName", e.getStateName())
                .param("sortOrder", e.getSortOrder())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PersonAddressEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced people.person row exists (live). */
    public boolean existsPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PERSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.person_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, PersonAddressEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PersonAddressEntity map(ResultSet rs, int row) throws SQLException {
        return PersonAddressEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .personId(rs.getObject("person_id", UUID.class))
                .addressType(rs.getString("address_type"))
                .line1(rs.getString("line1"))
                .line2(rs.getString("line2"))
                .cityId(rs.getObject("city_id", UUID.class))
                .cityName(rs.getString("city_name"))
                .stateProvinceId(rs.getObject("state_province_id", UUID.class))
                .countryId(rs.getObject("country_id", UUID.class))
                .postalCode(rs.getString("postal_code"))
                .isPrimary(rs.getObject("is_primary", Boolean.class))
                .stateName(rs.getString("state_name"))
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
