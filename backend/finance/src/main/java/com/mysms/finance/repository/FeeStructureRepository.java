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
import com.mysms.finance.domain.FeeStructureEntity;
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
 * Plain-JDBC access to finance.fee_structure: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class FeeStructureRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("gradeLevelId", "r.grade_level_id", ValueType.UUID),
            field("programId", "r.program_id", ValueType.UUID),
            field("feePlanId", "r.fee_plan_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<FeeStructureEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", FeeStructureEntity::getCreatedAt),
            Map.entry("updateTime", FeeStructureEntity::getUpdatedAt),
            Map.entry("name", FeeStructureEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.grade_level_id, r.program_id,
                   r.fee_plan_id, r.name, r.status, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM finance.fee_structure r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO finance.fee_structure
            (id, tenant_id, institution_id, academic_year_id, grade_level_id, program_id, fee_plan_id, name,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId, :gradeLevelId, :programId, :feePlanId, :name,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, grade_level_id, program_id, fee_plan_id, name,
                      status, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE finance.fee_structure
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE finance.fee_structure
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   grade_level_id = :gradeLevelId,
                   program_id = :programId,
                   fee_plan_id = :feePlanId,
                   name = :name,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_FEE_STRUCTURE_SCOPE_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM finance.fee_structure
                            WHERE tenant_id = :tenantId
                              AND academic_year_id IS NOT DISTINCT FROM :academicYearId AND grade_level_id IS NOT
                                  DISTINCT FROM :gradeLevelId AND program_id IS NOT DISTINCT FROM :programId AND
                                  fee_plan_id IS NOT DISTINCT FROM :feePlanId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_FEE_PLAN_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.fee_plan WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STUDENT_FEE_DUE_FEE_STRUCTURE_ID = """
            SELECT EXISTS (SELECT 1 FROM finance.student_fee_due WHERE tenant_id = :tenantId
                AND fee_structure_id = :id AND deleted_at IS NULL)
            """;

    /** finance.ArchiveFeeStructure: allowed while status is published. */
    private static final String TRANSITION_ARCHIVE_FEE_STRUCTURE = """
            UPDATE finance.fee_structure
               SET status = 'archived',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    /** finance.PublishFeeStructure: allowed while status is draft (its dues are generated first). */
    private static final String TRANSITION_PUBLISH_FEE_STRUCTURE = """
            UPDATE finance.fee_structure
               SET status = 'published',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'draft' AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public FeeStructureRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<FeeStructureEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(FeeStructureRepository::map).optional();
    }

    public Page<FeeStructureEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<FeeStructureEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(FeeStructureRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), FeeStructureEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public FeeStructureEntity insert(FeeStructureEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("programId", e.getProgramId())
                .param("feePlanId", e.getFeePlanId())
                .param("name", e.getName())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(FeeStructureRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(FeeStructureEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("programId", e.getProgramId())
                .param("feePlanId", e.getFeePlanId())
                .param("name", e.getName())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(FeeStructureEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_fee_structure_scope: academic_year_id, grade_level_id, program_id, fee_plan_id where (deleted_at IS NULL). */
    public boolean takenAcademicYearIdGradeLevelIdProgramIdFeePlanIdScope(FeeStructureEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_FEE_STRUCTURE_SCOPE_TAKEN)
                .param("academicYearId", e.getAcademicYearId())
                .param("gradeLevelId", e.getGradeLevelId())
                .param("programId", e.getProgramId())
                .param("feePlanId", e.getFeePlanId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced finance.fee_plan row exists (live). */
    public boolean existsFeePlanId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FEE_PLAN_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live finance.student_fee_due rows still reference the row. */
    public boolean referencedByStudentFeeDueFeeStructureId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STUDENT_FEE_DUE_FEE_STRUCTURE_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** finance.ArchiveFeeStructure; 0 rows means the row changed (or vanished) since it was read. */
    public int archiveFeeStructure(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_ARCHIVE_FEE_STRUCTURE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** finance.PublishFeeStructure; 0 rows means the row changed (or left draft) since it was read. */
    public int publishFeeStructure(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_PUBLISH_FEE_STRUCTURE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, FeeStructureEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static FeeStructureEntity map(ResultSet rs, int row) throws SQLException {
        return FeeStructureEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .gradeLevelId(rs.getObject("grade_level_id", UUID.class))
                .programId(rs.getObject("program_id", UUID.class))
                .feePlanId(rs.getObject("fee_plan_id", UUID.class))
                .name(rs.getString("name"))
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
