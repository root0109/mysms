package com.mysms.campus.library.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.domain.PurchaseRequestEntity;
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
 * Plain-JDBC access to library.purchase_request: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PurchaseRequestRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("requestNumber", "r.request_number", ValueType.STRING),
            field("title", "r.title", ValueType.STRING),
            field("authorText", "r.author_text", ValueType.STRING),
            field("edition", "r.edition", ValueType.STRING),
            field("publisherText", "r.publisher_text", ValueType.STRING),
            field("mediaTypeId", "r.media_type_id", ValueType.UUID),
            field("requestedByPersonId", "r.requested_by_person_id", ValueType.UUID),
            field("requestDate", "r.request_date", ValueType.DATE),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PurchaseRequestEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PurchaseRequestEntity::getCreatedAt),
            Map.entry("updateTime", PurchaseRequestEntity::getUpdatedAt),
            Map.entry("title", PurchaseRequestEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.request_number, r.title, r.author_text, r.edition, r.publisher_text,
                   r.media_type_id, r.requested_by_person_id, r.request_date, r.status, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM library.purchase_request r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO library.purchase_request
            (id, tenant_id, request_number, title, author_text, edition, publisher_text, media_type_id,
             requested_by_person_id, request_date, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :requestNumber, :title, :authorText, :edition, :publisherText, :mediaTypeId,
                    :requestedByPersonId, :requestDate, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, request_number, title, author_text, edition, publisher_text, media_type_id,
                      requested_by_person_id, request_date, status, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE library.purchase_request
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE library.purchase_request
               SET request_number = :requestNumber,
                   title = :title,
                   author_text = :authorText,
                   edition = :edition,
                   publisher_text = :publisherText,
                   media_type_id = :mediaTypeId,
                   requested_by_person_id = :requestedByPersonId,
                   request_date = :requestDate,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_MEDIA_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM library.media_type WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PURCHASE_REQUEST_COURSE_PURCHASE_REQUEST_ID = """
            SELECT EXISTS (SELECT 1 FROM library.purchase_request_course WHERE tenant_id = :tenantId
                AND purchase_request_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_PURCHASE_REQUEST_SUBJECT_PURCHASE_REQUEST_ID = """
            SELECT EXISTS (SELECT 1 FROM library.purchase_request_subject WHERE tenant_id = :tenantId
                AND purchase_request_id = :id AND deleted_at IS NULL)
            """;

    /** library.SubmitPurchaseRequest: allowed while status is draft. */
    private static final String TRANSITION_SUBMIT_PURCHASE_REQUEST = """
            UPDATE library.purchase_request
               SET status = 'requested',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** library.AcceptPurchaseRequest: allowed while status is requested. */
    private static final String TRANSITION_ACCEPT_PURCHASE_REQUEST = """
            UPDATE library.purchase_request
               SET status = 'accepted',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** library.RejectPurchaseRequest: allowed while status is requested. */
    private static final String TRANSITION_REJECT_PURCHASE_REQUEST = """
            UPDATE library.purchase_request
               SET status = 'rejected',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public PurchaseRequestRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PurchaseRequestEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PurchaseRequestRepository::map).optional();
    }

    public Page<PurchaseRequestEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PurchaseRequestEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PurchaseRequestRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PurchaseRequestEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PurchaseRequestEntity insert(PurchaseRequestEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("requestNumber", e.getRequestNumber())
                .param("title", e.getTitle())
                .param("authorText", e.getAuthorText())
                .param("edition", e.getEdition())
                .param("publisherText", e.getPublisherText())
                .param("mediaTypeId", e.getMediaTypeId())
                .param("requestedByPersonId", e.getRequestedByPersonId())
                .param("requestDate", e.getRequestDate())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PurchaseRequestRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PurchaseRequestEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("requestNumber", e.getRequestNumber())
                .param("title", e.getTitle())
                .param("authorText", e.getAuthorText())
                .param("edition", e.getEdition())
                .param("publisherText", e.getPublisherText())
                .param("mediaTypeId", e.getMediaTypeId())
                .param("requestedByPersonId", e.getRequestedByPersonId())
                .param("requestDate", e.getRequestDate())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PurchaseRequestEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced library.media_type row exists (live). */
    public boolean existsMediaTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_MEDIA_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live library.purchase_request_course rows still reference the row. */
    public boolean referencedByPurchaseRequestCoursePurchaseRequestId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PURCHASE_REQUEST_COURSE_PURCHASE_REQUEST_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live library.purchase_request_subject rows still reference the row. */
    public boolean referencedByPurchaseRequestSubjectPurchaseRequestId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PURCHASE_REQUEST_SUBJECT_PURCHASE_REQUEST_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** library.SubmitPurchaseRequest; 0 rows means the row changed (or vanished) since it was read. */
    public int submitPurchaseRequest(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_SUBMIT_PURCHASE_REQUEST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** library.AcceptPurchaseRequest; 0 rows means the row changed (or vanished) since it was read. */
    public int acceptPurchaseRequest(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_ACCEPT_PURCHASE_REQUEST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** library.RejectPurchaseRequest; 0 rows means the row changed (or vanished) since it was read. */
    public int rejectPurchaseRequest(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REJECT_PURCHASE_REQUEST)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    private static List<Object> sortValues(SortSpec sort, PurchaseRequestEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PurchaseRequestEntity map(ResultSet rs, int row) throws SQLException {
        return PurchaseRequestEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .requestNumber(rs.getString("request_number"))
                .title(rs.getString("title"))
                .authorText(rs.getString("author_text"))
                .edition(rs.getString("edition"))
                .publisherText(rs.getString("publisher_text"))
                .mediaTypeId(rs.getObject("media_type_id", UUID.class))
                .requestedByPersonId(rs.getObject("requested_by_person_id", UUID.class))
                .requestDate(rs.getObject("request_date", LocalDate.class))
                .status(rs.getString("status"))
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
