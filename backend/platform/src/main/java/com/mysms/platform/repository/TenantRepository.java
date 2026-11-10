package com.mysms.platform.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.TenantEntity;
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
 * Plain-JDBC access to platform.tenant (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. The table is global (no
 * tenant column, no row-level security): it is written only by
 * platform staff, through the operations whose scheme the edge enforces.
 */
@Repository
public class TenantRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("legalName", "r.legal_name", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("primaryEmail", "r.primary_email", ValueType.STRING),
            field("primaryPhone", "r.primary_phone", ValueType.STRING),
            field("countryId", "r.country_id", ValueType.UUID),
            field("defaultCurrencyId", "r.default_currency_id", ValueType.UUID),
            field("timezone", "r.timezone", ValueType.STRING),
            field("locale", "r.locale", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("code", "r.code", ValueType.STRING),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<TenantEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", TenantEntity::getCreatedAt),
            Map.entry("updateTime", TenantEntity::getUpdatedAt),
            Map.entry("code", TenantEntity::getCode),
            Map.entry("name", TenantEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.code, r.name, r.legal_name, r.status, r.primary_email, r.primary_phone, r.country_id,
                   r.default_currency_id, r.timezone, r.locale, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM platform.tenant r
             WHERE r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO platform.tenant
            (id, code, name, legal_name, status, primary_email, primary_phone, country_id, default_currency_id,
             timezone, locale, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :code, :name, :legalName, COALESCE(:status, 'active'::character varying), :primaryEmail,
                    :primaryPhone, :countryId, :defaultCurrencyId, COALESCE(:timezone, 'UTC'::character varying),
                    COALESCE(:locale, 'en'::character varying), :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, code, name, legal_name, status, primary_email, primary_phone, country_id, default_currency_id,
                      timezone, locale, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE platform.tenant
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE platform.tenant
               SET code = :code,
                   name = :name,
                   legal_name = :legalName,
                   status = :status,
                   primary_email = :primaryEmail,
                   primary_phone = :primaryPhone,
                   country_id = :countryId,
                   default_currency_id = :defaultCurrencyId,
                   timezone = :timezone,
                   locale = :locale,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_TENANT_CODE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM platform.tenant
                            WHERE code = :code AND id <> :id)
            """;

    private final JdbcClient jdbc;

    public TenantRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<TenantEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(TenantRepository::map).optional();
    }

    public Page<TenantEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<TenantEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(TenantRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), TenantEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public TenantEntity insert(TenantEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("legalName", e.getLegalName())
                .param("status", e.getStatus())
                .param("primaryEmail", e.getPrimaryEmail())
                .param("primaryPhone", e.getPrimaryPhone())
                .param("countryId", e.getCountryId())
                .param("defaultCurrencyId", e.getDefaultCurrencyId())
                .param("timezone", e.getTimezone())
                .param("locale", e.getLocale())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(TenantRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(TenantEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("code", e.getCode())
                .param("name", e.getName())
                .param("legalName", e.getLegalName())
                .param("status", e.getStatus())
                .param("primaryEmail", e.getPrimaryEmail())
                .param("primaryPhone", e.getPrimaryPhone())
                .param("countryId", e.getCountryId())
                .param("defaultCurrencyId", e.getDefaultCurrencyId())
                .param("timezone", e.getTimezone())
                .param("locale", e.getLocale())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(TenantEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_tenant_code: code. */
    public boolean takenCodeCode(TenantEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_TENANT_CODE_TAKEN).param("code", e.getCode()).param("id", e.getId())
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, TenantEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static TenantEntity map(ResultSet rs, int row) throws SQLException {
        return TenantEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .legalName(rs.getString("legal_name"))
                .status(rs.getString("status"))
                .primaryEmail(rs.getString("primary_email"))
                .primaryPhone(rs.getString("primary_phone"))
                .countryId(rs.getObject("country_id", UUID.class))
                .defaultCurrencyId(rs.getObject("default_currency_id", UUID.class))
                .timezone(rs.getString("timezone"))
                .locale(rs.getString("locale"))
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
