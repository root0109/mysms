package com.mysms.platform.org.business;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The terms an academic year's {@code term_structure} divides it into: consecutive parts of (nearly) equal length that
 * cover the year without gaps or overlaps, a day of remainder going to the earlier parts. Quarters nest inside their
 * semesters. {@code other} has no plan: such a year's terms are made by hand.
 */
final class TermPlan {

    /** One planned term; {@code children} nest inside it (quarters of a semester). */
    record Term(String name, int sequence, LocalDate startDate, LocalDate endDate, List<Term> children) {
    }

    private static final Map<String, List<Part>> STRUCTURES = Map.of(
            "two_semesters", parts("Semester", 2),
            "two_semesters_quarterly", List.of(
                    new Part("Semester 1", List.of("Quarter 1", "Quarter 2")),
                    new Part("Semester 2", List.of("Quarter 3", "Quarter 4"))),
            "two_semesters_final", List.of(
                    new Part("Semester 1", List.of("Quarter 1", "Quarter 2")),
                    new Part("Semester 2", List.of("Quarter 3", "Final examinations"))),
            "three_semesters", parts("Semester", 3),
            "four_quarters", parts("Quarter", 4),
            "final_year", List.of(new Part("Final year", List.of())));

    private record Part(String name, List<String> children) {
    }

    private TermPlan() {
    }

    /** The plan of a structure over the year's days; empty for {@code other}, no structure, or an unknown one. */
    static Optional<List<Term>> of(String structure, LocalDate start, LocalDate end) {
        List<Part> parts = structure == null ? null : STRUCTURES.get(structure);
        if (parts == null) {
            return Optional.empty();
        }
        List<LocalDate[]> spans = split(start, end, parts.size());
        List<Term> terms = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            LocalDate[] span = spans.get(i);
            List<Term> children = new ArrayList<>();
            List<LocalDate[]> childSpans = split(span[0], span[1], part.children().size());
            for (int c = 0; c < part.children().size(); c++) {
                children.add(new Term(part.children().get(c), c + 1, childSpans.get(c)[0], childSpans.get(c)[1],
                        List.of()));
            }
            terms.add(new Term(part.name(), i + 1, span[0], span[1], List.copyOf(children)));
        }
        return Optional.of(List.copyOf(terms));
    }

    /** Whether the year is long enough to give every planned term (nested ones included) at least one day. */
    static boolean fits(String structure, LocalDate start, LocalDate end) {
        List<Part> parts = STRUCTURES.get(structure);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        int finest = parts.stream().mapToInt(p -> Math.max(1, p.children().size())).sum();
        return days >= finest;
    }

    private static List<Part> parts(String name, int count) {
        List<Part> parts = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            parts.add(new Part(name + " " + i, List.of()));
        }
        return List.copyOf(parts);
    }

    /** {@code count} consecutive inclusive spans covering start..end; none for a count of zero. */
    private static List<LocalDate[]> split(LocalDate start, LocalDate end, int count) {
        List<LocalDate[]> spans = new ArrayList<>();
        if (count == 0) {
            return spans;
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        long base = days / count;
        long extra = days % count;
        LocalDate from = start;
        for (int i = 0; i < count; i++) {
            long length = base + (i < extra ? 1 : 0);
            LocalDate to = from.plusDays(length - 1);
            spans.add(new LocalDate[] {from, to});
            from = to.plusDays(1);
        }
        return spans;
    }
}
