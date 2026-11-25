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
import com.mysms.library.domain.LoanEntity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
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
 * Plain-JDBC access to library.loan: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class LoanRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("mediaUnitId", "r.media_unit_id", ValueType.UUID),
            field("mediaId", "r.media_id", ValueType.UUID),
            field("libraryCardId", "r.library_card_id", ValueType.UUID),
            field("borrowerUserId", "r.borrower_user_id", ValueType.UUID),
            field("issuedOn", "r.issued_on", ValueType.DATE),
            field("dueOn", "r.due_on", ValueType.DATE),
            field("returnedOn", "r.returned_on", ValueType.DATE),
            field("fineAmount", "r.fine_amount", ValueType.DECIMAL),
            field("invoiceId", "r.invoice_id", ValueType.UUID),
            field("issuedByUserId", "r.issued_by_user_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LoanEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LoanEntity::getCreatedAt),
            Map.entry("updateTime", LoanEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.media_unit_id, r.media_id, r.library_card_id, r.borrower_user_id, r.issued_on,
                   r.due_on, r.returned_on, r.fine_amount, r.invoice_id, r.issued_by_user_id, r.status, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM library.loan r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO library.loan
            (id, tenant_id, media_unit_id, media_id, library_card_id, borrower_user_id, issued_on, due_on,
             returned_on, fine_amount, invoice_id, issued_by_user_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :mediaUnitId, :mediaId, :libraryCardId, :borrowerUserId, :issuedOn, :dueOn,
                    :returnedOn, COALESCE(:fineAmount, 0), :invoiceId, :issuedByUserId, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, media_unit_id, media_id, library_card_id, borrower_user_id, issued_on, due_on,
                      returned_on, fine_amount, invoice_id, issued_by_user_id, status, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE library.loan
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE library.loan
               SET media_unit_id = :mediaUnitId,
                   media_id = :mediaId,
                   library_card_id = :libraryCardId,
                   borrower_user_id = :borrowerUserId,
                   issued_on = :issuedOn,
                   due_on = :dueOn,
                   returned_on = :returnedOn,
                   fine_amount = :fineAmount,
                   invoice_id = :invoiceId,
                   issued_by_user_id = :issuedByUserId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_LIBRARY_CARD_ID = """
            SELECT EXISTS (SELECT 1 FROM library.library_card WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_MEDIA_ID = """
            SELECT EXISTS (SELECT 1 FROM library.media WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM library.media p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    /** The loan's own lifecycle steps: status, the return date and the fine, under the version guard. */
    private static final String SETTLE = """
            UPDATE library.loan
               SET status = :status,
                   returned_on = :returnedOn,
                   fine_amount = :fineAmount,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String LINK_INVOICE = """
            UPDATE library.loan
               SET invoice_id = :invoiceId,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL AND invoice_id IS NULL
            """;
    /** The overdue fine per day of the card type behind the loan's card; empty when the card or type is gone. */
    private static final String FINE_PER_DAY = """
            SELECT t.fine_per_day
              FROM library.library_card c
              JOIN library.library_card_type t ON t.id = c.card_type_id AND t.tenant_id = :tenantId
             WHERE c.tenant_id = :tenantId
               AND c.id = :cardId
            """;
    /** The library card of the loan: whose it is, for the fine's invoice. */
    private static final String CARD_HOLDER = """
            SELECT c.institution_id, c.person_id, c.student_id, c.holder_type
              FROM library.library_card c
             WHERE c.tenant_id = :tenantId
               AND c.id = :cardId
            """;
    /** The borrower's own loans (lent to their account or on a card of their person), and the staffed ones. */
    private static final String REACHABLE = """
             (r.borrower_user_id = :borrowerUser
              OR (:staff AND (:staffAll OR r.media_id IN (SELECT s0.id FROM library.media s0
                                                          WHERE s0.tenant_id = :tenantId
                                                            AND s0.institution_id = ANY(:staffIds)))))
            """;
    /** {@link #REACHABLE}, and the loans on a card of the borrower's person. */
    private static final String REACHABLE_WITH_PERSON = """
             (r.borrower_user_id = :borrowerUser
              OR r.library_card_id IN (SELECT c0.id FROM library.library_card c0
                                        WHERE c0.tenant_id = :tenantId AND c0.person_id = :borrowerPerson)
              OR (:staff AND (:staffAll OR r.media_id IN (SELECT s0.id FROM library.media s0
                                                          WHERE s0.tenant_id = :tenantId
                                                            AND s0.institution_id = ANY(:staffIds)))))
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.media_id IN (SELECT p0.id FROM library.media p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public LoanRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LoanEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LoanRepository::map).optional();
    }

    public Page<LoanEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LoanEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LoanRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LoanEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LoanEntity insert(LoanEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("mediaUnitId", e.getMediaUnitId())
                .param("mediaId", e.getMediaId())
                .param("libraryCardId", e.getLibraryCardId())
                .param("borrowerUserId", e.getBorrowerUserId())
                .param("issuedOn", e.getIssuedOn())
                .param("dueOn", e.getDueOn())
                .param("returnedOn", e.getReturnedOn())
                .param("fineAmount", e.getFineAmount())
                .param("invoiceId", e.getInvoiceId())
                .param("issuedByUserId", e.getIssuedByUserId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LoanRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LoanEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("mediaUnitId", e.getMediaUnitId())
                .param("mediaId", e.getMediaId())
                .param("libraryCardId", e.getLibraryCardId())
                .param("borrowerUserId", e.getBorrowerUserId())
                .param("issuedOn", e.getIssuedOn())
                .param("dueOn", e.getDueOn())
                .param("returnedOn", e.getReturnedOn())
                .param("fineAmount", e.getFineAmount())
                .param("invoiceId", e.getInvoiceId())
                .param("issuedByUserId", e.getIssuedByUserId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LoanEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced library.library_card row exists (live). */
    public boolean existsLibraryCardId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LIBRARY_CARD_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced library.media row exists (live). */
    public boolean existsMediaId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_MEDIA_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through media_id (empty: a tenant-level row, or none). */
    /** Moves the loan to a status with its return date and fine; 0 rows means it changed since it was read. */
    public int settle(UUID id, int expected, String status, LocalDate returnedOn, BigDecimal fineAmount,
            OffsetDateTime now, UUID actor) {
        return jdbc.sql(SETTLE).param("id", id).param("expected", expected).param("status", status)
                .param("returnedOn", returnedOn).param("fineAmount", fineAmount).param("now", now)
                .param("actor", actor).update();
    }

    /** Records the fine's invoice once; 0 rows when the loan changed or already has one. */
    public int linkInvoice(UUID id, int expected, UUID invoiceId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(LINK_INVOICE).param("id", id).param("expected", expected).param("invoiceId", invoiceId)
                .param("now", now).param("actor", actor).update();
    }

    public Optional<BigDecimal> finePerDay(UUID cardId) {
        return jdbc.sql(FINE_PER_DAY).param("cardId", cardId).query(BigDecimal.class).optional();
    }

    public Optional<CardHolder> cardHolder(UUID cardId) {
        return jdbc.sql(CARD_HOLDER).param("cardId", cardId)
                .query((rs, row) -> new CardHolder(rs.getObject("institution_id", UUID.class),
                        rs.getObject("person_id", UUID.class), rs.getObject("student_id", UUID.class),
                        rs.getString("holder_type")))
                .optional();
    }

    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** The loans of the borrower's account or person, and every loan of the institutions the staff scope reaches. */
    public static SqlFragment reachableBy(UUID user, Optional<UUID> person, DataScope staff) {
        Map<String, Object> params = new HashMap<>(Map.of("borrowerUser", user, "staff", !staff.institutions().isEmpty()
                || staff.all(), "staffAll", staff.all(), "staffIds", staff.institutionArray()));
        person.ifPresent(p -> params.put("borrowerPerson", p));
        return new SqlFragment(person.isPresent() ? REACHABLE_WITH_PERSON : REACHABLE, params);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, LoanEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LoanEntity map(ResultSet rs, int row) throws SQLException {
        return LoanEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .mediaUnitId(rs.getObject("media_unit_id", UUID.class))
                .mediaId(rs.getObject("media_id", UUID.class))
                .libraryCardId(rs.getObject("library_card_id", UUID.class))
                .borrowerUserId(rs.getObject("borrower_user_id", UUID.class))
                .issuedOn(rs.getObject("issued_on", LocalDate.class))
                .dueOn(rs.getObject("due_on", LocalDate.class))
                .returnedOn(rs.getObject("returned_on", LocalDate.class))
                .fineAmount(rs.getBigDecimal("fine_amount"))
                .invoiceId(rs.getObject("invoice_id", UUID.class))
                .issuedByUserId(rs.getObject("issued_by_user_id", UUID.class))
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
