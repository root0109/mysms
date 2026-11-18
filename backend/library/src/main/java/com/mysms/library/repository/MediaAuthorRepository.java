package com.mysms.library.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.domain.MediaAuthorEntity;
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
 * Plain-JDBC access to library.media_author: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class MediaAuthorRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("mediaId", "r.media_id", ValueType.UUID),
            field("authorId", "r.author_id", ValueType.UUID),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<MediaAuthorEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", MediaAuthorEntity::getCreatedAt),
            Map.entry("updateTime", MediaAuthorEntity::getUpdatedAt),
            Map.entry("sortOrder", MediaAuthorEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.media_id, r.author_id, r.sort_order, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM library.media_author r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO library.media_author
            (id, tenant_id, media_id, author_id, sort_order, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :mediaId, :authorId, COALESCE(:sortOrder, 0), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, media_id, author_id, sort_order, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE library.media_author
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE library.media_author
               SET author_id = :authorId,
                   sort_order = :sortOrder,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_MEDIA_AUTHOR_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM library.media_author
                            WHERE tenant_id = :tenantId
                              AND media_id = :mediaId AND author_id = :authorId AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_AUTHOR_ID = """
            SELECT EXISTS (SELECT 1 FROM library.author WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_MEDIA_ID = """
            SELECT EXISTS (SELECT 1 FROM library.media WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM library.media p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.media_id IN (SELECT p0.id FROM library.media p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public MediaAuthorRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<MediaAuthorEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(MediaAuthorRepository::map).optional();
    }

    public Page<MediaAuthorEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<MediaAuthorEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(MediaAuthorRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), MediaAuthorEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public MediaAuthorEntity insert(MediaAuthorEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("mediaId", e.getMediaId())
                .param("authorId", e.getAuthorId())
                .param("sortOrder", e.getSortOrder())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(MediaAuthorRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(MediaAuthorEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("authorId", e.getAuthorId())
                .param("sortOrder", e.getSortOrder())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(MediaAuthorEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_media_author_1: media_id, author_id where (deleted_at IS NULL). */
    public boolean takenMediaIdAuthorId1(MediaAuthorEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_MEDIA_AUTHOR_1_TAKEN)
                .param("mediaId", e.getMediaId())
                .param("authorId", e.getAuthorId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced library.author row exists (live). */
    public boolean existsAuthorId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_AUTHOR_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced library.media row exists (live). */
    public boolean existsMediaId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_MEDIA_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through media_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.media_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, MediaAuthorEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static MediaAuthorEntity map(ResultSet rs, int row) throws SQLException {
        return MediaAuthorEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .mediaId(rs.getObject("media_id", UUID.class))
                .authorId(rs.getObject("author_id", UUID.class))
                .sortOrder(rs.getObject("sort_order", Integer.class))
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
