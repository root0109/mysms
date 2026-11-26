package com.mysms.academics.timetable.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.academics.timetable.config.TimetableDatabaseConfig;
import com.mysms.academics.timetable.domain.PeriodEntity;
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
 * Plain-JDBC access to timetable.period: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PeriodRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("durationMinutes", "r.duration_minutes", ValueType.DECIMAL),
            field("sequence", "r.sequence", ValueType.INTEGER),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PeriodEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PeriodEntity::getCreatedAt),
            Map.entry("updateTime", PeriodEntity::getUpdatedAt),
            Map.entry("name", PeriodEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.name, r.start_time, r.duration_minutes, r.sequence,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM timetable.period r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO timetable.period
            (id, tenant_id, institution_id, name, start_time, duration_minutes, sequence, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :name, :startTime, :durationMinutes, :sequence, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, name, start_time, duration_minutes, sequence, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE timetable.period
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE timetable.period
               SET institution_id = :institutionId,
                   name = :name,
                   start_time = :startTime,
                   duration_minutes = :durationMinutes,
                   sequence = :sequence,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PERIOD_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM timetable.period
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND name = :name AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String REFERENCED_BY_SESSION_PERIOD_ID = """
            SELECT EXISTS (SELECT 1 FROM timetable.session WHERE tenant_id = :tenantId
                AND period_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public PeriodRepository(@Qualifier(TimetableDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PeriodEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PeriodRepository::map).optional();
    }

    public Page<PeriodEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PeriodEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PeriodRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PeriodEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PeriodEntity insert(PeriodEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("name", e.getName())
                .param("startTime", e.getStartTime())
                .param("durationMinutes", e.getDurationMinutes())
                .param("sequence", e.getSequence())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PeriodRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PeriodEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("name", e.getName())
                .param("startTime", e.getStartTime())
                .param("durationMinutes", e.getDurationMinutes())
                .param("sequence", e.getSequence())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PeriodEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_period_1: institution_id, name where (deleted_at IS NULL). */
    public boolean takenInstitutionIdName1(PeriodEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PERIOD_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("name", e.getName())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether live timetable.session rows still reference the row. */
    public boolean referencedBySessionPeriodId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_SESSION_PERIOD_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, PeriodEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PeriodEntity map(ResultSet rs, int row) throws SQLException {
        return PeriodEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .name(rs.getString("name"))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .durationMinutes(rs.getBigDecimal("duration_minutes"))
                .sequence(rs.getObject("sequence", Integer.class))
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
