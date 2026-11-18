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
import com.mysms.finance.domain.RefundEntity;
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
 * Plain-JDBC access to finance.refund: a ledger table, so refunds are inserted and only their
 * gateway outcome is ever written to them (initiated → processed or failed), never deleted. A refund belongs to the
 * institution of its payment. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RefundRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("paymentId", "r.payment_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("refundMethod", "r.refund_method", ValueType.STRING),
            field("gatewayRefundId", "r.gateway_refund_id", ValueType.STRING),
            field("refundedAt", "r.refunded_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("amount", "r.amount", ValueType.DECIMAL));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RefundEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", RefundEntity::getCreatedAt),
            Map.entry("updateTime", RefundEntity::getUpdatedAt),
            Map.entry("amount", RefundEntity::getAmount));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.payment_id, r.amount, r.reason, r.status, r.gateway_refund_id,
                   r.refund_method, r.refunded_by_user_id, r.refunded_at, r.request_key, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM finance.refund r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String BY_REQUEST = SELECT_LIVE + " AND r.payment_id = :paymentId AND r.request_key = :key";
    private static final String BY_GATEWAY_REFUND = SELECT_LIVE + " AND r.gateway_refund_id = :gatewayRefundId";
    private static final String UNSETTLED_THROUGH_GATEWAY = SELECT_LIVE + """
               AND r.payment_id = :paymentId AND r.status = 'initiated' AND r.refund_method = 'gateway'
             ORDER BY r.id
            """;
    private static final String INSTITUTION_OF = """
            SELECT p.institution_id, p.payer_user_id FROM finance.payment p
             WHERE p.tenant_id = :tenantId
               AND p.id = :paymentId
            """;
    private static final String INSERT = """
            INSERT INTO finance.refund
            (id, tenant_id, payment_id, amount, reason, status, gateway_refund_id, refund_method, refunded_by_user_id,
             refunded_at, request_key, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :paymentId, :amount, :reason, :status, :gatewayRefundId, :refundMethod,
                    :refundedByUserId, :refundedAt, :requestKey, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            """;
    /** The gateway's answer or notification about an initiated refund: its id there, and processed or failed. */
    private static final String SETTLE = """
            UPDATE finance.refund
               SET status = :status, gateway_refund_id = COALESCE(:gatewayRefundId, gateway_refund_id),
                   refunded_at = :refundedAt, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status = 'initiated' AND deleted_at IS NULL
            """;
    private static final String NOTE_GATEWAY_ID = """
            UPDATE finance.refund
               SET gateway_refund_id = :gatewayRefundId, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status = 'initiated' AND gateway_refund_id IS NULL AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.payment_id IN (SELECT p.id FROM finance.payment p WHERE p.tenant_id = :tenantId
                 AND (p.institution_id = ANY(:scopeIds) OR p.institution_id IS NULL)))
            """;
    private static final String REACHABLE_OR_PAID = """
             (:reachAll OR r.payment_id IN (SELECT p.id FROM finance.payment p WHERE p.tenant_id = :tenantId
                 AND (p.institution_id = ANY(:reachIds) OR p.payer_user_id = :caller)))
            """;

    private final JdbcClient jdbc;

    public RefundRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RefundEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RefundRepository::map).optional();
    }

    /** The refund a request with this Idempotency-Key opened on the payment, if any. */
    public Optional<RefundEntity> byRequest(UUID paymentId, UUID key) {
        return jdbc.sql(BY_REQUEST).param("paymentId", paymentId).param("key", key).query(RefundRepository::map)
                .optional();
    }

    public Optional<RefundEntity> byGatewayRefund(String gatewayRefundId) {
        return jdbc.sql(BY_GATEWAY_REFUND).param("gatewayRefundId", gatewayRefundId).query(RefundRepository::map)
                .optional();
    }

    /** The payment's gateway refunds that wait for the gateway's answer (their amount stays committed meanwhile). */
    public List<RefundEntity> unsettledThroughGateway(UUID paymentId) {
        return jdbc.sql(UNSETTLED_THROUGH_GATEWAY).param("paymentId", paymentId).query(RefundRepository::map).list();
    }

    public Page<RefundEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RefundEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RefundRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RefundEntity::getId);
    }

    /** The payment's institution and payer (a refund is theirs). */
    public record Owner(Optional<UUID> institutionId, UUID payerUserId) {
    }

    public Owner ownerOf(UUID paymentId) {
        return jdbc.sql(INSTITUTION_OF).param("paymentId", paymentId)
                .query((rs, row) -> new Owner(Optional.ofNullable(rs.getObject("institution_id", UUID.class)),
                        rs.getObject("payer_user_id", UUID.class)))
                .single();
    }

    public void insert(RefundEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("paymentId", e.getPaymentId())
                .param("amount", e.getAmount())
                .param("reason", e.getReason())
                .param("status", e.getStatus())
                .param("gatewayRefundId", e.getGatewayRefundId())
                .param("refundMethod", e.getRefundMethod())
                .param("refundedByUserId", e.getRefundedByUserId())
                .param("refundedAt", e.getRefundedAt())
                .param("requestKey", e.getRequestKey())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** An initiated refund settles; 0 when it is no longer initiated (settled by an earlier answer). */
    public int settle(UUID id, String status, String gatewayRefundId, OffsetDateTime refundedAt, OffsetDateTime now,
            UUID actor) {
        return jdbc.sql(SETTLE).param("id", id).param("status", status).param("gatewayRefundId", gatewayRefundId)
                .param("refundedAt", refundedAt).param("now", now).param("actor", actor).update();
    }

    /** The gateway accepted the refund but has not paid it out yet: keep its id until the notification. */
    public int noteGatewayId(UUID id, String gatewayRefundId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(NOTE_GATEWAY_ID).param("id", id).param("gatewayRefundId", gatewayRefundId).param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only (a refund's institution is its payment's). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Rows of the fee office's reach, or refunds of payments the caller made. */
    public static SqlFragment reachableOrPaid(DataScope reach, UUID caller) {
        return new SqlFragment(REACHABLE_OR_PAID, Map.of("reachAll", reach.all(), "reachIds",
                reach.institutionArray(), "caller", caller));
    }

    private static List<Object> sortValues(SortSpec sort, RefundEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static RefundEntity map(ResultSet rs, int row) throws SQLException {
        return RefundEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .paymentId(rs.getObject("payment_id", UUID.class))
                .amount(rs.getBigDecimal("amount"))
                .reason(rs.getString("reason"))
                .status(rs.getString("status"))
                .gatewayRefundId(rs.getString("gateway_refund_id"))
                .refundMethod(rs.getString("refund_method"))
                .refundedByUserId(rs.getObject("refunded_by_user_id", UUID.class))
                .refundedAt(rs.getObject("refunded_at", OffsetDateTime.class))
                .requestKey(rs.getObject("request_key", UUID.class))
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
