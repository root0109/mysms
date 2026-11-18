package com.mysms.finance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.finance.config.FinanceDatabaseConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The money ledger of finance: the allocations of the payments that settle dues and invoices (finance.payment_allocation,
 * never updated) and the refunds that take money back (finance.refund). Allocations count only for money received
 * (payments received, or refunded later); refunds only once processed. Read-only projections, constant SQL, the tenant
 * filtered on every table.
 */
@Repository
public class LedgerRepository {

    /**
     * One allocation of a payment that holds money, with the payment's whole amount (the part of the payment no
     * allocation holds is what its refunds give back first, {@code Ledger}).
     */
    public record AllocationRow(UUID id, UUID paymentId, UUID invoiceId, UUID studentFeeDueId, BigDecimal amount,
            OffsetDateTime createdAt, BigDecimal paymentAmount) {
    }

    private static final String ALLOCATIONS_COLUMNS = """
            SELECT a.id, a.payment_id, a.invoice_id, a.student_fee_due_id, a.amount, a.created_at,
                   p.amount AS payment_amount
              FROM finance.payment_allocation a
              JOIN finance.payment p ON p.tenant_id = :tenantId AND p.id = a.payment_id
             WHERE a.tenant_id = :tenantId
               AND a.deleted_at IS NULL AND p.deleted_at IS NULL AND p.status IN ('received', 'refunded')
            """;
    /** Every allocation of the payments that settle any of these dues or invoices (refunds peel across them all). */
    private static final String ALLOCATIONS_TOUCHING = ALLOCATIONS_COLUMNS + """
               AND a.payment_id IN (SELECT t.payment_id FROM finance.payment_allocation t
                                     WHERE t.tenant_id = :tenantId AND t.deleted_at IS NULL
                                       AND (t.student_fee_due_id = ANY(:ids) OR t.invoice_id = ANY(:ids)))
            """;
    private static final String ALLOCATIONS_OF_PAYMENTS = ALLOCATIONS_COLUMNS + """
               AND a.payment_id = ANY(:paymentIds)
            """;
    private static final String REFUNDED = """
            SELECT r.payment_id, SUM(r.amount) AS refunded
              FROM finance.refund r
             WHERE r.tenant_id = :tenantId
               AND r.payment_id = ANY(:paymentIds) AND r.status = 'processed' AND r.deleted_at IS NULL
             GROUP BY r.payment_id
            """;
    private static final String COMMITTED = """
            SELECT COALESCE(SUM(r.amount), 0)
              FROM finance.refund r
             WHERE r.tenant_id = :tenantId
               AND r.payment_id = :paymentId AND r.status IN ('initiated', 'processed') AND r.deleted_at IS NULL
            """;
    private static final String INSERT_ALLOCATIONS = """
            INSERT INTO finance.payment_allocation
            (id, tenant_id, payment_id, invoice_id, student_fee_due_id, amount, currency_id, created_at, created_by,
             updated_at, updated_by, row_version)
            SELECT slice.id, :tenantId, :paymentId, slice.invoice_id, slice.student_fee_due_id, slice.amount,
                   :currencyId, :now, :actor, :now, :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:invoiceIds AS uuid[]), CAST(:dueIds AS uuid[]),
                          CAST(:amounts AS numeric[])) AS slice (id, invoice_id, student_fee_due_id, amount)
            """;

    private final JdbcClient jdbc;

    public LedgerRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The allocations of every payment that settles any of these dues or invoices. */
    public List<AllocationRow> allocationsTouching(Collection<UUID> dueOrInvoiceIds) {
        return jdbc.sql(ALLOCATIONS_TOUCHING).param("ids", dueOrInvoiceIds.toArray(UUID[]::new))
                .query((rs, row) -> allocation(rs)).list();
    }

    /** The allocations of these payments (when they hold money). */
    public List<AllocationRow> allocationsOf(Collection<UUID> paymentIds) {
        return jdbc.sql(ALLOCATIONS_OF_PAYMENTS).param("paymentIds", paymentIds.toArray(UUID[]::new))
                .query((rs, row) -> allocation(rs)).list();
    }

    /** The processed refunds of each payment, summed. */
    public Map<UUID, BigDecimal> refunded(Collection<UUID> paymentIds) {
        Map<UUID, BigDecimal> refunded = new HashMap<>();
        jdbc.sql(REFUNDED).param("paymentIds", paymentIds.toArray(UUID[]::new))
                .query((rs, row) -> refunded.put(rs.getObject("payment_id", UUID.class), rs.getBigDecimal("refunded")))
                .list();
        return refunded;
    }

    /** What the payment has given back or is giving back (refunds initiated or processed). */
    public BigDecimal committedRefunds(UUID paymentId) {
        return jdbc.sql(COMMITTED).param("paymentId", paymentId).query(BigDecimal.class).single();
    }

    /** One slice to write: exactly one of the invoice or the due. */
    public record Slice(UUID id, UUID invoiceId, UUID studentFeeDueId, BigDecimal amount) {
    }

    /** Writes the payment's slices in one statement. */
    public void insertAllocations(UUID paymentId, UUID currencyId, List<Slice> slices, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(INSERT_ALLOCATIONS)
                .param("paymentId", paymentId)
                .param("currencyId", currencyId)
                .param("ids", slices.stream().map(Slice::id).toArray(UUID[]::new))
                .param("invoiceIds", slices.stream().map(Slice::invoiceId).toArray(UUID[]::new))
                .param("dueIds", slices.stream().map(Slice::studentFeeDueId).toArray(UUID[]::new))
                .param("amounts", slices.stream().map(Slice::amount).toArray(BigDecimal[]::new))
                .param("now", now)
                .param("actor", actor)
                .update();
    }

    private static AllocationRow allocation(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AllocationRow(rs.getObject("id", UUID.class), rs.getObject("payment_id", UUID.class),
                rs.getObject("invoice_id", UUID.class), rs.getObject("student_fee_due_id", UUID.class),
                rs.getBigDecimal("amount"), rs.getObject("created_at", OffsetDateTime.class),
                rs.getBigDecimal("payment_amount"));
    }
}
