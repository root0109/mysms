package com.mysms.sis.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.domain.GuardianEntity;
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
 * Plain-JDBC access to sis.guardian (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live rows
 * only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class GuardianRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("personId", "r.person_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("profession", "r.profession", ValueType.STRING),
            field("subOccupation", "r.sub_occupation", ValueType.STRING),
            field("designation", "r.designation", ValueType.STRING),
            field("organizationName", "r.organization_name", ValueType.STRING),
            field("annualIncome", "r.annual_income", ValueType.DECIMAL),
            field("qualificationId", "r.qualification_id", ValueType.UUID),
            field("mobile", "r.mobile", ValueType.STRING),
            field("email", "r.email", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<GuardianEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", GuardianEntity::getCreatedAt),
            Map.entry("updateTime", GuardianEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.person_id, r.user_id, r.profession, r.sub_occupation, r.designation,
                   r.organization_name, r.annual_income, r.qualification_id, r.mobile, r.email, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM sis.guardian r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO sis.guardian
            (id, tenant_id, person_id, user_id, profession, sub_occupation, designation, organization_name,
             annual_income, qualification_id, mobile, email, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :personId, :userId, :profession, :subOccupation, :designation, :organizationName,
                    :annualIncome, :qualificationId, :mobile, :email, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, person_id, user_id, profession, sub_occupation, designation, organization_name,
                      annual_income, qualification_id, mobile, email, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE sis.guardian
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE sis.guardian
               SET person_id = :personId,
                   user_id = :userId,
                   profession = :profession,
                   sub_occupation = :subOccupation,
                   designation = :designation,
                   organization_name = :organizationName,
                   annual_income = :annualIncome,
                   qualification_id = :qualificationId,
                   mobile = :mobile,
                   email = :email,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_GUARDIAN_PERSON_ID_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM sis.guardian
                            WHERE tenant_id = :tenantId
                              AND person_id = :personId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String UQ_GUARDIAN_USER_ID_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM sis.guardian
                            WHERE tenant_id = :tenantId
                              AND user_id = :userId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_STUDENT_GUARDIAN_GUARDIAN_ID = """
            SELECT EXISTS (SELECT 1 FROM sis.student_guardian WHERE tenant_id = :tenantId
                AND guardian_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public GuardianRepository(@Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<GuardianEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(GuardianRepository::map).optional();
    }

    public Page<GuardianEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<GuardianEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(GuardianRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), GuardianEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public GuardianEntity insert(GuardianEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("personId", e.getPersonId())
                .param("userId", e.getUserId())
                .param("profession", e.getProfession())
                .param("subOccupation", e.getSubOccupation())
                .param("designation", e.getDesignation())
                .param("organizationName", e.getOrganizationName())
                .param("annualIncome", e.getAnnualIncome())
                .param("qualificationId", e.getQualificationId())
                .param("mobile", e.getMobile())
                .param("email", e.getEmail())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(GuardianRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(GuardianEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("personId", e.getPersonId())
                .param("userId", e.getUserId())
                .param("profession", e.getProfession())
                .param("subOccupation", e.getSubOccupation())
                .param("designation", e.getDesignation())
                .param("organizationName", e.getOrganizationName())
                .param("annualIncome", e.getAnnualIncome())
                .param("qualificationId", e.getQualificationId())
                .param("mobile", e.getMobile())
                .param("email", e.getEmail())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(GuardianEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_guardian_person_id: person_id where (deleted_at IS NULL). */
    public boolean takenPersonIdId(GuardianEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_GUARDIAN_PERSON_ID_TAKEN).param("personId", e.getPersonId())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** uq_guardian_user_id: user_id where (deleted_at IS NULL). */
    public boolean takenUserIdId(GuardianEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_GUARDIAN_USER_ID_TAKEN).param("userId", e.getUserId())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live sis.student_guardian rows still reference the row. */
    public boolean referencedByStudentGuardianGuardianId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STUDENT_GUARDIAN_GUARDIAN_ID).param("id", id)
                .query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, GuardianEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static GuardianEntity map(ResultSet rs, int row) throws SQLException {
        return GuardianEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .personId(rs.getObject("person_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .profession(rs.getString("profession"))
                .subOccupation(rs.getString("sub_occupation"))
                .designation(rs.getString("designation"))
                .organizationName(rs.getString("organization_name"))
                .annualIncome(rs.getBigDecimal("annual_income"))
                .qualificationId(rs.getObject("qualification_id", UUID.class))
                .mobile(rs.getString("mobile"))
                .email(rs.getString("email"))
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
