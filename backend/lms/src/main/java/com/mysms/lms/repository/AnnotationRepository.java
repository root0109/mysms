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
import com.mysms.lms.domain.AnnotationEntity;
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
 * Plain-JDBC access to lms.annotation (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class AnnotationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("contentItemId", "r.content_item_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("ebookId", "r.ebook_id", ValueType.UUID),
            field("fileId", "r.file_id", ValueType.UUID),
            field("annotationType", "r.annotation_type", ValueType.STRING),
            field("annotationSubtype", "r.annotation_subtype", ValueType.STRING),
            field("pageNumber", "r.page_number", ValueType.STRING),
            field("startCharPos", "r.start_char_pos", ValueType.STRING),
            field("charCount", "r.char_count", ValueType.STRING),
            field("location", "r.location", ValueType.STRING),
            field("annotatedText", "r.annotated_text", ValueType.STRING),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("embedCode", "r.embed_code", ValueType.STRING),
            field("url", "r.url", ValueType.STRING),
            field("mediaFileId", "r.media_file_id", ValueType.UUID),
            field("visibility", "r.visibility", ValueType.STRING),
            field("forumTopicId", "r.forum_topic_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AnnotationEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AnnotationEntity::getCreatedAt),
            Map.entry("updateTime", AnnotationEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_id, r.content_item_id, r.cohort_id, r.ebook_id, r.file_id,
                   r.annotation_type, r.annotation_subtype, r.page_number, r.start_char_pos, r.char_count, r.location,
                   r.annotated_text, r.title, r.description, r.embed_code, r.url, r.media_file_id, r.visibility,
                   r.forum_topic_id, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM lms.annotation r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.annotation
            (id, tenant_id, user_id, content_item_id, cohort_id, ebook_id, file_id, annotation_type,
             annotation_subtype, page_number, start_char_pos, char_count, location, annotated_text, title,
             description, embed_code, url, media_file_id, visibility, forum_topic_id, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :contentItemId, :cohortId, :ebookId, :fileId, :annotationType,
                    :annotationSubtype, :pageNumber, :startCharPos, :charCount, :location, :annotatedText, :title,
                    :description, :embedCode, :url, :mediaFileId,
                    COALESCE(:visibility, 'private'::character varying), :forumTopicId, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_id, content_item_id, cohort_id, ebook_id, file_id, annotation_type,
                      annotation_subtype, page_number, start_char_pos, char_count, location, annotated_text, title,
                      description, embed_code, url, media_file_id, visibility, forum_topic_id, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.annotation
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.annotation
               SET user_id = :userId,
                   content_item_id = :contentItemId,
                   cohort_id = :cohortId,
                   ebook_id = :ebookId,
                   file_id = :fileId,
                   annotation_type = :annotationType,
                   annotation_subtype = :annotationSubtype,
                   page_number = :pageNumber,
                   start_char_pos = :startCharPos,
                   char_count = :charCount,
                   location = :location,
                   annotated_text = :annotatedText,
                   title = :title,
                   description = :description,
                   embed_code = :embedCode,
                   url = :url,
                   media_file_id = :mediaFileId,
                   visibility = :visibility,
                   forum_topic_id = :forumTopicId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_EBOOK_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.ebook WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_FORUM_TOPIC_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_topic WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ANNOTATION_SHARE_ANNOTATION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.annotation_share WHERE tenant_id = :tenantId
                AND annotation_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.cohort_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public AnnotationRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AnnotationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AnnotationRepository::map).optional();
    }

    public Page<AnnotationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AnnotationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AnnotationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AnnotationEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AnnotationEntity insert(AnnotationEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("contentItemId", e.getContentItemId())
                .param("cohortId", e.getCohortId())
                .param("ebookId", e.getEbookId())
                .param("fileId", e.getFileId())
                .param("annotationType", e.getAnnotationType())
                .param("annotationSubtype", e.getAnnotationSubtype())
                .param("pageNumber", e.getPageNumber())
                .param("startCharPos", e.getStartCharPos())
                .param("charCount", e.getCharCount())
                .param("location", e.getLocation())
                .param("annotatedText", e.getAnnotatedText())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("embedCode", e.getEmbedCode())
                .param("url", e.getUrl())
                .param("mediaFileId", e.getMediaFileId())
                .param("visibility", e.getVisibility())
                .param("forumTopicId", e.getForumTopicId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AnnotationRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AnnotationEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("contentItemId", e.getContentItemId())
                .param("cohortId", e.getCohortId())
                .param("ebookId", e.getEbookId())
                .param("fileId", e.getFileId())
                .param("annotationType", e.getAnnotationType())
                .param("annotationSubtype", e.getAnnotationSubtype())
                .param("pageNumber", e.getPageNumber())
                .param("startCharPos", e.getStartCharPos())
                .param("charCount", e.getCharCount())
                .param("location", e.getLocation())
                .param("annotatedText", e.getAnnotatedText())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("embedCode", e.getEmbedCode())
                .param("url", e.getUrl())
                .param("mediaFileId", e.getMediaFileId())
                .param("visibility", e.getVisibility())
                .param("forumTopicId", e.getForumTopicId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AnnotationEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONTENT_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.ebook row exists (live). */
    public boolean existsEbookId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EBOOK_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.forum_topic row exists (live). */
    public boolean existsForumTopicId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FORUM_TOPIC_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.annotation_share rows still reference the row. */
    public boolean referencedByAnnotationShareAnnotationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ANNOTATION_SHARE_ANNOTATION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through cohort_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, AnnotationEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AnnotationEntity map(ResultSet rs, int row) throws SQLException {
        return AnnotationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .contentItemId(rs.getObject("content_item_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .ebookId(rs.getObject("ebook_id", UUID.class))
                .fileId(rs.getObject("file_id", UUID.class))
                .annotationType(rs.getString("annotation_type"))
                .annotationSubtype(rs.getString("annotation_subtype"))
                .pageNumber(rs.getString("page_number"))
                .startCharPos(rs.getString("start_char_pos"))
                .charCount(rs.getString("char_count"))
                .location(rs.getString("location"))
                .annotatedText(rs.getString("annotated_text"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .embedCode(rs.getString("embed_code"))
                .url(rs.getString("url"))
                .mediaFileId(rs.getObject("media_file_id", UUID.class))
                .visibility(rs.getString("visibility"))
                .forumTopicId(rs.getObject("forum_topic_id", UUID.class))
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
