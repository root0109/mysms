package com.mysms.learning.lms.repository;

import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.model.LessonDocument;
import com.mysms.learning.lms.domain.LessonEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
 * Access to lms.lesson. The editor documents are jsonb, written through {@code CAST(:x AS jsonb)} and read back as JSON
 * text.
 */
@Repository
public class LessonRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("chapterId", "r.chapter_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("includeInPreview", "r.include_in_preview", ValueType.BOOLEAN),
            field("youtubeVideoId", "r.youtube_video_id", ValueType.STRING),
            field("fileType", "r.file_type", ValueType.STRING),
            field("isScormPackage", "r.is_scorm_package", ValueType.BOOLEAN),
            field("version", "r.version", ValueType.INTEGER),
            field("supersededById", "r.superseded_by_id", ValueType.UUID),
            field("publishedAt", "r.published_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LessonEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LessonEntity::getCreatedAt),
            Map.entry("updateTime", LessonEntity::getUpdatedAt),
            Map.entry("sortOrder", LessonEntity::getSortOrder),
            Map.entry("title", LessonEntity::getTitle));

    private static final String COLUMNS = """
            r.id, r.tenant_id, r.chapter_id, r.title, r.sort_order, r.include_in_preview, r.body, r.content::text AS
                   content, r.teacher_notes, r.teacher_content::text AS teacher_content, r.youtube_video_id,
                   r.embedded_question, r.file_type, r.is_scorm_package, r.version, r.superseded_by_id,
                   r.published_at, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
            """;
    private static final String SELECT_LIVE = "SELECT " + COLUMNS + """
              FROM lms.lesson r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** The lessons of several chapters, in their order (one round trip for a whole outline). */
    private static final String OF_CHAPTERS = SELECT_LIVE
            + " AND r.chapter_id = ANY(:chapterIds) ORDER BY r.chapter_id, r.sort_order, r.id";
    /** The lessons of a course in reading order: its template chapters (no cohort), then each chapter's lessons. */
    private static final String IN_COURSE_ORDER = """
            SELECT r.id
              FROM lms.lesson r
              JOIN lms.chapter c ON c.tenant_id = :tenantId AND c.id = r.chapter_id AND c.deleted_at IS NULL
             WHERE r.tenant_id = :tenantId AND r.deleted_at IS NULL AND c.course_id = :courseId AND c.cohort_id IS NULL
             ORDER BY c.sort_order, c.id, r.sort_order, r.id
            """;
    private static final String INSERT = """
            INSERT INTO lms.lesson
            (id, tenant_id, chapter_id, title, sort_order, include_in_preview, body, content, teacher_notes,
             teacher_content, youtube_video_id, embedded_question, file_type, is_scorm_package, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :chapterId, :title, :sortOrder, COALESCE(:includeInPreview, false), :body,
                    CAST(:content AS jsonb), :teacherNotes, CAST(:teacherContent AS jsonb), :youtubeVideoId,
                    :embeddedQuestion, :fileType, COALESCE(:isScormPackage, false), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            """;
    private static final String UPDATE = """
            UPDATE lms.lesson
               SET chapter_id = :chapterId,
                   title = :title,
                   sort_order = :sortOrder,
                   include_in_preview = :includeInPreview,
                   body = :body,
                   content = CAST(:content AS jsonb),
                   teacher_notes = :teacherNotes,
                   teacher_content = CAST(:teacherContent AS jsonb),
                   youtube_video_id = :youtubeVideoId,
                   embedded_question = :embeddedQuestion,
                   file_type = :fileType,
                   is_scorm_package = :isScormPackage,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** lms.MoveLesson: the chapter and the place in it; the guard on the version was checked on the row as read. */
    private static final String MOVE = """
            UPDATE lms.lesson
               SET chapter_id = :chapterId, sort_order = :sortOrder, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** The current lessons of a chapter holding a place (uq_lesson_slot), in order, the moved one left out. */
    private static final String SLOTTED = """
            SELECT id FROM lms.lesson
             WHERE tenant_id = :tenantId AND chapter_id = :chapterId AND deleted_at IS NULL
               AND superseded_by_id IS NULL AND id <> :id
             ORDER BY sort_order, id
            """;
    /** Moves lessons far above any place first, since uq_lesson_slot is checked row by row while renumbering. */
    private static final String PARK = """
            UPDATE lms.lesson
               SET sort_order = sort_order + 1000000
             WHERE tenant_id = :tenantId AND id = ANY(:ids) AND deleted_at IS NULL
            """;
    private static final String PLACE = """
            UPDATE lms.lesson
               SET sort_order = :sortOrder, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.lesson
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CHAPTER = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String COURSE_OF_CHAPTER = """
            SELECT course_id FROM lms.chapter WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    /** The first table whose live rows still reference the lesson, or null (one round trip for the delete check). */
    private static final String FIRST_REFERENCE = """
            SELECT CASE
                     WHEN EXISTS (SELECT 1 FROM lms.content_item x WHERE x.tenant_id = :tenantId AND x.lesson_id = :id
                                     AND x.deleted_at IS NULL) THEN 'lms.content_item'
                     WHEN EXISTS (SELECT 1 FROM lms.lesson_activity x WHERE x.tenant_id = :tenantId
                                     AND x.lesson_id = :id AND x.deleted_at IS NULL) THEN 'lms.lesson_activity'
                     WHEN EXISTS (SELECT 1 FROM lms.lesson_progress x WHERE x.tenant_id = :tenantId
                                     AND x.lesson_id = :id AND x.deleted_at IS NULL) THEN 'lms.lesson_progress'
                     WHEN EXISTS (SELECT 1 FROM lms.quiz x WHERE x.tenant_id = :tenantId AND x.lesson_id = :id
                                     AND x.deleted_at IS NULL) THEN 'lms.quiz'
                     WHEN EXISTS (SELECT 1 FROM lms.assignment_submission x WHERE x.tenant_id = :tenantId
                                     AND x.lesson_id = :id AND x.deleted_at IS NULL) THEN 'lms.assignment_submission'
                     WHEN EXISTS (SELECT 1 FROM lms.timetable_entry x WHERE x.tenant_id = :tenantId
                                     AND x.lesson_id = :id AND x.deleted_at IS NULL) THEN 'lms.timetable_entry'
                     WHEN EXISTS (SELECT 1 FROM lms.lesson_note x WHERE x.tenant_id = :tenantId AND x.lesson_id = :id
                                     AND x.deleted_at IS NULL) THEN 'lms.lesson_note'
                   END
            """;
    /** Whether the lesson has a video the student has not watched at all yet (lms.MarkLessonComplete's video rule). */
    private static final String VIDEO_UNWATCHED = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson l WHERE l.tenant_id = :tenantId AND l.id = :id
                              AND l.youtube_video_id IS NOT NULL
                              AND NOT EXISTS (SELECT 1 FROM lms.video_watch w WHERE w.tenant_id = :tenantId
                                                 AND w.lesson_id = l.id AND w.user_id = :userId
                                                 AND w.deleted_at IS NULL AND w.watch_seconds > 0))
            """;
    /**
     * How many of the lesson's quizzes (student-facing activities) the student has no finished attempt of — with
     * {@code :passing}, no finished attempt at or above its pass mark (lms.MarkLessonComplete's quiz rules).
     */
    private static final String QUIZZES_OPEN = """
            SELECT count(*) FROM lms.lesson_activity a
             WHERE a.tenant_id = :tenantId AND a.lesson_id = :id AND a.deleted_at IS NULL AND a.quiz_id IS NOT NULL
               AND NOT a.teacher_only
               AND NOT EXISTS (SELECT 1 FROM lms.quiz_submission q WHERE q.tenant_id = :tenantId
                                  AND q.quiz_id = a.quiz_id AND q.user_id = :userId AND q.deleted_at IS NULL
                                  AND q.status IN ('submitted', 'graded')
                                  AND (NOT :passing OR q.percentage >= COALESCE(q.passing_percentage, 0)))
            """;
    private static final String INSTITUTION_OF_CHAPTER = """
            SELECT c.institution_id FROM lms.chapter p JOIN lms.course c ON c.tenant_id = :tenantId AND c.id = p.course_id
             WHERE p.tenant_id = :tenantId AND p.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.chapter_id IN (SELECT p1.id FROM lms.chapter p1 WHERE p1.tenant_id = :tenantId
                 AND p1.course_id IN (SELECT p2.id FROM lms.course p2 WHERE p2.tenant_id = :tenantId
                 AND (p2.institution_id = ANY(:scopeIds) OR p2.institution_id IS NULL))))
            """;

    private final JdbcClient jdbc;

    public LessonRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LessonEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LessonRepository::map).optional();
    }

    public Page<LessonEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LessonEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LessonRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LessonEntity::getId);
    }

    /** The live lessons of the chapters, ordered within each chapter. */
    public List<LessonEntity> ofChapters(List<UUID> chapterIds) {
        return jdbc.sql(OF_CHAPTERS).param("chapterIds", chapterIds.toArray(UUID[]::new))
                .query(LessonRepository::map).list();
    }

    /** The ids of the course's lessons in reading order. */
    public List<UUID> inCourseOrder(UUID courseId) {
        return jdbc.sql(IN_COURSE_ORDER).param("courseId", courseId).query(UUID.class).list();
    }

    public void insert(LessonEntity e) {
        params(jdbc.sql(INSERT), e)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LessonEntity e, int expectedVersion) {
        return params(jdbc.sql(UPDATE), e).param("id", e.getId()).param("expected", expectedVersion).update();
    }

    /**
     * Moves the lesson to a place in a chapter (at most its end) and renumbers the chapter from 0; 0 rows means it
     * changed since it was read, and the caller must roll back.
     */
    public int move(UUID id, int expected, UUID chapterId, int position, OffsetDateTime now, UUID actor) {
        List<UUID> order = new ArrayList<>(jdbc.sql(SLOTTED).param("chapterId", chapterId).param("id", id)
                .query(UUID.class).list());
        int place = Math.min(position, order.size());
        order.add(place, id);
        jdbc.sql(PARK).param("ids", order.toArray(UUID[]::new)).update();
        int moved = jdbc.sql(MOVE).param("id", id).param("expected", expected).param("chapterId", chapterId)
                .param("sortOrder", place).param("now", now).param("actor", actor).update();
        for (int i = 0; i < order.size(); i++) {
            if (i != place) {
                jdbc.sql(PLACE).param("id", order.get(i)).param("sortOrder", i).update();
            }
        }
        return moved;
    }

    public int softDelete(LessonEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public boolean existsChapter(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CHAPTER).param("id", id).query(Boolean.class).single());
    }

    /** The course of a live chapter. */
    public Optional<UUID> courseOfChapter(UUID chapterId) {
        return jdbc.sql(COURSE_OF_CHAPTER).param("id", chapterId).query(UUID.class).optional();
    }

    /** The first table of this module whose live rows reference the lesson. */
    public Optional<String> firstReference(UUID id) {
        return jdbc.sql(FIRST_REFERENCE).param("id", id).query((rs, row) -> Optional.ofNullable(rs.getString(1)))
                .single();
    }

    /** Whether the lesson has a video the student has not watched at all. */
    public boolean videoUnwatched(UUID id, UUID userId) {
        return Boolean.TRUE.equals(jdbc.sql(VIDEO_UNWATCHED).param("id", id).param("userId", userId)
                .query(Boolean.class).single());
    }

    /** How many of the lesson's quizzes the student has not finished (or, with {@code passing}, not passed). */
    public int quizzesOpen(UUID id, UUID userId, boolean passing) {
        return jdbc.sql(QUIZZES_OPEN).param("id", id).param("userId", userId).param("passing", passing)
                .query(Integer.class).single();
    }

    /** The institution owning lessons of the chapter (its course's; empty: tenant-level or none). */
    public Optional<UUID> institutionOfChapter(UUID chapterId) {
        return jdbc.sql(INSTITUTION_OF_CHAPTER).param("ref", chapterId)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static JdbcClient.StatementSpec params(JdbcClient.StatementSpec spec, LessonEntity e) {
        return spec.param("chapterId", e.getChapterId())
                .param("title", e.getTitle())
                .param("sortOrder", e.getSortOrder())
                .param("includeInPreview", e.getIncludeInPreview())
                .param("body", e.getBody())
                .param("content", json(e.getContent()))
                .param("teacherNotes", e.getTeacherNotes())
                .param("teacherContent", json(e.getTeacherContent()))
                .param("youtubeVideoId", e.getYoutubeVideoId())
                .param("embeddedQuestion", e.getEmbeddedQuestion())
                .param("fileType", e.getFileType())
                .param("isScormPackage", e.getIsScormPackage())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy());
    }

    private static String json(LessonDocument document) {
        return document == null ? null : document.json();
    }

    private static List<Object> sortValues(SortSpec sort, LessonEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LessonEntity map(ResultSet rs, int row) throws SQLException {
        String content = rs.getString("content");
        String teacherContent = rs.getString("teacher_content");
        return LessonEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .chapterId(rs.getObject("chapter_id", UUID.class))
                .title(rs.getString("title"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .includeInPreview(rs.getObject("include_in_preview", Boolean.class))
                .body(rs.getString("body"))
                .content(content == null ? null : new LessonDocument(content))
                .teacherNotes(rs.getString("teacher_notes"))
                .teacherContent(teacherContent == null ? null : new LessonDocument(teacherContent))
                .youtubeVideoId(rs.getString("youtube_video_id"))
                .embeddedQuestion(rs.getString("embedded_question"))
                .fileType(rs.getString("file_type"))
                .isScormPackage(rs.getObject("is_scorm_package", Boolean.class))
                .version(rs.getObject("version", Integer.class))
                .supersededById(rs.getObject("superseded_by_id", UUID.class))
                .publishedAt(rs.getObject("published_at", OffsetDateTime.class))
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
