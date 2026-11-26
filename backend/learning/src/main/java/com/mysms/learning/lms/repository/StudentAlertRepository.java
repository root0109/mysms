package com.mysms.learning.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.StudentAlertEntity;
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
 * Plain-JDBC access to lms.student_alert: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class StudentAlertRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("metricCode", "r.metric_code", ValueType.STRING),
            field("contentItemId", "r.content_item_id", ValueType.UUID),
            field("sentAt", "r.sent_at", ValueType.TIMESTAMP),
            field("sentBy", "r.sent_by", ValueType.UUID),
            field("subject", "r.subject", ValueType.STRING),
            field("message", "r.message", ValueType.STRING),
            field("details", "r.details", ValueType.STRING),
            field("readAt", "r.read_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StudentAlertEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StudentAlertEntity::getCreatedAt),
            Map.entry("updateTime", StudentAlertEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.user_id, r.cohort_id, r.course_id, r.metric_code, r.content_item_id,
                   r.sent_at, r.sent_by, r.subject, r.message, r.details, r.read_at, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.student_alert r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.student_alert
            (id, tenant_id, user_id, cohort_id, course_id, metric_code, content_item_id, sent_at, sent_by, subject,
             message, details, read_at, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :cohortId, :courseId, :metricCode, :contentItemId,
                    COALESCE(:sentAt, now()), :sentBy, :subject, :message, :details, :readAt, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, user_id, cohort_id, course_id, metric_code, content_item_id, sent_at, sent_by,
                      subject, message, details, read_at, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.student_alert
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.student_alert
               SET user_id = :userId,
                   cohort_id = :cohortId,
                   course_id = :courseId,
                   metric_code = :metricCode,
                   content_item_id = :contentItemId,
                   sent_at = :sentAt,
                   sent_by = :sentBy,
                   subject = :subject,
                   message = :message,
                   details = :details,
                   read_at = :readAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CONTENT_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COURSE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.course WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.cohort_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public StudentAlertRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StudentAlertEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StudentAlertRepository::map).optional();
    }

    public Page<StudentAlertEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StudentAlertEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StudentAlertRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StudentAlertEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StudentAlertEntity insert(StudentAlertEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("userId", e.getUserId())
                .param("cohortId", e.getCohortId())
                .param("courseId", e.getCourseId())
                .param("metricCode", e.getMetricCode())
                .param("contentItemId", e.getContentItemId())
                .param("sentAt", e.getSentAt())
                .param("sentBy", e.getSentBy())
                .param("subject", e.getSubject())
                .param("message", e.getMessage())
                .param("details", e.getDetails())
                .param("readAt", e.getReadAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StudentAlertRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StudentAlertEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("cohortId", e.getCohortId())
                .param("courseId", e.getCourseId())
                .param("metricCode", e.getMetricCode())
                .param("contentItemId", e.getContentItemId())
                .param("sentAt", e.getSentAt())
                .param("sentBy", e.getSentBy())
                .param("subject", e.getSubject())
                .param("message", e.getMessage())
                .param("details", e.getDetails())
                .param("readAt", e.getReadAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StudentAlertEntity e, int expectedVersion) {
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

    /** Whether the referenced lms.content_item row exists (live). */
    public boolean existsContentItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONTENT_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.course row exists (live). */
    public boolean existsCourseId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COURSE_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through cohort_id (empty: a tenant-level row, or none). */
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

    private static final String OF_COHORTS = SELECT_LIVE
            + " AND r.cohort_id = ANY(:cohortIds) ORDER BY r.sent_at DESC LIMIT :top";

    /** The cohorts' latest alerts, newest first. */
    public List<StudentAlertEntity> ofCohorts(List<UUID> cohortIds, int top) {
        if (cohortIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(OF_COHORTS).param("cohortIds", cohortIds.toArray(UUID[]::new)).param("top", top)
                .query(StudentAlertRepository::map).list();
    }

    private static List<Object> sortValues(SortSpec sort, StudentAlertEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StudentAlertEntity map(ResultSet rs, int row) throws SQLException {
        return StudentAlertEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
                .metricCode(rs.getString("metric_code"))
                .contentItemId(rs.getObject("content_item_id", UUID.class))
                .sentAt(rs.getObject("sent_at", OffsetDateTime.class))
                .sentBy(rs.getObject("sent_by", UUID.class))
                .subject(rs.getString("subject"))
                .message(rs.getString("message"))
                .details(rs.getString("details"))
                .readAt(rs.getObject("read_at", OffsetDateTime.class))
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
