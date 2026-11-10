package com.mysms.iam.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.domain.UserAccountEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to iam.user_account (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class UserAccountRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userType", "r.user_type", ValueType.STRING),
            field("personId", "r.person_id", ValueType.UUID),
            field("username", "r.username", ValueType.STRING),
            field("email", "r.email", ValueType.STRING),
            field("mobileNo", "r.mobile_no", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("emailVerifiedAt", "r.email_verified_at", ValueType.TIMESTAMP),
            field("mobileVerifiedAt", "r.mobile_verified_at", ValueType.TIMESTAMP),
            field("mustResetPassword", "r.must_reset_password", ValueType.BOOLEAN),
            field("referredByUserId", "r.referred_by_user_id", ValueType.UUID),
            field("termsAcceptedAt", "r.terms_accepted_at", ValueType.TIMESTAMP),
            field("locale", "r.locale", ValueType.STRING),
            field("timezone", "r.timezone", ValueType.STRING),
            field("lastLoginAt", "r.last_login_at", ValueType.TIMESTAMP),
            field("accountExpiresAt", "r.account_expires_at", ValueType.TIMESTAMP),
            field("failedLoginCount", "r.failed_login_count", ValueType.INTEGER),
            field("lastLoginRoleId", "r.last_login_role_id", ValueType.UUID),
            field("registeredAt", "r.registered_at", ValueType.TIMESTAMP),
            field("joinedOn", "r.joined_on", ValueType.DATE),
            field("externalRef", "r.external_ref", ValueType.STRING),
            field("signature", "r.signature", ValueType.STRING),
            field("useSignature", "r.use_signature", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<UserAccountEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", UserAccountEntity::getCreatedAt),
            Map.entry("updateTime", UserAccountEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_type, r.person_id, r.username, r.email, r.mobile_no, r.status,
                   r.email_verified_at, r.mobile_verified_at, r.must_reset_password, r.referred_by_user_id,
                   r.terms_accepted_at, r.locale, r.timezone, r.last_login_at, r.account_expires_at,
                   r.failed_login_count, r.last_login_role_id, r.registered_at, r.joined_on, r.external_ref,
                   r.signature, r.use_signature, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM iam.user_account r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO iam.user_account
            (id, tenant_id, user_type, person_id, username, email, mobile_no, referred_by_user_id, locale, timezone,
             account_expires_at, joined_on, external_ref, signature, use_signature, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userType, :personId, :username, :email, :mobileNo, :referredByUserId, :locale,
                    :timezone, :accountExpiresAt, :joinedOn, :externalRef, :signature,
                    COALESCE(:useSignature, false), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_type, person_id, username, email, mobile_no, status, email_verified_at,
                      mobile_verified_at, must_reset_password, referred_by_user_id, terms_accepted_at, locale, timezone,
                      last_login_at, account_expires_at, failed_login_count, last_login_role_id, registered_at,
                      joined_on, external_ref, signature, use_signature, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE iam.user_account
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE iam.user_account
               SET user_type = :userType,
                   person_id = :personId,
                   username = :username,
                   email = :email,
                   mobile_no = :mobileNo,
                   referred_by_user_id = :referredByUserId,
                   locale = :locale,
                   timezone = :timezone,
                   joined_on = :joinedOn,
                   external_ref = :externalRef,
                   signature = :signature,
                   use_signature = :useSignature,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_LAST_LOGIN_ROLE_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.role WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_REFERRED_BY_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.user_account WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LEGACY_CREDENTIAL_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.legacy_credential WHERE tenant_id = :tenantId
                AND user_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_MFA_CREDENTIAL_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.mfa_credential WHERE tenant_id = :tenantId
                AND user_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_USER_ACCOUNT_REFERRED_BY_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.user_account WHERE tenant_id = :tenantId
                AND referred_by_user_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_USER_DEPARTMENT_ACCESS_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.user_department_access WHERE tenant_id = :tenantId
                AND user_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_USER_PERMISSION_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.user_permission WHERE tenant_id = :tenantId
                AND user_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_USER_ROLE_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM iam.user_role WHERE tenant_id = :tenantId
                AND user_id = :id AND deleted_at IS NULL)
            """;

    /** iam.ActivateUser: allowed while status is inactive. */
    private static final String TRANSITION_ACTIVATE_USER = """
            UPDATE iam.user_account
               SET status = 'active',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** iam.DeactivateUser: allowed while status is active or locked. */
    private static final String TRANSITION_DEACTIVATE_USER = """
            UPDATE iam.user_account
               SET status = 'inactive',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** iam.UnlockUser: allowed in every state. */
    private static final String TRANSITION_UNLOCK_USER = """
            UPDATE iam.user_account
               SET failed_login_count = 0,
                   locked_until = NULL,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public UserAccountRepository(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<UserAccountEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(UserAccountRepository::map).optional();
    }

    public Page<UserAccountEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<UserAccountEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(UserAccountRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), UserAccountEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public UserAccountEntity insert(UserAccountEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userType", e.getUserType())
                .param("personId", e.getPersonId())
                .param("username", e.getUsername())
                .param("email", e.getEmail())
                .param("mobileNo", e.getMobileNo())
                .param("referredByUserId", e.getReferredByUserId())
                .param("locale", e.getLocale())
                .param("timezone", e.getTimezone())
                .param("accountExpiresAt", e.getAccountExpiresAt())
                .param("joinedOn", e.getJoinedOn())
                .param("externalRef", e.getExternalRef())
                .param("signature", e.getSignature())
                .param("useSignature", e.getUseSignature())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(UserAccountRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(UserAccountEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userType", e.getUserType())
                .param("personId", e.getPersonId())
                .param("username", e.getUsername())
                .param("email", e.getEmail())
                .param("mobileNo", e.getMobileNo())
                .param("referredByUserId", e.getReferredByUserId())
                .param("locale", e.getLocale())
                .param("timezone", e.getTimezone())
                .param("joinedOn", e.getJoinedOn())
                .param("externalRef", e.getExternalRef())
                .param("signature", e.getSignature())
                .param("useSignature", e.getUseSignature())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(UserAccountEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced iam.role row exists (live). */
    public boolean existsLastLoginRoleId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LAST_LOGIN_ROLE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced iam.user_account row exists (live). */
    public boolean existsReferredByUserId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_REFERRED_BY_USER_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live iam.legacy_credential rows still reference the row. */
    public boolean referencedByLegacyCredentialUserId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LEGACY_CREDENTIAL_USER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live iam.mfa_credential rows still reference the row. */
    public boolean referencedByMfaCredentialUserId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_MFA_CREDENTIAL_USER_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live iam.user_account rows still reference the row. */
    public boolean referencedByUserAccountReferredByUserId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_USER_ACCOUNT_REFERRED_BY_USER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live iam.user_department_access rows still reference the row. */
    public boolean referencedByUserDepartmentAccessUserId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_USER_DEPARTMENT_ACCESS_USER_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live iam.user_permission rows still reference the row. */
    public boolean referencedByUserPermissionUserId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_USER_PERMISSION_USER_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live iam.user_role rows still reference the row. */
    public boolean referencedByUserRoleUserId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_USER_ROLE_USER_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** iam.ActivateUser; 0 rows means the row changed (or vanished) since it was read. */
    public int activateUser(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_ACTIVATE_USER)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** iam.DeactivateUser; 0 rows means the row changed (or vanished) since it was read. */
    public int deactivateUser(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_DEACTIVATE_USER)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** iam.UnlockUser; 0 rows means the row changed (or vanished) since it was read. */
    public int unlockUser(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_UNLOCK_USER)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    private static List<Object> sortValues(SortSpec sort, UserAccountEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static UserAccountEntity map(ResultSet rs, int row) throws SQLException {
        return UserAccountEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userType(rs.getString("user_type"))
                .personId(rs.getObject("person_id", UUID.class))
                .username(rs.getString("username"))
                .email(rs.getString("email"))
                .mobileNo(rs.getString("mobile_no"))
                .status(rs.getString("status"))
                .emailVerifiedAt(rs.getObject("email_verified_at", OffsetDateTime.class))
                .mobileVerifiedAt(rs.getObject("mobile_verified_at", OffsetDateTime.class))
                .mustResetPassword(rs.getObject("must_reset_password", Boolean.class))
                .referredByUserId(rs.getObject("referred_by_user_id", UUID.class))
                .termsAcceptedAt(rs.getObject("terms_accepted_at", OffsetDateTime.class))
                .locale(rs.getString("locale"))
                .timezone(rs.getString("timezone"))
                .lastLoginAt(rs.getObject("last_login_at", OffsetDateTime.class))
                .accountExpiresAt(rs.getObject("account_expires_at", OffsetDateTime.class))
                .failedLoginCount(rs.getObject("failed_login_count", Integer.class))
                .lastLoginRoleId(rs.getObject("last_login_role_id", UUID.class))
                .registeredAt(rs.getObject("registered_at", OffsetDateTime.class))
                .joinedOn(rs.getObject("joined_on", LocalDate.class))
                .externalRef(rs.getString("external_ref"))
                .signature(rs.getString("signature"))
                .useSignature(rs.getObject("use_signature", Boolean.class))
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
