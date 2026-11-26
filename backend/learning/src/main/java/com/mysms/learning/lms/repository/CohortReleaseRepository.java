package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.model.ReleaseRuleRow;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC reads and writes of a cohort's conditional release: its enabled rules with their
 * conditions and targets, the facts the conditions are tested on (completed lessons, viewed items, quiz attempts —
 * grades come from {@link CohortGradebookRepository}) and the upsert of the resulting grants. Every statement filters
 * the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CohortReleaseRepository {

    private static final String RULES = """
            SELECT r.id, r.source_content_item_id, r.stop_if_true, ci.item_kind, ci.lesson_id, ci.quiz_id
              FROM lms.release_rule r
              JOIN lms.content_item ci ON ci.tenant_id = :tenantId AND ci.id = r.source_content_item_id
             WHERE r.tenant_id = :tenantId AND r.cohort_id = :cohortId AND NOT r.is_disabled AND r.deleted_at IS NULL
               AND ci.deleted_at IS NULL
             ORDER BY r.sort_order, r.id
            """;
    private static final String CONDITIONS = """
            SELECT release_rule_id, condition_type, value FROM lms.release_rule_condition
             WHERE tenant_id = :tenantId AND release_rule_id = ANY(:rules) AND deleted_at IS NULL
             ORDER BY sort_order, id
            """;
    private static final String TARGETS = """
            SELECT t.release_rule_id, COALESCE(t.content_item_id, ci.id) AS content_item_id, t.chapter_id
              FROM lms.release_rule_target t
              LEFT JOIN lms.content_item ci ON ci.tenant_id = :tenantId AND ci.chapter_id = t.chapter_id
                    AND ci.cohort_id = :cohortId AND ci.deleted_at IS NULL
             WHERE t.tenant_id = :tenantId AND t.release_rule_id = ANY(:rules) AND t.deleted_at IS NULL
               AND (t.content_item_id IS NOT NULL OR ci.id IS NOT NULL)
             ORDER BY t.id, ci.sort_order, ci.id
            """;
    private static final String COMPLETED_LESSONS = """
            SELECT user_id, lesson_id FROM lms.lesson_progress
             WHERE tenant_id = :tenantId AND user_id = ANY(:users) AND lesson_id = ANY(:lessons)
               AND status = 'complete' AND deleted_at IS NULL
            """;
    private static final String VIEWED = """
            SELECT DISTINCT user_id, content_item_id FROM lms.content_view_session
             WHERE tenant_id = :tenantId AND user_id = ANY(:users) AND content_item_id = ANY(:items)
               AND started_at >= :since AND deleted_at IS NULL
            """;
    private static final String ATTEMPTS = """
            SELECT user_id, quiz_id, count(*) AS attempts FROM lms.quiz_submission
             WHERE tenant_id = :tenantId AND user_id = ANY(:users) AND quiz_id = ANY(:quizzes) AND deleted_at IS NULL
             GROUP BY user_id, quiz_id
            """;
    private static final String UPSERT_GRANT = """
            INSERT INTO lms.release_grant (id, tenant_id, content_item_id, chapter_id, user_id, is_released,
                                           created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :itemId, :chapterId, :userId, :released, :now, :actor, :now, :actor, 1)
            ON CONFLICT (content_item_id, user_id) WHERE deleted_at IS NULL
            DO UPDATE SET is_released = EXCLUDED.is_released, updated_at = EXCLUDED.updated_at,
                          updated_by = EXCLUDED.updated_by, row_version = lms.release_grant.row_version + 1
                    WHERE lms.release_grant.is_released IS DISTINCT FROM EXCLUDED.is_released
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public CohortReleaseRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The cohort's enabled rules in evaluation order, each with its conditions and targets. */
    public List<ReleaseRuleRow> rules(UUID cohortId) {
        record Head(UUID id, UUID source, boolean stop, String kind, UUID lesson, UUID quiz) {
        }
        List<Head> heads = jdbc.sql(RULES).param("cohortId", cohortId)
                .query((rs, n) -> new Head(rs.getObject("id", UUID.class),
                        rs.getObject("source_content_item_id", UUID.class), rs.getBoolean("stop_if_true"),
                        rs.getString("item_kind"), rs.getObject("lesson_id", UUID.class),
                        rs.getObject("quiz_id", UUID.class)))
                .list();
        if (heads.isEmpty()) {
            return List.of();
        }
        UUID[] ids = heads.stream().map(Head::id).toArray(UUID[]::new);
        record Of<T>(UUID rule, T value) {
        }
        Map<UUID, List<ReleaseRuleRow.Condition>> conditions = new LinkedHashMap<>();
        jdbc.sql(CONDITIONS).param("rules", ids)
                .query((rs, n) -> new Of<>(rs.getObject("release_rule_id", UUID.class),
                        new ReleaseRuleRow.Condition(rs.getString("condition_type"), rs.getString("value"))))
                .list().forEach(row -> conditions.computeIfAbsent(row.rule(), id -> new ArrayList<>()).add(row.value()));
        Map<UUID, List<ReleaseRuleRow.Target>> targets = new LinkedHashMap<>();
        jdbc.sql(TARGETS).param("cohortId", cohortId).param("rules", ids)
                .query((rs, n) -> new Of<>(rs.getObject("release_rule_id", UUID.class),
                        new ReleaseRuleRow.Target(rs.getObject("content_item_id", UUID.class),
                                rs.getObject("chapter_id", UUID.class))))
                .list().forEach(row -> targets.computeIfAbsent(row.rule(), id -> new ArrayList<>()).add(row.value()));
        return heads.stream().map(head -> new ReleaseRuleRow(head.id(), head.source(), head.kind(), head.lesson(),
                head.quiz(), head.stop(), conditions.getOrDefault(head.id(), List.of()),
                targets.getOrDefault(head.id(), List.of()))).toList();
    }

    /** "user|lesson" keys of the completed lessons among these. */
    public Set<String> completedLessons(List<UUID> users, List<UUID> lessons) {
        if (users.isEmpty() || lessons.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(jdbc.sql(COMPLETED_LESSONS).param("users", users.toArray(UUID[]::new))
                .param("lessons", lessons.toArray(UUID[]::new))
                .query((rs, n) -> rs.getObject("user_id", UUID.class) + "|" + rs.getObject("lesson_id", UUID.class))
                .list());
    }

    /** "user|item" keys of the items these users opened since the given time. */
    public Set<String> viewed(List<UUID> users, List<UUID> items, OffsetDateTime since) {
        if (users.isEmpty() || items.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(jdbc.sql(VIEWED).param("users", users.toArray(UUID[]::new))
                .param("items", items.toArray(UUID[]::new)).param("since", since)
                .query((rs, n) -> rs.getObject("user_id", UUID.class) + "|"
                        + rs.getObject("content_item_id", UUID.class))
                .list());
    }

    /** Attempts per "user|quiz". */
    public Map<String, Integer> attempts(List<UUID> users, List<UUID> quizzes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (users.isEmpty() || quizzes.isEmpty()) {
            return counts;
        }
        jdbc.sql(ATTEMPTS).param("users", users.toArray(UUID[]::new)).param("quizzes", quizzes.toArray(UUID[]::new))
                .query((rs, n) -> Map.entry(rs.getObject("user_id", UUID.class) + "|"
                        + rs.getObject("quiz_id", UUID.class), rs.getInt("attempts")))
                .list().forEach(row -> counts.put(row.getKey(), row.getValue()));
        return counts;
    }

    /**
     * Records whether the item is released to the user; answers the grant's id when the row was created or its
     * release state changed (nothing when it already said so).
     */
    public List<UUID> grant(UUID id, UUID itemId, UUID chapterId, UUID userId, boolean released, OffsetDateTime now,
            UUID actor) {
        return jdbc.sql(UPSERT_GRANT).param("id", id).param("itemId", itemId).param("chapterId", chapterId)
                .param("userId", userId).param("released", released).param("now", now).param("actor", actor)
                .query(UUID.class).list();
    }
}
