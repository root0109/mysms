package com.mysms.hrms.payroll.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.hrms.payroll.business.PayslipCalculator.Component;
import com.mysms.hrms.payroll.business.PayslipCalculator.Line;
import com.mysms.hrms.payroll.business.PayslipCalculator.Result;
import com.mysms.hrms.payroll.business.PayslipCalculator.Salary;
import com.mysms.hrms.payroll.business.PayslipCalculator.Segment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The payslip arithmetic against amounts worked out by hand (the working is beside each expectation). */
class PayslipCalculatorTest {

    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_31 = LocalDate.of(2026, 1, 31);
    private static final UUID OLD = UUID.randomUUID();
    private static final UUID NEW = UUID.randomUUID();

    private static Component component(UUID salary, String code, String type, String amount) {
        return new Component(UUID.randomUUID(), salary, code, code + " name", type, new BigDecimal(amount));
    }

    private static List<String> amounts(Result result) {
        return result.lines().stream().map(line -> line.code() + " " + line.amount().toPlainString()).toList();
    }

    @Test
    void aSalaryInForceThroughoutThePeriodPaysItsComponentsInFull() {
        List<Segment> segments = PayslipCalculator.segments(JAN_1, JAN_31,
                List.of(new Salary(OLD, LocalDate.of(2025, 4, 1))));
        assertThat(segments).containsExactly(new Segment(OLD, JAN_1, JAN_31));

        Result result = PayslipCalculator.calculate(JAN_1, segments, List.of(
                component(OLD, "BASIC", "earning", "30000.00"),
                component(OLD, "HRA", "earning", "12000.50"),
                component(OLD, "PF", "deduction", "3600.00"),
                component(OLD, "TAX", "deduction", "1250.25"),
                component(OLD, "EPF", "defined", "1800.00"),
                component(NEW, "BASIC", "earning", "99999.00")));

        assertThat(amounts(result)).as("another salary's components are not this payslip's")
                .containsExactly("BASIC 30000.00", "HRA 12000.50", "PF 3600.00", "TAX 1250.25", "EPF 1800.00");
        // 30000.00 + 12000.50; the employer-defined 1800.00 is listed and counts in neither total
        assertThat(result.gross()).isEqualByComparingTo("42000.50");
        // 3600.00 + 1250.25
        assertThat(result.deductions()).isEqualByComparingTo("4850.25");
        // 42000.50 - 4850.25
        assertThat(result.net()).isEqualByComparingTo("37150.25");
        assertThat(result.net().scale()).isEqualTo(2);
    }

    @Test
    void aSalaryChangeInsideThePeriodPaysEachSalaryForItsDays() {
        // January has 31 days: the old salary is in force 1-15 (15 days), the new one 16-31 (16 days)
        List<Segment> segments = PayslipCalculator.segments(JAN_1, JAN_31, List.of(
                new Salary(NEW, LocalDate.of(2026, 1, 16)),
                new Salary(OLD, LocalDate.of(2025, 4, 1)),
                new Salary(UUID.randomUUID(), LocalDate.of(2024, 4, 1)),
                new Salary(UUID.randomUUID(), LocalDate.of(2026, 2, 1))));
        assertThat(segments).as("only the salary in force on the first day and the ones that start in the period")
                .containsExactly(new Segment(OLD, JAN_1, LocalDate.of(2026, 1, 15)),
                        new Segment(NEW, LocalDate.of(2026, 1, 16), JAN_31));

        Result result = PayslipCalculator.calculate(JAN_1, segments, List.of(
                component(OLD, "BASIC", "earning", "30000.00"),
                component(OLD, "HRA", "earning", "12000.50"),
                component(OLD, "PF", "deduction", "3600.00"),
                component(NEW, "BASIC", "earning", "36000.00"),
                component(NEW, "HRA", "earning", "14400.00"),
                component(NEW, "PF", "deduction", "4320.00"),
                component(NEW, "EPF", "defined", "1800.00")));

        assertThat(amounts(result)).containsExactly(
                "BASIC 14516.13",   // 30000.00 x 15 / 31 = 14516.129...
                "HRA 5806.69",      // 12000.50 x 15 / 31 = 5806.693...
                "PF 1741.94",       // 3600.00 x 15 / 31 = 1741.935...
                "BASIC 18580.65",   // 36000.00 x 16 / 31 = 18580.645...
                "HRA 7432.26",      // 14400.00 x 16 / 31 = 7432.258...
                "PF 2229.68",       // 4320.00 x 16 / 31 = 2229.677...
                "EPF 929.03");      // 1800.00 x 16 / 31 = 929.032...
        // 14516.13 + 5806.69 + 18580.65 + 7432.26
        assertThat(result.gross()).isEqualByComparingTo("46335.73");
        // 1741.94 + 2229.68
        assertThat(result.deductions()).isEqualByComparingTo("3971.62");
        // 46335.73 - 3971.62
        assertThat(result.net()).isEqualByComparingTo("42364.11");
    }

    @Test
    void aHalfCentIsRoundedUpAndTheTotalsAreTheSumsOfTheRoundedLines() {
        // April has 30 days: 1-15 and 16-30 are 15 days each, so every amount is halved
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);
        List<Segment> segments = PayslipCalculator.segments(start, end,
                List.of(new Salary(OLD, LocalDate.of(2026, 1, 1)), new Salary(NEW, LocalDate.of(2026, 4, 16))));

