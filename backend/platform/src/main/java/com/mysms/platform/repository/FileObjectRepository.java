package com.mysms.platform.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.FileObjectEntity;
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
 * Plain-JDBC access to platform.file_object: constant schema-qualified SQL, live rows only,
 * keyset paging over an allowlisted order, the row_version guard on every change, and the reads the download rules need
 * (the record types a file is attached to, whether a theme shows it). Every statement filters the caller's tenant
 * itself; row-level security is the backstop.
 */
@Repository
public class FileObjectRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("fileName", "r.file_name", ValueType.STRING),
            field("mimeType", "r.mime_type", ValueType.STRING),
            field("fileKind", "r.file_kind", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("ownerEntityType", "r.owner_entity_type", ValueType.STRING),
            field("ownerEntityId", "r.owner_entity_id", ValueType.UUID),
            field("collectionName", "r.collection_name", ValueType.STRING),
            field("uploadedByUserId", "r.uploaded_by_user_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("fileName", "r.file_name", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime desc";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<FileObjectEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("fileName", FileObjectEntity::getFileName),
            Map.entry("createTime", FileObjectEntity::getCreatedAt),
            Map.entry("updateTime", FileObjectEntity::getUpdatedAt));

    private static final String COLUMNS = """
            id, tenant_id, storage_disk, storage_key, storage_generation, file_name, original_name, mime_type,
            size_bytes, checksum_sha256, owner_entity_type, owner_entity_id, collection_name, sort_order, file_kind,
            is_shared, processing_status, processing_attempts, processing_error, status, scanned_at,
            uploaded_by_user_id, created_at, created_by, updated_at, updated_by, deleted_at, row_version""";
    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.storage_disk, r.storage_key, r.storage_generation, r.file_name,
                   r.original_name, r.mime_type, r.size_bytes, r.checksum_sha256, r.owner_entity_type,
                   r.owner_entity_id, r.collection_name, r.sort_order, r.file_kind, r.is_shared, r.processing_status,
                   r.processing_attempts, r.processing_error, r.status, r.scanned_at, r.uploaded_by_user_id,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM platform.file_object r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO platform.file_object
            (id, tenant_id, storage_disk, storage_key, storage_generation, file_name, original_name, mime_type,
             size_bytes, checksum_sha256, owner_entity_type, owner_entity_id, collection_name, file_kind, status,
             uploaded_by_user_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :storageDisk, :storageKey, :storageGeneration, :fileName, :originalName,
                    :mimeType, :sizeBytes, :checksumSha256, :ownerEntityType, :ownerEntityId, :collectionName,
                    :fileKind, :status, :uploadedByUserId, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING\s""" + COLUMNS;
    /**
     * CompleteUpload: a pending file becomes available, under the version it was read at, pinned to the generation of
     * the stored object that was checked.
     */
    private static final String TRANSITION_COMPLETE_UPLOAD = """
            UPDATE platform.file_object
               SET status = 'available',
                   storage_generation = :generation,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'pending' AND deleted_at IS NULL
            """;
    /** A deleted file's row stays (audit, references) as status deleted; its bytes are removed from the store. */
    private static final String SOFT_DELETE = """
            UPDATE platform.file_object
               SET deleted_at = :deletedAt,
                   status = 'deleted',
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String ATTACHED_TO = """
            SELECT DISTINCT a.entity_type
              FROM platform.entity_attachment a
             WHERE a.tenant_id = :tenantId
               AND a.deleted_at IS NULL
               AND a.file_id = :id
            """;
    private static final String SHOWN_BY_THEME = """
            SELECT EXISTS (SELECT 1 FROM platform.theme t
                            WHERE t.tenant_id = :tenantId AND t.deleted_at IS NULL
                              AND (t.thumbnail_file_id = :id OR t.thumbnail_alt_file_id = :id
                                   OR t.logo_file_id = :id))
            """;
    private static final String LIVE_COUNT = """
            SELECT count(*)
              FROM platform.file_object r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;

    /** Files whose name contains the text (pattern with LIKE wildcards escaped), by name; visibility appended. */
    private static final String SEARCH = SELECT_LIVE + " AND r.file_name ILIKE :pattern ESCAPE '\\' AND ";
    private static final String SEARCH_ORDER = " ORDER BY lower(r.file_name), r.id LIMIT :limit";
    /** Every live file of the tenant, oldest first (the stored-files report). */
    /** The caller's own earlier generated output with the same name and bytes (an import template). */
    private static final String FIND_OUTPUT = SELECT_LIVE + """
             AND r.uploaded_by_user_id = :user AND r.file_name = :name AND r.checksum_sha256 = :checksum
             AND r.status = 'available' AND r.owner_entity_type IS NULL
            ORDER BY r.created_at DESC, r.id LIMIT 1""";
    private static final String EXPORT = SELECT_LIVE + " ORDER BY r.created_at, r.id LIMIT :limit";

    private final JdbcClient jdbc;

    public FileObjectRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<FileObjectEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(FileObjectRepository::map).optional();
    }

    public Page<FileObjectEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<FileObjectEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(FileObjectRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), FileObjectEntity::getId);
    }

    public FileObjectEntity insert(FileObjectEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("storageDisk", e.getStorageDisk())
                .param("storageKey", e.getStorageKey())
                .param("storageGeneration", e.getStorageGeneration())
                .param("fileName", e.getFileName())
                .param("originalName", e.getOriginalName())
                .param("mimeType", e.getMimeType())
                .param("sizeBytes", e.getSizeBytes())
                .param("checksumSha256", e.getChecksumSha256())
                .param("ownerEntityType", e.getOwnerEntityType())
                .param("ownerEntityId", e.getOwnerEntityId())
                .param("collectionName", e.getCollectionName())
                .param("fileKind", e.getFileKind())
                .param("status", e.getStatus())
                .param("uploadedByUserId", e.getUploadedByUserId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(FileObjectRepository::map).single();
    }

    /** platform.CompleteUpload; 0 rows when the file changed or is no longer pending. */
    public int completeUpload(UUID id, int expected, long generation, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_COMPLETE_UPLOAD).param("id", id).param("expected", expected)
                .param("generation", generation).param("now", now).param("actor", actor).update();
    }

    public int softDelete(FileObjectEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** The record types (schema.table) the file is attached to through live platform.entity_attachment rows. */
    public List<String> attachedTo(UUID id) {
        return jdbc.sql(ATTACHED_TO).param("id", id).query(String.class).list();
    }

    /** Whether a live theme of the tenant shows the file (thumbnail, dark thumbnail or logo). */
    public boolean shownByTheme(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(SHOWN_BY_THEME).param("id", id).query(Boolean.class).single());
    }

    /** How many live files (pending or available) the tenant has. */
    public long liveCount() {
        return jdbc.sql(LIVE_COUNT).query(Long.class).single();
    }

    /** At most {@code limit} visible files whose name contains the text, by name. */
    public List<FileObjectEntity> search(String pattern, SqlFragment visible, int limit) {
        return jdbc.sql(SEARCH + "(" + visible.sql() + ")" + SEARCH_ORDER).params(visible.params())
                .param("pattern", pattern).param("limit", limit).query(FileObjectRepository::map).list();
    }

    /** The user's own available output file of this name and SHA-256, when one exists. */
    public Optional<FileObjectEntity> findOutput(UUID user, String name, String checksum) {
        return jdbc.sql(FIND_OUTPUT).param("user", user).param("name", name).param("checksum", checksum)
                .query(FileObjectRepository::map).optional();
    }

    /** At most {@code limit} live files of the tenant, oldest first. */
    public List<FileObjectEntity> export(int limit) {
        return jdbc.sql(EXPORT).param("limit", limit).query(FileObjectRepository::map).list();
    }

    /** Only the caller's own uploads, unless every file of the tenant may be seen. */
    public static SqlFragment visibleTo(UUID user, boolean everyone) {
        if (everyone) {
            return SqlFragment.TRUE;
        }
        return new SqlFragment("r.uploaded_by_user_id = :uploadedBy", Map.of("uploadedBy", user));
    }

    private static List<Object> sortValues(SortSpec sort, FileObjectEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static FileObjectEntity map(ResultSet rs, int row) throws SQLException {
        return FileObjectEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .storageDisk(rs.getString("storage_disk"))
                .storageKey(rs.getString("storage_key"))
                .storageGeneration(rs.getObject("storage_generation", Long.class))
                .fileName(rs.getString("file_name"))
                .originalName(rs.getString("original_name"))
                .mimeType(rs.getString("mime_type"))
                .sizeBytes(rs.getObject("size_bytes", Long.class))
                .checksumSha256(rs.getString("checksum_sha256"))
                .ownerEntityType(rs.getString("owner_entity_type"))
                .ownerEntityId(rs.getObject("owner_entity_id", UUID.class))
                .collectionName(rs.getString("collection_name"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
                .fileKind(rs.getString("file_kind"))
                .isShared(rs.getBoolean("is_shared"))
                .processingStatus(rs.getString("processing_status"))
                .processingAttempts(rs.getObject("processing_attempts", Integer.class))
                .processingError(rs.getString("processing_error"))
                .status(rs.getString("status"))
                .scannedAt(rs.getObject("scanned_at", OffsetDateTime.class))
                .uploadedByUserId(rs.getObject("uploaded_by_user_id", UUID.class))
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
