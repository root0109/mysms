package com.mysms.payroll.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.domain.DisbursementAccountEntity;
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
 * Plain-JDBC access to payroll.disbursement_account (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class DisbursementAccountRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("managedByUserId", "r.managed_by_user_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("accountKey", "r.account_key", ValueType.STRING),
            field("bankRoutingCode", "r.bank_routing_code", ValueType.STRING),
            field("branchName", "r.branch_name", ValueType.STRING),
            field("extraReference", "r.extra_reference", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<DisbursementAccountEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", DisbursementAccountEntity::getCreatedAt),
            Map.entry("updateTime", DisbursementAccountEntity::getUpdatedAt),
            Map.entry("name", DisbursementAccountEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.managed_by_user_id, r.name, r.account_key,
                   r.account_number_enc, r.bank_routing_code, r.branch_name, r.extra_reference, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM payroll.disbursement_account r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO payroll.disbursement_account
            (id, tenant_id, institution_id, managed_by_user_id, name, account_key, account_number_enc,
             bank_routing_code, branch_name, extra_reference, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :institutionId, :managedByUserId, :name, :accountKey, :accountNumberEnc,
                    :bankRoutingCode, :branchName, :extraReference, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, institution_id, managed_by_user_id, name, account_key, account_number_enc,
                      bank_routing_code, branch_name, extra_reference, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE payroll.disbursement_account
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE payroll.disbursement_account
               SET institution_id = :institutionId,
                   managed_by_user_id = :managedByUserId,
                   name = :name,
                   account_key = :accountKey,
                   account_number_enc = :accountNumberEnc,
                   bank_routing_code = :bankRoutingCode,
                   branch_name = :branchName,
                   extra_reference = :extraReference,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_DISBURSEMENT_ACCOUNT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM payroll.disbursement_account
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND account_key = :accountKey AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_PAYROLL_PAYMENT_DISBURSEMENT_ACCOUNT_ID = """
            SELECT EXISTS (SELECT 1 FROM payroll.payroll_payment WHERE tenant_id = :tenantId
                AND disbursement_account_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public DisbursementAccountRepository(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<DisbursementAccountEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(DisbursementAccountRepository::map).optional();
    }

    public Page<DisbursementAccountEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<DisbursementAccountEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(DisbursementAccountRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), DisbursementAccountEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public DisbursementAccountEntity insert(DisbursementAccountEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("managedByUserId", e.getManagedByUserId())
                .param("name", e.getName())
                .param("accountKey", e.getAccountKey())
                .param("accountNumberEnc", e.getAccountNumberEnc())
                .param("bankRoutingCode", e.getBankRoutingCode())
                .param("branchName", e.getBranchName())
                .param("extraReference", e.getExtraReference())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(DisbursementAccountRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(DisbursementAccountEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("managedByUserId", e.getManagedByUserId())
                .param("name", e.getName())
                .param("accountKey", e.getAccountKey())
                .param("accountNumberEnc", e.getAccountNumberEnc())
                .param("bankRoutingCode", e.getBankRoutingCode())
                .param("branchName", e.getBranchName())
                .param("extraReference", e.getExtraReference())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(DisbursementAccountEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_disbursement_account_1: institution_id, account_key where (deleted_at IS NULL). */
    public boolean takenInstitutionIdAccountKey1(DisbursementAccountEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_DISBURSEMENT_ACCOUNT_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("accountKey", e.getAccountKey())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live payroll.payroll_payment rows still reference the row. */
    public boolean referencedByPayrollPaymentDisbursementAccountId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_PAYROLL_PAYMENT_DISBURSEMENT_ACCOUNT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, DisbursementAccountEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static DisbursementAccountEntity map(ResultSet rs, int row) throws SQLException {
        return DisbursementAccountEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .managedByUserId(rs.getObject("managed_by_user_id", UUID.class))
                .name(rs.getString("name"))
                .accountKey(rs.getString("account_key"))
                .accountNumberEnc(rs.getString("account_number_enc"))
                .bankRoutingCode(rs.getString("bank_routing_code"))
                .branchName(rs.getString("branch_name"))
                .extraReference(rs.getString("extra_reference"))
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
