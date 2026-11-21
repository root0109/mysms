package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.NoticeTranslationEntity;
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
 * Plain-JDBC access to privacy.notice_translation: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class NoticeTranslationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("language", "r.language", ValueType.STRING),
            field("contentSha256", "r.content_sha256", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("language", "r.language", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "language";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<NoticeTranslationEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("language", NoticeTranslationEntity::getLanguage),
            Map.entry("createTime", NoticeTranslationEntity::getCreatedAt),
            Map.entry("updateTime", NoticeTranslationEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.notice_version_id, r.language, r.body_markdown, r.summary_for_children,
                   r.content_sha256, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM privacy.notice_translation r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String FIND_BY_LANGUAGE =
            SELECT_LIVE + " AND r.notice_version_id = :noticeVersionId AND r.language = :language";
    private static final String INSERT = """
            INSERT INTO privacy.notice_translation
            (id, tenant_id, notice_version_id, language, body_markdown, summary_for_children, content_sha256,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :noticeVersionId, :language, :bodyMarkdown, :summaryForChildren, :contentSha256,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, notice_version_id, language, body_markdown, summary_for_children, content_sha256,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String UPDATE = """
            UPDATE privacy.notice_translation
               SET language = :language,
                   body_markdown = :bodyMarkdown,
                   summary_for_children = :summaryForChildren,
                   content_sha256 = :contentSha256,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SOFT_DELETE = """
            UPDATE privacy.notice_translation
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String LANGUAGE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM privacy.notice_translation
                            WHERE tenant_id = :tenantId
                              AND notice_version_id = :noticeVersionId AND language = :language AND id <> :id
                              AND deleted_at IS NULL)
            """;
    private static final String EXISTS_FOR_VERSION = """
            SELECT EXISTS (SELECT 1 FROM privacy.notice_translation WHERE tenant_id = :tenantId
                AND notice_version_id = :noticeVersionId AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public NoticeTranslationRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<NoticeTranslationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(NoticeTranslationRepository::map).optional();
    }

    /** The version's live translation into the language. */
    public Optional<NoticeTranslationEntity> findByLanguage(UUID noticeVersionId, String language) {
        return jdbc.sql(FIND_BY_LANGUAGE).param("noticeVersionId", noticeVersionId).param("language", language)
                .query(NoticeTranslationRepository::map).optional();
    }

    public Page<NoticeTranslationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<NoticeTranslationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(NoticeTranslationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), NoticeTranslationEntity::getId);
    }

    /** Inserts the row and returns it as stored. */
    public NoticeTranslationEntity insert(NoticeTranslationEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("noticeVersionId", e.getNoticeVersionId())
                .param("language", e.getLanguage())
                .param("bodyMarkdown", e.getBodyMarkdown())
                .param("summaryForChildren", e.getSummaryForChildren())
                .param("contentSha256", e.getContentSha256())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(NoticeTranslationRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(NoticeTranslationEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("language", e.getLanguage())
                .param("bodyMarkdown", e.getBodyMarkdown())
                .param("summaryForChildren", e.getSummaryForChildren())
                .param("contentSha256", e.getContentSha256())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(NoticeTranslationEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_notice_translation_language: another live translation of the version has the language. */
    public boolean languageTaken(NoticeTranslationEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(LANGUAGE_TAKEN)
                .param("noticeVersionId", e.getNoticeVersionId())
                .param("language", e.getLanguage())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the version has at least one live translation. */
    public boolean existsForVersion(UUID noticeVersionId) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FOR_VERSION).param("noticeVersionId", noticeVersionId)
                .query(Boolean.class).single());
    }

    /** Children of the path's parent version. */
    public static SqlFragment ofParent(UUID noticeVersionId) {
        return new SqlFragment("r.notice_version_id = :parentId", Map.of("parentId", noticeVersionId));
    }

    private static List<Object> sortValues(SortSpec sort, NoticeTranslationEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static NoticeTranslationEntity map(ResultSet rs, int row) throws SQLException {
        return NoticeTranslationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .noticeVersionId(rs.getObject("notice_version_id", UUID.class))
                .language(rs.getString("language"))
                .bodyMarkdown(rs.getString("body_markdown"))
                .summaryForChildren(rs.getString("summary_for_children"))
                .contentSha256(rs.getString("content_sha256"))
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
