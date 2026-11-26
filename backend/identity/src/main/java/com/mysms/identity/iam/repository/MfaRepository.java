package com.mysms.identity.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * iam.mfa_credential: the caller's second factors. {@code secret_enc} holds the TOTP secret, or for the
 * {@code recovery_codes} row the hashes of the remaining codes, always as a {@code FieldCipher} envelope. Every
 * statement names the user, so one account can never read or change another's factors; row-level security binds
 * the tenant.
 */
@Repository
public class MfaRepository {

    public static final String TOTP = "totp";
    public static final String RECOVERY = "recovery_codes";

    private static final String COLUMNS = """
            SELECT id, credential_type, label, secret_enc, verified_at, last_used_at, is_active
              FROM iam.mfa_credential
             WHERE tenant_id = :tenantId
               AND user_id = :user AND deleted_at IS NULL
            """;
    private static final String INSERT = """
            INSERT INTO iam.mfa_credential
                   (id, tenant_id, user_id, credential_type, label, secret_enc, verified_at, is_active, created_at,
                    created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenant, :user, :type, :label, :secret, :verifiedAt, true, :now, :user, :now, :user, 1)
            """;
    private static final String VERIFY = """
            UPDATE iam.mfa_credential
               SET verified_at = :now, last_used_at = :usedAt, updated_at = :now, updated_by = :user,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND user_id = :user AND verified_at IS NULL AND deleted_at IS NULL
            """;
    private static final String TOUCH = """
            UPDATE iam.mfa_credential
               SET last_used_at = :usedAt, updated_at = :now, updated_by = :user, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND user_id = :user AND deleted_at IS NULL
            """;
    private static final String REPLACE_SECRET = """
            UPDATE iam.mfa_credential
               SET secret_enc = :secret, verified_at = COALESCE(verified_at, :now), updated_at = :now,
                   updated_by = :user, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND user_id = :user AND deleted_at IS NULL
            """;
    private static final String RETIRE = """
            UPDATE iam.mfa_credential
               SET deleted_at = :now, is_active = false, updated_at = :now, updated_by = :user,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND user_id = :user AND deleted_at IS NULL
            """;
    private static final String RETIRE_PENDING = """
            UPDATE iam.mfa_credential
               SET deleted_at = :now, is_active = false, updated_at = :now, updated_by = :user,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :user AND verified_at IS NULL AND deleted_at IS NULL
            """;
    private static final String RETIRE_TYPE = """
            UPDATE iam.mfa_credential
               SET deleted_at = :now, is_active = false, updated_at = :now, updated_by = :user,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND user_id = :user AND credential_type = :type AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public MfaRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** All live credentials of the user, oldest first, locked for the rest of the transaction. */
    public List<Credential> forUpdate(UUID user) {
        return jdbc.sql(COLUMNS + " ORDER BY created_at, id FOR UPDATE").param("user", user).query(MfaRepository::row)
                .list();
    }

    public List<Credential> list(UUID user) {
        return jdbc.sql(COLUMNS + " ORDER BY created_at, id").param("user", user).query(MfaRepository::row).list();
    }

    public void insert(UUID id, UUID tenant, UUID user, String type, String label, String secretEnvelope,
                       OffsetDateTime verifiedAt, OffsetDateTime now) {
        jdbc.sql(INSERT).param("id", id).param("tenant", tenant).param("user", user).param("type", type)
                .param("label", label).param("secret", secretEnvelope).param("verifiedAt", verifiedAt)
                .param("now", now).update();
    }

    public int verify(UUID id, UUID user, OffsetDateTime usedAt, OffsetDateTime now) {
        return jdbc.sql(VERIFY).param("id", id).param("user", user).param("usedAt", usedAt).param("now", now)
                .update();
    }

    public int touch(UUID id, UUID user, OffsetDateTime usedAt, OffsetDateTime now) {
        return jdbc.sql(TOUCH).param("id", id).param("user", user).param("usedAt", usedAt).param("now", now)
                .update();
    }

    public int replaceSecret(UUID id, UUID user, String secretEnvelope, OffsetDateTime now) {
        return jdbc.sql(REPLACE_SECRET).param("id", id).param("user", user).param("secret", secretEnvelope)
                .param("now", now).update();
    }

    public int retire(UUID id, UUID user, OffsetDateTime now) {
        return jdbc.sql(RETIRE).param("id", id).param("user", user).param("now", now).update();
    }

    public int retirePending(UUID user, OffsetDateTime now) {
        return jdbc.sql(RETIRE_PENDING).param("user", user).param("now", now).update();
    }

    public int retireType(UUID user, String type, OffsetDateTime now) {
        return jdbc.sql(RETIRE_TYPE).param("user", user).param("type", type).param("now", now).update();
    }

    private static Credential row(ResultSet rs, int row) throws SQLException {
        return new Credential(rs.getObject("id", UUID.class), rs.getString("credential_type"), rs.getString("label"),
                rs.getString("secret_enc"), Optional.ofNullable(rs.getObject("verified_at", OffsetDateTime.class)),
                Optional.ofNullable(rs.getObject("last_used_at", OffsetDateTime.class)), rs.getBoolean("is_active"));
    }

    /** One second factor; {@code secretEnvelope} is never logged or returned. */
    public record Credential(UUID id, String type, String label, String secretEnvelope,
                             Optional<OffsetDateTime> verifiedAt, Optional<OffsetDateTime> lastUsedAt,
                             boolean active) {

        public boolean usable() {
            return active && verifiedAt.isPresent();
        }

        @Override
        public String toString() {
            return "Credential[id=" + id + ", type=" + type + "]";
        }
    }
}
