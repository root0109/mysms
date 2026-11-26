package com.mysms.learning.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.SettingsPrimaryCountryEntity;
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
 * Plain-JDBC access to lms.settings_primary_country: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class SettingsPrimaryCountryRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("settingsId", "r.settings_id", ValueType.UUID),
            field("countryId", "r.country_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<SettingsPrimaryCountryEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", SettingsPrimaryCountryEntity::getCreatedAt),
            Map.entry("updateTime", SettingsPrimaryCountryEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.settings_id, r.country_id, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM lms.settings_primary_country r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.settings_primary_country
            (id, tenant_id, settings_id, country_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :settingsId, :countryId, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, settings_id, country_id, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.settings_primary_country
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.settings_primary_country
               SET country_id = :countryId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_SETTINGS_PRIMARY_COUNTRY_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.settings_primary_country
                            WHERE tenant_id = :tenantId
                              AND settings_id = :settingsId AND country_id = :countryId AND id <> :id AND ((deleted_at
                                  IS NULL)))
            """;
    private static final String EXISTS_SETTINGS_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.settings WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public SettingsPrimaryCountryRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<SettingsPrimaryCountryEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(SettingsPrimaryCountryRepository::map).optional();
    }

    public Page<SettingsPrimaryCountryEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<SettingsPrimaryCountryEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(SettingsPrimaryCountryRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), SettingsPrimaryCountryEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public SettingsPrimaryCountryEntity insert(SettingsPrimaryCountryEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("settingsId", e.getSettingsId())
                .param("countryId", e.getCountryId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(SettingsPrimaryCountryRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(SettingsPrimaryCountryEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("countryId", e.getCountryId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(SettingsPrimaryCountryEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_settings_primary_country_1: settings_id, country_id where (deleted_at IS NULL). */
    public boolean takenSettingsIdCountryId1(SettingsPrimaryCountryEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_SETTINGS_PRIMARY_COUNTRY_1_TAKEN)
                .param("settingsId", e.getSettingsId())
                .param("countryId", e.getCountryId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.settings row exists (live). */
    public boolean existsSettingsId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SETTINGS_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.settings_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, SettingsPrimaryCountryEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static SettingsPrimaryCountryEntity map(ResultSet rs, int row) throws SQLException {
        return SettingsPrimaryCountryEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .settingsId(rs.getObject("settings_id", UUID.class))
                .countryId(rs.getObject("country_id", UUID.class))
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
