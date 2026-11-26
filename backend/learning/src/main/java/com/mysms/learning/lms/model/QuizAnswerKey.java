package com.mysms.learning.lms.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Everything needed to mark one answer to one question of an attempt: the question's type and settings, the marks it
 * carries in this attempt, and its key — correct options, accepted texts, blanks, match pairs, hot-spot regions and
 * patterns, as the question bank holds them when the attempt is marked.
 *
 * @param marks the marks the question carries in the quiz (the attempt's snapshot when it has one)
 */
public record QuizAnswerKey(UUID questionId, String questionType, String questionText, boolean allowsMultiple,
                            BigDecimal marks, boolean caseSensitive, Boolean trueFalseAnswer, String numericAnswer,
                            String numericTolerance, boolean numericIsRange, String generalFeedback,
                            String correctFeedback, String incorrectFeedback, List<Option> options,
                            List<String> acceptedAnswers, List<Blank> blanks, Set<UUID> matchPairIds,
                            List<Region> regions, List<String> patterns) {

    public QuizAnswerKey {
        options = List.copyOf(options);
        acceptedAnswers = List.copyOf(acceptedAnswers);
        blanks = List.copyOf(blanks);
        matchPairIds = Set.copyOf(matchPairIds);
        regions = List.copyOf(regions);
        patterns = List.copyOf(patterns);
    }

    /** One option of a choices question. */
    public record Option(UUID id, boolean correct, String explanation) {
    }

    /** One blank of a fill-in question with the texts it accepts. */
    public record Blank(UUID id, List<String> acceptedTexts) {

        public Blank {
            acceptedTexts = List.copyOf(acceptedTexts);
        }
    }

    /** One region of a hot-spot image: {@code rect} (x1,y1,x2,y2), {@code circle} (cx,cy,r) or {@code polygon}. */
    public record Region(String shape, String coordinates, boolean correct) {
    }

    /** The ids of the correct options, in the bank's order. */
    public List<UUID> correctOptionIds() {
        return options.stream().filter(Option::correct).map(Option::id).toList();
    }

    /** Open-ended answers are marked by a teacher (GradeQuizAnswers); every other type is marked on submission. */
    public boolean markedByTeacher() {
        return "open_ended".equals(questionType);
    }
}
