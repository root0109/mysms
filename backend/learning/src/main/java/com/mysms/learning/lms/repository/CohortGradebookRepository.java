package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.model.CourseProgressRow;
import com.mysms.learning.lms.model.GradeCell;
import com.mysms.learning.lms.model.GradedItem;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC reads of a cohort's grades: its graded items (the gradebook's columns), the
 * entries of its active students for them, the students' progress in the cohort's courses, and the statement that
 * publishes a final grade. Group grades (entries of an assignment group) are not read: the gradebook shows each
 * student's own entries. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CohortGradebookRepository {

    private static final String GRADED_ITEMS = """
            SELECT r.id, COALESCE(r.title, q.title, a.title, x.title, d.title, r.item_kind) AS title, r.item_kind,
                   r.grading_category_id, r.points, r.weight_percent, r.due_at, r.sort_order, r.created_at
              FROM lms.content_item r
              LEFT JOIN lms.quiz q ON q.tenant_id = :tenantId AND q.id = r.quiz_id
              LEFT JOIN lms.assignment a ON a.tenant_id = :tenantId AND a.id = r.assignment_id
              LEFT JOIN lms.programming_exercise x ON x.tenant_id = :tenantId AND x.id = r.exercise_id
              LEFT JOIN lms.discussion_prompt d ON d.tenant_id = :tenantId AND d.id = r.discussion_prompt_id
             WHERE r.tenant_id = :tenantId AND r.cohort_id = :cohortId AND r.is_graded AND r.is_active
               AND r.deleted_at IS NULL AND (CAST(:category AS uuid) IS NULL OR r.grading_category_id = :category)
            """;
    private static final String CELLS = """
            SELECT g.content_item_id, g.user_id, g.id, g.points, COALESCE(g.points_possible, ci.points) AS possible,
                   g.status, g.is_published
              FROM lms.gradebook_entry g
              JOIN lms.content_item ci ON ci.tenant_id = :tenantId AND ci.id = g.content_item_id
             WHERE g.tenant_id = :tenantId AND g.cohort_id = :cohortId AND g.deleted_at IS NULL
               AND g.user_id = ANY(:users) AND g.content_item_id = ANY(:items)
            """;
    private static final String COURSE_PROGRESS = """
            SELECT u.user_id, cc.course_id, COALESCE(e.progress_percent, 0) AS progress
              FROM unnest(:users) AS u(user_id)
              CROSS JOIN lms.cohort_course cc
              LEFT JOIN lms.enrollment e ON e.tenant_id = :tenantId AND e.course_id = cc.course_id
                    AND e.user_id = u.user_id AND e.deleted_at IS NULL
             WHERE cc.tenant_id = :tenantId AND cc.cohort_id = :cohortId AND cc.deleted_at IS NULL
             ORDER BY cc.created_at, cc.id
            """;
    private static final String PUBLISH_FINAL_GRADE = """
            UPDATE lms.cohort_enrollment
               SET final_grade = :grade, final_gpa = :gpa, graded_by = :actor, graded_at = :now,
                   grade_published = true, grade_published_at = :now, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND user_id = :userId AND is_active
               AND deleted_at IS NULL
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public CohortGradebookRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Every graded, active item of the cohort (optionally of one category), in outline order. */
    public List<GradedItem> gradedItems(UUID cohortId, UUID categoryId) {
        return jdbc.sql(GRADED_ITEMS + " ORDER BY r.sort_order, r.created_at, r.id").param("cohortId", cohortId)
                .param("category", categoryId).query((rs, n) -> item(rs)).list();
    }

    /** Keyset order of {@link #gradedItems(PageRequest, UUID)}: creation time, then id. */
    public static final Map<String, FieldSpec> ITEM_SORT = Map.of(
            "createTime", new FieldSpec("createTime", "r.created_at",
                    ValueType.TIMESTAMP));

    /** A page of the cohort's graded items in creation order (the performance report pages by item). */
    public Page<GradedItem> gradedItems(PageRequest page, UUID cohortId) {
        SqlFragment keyset = page.keysetPredicate();
        List<GradedItem> rows = jdbc.sql(GRADED_ITEMS + " AND (" + keyset.sql() + ") ORDER BY " + page.orderByClause()
                        + page.limitClause())
                .param("cohortId", cohortId).param("category", null).params(keyset.params())
                .params(page.limitParams()).query((rs, n) -> item(rs)).list();
        return page.toPage(rows, row -> List.of(row.createdAt()), GradedItem::contentItemId);
    }

    /** The entries of these students for these items. */
    public List<GradeCell> cells(UUID cohortId, List<UUID> users, List<UUID> items) {
        if (users.isEmpty() || items.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(CELLS).param("cohortId", cohortId).param("users", users.toArray(UUID[]::new))
                .param("items", items.toArray(UUID[]::new))
                .query((rs, n) -> new GradeCell(rs.getObject("content_item_id", UUID.class),
                        rs.getObject("user_id", UUID.class), rs.getObject("id", UUID.class), rs.getBigDecimal("points"),
                        rs.getBigDecimal("possible"), rs.getString("status"), rs.getBoolean("is_published")))
                .list();
    }

    /** Each student's progress in each course of the cohort (0 where they have no enrolment in it). */
    public List<CourseProgressRow> courseProgress(UUID cohortId, List<UUID> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(COURSE_PROGRESS).param("cohortId", cohortId).param("users", users.toArray(UUID[]::new))
                .query((rs, n) -> new CourseProgressRow(rs.getObject("user_id", UUID.class),
                        rs.getObject("course_id", UUID.class), rs.getBigDecimal("progress")))
                .list();
    }

    /** Publishes one active student's final grade; answers the enrolment row it changed, if any. */
    public List<UUID> publishFinalGrade(UUID cohortId, UUID userId, String grade, BigDecimal gpa, OffsetDateTime now,
            UUID actor) {
        return jdbc.sql(PUBLISH_FINAL_GRADE).param("cohortId", cohortId).param("userId", userId).param("grade", grade)
                .param("gpa", gpa).param("now", now).param("actor", actor).query(UUID.class).list();
    }

    private static GradedItem item(ResultSet rs) throws SQLException {
        return new GradedItem(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("item_kind"),
                rs.getObject("grading_category_id", UUID.class), rs.getBigDecimal("points"),
                rs.getBigDecimal("weight_percent"), rs.getObject("due_at", OffsetDateTime.class),
                rs.getObject("created_at", OffsetDateTime.class));
    }
}
