package com.mysms.frontoffice.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.frontoffice.domain.PostalRecordEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to frontoffice.postal_record (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class PostalRecordRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("direction", "r.direction", ValueType.STRING),
            field("postType", "r.post_type", ValueType.STRING),
            field("referenceNumber", "r.reference_number", ValueType.STRING),
            field("isConfidential", "r.is_confidential", ValueType.BOOLEAN),
            field("senderName", "r.sender_name", ValueType.STRING),
            field("senderAddress", "r.sender_address", ValueType.STRING),
            field("receiverName", "r.receiver_name", ValueType.STRING),
            field("receiverAddress", "r.receiver_address", ValueType.STRING),
            field("postalDate", "r.postal_date", ValueType.DATE),
            field("description", "r.description", ValueType.STRING),
            field("attachmentFileId", "r.attachment_file_id", ValueType.UUID),
            field("loggedByUserId", "r.logged_by_user_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PostalRecordEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PostalRecordEntity::getCreatedAt),
            Map.entry("updateTime", PostalRecordEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.direction, r.post_type,
                   r.reference_number, r.is_confidential, r.sender_name, r.sender_address, r.receiver_name,
                   r.receiver_address, r.postal_date, r.description, r.attachment_file_id, r.logged_by_user_id,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM frontoffice.postal_record r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO frontoffice.postal_record
            (id, tenant_id, institution_id, academic_year_id, direction, post_type, reference_number,
             is_confidential, sender_name, sender_address, receiver_name, receiver_address, postal_date, description,
             attachment_file_id, logged_by_user_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :direction, :postType, :referenceNumber,
                    COALESCE(:isConfidential, false), :senderName, :senderAddress, :receiverName, :receiverAddress,
                    :postalDate, :description, :attachmentFileId, :loggedByUserId, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, direction, post_type, reference_number,
                      is_confidential, sender_name, sender_address, receiver_name, receiver_address, postal_date,
                      description, attachment_file_id, logged_by_user_id, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE frontoffice.postal_record
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE frontoffice.postal_record
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   direction = :direction,
                   post_type = :postType,
                   reference_number = :referenceNumber,
                   is_confidential = :isConfidential,
                   sender_name = :senderName,
                   sender_address = :senderAddress,
                   receiver_name = :receiverName,
                   receiver_address = :receiverAddress,
                   postal_date = :postalDate,
                   description = :description,
                   attachment_file_id = :attachmentFileId,
                   logged_by_user_id = :loggedByUserId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public PostalRecordRepository(@Qualifier(FrontofficeDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PostalRecordEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PostalRecordRepository::map).optional();
    }

    public Page<PostalRecordEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PostalRecordEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PostalRecordRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PostalRecordEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PostalRecordEntity insert(PostalRecordEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("direction", e.getDirection())
                .param("postType", e.getPostType())
                .param("referenceNumber", e.getReferenceNumber())
                .param("isConfidential", e.getIsConfidential())
                .param("senderName", e.getSenderName())
                .param("senderAddress", e.getSenderAddress())
                .param("receiverName", e.getReceiverName())
                .param("receiverAddress", e.getReceiverAddress())
                .param("postalDate", e.getPostalDate())
                .param("description", e.getDescription())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("loggedByUserId", e.getLoggedByUserId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PostalRecordRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PostalRecordEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("direction", e.getDirection())
                .param("postType", e.getPostType())
                .param("referenceNumber", e.getReferenceNumber())
                .param("isConfidential", e.getIsConfidential())
                .param("senderName", e.getSenderName())
                .param("senderAddress", e.getSenderAddress())
                .param("receiverName", e.getReceiverName())
                .param("receiverAddress", e.getReceiverAddress())
                .param("postalDate", e.getPostalDate())
                .param("description", e.getDescription())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("loggedByUserId", e.getLoggedByUserId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PostalRecordEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, PostalRecordEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PostalRecordEntity map(ResultSet rs, int row) throws SQLException {
        return PostalRecordEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .direction(rs.getString("direction"))
                .postType(rs.getString("post_type"))
                .referenceNumber(rs.getString("reference_number"))
                .isConfidential(rs.getObject("is_confidential", Boolean.class))
                .senderName(rs.getString("sender_name"))
                .senderAddress(rs.getString("sender_address"))
                .receiverName(rs.getString("receiver_name"))
                .receiverAddress(rs.getString("receiver_address"))
                .postalDate(rs.getObject("postal_date", LocalDate.class))
                .description(rs.getString("description"))
                .attachmentFileId(rs.getObject("attachment_file_id", UUID.class))
                .loggedByUserId(rs.getObject("logged_by_user_id", UUID.class))
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
