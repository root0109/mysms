package com.mysms.students.careers.repository;
import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.students.careers.domain.JobApplicationEntity;
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
 * Plain-JDBC access to careers.job_application: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class JobApplicationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("jobOpeningId", "r.job_opening_id", ValueType.UUID),
            field("applicantUserId", "r.applicant_user_id", ValueType.UUID),
            field("resumeFileId", "r.resume_file_id", ValueType.UUID),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<JobApplicationEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", JobApplicationEntity::getCreatedAt),
            Map.entry("updateTime", JobApplicationEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.job_opening_id, r.applicant_user_id, r.resume_file_id, r.status, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM careers.job_application r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO careers.job_application
            (id, tenant_id, job_opening_id, applicant_user_id, resume_file_id, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :jobOpeningId, :applicantUserId, :resumeFileId, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, job_opening_id, applicant_user_id, resume_file_id, status, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE careers.job_application
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE careers.job_application
               SET applicant_user_id = :applicantUserId,
                   resume_file_id = :resumeFileId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_JOB_APPLICATION_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM careers.job_application
                            WHERE tenant_id = :tenantId
                              AND job_opening_id = :jobOpeningId AND applicant_user_id = :applicantUserId AND id <> :id
                                  AND ((deleted_at IS NULL)))
            """;
    /** careers.UpdateApplicationStatus: the guard on the status was checked on the row as read. */
    private static final String SET_STATUS = """
            UPDATE careers.job_application
               SET status = :status,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_JOB_OPENING_ID = """
            SELECT EXISTS (SELECT 1 FROM careers.job_opening WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public JobApplicationRepository(@Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<JobApplicationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(JobApplicationRepository::map).optional();
    }

    public Page<JobApplicationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<JobApplicationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(JobApplicationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), JobApplicationEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public JobApplicationEntity insert(JobApplicationEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("jobOpeningId", e.getJobOpeningId())
                .param("applicantUserId", e.getApplicantUserId())
                .param("resumeFileId", e.getResumeFileId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(JobApplicationRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(JobApplicationEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("applicantUserId", e.getApplicantUserId())
                .param("resumeFileId", e.getResumeFileId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(JobApplicationEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_job_application_1: job_opening_id, applicant_user_id where (deleted_at IS NULL). */
    public boolean takenJobOpeningIdApplicantUserId1(JobApplicationEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_JOB_APPLICATION_1_TAKEN)
                .param("jobOpeningId", e.getJobOpeningId())
                .param("applicantUserId", e.getApplicantUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** careers.UpdateApplicationStatus; 0 rows means the row changed (or vanished) since it was read. */
    public int setStatus(UUID id, int expected, String status, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SET_STATUS).param("id", id).param("expected", expected).param("status", status)
                .param("now", now).param("actor", actor).update();
    }

    public boolean existsJobOpeningId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_JOB_OPENING_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.job_opening_id = :parentId", Map.of("parentId", parentId));
    }

    /** The applications of one applicant. */
    public static SqlFragment ofApplicant(UUID applicantUserId) {
        return new SqlFragment("r.applicant_user_id = :applicantUserId", Map.of("applicantUserId", applicantUserId));
    }

    private static List<Object> sortValues(SortSpec sort, JobApplicationEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static JobApplicationEntity map(ResultSet rs, int row) throws SQLException {
        return JobApplicationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .jobOpeningId(rs.getObject("job_opening_id", UUID.class))
                .applicantUserId(rs.getObject("applicant_user_id", UUID.class))
                .resumeFileId(rs.getObject("resume_file_id", UUID.class))
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
