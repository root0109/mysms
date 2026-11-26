package com.mysms.identity.people.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.identity.people.config.PeopleDatabaseConfig;
import com.mysms.identity.people.domain.WorkExperienceEntity;
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
 * Plain-JDBC access to people.work_experience: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class WorkExperienceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("personId", "r.person_id", ValueType.UUID),
            field("experienceType", "r.experience_type", ValueType.STRING),
            field("title", "r.title", ValueType.STRING),
            field("company", "r.company", ValueType.STRING),
            field("location", "r.location", ValueType.STRING),
            field("fromDate", "r.from_date", ValueType.DATE),
            field("toDate", "r.to_date", ValueType.DATE),
            field("isCurrent", "r.is_current", ValueType.BOOLEAN),
            field("description", "r.description", ValueType.STRING),
            field("companyWebsite", "r.company_website", ValueType.STRING),
            field("industry", "r.industry", ValueType.STRING),
            field("responsibilities", "r.responsibilities", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING),
            field("sortOrder", "r.sort_order", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<WorkExperienceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", WorkExperienceEntity::getCreatedAt),
            Map.entry("updateTime", WorkExperienceEntity::getUpdatedAt),
            Map.entry("title", WorkExperienceEntity::getTitle),
            Map.entry("sortOrder", WorkExperienceEntity::getSortOrder));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.person_id, r.experience_type, r.title, r.company, r.location, r.from_date,
                   r.to_date, r.is_current, r.description, r.company_website, r.industry, r.responsibilities,
                   r.sort_order, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM people.work_experience r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO people.work_experience
            (id, tenant_id, person_id, experience_type, title, company, location, from_date, to_date, is_current,
             description, company_website, industry, responsibilities, sort_order, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :personId, COALESCE(:experienceType, 'job'::character varying), :title, :company,
                    :location, :fromDate, :toDate, COALESCE(:isCurrent, false), :description, :companyWebsite,
                    :industry, :responsibilities, COALESCE(:sortOrder, 0), :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, person_id, experience_type, title, company, location, from_date, to_date,
                      is_current, description, company_website, industry, responsibilities, sort_order, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE people.work_experience
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE people.work_experience
               SET experience_type = :experienceType,
                   title = :title,
                   company = :company,
                   location = :location,
                   from_date = :fromDate,
                   to_date = :toDate,
                   is_current = :isCurrent,
                   description = :description,
                   company_website = :companyWebsite,
                   industry = :industry,
                   responsibilities = :responsibilities,
                   sort_order = :sortOrder,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.person WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public WorkExperienceRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<WorkExperienceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(WorkExperienceRepository::map).optional();
    }

    public Page<WorkExperienceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<WorkExperienceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(WorkExperienceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), WorkExperienceEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public WorkExperienceEntity insert(WorkExperienceEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("personId", e.getPersonId())
                .param("experienceType", e.getExperienceType())
                .param("title", e.getTitle())
                .param("company", e.getCompany())
                .param("location", e.getLocation())
                .param("fromDate", e.getFromDate())
                .param("toDate", e.getToDate())
                .param("isCurrent", e.getIsCurrent())
                .param("description", e.getDescription())
                .param("companyWebsite", e.getCompanyWebsite())
                .param("industry", e.getIndustry())
                .param("responsibilities", e.getResponsibilities())
                .param("sortOrder", e.getSortOrder())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(WorkExperienceRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(WorkExperienceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("experienceType", e.getExperienceType())
                .param("title", e.getTitle())
                .param("company", e.getCompany())
                .param("location", e.getLocation())
                .param("fromDate", e.getFromDate())
                .param("toDate", e.getToDate())
                .param("isCurrent", e.getIsCurrent())
                .param("description", e.getDescription())
                .param("companyWebsite", e.getCompanyWebsite())
                .param("industry", e.getIndustry())
                .param("responsibilities", e.getResponsibilities())
                .param("sortOrder", e.getSortOrder())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(WorkExperienceEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced people.person row exists (live). */
    public boolean existsPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PERSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.person_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, WorkExperienceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static WorkExperienceEntity map(ResultSet rs, int row) throws SQLException {
        return WorkExperienceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .personId(rs.getObject("person_id", UUID.class))
                .experienceType(rs.getString("experience_type"))
                .title(rs.getString("title"))
                .company(rs.getString("company"))
                .location(rs.getString("location"))
                .fromDate(rs.getObject("from_date", LocalDate.class))
                .toDate(rs.getObject("to_date", LocalDate.class))
                .isCurrent(rs.getObject("is_current", Boolean.class))
                .description(rs.getString("description"))
                .companyWebsite(rs.getString("company_website"))
                .industry(rs.getString("industry"))
                .responsibilities(rs.getString("responsibilities"))
                .sortOrder(rs.getObject("sort_order", Integer.class))
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
