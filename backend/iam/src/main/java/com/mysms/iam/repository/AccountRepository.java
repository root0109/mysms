package com.mysms.iam.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.MeAccountEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * iam.user_account for sign-in and for the caller's own account. Row-level security scopes every statement to the
 * bound tenant; the password hash is read only by {@link #findForSignIn} and {@link #findSignInById}.
 */
@Repository
public class AccountRepository {

    private static final String SIGN_IN_COLUMNS = """
            SELECT id, person_id, password_hash, status, locked_until, account_expires_at
              FROM iam.user_account
             WHERE tenant_id = :tenantId
               AND deleted_at IS NULL
            """;
    /** Login name, e-mail or mobile number; at most two rows are read so an ambiguous identifier is detected. */
    private static final String FIND_FOR_SIGN_IN = SIGN_IN_COLUMNS + """
               AND (lower(username) = lower(:identifier) OR lower(email) = lower(:identifier) OR mobile_no = :identifier)
             LIMIT 2
            """;
    private static final String FIND_SIGN_IN_BY_ID = SIGN_IN_COLUMNS + " AND id = :id";
    /**
     * Counts a failure; from the threshold on, locks sign-in for 1, 2, 4... minutes up to the maximum. Sign-in
     * bookkeeping is not a user edit: row_version and updated_at stay as they are, so administrators' ETags hold.
     */
    private static final String RECORD_FAILURE = """
            UPDATE iam.user_account
               SET failed_login_count = failed_login_count + 1,
                   locked_until = CASE WHEN failed_login_count + 1 >= :threshold
                       THEN CAST(:now AS timestamptz) + make_interval(mins => LEAST(:maxMinutes,
                            CAST(power(2, LEAST(failed_login_count + 1 - :threshold, 16)) AS integer)))
                       ELSE locked_until END
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;
    private static final String RECORD_SUCCESS = """
            UPDATE iam.user_account
               SET failed_login_count = 0, locked_until = NULL, last_login_at = :now
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = """
            SELECT id, tenant_id, person_id, username, email, mobile_no, status, email_verified_at, mobile_verified_at,
                   must_reset_password, referred_by_user_id, terms_accepted_at, locale, timezone, last_login_at,
                   account_expires_at, failed_login_count, last_login_role_id, registered_at, joined_on, external_ref,
                   signature, use_signature, user_type, created_at, created_by, updated_at, updated_by, deleted_at,
                   row_version
              FROM iam.user_account
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;
    private static final String HAS_ACTIVE_MFA = """
            SELECT EXISTS (SELECT 1 FROM iam.mfa_credential
                            WHERE tenant_id = :tenantId
                              AND user_id = :user AND is_active AND verified_at IS NOT NULL AND deleted_at IS NULL)
            """;
    private static final String USERNAME_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM iam.user_account WHERE tenant_id = :tenantId
                AND lower(username) = lower(:username) AND deleted_at IS NULL)
            """;
    private static final String INSERT = """
            INSERT INTO iam.user_account
                   (id, tenant_id, username, password_hash, status, user_type, registered_at, created_at, created_by,
                    updated_at, updated_by, row_version)
            VALUES (:id, :tenant, :username, :passwordHash, 'active', :userType, :now, :now, :actor, :now, :actor, 1)
            """;

    /** A new password (and optionally login name); clears the lockout. A user edit: the ETag moves on. */
    private static final String SET_CREDENTIALS = """
            UPDATE iam.user_account
               SET password_hash = :hash, username = COALESCE(:username, username),
                   must_reset_password = :mustReset, failed_login_count = 0, locked_until = NULL,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;
    private static final String ACCEPT_TERMS = """
            UPDATE iam.user_account
               SET terms_accepted_at = :now, updated_at = :now, updated_by = :id, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND deleted_at IS NULL
            """;
    /** A sign-in identifier must name one account: no other account may use it as username, e-mail or mobile. */
    private static final String IDENTIFIER_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM iam.user_account
                            WHERE tenant_id = :tenantId
                              AND (CAST(:id AS uuid) IS NULL OR id <> :id) AND deleted_at IS NULL
                              AND (lower(username) = lower(:value) OR lower(email) = lower(:value)
                                   OR mobile_no = :value))
            """;

    private final JdbcClient jdbc;

    public AccountRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Accounts matching a login name, e-mail or mobile number (0, 1 or — ambiguous — 2). */
    public List<SignInAccount> findForSignIn(String identifier) {
        return jdbc.sql(FIND_FOR_SIGN_IN).param("identifier", identifier).query(AccountRepository::signIn).list();
    }

    public Optional<SignInAccount> findSignInById(UUID id) {
        return jdbc.sql(FIND_SIGN_IN_BY_ID).param("id", id).query(AccountRepository::signIn).optional();
    }

    public void recordFailure(UUID id, OffsetDateTime now, int threshold, int maxLockMinutes) {
        jdbc.sql(RECORD_FAILURE).param("id", id).param("now", now).param("threshold", threshold)
                .param("maxMinutes", maxLockMinutes).update();
    }

    public void recordSuccess(UUID id, OffsetDateTime now) {
        jdbc.sql(RECORD_SUCCESS).param("id", id).param("now", now).update();
    }

    public Optional<MeAccountEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AccountRepository::account).optional();
    }

    public boolean hasActiveSecondFactor(UUID user) {
        return Boolean.TRUE.equals(jdbc.sql(HAS_ACTIVE_MFA).param("user", user).query(Boolean.class).single());
    }

    public boolean usernameTaken(String username) {
        return Boolean.TRUE.equals(jdbc.sql(USERNAME_TAKEN).param("username", username).query(Boolean.class).single());
    }

    /** A new active account with a password (bootstrap administrator). */
    public void insert(UUID id, UUID tenant, String username, String passwordHash, String userType, OffsetDateTime now,
                       UUID actor) {
        jdbc.sql(INSERT).param("id", id).param("tenant", tenant).param("username", username)
                .param("passwordHash", passwordHash).param("userType", userType).param("now", now)
                .param("actor", actor).update();
    }

    private static SignInAccount signIn(ResultSet rs, int row) throws SQLException {
        return new SignInAccount(
                rs.getObject("id", UUID.class),
                Optional.ofNullable(rs.getObject("person_id", UUID.class)),
                rs.getString("password_hash"),
                rs.getString("status"),
                Optional.ofNullable(rs.getObject("locked_until", OffsetDateTime.class)),
                Optional.ofNullable(rs.getObject("account_expires_at", OffsetDateTime.class)));
    }

    private static MeAccountEntity account(ResultSet rs, int row) throws SQLException {
        return MeAccountEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .personId(rs.getObject("person_id", UUID.class))
                .username(rs.getString("username"))
                .email(rs.getString("email"))
                .mobileNo(rs.getString("mobile_no"))
                .status(rs.getString("status"))
                .emailVerifiedAt(rs.getObject("email_verified_at", OffsetDateTime.class))
                .mobileVerifiedAt(rs.getObject("mobile_verified_at", OffsetDateTime.class))
                .mustResetPassword(rs.getBoolean("must_reset_password"))
                .referredByUserId(rs.getObject("referred_by_user_id", UUID.class))
                .termsAcceptedAt(rs.getObject("terms_accepted_at", OffsetDateTime.class))
                .locale(rs.getString("locale"))
                .timezone(rs.getString("timezone"))
                .lastLoginAt(rs.getObject("last_login_at", OffsetDateTime.class))
                .accountExpiresAt(rs.getObject("account_expires_at", OffsetDateTime.class))
                .failedLoginCount(rs.getInt("failed_login_count"))
                .lastLoginRoleId(rs.getObject("last_login_role_id", UUID.class))
                .registeredAt(rs.getObject("registered_at", OffsetDateTime.class))
                .joinedOn(rs.getObject("joined_on", LocalDate.class))
                .externalRef(rs.getString("external_ref"))
                .signature(rs.getString("signature"))
                .useSignature(rs.getBoolean("use_signature"))
                .userType(rs.getString("user_type"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    public int setCredentials(UUID id, String passwordHash, String username, boolean mustReset, OffsetDateTime now,
                              UUID actor) {
        return jdbc.sql(SET_CREDENTIALS).param("id", id).param("hash", passwordHash).param("username", username)
                .param("mustReset", mustReset).param("now", now).param("actor", actor).update();
    }

    public int acceptTerms(UUID id, OffsetDateTime now) {
        return jdbc.sql(ACCEPT_TERMS).param("id", id).param("now", now).update();
    }

    /** Whether another account (not {@code id}; any account when null) signs in with this value. */
    public boolean identifierTaken(String value, UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(IDENTIFIER_TAKEN).param("value", value).param("id", id)
                .query(Boolean.class).single());
    }
}
