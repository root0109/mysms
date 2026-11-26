package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access of course packages: the course template read for an export (chapters, lessons, quizzes with
 * their questions, assignments), the rows an import writes, and the import/export job. A course's template is its
 * chapters and items outside any cohort. Every statement filters the caller's tenant; row-level security is the
 * backstop.
 */
@Repository
public class CoursePackageRepository {

    public record CourseHead(UUID id, UUID institutionId, String code, String title, int versionMajor,
                             int versionMinor) {
    }

    public record ChapterRow(UUID id, String title, String description) {
    }

    public record LessonRow(UUID chapterId, String title, String body, boolean includeInPreview,
                            String youtubeVideoId) {
    }

    /** A quiz or assignment placed in a template chapter. */
    public record PlacedRow(UUID chapterId, String itemKind, UUID quizId, UUID assignmentId) {
    }

    public record QuizRow(UUID id, String title, String description, int passingPercentage, Integer durationMinutes,
                          Integer maxAttempts) {
    }

    public record QuestionRow(UUID quizId, UUID id, String questionType, String title, String questionText,
                              BigDecimal marks, boolean allowsMultiple, boolean caseSensitive,
                              Boolean trueFalseAnswer, String numericAnswer, String numericTolerance,
                              boolean numericIsRange, String generalFeedback) {
    }

    public record OptionRow(UUID questionId, String text, boolean correct, String explanation) {
    }

    public record AnswerRow(UUID questionId, String text) {
    }

    public record AssignmentRow(UUID id, String title, String question, String submissionType) {
    }

    /** The job of a package, as its workflows need it. */
    public record JobHead(UUID id, UUID institutionId, String direction, String packageFormat, String scope,
                          UUID courseId, UUID cohortId, UUID packageFileId, UUID requestedBy, String status,
                          String unarchiveStatus, int rowVersion) {
    }

