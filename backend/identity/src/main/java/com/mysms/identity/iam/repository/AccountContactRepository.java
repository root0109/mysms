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
 * iam.user_account as the recovery, verification and provisioning flows see it: where an account can be reached,
 * whether it may sign in, and the few columns those flows set (self-registration, a required password reset, a
 * verified address, the last role worked in). Never reads the password hash itself.
 */
@Repository
public class AccountContactRepository {

    private static final String COLUMNS = """
            SELECT id, email, mobile_no, status, account_expires_at, locale, email_verified_at, row_version,
                   password_hash IS NOT NULL AS has_password
              FROM iam.user_account
             WHERE tenant_id = :tenantId
               AND deleted_at IS NULL
            """;
    /** Login name, e-mail or mobile number; two rows at most, so an ambiguous identifier is seen as such. */
    private static final String BY_IDENTIFIER = COLUMNS + """
               AND (lower(username) = lower(:identifier) OR lower(email) = lower(:identifier) OR mobile_no = :identifier)
             LIMIT 2
            """;
    private static final String BY_ID = COLUMNS + " AND id = :id";
    private static final String MARK_SELF_REGISTERED = """
            UPDATE iam.user_account SET registered_at = :now, terms_accepted_at = :now
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;
    private static final String REQUIRE_RESET = """
            UPDATE iam.user_account
               SET must_reset_password = true, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** Only while the account still has the address the code went to, and only once. */
    private static final String MARK_EMAIL_VERIFIED = """
            UPDATE iam.user_account
               SET email_verified_at = :now, updated_at = :now, updated_by = :id, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND lower(email) = lower(:email) AND email_verified_at IS NULL AND deleted_at IS NULL
            """;
    /** Bookkeeping like a sign-in, not a user edit: the ETag stays. */
    private static final String SET_LAST_ROLE = """
            UPDATE iam.user_account SET last_login_role_id = :role
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;
    private static final String ROLE_BY_CODE = """
            SELECT id, is_system FROM iam.role
             WHERE tenant_id = :tenantId
               AND lower(code) = lower(:code) AND is_active AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public AccountContactRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public List<Contact> findByIdentifier(String identifier) {
        return jdbc.sql(BY_IDENTIFIER).param("identifier", identifier).query(AccountContactRepository::map).list();
    }

    public Optional<Contact> findById(UUID id) {
        return jdbc.sql(BY_ID).param("id", id).query(AccountContactRepository::map).optional();
    }

    public void markSelfRegistered(UUID id, OffsetDateTime now) {
        jdbc.sql(MARK_SELF_REGISTERED).param("id", id).param("now", now).update();
    }

    public int requireReset(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(REQUIRE_RESET).param("id", id).param("expected", expected).param("now", now)
                .param("actor", actor).update();
    }

    public int markEmailVerified(UUID id, String email, OffsetDateTime now) {
        return jdbc.sql(MARK_EMAIL_VERIFIED).param("id", id).param("email", email).param("now", now).update();
    }

    public void setLastRole(UUID id, UUID role) {
        jdbc.sql(SET_LAST_ROLE).param("id", id).param("role", role).update();
    }

    /** A live, active role of the tenant by its code (ignoring case). */
    public Optional<RoleRef> roleByCode(String code) {
        return jdbc.sql(ROLE_BY_CODE).param("code", code)
                .query((rs, row) -> new RoleRef(rs.getObject("id", UUID.class), rs.getBoolean("is_system")))
                .optional();
    }

    private static Contact map(ResultSet rs, int row) throws SQLException {
        return new Contact(rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("mobile_no"),
                rs.getString("status"), Optional.ofNullable(rs.getObject("account_expires_at", OffsetDateTime.class)),
                rs.getString("locale"), rs.getObject("email_verified_at", OffsetDateTime.class) != null,
                rs.getInt("row_version"), rs.getBoolean("has_password"));
    }

    /**
     * @param id            the account
     * @param email         its address, if any
     * @param mobileNo      its mobile number, if any
     * @param status        active, inactive, locked or exited
     * @param expiresAt     when the account stops working, if it does
     * @param locale        its preferred language, if set
     * @param emailVerified whether the address is verified
     * @param rowVersion    its version (ETag)
     * @param hasPassword   whether a password is set (a self-registered account has none until its first reset)
     */
    public record Contact(UUID id, String email, String mobileNo, String status, Optional<OffsetDateTime> expiresAt,
                          String locale, boolean emailVerified, int rowVersion, boolean hasPassword) {

        /** Active and not expired: only such an account is sent a code or may use one. */
        public boolean usableAt(OffsetDateTime now) {
            return "active".equals(status) && expiresAt.map(now::isBefore).orElse(true);
        }
    }

    /**
     * @param id     the role
     * @param system a system role (never handed out by sign-up or import)
     */
    public record RoleRef(UUID id, boolean system) {
    }
}
