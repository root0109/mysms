package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC writes of grades into the gradebook (lms.gradebook_entry with its history in lms.grade_change_log and the
 * rubric choices in lms.rubric_assessment): the graded item, the student's or group's entry locked for the change,
 * and the guarded writes. One entry per item and student or group (unique index {@code ix_gradebook_entry_1}).
 */
@Repository
public class GradebookPostingRepository {

    /** The graded content item with what grading needs from it and its cohort. */
    public record GradedItem(UUID id, UUID cohortId, UUID institutionId, boolean graded, BigDecimal points,
                             UUID rubricId, String itemKind, UUID assignmentId) {
    }

    /** One entry as the grading steps need it. */
    public record Entry(UUID id, UUID userId, UUID groupId, BigDecimal points, BigDecimal autoPoints,
                        BigDecimal manualPoints, String feedback, String teacherComment, String status,
                        boolean published, OffsetDateTime publishedAt, BigDecimal pointsPossible, int rowVersion) {
    }

    /** A rubric cell with the weight of its criterion and the score of its level. */
    public record RubricCell(UUID id, UUID criterionId, BigDecimal weightPercent, BigDecimal scorePercent) {
    }

    /** The values an entry takes. */
    public record Grade(BigDecimal points, BigDecimal autoPoints, BigDecimal manualPoints, String feedback,
                        String teacherComment, String status, boolean published, OffsetDateTime publishedAt,
                        BigDecimal pointsPossible, UUID quizSubmissionId) {
    }

