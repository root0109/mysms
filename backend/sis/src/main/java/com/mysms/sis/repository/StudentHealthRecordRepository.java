package com.mysms.sis.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.domain.StudentHealthRecordEntity;
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
 * Plain-JDBC access to sis.student_health_record: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class StudentHealthRecordRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("heightCm", "r.height_cm", ValueType.DECIMAL),
            field("weightKg", "r.weight_kg", ValueType.DECIMAL),
            field("medicalConditions", "r.medical_conditions", ValueType.STRING),
            field("medicationNeeds", "r.medication_needs", ValueType.STRING),
            field("medicationAllergies", "r.medication_allergies", ValueType.STRING),
            field("foodAllergies", "r.food_allergies", ValueType.STRING),
            field("otherAllergies", "r.other_allergies", ValueType.STRING),
            field("otherMedicalInfo", "r.other_medical_info", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StudentHealthRecordEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StudentHealthRecordEntity::getCreatedAt),
            Map.entry("updateTime", StudentHealthRecordEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.student_id, r.academic_year_id, r.height_cm, r.weight_kg,
                   r.medical_conditions, r.medication_needs, r.medication_allergies, r.food_allergies,
                   r.other_allergies, r.other_medical_info, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM sis.student_health_record r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO sis.student_health_record
            (id, tenant_id, student_id, academic_year_id, height_cm, weight_kg, medical_conditions, medication_needs,
             medication_allergies, food_allergies, other_allergies, other_medical_info, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :studentId, :academicYearId, :heightCm, :weightKg, :medicalConditions,
                    :medicationNeeds, :medicationAllergies, :foodAllergies, :otherAllergies, :otherMedicalInfo,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, student_id, academic_year_id, height_cm, weight_kg, medical_conditions,
                      medication_needs, medication_allergies, food_allergies, other_allergies, other_medical_info,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE sis.student_health_record
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE sis.student_health_record
               SET student_id = :studentId,
                   academic_year_id = :academicYearId,
                   height_cm = :heightCm,
                   weight_kg = :weightKg,
                   medical_conditions = :medicalConditions,
                   medication_needs = :medicationNeeds,
                   medication_allergies = :medicationAllergies,
                   food_allergies = :foodAllergies,
                   other_allergies = :otherAllergies,
                   other_medical_info = :otherMedicalInfo,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_STUDENT_HEALTH_RECORD_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM sis.student_health_record
                            WHERE tenant_id = :tenantId
                              AND student_id = :studentId AND academic_year_id = :academicYearId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_STUDENT_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.student WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM sis.student p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.student_id IN (SELECT p0.id FROM sis.student p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public StudentHealthRecordRepository(@Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StudentHealthRecordEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StudentHealthRecordRepository::map).optional();
    }

    public Page<StudentHealthRecordEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StudentHealthRecordEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StudentHealthRecordRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StudentHealthRecordEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StudentHealthRecordEntity insert(StudentHealthRecordEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("heightCm", e.getHeightCm())
                .param("weightKg", e.getWeightKg())
                .param("medicalConditions", e.getMedicalConditions())
                .param("medicationNeeds", e.getMedicationNeeds())
                .param("medicationAllergies", e.getMedicationAllergies())
                .param("foodAllergies", e.getFoodAllergies())
                .param("otherAllergies", e.getOtherAllergies())
                .param("otherMedicalInfo", e.getOtherMedicalInfo())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StudentHealthRecordRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StudentHealthRecordEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("heightCm", e.getHeightCm())
                .param("weightKg", e.getWeightKg())
                .param("medicalConditions", e.getMedicalConditions())
                .param("medicationNeeds", e.getMedicationNeeds())
                .param("medicationAllergies", e.getMedicationAllergies())
                .param("foodAllergies", e.getFoodAllergies())
                .param("otherAllergies", e.getOtherAllergies())
                .param("otherMedicalInfo", e.getOtherMedicalInfo())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StudentHealthRecordEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_student_health_record_1: student_id, academic_year_id where (deleted_at IS NULL). */
    public boolean takenStudentIdAcademicYearId1(StudentHealthRecordEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_STUDENT_HEALTH_RECORD_1_TAKEN)
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced sis.student row exists (live). */
    public boolean existsStudentId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_STUDENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through student_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, StudentHealthRecordEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StudentHealthRecordEntity map(ResultSet rs, int row) throws SQLException {
        return StudentHealthRecordEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .heightCm(rs.getBigDecimal("height_cm"))
                .weightKg(rs.getBigDecimal("weight_kg"))
                .medicalConditions(rs.getString("medical_conditions"))
                .medicationNeeds(rs.getString("medication_needs"))
                .medicationAllergies(rs.getString("medication_allergies"))
                .foodAllergies(rs.getString("food_allergies"))
                .otherAllergies(rs.getString("other_allergies"))
                .otherMedicalInfo(rs.getString("other_medical_info"))
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
