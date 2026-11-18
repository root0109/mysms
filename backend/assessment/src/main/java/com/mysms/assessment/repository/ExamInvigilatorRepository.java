package com.mysms.assessment.repository;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.ExamInvigilatorEntity;
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
 * Plain-JDBC access to assessment.exam_invigilator: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ExamInvigilatorRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("examId", "r.exam_id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ExamInvigilatorEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ExamInvigilatorEntity::getCreatedAt),
            Map.entry("updateTime", ExamInvigilatorEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.exam_id, r.staff_id, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM assessment.exam_invigilator r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO assessment.exam_invigilator
            (id, tenant_id, exam_id, staff_id, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :examId, :staffId, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, exam_id, staff_id, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.exam_invigilator
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.exam_invigilator
               SET staff_id = :staffId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_EXAM_INVIGILATOR_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam_invigilator
                            WHERE tenant_id = :tenantId
                              AND exam_id = :examId AND staff_id = :staffId AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_EXAM_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.exam WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM assessment.exam p0 JOIN assessment.exam_session p1 ON p1.tenant_id = :tenantId
                AND p1.id =
            p0.exam_session_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.exam_id IN (SELECT p0.id FROM assessment.exam p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.exam_session_id IN (SELECT p1.id
             FROM assessment.exam_session p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds))))))
            """;

    private final JdbcClient jdbc;

    public ExamInvigilatorRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ExamInvigilatorEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ExamInvigilatorRepository::map).optional();
    }

    public Page<ExamInvigilatorEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ExamInvigilatorEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ExamInvigilatorRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ExamInvigilatorEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ExamInvigilatorEntity insert(ExamInvigilatorEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("examId", e.getExamId())
                .param("staffId", e.getStaffId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ExamInvigilatorRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ExamInvigilatorEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("staffId", e.getStaffId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ExamInvigilatorEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_exam_invigilator_1: exam_id, staff_id where (deleted_at IS NULL). */
    public boolean takenExamIdStaffId1(ExamInvigilatorEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_EXAM_INVIGILATOR_1_TAKEN)
                .param("examId", e.getExamId())
                .param("staffId", e.getStaffId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced assessment.exam row exists (live). */
    public boolean existsExamId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EXAM_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through exam_id (empty: a tenant-level row, or none). */
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

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.exam_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ExamInvigilatorEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ExamInvigilatorEntity map(ResultSet rs, int row) throws SQLException {
        return ExamInvigilatorEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .examId(rs.getObject("exam_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
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
