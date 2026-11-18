package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.ConversationParticipantEntity;
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
 * Plain-JDBC access to comms.conversation_participant: live rows only, keyset paging, and the row_version guard on
 * every update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ConversationParticipantRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("conversationId", "r.conversation_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("lastReadAt", "r.last_read_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ConversationParticipantEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ConversationParticipantEntity::getCreatedAt),
            Map.entry("updateTime", ConversationParticipantEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.conversation_id, r.user_id, r.last_read_at, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.conversation_participant r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.conversation_participant
            (id, tenant_id, conversation_id, user_id, last_read_at, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :conversationId, :userId, :lastReadAt, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, conversation_id, user_id, last_read_at, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.conversation_participant
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.conversation_participant
               SET user_id = :userId,
                   last_read_at = :lastReadAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_CONVERSATION_PARTICIPANT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM comms.conversation_participant
                            WHERE tenant_id = :tenantId
                              AND conversation_id = :conversationId AND user_id = :userId AND id <> :id AND ((deleted_at
                                  IS NULL)))
            """;
    private static final String EXISTS_CONVERSATION_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.conversation WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM comms.conversation p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.conversation_id IN (SELECT p0.id FROM comms.conversation p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds) OR p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public ConversationParticipantRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ConversationParticipantEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ConversationParticipantRepository::map).optional();
    }

    public Page<ConversationParticipantEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ConversationParticipantEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ConversationParticipantRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ConversationParticipantEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ConversationParticipantEntity insert(ConversationParticipantEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("conversationId", e.getConversationId())
                .param("userId", e.getUserId())
                .param("lastReadAt", e.getLastReadAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ConversationParticipantRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ConversationParticipantEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("lastReadAt", e.getLastReadAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ConversationParticipantEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_conversation_participant_1: conversation_id, user_id where (deleted_at IS NULL). */
    public boolean takenConversationIdUserId1(ConversationParticipantEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_CONVERSATION_PARTICIPANT_1_TAKEN)
                .param("conversationId", e.getConversationId())
                .param("userId", e.getUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced comms.conversation row exists (live). */
    public boolean existsConversationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONVERSATION_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through conversation_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.conversation_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ConversationParticipantEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ConversationParticipantEntity map(ResultSet rs, int row) throws SQLException {
        return ConversationParticipantEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .conversationId(rs.getObject("conversation_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .lastReadAt(rs.getObject("last_read_at", OffsetDateTime.class))
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