    private static final String COURSE = """
            SELECT id, institution_id, code, title, version_major, version_minor FROM lms.course
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String COURSE_FOR_UPDATE = COURSE + " FOR UPDATE";
    private static final String TEMPLATE_CHAPTER = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId AND id = :id AND course_id = :courseId
                              AND cohort_id IS NULL AND deleted_at IS NULL)
            """;
    private static final String CHAPTERS = """
            SELECT id, title, description FROM lms.chapter
             WHERE tenant_id = :tenantId AND course_id = :courseId AND cohort_id IS NULL AND deleted_at IS NULL
             ORDER BY sort_order, id
            """;
    private static final String LESSONS = """
            SELECT chapter_id, title, body, include_in_preview, youtube_video_id FROM lms.lesson
             WHERE tenant_id = :tenantId AND chapter_id IN (:chapters) AND deleted_at IS NULL
             ORDER BY sort_order, id
            """;
    private static final String PLACED = """
            SELECT chapter_id, item_kind, quiz_id, assignment_id FROM lms.content_item
             WHERE tenant_id = :tenantId AND chapter_id IN (:chapters) AND cohort_id IS NULL
               AND item_kind IN ('quiz', 'assignment') AND deleted_at IS NULL
             ORDER BY sort_order, id
            """;
    private static final String QUIZZES = """
            SELECT id, title, description, passing_percentage, duration_minutes, max_attempts FROM lms.quiz
             WHERE tenant_id = :tenantId AND id IN (:ids) AND deleted_at IS NULL
            """;
    private static final String QUESTIONS = """
            SELECT qq.quiz_id, q.id, q.question_type, q.title, q.question_text, qq.marks, q.allows_multiple,
                   q.case_sensitive, q.true_false_answer, q.numeric_answer, q.numeric_tolerance, q.numeric_is_range,
                   q.general_feedback
              FROM lms.quiz_question qq
              JOIN lms.question q ON q.id = qq.question_id AND q.tenant_id = :tenantId AND q.deleted_at IS NULL
             WHERE qq.tenant_id = :tenantId AND qq.quiz_id IN (:ids) AND qq.deleted_at IS NULL
             ORDER BY qq.sort_order, qq.id
            """;
    private static final String OPTIONS = """
            SELECT question_id, option_text, is_correct, explanation FROM lms.question_option
             WHERE tenant_id = :tenantId AND question_id IN (:ids) AND deleted_at IS NULL ORDER BY sort_order, id
            """;
    private static final String ACCEPTED = """
            SELECT question_id, answer_text FROM lms.question_accepted_answer
             WHERE tenant_id = :tenantId AND question_id IN (:ids) AND deleted_at IS NULL ORDER BY sort_order, id
            """;
    private static final String ASSIGNMENTS = """
            SELECT id, title, question, submission_type FROM lms.assignment
             WHERE tenant_id = :tenantId AND id IN (:ids) AND deleted_at IS NULL
            """;
    private static final String NEXT_CHAPTER_ORDER = """
            SELECT COALESCE(MAX(sort_order), 0) FROM lms.chapter
             WHERE tenant_id = :tenantId AND course_id = :courseId AND cohort_id IS NULL AND parent_chapter_id IS NULL
               AND deleted_at IS NULL
            """;
    private static final String NEXT_LESSON_ORDER = """
            SELECT COALESCE(MAX(sort_order), 0) FROM lms.lesson
             WHERE tenant_id = :tenantId AND chapter_id = :chapterId AND deleted_at IS NULL
            """;
    private static final String NEXT_ITEM_ORDER = """
            SELECT COALESCE(MAX(sort_order), 0) FROM lms.content_item
             WHERE tenant_id = :tenantId AND chapter_id = :chapterId AND deleted_at IS NULL
            """;
    private static final String INSERT_CHAPTER = """
            INSERT INTO lms.chapter (id, tenant_id, course_id, title, sort_order, description, created_at, created_by,
                   updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :courseId, :title, :sortOrder, :description, :now, :actor, :now, :actor, 1)
            """;
    private static final String INSERT_LESSON = """
            INSERT INTO lms.lesson (id, tenant_id, chapter_id, title, sort_order, include_in_preview, body,
                   youtube_video_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :chapterId, :title, :sortOrder, :preview, :body, :youtube, :now, :actor, :now,
                    :actor, 1)
            """;
    private static final String INSERT_QUIZ = """
            INSERT INTO lms.quiz (id, tenant_id, title, course_id, institution_id, description, passing_percentage,
                   total_marks, duration_minutes, max_attempts, is_in_repository, created_at, created_by, updated_at,
                   updated_by, row_version)
            VALUES (:id, :tenantId, :title, :courseId, :institutionId, :description, :passing, :total, :duration,
                    :maxAttempts, :repository, :now, :actor, :now, :actor, 1)
            """;
    private static final String INSERT_QUESTION = """
            INSERT INTO lms.question (id, tenant_id, question_type, title, question_text, marks, allows_multiple,
                   case_sensitive, true_false_answer, numeric_answer, numeric_tolerance, numeric_is_range,
                   general_feedback, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :type, :title, :text, :marks, :multiple, :caseSensitive, :trueFalse, :numeric,
                    :tolerance, :range, :feedback, :now, :actor, :now, :actor, 1)
            """;
    private static final String INSERT_OPTION = """
            INSERT INTO lms.question_option (id, tenant_id, question_id, sort_order, option_text, is_correct,
                   explanation, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :questionId, :sortOrder, :text, :correct, :explanation, :now, :actor, :now,
                    :actor, 1)
            """;
    private static final String INSERT_ACCEPTED = """
            INSERT INTO lms.question_accepted_answer (id, tenant_id, question_id, sort_order, answer_text, created_at,
                   created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :questionId, :sortOrder, :text, :now, :actor, :now, :actor, 1)
            """;
    private static final String INSERT_QUIZ_QUESTION = """
            INSERT INTO lms.quiz_question (id, tenant_id, quiz_id, question_id, marks, sort_order, created_at,
                   created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :quizId, :questionId, :marks, :sortOrder, :now, :actor, :now, :actor, 1)
            """;
    private static final String INSERT_ASSIGNMENT = """
            INSERT INTO lms.assignment (id, tenant_id, title, question, submission_type, course_id, institution_id,
                   is_in_repository, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :title, :question, :submissionType, :courseId, :institutionId, :repository,
                    :now, :actor, :now, :actor, 1)
            """;
    private static final String INSERT_ITEM = """
            INSERT INTO lms.content_item (id, tenant_id, chapter_id, course_id, item_kind, quiz_id, assignment_id,
                   title, sort_order, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :chapterId, :courseId, :kind, :quizId, :assignmentId, :title, :sortOrder, :now,
                    :actor, :now, :actor, 1)
            """;
    private static final String INSERT_JOB = """
            INSERT INTO lms.import_export_job (id, tenant_id, institution_id, direction, package_format, scope,
                   course_id, target_chapter_id, add_to_repository, package_file_id, requested_by, requested_on,
                   started_on, status, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :direction, 'native', 'course', :courseId, :targetChapterId,
                    :repository, :fileId, :actor, :today, :today, 'in_progress', :now, :actor, :now, :actor, 1)
            """;
    private static final String FINISH_JOB = """
            UPDATE lms.import_export_job
               SET status = :status, error_log = :errorLog, package_file_id = COALESCE(:fileId, package_file_id),
                   course_code = :courseCode, course_title = :courseTitle, course_version = :courseVersion,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String JOB = """
            SELECT id, institution_id, direction, package_format, scope, course_id, cohort_id, package_file_id,
                   requested_by, status, unarchive_status, row_version
              FROM lms.import_export_job WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String JOB_FOR_UPDATE = JOB + " FOR UPDATE";
    private static final String COHORT_LIFECYCLE = """
            SELECT lifecycle_status FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
               FOR UPDATE
            """;
    private static final String RESTORE_COHORT = """
            UPDATE lms.cohort SET lifecycle_status = 'completed', updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND lifecycle_status = 'archived' AND deleted_at IS NULL
            """;
    private static final String RESTORED = """
            UPDATE lms.import_export_job SET unarchive_status = 'complete', updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public CoursePackageRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CourseHead> course(UUID id) {
        return jdbc.sql(COURSE).param("id", id).query(CoursePackageRepository::course).optional();
    }

    public Optional<CourseHead> lockCourse(UUID id) {
        return jdbc.sql(COURSE_FOR_UPDATE).param("id", id).query(CoursePackageRepository::course).optional();
    }

    public boolean templateChapter(UUID courseId, UUID chapterId) {
        return Boolean.TRUE.equals(jdbc.sql(TEMPLATE_CHAPTER).param("courseId", courseId).param("id", chapterId)
                .query(Boolean.class).single());
    }

    // ---- export reads

    public List<ChapterRow> chapters(UUID courseId) {
        return jdbc.sql(CHAPTERS).param("courseId", courseId).query((rs, n) -> new ChapterRow(
                rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("description"))).list();
    }

    public List<LessonRow> lessons(Collection<UUID> chapters) {
        return chapters.isEmpty() ? List.of() : jdbc.sql(LESSONS).param("chapters", chapters).query((rs, n) ->
                new LessonRow(rs.getObject("chapter_id", UUID.class), rs.getString("title"), rs.getString("body"),
                        rs.getBoolean("include_in_preview"), rs.getString("youtube_video_id"))).list();
    }

    public List<PlacedRow> placed(Collection<UUID> chapters) {
        return chapters.isEmpty() ? List.of() : jdbc.sql(PLACED).param("chapters", chapters).query((rs, n) ->
                new PlacedRow(rs.getObject("chapter_id", UUID.class), rs.getString("item_kind"),
                        rs.getObject("quiz_id", UUID.class), rs.getObject("assignment_id", UUID.class))).list();
    }

    public List<QuizRow> quizzes(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : jdbc.sql(QUIZZES).param("ids", ids).query((rs, n) -> new QuizRow(
                rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("description"),
                rs.getInt("passing_percentage"), (Integer) rs.getObject("duration_minutes"),
                (Integer) rs.getObject("max_attempts"))).list();
    }

    public List<QuestionRow> questions(Collection<UUID> quizIds) {
        return quizIds.isEmpty() ? List.of() : jdbc.sql(QUESTIONS).param("ids", quizIds).query((rs, n) ->
                new QuestionRow(rs.getObject("quiz_id", UUID.class), rs.getObject("id", UUID.class),
                        rs.getString("question_type"), rs.getString("title"), rs.getString("question_text"),
                        rs.getBigDecimal("marks"), rs.getBoolean("allows_multiple"), rs.getBoolean("case_sensitive"),
                        (Boolean) rs.getObject("true_false_answer"), rs.getString("numeric_answer"),
                        rs.getString("numeric_tolerance"), rs.getBoolean("numeric_is_range"),
                        rs.getString("general_feedback"))).list();
    }

    public List<OptionRow> options(Collection<UUID> questionIds) {
        return questionIds.isEmpty() ? List.of() : jdbc.sql(OPTIONS).param("ids", questionIds).query((rs, n) ->
                new OptionRow(rs.getObject("question_id", UUID.class), rs.getString("option_text"),
                        rs.getBoolean("is_correct"), rs.getString("explanation"))).list();
    }

    public List<AnswerRow> acceptedAnswers(Collection<UUID> questionIds) {
        return questionIds.isEmpty() ? List.of() : jdbc.sql(ACCEPTED).param("ids", questionIds).query((rs, n) ->
                new AnswerRow(rs.getObject("question_id", UUID.class), rs.getString("answer_text"))).list();
    }

    public List<AssignmentRow> assignments(Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : jdbc.sql(ASSIGNMENTS).param("ids", ids).query((rs, n) -> new AssignmentRow(
                rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("question"),
                rs.getString("submission_type"))).list();
    }

    // ---- import writes

    public int nextChapterOrder(UUID courseId) {
        return jdbc.sql(NEXT_CHAPTER_ORDER).param("courseId", courseId).query(Integer.class).single() + 1;
    }

    public int nextLessonOrder(UUID chapterId) {
        return jdbc.sql(NEXT_LESSON_ORDER).param("chapterId", chapterId).query(Integer.class).single() + 1;
    }

    public int nextItemOrder(UUID chapterId) {
        return jdbc.sql(NEXT_ITEM_ORDER).param("chapterId", chapterId).query(Integer.class).single() + 1;
    }

    public void insertChapter(UUID id, UUID courseId, String title, int sortOrder, String description,
            OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_CHAPTER).param("id", id).param("courseId", courseId).param("title", title)
                .param("sortOrder", sortOrder).param("description", description).param("now", now)
                .param("actor", actor).update();
    }

    public void insertLesson(UUID id, UUID chapterId, String title, int sortOrder, boolean preview, String body,
            String youtube, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_LESSON).param("id", id).param("chapterId", chapterId).param("title", title)
                .param("sortOrder", sortOrder).param("preview", preview).param("body", body).param("youtube", youtube)
                .param("now", now).param("actor", actor).update();
    }

    public void insertQuiz(UUID id, CourseHead course, String title, String description, int passing,
            BigDecimal total, Integer duration, Integer maxAttempts, boolean repository, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(INSERT_QUIZ).param("id", id).param("title", title).param("courseId", course.id())
                .param("institutionId", course.institutionId()).param("description", description)
                .param("passing", passing).param("total", total).param("duration", duration)
                .param("maxAttempts", maxAttempts).param("repository", repository).param("now", now)
                .param("actor", actor).update();
    }

    public void insertQuestion(UUID id, String type, String title, String text, BigDecimal marks, boolean multiple,
            boolean caseSensitive, Boolean trueFalse, String numeric, String tolerance, boolean range,
            String feedback, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_QUESTION).param("id", id).param("type", type).param("title", title).param("text", text)
                .param("marks", marks).param("multiple", multiple).param("caseSensitive", caseSensitive)
                .param("trueFalse", trueFalse).param("numeric", numeric).param("tolerance", tolerance).param("range", range)
                .param("feedback", feedback).param("now", now).param("actor", actor).update();
    }

    public void insertOption(UUID id, UUID questionId, int sortOrder, String text, boolean correct,
            String explanation, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_OPTION).param("id", id).param("questionId", questionId).param("sortOrder", sortOrder)
                .param("text", text).param("correct", correct).param("explanation", explanation).param("now", now)
                .param("actor", actor).update();
    }

    public void insertAccepted(UUID id, UUID questionId, int sortOrder, String text, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(INSERT_ACCEPTED).param("id", id).param("questionId", questionId).param("sortOrder", sortOrder)
                .param("text", text).param("now", now).param("actor", actor).update();
    }

    public void insertQuizQuestion(UUID id, UUID quizId, UUID questionId, BigDecimal marks, int sortOrder,
            OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_QUIZ_QUESTION).param("id", id).param("quizId", quizId).param("questionId", questionId)
                .param("marks", marks).param("sortOrder", sortOrder).param("now", now).param("actor", actor).update();
    }

    public void insertAssignment(UUID id, CourseHead course, String title, String question, String submissionType,
            boolean repository, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_ASSIGNMENT).param("id", id).param("title", title).param("question", question)
                .param("submissionType", submissionType).param("courseId", course.id())
                .param("institutionId", course.institutionId()).param("repository", repository).param("now", now)
                .param("actor", actor).update();
    }

    public void insertItem(UUID id, UUID chapterId, UUID courseId, String kind, UUID quizId, UUID assignmentId,
            String title, int sortOrder, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_ITEM).param("id", id).param("chapterId", chapterId).param("courseId", courseId)
                .param("kind", kind).param("quizId", quizId).param("assignmentId", assignmentId).param("title", title)
                .param("sortOrder", sortOrder).param("now", now).param("actor", actor).update();
    }

    // ---- jobs

    public void insertJob(UUID id, UUID institutionId, String direction, UUID courseId, UUID targetChapterId,
            boolean repository, UUID fileId, LocalDate today, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_JOB).param("id", id).param("institutionId", institutionId).param("direction", direction)
                .param("courseId", courseId).param("targetChapterId", targetChapterId).param("repository", repository)
                .param("fileId", fileId).param("today", today).param("now", now).param("actor", actor).update();
    }

    /** Ends a job as {@code complete} or {@code error}, with what the package recorded about its course. */
    public void finishJob(UUID id, String status, String errorLog, UUID fileId, String courseCode,
            String courseTitle, String courseVersion, OffsetDateTime now, UUID actor) {
        jdbc.sql(FINISH_JOB).param("id", id).param("status", status).param("errorLog", errorLog)
                .param("fileId", fileId).param("courseCode", courseCode).param("courseTitle", courseTitle)
                .param("courseVersion", courseVersion).param("now", now).param("actor", actor).update();
    }

    public Optional<JobHead> job(UUID id) {
        return jdbc.sql(JOB).param("id", id).query(CoursePackageRepository::job).optional();
    }

    public Optional<JobHead> lockJob(UUID id) {
        return jdbc.sql(JOB_FOR_UPDATE).param("id", id).query(CoursePackageRepository::job).optional();
    }

    public Optional<String> lockCohortLifecycle(UUID cohortId) {
        return jdbc.sql(COHORT_LIFECYCLE).param("id", cohortId).query(String.class).optional();
    }

    public int restoreCohort(UUID cohortId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RESTORE_COHORT).param("id", cohortId).param("now", now).param("actor", actor).update();
    }

    public int restored(UUID jobId, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RESTORED).param("id", jobId).param("expected", expected).param("now", now)
                .param("actor", actor).update();
    }

    private static CourseHead course(ResultSet rs, int n) throws SQLException {
        return new CourseHead(rs.getObject("id", UUID.class), rs.getObject("institution_id", UUID.class),
                rs.getString("code"), rs.getString("title"), rs.getInt("version_major"), rs.getInt("version_minor"));
    }

    private static JobHead job(ResultSet rs, int n) throws SQLException {
        return new JobHead(rs.getObject("id", UUID.class), rs.getObject("institution_id", UUID.class),
                rs.getString("direction"), rs.getString("package_format"), rs.getString("scope"),
                rs.getObject("course_id", UUID.class), rs.getObject("cohort_id", UUID.class),
                rs.getObject("package_file_id", UUID.class), rs.getObject("requested_by", UUID.class),
                rs.getString("status"), rs.getString("unarchive_status"), rs.getInt("row_version"));
    }
}
