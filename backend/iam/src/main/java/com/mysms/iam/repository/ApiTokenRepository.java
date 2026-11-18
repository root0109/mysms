package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.AuthTokenEntity;
import java.sql.Array;
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
 * Personal API tokens (iam.auth_token, token_type 'api'): the tenant in every statement, live rows only, keyset
 * paging, the row_version guard on revocation. Only the SHA-256 of a token is stored and it is only ever compared.
 */
@Repository
public class ApiTokenRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("expiresAt", "r.expires_at", ValueType.TIMESTAMP),
            field("lastUsedAt", "r.last_used_at", ValueType.TIMESTAMP),
            field("revokedAt", "r.revoked_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime desc";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AuthTokenEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AuthTokenEntity::getCreatedAt),
            Map.entry("updateTime", AuthTokenEntity::getUpdatedAt));

    private static final String COLUMNS = """
            r.id, r.tenant_id, r.user_id, r.token_type, r.name, r.sent_to, r.scopes, r.ip_address, r.expires_at,
            r.last_used_at, r.revoked_at, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
            r.row_version
            """;
    private static final String SELECT_LIVE = "SELECT " + COLUMNS + """
              FROM iam.auth_token r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL AND r.token_type = 'api'
            """;
    private static final String FIND = SELECT_LIVE + " AND r.id = :id AND r.user_id = :user";
    private static final String INSERT = """
            INSERT INTO iam.auth_token AS r
                   (id, tenant_id, user_id, token_type, name, token_hash, scopes, ip_address, expires_at, created_at,
                    created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, 'api', :name, :hash, CAST(:scopes AS varchar(100)[]), CAST(:ip AS inet),
                    :expiresAt, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING""" + " " + COLUMNS;
    /** Revocation is a soft delete: the token stops working and leaves the owner's list. */
    private static final String REVOKE = """
            UPDATE iam.auth_token
               SET revoked_at = COALESCE(revoked_at, :deletedAt), deleted_at = :deletedAt, updated_at = :updatedAt,
                   updated_by = :updatedBy, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND token_type = 'api' AND row_version = :expected AND deleted_at IS NULL
            """;
    /** Every usable token of the user revoked at once (credentials reset, signed out everywhere, deactivated). */
    private static final String REVOKE_ALL_OF_USER = """
            UPDATE iam.auth_token
               SET revoked_at = :now, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :user AND token_type = 'api' AND revoked_at IS NULL AND deleted_at IS NULL
            RETURNING id
            """;
    /** The exchange: a live, unrevoked, unexpired token, its last use recorded (no version change: not an edit). */
    private static final String USE = """
            UPDATE iam.auth_token r SET last_used_at = :now
             WHERE r.tenant_id = :tenantId
               AND r.token_hash = :hash AND r.token_type = 'api' AND r.revoked_at IS NULL AND r.deleted_at IS NULL
               AND r.expires_at > :now
            RETURNING""" + " " + COLUMNS;

    private final JdbcClient jdbc;

    public ApiTokenRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The user's live API token with this id. */
    public Optional<AuthTokenEntity> find(UUID user, UUID id) {
        return jdbc.sql(FIND).param("user", user).param("id", id).query(ApiTokenRepository::map).optional();
    }

    public Page<AuthTokenEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AuthTokenEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ApiTokenRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AuthTokenEntity::getId);
    }

    public AuthTokenEntity insert(AuthTokenEntity e, String hash) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("name", e.getName())
                .param("hash", hash)
                .param("scopes", e.getScopes() == null ? null : e.getScopes().toArray(String[]::new))
                .param("ip", e.getIpAddress())
                .param("expiresAt", e.getExpiresAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ApiTokenRepository::map).single();
    }

    /** 0 rows: the token changed (or vanished) since it was read. */
    public int revoke(AuthTokenEntity e, int expectedVersion) {
        return jdbc.sql(REVOKE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /**
     * Revokes every token of the user that still works; answers their ids. A revoked token stays in the owner's list
     * (with its revocation time) until the owner deletes it.
     */
    public List<UUID> revokeAllOf(UUID user, OffsetDateTime now, UUID actor) {
        return jdbc.sql(REVOKE_ALL_OF_USER).param("user", user).param("now", now).param("actor", actor)
                .query((rs, row) -> rs.getObject("id", UUID.class)).list();
    }

    /** The usable token with this hash, its use recorded; empty for anything revoked, expired or unknown. */
    public Optional<AuthTokenEntity> use(String hash, OffsetDateTime now) {
        return jdbc.sql(USE).param("hash", hash).param("now", now).query(ApiTokenRepository::map).optional();
    }

    /** The tokens of one user. */
    public static SqlFragment ofUser(UUID user) {
        return new SqlFragment("r.user_id = :ownerId", Map.of("ownerId", user));
    }

    private static List<Object> sortValues(SortSpec sort, AuthTokenEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AuthTokenEntity map(ResultSet rs, int row) throws SQLException {
        Array scopes = rs.getArray("scopes");
        return AuthTokenEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .tokenType(rs.getString("token_type"))
                .name(rs.getString("name"))
                .sentTo(rs.getString("sent_to"))
                .scopes(scopes == null ? null : List.of((String[]) scopes.getArray()))
                .ipAddress(rs.getString("ip_address"))
                .expiresAt(rs.getObject("expires_at", OffsetDateTime.class))
                .lastUsedAt(rs.getObject("last_used_at", OffsetDateTime.class))
                .revokedAt(rs.getObject("revoked_at", OffsetDateTime.class))
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
