package com.mysms.learning.lms.model;

import java.util.List;
import java.util.UUID;

/**
 * One enabled release rule of a cohort as the evaluator reads it (not a contract shape): its source item (and what
 * that item is), whether a match stops later rules, its conditions in order and the items it releases (a chapter
 * target stands for every item of the chapter).
 */
public record ReleaseRuleRow(UUID id, UUID sourceItemId, String sourceKind, UUID sourceLessonId, UUID sourceQuizId,
        boolean stopIfTrue, List<Condition> conditions, List<Target> targets) {

    /** One condition: its type (lms.release_rule_condition.condition_type) and value. */
    public record Condition(String type, String value) {
    }

    /** One released item, with the chapter it was reached through (null for an item target). */
    public record Target(UUID contentItemId, UUID chapterId) {
    }
}
