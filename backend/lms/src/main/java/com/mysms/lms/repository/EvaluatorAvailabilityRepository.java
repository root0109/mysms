package com.mysms.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.EvaluatorAvailabilityEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
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
 * Plain-JDBC access to lms.evaluator_availability: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class EvaluatorAvailabilityRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("evaluatorId", "r.evaluator_id", ValueType.UUID),
            field("weekday", "r.weekday", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<EvaluatorAvailabilityEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", EvaluatorAvailabilityEntity::getCreatedAt),
            Map.entry("updateTime", EvaluatorAvailabilityEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.evaluator_id, r.weekday, r.start_time, r.end_time, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.evaluator_availability r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.evaluator_availability
            (id, tenant_id, evaluator_id, weekday, start_time, end_time, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :evaluatorId, :weekday, :startTime, :endTime, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, evaluator_id, weekday, start_time, end_time, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.evaluator_availability
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.evaluator_availability
               SET weekday = :weekday,
                   start_time = :startTime,
                   end_time = :endTime,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_EVALUATOR_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.evaluator WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public EvaluatorAvailabilityRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<EvaluatorAvailabilityEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(EvaluatorAvailabilityRepository::map).optional();
    }

    public Page<EvaluatorAvailabilityEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<EvaluatorAvailabilityEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(EvaluatorAvailabilityRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), EvaluatorAvailabilityEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public EvaluatorAvailabilityEntity insert(EvaluatorAvailabilityEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("evaluatorId", e.getEvaluatorId())
                .param("weekday", e.getWeekday())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(EvaluatorAvailabilityRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(EvaluatorAvailabilityEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("weekday", e.getWeekday())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(EvaluatorAvailabilityEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.evaluator row exists (live). */
    public boolean existsEvaluatorId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EVALUATOR_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.evaluator_id = :parentId", Map.of("parentId", parentId));
    }

    private static final String OF_EVALUATOR = SELECT_LIVE + " AND r.evaluator_id = :evaluatorId"
            + " ORDER BY r.weekday, r.start_time";

    /** The evaluator's live weekly availability, in week order. */
    public List<EvaluatorAvailabilityEntity> ofEvaluator(UUID evaluatorId) {
        return jdbc.sql(OF_EVALUATOR).param("evaluatorId", evaluatorId)
                .query(EvaluatorAvailabilityRepository::map).list();
    }

    private static List<Object> sortValues(SortSpec sort, EvaluatorAvailabilityEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static EvaluatorAvailabilityEntity map(ResultSet rs, int row) throws SQLException {
        return EvaluatorAvailabilityEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .evaluatorId(rs.getObject("evaluator_id", UUID.class))
                .weekday(rs.getString("weekday"))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .endTime(rs.getObject("end_time", LocalTime.class))
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
