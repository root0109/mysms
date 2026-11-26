package com.mysms.students.careers.repository;
import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.students.careers.domain.CandidatePreferenceEntity;
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
 * Plain-JDBC access to careers.candidate_preference: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CandidatePreferenceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("personId", "r.person_id", ValueType.UUID),
            field("openTo", "r.open_to", ValueType.STRING),
            field("preferredLocation", "r.preferred_location", ValueType.STRING),
            field("dreamCompanies", "r.dream_companies", ValueType.STRING),
            field("attire", "r.attire", ValueType.STRING),
            field("collaboration", "r.collaboration", ValueType.STRING),
            field("roleClarity", "r.role_clarity", ValueType.STRING),
            field("locationPreference", "r.location_preference", ValueType.STRING),
            field("workHours", "r.work_hours", ValueType.STRING),
            field("companyType", "r.company_type", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CandidatePreferenceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CandidatePreferenceEntity::getCreatedAt),
            Map.entry("updateTime", CandidatePreferenceEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.person_id, r.open_to, r.preferred_location, r.dream_companies, r.attire,
                   r.collaboration, r.role_clarity, r.location_preference, r.work_hours, r.company_type, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM careers.candidate_preference r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO careers.candidate_preference
            (id, tenant_id, person_id, open_to, preferred_location, dream_companies, attire, collaboration,
             role_clarity, location_preference, work_hours, company_type, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :personId, :openTo, :preferredLocation, :dreamCompanies, :attire, :collaboration,
                    :roleClarity, :locationPreference, :workHours, :companyType, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, person_id, open_to, preferred_location, dream_companies, attire, collaboration,
                      role_clarity, location_preference, work_hours, company_type, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE careers.candidate_preference
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE careers.candidate_preference
               SET person_id = :personId,
                   open_to = :openTo,
                   preferred_location = :preferredLocation,
                   dream_companies = :dreamCompanies,
                   attire = :attire,
                   collaboration = :collaboration,
                   role_clarity = :roleClarity,
                   location_preference = :locationPreference,
                   work_hours = :workHours,
                   company_type = :companyType,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_CANDIDATE_PREFERENCE_PERSON_ID_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM careers.candidate_preference
                            WHERE tenant_id = :tenantId
                              AND person_id = :personId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String REFERENCED_BY_CANDIDATE_PREFERRED_FUNCTION_CANDIDATE_PREFERENCE_ID = """
            SELECT EXISTS (SELECT 1 FROM careers.candidate_preferred_function WHERE tenant_id = :tenantId
                AND candidate_preference_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CANDIDATE_PREFERRED_INDUSTRY_CANDIDATE_PREFERENCE_ID = """
            SELECT EXISTS (SELECT 1 FROM careers.candidate_preferred_industry WHERE tenant_id = :tenantId
                AND candidate_preference_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public CandidatePreferenceRepository(@Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CandidatePreferenceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CandidatePreferenceRepository::map).optional();
    }

    public Page<CandidatePreferenceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CandidatePreferenceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CandidatePreferenceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CandidatePreferenceEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CandidatePreferenceEntity insert(CandidatePreferenceEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("personId", e.getPersonId())
                .param("openTo", e.getOpenTo())
                .param("preferredLocation", e.getPreferredLocation())
                .param("dreamCompanies", e.getDreamCompanies())
                .param("attire", e.getAttire())
                .param("collaboration", e.getCollaboration())
                .param("roleClarity", e.getRoleClarity())
                .param("locationPreference", e.getLocationPreference())
                .param("workHours", e.getWorkHours())
                .param("companyType", e.getCompanyType())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CandidatePreferenceRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CandidatePreferenceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("personId", e.getPersonId())
                .param("openTo", e.getOpenTo())
                .param("preferredLocation", e.getPreferredLocation())
                .param("dreamCompanies", e.getDreamCompanies())
                .param("attire", e.getAttire())
                .param("collaboration", e.getCollaboration())
                .param("roleClarity", e.getRoleClarity())
                .param("locationPreference", e.getLocationPreference())
                .param("workHours", e.getWorkHours())
                .param("companyType", e.getCompanyType())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CandidatePreferenceEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_candidate_preference_person_id: person_id where (deleted_at IS NULL). */
    public boolean takenPersonIdId(CandidatePreferenceEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_CANDIDATE_PREFERENCE_PERSON_ID_TAKEN).param("personId", e.getPersonId())
                .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live careers.candidate_preferred_function rows still reference the row. */
    public boolean referencedByCandidatePreferredFunctionCandidatePreferenceId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CANDIDATE_PREFERRED_FUNCTION_CANDIDATE_PREFERENCE_ID)
                .param("id", id).query(Boolean.class).single());
    }

    /** Whether live careers.candidate_preferred_industry rows still reference the row. */
    public boolean referencedByCandidatePreferredIndustryCandidatePreferenceId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CANDIDATE_PREFERRED_INDUSTRY_CANDIDATE_PREFERENCE_ID)
                .param("id", id).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, CandidatePreferenceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CandidatePreferenceEntity map(ResultSet rs, int row) throws SQLException {
        return CandidatePreferenceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .personId(rs.getObject("person_id", UUID.class))
                .openTo(rs.getString("open_to"))
                .preferredLocation(rs.getString("preferred_location"))
                .dreamCompanies(rs.getString("dream_companies"))
                .attire(rs.getString("attire"))
                .collaboration(rs.getString("collaboration"))
                .roleClarity(rs.getString("role_clarity"))
                .locationPreference(rs.getString("location_preference"))
                .workHours(rs.getString("work_hours"))
                .companyType(rs.getString("company_type"))
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
