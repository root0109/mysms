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
import com.mysms.lms.domain.DriveItemEntity;
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
 * Plain-JDBC access to lms.drive_item (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class DriveItemRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("ownerUserId", "r.owner_user_id", ValueType.UUID),
            field("fileId", "r.file_id", ValueType.UUID),
            field("itemType", "r.item_type", ValueType.STRING),
            field("icon", "r.icon", ValueType.STRING),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("keywords", "r.keywords", ValueType.STRING),
            field("sizeBytes", "r.size_bytes", ValueType.INTEGER),
            field("linkUrl", "r.link_url", ValueType.STRING),
            field("uploadedAt", "r.uploaded_at", ValueType.TIMESTAMP),
            field("courseId", "r.course_id", ValueType.UUID),
            field("isShared", "r.is_shared", ValueType.BOOLEAN),
            field("likeCount", "r.like_count", ValueType.INTEGER),
            field("dislikeCount", "r.dislike_count", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<DriveItemEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", DriveItemEntity::getCreatedAt),
            Map.entry("updateTime", DriveItemEntity::getUpdatedAt),
            Map.entry("title", DriveItemEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.owner_user_id, r.file_id, r.item_type, r.icon, r.title, r.description,
                   r.keywords, r.size_bytes, r.link_url, r.uploaded_at, r.course_id, r.is_shared, r.like_count,
                   r.dislike_count, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM lms.drive_item r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.drive_item
            (id, tenant_id, owner_user_id, file_id, item_type, icon, title, description, keywords, size_bytes,
             link_url, uploaded_at, course_id, is_shared, like_count, dislike_count, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :ownerUserId, :fileId, :itemType, :icon, :title, :description, :keywords,
                    :sizeBytes, :linkUrl, COALESCE(:uploadedAt, now()), :courseId, COALESCE(:isShared, false),
                    COALESCE(:likeCount, 0), COALESCE(:dislikeCount, 0), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, owner_user_id, file_id, item_type, icon, title, description, keywords, size_bytes,
                      link_url, uploaded_at, course_id, is_shared, like_count, dislike_count, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.drive_item
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.drive_item
               SET owner_user_id = :ownerUserId,
                   file_id = :fileId,
                   item_type = :itemType,
                   icon = :icon,
                   title = :title,
                   description = :description,
                   keywords = :keywords,
                   size_bytes = :sizeBytes,
                   link_url = :linkUrl,
                   uploaded_at = :uploadedAt,
                   course_id = :courseId,
                   is_shared = :isShared,
                   like_count = :likeCount,
                   dislike_count = :dislikeCount,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_DRIVE_ITEM_SHARE_DRIVE_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.drive_item_share WHERE tenant_id = :tenantId
                AND drive_item_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.course p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.course_id IN (SELECT p0.id FROM lms.course p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.course_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public DriveItemRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<DriveItemEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(DriveItemRepository::map).optional();
    }

    public Page<DriveItemEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<DriveItemEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(DriveItemRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), DriveItemEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public DriveItemEntity insert(DriveItemEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("ownerUserId", e.getOwnerUserId())
                .param("fileId", e.getFileId())
                .param("itemType", e.getItemType())
                .param("icon", e.getIcon())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("keywords", e.getKeywords())
                .param("sizeBytes", e.getSizeBytes())
                .param("linkUrl", e.getLinkUrl())
                .param("uploadedAt", e.getUploadedAt())
                .param("courseId", e.getCourseId())
                .param("isShared", e.getIsShared())
                .param("likeCount", e.getLikeCount())
                .param("dislikeCount", e.getDislikeCount())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(DriveItemRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(DriveItemEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("ownerUserId", e.getOwnerUserId())
                .param("fileId", e.getFileId())
                .param("itemType", e.getItemType())
                .param("icon", e.getIcon())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("keywords", e.getKeywords())
                .param("sizeBytes", e.getSizeBytes())
                .param("linkUrl", e.getLinkUrl())
                .param("uploadedAt", e.getUploadedAt())
                .param("courseId", e.getCourseId())
                .param("isShared", e.getIsShared())
                .param("likeCount", e.getLikeCount())
                .param("dislikeCount", e.getDislikeCount())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(DriveItemEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.drive_item_share rows still reference the row. */
    public boolean referencedByDriveItemShareDriveItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_DRIVE_ITEM_SHARE_DRIVE_ITEM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through course_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, DriveItemEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static DriveItemEntity map(ResultSet rs, int row) throws SQLException {
        return DriveItemEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .ownerUserId(rs.getObject("owner_user_id", UUID.class))
                .fileId(rs.getObject("file_id", UUID.class))
                .itemType(rs.getString("item_type"))
                .icon(rs.getString("icon"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .keywords(rs.getString("keywords"))
                .sizeBytes(rs.getObject("size_bytes", Long.class))
                .linkUrl(rs.getString("link_url"))
                .uploadedAt(rs.getObject("uploaded_at", OffsetDateTime.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .isShared(rs.getObject("is_shared", Boolean.class))
                .likeCount(rs.getObject("like_count", Integer.class))
                .dislikeCount(rs.getObject("dislike_count", Integer.class))
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
