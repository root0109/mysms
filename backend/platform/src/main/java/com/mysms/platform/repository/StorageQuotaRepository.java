package com.mysms.platform.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.StorageQuotaEntity;
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
 * Plain-JDBC access to platform.storage_quota: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class StorageQuotaRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("quotaBytes", "r.quota_bytes", ValueType.INTEGER),
            field("usedBytes", "r.used_bytes", ValueType.INTEGER),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StorageQuotaEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StorageQuotaEntity::getCreatedAt),
            Map.entry("updateTime", StorageQuotaEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_id, r.institution_id, r.quota_bytes, r.used_bytes, r.is_active,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM platform.storage_quota r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** A user's own quota (the schema allows one live row per user in practice; the newest wins). */
    private static final String FIND_FOR_USER = SELECT_LIVE + " AND r.user_id = :userId ORDER BY r.created_at DESC LIMIT 1";
    /**
     * Counts bytes against the user's live quota row, in one statement so two uploads cannot both spend the last
     * bytes: an enforced quota refuses (0 rows) what would exceed it; an unenforced one only keeps the count.
     */
    private static final String SPEND = """
            UPDATE platform.storage_quota
               SET used_bytes = used_bytes + :bytes,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :userId AND deleted_at IS NULL
               AND (NOT is_active OR used_bytes + :bytes <= quota_bytes)
            RETURNING id
            """;
    /** Gives bytes back when a counted file is deleted (never below zero). */
    private static final String RELEASE = """
            UPDATE platform.storage_quota
               SET used_bytes = GREATEST(used_bytes - :bytes, 0),
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :userId AND deleted_at IS NULL
            RETURNING id
            """;
    private static final String INSERT = """
            INSERT INTO platform.storage_quota
            (id, tenant_id, user_id, institution_id, quota_bytes, used_bytes, is_active, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :institutionId, :quotaBytes, COALESCE(:usedBytes, 0),
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_id, institution_id, quota_bytes, used_bytes, is_active, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE platform.storage_quota
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE platform.storage_quota
               SET user_id = :userId,
                   institution_id = :institutionId,
                   quota_bytes = :quotaBytes,
                   used_bytes = :usedBytes,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public StorageQuotaRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StorageQuotaEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StorageQuotaRepository::map).optional();
    }

    public Page<StorageQuotaEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StorageQuotaEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StorageQuotaRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StorageQuotaEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StorageQuotaEntity insert(StorageQuotaEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("institutionId", e.getInstitutionId())
                .param("quotaBytes", e.getQuotaBytes())
                .param("usedBytes", e.getUsedBytes())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StorageQuotaRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StorageQuotaEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("institutionId", e.getInstitutionId())
                .param("quotaBytes", e.getQuotaBytes())
                .param("usedBytes", e.getUsedBytes())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StorageQuotaEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, StorageQuotaEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    /** The user's own live quota, when one is set. */
    public Optional<StorageQuotaEntity> findForUser(UUID userId) {
        return jdbc.sql(FIND_FOR_USER).param("userId", userId).query(StorageQuotaRepository::map).optional();
    }

    /** Counts the bytes on the user's quota rows; returns the rows that took them (none: refused or no quota). */
    public List<UUID> spend(UUID userId, long bytes, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SPEND).param("userId", userId).param("bytes", bytes).param("now", now).param("actor", actor)
                .query(UUID.class).list();
    }

    /** Gives the bytes back on the user's quota rows; returns the rows that changed. */
    public List<UUID> release(UUID userId, long bytes, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RELEASE).param("userId", userId).param("bytes", bytes).param("now", now)
                .param("actor", actor).query(UUID.class).list();
    }

    private static StorageQuotaEntity map(ResultSet rs, int row) throws SQLException {
        return StorageQuotaEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .quotaBytes(rs.getObject("quota_bytes", Long.class))
                .usedBytes(rs.getObject("used_bytes", Long.class))
                .isActive(rs.getObject("is_active", Boolean.class))
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
