package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC statements that copy a course's outline into a cohort: chapters, their content
 * items and learning objectives, the course's grading categories and the release rules of the copied items with
 * their conditions and targets. Each copy is one set-based INSERT … SELECT joined to the old → new id pairs the
 * service generated (UUIDv7, in Java), so the copy is exact and references are re-pointed in the same statement.
 *
 * <p>Template day offsets become dates: day N of a chapter or item lands on the base date plus N − 1 days at its
 * time (start of day when it has none; the end of the day for due and closing times), in the cohort's time zone.
 * Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CohortOutlineRepository {

    private static final String COURSE_OF_COHORT = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_course WHERE tenant_id = :tenantId AND cohort_id = :cohortId
                              AND course_id = :courseId AND deleted_at IS NULL)
            """;
    private static final String TEMPLATE_CHAPTERS = """
            SELECT c.id FROM lms.chapter c
             WHERE c.tenant_id = :tenantId AND c.course_id = :courseId AND c.cohort_id IS NULL
               AND c.deleted_at IS NULL AND (:all OR c.id = ANY(:chosen))
               AND NOT EXISTS (SELECT 1 FROM lms.chapter k WHERE k.tenant_id = :tenantId AND k.cohort_id = :cohortId
                                  AND k.copied_from_id = c.id AND k.deleted_at IS NULL)
             ORDER BY c.parent_chapter_id NULLS FIRST, c.sort_order, c.id
            """;
    private static final String COPY_CHAPTERS = """
            INSERT INTO lms.chapter (id, tenant_id, course_id, cohort_id, parent_chapter_id, title, sort_order,
                   is_scorm_package, scorm_package_file_id, scorm_package_path, scorm_manifest_file, scorm_launch_file,
                   description, chapter_number, start_day, start_time, due_day, due_time, starts_at, ends_at,
                   allow_delete, allow_edit_details, allow_edit_dates, copied_from_id, is_original, created_at,
                   created_by, updated_at, updated_by, row_version)
            SELECT m.new_id, c.tenant_id, c.course_id, :cohortId, p.new_id, c.title, c.sort_order, c.is_scorm_package,
                   c.scorm_package_file_id, c.scorm_package_path, c.scorm_manifest_file, c.scorm_launch_file,
                   c.description, c.chapter_number, c.start_day, c.start_time, c.due_day, c.due_time,
                   CASE WHEN c.start_day IS NULL THEN c.starts_at
                        ELSE (CAST(:base AS date) + (c.start_day - 1)
                              + COALESCE(c.start_time, TIME '00:00')) AT TIME ZONE :zone END,
                   CASE WHEN c.due_day IS NULL THEN c.ends_at
                        ELSE (CAST(:base AS date) + (c.due_day - 1)
                              + COALESCE(c.due_time, TIME '23:59:59')) AT TIME ZONE :zone END,
                   c.allow_delete, c.allow_edit_details, c.allow_edit_dates, c.id, false, :now, :actor, :now, :actor, 1
              FROM lms.chapter c JOIN unnest(:olds, :news) AS m(old_id, new_id) ON m.old_id = c.id
              LEFT JOIN unnest(:olds, :news) AS p(old_id, new_id) ON p.old_id = c.parent_chapter_id
             WHERE c.tenant_id = :tenantId
            """;
    private static final String TEMPLATE_ITEMS = """
            SELECT ci.id FROM lms.content_item ci
             WHERE ci.tenant_id = :tenantId AND ci.chapter_id = ANY(:chapters) AND ci.cohort_id IS NULL
               AND ci.deleted_at IS NULL
             ORDER BY ci.sort_order, ci.id
            """;
    private static final String COPY_GRADING_CATEGORIES = """
            INSERT INTO lms.grading_category (id, tenant_id, cohort_id, name, weight_percent, equal_item_weighting,
                   created_at, created_by, updated_at, updated_by, row_version)
            SELECT m.new_id, g.tenant_id, :cohortId, g.name, g.weight_percent, g.equal_item_weighting, :now, :actor,
                   :now, :actor, 1
              FROM lms.grading_category g JOIN unnest(:olds, :news) AS m(old_id, new_id) ON m.old_id = g.id
             WHERE g.tenant_id = :tenantId
            """;
    private static final String TEMPLATE_GRADING_CATEGORIES = """
            SELECT g.id FROM lms.grading_category g
             WHERE g.tenant_id = :tenantId AND g.course_id = :courseId AND g.deleted_at IS NULL
               AND NOT EXISTS (SELECT 1 FROM lms.grading_category k WHERE k.tenant_id = :tenantId
                                  AND k.cohort_id = :cohortId AND k.name = g.name AND k.deleted_at IS NULL)
             ORDER BY g.created_at, g.id
            """;
    private static final String COPY_ITEMS = """
            INSERT INTO lms.content_item (id, tenant_id, exercise_id, chapter_id, course_id, cohort_id, item_kind,
                   lesson_id, file_id, assignment_id, quiz_id, discussion_prompt_id, participation_activity_id,
                   web_resource_id, ebook_id, external_tool_resource_id, live_class_id, link_url, title, sort_order,
                   is_active, is_always_visible, visible_from, visible_until, visible_until_cohort_end, attempt_from,
                   due_at, attempt_until_cohort_end, visible_start_day, visible_start_time, visible_end_day,
                   visible_end_time, attempt_start_day, attempt_start_time, attempt_end_day, attempt_end_time,
                   late_submission_allowed, late_submission_days, is_graded, points, weight_percent,
                   grading_category_id, rubric_id, plagiarism_check_required, show_in_calendar, allow_delete,
                   allow_edit_details, allow_edit_dates, allow_edit_grading, copied_from_id, is_original, created_at,
                   created_by, updated_at, updated_by, row_version)
            SELECT m.new_id, ci.tenant_id, ci.exercise_id, ch.new_id, ci.course_id, :cohortId, ci.item_kind,
                   ci.lesson_id, ci.file_id, ci.assignment_id, ci.quiz_id, ci.discussion_prompt_id,
                   ci.participation_activity_id, ci.web_resource_id, ci.ebook_id, ci.external_tool_resource_id,
                   ci.live_class_id, ci.link_url, ci.title, ci.sort_order, ci.is_active, ci.is_always_visible,
                   CASE WHEN ci.visible_start_day IS NULL THEN ci.visible_from
                        ELSE (CAST(:base AS date) + (ci.visible_start_day - 1)
                              + COALESCE(ci.visible_start_time, TIME '00:00')) AT TIME ZONE :zone END,
                   CASE WHEN ci.visible_end_day IS NULL THEN ci.visible_until
                        ELSE (CAST(:base AS date) + (ci.visible_end_day - 1)
                              + COALESCE(ci.visible_end_time, TIME '23:59:59')) AT TIME ZONE :zone END,
                   ci.visible_until_cohort_end,
                   CASE WHEN ci.attempt_start_day IS NULL THEN ci.attempt_from
                        ELSE (CAST(:base AS date) + (ci.attempt_start_day - 1)
                              + COALESCE(ci.attempt_start_time, TIME '00:00')) AT TIME ZONE :zone END,
                   CASE WHEN ci.attempt_end_day IS NULL THEN ci.due_at
                        ELSE (CAST(:base AS date) + (ci.attempt_end_day - 1)
                              + COALESCE(ci.attempt_end_time, TIME '23:59:59')) AT TIME ZONE :zone END,
                   ci.attempt_until_cohort_end, ci.visible_start_day, ci.visible_start_time, ci.visible_end_day,
                   ci.visible_end_time, ci.attempt_start_day, ci.attempt_start_time, ci.attempt_end_day,
                   ci.attempt_end_time, ci.late_submission_allowed, ci.late_submission_days, ci.is_graded, ci.points,
                   ci.weight_percent,
                   (SELECT k.id FROM lms.grading_category g JOIN lms.grading_category k ON k.tenant_id = :tenantId
                       AND k.cohort_id = :cohortId AND k.name = g.name AND k.deleted_at IS NULL
                     WHERE g.tenant_id = :tenantId AND g.id = ci.grading_category_id
                     ORDER BY k.created_at, k.id LIMIT 1),
                   ci.rubric_id, ci.plagiarism_check_required, ci.show_in_calendar, ci.allow_delete,
                   ci.allow_edit_details, ci.allow_edit_dates, ci.allow_edit_grading, ci.id, false, :now, :actor, :now,
                   :actor, 1
              FROM lms.content_item ci
              JOIN unnest(:olds, :news) AS m(old_id, new_id) ON m.old_id = ci.id
              JOIN unnest(:oldChapters, :newChapters) AS ch(old_id, new_id) ON ch.old_id = ci.chapter_id
             WHERE ci.tenant_id = :tenantId
            """;
    private static final String COPY_OBJECTIVES = """
            INSERT INTO lms.learning_objective (id, tenant_id, chapter_id, objective, sort_order, copied_from_id,
                   created_at, created_by, updated_at, updated_by, row_version)
            SELECT m.new_id, o.tenant_id, ch.new_id, o.objective, o.sort_order, o.id, :now, :actor, :now, :actor, 1
              FROM lms.learning_objective o
              JOIN unnest(:olds, :news) AS m(old_id, new_id) ON m.old_id = o.id
              JOIN unnest(:oldChapters, :newChapters) AS ch(old_id, new_id) ON ch.old_id = o.chapter_id
             WHERE o.tenant_id = :tenantId
            """;
    private static final String TEMPLATE_OBJECTIVES = """
            SELECT o.id FROM lms.learning_objective o
             WHERE o.tenant_id = :tenantId AND o.chapter_id = ANY(:chapters) AND o.deleted_at IS NULL
             ORDER BY o.sort_order, o.id
            """;
    private static final String TEMPLATE_RULES = """
            SELECT r.id FROM lms.release_rule r
             WHERE r.tenant_id = :tenantId AND r.course_id = :courseId AND r.cohort_id IS NULL
               AND r.source_content_item_id = ANY(:items) AND r.deleted_at IS NULL
             ORDER BY r.sort_order, r.id
            """;
    private static final String COPY_RULES = """
            INSERT INTO lms.release_rule (id, tenant_id, cohort_id, source_content_item_id, title, stop_if_true,
                   is_disabled, reassign_source, sort_order, copied_from_id, created_at, created_by, updated_at,
                   updated_by, row_version)
            SELECT m.new_id, r.tenant_id, :cohortId, s.new_id, r.title, r.stop_if_true, r.is_disabled,
                   r.reassign_source, r.sort_order, r.id, :now, :actor, :now, :actor, 1
              FROM lms.release_rule r
              JOIN unnest(:olds, :news) AS m(old_id, new_id) ON m.old_id = r.id
              JOIN unnest(:oldItems, :newItems) AS s(old_id, new_id) ON s.old_id = r.source_content_item_id
             WHERE r.tenant_id = :tenantId
            """;
    private static final String TEMPLATE_CONDITIONS = """
            SELECT c.id FROM lms.release_rule_condition c
             WHERE c.tenant_id = :tenantId AND c.release_rule_id = ANY(:rules) AND c.deleted_at IS NULL
             ORDER BY c.sort_order, c.id
            """;
    private static final String COPY_CONDITIONS = """
            INSERT INTO lms.release_rule_condition (id, tenant_id, release_rule_id, operator, value, value2, sort_order,
                   condition_type, created_at, created_by, updated_at, updated_by, row_version)
            SELECT m.new_id, c.tenant_id, r.new_id, c.operator, c.value, c.value2, c.sort_order, c.condition_type,
                   :now, :actor, :now, :actor, 1
              FROM lms.release_rule_condition c
              JOIN unnest(:olds, :news) AS m(old_id, new_id) ON m.old_id = c.id
              JOIN unnest(:oldRules, :newRules) AS r(old_id, new_id) ON r.old_id = c.release_rule_id
             WHERE c.tenant_id = :tenantId
            """;
    private static final String TEMPLATE_TARGETS = """
            SELECT t.id FROM lms.release_rule_target t
             WHERE t.tenant_id = :tenantId AND t.release_rule_id = ANY(:rules) AND t.deleted_at IS NULL
               AND (t.content_item_id = ANY(:items) OR t.chapter_id = ANY(:chapters))
             ORDER BY t.id
            """;
    private static final String COPY_TARGETS = """
            INSERT INTO lms.release_rule_target (id, tenant_id, release_rule_id, content_item_id, chapter_id,
                   created_at, created_by, updated_at, updated_by, row_version)
            SELECT m.new_id, t.tenant_id, r.new_id, i.new_id, ch.new_id, :now, :actor, :now, :actor, 1
              FROM lms.release_rule_target t
              JOIN unnest(:olds, :news) AS m(old_id, new_id) ON m.old_id = t.id
              JOIN unnest(:oldRules, :newRules) AS r(old_id, new_id) ON r.old_id = t.release_rule_id
              LEFT JOIN unnest(:oldItems, :newItems) AS i(old_id, new_id) ON i.old_id = t.content_item_id
              LEFT JOIN unnest(:oldChapters, :newChapters) AS ch(old_id, new_id) ON ch.old_id = t.chapter_id
             WHERE t.tenant_id = :tenantId
            """;

    private static final String SYLLABUS = """
            SELECT ch.title AS chapter, ci.title, ci.item_kind, ci.visible_from, ci.due_at, ci.points, ci.is_graded
              FROM lms.content_item ci
              JOIN lms.chapter ch ON ch.tenant_id = :tenantId AND ch.id = ci.chapter_id
             WHERE ci.tenant_id = :tenantId AND ci.cohort_id = :cohortId AND ci.is_active
               AND ci.deleted_at IS NULL AND ch.deleted_at IS NULL
             ORDER BY ch.sort_order, ch.title, ci.sort_order, ci.title
            """;

    private final JdbcClient jdbc;

    public CohortOutlineRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** One syllabus line per active content item of the cohort, in outline order. */
    public record SyllabusRow(String chapter, String title, String itemKind, OffsetDateTime visibleFrom,
            OffsetDateTime dueAt, BigDecimal points, boolean isGraded) {
    }

    public List<SyllabusRow> syllabus(UUID cohortId) {
        return jdbc.sql(SYLLABUS).param("cohortId", cohortId)
                .query((rs, row) -> new SyllabusRow(rs.getString("chapter"), rs.getString("title"),
                        rs.getString("item_kind"), rs.getObject("visible_from", OffsetDateTime.class),
                        rs.getObject("due_at", OffsetDateTime.class), rs.getBigDecimal("points"),
                        rs.getBoolean("is_graded")))
                .list();
    }

    /** Old → new ids of one copied table, the arrays every copy statement joins on. */
    public record Pairs(UUID[] olds, UUID[] news) {

        public boolean isEmpty() {
            return olds.length == 0;
        }
    }

    /** What every copy statement shares: the cohort, the base date and zone of the day offsets, when and who. */
    public record Copy(UUID cohortId, LocalDate base, String zone, OffsetDateTime now, UUID actor) {
    }

    /** Whether the course is one the cohort teaches. */
    public boolean isCourseOfCohort(UUID cohortId, UUID courseId) {
        return Boolean.TRUE.equals(jdbc.sql(COURSE_OF_COHORT).param("cohortId", cohortId).param("courseId", courseId)
                .query(Boolean.class).single());
    }

    /** The course's template chapters to copy (all, or the chosen), less those the cohort already has a copy of. */
    public List<UUID> templateChapters(UUID courseId, UUID cohortId, List<UUID> chosen) {
        return jdbc.sql(TEMPLATE_CHAPTERS).param("courseId", courseId).param("cohortId", cohortId)
                .param("all", chosen == null || chosen.isEmpty())
                .param("chosen", chosen == null ? new UUID[0] : chosen.toArray(UUID[]::new))
                .query(UUID.class).list();
    }

    public List<UUID> templateItems(UUID[] chapters) {
        return jdbc.sql(TEMPLATE_ITEMS).param("chapters", chapters).query(UUID.class).list();
    }

    public List<UUID> templateGradingCategories(UUID courseId, UUID cohortId) {
        return jdbc.sql(TEMPLATE_GRADING_CATEGORIES).param("courseId", courseId).param("cohortId", cohortId)
                .query(UUID.class).list();
    }

    public List<UUID> templateObjectives(UUID[] chapters) {
        return jdbc.sql(TEMPLATE_OBJECTIVES).param("chapters", chapters).query(UUID.class).list();
    }

    public List<UUID> templateRules(UUID courseId, UUID[] items) {
        return jdbc.sql(TEMPLATE_RULES).param("courseId", courseId).param("items", items).query(UUID.class).list();
    }

    public List<UUID> templateConditions(UUID[] rules) {
        return jdbc.sql(TEMPLATE_CONDITIONS).param("rules", rules).query(UUID.class).list();
    }

    public List<UUID> templateTargets(UUID[] rules, UUID[] items, UUID[] chapters) {
        return jdbc.sql(TEMPLATE_TARGETS).param("rules", rules).param("items", items).param("chapters", chapters)
                .query(UUID.class).list();
    }

    /**
     * Copies the chapters (dated from their day offsets); a copied child points at its parent's copy (inserted by the
     * same statement, whose foreign keys are checked at its end), a child copied without its parent becomes top-level.
     */
    public void copyChapters(Copy copy, Pairs chapters) {
        dated(COPY_CHAPTERS, copy, chapters).update();
    }

    public void copyGradingCategories(Copy copy, Pairs categories) {
        written(COPY_GRADING_CATEGORIES, copy, categories).update();
    }

    /** Copies the items into their copied chapters (dated), each graded item in the cohort's category of its name. */
    public void copyItems(Copy copy, Pairs items, Pairs chapters) {
        dated(COPY_ITEMS, copy, items).param("oldChapters", chapters.olds()).param("newChapters", chapters.news())
                .update();
    }

    public void copyObjectives(Copy copy, Pairs objectives, Pairs chapters) {
        written(COPY_OBJECTIVES, copy, objectives).param("oldChapters", chapters.olds())
                .param("newChapters", chapters.news()).update();
    }

    public void copyRules(Copy copy, Pairs rules, Pairs items) {
        written(COPY_RULES, copy, rules).param("oldItems", items.olds()).param("newItems", items.news()).update();
    }

    public void copyConditions(Copy copy, Pairs conditions, Pairs rules) {
        written(COPY_CONDITIONS, copy, conditions).param("oldRules", rules.olds()).param("newRules", rules.news())
                .update();
    }

    public void copyTargets(Copy copy, Pairs targets, Pairs rules, Pairs items, Pairs chapters) {
        written(COPY_TARGETS, copy, targets).param("oldRules", rules.olds()).param("newRules", rules.news())
                .param("oldItems", items.olds()).param("newItems", items.news())
                .param("oldChapters", chapters.olds()).param("newChapters", chapters.news()).update();
    }

    private JdbcClient.StatementSpec written(String sql, Copy copy, Pairs pairs) {
        return jdbc.sql(sql).param("cohortId", copy.cohortId()).param("olds", pairs.olds()).param("news", pairs.news())
                .param("now", copy.now()).param("actor", copy.actor());
    }

    private JdbcClient.StatementSpec dated(String sql, Copy copy, Pairs pairs) {
        return written(sql, copy, pairs).param("base", copy.base()).param("zone", copy.zone());
    }
}
