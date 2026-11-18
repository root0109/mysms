package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.finance.repository.LedgerRepository.AllocationRow;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Refunds taken from a payment's unallocated part first, then peeled off its slices newest first, exactly (no I/O). */
class LedgerTest {

    private static final OffsetDateTime EARLY = OffsetDateTime.parse("2026-04-01T09:00:00Z");
    private static final OffsetDateTime LATE = OffsetDateTime.parse("2026-04-02T09:00:00Z");

    @Test
    void aRefundGivesBackTheLatestSliceFirstAndNeverMoreThanASliceHeld() {
        UUID payment = UUID.randomUUID();
        UUID invoice = UUID.randomUUID();
        UUID due = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID first = new UUID(1, 1);
        UUID second = new UUID(1, 2);
        AllocationRow toInvoice = new AllocationRow(first, payment, invoice, null, d("10000.00"), EARLY,
                d("15000.00"));
        AllocationRow toDue = new AllocationRow(second, payment, null, due, d("5000.00"), EARLY,
                d("15000.00"));
        AllocationRow untouched = new AllocationRow(UUID.randomUUID(), other, invoice, null, d("250.00"), LATE,
                d("250.00"));

        Ledger ledger = Ledger.of(List.of(toInvoice, toDue, untouched), Map.of(payment, d("6000.00")));
        assertThat(ledger.held(second)).isEqualByComparingTo("0.00");
        assertThat(ledger.held(first)).isEqualByComparingTo("9000.00");
        assertThat(ledger.paidTo(invoice)).isEqualByComparingTo("9250.00");
        assertThat(ledger.paidTo(due)).isEqualByComparingTo("0.00");
        assertThat(ledger.held(UUID.randomUUID())).isEqualByComparingTo("0");

        Ledger whole = Ledger.of(List.of(toInvoice, toDue), Map.of());
        assertThat(whole.paidTo(invoice)).isEqualByComparingTo("10000.00");
        assertThat(whole.paidTo(due)).isEqualByComparingTo("5000.00");
    }

    @Test
    void aRefundGivesBackTheUnallocatedPartOfAPaymentBeforeAnySlice() {
        // invoice 100.00; a counter payment of 40.00, then the gateway captures 100.00: 60.00 settle the invoice and
        // 40.00 stay unallocated; refunding the surplus 40.00 leaves the invoice paid in full
        UUID invoice = UUID.randomUUID();
        UUID counter = UUID.randomUUID();
        UUID online = UUID.randomUUID();
        AllocationRow atCounter = new AllocationRow(new UUID(2, 1), counter, invoice, null, d("40.00"), EARLY,
                d("40.00"));
        AllocationRow captured = new AllocationRow(new UUID(2, 2), online, invoice, null, d("60.00"), LATE,
                d("100.00"));

        Ledger surplus = Ledger.of(List.of(atCounter, captured), Map.of(online, d("40.00")));
        assertThat(surplus.paidTo(invoice)).isEqualByComparingTo("100.00");
        assertThat(surplus.held(captured.id())).isEqualByComparingTo("60.00");

        Ledger more = Ledger.of(List.of(atCounter, captured), Map.of(online, d("55.00")));
        assertThat(more.held(captured.id())).as("15.00 beyond the surplus come off the slice")
                .isEqualByComparingTo("45.00");
        assertThat(more.paidTo(invoice)).isEqualByComparingTo("85.00");

        Ledger all = Ledger.of(List.of(atCounter, captured), Map.of(online, d("100.00"), counter, d("40.00")));
        assertThat(all.paidTo(invoice)).isEqualByComparingTo("0.00");
    }

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }
}
