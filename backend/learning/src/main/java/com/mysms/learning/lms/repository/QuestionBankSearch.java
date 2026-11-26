package com.mysms.learning.lms.repository;

import com.mysms.core.paging.SqlFragment;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The conditions of a question-bank search (SearchQuestionBank) over {@link QuestionRepository}'s rows: live,
 * unretired questions, by text (title, text or code, case-insensitive, the text taken literally), by type, and not
 * already in a given quiz.
 */
public final class QuestionBankSearch {

    private static final String UNRETIRED = "r.is_retired = false";
    private static final String TEXT = "(r.title ILIKE :bankText ESCAPE '\\' OR r.question_text ILIKE :bankText "
            + "ESCAPE '\\' OR r.question_code ILIKE :bankText ESCAPE '\\')";
    private static final String TYPES = "r.question_type = ANY(:bankTypes)";
    private static final String NOT_IN_QUIZ = "NOT EXISTS (SELECT 1 FROM lms.quiz_question qq WHERE "
            + "qq.tenant_id = r.tenant_id AND qq.quiz_id = :bankQuizId AND qq.question_id = r.id "
            + "AND qq.deleted_at IS NULL)";

    private QuestionBankSearch() {
    }

    public static SqlFragment of(String text, List<String> types, UUID excludeQuizId) {
        SqlFragment where = new SqlFragment(UNRETIRED, Map.of());
        if (text != null && !text.isBlank()) {
            where = where.and(new SqlFragment(TEXT, Map.of("bankText", "%" + literal(text.trim()) + "%")));
        }
        if (types != null && !types.isEmpty()) {
            where = where.and(new SqlFragment(TYPES, Map.of("bankTypes", types.toArray(String[]::new))));
        }
        if (excludeQuizId != null) {
            where = where.and(new SqlFragment(NOT_IN_QUIZ, Map.of("bankQuizId", excludeQuizId)));
        }
        return where;
    }

    /** The text with LIKE's wildcards taken literally. */
    static String literal(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
