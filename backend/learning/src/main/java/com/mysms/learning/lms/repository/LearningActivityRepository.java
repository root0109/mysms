package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The read models of learning activity that have no table of their own: the public catalogue's statistics and a
 * student's active days. Aggregates over this module's own tables, tenant-scoped like every read.
 */
@Repository
public class LearningActivityRepository {

    /** The public statistics of the tenant's catalogue (lms.GetLmsStatistics). */
    public record Statistics(int publishedCourses, int students, int enrollments, int certificatesIssued,
                             BigDecimal completionPercent) {
    }

    /** How many things a student did on one day (UTC). */
    public record Day(LocalDate date, int count) {
    }

    private static final String STATISTICS = """
            SELECT (SELECT count(*) FROM lms.course c WHERE c.tenant_id = :tenantId AND c.deleted_at IS NULL
                       AND c.is_published AND c.lifecycle_status = 'published') AS published_courses,
                   (SELECT count(DISTINCT e.user_id) FROM lms.enrollment e WHERE e.tenant_id = :tenantId
                       AND e.deleted_at IS NULL AND e.member_type = 'student') AS students,
                   (SELECT count(*) FROM lms.enrollment e WHERE e.tenant_id = :tenantId AND e.deleted_at IS NULL
                       AND e.member_type = 'student') AS enrollments,
                   (SELECT count(*) FROM lms.certificate x WHERE x.tenant_id = :tenantId
                       AND x.deleted_at IS NULL) AS certificates,
                   (SELECT COALESCE(ROUND(100.0 * count(*) FILTER (WHERE e.progress_percent >= 100)
                                          / NULLIF(count(*), 0), 2), 0)
                      FROM lms.enrollment e WHERE e.tenant_id = :tenantId AND e.deleted_at IS NULL
                       AND e.member_type = 'student') AS completion
            """;
    /**
     * The days a student completed or touched a lesson or SCORM chapter, or watched a lesson video, since a date,
     * with how many such records changed that day.
     */
    private static final String PROGRESS_DAYS = """
            SELECT (p.updated_at AT TIME ZONE 'UTC')::date AS day, count(*) AS n FROM lms.lesson_progress p
             WHERE p.tenant_id = :tenantId AND p.user_id = :userId AND p.deleted_at IS NULL AND p.updated_at >= :since
             GROUP BY 1
            """;
    private static final String WATCH_DAYS = """
            SELECT (w.updated_at AT TIME ZONE 'UTC')::date AS day, count(*) AS n FROM lms.video_watch w
             WHERE w.tenant_id = :tenantId AND w.user_id = :userId AND w.deleted_at IS NULL AND w.updated_at >= :since
             GROUP BY 1
            """;

    /**
     * The students of a course per progress range: not started (0), the four quarters, and complete (100) — every
     * range listed, empty ones with 0, in order.
     */
    private static final String PROGRESS_BUCKETS = """
            SELECT b.label, count(e.id) AS students
              FROM (VALUES (1, '0%', 0.00, 0.00), (2, '1-25%', 0.01, 25.00), (3, '26-50%', 25.01, 50.00),
                           (4, '51-75%', 50.01, 75.00), (5, '76-99%', 75.01, 99.99), (6, '100%', 100.00, 100.00))
                   AS b(n, label, low, high)
              LEFT JOIN lms.enrollment e ON e.tenant_id = :tenantId AND e.course_id = :courseId AND e.deleted_at IS NULL
                   AND e.member_type = 'student' AND e.progress_percent BETWEEN b.low AND b.high
             GROUP BY b.n, b.label
             ORDER BY b.n
            """;

    /** Students of one progress range. */
    public record Bucket(String label, int students) {
    }

    private final JdbcClient jdbc;

    public LearningActivityRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Statistics statistics() {
        return jdbc.sql(STATISTICS).query((rs, row) -> new Statistics(rs.getInt("published_courses"),
                rs.getInt("students"), rs.getInt("enrollments"), rs.getInt("certificates"),
                rs.getBigDecimal("completion"))).single();
    }

    /** The course's students per progress range, every range in order. */
    public List<Bucket> progressBuckets(UUID courseId) {
        return jdbc.sql(PROGRESS_BUCKETS).param("courseId", courseId)
                .query((rs, row) -> new Bucket(rs.getString("label"), rs.getInt("students"))).list();
    }

