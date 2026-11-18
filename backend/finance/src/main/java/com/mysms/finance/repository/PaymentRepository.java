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
import com.mysms.finance.domain.PaymentEntity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
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
 * Plain-JDBC access to finance.payment: live rows only, keyset paging over an allowlisted order, the payment steps
 * guarded by the state they leave, and the lock the receipt and refund workflows take. Every statement filters the
 * caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PaymentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("payerUserId", "r.payer_user_id", ValueType.UUID),
            field("payerPersonId", "r.payer_person_id", ValueType.UUID),
            field("purpose", "r.purpose", ValueType.STRING),
            field("invoiceId", "r.invoice_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("paymentMethod", "r.payment_method", ValueType.STRING),
            field("receiptNumber", "r.receipt_number", ValueType.STRING),
            field("gatewayOrderId", "r.gateway_order_id", ValueType.STRING),
            field("gatewayPaymentId", "r.gateway_payment_id", ValueType.STRING),
            field("currencyId", "r.currency_id", ValueType.UUID),
            field("receivedAt", "r.received_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("amount", "r.amount", ValueType.DECIMAL));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    /** The number series of receipts for counter payments. */
    public static final String RECEIPT_NUMBERS = "receipt";

    private static final Map<String, Function<PaymentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PaymentEntity::getCreatedAt),
            Map.entry("updateTime", PaymentEntity::getUpdatedAt),
            Map.entry("amount", PaymentEntity::getAmount));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.payer_user_id, r.purpose, r.invoice_id, r.lms_course_id,
                   r.lms_cohort_id, r.admission_application_id, r.gateway_name, r.gateway_order_id,
                   r.gateway_payment_id, r.original_amount, r.discount_amount, r.amount, r.amount_with_tax,
                   r.currency_id, r.coupon_id, r.coupon_code, r.billing_name, r.billing_address_id,
                   r.tax_registration_number, r.tax_identity_enc, r.acquisition_source_id, r.payer_consent, r.status,
                   r.received_at, r.payer_person_id, r.payer_name, r.payment_method, r.receipt_number,
                   r.instrument_reference, r.instrument_date, r.bank_name, r.gateway_account_id,
                   r.received_by_user_id, r.gateway_currency, r.gateway_amount_minor, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM finance.payment r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String LOCK = SELECT_LIVE + " AND r.id = :id FOR UPDATE";
    private static final String BY_ORDER = SELECT_LIVE + """
               AND r.gateway_name = :gatewayName AND r.gateway_order_id = :orderId
            """;
    private static final String BY_GATEWAY_PAYMENT = SELECT_LIVE + """
               AND r.gateway_name = :gatewayName AND r.gateway_payment_id = :gatewayPaymentId
            """;
    /** The payments of a student: allocated to their dues or invoices, or opened for one of their invoices. */
    private static final String OF_STUDENT = SELECT_LIVE + """
               AND (r.id IN (SELECT a.payment_id FROM finance.payment_allocation a
                              WHERE a.tenant_id = :tenantId AND a.deleted_at IS NULL
                                AND (a.student_fee_due_id = ANY(:dueIds) OR a.invoice_id = ANY(:invoiceIds)))
                    OR r.invoice_id = ANY(:invoiceIds))
             ORDER BY r.created_at, r.id
            """;
    private static final String INSERT = """
            INSERT INTO finance.payment
            (id, tenant_id, institution_id, payer_user_id, purpose, invoice_id, gateway_name, gateway_order_id,
             original_amount, discount_amount, amount, amount_with_tax, currency_id, billing_name, billing_address_id,
             tax_registration_number, tax_identity_enc, payer_consent, status, received_at, payer_person_id,
             payer_name, payment_method, receipt_number, instrument_reference, instrument_date, bank_name,
             gateway_account_id, received_by_user_id, gateway_currency, gateway_amount_minor, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :payerUserId, :purpose, :invoiceId, :gatewayName, :gatewayOrderId,
                    :originalAmount, :discountAmount, :amount, :amountWithTax, :currencyId, :billingName,
                    :billingAddressId, :taxRegistrationNumber, :taxIdentityEnc, :payerConsent, :status, :receivedAt,
                    :payerPersonId, :payerName, :paymentMethod, :receiptNumber, :instrumentReference,
                    :instrumentDate, :bankName, :gatewayAccountId, :receivedByUserId, :gatewayCurrency,
                    :gatewayAmountMinor, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            """;
    /** The gateway captured the money of an order that waits for it (or failed before). */
    private static final String RECEIVE = """
            UPDATE finance.payment
               SET status = 'received', gateway_payment_id = :gatewayPaymentId, received_at = :now,
                   updated_at = :now, updated_by = NULL, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status IN ('initiated', 'failed') AND deleted_at IS NULL
            """;
    private static final String FAIL = """
            UPDATE finance.payment
               SET status = 'failed', updated_at = :now, updated_by = NULL, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status = 'initiated' AND deleted_at IS NULL
            """;
    /** Every unit of the payment went back: the payment is refunded. */
    private static final String MARK_REFUNDED = """
            UPDATE finance.payment
               SET status = 'refunded', updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status = 'received' AND deleted_at IS NULL
            """;
    private static final String ALLOCATIONS = """
            SELECT a.id, a.payment_id, a.invoice_id, a.student_fee_due_id, a.amount, a.currency_id
              FROM finance.payment_allocation a
             WHERE a.tenant_id = :tenantId
               AND a.payment_id = ANY(:paymentIds) AND a.deleted_at IS NULL
             ORDER BY a.created_at, a.id
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;
    private static final String REACHABLE_OR_PAID = """
             (:reachAll OR r.institution_id = ANY(:reachIds) OR r.payer_user_id = :caller)
            """;

    private final JdbcClient jdbc;

    public PaymentRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PaymentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PaymentRepository::map).optional();
    }

    /** The payment, locked for the rest of the transaction (receipt and refunds of one payment run one at a time). */
    public Optional<PaymentEntity> lock(UUID id) {
        return jdbc.sql(LOCK).param("id", id).query(PaymentRepository::map).optional();
    }

    public Optional<PaymentEntity> byOrder(String gatewayName, String orderId) {
        return jdbc.sql(BY_ORDER).param("gatewayName", gatewayName).param("orderId", orderId)
                .query(PaymentRepository::map).optional();
    }

    public Optional<PaymentEntity> byGatewayPayment(String gatewayName, String gatewayPaymentId) {
        return jdbc.sql(BY_GATEWAY_PAYMENT).param("gatewayName", gatewayName)
                .param("gatewayPaymentId", gatewayPaymentId).query(PaymentRepository::map).optional();
    }

    public Page<PaymentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PaymentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PaymentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PaymentEntity::getId);
    }

    public List<PaymentEntity> ofStudent(Collection<UUID> dueIds, Collection<UUID> invoiceIds) {
        return jdbc.sql(OF_STUDENT).param("dueIds", dueIds.toArray(UUID[]::new))
                .param("invoiceIds", invoiceIds.toArray(UUID[]::new)).query(PaymentRepository::map).list();
    }

    public void insert(PaymentEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("payerUserId", e.getPayerUserId())
                .param("purpose", e.getPurpose())
                .param("invoiceId", e.getInvoiceId())
                .param("gatewayName", e.getGatewayName())
                .param("gatewayOrderId", e.getGatewayOrderId())
                .param("originalAmount", e.getOriginalAmount())
                .param("discountAmount", e.getDiscountAmount())
                .param("amount", e.getAmount())
                .param("amountWithTax", e.getAmountWithTax())
                .param("currencyId", e.getCurrencyId())
                .param("billingName", e.getBillingName())
                .param("billingAddressId", e.getBillingAddressId())
                .param("taxRegistrationNumber", e.getTaxRegistrationNumber())
                .param("taxIdentityEnc", e.getTaxIdentityEnc())
                .param("payerConsent", e.isPayerConsent())
                .param("status", e.getStatus())
                .param("receivedAt", e.getReceivedAt())
                .param("payerPersonId", e.getPayerPersonId())
                .param("payerName", e.getPayerName())
                .param("paymentMethod", e.getPaymentMethod())
                .param("receiptNumber", e.getReceiptNumber())
                .param("instrumentReference", e.getInstrumentReference())
                .param("instrumentDate", e.getInstrumentDate())
                .param("bankName", e.getBankName())
                .param("gatewayAccountId", e.getGatewayAccountId())
                .param("receivedByUserId", e.getReceivedByUserId())
                .param("gatewayCurrency", e.getGatewayCurrency())
                .param("gatewayAmountMinor", e.getGatewayAmountMinor())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    public int receive(UUID id, String gatewayPaymentId, OffsetDateTime now) {
        return jdbc.sql(RECEIVE).param("id", id).param("gatewayPaymentId", gatewayPaymentId).param("now", now)
                .update();
    }

    public int fail(UUID id, OffsetDateTime now) {
        return jdbc.sql(FAIL).param("id", id).param("now", now).update();
    }

    public int markRefunded(UUID id, OffsetDateTime now, UUID actor) {
        return jdbc.sql(MARK_REFUNDED).param("id", id).param("now", now).param("actor", actor).update();
    }

    /** One allocation as the contract shows it. */
    public record AllocationView(UUID id, UUID paymentId, UUID invoiceId, UUID studentFeeDueId, BigDecimal amount,
            UUID currencyId) {
    }

    /** The allocations of these payments, in the order they were made. */
    public List<AllocationView> allocations(Collection<UUID> paymentIds) {
        return jdbc.sql(ALLOCATIONS).param("paymentIds", paymentIds.toArray(UUID[]::new))
                .query((rs, row) -> new AllocationView(rs.getObject("id", UUID.class),
                        rs.getObject("payment_id", UUID.class), rs.getObject("invoice_id", UUID.class),
                        rs.getObject("student_fee_due_id", UUID.class), rs.getBigDecimal("amount"),
                        rs.getObject("currency_id", UUID.class)))
                .list();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Rows of the fee office's reach, or payments the caller made. */
    public static SqlFragment reachableOrPaid(DataScope reach, UUID caller) {
        return new SqlFragment(REACHABLE_OR_PAID, Map.of("reachAll", reach.all(), "reachIds",
                reach.institutionArray(), "caller", caller));
    }

    private static List<Object> sortValues(SortSpec sort, PaymentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PaymentEntity map(ResultSet rs, int row) throws SQLException {
        return PaymentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .payerUserId(rs.getObject("payer_user_id", UUID.class))
                .purpose(rs.getString("purpose"))
                .invoiceId(rs.getObject("invoice_id", UUID.class))
                .lmsCourseId(rs.getObject("lms_course_id", UUID.class))
                .lmsCohortId(rs.getObject("lms_cohort_id", UUID.class))
                .admissionApplicationId(rs.getObject("admission_application_id", UUID.class))
                .gatewayName(rs.getString("gateway_name"))
                .gatewayOrderId(rs.getString("gateway_order_id"))
                .gatewayPaymentId(rs.getString("gateway_payment_id"))
                .originalAmount(rs.getBigDecimal("original_amount"))
                .discountAmount(rs.getBigDecimal("discount_amount"))
                .amount(rs.getBigDecimal("amount"))
                .amountWithTax(rs.getBigDecimal("amount_with_tax"))
                .currencyId(rs.getObject("currency_id", UUID.class))
                .couponId(rs.getObject("coupon_id", UUID.class))
                .couponCode(rs.getString("coupon_code"))
                .billingName(rs.getString("billing_name"))
                .billingAddressId(rs.getObject("billing_address_id", UUID.class))
                .taxRegistrationNumber(rs.getString("tax_registration_number"))
                .taxIdentityEnc(rs.getString("tax_identity_enc"))
                .acquisitionSourceId(rs.getObject("acquisition_source_id", UUID.class))
                .payerConsent(rs.getBoolean("payer_consent"))
                .status(rs.getString("status"))
                .receivedAt(rs.getObject("received_at", OffsetDateTime.class))
                .payerPersonId(rs.getObject("payer_person_id", UUID.class))
                .payerName(rs.getString("payer_name"))
                .paymentMethod(rs.getString("payment_method"))
                .receiptNumber(rs.getString("receipt_number"))
                .instrumentReference(rs.getString("instrument_reference"))
                .instrumentDate(rs.getObject("instrument_date", LocalDate.class))
                .bankName(rs.getString("bank_name"))
                .gatewayAccountId(rs.getObject("gateway_account_id", UUID.class))
                .receivedByUserId(rs.getObject("received_by_user_id", UUID.class))
                .gatewayCurrency(rs.getString("gateway_currency"))
                .gatewayAmountMinor(rs.getObject("gateway_amount_minor", Long.class))
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
