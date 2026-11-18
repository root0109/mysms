package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.ContentItemEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Plain-JDBC access to lms.content_item: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ContentItemRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("exerciseId", "r.exercise_id", ValueType.UUID),
            field("chapterId", "r.chapter_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("itemKind", "r.item_kind", ValueType.STRING),
            field("lessonId", "r.lesson_id", ValueType.UUID),
            field("fileId", "r.file_id", ValueType.UUID),
            field("assignmentId", "r.assignment_id", ValueType.UUID),
            field("quizId", "r.quiz_id", ValueType.UUID),
            field("discussionPromptId", "r.discussion_prompt_id", ValueType.UUID),
            field("participationActivityId", "r.participation_activity_id", ValueType.UUID),
            field("webResourceId", "r.web_resource_id", ValueType.UUID),
            field("ebookId", "r.ebook_id", ValueType.UUID),
            field("externalToolResourceId", "r.external_tool_resource_id", ValueType.UUID),
            field("liveClassId", "r.live_class_id", ValueType.UUID),
            field("linkUrl", "r.link_url", ValueType.STRING),
            field("title", "r.title", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("isAlwaysVisible", "r.is_always_visible", ValueType.BOOLEAN),
            field("visibleFrom", "r.visible_from", ValueType.TIMESTAMP),
            field("visibleUntil", "r.visible_until", ValueType.TIMESTAMP),
            field("visibleUntilCohortEnd", "r.visible_until_cohort_end", ValueType.BOOLEAN),
            field("attemptFrom", "r.attempt_from", ValueType.TIMESTAMP),
            field("dueAt", "r.due_at", ValueType.TIMESTAMP),
            field("attemptUntilCohortEnd", "r.attempt_until_cohort_end", ValueType.BOOLEAN),
            field("visibleStartDay", "r.visible_start_day", ValueType.INTEGER),
            field("visibleEndDay", "r.visible_end_day", ValueType.INTEGER),
            field("attemptStartDay", "r.attempt_start_day", ValueType.INTEGER),
            field("attemptEndDay", "r.attempt_end_day", ValueType.INTEGER),
            field("lateSubmissionAllowed", "r.late_submission_allowed", ValueType.BOOLEAN),
            field("lateSubmissionDays", "r.late_submission_days", ValueType.INTEGER),
            field("isGraded", "r.is_graded", ValueType.BOOLEAN),
            field("points", "r.points", ValueType.DECIMAL),
            field("weightPercent", "r.weight_percent", ValueType.DECIMAL),
            field("gradingCategoryId", "r.grading_category_id", ValueType.UUID),
            field("rubricId", "r.rubric_id", ValueType.UUID),
            field("plagiarismCheckRequired", "r.plagiarism_check_required", ValueType.BOOLEAN),
            field("showInCalendar", "r.show_in_calendar", ValueType.BOOLEAN),
            field("allowDelete", "r.allow_delete", ValueType.BOOLEAN),
            field("allowEditDetails", "r.allow_edit_details", ValueType.BOOLEAN),
            field("allowEditDates", "r.allow_edit_dates", ValueType.BOOLEAN),
            field("allowEditGrading", "r.allow_edit_grading", ValueType.BOOLEAN),
            field("copiedFromId", "r.copied_from_id", ValueType.UUID),
            field("isOriginal", "r.is_original", ValueType.BOOLEAN),
            field("viewCount", "r.view_count", ValueType.INTEGER),
            field("likeCount", "r.like_count", ValueType.INTEGER),
            field("ratingTotal", "r.rating_total", ValueType.DECIMAL),
            field("ratingCount", "r.rating_count", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ContentItemEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ContentItemEntity::getCreatedAt),
            Map.entry("updateTime", ContentItemEntity::getUpdatedAt),
            Map.entry("sortOrder", ContentItemEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.exercise_id, r.chapter_id, r.course_id, r.cohort_id, r.item_kind, r.lesson_id,
                   r.file_id, r.assignment_id, r.quiz_id, r.discussion_prompt_id, r.participation_activity_id,
                   r.web_resource_id, r.ebook_id, r.external_tool_resource_id, r.live_class_id, r.link_url, r.title,
                   r.sort_order, r.is_active, r.is_always_visible, r.visible_from, r.visible_until,
                   r.visible_until_cohort_end, r.attempt_from, r.due_at, r.attempt_until_cohort_end,
                   r.visible_start_day, r.visible_start_time, r.visible_end_day, r.visible_end_time,
                   r.attempt_start_day, r.attempt_start_time, r.attempt_end_day, r.attempt_end_time,
                   r.late_submission_allowed, r.late_submission_days, r.is_graded, r.points, r.weight_percent,
                   r.grading_category_id, r.rubric_id, r.plagiarism_check_required, r.show_in_calendar,
                   r.allow_delete, r.allow_edit_details, r.allow_edit_dates, r.allow_edit_grading, r.copied_from_id,
                   r.is_original, r.view_count, r.like_count, r.rating_total, r.rating_count, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.content_item r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** lms.SetContentItemVisibility: shows or hides a cohort item under its version. */
    private static final String SET_ACTIVE = """
            UPDATE lms.content_item
               SET is_active = :active, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** Makes room at a place in a chapter: the items from there on move one down (the moved item excluded). */
    private static final String SHIFT_DOWN = """
            UPDATE lms.content_item
               SET sort_order = sort_order + 1, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND chapter_id = :chapterId AND deleted_at IS NULL AND sort_order >= :start
               AND id <> :id
            """;
    /** lms.MoveContentItem: the chapter and the place in it, under the item's version. */
    private static final String MOVE = """
            UPDATE lms.content_item
               SET chapter_id = :chapterId, sort_order = :sortOrder, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** lms.RecordItemView: one more view; the cached likes are recounted from the students' ratings. */
    private static final String COUNT_VIEW = """
            UPDATE lms.content_item
               SET view_count = view_count + 1,
                   like_count = (SELECT count(*) FROM lms.content_item_rating x WHERE x.tenant_id = :tenantId
                                    AND x.content_item_id = :id AND x.deleted_at IS NULL AND x.is_like)
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    /** A student likes an item: one rating row per item and student (uq_content_item_rating_1). */
    private static final String LIKE = """
            INSERT INTO lms.content_item_rating
            (id, tenant_id, content_item_id, cohort_id, user_id, is_like, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:ratingId, :tenantId, :id, :cohortId, :userId, true, :now, :userId, :now, :userId, 1)
            ON CONFLICT (content_item_id, user_id) WHERE deleted_at IS NULL
            DO UPDATE SET is_like = true, updated_at = :now, updated_by = :userId,
                          row_version = lms.content_item_rating.row_version + 1
             WHERE lms.content_item_rating.tenant_id = :tenantId AND NOT COALESCE(lms.content_item_rating.is_like, false)
            """;
    /** Whether the user may follow the item as a student: enrolled in its course, or in its cohort. */
    private static final String LEARNER_OF = """
            SELECT EXISTS (SELECT 1 FROM lms.enrollment e WHERE e.tenant_id = :tenantId AND e.course_id = :courseId
                              AND e.user_id = :userId AND e.deleted_at IS NULL)
                OR EXISTS (SELECT 1 FROM lms.cohort_enrollment c WHERE c.tenant_id = :tenantId
                              AND c.cohort_id = :cohortId AND c.user_id = :userId AND c.deleted_at IS NULL
                              AND c.is_active AND c.removed_at IS NULL)
            """;
    private static final String INSERT = """
            INSERT INTO lms.content_item
            (id, tenant_id, exercise_id, chapter_id, course_id, cohort_id, item_kind, lesson_id, file_id,
             assignment_id, quiz_id, discussion_prompt_id, participation_activity_id, web_resource_id, ebook_id,
             external_tool_resource_id, live_class_id, link_url, title, sort_order, is_active, is_always_visible,
             visible_from, visible_until, visible_until_cohort_end, attempt_from, due_at, attempt_until_cohort_end,
             visible_start_day, visible_start_time, visible_end_day, visible_end_time, attempt_start_day,
             attempt_start_time, attempt_end_day, attempt_end_time, late_submission_allowed, late_submission_days,
             is_graded, points, weight_percent, grading_category_id, rubric_id, plagiarism_check_required,
             show_in_calendar, allow_delete, allow_edit_details, allow_edit_dates, allow_edit_grading,
             copied_from_id, is_original, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :exerciseId, :chapterId, :courseId, :cohortId, :itemKind, :lessonId, :fileId,
                    :assignmentId, :quizId, :discussionPromptId, :participationActivityId, :webResourceId, :ebookId,
                    :externalToolResourceId, :liveClassId, :linkUrl, :title, COALESCE(:sortOrder, 0),
                    COALESCE(:isActive, true), COALESCE(:isAlwaysVisible, true), :visibleFrom, :visibleUntil,
                    COALESCE(:visibleUntilCohortEnd, false), :attemptFrom, :dueAt,
                    COALESCE(:attemptUntilCohortEnd, false), :visibleStartDay, :visibleStartTime, :visibleEndDay,
                    :visibleEndTime, :attemptStartDay, :attemptStartTime, :attemptEndDay, :attemptEndTime,
                    COALESCE(:lateSubmissionAllowed, false), :lateSubmissionDays, COALESCE(:isGraded, false),
                    :points, :weightPercent, :gradingCategoryId, :rubricId,
                    COALESCE(:plagiarismCheckRequired, false), COALESCE(:showInCalendar, true),
                    COALESCE(:allowDelete, true), COALESCE(:allowEditDetails, true), COALESCE(:allowEditDates, true),
                    COALESCE(:allowEditGrading, true), :copiedFromId, COALESCE(:isOriginal, true), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, exercise_id, chapter_id, course_id, cohort_id, item_kind, lesson_id, file_id,
                      assignment_id, quiz_id, discussion_prompt_id, participation_activity_id, web_resource_id,
                      ebook_id, external_tool_resource_id, live_class_id, link_url, title, sort_order, is_active,
                      is_always_visible, visible_from, visible_until, visible_until_cohort_end, attempt_from, due_at,
                      attempt_until_cohort_end, visible_start_day, visible_start_time, visible_end_day,
                      visible_end_time, attempt_start_day, attempt_start_time, attempt_end_day, attempt_end_time,
                      late_submission_allowed, late_submission_days, is_graded, points, weight_percent,
                      grading_category_id, rubric_id, plagiarism_check_required, show_in_calendar, allow_delete,
                      allow_edit_details, allow_edit_dates, allow_edit_grading, copied_from_id, is_original, view_count,
                      like_count, rating_total, rating_count, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.content_item
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.content_item
               SET exercise_id = :exerciseId,
                   chapter_id = :chapterId,
                   course_id = :courseId,
                   cohort_id = :cohortId,
                   item_kind = :itemKind,
                   lesson_id = :lessonId,
                   file_id = :fileId,
                   assignment_id = :assignmentId,
                   quiz_id = :quizId,
                   discussion_prompt_id = :discussionPromptId,
                   participation_activity_id = :participationActivityId,
                   web_resource_id = :webResourceId,
                   ebook_id = :ebookId,
                   external_tool_resource_id = :externalToolResourceId,
                   live_class_id = :liveClassId,
                   link_url = :linkUrl,
                   title = :title,
                   sort_order = :sortOrder,
                   is_active = :isActive,
                   is_always_visible = :isAlwaysVisible,
                   visible_from = :visibleFrom,
                   visible_until = :visibleUntil,
                   visible_until_cohort_end = :visibleUntilCohortEnd,
                   attempt_from = :attemptFrom,
                   due_at = :dueAt,
                   attempt_until_cohort_end = :attemptUntilCohortEnd,
                   visible_start_day = :visibleStartDay,
                   visible_start_time = :visibleStartTime,
                   visible_end_day = :visibleEndDay,
                   visible_end_time = :visibleEndTime,
                   attempt_start_day = :attemptStartDay,
                   attempt_start_time = :attemptStartTime,
                   attempt_end_day = :attemptEndDay,
                   attempt_end_time = :attemptEndTime,
                   late_submission_allowed = :lateSubmissionAllowed,
                   late_submission_days = :lateSubmissionDays,
                   is_graded = :isGraded,
                   points = :points,
                   weight_percent = :weightPercent,
                   grading_category_id = :gradingCategoryId,
                   rubric_id = :rubricId,
                   plagiarism_check_required = :plagiarismCheckRequired,
                   show_in_calendar = :showInCalendar,
                   allow_delete = :allowDelete,
                   allow_edit_details = :allowEditDetails,
                   allow_edit_dates = :allowEditDates,
                   allow_edit_grading = :allowEditGrading,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_ASSIGNMENT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COPIED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_DISCUSSION_PROMPT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.discussion_prompt WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_EBOOK_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.ebook WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_EXERCISE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.programming_exercise WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_EXTERNAL_TOOL_RESOURCE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.external_tool_resource WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_GRADING_CATEGORY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.grading_category WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_LESSON_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_LIVE_CLASS_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.live_class WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PARTICIPATION_ACTIVITY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.participation_activity WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_QUIZ_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_RUBRIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_WEB_RESOURCE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.web_resource WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ANNOTATION_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.annotation WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_GROUP_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_GROUP_MEMBER_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group_member WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ASSIGNMENT_SUBMISSION_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_submission WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_COPIED_FROM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND copied_from_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_RATING_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item_rating WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_VIEW_SESSION_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_view_session WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_TOPIC_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_topic WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_GRADEBOOK_ENTRY_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.gradebook_entry WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ITEM_OVERRIDE_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.item_override WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUIZ_SUBMISSION_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.quiz_submission WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_REASSIGNMENT_LOG_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.reassignment_log WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_GRANT_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_grant WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_RULE_SOURCE_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_rule WHERE tenant_id = :tenantId
                AND source_content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELEASE_RULE_TARGET_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.release_rule_target WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RUBRIC_ASSESSMENT_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.rubric_assessment WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STUDENT_ALERT_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.student_alert WHERE tenant_id = :tenantId
                AND content_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.course p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.course_id IN (SELECT p0.id FROM lms.course p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.course_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public ContentItemRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Shows (true) or hides the item; 0 rows means it changed since it was read. */
    public int setActive(UUID id, boolean active, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_ACTIVE).param("id", id).param("active", active).param("expected", expected)
                .param("now", now).param("actor", actor).update();
    }

    /** Moves the item to a place in a chapter, making room there; 0 rows means it changed since it was read. */
    public int move(UUID id, int expected, UUID chapterId, int sortOrder, OffsetDateTime now, UUID actor) {
        jdbc.sql(SHIFT_DOWN).param("chapterId", chapterId).param("start", sortOrder).param("id", id).update();
        return jdbc.sql(MOVE).param("id", id).param("expected", expected).param("chapterId", chapterId)
                .param("sortOrder", sortOrder).param("now", now).param("actor", actor).update();
    }

    /** Counts a view of the item and, when the student liked it, their like. */
    public void countView(UUID id, UUID cohortId, UUID userId, boolean liked, UUID ratingId, OffsetDateTime now) {
        if (liked) {
            jdbc.sql(LIKE).param("ratingId", ratingId).param("id", id).param("cohortId", cohortId)
                    .param("userId", userId).param("now", now).update();
        }
        jdbc.sql(COUNT_VIEW).param("id", id).update();
    }

    /** Whether the user follows the item's course or cohort as a student. */
    public boolean isLearnerOf(ContentItemEntity item, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(LEARNER_OF).param("courseId", item.getCourseId())
                .param("cohortId", item.getCohortId()).param("userId", userId).query(Boolean.class).single());
    }

    public Optional<ContentItemEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ContentItemRepository::map).optional();
    }

    public Page<ContentItemEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ContentItemEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ContentItemRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ContentItemEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ContentItemEntity insert(ContentItemEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("exerciseId", e.getExerciseId())
                .param("chapterId", e.getChapterId())
                .param("courseId", e.getCourseId())
                .param("cohortId", e.getCohortId())
                .param("itemKind", e.getItemKind())
                .param("lessonId", e.getLessonId())
                .param("fileId", e.getFileId())
                .param("assignmentId", e.getAssignmentId())
                .param("quizId", e.getQuizId())
                .param("discussionPromptId", e.getDiscussionPromptId())
                .param("participationActivityId", e.getParticipationActivityId())
                .param("webResourceId", e.getWebResourceId())
                .param("ebookId", e.getEbookId())
                .param("externalToolResourceId", e.getExternalToolResourceId())
                .param("liveClassId", e.getLiveClassId())
                .param("linkUrl", e.getLinkUrl())
                .param("title", e.getTitle())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("isAlwaysVisible", e.getIsAlwaysVisible())
                .param("visibleFrom", e.getVisibleFrom())
                .param("visibleUntil", e.getVisibleUntil())
                .param("visibleUntilCohortEnd", e.getVisibleUntilCohortEnd())
                .param("attemptFrom", e.getAttemptFrom())
                .param("dueAt", e.getDueAt())
                .param("attemptUntilCohortEnd", e.getAttemptUntilCohortEnd())
                .param("visibleStartDay", e.getVisibleStartDay())
                .param("visibleStartTime", e.getVisibleStartTime())
                .param("visibleEndDay", e.getVisibleEndDay())
                .param("visibleEndTime", e.getVisibleEndTime())
                .param("attemptStartDay", e.getAttemptStartDay())
                .param("attemptStartTime", e.getAttemptStartTime())
                .param("attemptEndDay", e.getAttemptEndDay())
                .param("attemptEndTime", e.getAttemptEndTime())
                .param("lateSubmissionAllowed", e.getLateSubmissionAllowed())
                .param("lateSubmissionDays", e.getLateSubmissionDays())
                .param("isGraded", e.getIsGraded())
                .param("points", e.getPoints())
                .param("weightPercent", e.getWeightPercent())
                .param("gradingCategoryId", e.getGradingCategoryId())
                .param("rubricId", e.getRubricId())
                .param("plagiarismCheckRequired", e.getPlagiarismCheckRequired())
                .param("showInCalendar", e.getShowInCalendar())
                .param("allowDelete", e.getAllowDelete())
                .param("allowEditDetails", e.getAllowEditDetails())
                .param("allowEditDates", e.getAllowEditDates())
                .param("allowEditGrading", e.getAllowEditGrading())
                .param("copiedFromId", e.getCopiedFromId())
                .param("isOriginal", e.getIsOriginal())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ContentItemRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ContentItemEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("exerciseId", e.getExerciseId())
                .param("chapterId", e.getChapterId())
                .param("courseId", e.getCourseId())
                .param("cohortId", e.getCohortId())
                .param("itemKind", e.getItemKind())
                .param("lessonId", e.getLessonId())
                .param("fileId", e.getFileId())
                .param("assignmentId", e.getAssignmentId())
                .param("quizId", e.getQuizId())
                .param("discussionPromptId", e.getDiscussionPromptId())
                .param("participationActivityId", e.getParticipationActivityId())
                .param("webResourceId", e.getWebResourceId())
                .param("ebookId", e.getEbookId())
                .param("externalToolResourceId", e.getExternalToolResourceId())
                .param("liveClassId", e.getLiveClassId())
                .param("linkUrl", e.getLinkUrl())
                .param("title", e.getTitle())
                .param("sortOrder", e.getSortOrder())
                .param("isActive", e.getIsActive())
                .param("isAlwaysVisible", e.getIsAlwaysVisible())
                .param("visibleFrom", e.getVisibleFrom())
                .param("visibleUntil", e.getVisibleUntil())
                .param("visibleUntilCohortEnd", e.getVisibleUntilCohortEnd())
                .param("attemptFrom", e.getAttemptFrom())
                .param("dueAt", e.getDueAt())
                .param("attemptUntilCohortEnd", e.getAttemptUntilCohortEnd())
                .param("visibleStartDay", e.getVisibleStartDay())
                .param("visibleStartTime", e.getVisibleStartTime())
                .param("visibleEndDay", e.getVisibleEndDay())
                .param("visibleEndTime", e.getVisibleEndTime())
                .param("attemptStartDay", e.getAttemptStartDay())
                .param("attemptStartTime", e.getAttemptStartTime())
                .param("attemptEndDay", e.getAttemptEndDay())
                .param("attemptEndTime", e.getAttemptEndTime())
                .param("lateSubmissionAllowed", e.getLateSubmissionAllowed())
                .param("lateSubmissionDays", e.getLateSubmissionDays())
                .param("isGraded", e.getIsGraded())
                .param("points", e.getPoints())
                .param("weightPercent", e.getWeightPercent())
                .param("gradingCategoryId", e.getGradingCategoryId())
                .param("rubricId", e.getRubricId())
                .param("plagiarismCheckRequired", e.getPlagiarismCheckRequired())
                .param("showInCalendar", e.getShowInCalendar())
                .param("allowDelete", e.getAllowDelete())
                .param("allowEditDetails", e.getAllowEditDetails())
                .param("allowEditDates", e.getAllowEditDates())
                .param("allowEditGrading", e.getAllowEditGrading())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ContentItemEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.assignment row exists (live). */
    public boolean existsAssignmentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ASSIGNMENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsCopiedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COPIED_FROM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.discussion_prompt row exists (live). */
    public boolean existsDiscussionPromptId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_DISCUSSION_PROMPT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.ebook row exists (live). */
    public boolean existsEbookId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EBOOK_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.programming_exercise row exists (live). */
    public boolean existsExerciseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXERCISE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.external_tool_resource row exists (live). */
    public boolean existsExternalToolResourceId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXTERNAL_TOOL_RESOURCE_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced lms.grading_category row exists (live). */
    public boolean existsGradingCategoryId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_GRADING_CATEGORY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.lesson row exists (live). */
    public boolean existsLessonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LESSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.live_class row exists (live). */
    public boolean existsLiveClassId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LIVE_CLASS_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.participation_activity row exists (live). */
    public boolean existsParticipationActivityId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PARTICIPATION_ACTIVITY_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced lms.quiz row exists (live). */
    public boolean existsQuizId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUIZ_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.rubric row exists (live). */
    public boolean existsRubricId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_RUBRIC_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.web_resource row exists (live). */
    public boolean existsWebResourceId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_WEB_RESOURCE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.annotation rows still reference the row. */
    public boolean referencedByAnnotationContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ANNOTATION_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.assignment_group rows still reference the row. */
    public boolean referencedByAssignmentGroupContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_GROUP_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.assignment_group_member rows still reference the row. */
    public boolean referencedByAssignmentGroupMemberContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_GROUP_MEMBER_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.assignment_submission rows still reference the row. */
    public boolean referencedByAssignmentSubmissionContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ASSIGNMENT_SUBMISSION_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemCopiedFromId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_COPIED_FROM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.content_item_rating rows still reference the row. */
    public boolean referencedByContentItemRatingContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_RATING_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.content_view_session rows still reference the row. */
    public boolean referencedByContentViewSessionContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_VIEW_SESSION_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.forum_topic rows still reference the row. */
    public boolean referencedByForumTopicContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_TOPIC_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.gradebook_entry rows still reference the row. */
    public boolean referencedByGradebookEntryContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_GRADEBOOK_ENTRY_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.item_override rows still reference the row. */
    public boolean referencedByItemOverrideContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ITEM_OVERRIDE_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.quiz_submission rows still reference the row. */
    public boolean referencedByQuizSubmissionContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUIZ_SUBMISSION_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.reassignment_log rows still reference the row. */
    public boolean referencedByReassignmentLogContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_REASSIGNMENT_LOG_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.release_grant rows still reference the row. */
    public boolean referencedByReleaseGrantContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_GRANT_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.release_rule rows still reference the row. */
    public boolean referencedByReleaseRuleSourceContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_RULE_SOURCE_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.release_rule_target rows still reference the row. */
    public boolean referencedByReleaseRuleTargetContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELEASE_RULE_TARGET_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.rubric_assessment rows still reference the row. */
    public boolean referencedByRubricAssessmentContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RUBRIC_ASSESSMENT_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.student_alert rows still reference the row. */
    public boolean referencedByStudentAlertContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STUDENT_ALERT_CONTENT_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through course_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static final String NEXT_SORT_ORDER = """
            SELECT COALESCE(MAX(sort_order) + 1, 0) FROM lms.content_item
             WHERE tenant_id = :tenantId
               AND chapter_id = :chapterId AND deleted_at IS NULL
            """;

    /** The next free place at the end of a chapter. */
    public int nextSortOrder(UUID chapterId) {
        return jdbc.sql(NEXT_SORT_ORDER).param("chapterId", chapterId).query(Integer.class).single();
    }

    private static final String OF_CHAPTERS = SELECT_LIVE
            + " AND r.chapter_id = ANY(:chapterIds) ORDER BY r.chapter_id, r.sort_order, r.created_at";

    /** The live items of the chapters, in reading order. */
    public List<ContentItemEntity> ofChapters(List<UUID> chapterIds) {
        if (chapterIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(OF_CHAPTERS).param("chapterIds", chapterIds.toArray(UUID[]::new))
                .query(ContentItemRepository::map).list();
    }

    private static List<Object> sortValues(SortSpec sort, ContentItemEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ContentItemEntity map(ResultSet rs, int row) throws SQLException {
        return ContentItemEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .exerciseId(rs.getObject("exercise_id", UUID.class))
                .chapterId(rs.getObject("chapter_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .itemKind(rs.getString("item_kind"))
                .lessonId(rs.getObject("lesson_id", UUID.class))
                .fileId(rs.getObject("file_id", UUID.class))
                .assignmentId(rs.getObject("assignment_id", UUID.class))
                .quizId(rs.getObject("quiz_id", UUID.class))
                .discussionPromptId(rs.getObject("discussion_prompt_id", UUID.class))
                .participationActivityId(rs.getObject("participation_activity_id", UUID.class))
                .webResourceId(rs.getObject("web_resource_id", UUID.class))
                .ebookId(rs.getObject("ebook_id", UUID.class))
                .externalToolResourceId(rs.getObject("external_tool_resource_id", UUID.class))
                .liveClassId(rs.getObject("live_class_id", UUID.class))
                .linkUrl(rs.getString("link_url"))
                .title(rs.getString("title"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .isActive(rs.getObject("is_active", Boolean.class))
                .isAlwaysVisible(rs.getObject("is_always_visible", Boolean.class))
                .visibleFrom(rs.getObject("visible_from", OffsetDateTime.class))
                .visibleUntil(rs.getObject("visible_until", OffsetDateTime.class))
                .visibleUntilCohortEnd(rs.getObject("visible_until_cohort_end", Boolean.class))
                .attemptFrom(rs.getObject("attempt_from", OffsetDateTime.class))
                .dueAt(rs.getObject("due_at", OffsetDateTime.class))
                .attemptUntilCohortEnd(rs.getObject("attempt_until_cohort_end", Boolean.class))
                .visibleStartDay(rs.getObject("visible_start_day", Integer.class))
                .visibleStartTime(rs.getObject("visible_start_time", LocalTime.class))
                .visibleEndDay(rs.getObject("visible_end_day", Integer.class))
                .visibleEndTime(rs.getObject("visible_end_time", LocalTime.class))
                .attemptStartDay(rs.getObject("attempt_start_day", Integer.class))
                .attemptStartTime(rs.getObject("attempt_start_time", LocalTime.class))
                .attemptEndDay(rs.getObject("attempt_end_day", Integer.class))
                .attemptEndTime(rs.getObject("attempt_end_time", LocalTime.class))
                .lateSubmissionAllowed(rs.getObject("late_submission_allowed", Boolean.class))
                .lateSubmissionDays(rs.getObject("late_submission_days", Integer.class))
                .isGraded(rs.getObject("is_graded", Boolean.class))
                .points(rs.getBigDecimal("points"))
                .weightPercent(rs.getBigDecimal("weight_percent"))
                .gradingCategoryId(rs.getObject("grading_category_id", UUID.class))
                .rubricId(rs.getObject("rubric_id", UUID.class))
                .plagiarismCheckRequired(rs.getObject("plagiarism_check_required", Boolean.class))
                .showInCalendar(rs.getObject("show_in_calendar", Boolean.class))
                .allowDelete(rs.getObject("allow_delete", Boolean.class))
                .allowEditDetails(rs.getObject("allow_edit_details", Boolean.class))
                .allowEditDates(rs.getObject("allow_edit_dates", Boolean.class))
                .allowEditGrading(rs.getObject("allow_edit_grading", Boolean.class))
                .copiedFromId(rs.getObject("copied_from_id", UUID.class))
                .isOriginal(rs.getObject("is_original", Boolean.class))
                .viewCount(rs.getObject("view_count", Long.class))
                .likeCount(rs.getObject("like_count", Long.class))
                .ratingTotal(rs.getBigDecimal("rating_total"))
                .ratingCount(rs.getObject("rating_count", Integer.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
