package com.mysms.learning.lms.business;

import com.mysms.learning.lms.api.model.BlankAnswer;
import com.mysms.learning.lms.api.model.HotspotPoint;
import com.mysms.learning.lms.api.model.MatchAnswer;
import com.mysms.learning.lms.api.model.QuizAnswerInput;
import com.mysms.learning.lms.model.QuizAnswerKey;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Marks one answer against its {@link QuizAnswerKey}: pure, no I/O, the one place the nine question types are judged
 * (a Strategy per type, chosen by the question's type). An answer is right or wrong as a whole — no partial credit —
 * and a right answer earns the question's marks. With negative marking a wrong answer that was given (not left blank)
 * costs the quiz's penalty; a blank answer earns nothing. Open-ended answers are left for the teacher.
 */
public final class QuizGrader {

    /** Longest answer text a pattern is matched against (bounds the regular-expression work). */
    static final int MAX_PATTERN_INPUT = 2000;

    private QuizGrader() {
    }

    /** The marking of one answer: {@code correct} and {@code marks} are null while a teacher still has to mark it. */
    public record Marked(Boolean correct, BigDecimal marks, boolean answered, String feedback) {

        public boolean pending() {
            return correct == null;
        }
    }

    /**
     * @param penalty marks a wrong given answer costs (null or zero: no negative marking)
     */
    public static Marked mark(QuizAnswerKey key, QuizAnswerInput answer, BigDecimal penalty) {
        boolean answered = answered(answer);
        if (key.markedByTeacher() && answered) {
            return new Marked(null, null, true, key.generalFeedback());
        }
        boolean correct = answered && !key.markedByTeacher() && judge(key, answer);
        BigDecimal marks;
        if (correct) {
            marks = key.marks();
        } else if (answered && penalty != null && penalty.signum() > 0) {
            marks = penalty.negate();
        } else {
            marks = BigDecimal.ZERO;
        }
        String feedback = correct ? key.correctFeedback() : key.incorrectFeedback();
        return new Marked(correct, marks.setScale(2), answered, feedback == null ? key.generalFeedback() : feedback);
    }

    /** Whether the answer is right, by the question's type (an unanswered question is never right). */
    static boolean judge(QuizAnswerKey key, QuizAnswerInput answer) {
        return switch (key.questionType()) {
            case "choices" -> choices(key, answer);
            case "true_false" -> trueFalse(key, answer);
            case "user_input" -> anyText(key.acceptedAnswers(), answer.getAnswerText(), key.caseSensitive());
            case "numeric" -> numeric(key, answer.getNumericValue() != null ? answer.getNumericValue()
                    : answer.getAnswerText());
            case "fill_blank" -> blanks(key, answer.getBlankAnswers());
            case "matching" -> matching(key, answer.getMatchAnswers());
            case "hotspot" -> hotspot(key, answer.getHotspotPoints());
            case "pattern" -> pattern(key, answer.getAnswerText());
            default -> false;
        };
    }

    /** True when the student gave anything for the question. */
    static boolean answered(QuizAnswerInput answer) {
        return answer != null && (notEmpty(answer.getSelectedOptionIds()) || hasText(answer.getAnswerText())
                || hasText(answer.getNumericValue()) || notEmpty(answer.getBlankAnswers())
                || notEmpty(answer.getMatchAnswers()) || notEmpty(answer.getHotspotPoints()));
    }

    private static boolean choices(QuizAnswerKey key, QuizAnswerInput answer) {
        Set<UUID> chosen = answer.getSelectedOptionIds() == null ? Set.of() : new HashSet<>(answer.getSelectedOptionIds());
        Set<UUID> correct = Set.copyOf(key.correctOptionIds());
        if (!key.allowsMultiple() && chosen.size() > 1) {
            return false;
        }
        return !correct.isEmpty() && chosen.equals(correct);
    }

    private static boolean trueFalse(QuizAnswerKey key, QuizAnswerInput answer) {
        String given = answer.getAnswerText();
        String text = given == null ? "" : given.trim().toLowerCase(Locale.ROOT);
        Boolean expected = key.trueFalseAnswer();
        if (expected == null || !("true".equals(text) || "false".equals(text))) {
            return false;
        }
        return expected == Boolean.parseBoolean(text);
    }

    private static boolean anyText(List<String> accepted, String given, boolean caseSensitive) {
        if (given == null) {
            return false;
        }
        String answer = normalise(given, caseSensitive);
        return accepted.stream().anyMatch(text -> normalise(text, caseSensitive).equals(answer));
    }

    private static boolean numeric(QuizAnswerKey key, String given) {
        BigDecimal value = number(given);
        BigDecimal expected = number(key.numericAnswer());
        if (value == null || expected == null) {
            return false;
        }
        BigDecimal second = number(key.numericTolerance());
        if (key.numericIsRange()) {
            return second != null && value.compareTo(expected.min(second)) >= 0
                    && value.compareTo(expected.max(second)) <= 0;
        }
        BigDecimal tolerance = second == null ? BigDecimal.ZERO : second.abs();
        return value.subtract(expected).abs().compareTo(tolerance) <= 0;
    }

    private static boolean blanks(QuizAnswerKey key, List<BlankAnswer> given) {
        if (key.blanks().isEmpty() || given == null) {
            return false;
        }
        Map<UUID, String> byBlank = given.stream().filter(b -> b != null && b.getBlankId() != null)
                .collect(Collectors.toMap(BlankAnswer::getBlankId, b -> b.getText() == null ? "" : b.getText(),
                        (first, second) -> second));
        return key.blanks().stream().allMatch(blank -> anyText(blank.acceptedTexts(), byBlank.get(blank.id()),
                key.caseSensitive()));
    }

    /** Every left item matched to its own pair's right item, each exactly once. */
    private static boolean matching(QuizAnswerKey key, List<MatchAnswer> given) {
        if (key.matchPairIds().isEmpty() || given == null || given.size() != key.matchPairIds().size()) {
            return false;
        }
        Set<UUID> lefts = new HashSet<>();
        for (MatchAnswer match : given) {
            if (match == null || match.getLeftId() == null || !match.getLeftId().equals(match.getRightId())
                    || !key.matchPairIds().contains(match.getLeftId()) || !lefts.add(match.getLeftId())) {
                return false;
            }
        }
        return true;
    }

    /** Every correct region holds a point, and no point lies outside the correct regions. */
    private static boolean hotspot(QuizAnswerKey key, List<HotspotPoint> given) {
        List<QuizAnswerKey.Region> correct = key.regions().stream().filter(QuizAnswerKey.Region::correct).toList();
        if (correct.isEmpty() || given == null || given.isEmpty()) {
            return false;
        }
        List<double[]> points = given.stream().map(p -> p == null ? null : point(p)).toList();
        if (points.contains(null)) {
            return false;
        }
        Function<double[], Boolean> inAnyCorrect = p -> correct.stream().anyMatch(r -> contains(r, p));
        return points.stream().allMatch(inAnyCorrect::apply)
                && correct.stream().allMatch(r -> points.stream().anyMatch(p -> contains(r, p)));
    }

    private static boolean pattern(QuizAnswerKey key, String given) {
        if (given == null || given.length() > MAX_PATTERN_INPUT) {
            return false;
        }
        int flags = key.caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        for (String expression : key.patterns()) {
            try {
                if (Pattern.compile(expression, flags).matcher(given.trim()).matches()) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                // an unusable pattern accepts nothing; the others are still tried
            }
        }
        return false;
    }

    static boolean contains(QuizAnswerKey.Region region, double[] p) {
        double[] c = numbers(region.coordinates());
        if (c == null) {
            return false;
        }
        switch (region.shape()) {
            case "rect":
                return c.length == 4 && p[0] >= Math.min(c[0], c[2]) && p[0] <= Math.max(c[0], c[2])
                        && p[1] >= Math.min(c[1], c[3]) && p[1] <= Math.max(c[1], c[3]);
            case "circle":
                return c.length == 3 && Math.hypot(p[0] - c[0], p[1] - c[1]) <= c[2];
            case "polygon":
                return c.length >= 6 && c.length % 2 == 0 && insidePolygon(c, p);
            default:
                return false;
        }
    }

    /** Ray casting: the point is inside when a ray from it crosses the outline an odd number of times. */
    private static boolean insidePolygon(double[] c, double[] p) {
        boolean inside = false;
        int n = c.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = c[2 * i];
            double yi = c[2 * i + 1];
            double xj = c[2 * j];
            double yj = c[2 * j + 1];
            if ((yi > p[1]) != (yj > p[1]) && p[0] < (xj - xi) * (p[1] - yi) / (yj - yi) + xi) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double[] point(HotspotPoint point) {
        BigDecimal x = number(point.getX());
        BigDecimal y = number(point.getY());
        return x == null || y == null ? null : new double[] {x.doubleValue(), y.doubleValue()};
    }

    private static double[] numbers(String coordinates) {
        String[] parts = coordinates == null ? new String[0] : coordinates.split("[,\\s]+");
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            BigDecimal value = number(parts[i]);
            if (value == null) {
                return null;
            }
            values[i] = value.doubleValue();
        }
        return values.length == 0 ? null : values;
    }

    static BigDecimal number(String text) {
        if (text == null || text.isBlank() || text.length() > 50) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalise(String text, boolean caseSensitive) {
        String collapsed = text.trim().replaceAll("\\s+", " ");
        return caseSensitive ? collapsed : collapsed.toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean notEmpty(List<?> values) {
        return values != null && !values.isEmpty();
    }
}
