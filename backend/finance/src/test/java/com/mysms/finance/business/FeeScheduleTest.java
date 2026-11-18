package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.ValidationFailedException;
import com.mysms.finance.repository.FeeScheduleRepository.ComponentRow;
import com.mysms.finance.repository.FeeScheduleRepository.ConcessionRow;
import com.mysms.finance.repository.FeeScheduleRepository.InstallmentRow;
import com.mysms.finance.repository.FeeScheduleRepository.PlanRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The pricing of a fee plan, worked by hand (no I/O). */
class FeeScheduleTest {

    private static final LocalDate START = LocalDate.parse("2026-04-01");
    private final UUID currency = UUID.randomUUID();
    private final UUID tuition = UUID.randomUUID();
    private final UUID bus = UUID.randomUUID();
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test
    void aSplitGivesTheLeftOverCentsToTheSharesCutTheMost() {
        assertThat(FeeSchedule.split(new BigDecimal("100.00"), List.of(d("33.333"), d("33.333"), d("33.334"))))
                .containsExactly(d("33.33"), d("33.33"), d("33.34"));
        // 0.005 rounds up: 10.01 × 50 % = 5.005 → 5.01, the rest 5.00
        assertThat(FeeSchedule.split(new BigDecimal("10.01"), List.of(d("50"), d("50"))))
                .containsExactly(d("5.01"), d("5.00"));
    }

    @Test
    void aSplitOfAFewCentsIsExactAndNeverNegative() {
        // rounding each share on its own would give 0.01, 0.01 and a last share of -0.01 (dropped: 0.02 charged)
        assertThat(FeeSchedule.split(new BigDecimal("0.01"), List.of(d("50"), d("50"), d("0"))))
                .containsExactly(d("0.01"), d("0.00"), d("0.00"));
        assertThat(FeeSchedule.split(new BigDecimal("0.02"), List.of(d("33.333"), d("33.333"), d("33.334"))))
                .containsExactly(d("0.01"), d("0.00"), d("0.01"));
        java.util.Random random = new java.util.Random(20261112L);
        for (int run = 0; run < 2000; run++) {
            BigDecimal total = BigDecimal.valueOf(random.nextInt(500), 2);
            int n = 1 + random.nextInt(6);
            List<BigDecimal> shares = new java.util.ArrayList<>();
            BigDecimal left = d("100.000");
            for (int i = 0; i < n - 1; i++) {
                BigDecimal share = BigDecimal.valueOf(random.nextInt(left.movePointRight(3).intValue() + 1), 3);
                shares.add(share);
                left = left.subtract(share);
            }
            shares.add(left);
            List<BigDecimal> parts = FeeSchedule.split(total, shares);
            assertThat(parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).as("%s by %s", total, shares)
                    .isEqualByComparingTo(total);
            for (int i = 0; i < n; i++) {
                BigDecimal exact = total.multiply(shares.get(i)).divide(new BigDecimal("100"));
                assertThat(parts.get(i).signum()).isNotNegative();
                assertThat(parts.get(i).subtract(exact).abs()).isLessThan(d("0.01"));
            }
        }
    }

    @Test
    void fixedDaysScheduleMovesByThePlansOffsetAndFixedDatesKeepTheirDate() {
        PlanRow before = new PlanRow(UUID.randomUUID(), UUID.randomUUID(), "fixed_days", 10, "before", null, true);
        FeeSchedule days = FeeSchedule.of(before, installments(), components(), new BigDecimal("1000.00"), START);
        assertThat(days.slots()).extracting(FeeSchedule.Slot::dueDate)
                .containsExactly(LocalDate.parse("2026-03-22"), LocalDate.parse("2026-04-21"));

        PlanRow dated = new PlanRow(UUID.randomUUID(), UUID.randomUUID(), "fixed_date", null, null, null, true);
        List<InstallmentRow> withDates = List.of(new InstallmentRow(first, 1, null, LocalDate.parse("2026-05-10"),
                d("40")), new InstallmentRow(second, 2, null, null, d("60")));
        FeeSchedule fixed = FeeSchedule.of(dated, withDates, components(), new BigDecimal("1000.00"), START);
        assertThat(fixed.slots()).extracting(FeeSchedule.Slot::dueDate)
                .containsExactly(START, LocalDate.parse("2026-05-10"));
        assertThat(fixed.slots()).extracting(FeeSchedule.Slot::amount).containsExactly(d("600.00"), d("400.00"));
        assertThat(fixed.currencyId()).isEqualTo(currency);
    }

