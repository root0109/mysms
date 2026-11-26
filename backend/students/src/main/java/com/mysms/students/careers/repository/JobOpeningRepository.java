package com.mysms.students.careers.repository;
import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.students.careers.domain.JobOpeningEntity;
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
 * Plain-JDBC access to careers.job_opening: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class JobOpeningRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("postedByUserId", "r.posted_by_user_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("location", "r.location", ValueType.STRING),
            field("countryId", "r.country_id", ValueType.UUID),
            field("employmentType", "r.employment_type", ValueType.STRING),
            field("workMode", "r.work_mode", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("companyName", "r.company_name", ValueType.STRING),
            field("companyWebsite", "r.company_website", ValueType.STRING),
            field("companyEmail", "r.company_email", ValueType.STRING),
            field("companyLogoFileId", "r.company_logo_file_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<JobOpeningEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", JobOpeningEntity::getCreatedAt),
            Map.entry("updateTime", JobOpeningEntity::getUpdatedAt),
            Map.entry("title", JobOpeningEntity::getTitle));

    /** The openings the public sees: open ones (added to the caller's filter for guests). */
    public static final SqlFragment OPEN_ONLY = new SqlFragment("r.status = 'open'", Map.of());

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.posted_by_user_id, r.title, r.location, r.country_id, r.employment_type,
                   r.work_mode, r.status, r.description, r.company_name, r.company_website, r.company_email,
                   r.company_logo_file_id, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM careers.job_opening r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO careers.job_opening
            (id, tenant_id, posted_by_user_id, title, location, country_id, employment_type, work_mode, description,
             company_name, company_website, company_email, company_logo_file_id, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :postedByUserId, :title, :location, :countryId, :employmentType, :workMode,
                    :description, :companyName, :companyWebsite, :companyEmail, :companyLogoFileId, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, posted_by_user_id, title, location, country_id, employment_type, work_mode, status,
                      description, company_name, company_website, company_email, company_logo_file_id, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE careers.job_opening
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE careers.job_opening
               SET posted_by_user_id = :postedByUserId,
                   title = :title,
                   location = :location,
                   country_id = :countryId,
                   employment_type = :employmentType,
                   work_mode = :workMode,
                   description = :description,
                   company_name = :companyName,
                   company_website = :companyWebsite,
                   company_email = :companyEmail,
                   company_logo_file_id = :companyLogoFileId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_JOB_APPLICATION_JOB_OPENING_ID = """
            SELECT EXISTS (SELECT 1 FROM careers.job_application WHERE tenant_id = :tenantId
                AND job_opening_id = :id AND deleted_at IS NULL)
            """;

    /** careers.CloseJobOpening: allowed while status is open. */
    private static final String TRANSITION_CLOSE_JOB_OPENING = """
            UPDATE careers.job_opening
               SET status = 'closed',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** careers.ReopenJobOpening: allowed while status is closed. */
    private static final String TRANSITION_REOPEN_JOB_OPENING = """
            UPDATE careers.job_opening
               SET status = 'open',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public JobOpeningRepository(@Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<JobOpeningEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(JobOpeningRepository::map).optional();
    }

    public Page<JobOpeningEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<JobOpeningEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(JobOpeningRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), JobOpeningEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public JobOpeningEntity insert(JobOpeningEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("postedByUserId", e.getPostedByUserId())
                .param("title", e.getTitle())
                .param("location", e.getLocation())
                .param("countryId", e.getCountryId())
                .param("employmentType", e.getEmploymentType())
                .param("workMode", e.getWorkMode())
                .param("description", e.getDescription())
                .param("companyName", e.getCompanyName())
                .param("companyWebsite", e.getCompanyWebsite())
                .param("companyEmail", e.getCompanyEmail())
                .param("companyLogoFileId", e.getCompanyLogoFileId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(JobOpeningRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(JobOpeningEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("postedByUserId", e.getPostedByUserId())
                .param("title", e.getTitle())
                .param("location", e.getLocation())
                .param("countryId", e.getCountryId())
                .param("employmentType", e.getEmploymentType())
                .param("workMode", e.getWorkMode())
                .param("description", e.getDescription())
                .param("companyName", e.getCompanyName())
                .param("companyWebsite", e.getCompanyWebsite())
                .param("companyEmail", e.getCompanyEmail())
                .param("companyLogoFileId", e.getCompanyLogoFileId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(JobOpeningEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live careers.job_application rows still reference the row. */
    public boolean referencedByJobApplicationJobOpeningId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_JOB_APPLICATION_JOB_OPENING_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** careers.CloseJobOpening; 0 rows means the row changed (or vanished) since it was read. */
    public int closeJobOpening(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CLOSE_JOB_OPENING)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** careers.ReopenJobOpening; 0 rows means the row changed (or vanished) since it was read. */
    public int reopenJobOpening(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REOPEN_JOB_OPENING)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    private static List<Object> sortValues(SortSpec sort, JobOpeningEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static JobOpeningEntity map(ResultSet rs, int row) throws SQLException {
        return JobOpeningEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .postedByUserId(rs.getObject("posted_by_user_id", UUID.class))
                .title(rs.getString("title"))
                .location(rs.getString("location"))
                .countryId(rs.getObject("country_id", UUID.class))
                .employmentType(rs.getString("employment_type"))
                .workMode(rs.getString("work_mode"))
                .status(rs.getString("status"))
                .description(rs.getString("description"))
                .companyName(rs.getString("company_name"))
                .companyWebsite(rs.getString("company_website"))
                .companyEmail(rs.getString("company_email"))
                .companyLogoFileId(rs.getObject("company_logo_file_id", UUID.class))
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
