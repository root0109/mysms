package com.mysms.lms.business;

import com.mysms.lms.model.GradeCell;
import com.mysms.lms.model.ReleaseRuleRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Conditional release, decided without I/O (an Interpreter over the rule rows, unit-tested): for each student the
 * cohort's enabled rules are tried in order; a rule matches when every one of its conditions holds on its source item
 * (a rule without conditions never matches), and a match of a rule that stops later rules ends that student's
 * evaluation. Every target of a tried rule is released when at least one tried rule that targets it matched, and
 * withdrawn otherwise; targets only skipped rules reach are left as they are.
 *
 * <p>Conditions: {@code completed} — the lesson completed, or any other item graded or excused; {@code submitted} —
 * submitted or graded (a quiz also by any attempt); {@code score_at_least} / {@code score_below} — the graded percent
 * against the value; {@code viewed} — opened at least once; {@code date_after} / {@code date_before} — today against
 * the value (YYYY-MM-DD); {@code attempts_at_least} — quiz attempts. The {@code not_} forms negate. A value that is not
 * a number or a date where one is needed makes its condition false.
 */
final class ReleaseRuleEvaluator {

    private ReleaseRuleEvaluator() {
    }

    /** What the conditions are tested on, keyed "user|id". */
    record Facts(Map<String, GradeCell> cells, Set<String> completedLessons, Set<String> viewed,
            Map<String, Integer> attempts, LocalDate today) {
    }

    /** One decided grant. */
    record Decision(UUID userId, UUID contentItemId, UUID chapterId, boolean released) {
    }

    static List<Decision> evaluate(List<ReleaseRuleRow> rules, List<UUID> students, Facts facts) {
        Map<String, Decision> decisions = new LinkedHashMap<>();
        for (UUID student : students) {
            for (ReleaseRuleRow rule : rules) {
                boolean matched = !rule.conditions().isEmpty()
                        && rule.conditions().stream().allMatch(condition -> holds(condition, rule, student, facts));
                for (ReleaseRuleRow.Target target : rule.targets()) {
                    String key = student + "|" + target.contentItemId();
                    Decision before = decisions.get(key);
                    boolean released = matched || before != null && before.released();
                    UUID chapter = before != null && before.chapterId() != null ? before.chapterId()
                            : target.chapterId();
                    decisions.put(key, new Decision(student, target.contentItemId(), chapter, released));
                }
                if (matched && rule.stopIfTrue()) {
                    break;
                }
            }
        }
        return List.copyOf(decisions.values());
    }

    static boolean holds(ReleaseRuleRow.Condition condition, ReleaseRuleRow rule, UUID student, Facts facts) {
        String item = student + "|" + rule.sourceItemId();
        GradeCell cell = facts.cells().get(item);
        String status = cell == null ? GradeCell.NOT_SUBMITTED : cell.status();
        return switch (condition.type()) {
            case "completed" -> completed(rule, student, status, facts);
            case "not_completed" -> !completed(rule, student, status, facts);
            case "submitted" -> submitted(rule, student, status, facts);
            case "not_submitted" -> !submitted(rule, student, status, facts);
            case "score_at_least" -> score(cell, condition.value(), true);
            case "score_below" -> score(cell, condition.value(), false);
            case "viewed" -> facts.viewed().contains(item);
            case "date_after" -> date(condition.value(), facts.today(), true);
            case "date_before" -> date(condition.value(), facts.today(), false);
            case "attempts_at_least" -> attempts(rule, student, condition.value(), facts);
            default -> false;
        };
    }

    private static boolean completed(ReleaseRuleRow rule, UUID student, String status, Facts facts) {
        if ("lesson".equals(rule.sourceKind())) {
            return facts.completedLessons().contains(student + "|" + rule.sourceLessonId());
        }
        return GradeCell.GRADED.equals(status) || GradeCell.EXCUSED.equals(status);
    }

    private static boolean submitted(ReleaseRuleRow rule, UUID student, String status, Facts facts) {
        return GradeCell.GRADED.equals(status) || GradeCell.SUBMITTED.equals(status)
                || rule.sourceQuizId() != null && facts.attempts().getOrDefault(student + "|" + rule.sourceQuizId(), 0) > 0;
    }

    private static boolean score(GradeCell cell, String value, boolean atLeast) {
        BigDecimal threshold = number(value);
        if (cell == null || threshold == null || !GradeCalculator.counts(cell)) {
            return false;
        }
        int compared = GradeCalculator.percent(cell).compareTo(threshold);
        return atLeast ? compared >= 0 : compared < 0;
    }

    private static boolean date(String value, LocalDate today, boolean after) {
        try {
            LocalDate date = LocalDate.parse(value == null ? "" : value.trim());
            return after ? today.isAfter(date) : today.isBefore(date);
        } catch (DateTimeParseException notADate) {
            return false;
        }
    }

    private static boolean attempts(ReleaseRuleRow rule, UUID student, String value, Facts facts) {
        BigDecimal threshold = number(value);
        if (rule.sourceQuizId() == null || threshold == null) {
            return false;
        }
        int made = facts.attempts().getOrDefault(student + "|" + rule.sourceQuizId(), 0);
        return BigDecimal.valueOf(made).compareTo(threshold) >= 0;
    }

    private static BigDecimal number(String value) {
        try {
            return value == null ? null : new BigDecimal(value.trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }
}