    /** The student's active days since the date (inclusive, UTC), in date order. */
    public List<Day> activeDays(UUID userId, LocalDate since) {
        OffsetDateTime from = since.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        Map<LocalDate, Integer> days = new TreeMap<>();
        for (String sql : List.of(PROGRESS_DAYS, WATCH_DAYS)) {
            jdbc.sql(sql).param("userId", userId).param("since", from)
                    .query((rs, row) -> new Day(rs.getObject("day", LocalDate.class), rs.getInt("n"))).list()
                    .forEach(day -> days.merge(day.date(), day.count(), Integer::sum));
        }
        return days.entrySet().stream().map(day -> new Day(day.getKey(), day.getValue())).toList();
    }

    /** One upcoming calendar entry of a teacher's cohorts (a due item, a cohort session or a live class). */
    public record Upcoming(UUID id, String title, OffsetDateTime startAt,
                           OffsetDateTime endAt, UUID cohortId, UUID contentItemId) {
    }

    /** Unread posts of one cohort. */
    public record CohortUnread(UUID cohortId, int posts) {
    }

    private static final String TEACHING_COHORTS = """
            SELECT DISTINCT t.cohort_id FROM lms.cohort_teacher t
             WHERE t.tenant_id = :tenantId AND t.deleted_at IS NULL AND t.user_id = :userId
            """;
    private static final String ENROLLED_COHORTS = """
            SELECT DISTINCT m.cohort_id FROM lms.cohort_enrollment m
             WHERE m.tenant_id = :tenantId AND m.deleted_at IS NULL AND m.user_id = :userId
            """;
    /**
     * Work in the cohorts waiting for a teacher: handed-in assignment submissions not graded yet, and quiz attempts
     * submitted with answers still to mark.
     */
    private static final String TO_GRADE = """
            SELECT (SELECT count(*) FROM lms.assignment_submission a
                      JOIN lms.content_item i ON i.tenant_id = :tenantId AND i.id = a.content_item_id
                           AND i.deleted_at IS NULL
                     WHERE a.tenant_id = :tenantId AND a.deleted_at IS NULL AND a.finalized_at IS NOT NULL
                       AND a.status = 'not_graded' AND i.cohort_id = ANY(:cohortIds))
                 + (SELECT count(*) FROM lms.quiz_submission q
                     WHERE q.tenant_id = :tenantId AND q.deleted_at IS NULL AND q.status = 'submitted'
                       AND q.cohort_id = ANY(:cohortIds))
            """;
    /**
     * Posts in the cohorts' forums the user has not read — neither marked read one by one nor older than a topic-wide
     * read mark (MarkTopicRead) — drafts, hidden posts and the user's own aside.
     */
    private static final String UNREAD_POSTS = """
            SELECT f.cohort_id, count(*) AS posts
              FROM lms.forum_post p
              JOIN lms.forum_topic t ON t.tenant_id = :tenantId AND t.id = p.forum_topic_id AND t.deleted_at IS NULL
              JOIN lms.forum_section s ON s.tenant_id = :tenantId AND s.id = t.forum_section_id
                   AND s.deleted_at IS NULL
              JOIN lms.forum f ON f.tenant_id = :tenantId AND f.id = s.forum_id AND f.deleted_at IS NULL
             WHERE p.tenant_id = :tenantId AND p.deleted_at IS NULL AND NOT p.is_draft AND NOT p.is_hidden
               AND NOT p.is_hidden_by_parent
               AND p.author_user_id <> :userId AND f.cohort_id = ANY(:cohortIds)
               AND NOT EXISTS (SELECT 1 FROM lms.forum_read_state r
                                WHERE r.tenant_id = :tenantId AND r.user_id = :userId AND r.forum_topic_id = t.id
                                  AND r.deleted_at IS NULL
                                  AND (r.forum_post_id = p.id
                                       OR r.forum_post_id IS NULL AND r.read_at >= p.posted_at))
             GROUP BY f.cohort_id
            """;
    private static final String UPCOMING_DUE = """
            SELECT i.id, COALESCE(i.title, '') AS title, i.due_at AS start_at, i.due_at AS end_at, i.cohort_id,
                   i.id AS content_item_id
              FROM lms.content_item i
             WHERE i.tenant_id = :tenantId AND i.deleted_at IS NULL AND i.cohort_id = ANY(:cohortIds)
               AND i.due_at >= :fromTime
             ORDER BY i.due_at
             LIMIT :top
            """;
    private static final String UPCOMING_SESSIONS = """
            SELECT s.id, COALESCE(s.description, 'Session') AS title, s.starts_at AS start_at, s.ends_at AS end_at,
                   s.cohort_id, NULL::uuid AS content_item_id
              FROM lms.cohort_session s
             WHERE s.tenant_id = :tenantId AND s.deleted_at IS NULL AND NOT s.is_cancelled
               AND s.cohort_id = ANY(:cohortIds) AND s.starts_at >= :fromTime
             ORDER BY s.starts_at
             LIMIT :top
            """;
    private static final String UPCOMING_LIVE_CLASSES = """
            SELECT c.id, c.title, (c.class_date + c.start_time) AT TIME ZONE c.timezone AS start_at,
                   (c.class_date + c.start_time) AT TIME ZONE c.timezone + make_interval(mins => c.duration_minutes)
                       AS end_at,
                   c.cohort_id, NULL::uuid AS content_item_id
              FROM lms.live_class c
             WHERE c.tenant_id = :tenantId AND c.deleted_at IS NULL AND c.cohort_id = ANY(:cohortIds)
               AND (c.class_date + c.start_time) AT TIME ZONE c.timezone >= :fromTime
             ORDER BY start_at
             LIMIT :top
            """;

