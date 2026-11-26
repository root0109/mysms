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
import com.mysms.learning.lms.domain.CohortScheduleSlotEntity;
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
 * Plain-JDBC access to lms.cohort_schedule_slot: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CohortScheduleSlotRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("slotType", "r.slot_type", ValueType.STRING),
            field("cohortTeacherId", "r.cohort_teacher_id", ValueType.UUID),
            field("weekday", "r.weekday", ValueType.STRING),
            field("venue", "r.venue", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CohortScheduleSlotEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CohortScheduleSlotEntity::getCreatedAt),
            Map.entry("updateTime", CohortScheduleSlotEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.cohort_id, r.slot_type, r.cohort_teacher_id, r.weekday, r.start_time,
                   r.end_time, r.venue, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM lms.cohort_schedule_slot r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.cohort_schedule_slot
            (id, tenant_id, cohort_id, slot_type, cohort_teacher_id, weekday, start_time, end_time, venue,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :cohortId, COALESCE(:slotType, 'class_meeting'::character varying),
                    :cohortTeacherId, :weekday, :startTime, :endTime, :venue, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, cohort_id, slot_type, cohort_teacher_id, weekday, start_time, end_time, venue,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.cohort_schedule_slot
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.cohort_schedule_slot
               SET slot_type = :slotType,
                   cohort_teacher_id = :cohortTeacherId,
                   weekday = :weekday,
                   start_time = :startTime,
                   end_time = :endTime,
                   venue = :venue,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_COHORT_TEACHER_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_teacher WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public CohortScheduleSlotRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CohortScheduleSlotEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CohortScheduleSlotRepository::map).optional();
    }

    public Page<CohortScheduleSlotEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CohortScheduleSlotEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CohortScheduleSlotRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CohortScheduleSlotEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CohortScheduleSlotEntity insert(CohortScheduleSlotEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("cohortId", e.getCohortId())
                .param("slotType", e.getSlotType())
                .param("cohortTeacherId", e.getCohortTeacherId())
                .param("weekday", e.getWeekday())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("venue", e.getVenue())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CohortScheduleSlotRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CohortScheduleSlotEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("slotType", e.getSlotType())
                .param("cohortTeacherId", e.getCohortTeacherId())
                .param("weekday", e.getWeekday())
                .param("startTime", e.getStartTime())
                .param("endTime", e.getEndTime())
                .param("venue", e.getVenue())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CohortScheduleSlotEntity e, int expectedVersion) {
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

    /** Whether the referenced lms.cohort_teacher row exists (live). */
    public boolean existsCohortTeacherId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_TEACHER_ID).param("id", id).query(Boolean.class).single());
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

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.cohort_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, CohortScheduleSlotEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CohortScheduleSlotEntity map(ResultSet rs, int row) throws SQLException {
        return CohortScheduleSlotEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .slotType(rs.getString("slot_type"))
                .cohortTeacherId(rs.getObject("cohort_teacher_id", UUID.class))
                .weekday(rs.getString("weekday"))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .endTime(rs.getObject("end_time", LocalTime.class))
                .venue(rs.getString("venue"))
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
