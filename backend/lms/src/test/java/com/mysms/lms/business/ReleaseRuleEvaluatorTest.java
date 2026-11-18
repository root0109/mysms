package com.mysms.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.lms.model.GradeCell;
import com.mysms.lms.model.ReleaseRuleRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The conditional-release interpreter: condition kinds, rule order, stop-if-true, released versus withdrawn. */
class ReleaseRuleEvaluatorTest {

    private static final UUID SOURCE = UUID.randomUUID();
    private static final UUID TARGET = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID QUIZ_ID = UUID.randomUUID();
    private static final UUID ASHA = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    private static ReleaseRuleRow rule(boolean stop, ReleaseRuleRow.Condition... conditions) {
        return new ReleaseRuleRow(UUID.randomUUID(), SOURCE, "assignment", null, null, stop, List.of(conditions),
                List.of(new ReleaseRuleRow.Target(TARGET, null)));
    }

    private static Facts graded(String points, String possible) {
        return new Facts(Map.of(ASHA + "|" + SOURCE, new GradeCell(SOURCE, ASHA, UUID.randomUUID(),
                points == null ? null : new java.math.BigDecimal(points),
                possible == null ? null : new java.math.BigDecimal(possible), GradeCell.GRADED, true)));
    }

    /** Facts with defaults, so each test states only what it is about. */
    private record Facts(Map<String, GradeCell> cells) {
        ReleaseRuleEvaluator.Facts build() {
            return new ReleaseRuleEvaluator.Facts(cells, Set.of(), Set.of(), Map.of(), TODAY);
        }
    }

    @Test
    void aScoreGateReleasesAtTheThresholdAndWithdrawsBelowIt() {
        ReleaseRuleRow gate = rule(false, new ReleaseRuleRow.Condition("score_at_least", "70"));
        List<ReleaseRuleEvaluator.Decision> passed = ReleaseRuleEvaluator.evaluate(List.of(gate), List.of(ASHA),
                graded("7", "10").build());
        assertThat(passed).containsExactly(new ReleaseRuleEvaluator.Decision(ASHA, TARGET, null, true));
        List<ReleaseRuleEvaluator.Decision> failed = ReleaseRuleEvaluator.evaluate(List.of(gate), List.of(ASHA),
                graded("6.9", "10").build());
        assertThat(failed).containsExactly(new ReleaseRuleEvaluator.Decision(ASHA, TARGET, null, false));
    }

    @Test
    void aRuleWithoutConditionsNeverMatchesAndBadValuesAreFalse() {
        assertThat(ReleaseRuleEvaluator.evaluate(List.of(rule(false)), List.of(ASHA), graded("9", "10").build()))
                .containsExactly(new ReleaseRuleEvaluator.Decision(ASHA, TARGET, null, false));
        assertThat(ReleaseRuleEvaluator.evaluate(List.of(rule(false,
                new ReleaseRuleRow.Condition("score_at_least", "not a number"))), List.of(ASHA),
                graded("9", "10").build()).get(0).released()).isFalse();
    }

    @Test
    void everyConditionMustHoldAndOneMatchingRuleIsEnough() {
        ReleaseRuleRow both = rule(false, new ReleaseRuleRow.Condition("score_at_least", "50"),
                new ReleaseRuleRow.Condition("date_after", "2026-11-01"));
        assertThat(ReleaseRuleEvaluator.evaluate(List.of(both), List.of(ASHA), graded("6", "10").build())
                .get(0).released()).isTrue();
        ReleaseRuleRow futureOnly = rule(false, new ReleaseRuleRow.Condition("date_after", "2026-12-01"));
        assertThat(ReleaseRuleEvaluator.evaluate(List.of(futureOnly, both), List.of(ASHA),
                graded("6", "10").build()).get(0).released()).as("the later rule still releases the target").isTrue();
    }

    @Test
    void aMatchedStopRuleEndsTheStudentsEvaluation() {
        ReleaseRuleRow stop = rule(true, new ReleaseRuleRow.Condition("completed", null));
        UUID otherTarget = UUID.randomUUID();
        ReleaseRuleRow later = new ReleaseRuleRow(UUID.randomUUID(), SOURCE, "assignment", null, null, false,
                List.of(new ReleaseRuleRow.Condition("completed", null)),
                List.of(new ReleaseRuleRow.Target(otherTarget, null)));
        List<ReleaseRuleEvaluator.Decision> decisions = ReleaseRuleEvaluator.evaluate(List.of(stop, later),
                List.of(ASHA), graded("5", "10").build());
        assertThat(decisions).as("the second rule was skipped, so its target is untouched")
                .containsExactly(new ReleaseRuleEvaluator.Decision(ASHA, TARGET, null, true));
    }

    @Test
    void lessonsQuizzesViewsAndAttemptsHaveTheirOwnFacts() {
        ReleaseRuleRow lesson = new ReleaseRuleRow(UUID.randomUUID(), SOURCE, "lesson", LESSON, null, false,
                List.of(new ReleaseRuleRow.Condition("completed", null)),
                List.of(new ReleaseRuleRow.Target(TARGET, null)));
        ReleaseRuleEvaluator.Facts finished = new ReleaseRuleEvaluator.Facts(Map.of(),
                Set.of(ASHA + "|" + LESSON), Set.of(), Map.of(), TODAY);
        assertThat(ReleaseRuleEvaluator.evaluate(List.of(lesson), List.of(ASHA), finished).get(0).released())
                .isTrue();

        ReleaseRuleRow quiz = new ReleaseRuleRow(UUID.randomUUID(), SOURCE, "quiz", null, QUIZ_ID, false,
                List.of(new ReleaseRuleRow.Condition("attempts_at_least", "2")),
                List.of(new ReleaseRuleRow.Target(TARGET, null)));
        ReleaseRuleEvaluator.Facts attempts = new ReleaseRuleEvaluator.Facts(Map.of(), Set.of(), Set.of(),
                Map.of(ASHA + "|" + QUIZ_ID, 2), TODAY);
        assertThat(ReleaseRuleEvaluator.evaluate(List.of(quiz), List.of(ASHA), attempts).get(0).released()).isTrue();

        ReleaseRuleRow viewed = rule(false, new ReleaseRuleRow.Condition("viewed", null));
        ReleaseRuleEvaluator.Facts opened = new ReleaseRuleEvaluator.Facts(Map.of(),
                Set.of(), Set.of(ASHA + "|" + SOURCE), Map.of(), TODAY);
        assertThat(ReleaseRuleEvaluator.evaluate(List.of(viewed), List.of(ASHA), opened).get(0).released()).isTrue();
        assertThat(ReleaseRuleEvaluator.holds(new ReleaseRuleRow.Condition("not_submitted", null), quiz, ASHA,
                attempts)).as("two attempts count as submitted").isFalse();
    }
}