    private static final String ITEM = """
            SELECT ci.id, COALESCE(ci.cohort_id, ch.cohort_id) AS cohort_id, co.institution_id, ci.is_graded,
                   ci.points, ci.rubric_id, ci.item_kind, ci.assignment_id
              FROM lms.content_item ci
              LEFT JOIN lms.chapter ch ON ch.id = ci.chapter_id AND ch.tenant_id = :tenantId
              LEFT JOIN lms.cohort co ON co.id = COALESCE(ci.cohort_id, ch.cohort_id) AND co.tenant_id = :tenantId
             WHERE ci.tenant_id = :tenantId AND ci.id = :id AND ci.deleted_at IS NULL
            """;
    private static final String LOCK_FOR_USER = """
            SELECT id, user_id, group_id, points, auto_points, manual_points, feedback, teacher_comment, status,
                   is_published, published_at, points_possible, row_version
              FROM lms.gradebook_entry
             WHERE tenant_id = :tenantId AND content_item_id = :itemId AND user_id = :userId AND deleted_at IS NULL
             FOR UPDATE
            """;
    private static final String LOCK_FOR_GROUP = """
            SELECT id, user_id, group_id, points, auto_points, manual_points, feedback, teacher_comment, status,
                   is_published, published_at, points_possible, row_version
              FROM lms.gradebook_entry
             WHERE tenant_id = :tenantId AND content_item_id = :itemId AND group_id = :groupId AND deleted_at IS NULL
             FOR UPDATE
            """;
    private static final String INSERT = """
            INSERT INTO lms.gradebook_entry (id, tenant_id, cohort_id, content_item_id, user_id, group_id,
                   quiz_submission_id, points, auto_points, manual_points, feedback, teacher_comment, status,
                   graded_by, graded_at, is_published, published_at, points_possible, created_at, created_by,
                   updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :cohortId, :itemId, :userId, :groupId, :quizSubmissionId, :points, :autoPoints,
                    :manualPoints, :feedback, :teacherComment, :status, :actor, :now, :published, :publishedAt,
                    :pointsPossible, :now, :actor, :now, :actor, 1)
            ON CONFLICT DO NOTHING
            """;
    private static final String UPDATE = """
            UPDATE lms.gradebook_entry
               SET points = :points, auto_points = :autoPoints, manual_points = :manualPoints, feedback = :feedback,
                   teacher_comment = :teacherComment, status = :status, graded_by = :actor, graded_at = :now,
                   is_published = :published, published_at = :publishedAt, points_possible = :pointsPossible,
                   quiz_submission_id = COALESCE(:quizSubmissionId, quiz_submission_id), updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String CHANGE_LOG = """
            INSERT INTO lms.grade_change_log (id, tenant_id, gradebook_entry_id, points, auto_points, manual_points,
                   comment, was_published, published_at, changed_by, changed_at, student_user_id, created_at,
                   created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :entryId, :points, :autoPoints, :manualPoints, :comment, :published, :publishedAt,
                    :actor, :now, :studentId, :now, :actor, :now, :actor, 1)
            """;
    private static final String RUBRIC_CELLS = """
            SELECT c.id, c.rubric_criterion_id, cr.weight_percent, l.score_percent
              FROM lms.rubric_cell c
              JOIN lms.rubric_criterion cr ON cr.id = c.rubric_criterion_id AND cr.tenant_id = :tenantId
              JOIN lms.rubric_level l ON l.id = c.rubric_level_id AND l.tenant_id = :tenantId
             WHERE c.tenant_id = :tenantId AND c.rubric_id = :rubricId AND c.id IN (:ids) AND c.deleted_at IS NULL
            """;
    private static final String RUBRIC_VERSION = """
            SELECT version FROM lms.rubric WHERE tenant_id = :tenantId AND id = :id
            """;
    private static final String CLEAR_RUBRIC_FOR_USER = """
            UPDATE lms.rubric_assessment SET deleted_at = :now, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND content_item_id = :itemId AND user_id = :userId AND deleted_at IS NULL
            """;
    private static final String CLEAR_RUBRIC_FOR_GROUP = """
            UPDATE lms.rubric_assessment SET deleted_at = :now, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND content_item_id = :itemId AND group_id = :groupId AND deleted_at IS NULL
            """;
    private static final String INSERT_RUBRIC = """
            INSERT INTO lms.rubric_assessment (id, tenant_id, content_item_id, user_id, group_id, rubric_cell_id,
                   rubric_version, points_awarded, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :itemId, :userId, :groupId, :cellId, :version, :points, :now, :actor, :now,
                    :actor, 1)
            """;
    private static final String ENROLLED = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_enrollment WHERE tenant_id = :tenantId AND cohort_id = :cohortId
                              AND user_id = :userId AND deleted_at IS NULL)
            """;
    private static final String GROUP_OF_ITEM = """
            SELECT EXISTS (SELECT 1 FROM lms.assignment_group WHERE tenant_id = :tenantId AND id = :groupId
                              AND content_item_id = :itemId AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public GradebookPostingRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<GradedItem> item(UUID id) {
        return jdbc.sql(ITEM).param("id", id).query((rs, n) -> new GradedItem(rs.getObject("id", UUID.class),
                rs.getObject("cohort_id", UUID.class), rs.getObject("institution_id", UUID.class),
                rs.getBoolean("is_graded"), rs.getBigDecimal("points"), rs.getObject("rubric_id", UUID.class),
                rs.getString("item_kind"), rs.getObject("assignment_id", UUID.class))).optional();
    }

    /** The student's or the group's entry for the item, locked until the transaction ends. */
    public Optional<Entry> lock(UUID itemId, UUID userId, UUID groupId) {
        String sql = userId != null ? LOCK_FOR_USER : LOCK_FOR_GROUP;
        return jdbc.sql(sql).param("itemId", itemId).param(userId != null ? "userId" : "groupId",
                userId != null ? userId : groupId).query(GradebookPostingRepository::entry).optional();
    }

    /** Inserts a new entry; false when another call inserted the same student's or group's entry meanwhile. */
    public boolean insert(UUID id, UUID cohortId, UUID itemId, UUID userId, UUID groupId, Grade grade,
            OffsetDateTime now, UUID actor) {
        return params(jdbc.sql(INSERT), grade).param("id", id).param("cohortId", cohortId).param("itemId", itemId)
                .param("userId", userId).param("groupId", groupId).param("now", now).param("actor", actor)
                .update() == 1;
    }

    public int update(UUID id, int expected, Grade grade, OffsetDateTime now, UUID actor) {
        return params(jdbc.sql(UPDATE), grade).param("id", id).param("expected", expected).param("now", now)
                .param("actor", actor).update();
    }

    public void logChange(UUID id, UUID entryId, Grade grade, UUID studentId, OffsetDateTime now, UUID actor) {
        String comment = grade.feedback() != null ? grade.feedback() : grade.teacherComment();
        jdbc.sql(CHANGE_LOG).param("id", id).param("entryId", entryId).param("points", grade.points())
                .param("autoPoints", grade.autoPoints()).param("manualPoints", grade.manualPoints())
                .param("comment", comment).param("published", grade.published())
                .param("publishedAt", grade.publishedAt()).param("studentId", studentId).param("now", now)
                .param("actor", actor).update();
    }

    public List<RubricCell> rubricCells(UUID rubricId, Collection<UUID> ids) {
        return jdbc.sql(RUBRIC_CELLS).param("rubricId", rubricId).param("ids", ids).query((rs, n) -> new RubricCell(
                rs.getObject("id", UUID.class), rs.getObject("rubric_criterion_id", UUID.class),
                rs.getBigDecimal("weight_percent"), rs.getBigDecimal("score_percent"))).list();
    }

    public Integer rubricVersion(UUID rubricId) {
        return jdbc.sql(RUBRIC_VERSION).param("id", rubricId).query(Integer.class).optional().orElse(null);
    }

    /** Replaces the rubric choices of the student or group for the item. */
    public void clearRubric(UUID itemId, UUID userId, UUID groupId, OffsetDateTime now, UUID actor) {
        jdbc.sql(userId != null ? CLEAR_RUBRIC_FOR_USER : CLEAR_RUBRIC_FOR_GROUP).param("itemId", itemId)
                .param(userId != null ? "userId" : "groupId", userId != null ? userId : groupId).param("now", now)
                .param("actor", actor).update();
    }

    public void insertRubric(UUID id, UUID itemId, UUID userId, UUID groupId, UUID cellId, Integer version,
            BigDecimal points, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_RUBRIC).param("id", id).param("itemId", itemId).param("userId", userId)
                .param("groupId", groupId).param("cellId", cellId).param("version", version).param("points", points)
                .param("now", now).param("actor", actor).update();
    }

    public boolean enrolled(UUID cohortId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(ENROLLED).param("cohortId", cohortId).param("userId", userId)
                .query(Boolean.class).single());
    }

    public boolean groupOfItem(UUID groupId, UUID itemId) {
        return Boolean.TRUE.equals(jdbc.sql(GROUP_OF_ITEM).param("groupId", groupId).param("itemId", itemId)
                .query(Boolean.class).single());
    }

    private static JdbcClient.StatementSpec params(JdbcClient.StatementSpec spec, Grade grade) {
        return spec.param("points", grade.points()).param("autoPoints", grade.autoPoints())
                .param("manualPoints", grade.manualPoints()).param("feedback", grade.feedback())
                .param("teacherComment", grade.teacherComment()).param("status", grade.status())
                .param("published", grade.published()).param("publishedAt", grade.publishedAt())
                .param("pointsPossible", grade.pointsPossible()).param("quizSubmissionId", grade.quizSubmissionId());
    }

    private static Entry entry(ResultSet rs, int n) throws SQLException {
        return new Entry(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("group_id", UUID.class), rs.getBigDecimal("points"), rs.getBigDecimal("auto_points"),
                rs.getBigDecimal("manual_points"), rs.getString("feedback"), rs.getString("teacher_comment"),
                rs.getString("status"), rs.getBoolean("is_published"),
                rs.getObject("published_at", OffsetDateTime.class), rs.getBigDecimal("points_possible"),
                rs.getInt("row_version"));
    }
}
