package com.mysms.admission.repository;
import com.mysms.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.domain.ApplicationGuardianEntity;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
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
 * Plain-JDBC access to admission.application_guardian (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class ApplicationGuardianRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("applicationId", "r.application_id", ValueType.UUID),
            field("relationship", "r.relationship", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("qualificationId", "r.qualification_id", ValueType.UUID),
            field("designation", "r.designation", ValueType.STRING),
            field("occupation", "r.occupation", ValueType.STRING),
            field("organisation", "r.organisation", ValueType.STRING),
            field("annualIncome", "r.annual_income", ValueType.DECIMAL),
            field("mobile", "r.mobile", ValueType.STRING),
            field("email", "r.email", ValueType.STRING),
            field("photoFileId", "r.photo_file_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ApplicationGuardianEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ApplicationGuardianEntity::getCreatedAt),
            Map.entry("updateTime", ApplicationGuardianEntity::getUpdatedAt),
            Map.entry("name", ApplicationGuardianEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.application_id, r.relationship, r.name, r.qualification_id, r.designation,
                   r.occupation, r.organisation, r.annual_income, r.mobile, r.email, r.national_id_enc,
                   r.photo_file_id, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM admission.application_guardian r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO admission.application_guardian
            (id, tenant_id, application_id, relationship, name, qualification_id, designation, occupation,
             organisation, annual_income, mobile, email, national_id_enc, photo_file_id, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :applicationId, :relationship, :name, :qualificationId, :designation,
                    :occupation, :organisation, :annualIncome, :mobile, :email, :nationalIdEnc, :photoFileId,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, application_id, relationship, name, qualification_id, designation, occupation,
                      organisation, annual_income, mobile, email, national_id_enc, photo_file_id, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE admission.application_guardian
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE admission.application_guardian
               SET relationship = :relationship,
                   name = :name,
                   qualification_id = :qualificationId,
                   designation = :designation,
                   occupation = :occupation,
                   organisation = :organisation,
                   annual_income = :annualIncome,
                   mobile = :mobile,
                   email = :email,
                   national_id_enc = :nationalIdEnc,
                   photo_file_id = :photoFileId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_APPLICATION_GUARDIAN_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM admission.application_guardian
                            WHERE tenant_id = :tenantId
                              AND application_id = :applicationId AND relationship = :relationship AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_APPLICATION_ID = """
            SELECT EXISTS (SELECT 1 FROM admission.application WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM admission.application p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.application_id IN (SELECT p0.id FROM admission.application p0 WHERE p0.tenant_id =
                 :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public ApplicationGuardianRepository(@Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ApplicationGuardianEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ApplicationGuardianRepository::map).optional();
    }

    public Page<ApplicationGuardianEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ApplicationGuardianEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ApplicationGuardianRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ApplicationGuardianEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ApplicationGuardianEntity insert(ApplicationGuardianEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("applicationId", e.getApplicationId())
                .param("relationship", e.getRelationship())
                .param("name", e.getName())
                .param("qualificationId", e.getQualificationId())
                .param("designation", e.getDesignation())
                .param("occupation", e.getOccupation())
                .param("organisation", e.getOrganisation())
                .param("annualIncome", e.getAnnualIncome())
                .param("mobile", e.getMobile())
                .param("email", e.getEmail())
                .param("nationalIdEnc", e.getNationalIdEnc())
                .param("photoFileId", e.getPhotoFileId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ApplicationGuardianRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ApplicationGuardianEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("relationship", e.getRelationship())
                .param("name", e.getName())
                .param("qualificationId", e.getQualificationId())
                .param("designation", e.getDesignation())
                .param("occupation", e.getOccupation())
                .param("organisation", e.getOrganisation())
                .param("annualIncome", e.getAnnualIncome())
                .param("mobile", e.getMobile())
                .param("email", e.getEmail())
                .param("nationalIdEnc", e.getNationalIdEnc())
                .param("photoFileId", e.getPhotoFileId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ApplicationGuardianEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_application_guardian_1: application_id, relationship where (deleted_at IS NULL). */
    public boolean takenApplicationIdRelationship1(ApplicationGuardianEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_APPLICATION_GUARDIAN_1_TAKEN)
                .param("applicationId", e.getApplicationId())
                .param("relationship", e.getRelationship())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced admission.application row exists (live). */
    public boolean existsApplicationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_APPLICATION_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through application_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.application_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ApplicationGuardianEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ApplicationGuardianEntity map(ResultSet rs, int row) throws SQLException {
        return ApplicationGuardianEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .applicationId(rs.getObject("application_id", UUID.class))
                .relationship(rs.getString("relationship"))
                .name(rs.getString("name"))
                .qualificationId(rs.getObject("qualification_id", UUID.class))
                .designation(rs.getString("designation"))
                .occupation(rs.getString("occupation"))
                .organisation(rs.getString("organisation"))
                .annualIncome(rs.getBigDecimal("annual_income"))
                .mobile(rs.getString("mobile"))
                .email(rs.getString("email"))
                .nationalIdEnc(rs.getString("national_id_enc"))
                .photoFileId(rs.getObject("photo_file_id", UUID.class))
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
