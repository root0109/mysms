package com.mysms.facility.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.facility.config.FacilityDatabaseConfig;
import com.mysms.facility.domain.RoomFacilityEntity;
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
 * Plain-JDBC access to facility.room_facility: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RoomFacilityRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("roomId", "r.room_id", ValueType.UUID),
            field("facilityId", "r.facility_id", ValueType.UUID),
            field("quantity", "r.quantity", ValueType.DECIMAL),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RoomFacilityEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", RoomFacilityEntity::getCreatedAt),
            Map.entry("updateTime", RoomFacilityEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.room_id, r.facility_id, r.quantity, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM facility.room_facility r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO facility.room_facility
            (id, tenant_id, room_id, facility_id, quantity, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :roomId, :facilityId, :quantity, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, room_id, facility_id, quantity, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE facility.room_facility
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE facility.room_facility
               SET facility_id = :facilityId,
                   quantity = :quantity,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ROOM_FACILITY_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM facility.room_facility
                            WHERE tenant_id = :tenantId
                              AND room_id = :roomId AND facility_id = :facilityId AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_FACILITY_ID = """
            SELECT EXISTS (SELECT 1 FROM facility.facility WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_ROOM_ID = """
            SELECT EXISTS (SELECT 1 FROM facility.room WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM facility.room p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.room_id IN (SELECT p0.id FROM facility.room p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public RoomFacilityRepository(@Qualifier(FacilityDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<RoomFacilityEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(RoomFacilityRepository::map).optional();
    }

    public Page<RoomFacilityEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RoomFacilityEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(RoomFacilityRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RoomFacilityEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public RoomFacilityEntity insert(RoomFacilityEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("roomId", e.getRoomId())
                .param("facilityId", e.getFacilityId())
                .param("quantity", e.getQuantity())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(RoomFacilityRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(RoomFacilityEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("facilityId", e.getFacilityId())
                .param("quantity", e.getQuantity())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(RoomFacilityEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_room_facility_1: room_id, facility_id where (deleted_at IS NULL). */
    public boolean takenRoomIdFacilityId1(RoomFacilityEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ROOM_FACILITY_1_TAKEN)
                .param("roomId", e.getRoomId())
                .param("facilityId", e.getFacilityId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced facility.facility row exists (live). */
    public boolean existsFacilityId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_FACILITY_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced facility.room row exists (live). */
    public boolean existsRoomId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ROOM_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through room_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.room_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, RoomFacilityEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static RoomFacilityEntity map(ResultSet rs, int row) throws SQLException {
        return RoomFacilityEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .roomId(rs.getObject("room_id", UUID.class))
                .facilityId(rs.getObject("facility_id", UUID.class))
                .quantity(rs.getBigDecimal("quantity"))
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
