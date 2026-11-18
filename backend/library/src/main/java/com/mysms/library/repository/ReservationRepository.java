package com.mysms.library.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.domain.ReservationEntity;
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
 * Plain-JDBC access to library.reservation: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ReservationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("mediaId", "r.media_id", ValueType.UUID),
            field("reference", "r.reference", ValueType.STRING),
            field("requestedByPersonId", "r.requested_by_person_id", ValueType.UUID),
            field("requestedByUserId", "r.requested_by_user_id", ValueType.UUID),
            field("dateFrom", "r.date_from", ValueType.DATE),
            field("dateTo", "r.date_to", ValueType.DATE),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ReservationEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ReservationEntity::getCreatedAt),
            Map.entry("updateTime", ReservationEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.media_id, r.reference, r.requested_by_person_id, r.requested_by_user_id,
                   r.date_from, r.date_to, r.status, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM library.reservation r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO library.reservation
            (id, tenant_id, media_id, reference, requested_by_person_id, requested_by_user_id, date_from, date_to,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :mediaId, :reference, :requestedByPersonId, :requestedByUserId, :dateFrom,
                    :dateTo, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, media_id, reference, requested_by_person_id, requested_by_user_id, date_from,
                      date_to, status, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE library.reservation
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE library.reservation
               SET media_id = :mediaId,
                   reference = :reference,
                   requested_by_person_id = :requestedByPersonId,
                   requested_by_user_id = :requestedByUserId,
                   date_from = :dateFrom,
                   date_to = :dateTo,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_MEDIA_ID = """
            SELECT EXISTS (SELECT 1 FROM library.media WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM library.media p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** library.AcceptReservation: allowed while status is requested. */
    private static final String TRANSITION_ACCEPT_RESERVATION = """
            UPDATE library.reservation
               SET status = 'accepted',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** library.RejectReservation: allowed while status is requested. */
    private static final String TRANSITION_REJECT_RESERVATION = """
            UPDATE library.reservation
               SET status = 'rejected',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.media_id IN (SELECT p0.id FROM library.media p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public ReservationRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ReservationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ReservationRepository::map).optional();
    }

    public Page<ReservationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ReservationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ReservationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ReservationEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ReservationEntity insert(ReservationEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("mediaId", e.getMediaId())
                .param("reference", e.getReference())
                .param("requestedByPersonId", e.getRequestedByPersonId())
                .param("requestedByUserId", e.getRequestedByUserId())
                .param("dateFrom", e.getDateFrom())
                .param("dateTo", e.getDateTo())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ReservationRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ReservationEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("mediaId", e.getMediaId())
                .param("reference", e.getReference())
                .param("requestedByPersonId", e.getRequestedByPersonId())
                .param("requestedByUserId", e.getRequestedByUserId())
                .param("dateFrom", e.getDateFrom())
                .param("dateTo", e.getDateTo())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ReservationEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced library.media row exists (live). */
    public boolean existsMediaId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_MEDIA_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through media_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** library.AcceptReservation; 0 rows means the row changed (or vanished) since it was read. */
    public int acceptReservation(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_ACCEPT_RESERVATION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** library.RejectReservation; 0 rows means the row changed (or vanished) since it was read. */
    public int rejectReservation(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_REJECT_RESERVATION)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, ReservationEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ReservationEntity map(ResultSet rs, int row) throws SQLException {
        return ReservationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .mediaId(rs.getObject("media_id", UUID.class))
                .reference(rs.getString("reference"))
                .requestedByPersonId(rs.getObject("requested_by_person_id", UUID.class))
                .requestedByUserId(rs.getObject("requested_by_user_id", UUID.class))
                .dateFrom(rs.getObject("date_from", LocalDate.class))
                .dateTo(rs.getObject("date_to", LocalDate.class))
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
