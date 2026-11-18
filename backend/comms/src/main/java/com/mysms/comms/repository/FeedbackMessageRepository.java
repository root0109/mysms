package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.FeedbackMessageEntity;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
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
 * Plain-JDBC access to comms.feedback_message: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class FeedbackMessageRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("threadId", "r.thread_id", ValueType.UUID),
            field("senderUserId", "r.sender_user_id", ValueType.UUID),
            field("category", "r.category", ValueType.STRING),
            field("message", "r.message", ValueType.STRING),
            field("attachmentFileId", "r.attachment_file_id", ValueType.UUID),
            field("reviewStatus", "r.review_status", ValueType.STRING),
            field("deletedBySender", "r.deleted_by_sender", ValueType.BOOLEAN),
            field("deletedByReceiver", "r.deleted_by_receiver", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<FeedbackMessageEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", FeedbackMessageEntity::getCreatedAt),
            Map.entry("updateTime", FeedbackMessageEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.thread_id, r.sender_user_id, r.category, r.message, r.attachment_file_id,
                   r.review_status, r.deleted_by_sender, r.deleted_by_receiver, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.feedback_message r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.feedback_message
            (id, tenant_id, thread_id, sender_user_id, category, message, attachment_file_id, review_status,
             deleted_by_sender, deleted_by_receiver, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :threadId, :senderUserId, :category, :message, :attachmentFileId,
                    COALESCE(:reviewStatus, 'new'::character varying), COALESCE(:deletedBySender, false),
                    COALESCE(:deletedByReceiver, false), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, thread_id, sender_user_id, category, message, attachment_file_id, review_status,
                      deleted_by_sender, deleted_by_receiver, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.feedback_message
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.feedback_message
               SET sender_user_id = :senderUserId,
                   category = :category,
                   message = :message,
                   attachment_file_id = :attachmentFileId,
                   review_status = :reviewStatus,
                   deleted_by_sender = :deletedBySender,
                   deleted_by_receiver = :deletedByReceiver,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_THREAD_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.feedback_thread WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM comms.feedback_thread p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.thread_id IN (SELECT p0.id FROM comms.feedback_thread p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public FeedbackMessageRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<FeedbackMessageEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(FeedbackMessageRepository::map).optional();
    }

    public Page<FeedbackMessageEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<FeedbackMessageEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(FeedbackMessageRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), FeedbackMessageEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public FeedbackMessageEntity insert(FeedbackMessageEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("threadId", e.getThreadId())
                .param("senderUserId", e.getSenderUserId())
                .param("category", e.getCategory())
                .param("message", e.getMessage())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("reviewStatus", e.getReviewStatus())
                .param("deletedBySender", e.getDeletedBySender())
                .param("deletedByReceiver", e.getDeletedByReceiver())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(FeedbackMessageRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(FeedbackMessageEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("senderUserId", e.getSenderUserId())
                .param("category", e.getCategory())
                .param("message", e.getMessage())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("reviewStatus", e.getReviewStatus())
                .param("deletedBySender", e.getDeletedBySender())
                .param("deletedByReceiver", e.getDeletedByReceiver())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(FeedbackMessageEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced comms.feedback_thread row exists (live). */
    public boolean existsThreadId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_THREAD_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through thread_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.thread_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, FeedbackMessageEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static FeedbackMessageEntity map(ResultSet rs, int row) throws SQLException {
        return FeedbackMessageEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .threadId(rs.getObject("thread_id", UUID.class))
                .senderUserId(rs.getObject("sender_user_id", UUID.class))
                .category(rs.getString("category"))
                .message(rs.getString("message"))
                .attachmentFileId(rs.getObject("attachment_file_id", UUID.class))
                .reviewStatus(rs.getString("review_status"))
                .deletedBySender(rs.getObject("deleted_by_sender", Boolean.class))
                .deletedByReceiver(rs.getObject("deleted_by_receiver", Boolean.class))
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
