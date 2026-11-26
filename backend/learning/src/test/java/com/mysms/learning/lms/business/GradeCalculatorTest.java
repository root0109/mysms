package com.mysms.learning.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.learning.lms.model.GradeCell;
import com.mysms.learning.lms.model.GradedItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The gradebook arithmetic with hand-worked numbers: what counts, category percents, weighting, item statistics. */
class GradeCalculatorTest {

    private static final UUID HOMEWORK_CATEGORY = UUID.randomUUID();
    private static final UUID QUIZ_CATEGORY = UUID.randomUUID();
    private static final UUID HOMEWORK = UUID.randomUUID();
    private static final UUID QUIZ = UUID.randomUUID();
    private static final UUID STUDENT = UUID.randomUUID();

    private static final List<GradedItem> ITEMS = List.of(
            new GradedItem(HOMEWORK, "Homework 1", "assignment", HOMEWORK_CATEGORY, BigDecimal.TEN, null, null, null),
            new GradedItem(QUIZ, "Quiz 1", "quiz", QUIZ_CATEGORY, BigDecimal.valueOf(20), null, null, null));
    private static final Map<UUID, GradeCalculator.Category> WEIGHTED = Map.of(
            HOMEWORK_CATEGORY, new GradeCalculator.Category(HOMEWORK_CATEGORY, BigDecimal.valueOf(60), false),
            QUIZ_CATEGORY, new GradeCalculator.Category(QUIZ_CATEGORY, BigDecimal.valueOf(40), false));

    private static GradeCell cell(UUID item, String points, String possible, String status) {
        return new GradeCell(item, STUDENT, UUID.randomUUID(), points == null ? null : new BigDecimal(points),
                possible == null ? null : new BigDecimal(possible), status, true);
    }

    @Test
    void onlyGradedEntriesWithPointsCount() {
        assertThat(GradeCalculator.counts(cell(HOMEWORK, "8", "10", GradeCell.GRADED))).isTrue();
        assertThat(GradeCalculator.counts(cell(HOMEWORK, null, "10", GradeCell.GRADED))).isFalse();
        assertThat(GradeCalculator.counts(cell(HOMEWORK, "8", "0", GradeCell.GRADED))).isFalse();
        assertThat(GradeCalculator.counts(cell(HOMEWORK, "8", "10", GradeCell.SUBMITTED))).isFalse();
        assertThat(GradeCalculator.counts(cell(HOMEWORK, "8", "10", GradeCell.EXCUSED))).isFalse();
    }

    @Test
    void theTotalIsTheWeightedMeanOfTheCategoryPercents() {
        // 8/10 homework = 80%, 15/20 quiz = 75%: (60×80 + 40×75) / 100 = 78.00
        GradeCalculator.StudentGrade grade = GradeCalculator.studentGrade(ITEMS, WEIGHTED, List.of(
                cell(HOMEWORK, "8", "10", GradeCell.GRADED), cell(QUIZ, "15", "20", GradeCell.GRADED)));
        assertThat(grade.categoryPercents()).containsEntry(HOMEWORK_CATEGORY, new BigDecimal("80.00"))
                .containsEntry(QUIZ_CATEGORY, new BigDecimal("75.00"));
        assertThat(grade.totalPercent()).contains(new BigDecimal("78.00"));
    }

    @Test
    void anExcusedCategoryLeavesItsWeightOut() {
        // only the quiz is in play: (40×50) / 40 = 50.00
        GradeCalculator.StudentGrade grade = GradeCalculator.studentGrade(ITEMS, WEIGHTED, List.of(
                cell(HOMEWORK, null, null, GradeCell.EXCUSED), cell(QUIZ, "10", "20", GradeCell.GRADED)));
        assertThat(grade.categoryPercents()).containsOnlyKeys(QUIZ_CATEGORY);
        assertThat(grade.totalPercent()).contains(new BigDecimal("50.00"));
    }

    @Test
    void withoutWeightsTheTotalIsPointsOverPossible() {
        // (8 + 15) / (10 + 20) = 76.67, and a student with nothing graded has no total
        GradeCalculator.StudentGrade grade = GradeCalculator.studentGrade(ITEMS, Map.of(), List.of(
                cell(HOMEWORK, "8", "10", GradeCell.GRADED), cell(QUIZ, "15", "20", GradeCell.GRADED)));
        assertThat(grade.totalPercent()).contains(new BigDecimal("76.67"));
        assertThat(GradeCalculator.studentGrade(ITEMS, Map.of(), List.of()).totalPercent()).isEmpty();
    }

    @Test
    void equalItemWeightingAveragesThePercentsInsteadOfThePoints() {
        // 2/10 (20%) and 20/20 (100%) average to 60.00, not (2+20)/(10+20) = 73.33
        UUID both = UUID.randomUUID();
        Map<UUID, GradeCalculator.Category> equal = Map.of(both,
                new GradeCalculator.Category(both, BigDecimal.valueOf(100), true));
        List<GradedItem> items = List.of(
                new GradedItem(HOMEWORK, "Homework 1", "assignment", both, BigDecimal.TEN, null, null, null),
                new GradedItem(QUIZ, "Quiz 1", "quiz", both, BigDecimal.valueOf(20), null, null, null));
        GradeCalculator.StudentGrade grade = GradeCalculator.studentGrade(items, equal, List.of(
                cell(HOMEWORK, "2", "10", GradeCell.GRADED), cell(QUIZ, "20", "20", GradeCell.GRADED)));
        assertThat(grade.totalPercent()).contains(new BigDecimal("60.00"));
    }

    @Test
    void itemStatisticsTellTheAverageMedianAndWhoIsMissing() {
        // 80%, 75% and 50% graded; one more student submitted ungraded; of 6 active students 2 are unaccounted
        GradeCalculator.ItemStats stats = GradeCalculator.itemStats(List.of(
                cell(QUIZ, "16", "20", GradeCell.GRADED), cell(QUIZ, "15", "20", GradeCell.GRADED),
                cell(QUIZ, "10", "20", GradeCell.GRADED), cell(QUIZ, null, "20", GradeCell.SUBMITTED)), 6);
        assertThat(stats.averagePercent()).isEqualTo(new BigDecimal("68.33"));
        assertThat(stats.medianPercent()).isEqualTo(new BigDecimal("75.00"));
        assertThat(stats.submitted()).isEqualTo(4);
        assertThat(stats.graded()).isEqualTo(3);
        assertThat(stats.missing()).isEqualTo(2);
        GradeCalculator.ItemStats none = GradeCalculator.itemStats(List.of(), 3);
        assertThat(none.averagePercent()).isNull();
        assertThat(none.missing()).isEqualTo(3);
    }
}
