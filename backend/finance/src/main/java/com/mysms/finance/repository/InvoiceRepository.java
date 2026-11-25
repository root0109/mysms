package com.mysms.finance.repository;

import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.domain.InvoiceEntity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
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
 * Plain-JDBC access to finance.invoice and the lines its workflows write: live rows only, keyset paging over an
 * allowlisted order, the row_version guard on every update and step, and the locks the payment workflows take. Every
 * statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class InvoiceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("invoiceNumber", "r.invoice_number", ValueType.STRING),
            field("studentId", "r.student_id", ValueType.UUID),
            field("billToUserId", "r.bill_to_user_id", ValueType.UUID),
            field("billToName", "r.bill_to_name", ValueType.STRING),
            field("invoiceDate", "r.invoice_date", ValueType.DATE),
            field("dueDate", "r.due_date", ValueType.DATE),
            field("currencyId", "r.currency_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("invoiceType", "r.invoice_type", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("invoiceDate", "r.invoice_date", ValueType.DATE),
            field("invoiceNumber", "r.invoice_number", ValueType.STRING),
            field("total", "r.total", ValueType.DECIMAL));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    /** The number series of posted invoices. */
    public static final String INVOICE_NUMBERS = "invoice";

    private static final Map<String, Function<InvoiceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", InvoiceEntity::getCreatedAt),
            Map.entry("updateTime", InvoiceEntity::getUpdatedAt),
            Map.entry("invoiceDate", InvoiceEntity::getInvoiceDate),
            Map.entry("invoiceNumber", InvoiceEntity::getInvoiceNumber),
            Map.entry("total", InvoiceEntity::getTotal));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.invoice_number, r.student_id, r.bill_to_user_id,
                   r.bill_to_name, r.invoice_date, r.due_date, r.currency_id, r.subtotal, r.discount_total,
                   r.tax_total, r.total, r.amount_paid, r.status, r.invoice_type, r.credit_for_invoice_id,
                   r.posted_at, r.cancelled_at, r.document_file_id, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM finance.invoice r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    /** The row whether live or deleted: an id that is taken stays taken. */
    private static final String FIND_ANY_BY_ID = """
            SELECT r.id, r.tenant_id, r.institution_id, r.invoice_number, r.student_id, r.bill_to_user_id,
                   r.bill_to_name, r.invoice_date, r.due_date, r.currency_id, r.subtotal, r.discount_total,
                   r.tax_total, r.total, r.amount_paid, r.status, r.invoice_type, r.credit_for_invoice_id,
                   r.posted_at, r.cancelled_at, r.document_file_id, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM finance.invoice r
             WHERE r.tenant_id = :tenantId
               AND r.id = :id
            """;
    private static final String LOCK = SELECT_LIVE + " AND r.id = ANY(:ids) ORDER BY r.id FOR UPDATE";
    private static final String OF_STUDENT = SELECT_LIVE + """
               AND r.student_id = :studentId
             ORDER BY r.invoice_date, r.created_at, r.id
            """;
    /** The invoices a reminder run covers: the open ones of the institution, or the named ones there. */
    private static final String OPEN_OF_INSTITUTION = SELECT_LIVE + """
               AND r.institution_id = :institutionId AND r.status IN ('posted', 'partially_paid')
               AND r.total > r.amount_paid
               AND (:everyOpen OR r.id = ANY(:ids))
             ORDER BY r.due_date NULLS LAST, r.id
            """;
    private static final String INSERT = """
            INSERT INTO finance.invoice
            (id, tenant_id, institution_id, invoice_number, student_id, bill_to_user_id, bill_to_name, invoice_date,
             due_date, currency_id, subtotal, discount_total, tax_total, total, amount_paid, status, invoice_type,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :invoiceNumber, :studentId, :billToUserId, :billToName,
                    :invoiceDate, :dueDate, :currencyId, :subtotal, :discountTotal, :taxTotal, :total, 0, 'draft',
                    'invoice', :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            """;
    /** {@link #INSERT}, writing nothing when the id is taken (a concurrent writer of the same id waits, then skips). */
    private static final String INSERT_IF_ABSENT = INSERT + " ON CONFLICT (id) DO NOTHING";
    private static final String UPDATE = """
            UPDATE finance.invoice
               SET institution_id = :institutionId,
                   bill_to_name = :billToName,
                   invoice_date = :invoiceDate,
                   due_date = :dueDate,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'draft' AND deleted_at IS NULL
            """;
    /** finance.PostInvoice: allowed while status is draft. */
    private static final String TRANSITION_POST = """
            UPDATE finance.invoice
               SET status = 'posted', invoice_number = :invoiceNumber, posted_at = :now,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'draft' AND deleted_at IS NULL
            """;
    /** finance.CancelInvoice: allowed while status is draft. */
    private static final String TRANSITION_CANCEL = """
            UPDATE finance.invoice
               SET status = 'cancelled', cancelled_at = :now,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'draft' AND deleted_at IS NULL
            """;
    /** The money held for a posted invoice changed: its paid amount and the state that follows from it. */
    private static final String SETTLE = """
            UPDATE finance.invoice
               SET amount_paid = :amountPaid,
                   status = CASE WHEN :amountPaid >= total THEN 'paid'
                                 WHEN :amountPaid > 0 THEN 'partially_paid'
                                 ELSE 'posted' END,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status IN ('posted', 'partially_paid', 'paid') AND deleted_at IS NULL
            """;
    private static final String INSERT_LINES = """
            INSERT INTO finance.invoice_line
            (id, tenant_id, invoice_id, fee_head_id, description, quantity, unit_price, discount_amount, tax_amount,
             line_total, created_at, created_by, updated_at, updated_by, row_version)
            SELECT line.id, :tenantId, :invoiceId, line.fee_head_id, line.description, 1, line.unit_price,
                   line.discount_amount, 0, line.line_total, :now, :actor, :now, :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:feeHeadIds AS uuid[]), CAST(:descriptions AS varchar[]),
                          CAST(:unitPrices AS numeric[]), CAST(:discounts AS numeric[]), CAST(:totals AS numeric[]))
                   AS line (id, fee_head_id, description, unit_price, discount_amount, line_total)
            """;
    private static final String LINE_COUNT = """
            SELECT COUNT(*) FROM finance.invoice_line
             WHERE tenant_id = :tenantId
               AND invoice_id = :invoiceId AND deleted_at IS NULL
            """;
    private static final String FEE_HEAD_NAMES = """
            SELECT h.id, h.name FROM finance.fee_head h
             WHERE h.tenant_id = :tenantId
               AND h.id = ANY(:ids)
            """;
    private static final String INSTALLMENT_NUMBERS = """
            SELECT i.id, i.sequence FROM finance.fee_plan_installment i
             WHERE i.tenant_id = :tenantId
               AND i.id = ANY(:ids)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;
    private static final String REACHABLE_OR_OWN = """
             (:reachAll OR r.institution_id = ANY(:reachIds) OR r.student_id = ANY(:ownIds)
              OR r.bill_to_user_id = :caller)
            """;

    private final JdbcClient jdbc;

    public InvoiceRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<InvoiceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(InvoiceRepository::map).optional();
    }

    public Page<InvoiceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<InvoiceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(InvoiceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), InvoiceEntity::getId);
    }

    /** The invoices, locked for the rest of the transaction in id order. */
    public List<InvoiceEntity> lock(Collection<UUID> ids) {
        return jdbc.sql(LOCK).param("ids", ids.toArray(UUID[]::new)).query(InvoiceRepository::map).list();
    }

    public List<InvoiceEntity> ofStudent(UUID studentId) {
        return jdbc.sql(OF_STUDENT).param("studentId", studentId).query(InvoiceRepository::map).list();
    }

    /** The institution's open invoices (posted, not fully paid); only the named ones when {@code ids} is given. */
    public List<InvoiceEntity> openOfInstitution(UUID institutionId, Collection<UUID> ids) {
        return jdbc.sql(OPEN_OF_INSTITUTION).param("institutionId", institutionId)
                .param("everyOpen", ids.isEmpty()).param("ids", ids.toArray(UUID[]::new))
                .query(InvoiceRepository::map).list();
    }

    public void insert(InvoiceEntity e) {
        bind(jdbc.sql(INSERT), e).update();
    }

    /** Inserts the invoice unless its id is taken; whether it was written. */
    public boolean insertIfAbsent(InvoiceEntity e) {
        return bind(jdbc.sql(INSERT_IF_ABSENT), e).update() == 1;
    }

    /** The invoice with the id, live or deleted. */
    public Optional<InvoiceEntity> findAnyById(UUID id) {
        return jdbc.sql(FIND_ANY_BY_ID).param("id", id).query(InvoiceRepository::map).optional();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, InvoiceEntity e) {
        return spec
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("invoiceNumber", e.getInvoiceNumber())
                .param("studentId", e.getStudentId())
                .param("billToUserId", e.getBillToUserId())
                .param("billToName", e.getBillToName())
                .param("invoiceDate", e.getInvoiceDate())
                .param("dueDate", e.getDueDate())
                .param("currencyId", e.getCurrencyId())
                .param("subtotal", e.getSubtotal())
                .param("discountTotal", e.getDiscountTotal())
                .param("taxTotal", e.getTaxTotal())
                .param("total", e.getTotal())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion());
    }

    /** Updates a draft's writable fields under the ETag guard; 0 rows: changed since read, or no longer a draft. */
    public int update(InvoiceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("billToName", e.getBillToName())
                .param("invoiceDate", e.getInvoiceDate())
                .param("dueDate", e.getDueDate())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int post(UUID id, int expected, String invoiceNumber, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_POST).param("id", id).param("expected", expected)
                .param("invoiceNumber", invoiceNumber).param("now", now).param("actor", actor).update();
    }

    public int cancel(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL).param("id", id).param("expected", expected).param("now", now)
                .param("actor", actor).update();
    }

    /** Sets what a posted invoice holds (the ledger's figure) and its state; the row is locked by the caller. */
    public int settle(UUID id, BigDecimal amountPaid, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SETTLE).param("id", id).param("amountPaid", amountPaid).param("now", now)
                .param("actor", actor).update();
    }

    /** One line of an invoice raised from dues. */
    public record DueLine(UUID id, UUID feeHeadId, String description, BigDecimal unitPrice, BigDecimal discount,
            BigDecimal total) {
    }

    /** Writes an invoice's lines in one statement. */
    public void insertLines(UUID invoiceId, List<DueLine> lines, OffsetDateTime now, UUID actor) {
        jdbc.sql(INSERT_LINES)
                .param("invoiceId", invoiceId)
                .param("ids", lines.stream().map(DueLine::id).toArray(UUID[]::new))
                .param("feeHeadIds", lines.stream().map(DueLine::feeHeadId).toArray(UUID[]::new))
                .param("descriptions", lines.stream().map(DueLine::description).toArray(String[]::new))
                .param("unitPrices", lines.stream().map(DueLine::unitPrice).toArray(BigDecimal[]::new))
                .param("discounts", lines.stream().map(DueLine::discount).toArray(BigDecimal[]::new))
                .param("totals", lines.stream().map(DueLine::total).toArray(BigDecimal[]::new))
                .param("now", now)
                .param("actor", actor)
                .update();
    }

    public long lineCount(UUID invoiceId) {
        return jdbc.sql(LINE_COUNT).param("invoiceId", invoiceId).query(Long.class).single();
    }

    /** The names of fee heads (deleted ones included: a due keeps the head it was raised for). */
    public Map<UUID, String> feeHeadNames(Collection<UUID> ids) {
        return jdbc.sql(FEE_HEAD_NAMES).param("ids", ids.toArray(UUID[]::new))
                .query((rs, row) -> Map.entry(rs.getObject("id", UUID.class), rs.getString("name"))).list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** The numbers of installments (deleted ones included). */
    public Map<UUID, Integer> installmentNumbers(Collection<UUID> ids) {
        return jdbc.sql(INSTALLMENT_NUMBERS).param("ids", ids.toArray(UUID[]::new))
                .query((rs, row) -> Map.entry(rs.getObject("id", UUID.class), rs.getInt("sequence"))).list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Rows of the fee office's reach, of the caller's own students, or billed to the caller. */
    public static SqlFragment reachableOrOwn(DataScope reach, Collection<UUID> ownStudents, UUID caller) {
        return new SqlFragment(REACHABLE_OR_OWN, Map.of("reachAll", reach.all(), "reachIds", reach.institutionArray(),
                "ownIds", ownStudents.toArray(UUID[]::new), "caller", caller));
    }

    private static List<Object> sortValues(SortSpec sort, InvoiceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static InvoiceEntity map(ResultSet rs, int row) throws SQLException {
        return InvoiceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .invoiceNumber(rs.getString("invoice_number"))
                .studentId(rs.getObject("student_id", UUID.class))
                .billToUserId(rs.getObject("bill_to_user_id", UUID.class))
                .billToName(rs.getString("bill_to_name"))
                .invoiceDate(rs.getObject("invoice_date", LocalDate.class))
                .dueDate(rs.getObject("due_date", LocalDate.class))
                .currencyId(rs.getObject("currency_id", UUID.class))
                .subtotal(rs.getBigDecimal("subtotal"))
                .discountTotal(rs.getBigDecimal("discount_total"))
                .taxTotal(rs.getBigDecimal("tax_total"))
                .total(rs.getBigDecimal("total"))
                .amountPaid(rs.getBigDecimal("amount_paid"))
                .status(rs.getString("status"))
                .invoiceType(rs.getString("invoice_type"))
                .creditForInvoiceId(rs.getObject("credit_for_invoice_id", UUID.class))
                .postedAt(rs.getObject("posted_at", OffsetDateTime.class))
                .cancelledAt(rs.getObject("cancelled_at", OffsetDateTime.class))
                .documentFileId(rs.getObject("document_file_id", UUID.class))
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