    /** The cohorts the user teaches. */
    public List<UUID> teachingCohorts(UUID userId) {
        return jdbc.sql(TEACHING_COHORTS).param("userId", userId).query(UUID.class).list();
    }

    /** The cohorts the user belongs to, as a student or a teacher. */
    public List<UUID> memberCohorts(UUID userId) {
        Set<UUID> cohorts = new LinkedHashSet<>(jdbc.sql(ENROLLED_COHORTS).param("userId", userId).query(UUID.class)
                .list());
        cohorts.addAll(teachingCohorts(userId));
        return List.copyOf(cohorts);
    }

    /** Work waiting for a teacher across the cohorts (see TO_GRADE). */
    public int toGrade(List<UUID> cohortIds) {
        if (cohortIds.isEmpty()) {
            return 0;
        }
        return jdbc.sql(TO_GRADE).param("cohortIds", cohortIds.toArray(UUID[]::new)).query(Integer.class).single();
    }

    /** The user's unread forum posts per cohort. */
    public List<CohortUnread> unreadPosts(UUID userId, List<UUID> cohortIds) {
        if (cohortIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(UNREAD_POSTS).param("userId", userId).param("cohortIds", cohortIds.toArray(UUID[]::new))
                .query((rs, row) -> new CohortUnread(rs.getObject("cohort_id", UUID.class), rs.getInt("posts")))
                .list();
    }

    /** The next due items of the cohorts, soonest first. */
    public List<Upcoming> upcomingDue(List<UUID> cohortIds, OffsetDateTime from, int top) {
        return upcoming(UPCOMING_DUE, cohortIds, from, top);
    }

    /** The next cohort sessions and live classes of the cohorts, soonest first. */
    public List<Upcoming> upcomingSessions(List<UUID> cohortIds, OffsetDateTime from, int top) {
        List<Upcoming> both = new ArrayList<>(upcoming(UPCOMING_SESSIONS, cohortIds, from, top));
        both.addAll(upcoming(UPCOMING_LIVE_CLASSES, cohortIds, from, top));
        return both.stream().sorted(Comparator.comparing(Upcoming::startAt)).limit(top).toList();
    }

    private List<Upcoming> upcoming(String sql, List<UUID> cohortIds, OffsetDateTime from, int top) {
        if (cohortIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(sql).param("cohortIds", cohortIds.toArray(UUID[]::new)).param("fromTime", from).param("top", top)
                .query((rs, row) -> new Upcoming(rs.getObject("id", UUID.class), rs.getString("title"),
                        rs.getObject("start_at", OffsetDateTime.class),
                        rs.getObject("end_at", OffsetDateTime.class),
                        rs.getObject("cohort_id", UUID.class), rs.getObject("content_item_id", UUID.class)))
                .list();
    }
}
