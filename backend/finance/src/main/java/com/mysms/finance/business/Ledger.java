package com.mysms.finance.business;

import com.mysms.finance.repository.LedgerRepository.AllocationRow;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What each allocation still holds once refunds are taken back, exactly. A refund first gives back the part of its
 * payment no allocation holds, then is peeled off the payment's allocations newest first.
 */
final class Ledger {

    private final Map<UUID, BigDecimal> held = new HashMap<>();
    private final List<AllocationRow> allocations;

    private Ledger(List<AllocationRow> allocations, Map<UUID, BigDecimal> refundedByPayment) {
        this.allocations = List.copyOf(allocations);
        Map<UUID, List<AllocationRow>> byPayment = new HashMap<>();
        allocations.forEach(a -> byPayment.computeIfAbsent(a.paymentId(), k -> new ArrayList<>()).add(a));
        byPayment.forEach((payment, slices) -> {
            BigDecimal allocated = slices.stream().map(AllocationRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unallocated = slices.get(0).paymentAmount().subtract(allocated).max(BigDecimal.ZERO);
            BigDecimal refund = refundedByPayment.getOrDefault(payment, BigDecimal.ZERO).subtract(unallocated)
                    .max(BigDecimal.ZERO);
            slices.sort(Comparator.comparing(AllocationRow::createdAt).thenComparing(AllocationRow::id).reversed());
            for (AllocationRow slice : slices) {
                BigDecimal peeled = refund.min(slice.amount());
                refund = refund.subtract(peeled);
                held.put(slice.id(), slice.amount().subtract(peeled));
            }
        });
    }

    static Ledger of(List<AllocationRow> allocations, Map<UUID, BigDecimal> refundedByPayment) {
        return new Ledger(allocations, refundedByPayment);
    }

    /** The money held for a due or an invoice: its allocations, net of the refunds peeled off them. */
    BigDecimal paidTo(UUID dueOrInvoiceId) {
        return allocations.stream()
                .filter(a -> dueOrInvoiceId.equals(a.studentFeeDueId()) || dueOrInvoiceId.equals(a.invoiceId()))
                .map(a -> held.get(a.id()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** What one allocation still holds. */
    BigDecimal held(UUID allocationId) {
        return held.getOrDefault(allocationId, BigDecimal.ZERO);
    }
}
