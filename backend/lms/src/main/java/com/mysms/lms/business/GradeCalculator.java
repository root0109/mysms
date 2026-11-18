package com.mysms.lms.business;

import com.mysms.lms.model.GradeCell;
import com.mysms.lms.model.GradedItem;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The gradebook's arithmetic, in one place and without I/O (so it is unit-tested with hand-worked numbers). Money is
 * not involved, but percentages are still exact decimals: every quotient is taken to 10 places and only the answer is
 * rounded, half up, to 2.
 *
 * <ul>
 *   <li>An entry <em>counts</em> when it is graded, has points and the item is out of more than 0 points; excused,
 *       submitted-but-ungraded and missing work counts for nothing.</li>
 *   <li>A category's percent is the points earned over the points possible of its counting entries; with
 *       {@code equalItemWeighting} it is the mean of their percents instead.</li>
 *   <li>The total is the weighted mean of the category percents (weights of categories with counting work only)
 *       when any category in play has a weight; otherwise all counting points over all their possible points.</li>
 * </ul>
 */
final class GradeCalculator {

    private static final MathContext EXACT = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int QUOTIENT_SCALE = 10;
    private static final int SHOWN_SCALE = 2;

    private GradeCalculator() {
    }

    /** One grading category as the arithmetic needs it. */
    record Category(UUID id, BigDecimal weightPercent, boolean equalItemWeighting) {
    }

    /** The statistics of one item across the cohort's active students. */
    record ItemStats(BigDecimal averagePercent, BigDecimal medianPercent, int submitted, int graded, int missing) {
    }

    /** One student's category percents (only categories with counting work) and total; empty total when none. */
    record StudentGrade(Map<UUID, BigDecimal> categoryPercents, Optional<BigDecimal> totalPercent) {
    }

    /** Whether an entry counts toward percentages. */
    static boolean counts(GradeCell cell) {
        return GradeCell.GRADED.equals(cell.status()) && cell.points() != null && cell.pointsPossible() != null
                && cell.pointsPossible().signum() > 0;
    }

    /** The percent of one counting entry, unrounded. */
    static BigDecimal percent(GradeCell cell) {
        return cell.points().multiply(HUNDRED).divide(cell.pointsPossible(), QUOTIENT_SCALE, RoundingMode.HALF_UP);
    }

    /** A percentage as the contract shows it: 2 decimals, half up. */
    static BigDecimal shown(BigDecimal value) {
        return value.setScale(SHOWN_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * The statistics of one item: the average and median percent of its counting entries, how many were submitted
     * (submitted or graded), graded, and missing (active students with neither a submission, a grade nor an excuse).
     */
    static ItemStats itemStats(Collection<GradeCell> cells, int activeStudents) {
        List<BigDecimal> percents = new ArrayList<>();
        int submitted = 0;
        int graded = 0;
        int accounted = 0;
        for (GradeCell cell : cells) {
            boolean isGraded = GradeCell.GRADED.equals(cell.status());
            if (isGraded || GradeCell.SUBMITTED.equals(cell.status())) {
                submitted++;
            }
            if (isGraded) {
                graded++;
            }
            if (isGraded || GradeCell.SUBMITTED.equals(cell.status()) || GradeCell.EXCUSED.equals(cell.status())) {
                accounted++;
            }
            if (counts(cell)) {
                percents.add(percent(cell));
            }
        }
        if (percents.isEmpty()) {
            return new ItemStats(null, null, submitted, graded, Math.max(0, activeStudents - accounted));
        }
        BigDecimal sum = percents.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(percents.size()), QUOTIENT_SCALE, RoundingMode.HALF_UP);
        percents.sort(BigDecimal::compareTo);
        int middle = percents.size() / 2;
        BigDecimal median = percents.size() % 2 == 1 ? percents.get(middle)
                : percents.get(middle - 1).add(percents.get(middle)).divide(BigDecimal.valueOf(2), EXACT);
        return new ItemStats(shown(average), shown(median), submitted, graded, Math.max(0, activeStudents - accounted));
    }

    /** One student's grade from their entries (any of the given items; others are ignored). */
    static StudentGrade studentGrade(List<GradedItem> items, Map<UUID, Category> categories,
            Collection<GradeCell> cells) {
        Map<UUID, GradedItem> byId = new LinkedHashMap<>();
        items.forEach(item -> byId.put(item.contentItemId(), item));
        Map<UUID, List<GradeCell>> byCategory = new LinkedHashMap<>();
        BigDecimal earned = BigDecimal.ZERO;
        BigDecimal possible = BigDecimal.ZERO;
        for (GradeCell cell : cells) {
            GradedItem item = byId.get(cell.contentItemId());
            if (item == null || !counts(cell)) {
                continue;
            }
            earned = earned.add(cell.points());
            possible = possible.add(cell.pointsPossible());
            if (item.gradingCategoryId() != null && categories.containsKey(item.gradingCategoryId())) {
                byCategory.computeIfAbsent(item.gradingCategoryId(), id -> new ArrayList<>()).add(cell);
            }
        }
        Map<UUID, BigDecimal> exact = new LinkedHashMap<>();
        byCategory.forEach((id, counted) -> exact.put(id, categoryPercent(categories.get(id), counted)));
        Map<UUID, BigDecimal> shownPercents = new LinkedHashMap<>();
        exact.forEach((id, value) -> shownPercents.put(id, shown(value)));
        boolean weighted = exact.keySet().stream().map(categories::get).map(Category::weightPercent)
                .anyMatch(Objects::nonNull);
        Optional<BigDecimal> total;
        if (weighted) {
            BigDecimal weightSum = BigDecimal.ZERO;
            BigDecimal weightedSum = BigDecimal.ZERO;
            for (Map.Entry<UUID, BigDecimal> category : exact.entrySet()) {
                BigDecimal weight = categories.get(category.getKey()).weightPercent();
                if (weight != null && weight.signum() > 0) {
                    weightSum = weightSum.add(weight);
                    weightedSum = weightedSum.add(weight.multiply(category.getValue()));
                }
            }
            total = weightSum.signum() == 0 ? Optional.empty()
                    : Optional.of(shown(weightedSum.divide(weightSum, QUOTIENT_SCALE, RoundingMode.HALF_UP)));
        } else {
            total = possible.signum() == 0 ? Optional.empty()
                    : Optional.of(shown(earned.multiply(HUNDRED).divide(possible, QUOTIENT_SCALE, RoundingMode.HALF_UP)));
        }
        return new StudentGrade(shownPercents, total);
    }

    private static BigDecimal categoryPercent(Category category, List<GradeCell> counted) {
        if (category.equalItemWeighting()) {
            BigDecimal sum = counted.stream().map(GradeCalculator::percent).reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.divide(BigDecimal.valueOf(counted.size()), QUOTIENT_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal earned = counted.stream().map(GradeCell::points).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal possible = counted.stream().map(GradeCell::pointsPossible).reduce(BigDecimal.ZERO, BigDecimal::add);
        return earned.multiply(HUNDRED).divide(possible, QUOTIENT_SCALE, RoundingMode.HALF_UP);
    }
}
