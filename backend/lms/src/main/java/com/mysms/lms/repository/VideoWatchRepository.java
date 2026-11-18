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
import com.mysms.lms.domain.VideoWatchEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Plain-JDBC access to lms.video_watch: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class VideoWatchRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("lessonId", "r.lesson_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("videoSource", "r.video_source", ValueType.STRING),
            field("watchSeconds", "r.watch_seconds", ValueType.DECIMAL),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<VideoWatchEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", VideoWatchEntity::getCreatedAt),
            Map.entry("updateTime", VideoWatchEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.lesson_id, r.user_id, r.video_source, r.watch_seconds, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.video_watch r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /**
     * Records watch time: one row per lesson, student and video (uq_video_watch_1 decides between concurrent first
     * calls); the time kept is the longest reported, so a replayed or out-of-order report never lowers it.
     */
    private static final String UPSERT = """
            INSERT INTO lms.video_watch
            (id, tenant_id, lesson_id, user_id, video_source, watch_seconds, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :lessonId, :userId, :videoSource, :watchSeconds, :now, :actor, :now, :actor, 1)
            ON CONFLICT (lesson_id, user_id, video_source) WHERE deleted_at IS NULL
            DO UPDATE SET watch_seconds = GREATEST(lms.video_watch.watch_seconds, EXCLUDED.watch_seconds),
                          updated_at = :now, updated_by = :actor, row_version = lms.video_watch.row_version + 1
             WHERE lms.video_watch.tenant_id = :tenantId
            RETURNING id, tenant_id, lesson_id, user_id, video_source, watch_seconds, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String INSERT = """
            INSERT INTO lms.video_watch
            (id, tenant_id, lesson_id, user_id, video_source, watch_seconds, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :lessonId, :userId, :videoSource, :watchSeconds, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, lesson_id, user_id, video_source, watch_seconds, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.video_watch
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.video_watch
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_VIDEO_WATCH_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.video_watch
                            WHERE tenant_id = :tenantId
                              AND lesson_id = :lessonId AND user_id = :userId AND video_source = :videoSource AND id <>
                                  :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_LESSON_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.lesson WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p2.institution_id FROM lms.lesson p0 JOIN lms.chapter p1 ON p1.tenant_id = :tenantId AND p1.id =
                p0.chapter_id JOIN lms.course p2
            ON p2.tenant_id = :tenantId AND p2.id = p1.course_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.lesson_id IN (SELECT p0.id FROM lms.lesson p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.chapter_id IN (SELECT p1.id FROM
             lms.chapter p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.course_id IN (SELECT p2.id FROM lms.course p2 WHERE p2.tenant_id = :tenantId
                 AND (p2.institution_id =
             ANY(:scopeIds) OR p2.institution_id IS NULL)))))))
            """;

    private final JdbcClient jdbc;

    public VideoWatchRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<VideoWatchEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(VideoWatchRepository::map).optional();
    }

    public Page<VideoWatchEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<VideoWatchEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(VideoWatchRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), VideoWatchEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public VideoWatchEntity insert(VideoWatchEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("lessonId", e.getLessonId())
                .param("userId", e.getUserId())
                .param("videoSource", e.getVideoSource())
                .param("watchSeconds", e.getWatchSeconds())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(VideoWatchRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(VideoWatchEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(VideoWatchEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_video_watch_1: lesson_id, user_id, video_source where (deleted_at IS NULL). */
    public boolean takenLessonIdUserIdVideoSource1(VideoWatchEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_VIDEO_WATCH_1_TAKEN)
                .param("lessonId", e.getLessonId())
                .param("userId", e.getUserId())
                .param("videoSource", e.getVideoSource())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.lesson row exists (live). */
    public boolean existsLessonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LESSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through lesson_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Records the watch time (see {@link #UPSERT}) and returns the row as stored. */
    public VideoWatchEntity upsert(VideoWatchEntity e, OffsetDateTime now, UUID actor) {
        return jdbc.sql(UPSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("lessonId", e.getLessonId())
                .param("userId", e.getUserId())
                .param("videoSource", e.getVideoSource())
                .param("watchSeconds", e.getWatchSeconds())
                .param("now", now)
                .param("actor", actor)
                .query(VideoWatchRepository::map).single();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.lesson_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, VideoWatchEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static VideoWatchEntity map(ResultSet rs, int row) throws SQLException {
        return VideoWatchEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .lessonId(rs.getObject("lesson_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .videoSource(rs.getString("video_source"))
                .watchSeconds(rs.getBigDecimal("watch_seconds"))
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