        Result result = PayslipCalculator.calculate(start, segments, List.of(
                component(OLD, "A", "earning", "100.01"),
                component(OLD, "B", "earning", "0.01"),
                component(OLD, "C", "deduction", "33.33"),
                component(NEW, "A", "earning", "100.01"),
                component(NEW, "C", "deduction", "33.33")));

        assertThat(amounts(result)).containsExactly(
                "A 50.01",    // 100.01 / 2 = 50.005, half-up (half-even would keep 50.00)
                "B 0.01",     // 0.01 / 2 = 0.005
                "C 16.67",    // 33.33 / 2 = 16.665
                "A 50.01",
                "C 16.67");
        // 50.01 + 0.01 + 50.01: the rounded lines, one cent more than the unrounded 100.02 would give
        assertThat(result.gross()).isEqualByComparingTo("100.03");
        // 16.67 + 16.67
        assertThat(result.deductions()).isEqualByComparingTo("33.34");
        assertThat(result.net()).isEqualByComparingTo("66.69");
        assertThat(result.lines().stream().filter(line -> "earning".equals(line.type())).map(Line::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(result.gross());
    }

    @Test
    void aFirstSalaryThatStartsInsideThePeriodPaysFromItsFirstDayOnly() {
        // March has 31 days; in force 17-31 = 15 days
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);
        List<Segment> segments = PayslipCalculator.segments(start, end,
                List.of(new Salary(NEW, LocalDate.of(2026, 3, 17))));
        assertThat(segments).containsExactly(new Segment(NEW, LocalDate.of(2026, 3, 17), end));

        Result result = PayslipCalculator.calculate(start, segments, List.of(
                component(NEW, "BASIC", "earning", "25000.00"),
                component(NEW, "PF", "deduction", "2500.00")));

        assertThat(amounts(result)).containsExactly(
                "BASIC 12096.77",   // 25000.00 x 15 / 31 = 12096.774...
                "PF 1209.68");      // 2500.00 x 15 / 31 = 1209.677...
        assertThat(result.net()).isEqualByComparingTo("10887.09");
    }

    @Test
    void deductionsLargerThanTheEarningsGiveANegativeNetAndNoSalaryGivesNothing() {
        Result negative = PayslipCalculator.calculate(JAN_1, List.of(new Segment(OLD, JAN_1, JAN_31)),
                List.of(component(OLD, "BASIC", "earning", "100.00"), component(OLD, "LOAN", "deduction", "250.50")));
        assertThat(negative.net()).isEqualByComparingTo("-150.50");

        assertThat(PayslipCalculator.segments(JAN_1, JAN_31, List.of(new Salary(NEW, LocalDate.of(2026, 2, 1)))))
                .isEmpty();
        Result nothing = PayslipCalculator.calculate(JAN_1, List.of(), List.of());
        assertThat(nothing.lines()).isEmpty();
        assertThat(nothing.net()).isEqualByComparingTo("0.00");
    }

    @Test
    void halfAMonthPaysItsDaysOfTheMonthAndTwoHalvesPayTheMonthOnce() {
        LocalDate jan15 = LocalDate.of(2026, 1, 15);
        LocalDate jan16 = LocalDate.of(2026, 1, 16);
        List<Salary> salary = List.of(new Salary(OLD, LocalDate.of(2025, 4, 1)));
        List<Component> components = List.of(
                component(OLD, "BASIC", "earning", "30000.00"),
                component(OLD, "HRA", "earning", "100.01"),
                component(OLD, "PF", "deduction", "0.03"));

        Result first = PayslipCalculator.calculate(JAN_1,
                PayslipCalculator.segments(JAN_1, jan15, salary), components);
        assertThat(amounts(first)).containsExactly(
                "BASIC 14516.13",   // 30000.00 x 15 / 31 = 14516.129...
                "HRA 48.39",        // 100.01 x 15 / 31 = 48.391...
                "PF 0.01");         // 0.03 x 15 / 31 = 0.0145...
        Result second = PayslipCalculator.calculate(jan16,
                PayslipCalculator.segments(jan16, JAN_31, salary), components);
        assertThat(amounts(second)).containsExactly(
                "BASIC 15483.87",   // 30000.00 x 16 / 31 = 15483.870...
                "HRA 51.62",        // 100.01 x 16 / 31 = 51.618...
                "PF 0.02");         // 0.03 x 16 / 31 = 0.0154...
        // the two halves pay the month once: 14516.13 + 15483.87 = 30000.00, 48.39 + 51.62 = 100.01, 0.01 + 0.02
        assertThat(first.gross().add(second.gross())).isEqualByComparingTo("30100.01");
        assertThat(first.deductions().add(second.deductions())).isEqualByComparingTo("0.03");
        // 14516.13 + 48.39 - 0.01 and 15483.87 + 51.62 - 0.02
        assertThat(first.net()).isEqualByComparingTo("14564.51");
        assertThat(second.net()).isEqualByComparingTo("15535.47");
    }

    @Test
    void oneDayPaysOneDayOfTheMonth() {
        // February 2026 has 28 days: 2800.00 x 1 / 28 = 100.00
        LocalDate feb10 = LocalDate.of(2026, 2, 10);
        Result day = PayslipCalculator.calculate(feb10,
                PayslipCalculator.segments(feb10, feb10, List.of(new Salary(OLD, LocalDate.of(2025, 4, 1)))),
                List.of(component(OLD, "BASIC", "earning", "2800.00")));
        assertThat(amounts(day)).containsExactly("BASIC 100.00");
    }
}
