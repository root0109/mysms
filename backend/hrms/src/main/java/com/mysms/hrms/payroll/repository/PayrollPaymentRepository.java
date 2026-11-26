package com.mysms.hrms.payroll.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.hrms.payroll.config.PayrollDatabaseConfig;
import com.mysms.hrms.payroll.domain.PayrollPaymentEntity;
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
 * Plain-JDBC access to payroll.payroll_payment: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PayrollPaymentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("transactionNumber", "r.transaction_number", ValueType.STRING),
            field("paymentTypeId", "r.payment_type_id", ValueType.UUID),
            field("disbursementAccountId", "r.disbursement_account_id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("payslipId", "r.payslip_id", ValueType.UUID),
            field("transactionDate", "r.transaction_date", ValueType.DATE),
            field("amount", "r.amount", ValueType.DECIMAL),
            field("paymentMethod", "r.payment_method", ValueType.STRING),
            field("transactionDetails", "r.transaction_details", ValueType.STRING),
            field("referenceNumber", "r.reference_number", ValueType.STRING),
            field("attachmentFileId", "r.attachment_file_id", ValueType.UUID),
            field("remarks", "r.remarks", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PayrollPaymentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PayrollPaymentEntity::getCreatedAt),
            Map.entry("updateTime", PayrollPaymentEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.transaction_number, r.payment_type_id,
                   r.disbursement_account_id, r.staff_id, r.payslip_id, r.transaction_date, r.amount,
                   r.payment_method, r.transaction_details, r.reference_number, r.attachment_file_id, r.remarks,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM payroll.payroll_payment r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO payroll.payroll_payment
            (id, tenant_id, institution_id, transaction_number, payment_type_id, disbursement_account_id, staff_id,
             payslip_id, transaction_date, amount, payment_method, transaction_details, reference_number,
             attachment_file_id, remarks, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :transactionNumber, :paymentTypeId, :disbursementAccountId,
                    :staffId, :payslipId, :transactionDate, :amount, :paymentMethod, :transactionDetails,
                    :referenceNumber, :attachmentFileId, :remarks, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, institution_id, transaction_number, payment_type_id, disbursement_account_id,
                      staff_id, payslip_id, transaction_date, amount, payment_method, transaction_details,
                      reference_number, attachment_file_id, remarks, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE payroll.payroll_payment
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE payroll.payroll_payment
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PAYROLL_PAYMENT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM payroll.payroll_payment
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND transaction_number = :transactionNumber AND id <>
                                  :id)
            """;
    private static final String EXISTS_DISBURSEMENT_ACCOUNT_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.disbursement_account WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PAYMENT_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.payment_type WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PAYSLIP_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.payslip WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public PayrollPaymentRepository(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PayrollPaymentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PayrollPaymentRepository::map).optional();
    }

    public Page<PayrollPaymentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PayrollPaymentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PayrollPaymentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PayrollPaymentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PayrollPaymentEntity insert(PayrollPaymentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("transactionNumber", e.getTransactionNumber())
                .param("paymentTypeId", e.getPaymentTypeId())
                .param("disbursementAccountId", e.getDisbursementAccountId())
                .param("staffId", e.getStaffId())
                .param("payslipId", e.getPayslipId())
                .param("transactionDate", e.getTransactionDate())
                .param("amount", e.getAmount())
                .param("paymentMethod", e.getPaymentMethod())
                .param("transactionDetails", e.getTransactionDetails())
                .param("referenceNumber", e.getReferenceNumber())
                .param("attachmentFileId", e.getAttachmentFileId())
                .param("remarks", e.getRemarks())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PayrollPaymentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PayrollPaymentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PayrollPaymentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_payroll_payment_1: institution_id, transaction_number. */
    public boolean takenInstitutionIdTransactionNumber1(PayrollPaymentEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PAYROLL_PAYMENT_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("transactionNumber", e.getTransactionNumber())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced payroll.disbursement_account row exists (live). */
    public boolean existsDisbursementAccountId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_DISBURSEMENT_ACCOUNT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether the referenced payroll.payment_type row exists (live). */
    public boolean existsPaymentTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PAYMENT_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced payroll.payslip row exists (live). */
    public boolean existsPayslipId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PAYSLIP_ID).param("id", id).query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, PayrollPaymentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PayrollPaymentEntity map(ResultSet rs, int row) throws SQLException {
        return PayrollPaymentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .transactionNumber(rs.getString("transaction_number"))
                .paymentTypeId(rs.getObject("payment_type_id", UUID.class))
                .disbursementAccountId(rs.getObject("disbursement_account_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
                .payslipId(rs.getObject("payslip_id", UUID.class))
                .transactionDate(rs.getObject("transaction_date", LocalDate.class))
                .amount(rs.getBigDecimal("amount"))
                .paymentMethod(rs.getString("payment_method"))
                .transactionDetails(rs.getString("transaction_details"))
                .referenceNumber(rs.getString("reference_number"))
                .attachmentFileId(rs.getObject("attachment_file_id", UUID.class))
                .remarks(rs.getString("remarks"))
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
