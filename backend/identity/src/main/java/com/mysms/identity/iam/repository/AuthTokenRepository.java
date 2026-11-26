package com.mysms.identity.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Refresh tokens in iam.auth_token (token_type 'refresh'): only the SHA-256 is stored; {@code name} holds the token
 * family — the sign-in session — so rotation and reuse detection can revoke a whole family at once.
 */
@Repository
public class AuthTokenRepository {

    private static final String INSERT = """
            INSERT INTO iam.auth_token
                   (id, tenant_id, user_id, token_type, name, token_hash, ip_address, expires_at, created_at, created_by,
                    updated_at, updated_by, row_version)
            VALUES (:id, :tenant, :user, 'refresh', :family, :hash, CAST(:ip AS inet), :expiresAt, :now, :user, :now,
                    :user, 1)
            """;
    private static final String FIND_FOR_UPDATE = """
            SELECT id, user_id, name, expires_at, revoked_at, last_used_at
              FROM iam.auth_token
             WHERE tenant_id = :tenantId
               AND token_hash = :hash AND token_type = 'refresh' AND deleted_at IS NULL
               FOR UPDATE
            """;
    private static final String REVOKE = """
            UPDATE iam.auth_token SET revoked_at = :now, last_used_at = :now
             WHERE tenant_id = :tenantId
               AND id = :id AND revoked_at IS NULL
            """;
    private static final String REVOKE_FAMILY = """
            UPDATE iam.auth_token SET revoked_at = :now
             WHERE tenant_id = :tenantId
               AND token_type = 'refresh' AND name = :family AND revoked_at IS NULL
            """;
    private static final String REVOKE_ALL = """
            UPDATE iam.auth_token SET revoked_at = :now
             WHERE tenant_id = :tenantId
               AND token_type = 'refresh' AND user_id = :user AND revoked_at IS NULL
            """;

    /** A one-time sign-in step (the MFA token): `name` says what it completes, `mfa` or `mfa_enroll:<credential>`. */
    private static final String INSERT_ONE_TIME = """
            INSERT INTO iam.auth_token
                   (id, tenant_id, user_id, token_type, name, token_hash, expires_at, created_at, created_by,
                    updated_at, updated_by, row_version)
            VALUES (:id, :tenant, :user, 'one_time_login', :name, :hash, :expiresAt, :now, :user, :now, :user, 1)
            """;
    private static final String FIND_ONE_TIME = """
            SELECT id, user_id, name, expires_at, revoked_at
              FROM iam.auth_token
             WHERE tenant_id = :tenantId
               AND token_hash = :hash AND token_type = 'one_time_login' AND deleted_at IS NULL
               FOR UPDATE
            """;
    private static final String REVOKE_ONE_TIME = """
            UPDATE iam.auth_token SET revoked_at = :now
             WHERE tenant_id = :tenantId
               AND token_type = 'one_time_login' AND user_id = :user AND revoked_at IS NULL
            """;
    private static final String REVOKE_ALL_EXCEPT = """
            UPDATE iam.auth_token SET revoked_at = :now
             WHERE tenant_id = :tenantId
               AND token_type = 'refresh' AND user_id = :user AND name <> :family AND revoked_at IS NULL
            """;

    private final JdbcClient jdbc;

    public AuthTokenRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public void insertRefresh(UUID id, UUID tenant, UUID user, UUID family, String hash, String ip,
                              OffsetDateTime expiresAt, OffsetDateTime now) {
        jdbc.sql(INSERT).param("id", id).param("tenant", tenant).param("user", user).param("family", family.toString())
                .param("hash", hash).param("ip", ip).param("expiresAt", expiresAt).param("now", now).update();
    }

    /** Locks the token row for the rest of the transaction, so two concurrent refreshes cannot both rotate it. */
    public Optional<RefreshRow> findForUpdate(String hash) {
        return jdbc.sql(FIND_FOR_UPDATE).param("hash", hash)
                .query((rs, row) -> new RefreshRow(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        UUID.fromString(rs.getString("name")), rs.getObject("expires_at", OffsetDateTime.class),
                        Optional.ofNullable(rs.getObject("revoked_at", OffsetDateTime.class)),
                        Optional.ofNullable(rs.getObject("last_used_at", OffsetDateTime.class))))
                .optional();
    }

    public int revoke(UUID id, OffsetDateTime now) {
        return jdbc.sql(REVOKE).param("id", id).param("now", now).update();
    }

    public int revokeFamily(UUID family, OffsetDateTime now) {
        return jdbc.sql(REVOKE_FAMILY).param("family", family.toString()).param("now", now).update();
    }

    public int revokeAll(UUID user, OffsetDateTime now) {
        return jdbc.sql(REVOKE_ALL).param("user", user).param("now", now).update();
    }

    public void insertOneTime(UUID id, UUID tenant, UUID user, String name, String hash, OffsetDateTime expiresAt,
                              OffsetDateTime now) {
        jdbc.sql(INSERT_ONE_TIME).param("id", id).param("tenant", tenant).param("user", user).param("name", name)
                .param("hash", hash).param("expiresAt", expiresAt).param("now", now).update();
    }

    /** Locks the one-time token row, so a code can complete it once only. */
    public Optional<OneTimeRow> findOneTimeForUpdate(String hash) {
        return jdbc.sql(FIND_ONE_TIME).param("hash", hash)
                .query((rs, row) -> new OneTimeRow(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("name"), rs.getObject("expires_at", OffsetDateTime.class),
                        rs.getObject("revoked_at", OffsetDateTime.class) != null))
                .optional();
    }

    /** Ends every open one-time sign-in step of the user (after success, or when it can no longer succeed). */
    public int revokeOneTime(UUID user, OffsetDateTime now) {
        return jdbc.sql(REVOKE_ONE_TIME).param("user", user).param("now", now).update();
    }

    /** Signs the user out of every other session (password change keeps the current one). */
    public int revokeAllExcept(UUID user, UUID family, OffsetDateTime now) {
        return jdbc.sql(REVOKE_ALL_EXCEPT).param("user", user).param("family", family.toString()).param("now", now)
                .update();
    }

    /** One open MFA step: who it is for, what it completes, until when. */
    public record OneTimeRow(UUID id, UUID userId, String name, OffsetDateTime expiresAt, boolean revoked) {
    }

    /**
     * @param family    the sign-in session the token belongs to
     * @param revokedAt  set once the token was rotated or revoked (sign-out, refused refresh)
     * @param usedAt     set only by rotation: presenting a rotated token again is reuse (possible theft), while a
     *                   token revoked by sign-out is simply refused
     */
    public record RefreshRow(UUID id, UUID userId, UUID family, OffsetDateTime expiresAt,
                             Optional<OffsetDateTime> revokedAt, Optional<OffsetDateTime> usedAt) {

        public boolean rotated() {
            return revokedAt.isPresent() && usedAt.isPresent();
        }
    }
}
