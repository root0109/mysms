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
import com.mysms.campus.transport.domain.RouteStopEntity;
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
 * Plain-JDBC access to transport.route_stop: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RouteStopRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("routeId", "r.route_id", ValueType.UUID),
            field("sequence", "r.sequence", ValueType.INTEGER),
            field("stopName", "r.stop_name", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("sequence", "r.sequence", ValueType.INTEGER));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RouteStopEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", RouteStopEntity::getCreatedAt),
            Map.entry("updateTime", RouteStopEntity::getUpdatedAt),
            Map.entry("sequence", RouteStopEntity::getSequence));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.route_id, r.sequence, r.stop_name, r.pickup_time, r.drop_time, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM transport.route_stop r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO transport.route_stop
            (id, tenant_id, route_id, sequence, stop_name, pickup_time, drop_time, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :routeId, :sequence, :stopName, :pickupTime, :dropTime, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, route_id, sequence, stop_name, pickup_time, drop_time, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE transport.route_stop
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE transport.route_stop
               SET sequence = :sequence,
                   stop_name = :stopName,
                   pickup_time = :pickupTime,
                   drop_time = :dropTime,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ROUTE_STOP_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM transport.route_stop
                            WHERE tenant_id = :tenantId
                              AND route_id = :routeId AND sequence = :sequence AND id <> :id)
            """;
    private static final String EXISTS_ROUTE_ID = """
            SELECT EXISTS (SELECT 1 FROM transport.route WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_STUDENT_TRANSPORT_ROUTE_STOP_ID = """
            SELECT EXISTS (SELECT 1 FROM transport.student_transport WHERE tenant_id = :tenantId
                AND route_stop_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM transport.route p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.route_id IN (SELECT p0.id FROM transport.route p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public RouteStopRepository(@Qualifier(TransportDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RouteStopEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RouteStopRepository::map).optional();
    }

    public Page<RouteStopEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RouteStopEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RouteStopRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RouteStopEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public RouteStopEntity insert(RouteStopEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("routeId", e.getRouteId())
                .param("sequence", e.getSequence())
                .param("stopName", e.getStopName())
                .param("pickupTime", e.getPickupTime())
                .param("dropTime", e.getDropTime())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(RouteStopRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(RouteStopEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("sequence", e.getSequence())
                .param("stopName", e.getStopName())
                .param("pickupTime", e.getPickupTime())
                .param("dropTime", e.getDropTime())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(RouteStopEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_route_stop_1: route_id, sequence. */
    public boolean takenRouteIdSequence1(RouteStopEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ROUTE_STOP_1_TAKEN)
                .param("routeId", e.getRouteId())
                .param("sequence", e.getSequence())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced transport.route row exists (live). */
    public boolean existsRouteId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ROUTE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live transport.student_transport rows still reference the row. */
    public boolean referencedByStudentTransportRouteStopId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_STUDENT_TRANSPORT_ROUTE_STOP_ID).param("id", id)
                .query(Boolean.class).single());
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

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.route_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, RouteStopEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static RouteStopEntity map(ResultSet rs, int row) throws SQLException {
        return RouteStopEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .routeId(rs.getObject("route_id", UUID.class))
                .sequence(rs.getObject("sequence", Integer.class))
                .stopName(rs.getString("stop_name"))
                .pickupTime(rs.getObject("pickup_time", LocalTime.class))
                .dropTime(rs.getObject("drop_time", LocalTime.class))
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
