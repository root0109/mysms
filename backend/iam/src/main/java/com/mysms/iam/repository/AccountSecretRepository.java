package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The one-time secrets iam mails to an account holder (iam.auth_token of type password_reset, email_verification or
 * mobile_verification): only their SHA-256 is stored, together with the address they went to, and a secret is
 * consumed by one statement that succeeds once, before its expiry, for its own type only.
 */
@Repository
public class AccountSecretRepository {

    public static final String PASSWORD_RESET = "password_reset";
    public static final String EMAIL_VERIFICATION = "email_verification";

    private static final String INSERT = """
            INSERT INTO iam.auth_token
                   (id, tenant_id, user_id, token_type, name, token_hash, sent_to, ip_address, expires_at, created_at,
                    created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :user, :type, :purpose, :hash, :sentTo, CAST(:ip AS inet), :expiresAt, :now,
                    :actor, :now, :actor, 1)
            """;
    /** Atomic single use: the first statement to reach a live, unexpired secret consumes it; every other finds none. */
    private static final String CONSUME = """
            UPDATE iam.auth_token
               SET revoked_at = :now, last_used_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND token_hash = :hash AND token_type = :type AND revoked_at IS NULL AND deleted_at IS NULL
               AND expires_at > :now
            RETURNING user_id, sent_to, name
            """;
    private static final String COUNT_SINCE = """
            SELECT count(*) FROM iam.auth_token
             WHERE tenant_id = :tenantId
               AND user_id = :user AND token_type = :type AND created_at > :since AND deleted_at IS NULL
            """;
    private static final String REVOKE_OPEN = """
            UPDATE iam.auth_token SET revoked_at = :now
             WHERE tenant_id = :tenantId
               AND user_id = :user AND token_type = :type AND revoked_at IS NULL AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public AccountSecretRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /**
     * @param purpose what the secret completes (kept in {@code name}): {@code reset}, {@code forced}, {@code sign_up},
     *                {@code verify}
     * @param actor   who caused it to be sent (the holder themselves, or an administrator)
     */
    public void insert(UUID id, UUID tenant, UUID user, String type, String purpose, String hash, String sentTo,
                       String ip, OffsetDateTime expiresAt, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT).param("id", id).param("tenantId", tenant).param("user", user).param("type", type)
                .param("purpose", purpose).param("hash", hash).param("sentTo", sentTo).param("ip", ip)
                .param("expiresAt", expiresAt).param("now", now).param("actor", actor).update();
    }

    /** Consumes the secret if it is live, unexpired and of this type; empty for anything else. */
    public Optional<Consumed> consume(String hash, String type, OffsetDateTime now) {
        return jdbc.sql(CONSUME).param("hash", hash).param("type", type).param("now", now)
                .query((rs, row) -> new Consumed(rs.getObject("user_id", UUID.class), rs.getString("sent_to"),
                        rs.getString("name")))
                .optional();
    }

    /** How many secrets of this type were issued to the user after {@code since} (the sending throttle). */
    public int countSince(UUID user, String type, OffsetDateTime since) {
        return jdbc.sql(COUNT_SINCE).param("user", user).param("type", type).param("since", since)
                .query(Long.class).single().intValue();
    }

    /** Every open secret of this type stops working (a new password, a confirmed address). */
    public int revokeOpen(UUID user, String type, OffsetDateTime now) {
        return jdbc.sql(REVOKE_OPEN).param("user", user).param("type", type).param("now", now).update();
    }

    /**
     * @param userId  the account the secret belongs to
     * @param sentTo  the address it was mailed to
     * @param purpose what it completes
     */
    public record Consumed(UUID userId, String sentTo, String purpose) {
    }
}
