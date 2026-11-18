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
import com.mysms.finance.domain.StudentFeeDueEntity;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to finance.student_fee_due: live rows only, keyset paging over an allowlisted order, the
 * row_version guard on every update, the rows a fee run writes in one statement, and the locks the invoicing and
 * payment workflows take. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class StudentFeeDueRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("installmentId", "r.installment_id", ValueType.UUID),
            field("feeHeadId", "r.fee_head_id", ValueType.UUID),
            field("feeStructureId", "r.fee_structure_id", ValueType.UUID),
            field("invoiceId", "r.invoice_id", ValueType.UUID),
            field("currencyId", "r.currency_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("dueDate", "r.due_date", ValueType.DATE),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("amount", "r.amount", ValueType.DECIMAL));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StudentFeeDueEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StudentFeeDueEntity::getCreatedAt),
            Map.entry("updateTime", StudentFeeDueEntity::getUpdatedAt),
            Map.entry("amount", StudentFeeDueEntity::getAmount));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.student_id, r.program_enrollment_id,
                   r.student_enrollment_id, r.installment_id, r.fee_head_id, r.due_date, r.amount, r.currency_id,
                   r.discount_percent, r.fee_factor, r.status, r.invoice_id, r.academic_year_id, r.fee_structure_id,
                   r.concession_id, r.discount_amount, r.net_amount, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM finance.student_fee_due r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String LOCK = SELECT_LIVE + " AND r.id = ANY(:ids) ORDER BY r.id FOR UPDATE";
    private static final String OF_STUDENT = SELECT_LIVE + """
               AND r.student_id = :studentId
               AND (CAST(:academicYearId AS uuid) IS NULL OR r.academic_year_id = :academicYearId)
             ORDER BY r.due_date NULLS LAST, r.created_at, r.id
            """;
    private static final String OF_INVOICE = SELECT_LIVE + " AND r.invoice_id = :invoiceId ORDER BY r.due_date, r.id";
    private static final String UPDATE = """
            UPDATE finance.student_fee_due
               SET institution_id = :institutionId,
                   due_date = :dueDate,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** The dues of one student a fee run writes, in one statement; a slot already charged is left as it is. */
    private static final String INSERT_RUN = """
            INSERT INTO finance.student_fee_due
            (id, tenant_id, institution_id, student_id, program_enrollment_id, student_enrollment_id, installment_id,
             fee_head_id, due_date, amount, currency_id, discount_percent, status, academic_year_id, fee_structure_id,
             concession_id, discount_amount, net_amount, created_at, created_by, updated_at, updated_by, row_version)
            SELECT due.id, :tenantId, :institutionId, :studentId, :programEnrollmentId, :studentEnrollmentId,
                   due.installment_id, due.fee_head_id, due.due_date, due.amount, :currencyId, :discountPercent,
                   'draft', :academicYearId, :feeStructureId, due.concession_id, due.discount_amount, due.net_amount,
                   :now, :actor, :now, :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:installmentIds AS uuid[]), CAST(:feeHeadIds AS uuid[]),
                          CAST(:dueDates AS date[]), CAST(:amounts AS numeric[]), CAST(:concessionIds AS uuid[]),
                          CAST(:discountAmounts AS numeric[]), CAST(:netAmounts AS numeric[]))
                   AS due (id, installment_id, fee_head_id, due_date, amount, concession_id, discount_amount,
                           net_amount)
            ON CONFLICT (tenant_id, student_id, academic_year_id, installment_id, fee_head_id)
               WHERE deleted_at IS NULL AND status <> 'cancelled'
                     AND (installment_id IS NOT NULL OR fee_head_id IS NOT NULL)
               DO NOTHING
            RETURNING id
            """;
    private static final String MARK_INVOICED = """
            UPDATE finance.student_fee_due
               SET status = 'invoiced', invoice_id = :invoiceId, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = ANY(:ids) AND status = 'draft' AND invoice_id IS NULL AND deleted_at IS NULL
            """;
    private static final String RELEASE = """
            UPDATE finance.student_fee_due
               SET status = 'draft', invoice_id = NULL, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND invoice_id = :invoiceId AND deleted_at IS NULL
            """;
    private static final String WITH_ALLOCATIONS = """
            SELECT DISTINCT a.student_fee_due_id
              FROM finance.payment_allocation a
             WHERE a.tenant_id = :tenantId
               AND a.student_fee_due_id = ANY(:ids) AND a.deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;
    private static final String REACHABLE_OR_OWN = """
             (:reachAll OR r.institution_id = ANY(:reachIds) OR r.student_id = ANY(:ownIds))
            """;

    private final JdbcClient jdbc;

    public StudentFeeDueRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StudentFeeDueEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StudentFeeDueRepository::map).optional();
    }

    public Page<StudentFeeDueEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StudentFeeDueEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StudentFeeDueRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StudentFeeDueEntity::getId);
    }

    /** The dues, locked for the rest of the transaction in id order (so two workflows never deadlock). */
    public List<StudentFeeDueEntity> lock(Collection<UUID> ids) {
        return jdbc.sql(LOCK).param("ids", ids.toArray(UUID[]::new)).query(StudentFeeDueRepository::map).list();
    }

    /** A student's live dues, of one academic year when given, in due-date order. */
    public List<StudentFeeDueEntity> ofStudent(UUID studentId, UUID academicYearId) {
        return jdbc.sql(OF_STUDENT).param("studentId", studentId).param("academicYearId", academicYearId)
                .query(StudentFeeDueRepository::map).list();
    }

    /** The dues an invoice bills. */
    public List<StudentFeeDueEntity> ofInvoice(UUID invoiceId) {
        return jdbc.sql(OF_INVOICE).param("invoiceId", invoiceId).query(StudentFeeDueRepository::map).list();
    }

    /** Updates the writable fields under the ETag guard; 0 rows means the row changed since it was read. */
    public int update(StudentFeeDueEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("dueDate", e.getDueDate())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** One student's dues of a run; answers the ids written (a slot already charged is skipped). */
    public List<UUID> insertRun(DueRun run, List<RunDue> dues, OffsetDateTime now, UUID actor) {
        return jdbc.sql(INSERT_RUN)
                .param("institutionId", run.institutionId())
                .param("studentId", run.studentId())
                .param("programEnrollmentId", run.programEnrollmentId())
                .param("studentEnrollmentId", run.studentEnrollmentId())
                .param("currencyId", run.currencyId())
                .param("discountPercent", run.discountPercent())
                .param("academicYearId", run.academicYearId())
                .param("feeStructureId", run.feeStructureId())
                .param("ids", dues.stream().map(RunDue::id).toArray(UUID[]::new))
                .param("installmentIds", dues.stream().map(RunDue::installmentId).toArray(UUID[]::new))
                .param("feeHeadIds", dues.stream().map(RunDue::feeHeadId).toArray(UUID[]::new))
                .param("dueDates", dues.stream().map(due -> due.dueDate() == null ? null : due.dueDate().toString())
                        .toArray(String[]::new))
                .param("amounts", dues.stream().map(RunDue::amount).toArray(BigDecimal[]::new))
                .param("concessionIds", dues.stream().map(RunDue::concessionId).toArray(UUID[]::new))
                .param("discountAmounts", dues.stream().map(RunDue::discountAmount).toArray(BigDecimal[]::new))
                .param("netAmounts", dues.stream().map(RunDue::netAmount).toArray(BigDecimal[]::new))
                .param("now", now)
                .param("actor", actor)
                .query((rs, row) -> rs.getObject("id", UUID.class)).list();
    }

    /** What one student's dues of a run share. */
    public record DueRun(UUID institutionId, UUID studentId, UUID programEnrollmentId, UUID studentEnrollmentId,
            UUID currencyId, BigDecimal discountPercent, UUID academicYearId, UUID feeStructureId) {
    }

    /** One due of a run. */
    public record RunDue(UUID id, UUID installmentId, UUID feeHeadId, LocalDate dueDate, BigDecimal amount,
            UUID concessionId, BigDecimal discountAmount, BigDecimal netAmount) {
    }

    /** Bills the draft dues on the invoice; answers how many changed. */
    public int markInvoiced(Collection<UUID> ids, UUID invoiceId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(MARK_INVOICED).param("ids", ids.toArray(UUID[]::new)).param("invoiceId", invoiceId)
                .param("now", now).param("actor", actor).update();
    }

    /** Releases the dues of a cancelled invoice, so they can be billed again. */
    public int release(UUID invoiceId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(RELEASE).param("invoiceId", invoiceId).param("now", now).param("actor", actor).update();
    }

    /** Which of these dues a payment settles (in part). */
    public Set<UUID> withAllocations(Collection<UUID> ids) {
        return Set.copyOf(jdbc.sql(WITH_ALLOCATIONS).param("ids", ids.toArray(UUID[]::new))
                .query((rs, row) -> rs.getObject(1, UUID.class)).list());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Rows of the fee office's reach, or of the caller's own students. */
    public static SqlFragment reachableOrOwn(DataScope reach, Collection<UUID> ownStudents) {
        return new SqlFragment(REACHABLE_OR_OWN, Map.of("reachAll", reach.all(), "reachIds", reach.institutionArray(),
                "ownIds", ownStudents.toArray(UUID[]::new)));
    }

    private static List<Object> sortValues(SortSpec sort, StudentFeeDueEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StudentFeeDueEntity map(ResultSet rs, int row) throws SQLException {
        return StudentFeeDueEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .programEnrollmentId(rs.getObject("program_enrollment_id", UUID.class))
                .studentEnrollmentId(rs.getObject("student_enrollment_id", UUID.class))
                .installmentId(rs.getObject("installment_id", UUID.class))
                .feeHeadId(rs.getObject("fee_head_id", UUID.class))
                .dueDate(rs.getObject("due_date", LocalDate.class))
                .amount(rs.getBigDecimal("amount"))
                .currencyId(rs.getObject("currency_id", UUID.class))
                .discountPercent(rs.getBigDecimal("discount_percent"))
                .feeFactor(rs.getBigDecimal("fee_factor"))
                .status(rs.getString("status"))
                .invoiceId(rs.getObject("invoice_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .feeStructureId(rs.getObject("fee_structure_id", UUID.class))
                .concessionId(rs.getObject("concession_id", UUID.class))
                .discountAmount(rs.getBigDecimal("discount_amount"))
                .netAmount(rs.getBigDecimal("net_amount"))
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
