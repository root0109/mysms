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
import com.mysms.finance.domain.FeeConcessionEntity;
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
 * Plain-JDBC access to finance.fee_concession (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class FeeConcessionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("feeHeadId", "r.fee_head_id", ValueType.UUID),
            field("concessionType", "r.concession_type", ValueType.STRING),
            field("concessionValue", "r.concession_value", ValueType.DECIMAL),
            field("reason", "r.reason", ValueType.STRING),
            field("approvedByUserId", "r.approved_by_user_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("validFrom", "r.valid_from", ValueType.DATE),
            field("validUntil", "r.valid_until", ValueType.DATE),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<FeeConcessionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", FeeConcessionEntity::getCreatedAt),
            Map.entry("updateTime", FeeConcessionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.student_id, r.academic_year_id, r.fee_head_id,
                   r.concession_type, r.concession_value, r.reason, r.approved_by_user_id, r.status, r.valid_from,
                   r.valid_until, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM finance.fee_concession r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO finance.fee_concession
            (id, tenant_id, institution_id, student_id, academic_year_id, fee_head_id, concession_type,
             concession_value, reason, status, valid_from, valid_until, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :studentId, :academicYearId, :feeHeadId, :concessionType,
                    :concessionValue, :reason, COALESCE(:status, 'active'::character varying), :validFrom,
                    :validUntil, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, student_id, academic_year_id, fee_head_id, concession_type,
                      concession_value, reason, approved_by_user_id, status, valid_from, valid_until, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE finance.fee_concession
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE finance.fee_concession
               SET institution_id = :institutionId,
                   fee_head_id = :feeHeadId,
                   concession_type = :concessionType,
                   concession_value = :concessionValue,
                   reason = :reason,
                   status = :status,
                   valid_from = :validFrom,
                   valid_until = :validUntil,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_FEE_HEAD_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.fee_head WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STUDENT_FEE_DUE_CONCESSION_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.student_fee_due WHERE tenant_id = :tenantId
                AND concession_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public FeeConcessionRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<FeeConcessionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(FeeConcessionRepository::map).optional();
    }

    public Page<FeeConcessionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<FeeConcessionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(FeeConcessionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), FeeConcessionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public FeeConcessionEntity insert(FeeConcessionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("feeHeadId", e.getFeeHeadId())
                .param("concessionType", e.getConcessionType())
                .param("concessionValue", e.getConcessionValue())
                .param("reason", e.getReason())
                .param("status", e.getStatus())
                .param("validFrom", e.getValidFrom())
                .param("validUntil", e.getValidUntil())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(FeeConcessionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(FeeConcessionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("feeHeadId", e.getFeeHeadId())
                .param("concessionType", e.getConcessionType())
                .param("concessionValue", e.getConcessionValue())
                .param("reason", e.getReason())
                .param("status", e.getStatus())
                .param("validFrom", e.getValidFrom())
                .param("validUntil", e.getValidUntil())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(FeeConcessionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced finance.fee_head row exists (live). */
    public boolean existsFeeHeadId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FEE_HEAD_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live finance.student_fee_due rows still reference the row. */
    public boolean referencedByStudentFeeDueConcessionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STUDENT_FEE_DUE_CONCESSION_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, FeeConcessionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static FeeConcessionEntity map(ResultSet rs, int row) throws SQLException {
        return FeeConcessionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .feeHeadId(rs.getObject("fee_head_id", UUID.class))
                .concessionType(rs.getString("concession_type"))
                .concessionValue(rs.getBigDecimal("concession_value"))
                .reason(rs.getString("reason"))
                .approvedByUserId(rs.getObject("approved_by_user_id", UUID.class))
                .status(rs.getString("status"))
                .validFrom(rs.getObject("valid_from", LocalDate.class))
                .validUntil(rs.getObject("valid_until", LocalDate.class))
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
