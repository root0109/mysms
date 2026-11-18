package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.LessonNoteEntity;
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
 * Access to lms.lesson_note. Every statement names the writer next to the tenant, so another user's note is never
 * reached.
 */
@Repository
public class LessonNoteRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("color", "r.color", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LessonNoteEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LessonNoteEntity::getCreatedAt),
            Map.entry("updateTime", LessonNoteEntity::getUpdatedAt));

    private static final String SELECT_OWN = """
            SELECT r.id, r.tenant_id, r.lesson_id, r.user_id, r.color, r.highlighted_text, r.note, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.lesson_note r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
               AND r.user_id = :userId
               AND r.lesson_id = :lessonId
            """;
    private static final String FIND = SELECT_OWN + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.lesson_note
            (id, tenant_id, lesson_id, user_id, color, highlighted_text, note, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :lessonId, :userId, COALESCE(:color, 'yellow'), :highlightedText, :note, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, lesson_id, user_id, color, highlighted_text, note, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String UPDATE = """
            UPDATE lms.lesson_note
               SET color = :color, highlighted_text = :highlightedText, note = :note, updated_at = :updatedAt,
                   updated_by = :updatedBy, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND user_id = :userId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.lesson_note
               SET deleted_at = :deletedAt, updated_at = :updatedAt, updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND user_id = :userId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public LessonNoteRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The writer's live note on the lesson. */
    public Optional<LessonNoteEntity> findOwn(UUID userId, UUID lessonId, UUID id) {
        return jdbc.sql(FIND).param("userId", userId).param("lessonId", lessonId).param("id", id)
                .query(LessonNoteRepository::map).optional();
    }

    /** The writer's notes on the lesson, page by page. */
    public Page<LessonNoteEntity> listOwn(UUID userId, UUID lessonId, PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_OWN + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LessonNoteEntity> rows = jdbc.sql(sql).param("userId", userId).param("lessonId", lessonId)
                .params(where.params()).params(page.limitParams()).query(LessonNoteRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LessonNoteEntity::getId);
    }

    public LessonNoteEntity insert(LessonNoteEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("lessonId", e.getLessonId())
                .param("userId", e.getUserId())
                .param("color", e.getColor())
                .param("highlightedText", e.getHighlightedText())
                .param("note", e.getNote())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LessonNoteRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LessonNoteEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("color", e.getColor())
                .param("highlightedText", e.getHighlightedText())
                .param("note", e.getNote())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LessonNoteEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    private static List<Object> sortValues(SortSpec sort, LessonNoteEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LessonNoteEntity map(ResultSet rs, int row) throws SQLException {
        return LessonNoteEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .lessonId(rs.getObject("lesson_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .color(rs.getString("color"))
                .highlightedText(rs.getString("highlighted_text"))
                .note(rs.getString("note"))
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
