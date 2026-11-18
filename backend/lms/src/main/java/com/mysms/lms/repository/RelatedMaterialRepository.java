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
import com.mysms.lms.domain.RelatedMaterialEntity;
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
 * Plain-JDBC access to lms.related_material: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RelatedMaterialRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("ebookId", "r.ebook_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("url", "r.url", ValueType.STRING),
            field("summary", "r.summary", ValueType.STRING),
            field("source", "r.source", ValueType.STRING),
            field("mediaType", "r.media_type", ValueType.STRING),
            field("contentLength", "r.content_length", ValueType.INTEGER),
            field("contentLengthBand", "r.content_length_band", ValueType.STRING),
            field("videoLengthSeconds", "r.video_length_seconds", ValueType.INTEGER),
            field("videoLengthBand", "r.video_length_band", ValueType.STRING),
            field("initialRank", "r.initial_rank", ValueType.INTEGER),
            field("interactivityLevel", "r.interactivity_level", ValueType.INTEGER),
            field("interactivityBand", "r.interactivity_band", ValueType.STRING),
            field("conceptualLevel", "r.conceptual_level", ValueType.STRING),
            field("languageComplexity", "r.language_complexity", ValueType.DECIMAL),
            field("languageComplexityBand", "r.language_complexity_band", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RelatedMaterialEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", RelatedMaterialEntity::getCreatedAt),
            Map.entry("updateTime", RelatedMaterialEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.ebook_id, r.title, r.url, r.summary, r.source, r.media_type, r.content_length,
                   r.content_length_band, r.video_length_seconds, r.video_length_band, r.initial_rank,
                   r.interactivity_level, r.interactivity_band, r.conceptual_level, r.language_complexity,
                   r.language_complexity_band, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM lms.related_material r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.related_material
            (id, tenant_id, ebook_id, title, url, summary, source, media_type, content_length, content_length_band,
             video_length_seconds, video_length_band, initial_rank, interactivity_level, interactivity_band,
             conceptual_level, language_complexity, language_complexity_band, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :ebookId, :title, :url, :summary, :source, :mediaType, :contentLength,
                    :contentLengthBand, :videoLengthSeconds, :videoLengthBand, :initialRank, :interactivityLevel,
                    :interactivityBand, :conceptualLevel, :languageComplexity, :languageComplexityBand, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, ebook_id, title, url, summary, source, media_type, content_length,
                      content_length_band, video_length_seconds, video_length_band, initial_rank, interactivity_level,
                      interactivity_band, conceptual_level, language_complexity, language_complexity_band, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.related_material
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.related_material
               SET title = :title,
                   url = :url,
                   summary = :summary,
                   source = :source,
                   media_type = :mediaType,
                   content_length = :contentLength,
                   content_length_band = :contentLengthBand,
                   video_length_seconds = :videoLengthSeconds,
                   video_length_band = :videoLengthBand,
                   initial_rank = :initialRank,
                   interactivity_level = :interactivityLevel,
                   interactivity_band = :interactivityBand,
                   conceptual_level = :conceptualLevel,
                   language_complexity = :languageComplexity,
                   language_complexity_band = :languageComplexityBand,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_EBOOK_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.ebook WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RELATED_MATERIAL_RANK_ITEM_RELATED_MATERIAL_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.related_material_rank_item WHERE tenant_id = :tenantId
                AND related_material_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.ebook p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.ebook_id IN (SELECT p0.id FROM lms.ebook p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public RelatedMaterialRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RelatedMaterialEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RelatedMaterialRepository::map).optional();
    }

    public Page<RelatedMaterialEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RelatedMaterialEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RelatedMaterialRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RelatedMaterialEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public RelatedMaterialEntity insert(RelatedMaterialEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("ebookId", e.getEbookId())
                .param("title", e.getTitle())
                .param("url", e.getUrl())
                .param("summary", e.getSummary())
                .param("source", e.getSource())
                .param("mediaType", e.getMediaType())
                .param("contentLength", e.getContentLength())
                .param("contentLengthBand", e.getContentLengthBand())
                .param("videoLengthSeconds", e.getVideoLengthSeconds())
                .param("videoLengthBand", e.getVideoLengthBand())
                .param("initialRank", e.getInitialRank())
                .param("interactivityLevel", e.getInteractivityLevel())
                .param("interactivityBand", e.getInteractivityBand())
                .param("conceptualLevel", e.getConceptualLevel())
                .param("languageComplexity", e.getLanguageComplexity())
                .param("languageComplexityBand", e.getLanguageComplexityBand())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(RelatedMaterialRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(RelatedMaterialEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("title", e.getTitle())
                .param("url", e.getUrl())
                .param("summary", e.getSummary())
                .param("source", e.getSource())
                .param("mediaType", e.getMediaType())
                .param("contentLength", e.getContentLength())
                .param("contentLengthBand", e.getContentLengthBand())
                .param("videoLengthSeconds", e.getVideoLengthSeconds())
                .param("videoLengthBand", e.getVideoLengthBand())
                .param("initialRank", e.getInitialRank())
                .param("interactivityLevel", e.getInteractivityLevel())
                .param("interactivityBand", e.getInteractivityBand())
                .param("conceptualLevel", e.getConceptualLevel())
                .param("languageComplexity", e.getLanguageComplexity())
                .param("languageComplexityBand", e.getLanguageComplexityBand())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(RelatedMaterialEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.ebook row exists (live). */
    public boolean existsEbookId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EBOOK_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.related_material_rank_item rows still reference the row. */
    public boolean referencedByRelatedMaterialRankItemRelatedMaterialId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RELATED_MATERIAL_RANK_ITEM_RELATED_MATERIAL_ID)
                .param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through ebook_id (empty: a tenant-level row, or none). */
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

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.ebook_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, RelatedMaterialEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static RelatedMaterialEntity map(ResultSet rs, int row) throws SQLException {
        return RelatedMaterialEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .ebookId(rs.getObject("ebook_id", UUID.class))
                .title(rs.getString("title"))
                .url(rs.getString("url"))
                .summary(rs.getString("summary"))
                .source(rs.getString("source"))
                .mediaType(rs.getString("media_type"))
                .contentLength(rs.getObject("content_length", Long.class))
                .contentLengthBand(rs.getString("content_length_band"))
                .videoLengthSeconds(rs.getObject("video_length_seconds", Long.class))
                .videoLengthBand(rs.getString("video_length_band"))
                .initialRank(rs.getObject("initial_rank", Integer.class))
                .interactivityLevel(rs.getObject("interactivity_level", Integer.class))
                .interactivityBand(rs.getString("interactivity_band"))
                .conceptualLevel(rs.getString("conceptual_level"))
                .languageComplexity(rs.getBigDecimal("language_complexity"))
                .languageComplexityBand(rs.getString("language_complexity_band"))
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
