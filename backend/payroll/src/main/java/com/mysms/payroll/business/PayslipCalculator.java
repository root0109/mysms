package com.mysms.payroll.business;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The arithmetic of one payslip (pure, exact decimals), kept apart from the workflow so every amount can be checked by
 * hand. <p>Component amounts are monthly. A pay period lies within one calendar month; a salary is in force from its
 * effective date until the day before the next one takes effect, and pays {@code amount × days in force ÷ days of the
 * month}, so a month paid in two periods is paid once and days before the first salary are not paid. Every line is
 * rounded half-up to the cent ({@code numeric(14,2)}); the totals sum the rounded lines, so gross = earnings,
 * deductions = deduction lines, net = gross − deductions. An employer-defined component ({@code defined}) is listed and
 * counts in neither total.
 */
final class PayslipCalculator {

    static final String EARNING = "earning";
    static final String DEDUCTION = "deduction";
    /** The scale of every stored amount (numeric(14,2)). */
    static final int SCALE = 2;

    private PayslipCalculator() {
    }

    /** A staff salary as the calculation sees it. */
    record Salary(UUID id, LocalDate effectiveDate) {
    }

    /** A component of a staff salary: its resolved amount, and the pay component it stands for. */
    record Component(UUID id, UUID staffSalaryId, String code, String name, String type, BigDecimal amount) {
    }

    /** The days of the period one salary is in force, both ends counted. */
    record Segment(UUID staffSalaryId, LocalDate from, LocalDate to) {

        long days() {
            return ChronoUnit.DAYS.between(from, to) + 1;
        }
    }

    /** One line of the payslip. */
    record Line(UUID staffSalaryComponentId, String code, String name, String type, BigDecimal amount) {
    }

    /** The lines in the order they are listed, and the totals over them. */
    record Result(List<Line> lines, BigDecimal gross, BigDecimal deductions, BigDecimal net) {

        Result {
            lines = List.copyOf(lines);
        }
    }

    /**
     * The salaries that pay for the period and the days each is in force, in date order: the salary in force on the
     * first day (if any) and every salary that takes effect later in the period.
     */
    static List<Segment> segments(LocalDate periodStart, LocalDate periodEnd, List<Salary> salaries) {
        List<Salary> inOrder = salaries.stream()
                .filter(salary -> !salary.effectiveDate().isAfter(periodEnd))
                .sorted(Comparator.comparing(Salary::effectiveDate))
                .toList();
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < inOrder.size(); i++) {
            Salary salary = inOrder.get(i);
            LocalDate from = salary.effectiveDate().isBefore(periodStart) ? periodStart : salary.effectiveDate();
            LocalDate to = i + 1 < inOrder.size() ? inOrder.get(i + 1).effectiveDate().minusDays(1) : periodEnd;
            if (!to.isBefore(from)) {
                segments.add(new Segment(salary.id(), from, to));
            }
        }
        return segments;
    }

    /**
     * The lines and totals of a payslip for a period within the month of {@code periodStart}, from the salaries in
     * force and their components.
     */
    static Result calculate(LocalDate periodStart, List<Segment> segments, List<Component> components) {
        BigDecimal monthDays = BigDecimal.valueOf(periodStart.lengthOfMonth());
        List<Line> lines = new ArrayList<>();
        BigDecimal gross = BigDecimal.ZERO.setScale(SCALE);
        BigDecimal deductions = BigDecimal.ZERO.setScale(SCALE);
        for (Segment segment : segments) {
            BigDecimal days = BigDecimal.valueOf(segment.days());
            for (Component component : components) {
                if (!segment.staffSalaryId().equals(component.staffSalaryId())) {
                    continue;
                }
                BigDecimal amount = component.amount().multiply(days).divide(monthDays, SCALE, RoundingMode.HALF_UP);
                lines.add(new Line(component.id(), component.code(), component.name(), component.type(), amount));
                if (EARNING.equals(component.type())) {
                    gross = gross.add(amount);
                } else if (DEDUCTION.equals(component.type())) {
                    deductions = deductions.add(amount);
                }
            }
        }
        return new Result(lines, gross, deductions, gross.subtract(deductions));
    }
}
