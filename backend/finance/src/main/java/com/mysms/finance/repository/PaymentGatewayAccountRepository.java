package com.mysms.finance.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.domain.PaymentGatewayAccountEntity;
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
 * Plain-JDBC access to finance.payment_gateway_account: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class PaymentGatewayAccountRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("gatewayName", "r.gateway_name", ValueType.STRING),
            field("displayName", "r.display_name", ValueType.STRING),
            field("merchantId", "r.merchant_id", ValueType.STRING),
            field("keyId", "r.key_id", ValueType.STRING),
            field("currencyId", "r.currency_id", ValueType.UUID),
            field("isDefault", "r.is_default", ValueType.BOOLEAN),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PaymentGatewayAccountEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PaymentGatewayAccountEntity::getCreatedAt),
            Map.entry("updateTime", PaymentGatewayAccountEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.key_secret_enc, r.webhook_secret_enc, r.institution_id, r.gateway_name,
                   r.display_name, r.merchant_id, r.key_id, r.currency_id, r.is_default, r.is_active, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM finance.payment_gateway_account r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO finance.payment_gateway_account
            (id, tenant_id, key_secret_enc, webhook_secret_enc, institution_id, gateway_name, display_name,
             merchant_id, key_id, currency_id, is_default, is_active, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :keySecretEnc, :webhookSecretEnc, :institutionId, :gatewayName, :displayName,
                    :merchantId, :keyId, :currencyId, COALESCE(:isDefault, false), COALESCE(:isActive, true),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, key_secret_enc, webhook_secret_enc, institution_id, gateway_name, display_name,
                      merchant_id, key_id, currency_id, is_default, is_active, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE finance.payment_gateway_account
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE finance.payment_gateway_account
               SET key_secret_enc = :keySecretEnc,
                   webhook_secret_enc = :webhookSecretEnc,
                   institution_id = :institutionId,
                   gateway_name = :gatewayName,
                   display_name = :displayName,
                   merchant_id = :merchantId,
                   key_id = :keyId,
                   currency_id = :currencyId,
                   is_default = :isDefault,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PAYMENT_GATEWAY_ACCOUNT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM finance.payment_gateway_account
                            WHERE tenant_id = :tenantId
                              AND gateway_name = :gatewayName AND display_name = :displayName AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_GATEWAY_EVENT_GATEWAY_ACCOUNT_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.gateway_event WHERE tenant_id = :tenantId
                AND gateway_account_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PAYMENT_GATEWAY_ACCOUNT_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.payment WHERE tenant_id = :tenantId
                AND gateway_account_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public PaymentGatewayAccountRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PaymentGatewayAccountEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PaymentGatewayAccountRepository::map).optional();
    }

    public Page<PaymentGatewayAccountEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PaymentGatewayAccountEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PaymentGatewayAccountRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PaymentGatewayAccountEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PaymentGatewayAccountEntity insert(PaymentGatewayAccountEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("keySecretEnc", e.getKeySecretEnc())
                .param("webhookSecretEnc", e.getWebhookSecretEnc())
                .param("institutionId", e.getInstitutionId())
                .param("gatewayName", e.getGatewayName())
                .param("displayName", e.getDisplayName())
                .param("merchantId", e.getMerchantId())
                .param("keyId", e.getKeyId())
                .param("currencyId", e.getCurrencyId())
                .param("isDefault", e.getIsDefault())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PaymentGatewayAccountRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PaymentGatewayAccountEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("keySecretEnc", e.getKeySecretEnc())
                .param("webhookSecretEnc", e.getWebhookSecretEnc())
                .param("institutionId", e.getInstitutionId())
                .param("gatewayName", e.getGatewayName())
                .param("displayName", e.getDisplayName())
                .param("merchantId", e.getMerchantId())
                .param("keyId", e.getKeyId())
                .param("currencyId", e.getCurrencyId())
                .param("isDefault", e.getIsDefault())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PaymentGatewayAccountEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_payment_gateway_account_1: gateway_name, display_name where (deleted_at IS NULL). */
    public boolean takenGatewayNameDisplayName1(PaymentGatewayAccountEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PAYMENT_GATEWAY_ACCOUNT_1_TAKEN)
                .param("gatewayName", e.getGatewayName())
                .param("displayName", e.getDisplayName())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live finance.gateway_event rows still reference the row. */
    public boolean referencedByGatewayEventGatewayAccountId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_GATEWAY_EVENT_GATEWAY_ACCOUNT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live finance.payment rows still reference the row. */
    public boolean referencedByPaymentGatewayAccountId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PAYMENT_GATEWAY_ACCOUNT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, PaymentGatewayAccountEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PaymentGatewayAccountEntity map(ResultSet rs, int row) throws SQLException {
        return PaymentGatewayAccountEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .keySecretEnc(rs.getString("key_secret_enc"))
                .webhookSecretEnc(rs.getString("webhook_secret_enc"))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .gatewayName(rs.getString("gateway_name"))
                .displayName(rs.getString("display_name"))
                .merchantId(rs.getString("merchant_id"))
                .keyId(rs.getString("key_id"))
                .currencyId(rs.getObject("currency_id", UUID.class))
                .isDefault(rs.getObject("is_default", Boolean.class))
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
