package com.mysms.finance.business;

import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.finance.repository.FeeScheduleRepository.ComponentRow;
import com.mysms.finance.repository.FeeScheduleRepository.ConcessionRow;
import com.mysms.finance.repository.FeeScheduleRepository.InstallmentRow;
import com.mysms.finance.repository.FeeScheduleRepository.PlanRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The dues a fee plan charges one student (pure calculation, no I/O). The plan's total — the request's override, or
 * the sum of the default amounts of the fee heads it includes — is shared out over the installments by their share
 * percentages, and each installment over its fee heads by theirs, each split exact to the cent ({@link #split}), so the
 * dues add up to the total exactly. Each installment falls due on
 * its fixed date, or (fixed_days) the given days after the schedule start, moved by the plan's offset.
 *
 * <p>Per student, the plan's discount percentage comes off every due, then at most one concession: an active one of
 * the student for the year valid on the due date, a fee head's own before an all-heads one. A percent concession
 * takes its share of what is left; an amount concession is a budget spent on the dues in due-date order.
 */
final class FeeSchedule {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int CENTS = 2;
    private static final BigDecimal CENT = new BigDecimal("0.01");

    /** One due of the plan before discounts: the installment, the fee head, when it falls due and its amount. */
    record Slot(UUID installmentId, int sequence, UUID feeHeadId, LocalDate dueDate, BigDecimal amount) {
    }

    /** A slot priced for one student. */
    record PricedDue(Slot slot, BigDecimal discountPercent, BigDecimal discountAmount, UUID concessionId) {

        BigDecimal net() {
            return slot.amount().subtract(discountAmount);
        }
    }

    private final List<Slot> slots;
    private final UUID currencyId;
    private final BigDecimal discountPercent;

    private FeeSchedule(List<Slot> slots, UUID currencyId, BigDecimal discountPercent) {
        this.slots = slots;
        this.currencyId = currencyId;
        this.discountPercent = discountPercent;
    }

    /** The plan's slots; 422 with every reason the plan cannot be charged. */
    static FeeSchedule of(PlanRow plan, List<InstallmentRow> installments, List<ComponentRow> components,
            BigDecimal totalOverride, LocalDate startDate) {
        List<FieldError> errors = new ArrayList<>();
        if (!plan.active()) {
            errors.add(new FieldError("feePlanId", "fee plan is not active", "INACTIVE"));
        }
        if (installments.isEmpty()) {
            errors.add(new FieldError("feePlanId", "fee plan has no installments", "NO_INSTALLMENTS"));
        } else if (sum(installments.stream().map(InstallmentRow::sharePercent).toList()).compareTo(HUNDRED) != 0) {
            errors.add(new FieldError("feePlanId", "installment shares must total 100", "SHARES"));
        }
        Map<UUID, List<ComponentRow>> byInstallment = new HashMap<>();
        components.forEach(c -> byInstallment.computeIfAbsent(c.installmentId(), k -> new ArrayList<>()).add(c));
        for (InstallmentRow installment : installments) {
            List<ComponentRow> heads = byInstallment.getOrDefault(installment.id(), List.of());
            if (heads.isEmpty()) {
                errors.add(new FieldError("feePlanId", "installment " + installment.sequence() + " has no fee heads",
                        "NO_FEE_HEADS"));
            } else if (sum(heads.stream().map(ComponentRow::sharePercent).toList()).compareTo(HUNDRED) != 0) {
                errors.add(new FieldError("feePlanId", "fee head shares of installment " + installment.sequence()
                        + " must total 100", "SHARES"));
            }
        }
        Set<UUID> currencies = new LinkedHashSet<>();
        components.forEach(c -> currencies.add(c.currencyId()));
        if (currencies.size() != 1 || currencies.contains(null)) {
            errors.add(new FieldError("feePlanId", "the plan's fee heads must all name one currency", "CURRENCY"));
        }
        BigDecimal total = totalOverride;
        if (total == null) {
            Map<UUID, BigDecimal> defaults = new HashMap<>();
            components.forEach(c -> defaults.put(c.feeHeadId(), c.defaultAmount()));
            if (defaults.containsValue(null)) {
                errors.add(new FieldError("totalAmount", "a fee head of the plan has no default amount; give the total",
                        "REQUIRED"));
            } else {
                total = sum(List.copyOf(defaults.values()));
            }
        } else if (total.signum() <= 0) {
            errors.add(new FieldError("totalAmount", "must be greater than 0", "Min"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        List<Slot> slots = new ArrayList<>();
        List<BigDecimal> installmentAmounts =
                split(total, installments.stream().map(InstallmentRow::sharePercent).toList());
        for (int i = 0; i < installments.size(); i++) {
            InstallmentRow installment = installments.get(i);
            List<ComponentRow> heads = byInstallment.get(installment.id());
            List<BigDecimal> headAmounts =
                    split(installmentAmounts.get(i), heads.stream().map(ComponentRow::sharePercent).toList());
            LocalDate due = dueDate(plan, installment, startDate);
            for (int h = 0; h < heads.size(); h++) {
                if (headAmounts.get(h).signum() > 0) {
                    slots.add(new Slot(installment.id(), installment.sequence(), heads.get(h).feeHeadId(), due,
                            headAmounts.get(h)));
                }
            }
        }
        slots.sort(Comparator.comparing(Slot::dueDate).thenComparing(Slot::sequence));
        return new FeeSchedule(List.copyOf(slots), currencies.iterator().next(), plan.discountPercent());
    }

    UUID currencyId() {
        return currencyId;
    }

    List<Slot> slots() {
        return slots;
    }

    /** The slots priced for one student with that student's concessions (in their created order). */
    List<PricedDue> price(List<ConcessionRow> concessions) {
        Map<UUID, BigDecimal> budgets = new HashMap<>();
        concessions.stream().filter(c -> "amount".equals(c.type())).forEach(c -> budgets.put(c.id(), c.value()));
        List<PricedDue> priced = new ArrayList<>();
        for (Slot slot : slots) {
            BigDecimal planDiscount = discountPercent == null ? BigDecimal.ZERO.setScale(CENTS)
                    : percentOf(slot.amount(), discountPercent);
            BigDecimal rest = slot.amount().subtract(planDiscount);
            Optional<ConcessionRow> concession = concessionFor(slot, concessions, budgets);
            BigDecimal granted = BigDecimal.ZERO.setScale(CENTS);
            if (concession.isPresent()) {
                ConcessionRow c = concession.get();
                if ("percent".equals(c.type())) {
                    granted = percentOf(rest, c.value());
                } else {
                    granted = budgets.get(c.id()).min(rest).setScale(CENTS, RoundingMode.UNNECESSARY);
                    budgets.put(c.id(), budgets.get(c.id()).subtract(granted));
                }
            }
            priced.add(new PricedDue(slot, discountPercent, planDiscount.add(granted),
                    granted.signum() > 0 ? concession.get().id() : null));
        }
        return priced;
    }

    private static Optional<ConcessionRow> concessionFor(Slot slot, List<ConcessionRow> concessions,
            Map<UUID, BigDecimal> budgets) {
        Optional<ConcessionRow> own = Optional.empty();
        Optional<ConcessionRow> any = Optional.empty();
        for (ConcessionRow c : concessions) {
            boolean usable = validOn(c, slot.dueDate())
                    && (!"amount".equals(c.type()) || budgets.get(c.id()).signum() > 0);
            if (!usable) {
                continue;
            }
            if (Objects.equals(c.feeHeadId(), slot.feeHeadId()) && own.isEmpty()) {
                own = Optional.of(c);
            } else if (c.feeHeadId() == null && any.isEmpty()) {
                any = Optional.of(c);
            }
        }
        return own.isPresent() ? own : any;
    }

    private static boolean validOn(ConcessionRow c, LocalDate date) {
        return date == null || (c.validFrom() == null || !date.isBefore(c.validFrom()))
                && (c.validUntil() == null || !date.isAfter(c.validUntil()));
    }

    private static LocalDate dueDate(PlanRow plan, InstallmentRow installment, LocalDate startDate) {
        if ("fixed_date".equals(plan.scheduleBasis())) {
            return installment.dueDate() == null ? startDate : installment.dueDate();
        }
        int offset = plan.offsetDays() == null ? 0 : plan.offsetDays();
        LocalDate base = "before".equals(plan.offsetDirection()) ? startDate.minusDays(offset)
                : startDate.plusDays(offset);
        return base.plusDays(installment.dueDays() == null ? 0 : installment.dueDays());
    }

    /**
     * {@code total} shared by the percentages exactly to the cent: each share gets its exact part rounded down, and
     * the cents left go one each to the shares cut the most (the earlier on a tie). Never negative; adds up to total.
     */
    static List<BigDecimal> split(BigDecimal total, List<BigDecimal> shares) {
        List<BigDecimal> parts = new ArrayList<>();
        List<BigDecimal> cut = new ArrayList<>();
        BigDecimal given = BigDecimal.ZERO;
        for (BigDecimal share : shares) {
            BigDecimal exact = total.multiply(share).divide(HUNDRED);
            BigDecimal part = exact.setScale(CENTS, RoundingMode.FLOOR);
            parts.add(part);
            cut.add(exact.subtract(part));
            given = given.add(part);
        }
        int cents = total.subtract(given).movePointRight(CENTS).intValueExact();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < shares.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparing((Integer i) -> cut.get(i)).reversed().thenComparing(i -> i));
        for (int i = 0; i < cents; i++) {
            int share = order.get(i % order.size());
            parts.set(share, parts.get(share).add(CENT));
        }
        return parts;
    }

    private static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(HUNDRED, CENTS, RoundingMode.HALF_UP);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
