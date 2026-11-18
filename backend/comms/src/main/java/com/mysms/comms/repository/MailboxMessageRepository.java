package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.MailboxMessageEntity;
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
 * Plain-JDBC access to comms.mailbox_message: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class MailboxMessageRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("ownerUserId", "r.owner_user_id", ValueType.UUID),
            field("aboutStudentId", "r.about_student_id", ValueType.UUID),
            field("folder", "r.folder", ValueType.STRING),
            field("fromAddress", "r.from_address", ValueType.STRING),
            field("fromDisplay", "r.from_display", ValueType.STRING),
            field("toAddresses", "r.to_addresses", ValueType.STRING),
            field("subject", "r.subject", ValueType.STRING),
            field("body", "r.body", ValueType.STRING),
            field("deliveryStatus", "r.delivery_status", ValueType.STRING),
            field("providerMessageId", "r.provider_message_id", ValueType.STRING),
            field("isProcessed", "r.is_processed", ValueType.BOOLEAN),
            field("processedAt", "r.processed_at", ValueType.TIMESTAMP),
            field("dispatchedAt", "r.dispatched_at", ValueType.TIMESTAMP),
            field("readAt", "r.read_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<MailboxMessageEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", MailboxMessageEntity::getCreatedAt),
            Map.entry("updateTime", MailboxMessageEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.owner_user_id, r.about_student_id,
                   r.folder, r.from_address, r.from_display, r.to_addresses, r.subject, r.body, r.delivery_status,
                   r.provider_message_id, r.is_processed, r.processed_at, r.dispatched_at, r.read_at, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.mailbox_message r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.mailbox_message
            (id, tenant_id, institution_id, academic_year_id, owner_user_id, about_student_id, folder, from_address,
             from_display, to_addresses, subject, body, delivery_status, provider_message_id, is_processed,
             processed_at, dispatched_at, read_at, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :ownerUserId, :aboutStudentId, :folder,
                    :fromAddress, :fromDisplay, :toAddresses, :subject, :body, :deliveryStatus, :providerMessageId,
                    COALESCE(:isProcessed, false), :processedAt, :dispatchedAt, :readAt, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, owner_user_id, about_student_id, folder,
                      from_address, from_display, to_addresses, subject, body, delivery_status, provider_message_id,
                      is_processed, processed_at, dispatched_at, read_at, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.mailbox_message
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.mailbox_message
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public MailboxMessageRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<MailboxMessageEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(MailboxMessageRepository::map).optional();
    }

    public Page<MailboxMessageEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<MailboxMessageEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(MailboxMessageRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), MailboxMessageEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public MailboxMessageEntity insert(MailboxMessageEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("ownerUserId", e.getOwnerUserId())
                .param("aboutStudentId", e.getAboutStudentId())
                .param("folder", e.getFolder())
                .param("fromAddress", e.getFromAddress())
                .param("fromDisplay", e.getFromDisplay())
                .param("toAddresses", e.getToAddresses())
                .param("subject", e.getSubject())
                .param("body", e.getBody())
                .param("deliveryStatus", e.getDeliveryStatus())
                .param("providerMessageId", e.getProviderMessageId())
                .param("isProcessed", e.getIsProcessed())
                .param("processedAt", e.getProcessedAt())
                .param("dispatchedAt", e.getDispatchedAt())
                .param("readAt", e.getReadAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(MailboxMessageRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(MailboxMessageEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(MailboxMessageEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, MailboxMessageEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static MailboxMessageEntity map(ResultSet rs, int row) throws SQLException {
        return MailboxMessageEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .ownerUserId(rs.getObject("owner_user_id", UUID.class))
                .aboutStudentId(rs.getObject("about_student_id", UUID.class))
                .folder(rs.getString("folder"))
                .fromAddress(rs.getString("from_address"))
                .fromDisplay(rs.getString("from_display"))
                .toAddresses(rs.getString("to_addresses"))
                .subject(rs.getString("subject"))
                .body(rs.getString("body"))
                .deliveryStatus(rs.getString("delivery_status"))
                .providerMessageId(rs.getString("provider_message_id"))
                .isProcessed(rs.getObject("is_processed", Boolean.class))
                .processedAt(rs.getObject("processed_at", OffsetDateTime.class))
                .dispatchedAt(rs.getObject("dispatched_at", OffsetDateTime.class))
                .readAt(rs.getObject("read_at", OffsetDateTime.class))
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
