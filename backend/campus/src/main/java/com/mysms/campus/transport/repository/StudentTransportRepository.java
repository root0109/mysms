package com.mysms.campus.transport.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.campus.transport.config.TransportDatabaseConfig;
import com.mysms.campus.transport.domain.StudentTransportEntity;
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
 * Plain-JDBC access to transport.student_transport: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class StudentTransportRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("transportMode", "r.transport_mode", ValueType.STRING),
            field("routeId", "r.route_id", ValueType.UUID),
            field("routeStopId", "r.route_stop_id", ValueType.UUID),
            field("busPassIssued", "r.bus_pass_issued", ValueType.BOOLEAN),
            field("details", "r.details", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<StudentTransportEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", StudentTransportEntity::getCreatedAt),
            Map.entry("updateTime", StudentTransportEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.student_id, r.academic_year_id, r.transport_mode, r.route_id, r.route_stop_id,
                   r.bus_pass_issued, r.details, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM transport.student_transport r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO transport.student_transport
            (id, tenant_id, student_id, academic_year_id, transport_mode, route_id, route_stop_id, bus_pass_issued,
             details, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :studentId, :academicYearId, :transportMode, :routeId, :routeStopId,
                    COALESCE(:busPassIssued, false), :details, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, student_id, academic_year_id, transport_mode, route_id, route_stop_id,
                      bus_pass_issued, details, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE transport.student_transport
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE transport.student_transport
               SET student_id = :studentId,
                   academic_year_id = :academicYearId,
                   transport_mode = :transportMode,
                   route_id = :routeId,
                   route_stop_id = :routeStopId,
                   bus_pass_issued = :busPassIssued,
                   details = :details,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_STUDENT_TRANSPORT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM transport.student_transport
                            WHERE tenant_id = :tenantId
                              AND student_id = :studentId AND academic_year_id = :academicYearId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_ROUTE_ID = """
            SELECT EXISTS (SELECT 1 FROM transport.route WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_ROUTE_STOP_ID = """
            SELECT EXISTS (SELECT 1 FROM transport.route_stop WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM transport.route p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.route_id IN (SELECT p0.id FROM transport.route p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))) OR r.route_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public StudentTransportRepository(@Qualifier(TransportDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<StudentTransportEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(StudentTransportRepository::map).optional();
    }

    public Page<StudentTransportEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<StudentTransportEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(StudentTransportRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), StudentTransportEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public StudentTransportEntity insert(StudentTransportEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("transportMode", e.getTransportMode())
                .param("routeId", e.getRouteId())
                .param("routeStopId", e.getRouteStopId())
                .param("busPassIssued", e.getBusPassIssued())
                .param("details", e.getDetails())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(StudentTransportRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(StudentTransportEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
                .param("transportMode", e.getTransportMode())
                .param("routeId", e.getRouteId())
                .param("routeStopId", e.getRouteStopId())
                .param("busPassIssued", e.getBusPassIssued())
                .param("details", e.getDetails())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(StudentTransportEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_student_transport_1: student_id, academic_year_id where (deleted_at IS NULL). */
    public boolean takenStudentIdAcademicYearId1(StudentTransportEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_STUDENT_TRANSPORT_1_TAKEN)
                .param("studentId", e.getStudentId())
                .param("academicYearId", e.getAcademicYearId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced transport.route row exists (live). */
    public boolean existsRouteId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ROUTE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced transport.route_stop row exists (live). */
    public boolean existsRouteStopId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ROUTE_STOP_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through route_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, StudentTransportEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static StudentTransportEntity map(ResultSet rs, int row) throws SQLException {
        return StudentTransportEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .transportMode(rs.getString("transport_mode"))
                .routeId(rs.getObject("route_id", UUID.class))
                .routeStopId(rs.getObject("route_stop_id", UUID.class))
                .busPassIssued(rs.getObject("bus_pass_issued", Boolean.class))
                .details(rs.getString("details"))
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
