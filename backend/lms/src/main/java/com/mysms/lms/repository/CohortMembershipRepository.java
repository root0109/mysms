package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.model.Placement;
import com.mysms.lms.model.RosterRow;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to a cohort's members — its students (lms.cohort_enrollment) and staff
 * (lms.cohort_teacher) read together as the roster, and the statements the enrolment workflow changes them with. A
 * member who leaves keeps their row (inactive, with the time and reason), so their grades and history stay; coming
 * back reactivates it. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CohortMembershipRepository {

    /** The roster's order: when members were placed, then the row id. */
    public static final Map<String, FieldSpec> ROSTER_SORT = Map.of("createTime",
            new FieldSpec("createTime", "r.created_at", ValueType.TIMESTAMP));

    // the roster reads students and staff separately (no compound statement) and merges them
    private static final String ROSTER_STUDENTS = """
            SELECT r.id, r.user_id, 'student' AS role_code, r.is_active, r.created_at
              FROM lms.cohort_enrollment r
             WHERE r.tenant_id = :tenantId AND r.cohort_id = :cohortId AND r.deleted_at IS NULL
               AND (:includeInactive OR r.is_active)
            """;
    private static final String ROSTER_STAFF = """
            SELECT r.id, r.user_id, 'teacher' AS role_code, r.is_active, r.created_at
              FROM lms.cohort_teacher r
             WHERE r.tenant_id = :tenantId AND r.cohort_id = :cohortId AND r.deleted_at IS NULL
               AND (:includeInactive OR r.is_active)
            """;
    private static final String ACTIVE_STUDENTS = """
            SELECT count(*) FROM lms.cohort_enrollment
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND is_active AND deleted_at IS NULL
            """;
    private static final String ACTIVE_STUDENT_IDS = """
            SELECT user_id FROM lms.cohort_enrollment
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND is_active AND deleted_at IS NULL
             ORDER BY created_at, id
            """;
    private static final String ENROLLMENT_OF = """
            SELECT id, is_active, row_version FROM lms.cohort_enrollment
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND user_id = :userId AND deleted_at IS NULL
            """;
    private static final String TEACHER_OF = """
            SELECT id, is_active, row_version FROM lms.cohort_teacher
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND user_id = :userId AND deleted_at IS NULL
            """;
    private static final String REACTIVATE_ENROLLMENT = """
            UPDATE lms.cohort_enrollment
               SET is_active = true, removed_at = NULL, removal_reason = NULL,
                   payment_id = COALESCE(:paymentId, payment_id), is_self_enrolled = :selfEnrolled,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REMOVE_ENROLLMENT = """
            UPDATE lms.cohort_enrollment
               SET is_active = false, removed_at = :now, removal_reason = 'manual',
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SET_TEACHER = """
            UPDATE lms.cohort_teacher
               SET is_active = true, removed_at = NULL, role_id = :roleId, role_label = :roleLabel,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REMOVE_TEACHER = """
            UPDATE lms.cohort_teacher
               SET is_active = false, removed_at = :now,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String COURSES_OF = """
            SELECT course_id FROM lms.cohort_course
             WHERE tenant_id = :tenantId AND cohort_id = :cohortId AND deleted_at IS NULL ORDER BY created_at, id
            """;
    private static final String ENROL_IN_COURSE = """
            INSERT INTO lms.enrollment (id, tenant_id, course_id, user_id, member_type, member_role, source_cohort_id,
                                        payment_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :courseId, :userId, 'student', 'member', :cohortId, :paymentId, :now, :actor, :now,
                    :actor, 1)
            ON CONFLICT (course_id, user_id) WHERE (deleted_at IS NULL) DO NOTHING
            """;

    private final JdbcClient jdbc;

    public CohortMembershipRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** A page of the roster: students and staff, optionally one role only, inactive members only when asked. */
    public Page<RosterRow> roster(PageRequest page, UUID cohortId, String roleCode, boolean includeInactive) {
        long skip = ((Number) page.limitParams().get("offset")).longValue();
        long reach = skip + page.size() + 1;
        List<RosterRow> rows = new ArrayList<>();
        if (roleCode == null || RosterRow.STUDENT.equals(roleCode)) {
            rows.addAll(rosterPart(ROSTER_STUDENTS, page, cohortId, includeInactive, reach));
        }
        if (roleCode == null || RosterRow.TEACHER.equals(roleCode)) {
            rows.addAll(rosterPart(ROSTER_STAFF, page, cohortId, includeInactive, reach));
        }
        // PostgreSQL orders uuids byte by byte, as their lower-case text sorts
        Comparator<RosterRow> order = Comparator.comparing(RosterRow::createdAt)
                .thenComparing(row -> row.id().toString());
        List<RosterRow> merged = rows.stream().sorted(page.sort().allDescending() ? order.reversed() : order)
                .skip(skip).limit(page.size() + 1L).toList();
        return page.toPage(merged, row -> List.of(row.createdAt()), RosterRow::id);
    }

    /** One side of the roster after the cursor, in the page's order, at most {@code reach} rows. */
    private List<RosterRow> rosterPart(String select, PageRequest page, UUID cohortId, boolean includeInactive,
            long reach) {
        SqlFragment keyset = page.keysetPredicate();
        String sql = select + " AND (" + keyset.sql() + ") ORDER BY " + page.orderByClause() + " LIMIT :reach";
        return jdbc.sql(sql).param("cohortId", cohortId).param("includeInactive", includeInactive)
                .params(keyset.params()).param("reach", reach)
                .query((rs, n) -> new RosterRow(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getString("role_code"), rs.getBoolean("is_active"),
                        rs.getObject("created_at", OffsetDateTime.class)))
                .list();
    }

    /** The cohort's active students now (the seats taken). */
    public int activeStudents(UUID cohortId) {
        return jdbc.sql(ACTIVE_STUDENTS).param("cohortId", cohortId).query(Integer.class).single();
    }

    /** The cohort's active students, in the order they joined. */
    public List<UUID> activeStudentIds(UUID cohortId) {
        return jdbc.sql(ACTIVE_STUDENT_IDS).param("cohortId", cohortId).query(UUID.class).list();
    }

    /** The user's live enrolment row in the cohort, active or not. */
    public Optional<Placement> enrollmentOf(UUID cohortId, UUID userId) {
        return placement(ENROLLMENT_OF, cohortId, userId);
    }

    /** The user's live staff row in the cohort, active or not. */
    public Optional<Placement> teacherOf(UUID cohortId, UUID userId) {
        return placement(TEACHER_OF, cohortId, userId);
    }

    /** A student who left comes back (keeping their row, so their grades stay with it). */
    public int reactivateEnrollment(Placement row, UUID paymentId, boolean selfEnrolled, OffsetDateTime now,
            UUID actor) {
        return guarded(REACTIVATE_ENROLLMENT, row, now, actor).param("paymentId", paymentId)
                .param("selfEnrolled", selfEnrolled).update();
    }

    /** A student leaves the cohort (by hand); the row stays, inactive, with the time and reason. */
    public int removeEnrollment(Placement row, OffsetDateTime now, UUID actor) {
        return guarded(REMOVE_ENROLLMENT, row, now, actor).update();
    }

    /** A staff member is (again) active, with the role given. */
    public int setTeacher(Placement row, UUID roleId, String roleLabel, OffsetDateTime now, UUID actor) {
        return guarded(SET_TEACHER, row, now, actor).param("roleId", roleId).param("roleLabel", roleLabel).update();
    }

    /** A staff member leaves the cohort; the row stays, inactive. */
    public int removeTeacher(Placement row, OffsetDateTime now, UUID actor) {
        return guarded(REMOVE_TEACHER, row, now, actor).update();
    }

    /** The courses the cohort teaches. */
    public List<UUID> coursesOf(UUID cohortId) {
        return jdbc.sql(COURSES_OF).param("cohortId", cohortId).query(UUID.class).list();
    }

    /** Enrols the student in one course of the cohort (lms.enrollment, sourced from the cohort) unless already in it. */
    public void enrolInCourse(UUID id, UUID cohortId, UUID courseId, UUID userId, UUID paymentId, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(ENROL_IN_COURSE).param("id", id).param("cohortId", cohortId).param("courseId", courseId)
                .param("userId", userId).param("paymentId", paymentId).param("now", now).param("actor", actor)
                .update();
    }

    private Optional<Placement> placement(String sql, UUID cohortId, UUID userId) {
        return jdbc.sql(sql).param("cohortId", cohortId).param("userId", userId)
                .query((rs, n) -> new Placement(rs.getObject("id", UUID.class), rs.getBoolean("is_active"),
                        rs.getInt("row_version")))
                .optional();
    }

    private JdbcClient.StatementSpec guarded(String sql, Placement row, OffsetDateTime now, UUID actor) {
        return jdbc.sql(sql).param("id", row.id()).param("expected", row.rowVersion()).param("now", now)
                .param("actor", actor);
    }
}
