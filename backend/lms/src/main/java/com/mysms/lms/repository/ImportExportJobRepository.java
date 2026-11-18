package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.ImportExportJobEntity;
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
 * Plain-JDBC access to lms.import_export_job: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ImportExportJobRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("direction", "r.direction", ValueType.STRING),
            field("packageFormat", "r.package_format", ValueType.STRING),
            field("scope", "r.scope", ValueType.STRING),
            field("courseId", "r.course_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("targetChapterId", "r.target_chapter_id", ValueType.UUID),
            field("addToRepository", "r.add_to_repository", ValueType.BOOLEAN),
            field("packageFileId", "r.package_file_id", ValueType.UUID),
            field("requestedBy", "r.requested_by", ValueType.UUID),
            field("requestedOn", "r.requested_on", ValueType.DATE),
            field("startedOn", "r.started_on", ValueType.DATE),
            field("status", "r.status", ValueType.STRING),
            field("unarchiveStatus", "r.unarchive_status", ValueType.STRING),
            field("errorLog", "r.error_log", ValueType.STRING),
            field("cohortCode", "r.cohort_code", ValueType.STRING),
            field("cohortStartDate", "r.cohort_start_date", ValueType.DATE),
            field("courseCode", "r.course_code", ValueType.STRING),
            field("courseVersion", "r.course_version", ValueType.STRING),
            field("courseTitle", "r.course_title", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ImportExportJobEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ImportExportJobEntity::getCreatedAt),
            Map.entry("updateTime", ImportExportJobEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.direction, r.package_format, r.scope, r.course_id,
                   r.cohort_id, r.target_chapter_id, r.add_to_repository, r.package_file_id, r.requested_by,
                   r.requested_on, r.started_on, r.status, r.unarchive_status, r.error_log, r.cohort_code,
                   r.cohort_start_date, r.course_code, r.course_version, r.course_title, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.import_export_job r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.import_export_job
            (id, tenant_id, institution_id, direction, package_format, scope, course_id, cohort_id,
             target_chapter_id, add_to_repository, package_file_id, requested_by, requested_on, started_on,
             unarchive_status, error_log, cohort_code, cohort_start_date, course_code, course_version, course_title,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :direction, :packageFormat, :scope, :courseId, :cohortId,
                    :targetChapterId, COALESCE(:addToRepository, false), :packageFileId, :requestedBy, :requestedOn,
                    :startedOn, :unarchiveStatus, :errorLog, :cohortCode, :cohortStartDate, :courseCode,
                    :courseVersion, :courseTitle, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, direction, package_format, scope, course_id, cohort_id,
                      target_chapter_id, add_to_repository, package_file_id, requested_by, requested_on, started_on,
                      status, unarchive_status, error_log, cohort_code, cohort_start_date, course_code, course_version,
                      course_title, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.import_export_job
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.import_export_job
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_TARGET_CHAPTER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.chapter WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;

    /** lms.CancelImportExportJob: allowed while status is pending. */
    private static final String TRANSITION_CANCEL_IMPORT_EXPORT_JOB = """
            UPDATE lms.import_export_job
               SET status = 'cancelled',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public ImportExportJobRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ImportExportJobEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ImportExportJobRepository::map).optional();
    }

    public Page<ImportExportJobEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ImportExportJobEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ImportExportJobRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ImportExportJobEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ImportExportJobEntity insert(ImportExportJobEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("direction", e.getDirection())
                .param("packageFormat", e.getPackageFormat())
                .param("scope", e.getScope())
                .param("courseId", e.getCourseId())
                .param("cohortId", e.getCohortId())
                .param("targetChapterId", e.getTargetChapterId())
                .param("addToRepository", e.getAddToRepository())
                .param("packageFileId", e.getPackageFileId())
                .param("requestedBy", e.getRequestedBy())
                .param("requestedOn", e.getRequestedOn())
                .param("startedOn", e.getStartedOn())
                .param("unarchiveStatus", e.getUnarchiveStatus())
                .param("errorLog", e.getErrorLog())
                .param("cohortCode", e.getCohortCode())
                .param("cohortStartDate", e.getCohortStartDate())
                .param("courseCode", e.getCourseCode())
                .param("courseVersion", e.getCourseVersion())
                .param("courseTitle", e.getCourseTitle())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ImportExportJobRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ImportExportJobEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ImportExportJobEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.chapter row exists (live). */
    public boolean existsTargetChapterId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_TARGET_CHAPTER_ID).param("id", id).query(Boolean.class).single());
    }

    /** lms.CancelImportExportJob; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelImportExportJob(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_IMPORT_EXPORT_JOB)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, ImportExportJobEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ImportExportJobEntity map(ResultSet rs, int row) throws SQLException {
        return ImportExportJobEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .direction(rs.getString("direction"))
                .packageFormat(rs.getString("package_format"))
                .scope(rs.getString("scope"))
                .courseId(rs.getObject("course_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .targetChapterId(rs.getObject("target_chapter_id", UUID.class))
                .addToRepository(rs.getObject("add_to_repository", Boolean.class))
                .packageFileId(rs.getObject("package_file_id", UUID.class))
                .requestedBy(rs.getObject("requested_by", UUID.class))
                .requestedOn(rs.getObject("requested_on", LocalDate.class))
                .startedOn(rs.getObject("started_on", LocalDate.class))
                .status(rs.getString("status"))
                .unarchiveStatus(rs.getString("unarchive_status"))
                .errorLog(rs.getString("error_log"))
                .cohortCode(rs.getString("cohort_code"))
                .cohortStartDate(rs.getObject("cohort_start_date", LocalDate.class))
                .courseCode(rs.getString("course_code"))
                .courseVersion(rs.getString("course_version"))
                .courseTitle(rs.getString("course_title"))
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