    @Test
    void theDefaultTotalIsTheSumOfTheFeeHeadsAndAZeroShareWritesNoDue() {
        PlanRow plan = new PlanRow(UUID.randomUUID(), UUID.randomUUID(), "fixed_days", null, null, d("5.00"), true);
        List<ComponentRow> heads = List.of(new ComponentRow(first, tuition, 1, d("100"), d("900.00"), currency),
                new ComponentRow(second, tuition, 1, d("100"), d("900.00"), currency),
                new ComponentRow(second, bus, 2, d("0"), d("100.00"), currency));
        FeeSchedule schedule = FeeSchedule.of(plan, installments(), heads, null, START);
        // 900.00 + 100.00 = 1000.00: 400.00, then 600.00 all Tuition (Bus 0 %)
        assertThat(schedule.slots()).extracting(FeeSchedule.Slot::amount).containsExactly(d("400.00"), d("600.00"));
        List<FeeSchedule.PricedDue> priced = schedule.price(List.of());
        assertThat(priced).extracting(FeeSchedule.PricedDue::discountAmount).containsExactly(d("20.00"), d("30.00"));
        assertThat(priced).extracting(FeeSchedule.PricedDue::net).containsExactly(d("380.00"), d("570.00"));
    }

    @Test
    void aHeadsOwnConcessionWinsAndAnAmountIsABudgetOverTheDues() {
        PlanRow plan = new PlanRow(UUID.randomUUID(), UUID.randomUUID(), "fixed_days", null, null, null, true);
        FeeSchedule schedule = FeeSchedule.of(plan, installments(), components(), new BigDecimal("1000.00"), START);
        UUID own = UUID.randomUUID();
        UUID any = UUID.randomUUID();
        UUID expired = UUID.randomUUID();
        List<FeeSchedule.PricedDue> priced = schedule.price(List.of(
                new ConcessionRow(expired, null, null, "percent", d("50"), null, LocalDate.parse("2026-03-31")),
                new ConcessionRow(any, null, null, "amount", d("500.00"), null, null),
                new ConcessionRow(own, null, tuition, "percent", d("10"), LocalDate.parse("2026-04-15"), null)));
        // installment 1 (400.00, 2026-04-01): Tuition's own is not valid yet, the 500.00 budget takes 400.00
        // installment 2 (600.00, 2026-05-01): Tuition's own 10 % → 60.00
        assertThat(priced).extracting(FeeSchedule.PricedDue::discountAmount).containsExactly(d("400.00"), d("60.00"));
        assertThat(priced).extracting(FeeSchedule.PricedDue::concessionId).containsExactly(any, own);
        assertThat(priced).extracting(FeeSchedule.PricedDue::net).containsExactly(d("0.00"), d("540.00"));
    }

    @Test
    void aPlanThatCannotBeChargedIsRefusedWithEveryReason() {
        PlanRow inactive = new PlanRow(UUID.randomUUID(), UUID.randomUUID(), "fixed_days", null, null, null, false);
        assertThatThrownBy(() -> FeeSchedule.of(inactive, List.of(), List.of(), null, START))
                .isInstanceOf(ValidationFailedException.class);
        PlanRow plan = new PlanRow(UUID.randomUUID(), UUID.randomUUID(), "fixed_days", null, null, null, true);
        List<InstallmentRow> lopsided = List.of(new InstallmentRow(first, 1, 0, null, d("40")),
                new InstallmentRow(second, 2, 30, null, d("50")));
        List<ComponentRow> mixed = List.of(new ComponentRow(first, tuition, 1, d("90"), null, currency),
                new ComponentRow(second, bus, 1, d("100"), d("10.00"), UUID.randomUUID()));
        ValidationFailedException refused = (ValidationFailedException) org.assertj.core.api.Assertions
                .catchThrowable(() -> FeeSchedule.of(plan, lopsided, mixed, null, START));
        assertThat(refused.errors()).extracting(error -> error.code())
                .contains("SHARES", "CURRENCY", "REQUIRED");
        assertThatThrownBy(() -> FeeSchedule.of(plan, installments(), components(), d("-1"), START))
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> FeeSchedule.of(plan, installments(), List.of(), d("100"), START))
                .isInstanceOf(ValidationFailedException.class);
    }

    private List<InstallmentRow> installments() {
        return List.of(new InstallmentRow(first, 1, 0, null, d("40")), new InstallmentRow(second, 2, 30, null,
                d("60")));
    }

    private List<ComponentRow> components() {
        return List.of(new ComponentRow(first, tuition, 1, d("100"), null, currency),
                new ComponentRow(second, tuition, 1, d("100"), null, currency));
    }

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }
}
