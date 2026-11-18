package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.EventOccurrenceEntity;
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
 * Plain-JDBC access to comms.event_occurrence: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class EventOccurrenceRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("eventId", "r.event_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("color", "r.color", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("startAt", "r.start_at", ValueType.TIMESTAMP),
            field("endAt", "r.end_at", ValueType.TIMESTAMP),
            field("isCancelled", "r.is_cancelled", ValueType.BOOLEAN),
            field("responseStatus", "r.response_status", ValueType.STRING),
            field("importance", "r.importance", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<EventOccurrenceEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", EventOccurrenceEntity::getCreatedAt),
            Map.entry("updateTime", EventOccurrenceEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.event_id, r.user_id, r.cohort_id, r.title, r.color, r.description, r.start_at,
                   r.end_at, r.is_cancelled, r.response_status, r.importance, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.event_occurrence r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.event_occurrence
            (id, tenant_id, event_id, user_id, cohort_id, title, color, description, start_at, end_at, is_cancelled,
             response_status, importance, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :eventId, :userId, :cohortId, :title, :color, :description, :startAt, :endAt,
                    COALESCE(:isCancelled, false), :responseStatus, :importance, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, event_id, user_id, cohort_id, title, color, description, start_at, end_at,
                      is_cancelled, response_status, importance, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.event_occurrence
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.event_occurrence
               SET user_id = :userId,
                   cohort_id = :cohortId,
                   title = :title,
                   color = :color,
                   description = :description,
                   start_at = :startAt,
                   end_at = :endAt,
                   is_cancelled = :isCancelled,
                   response_status = :responseStatus,
                   importance = :importance,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_EVENT_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.event WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM comms.event p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;

    /** comms.CancelEventOccurrence: allowed while is_cancelled is false. */
    private static final String TRANSITION_CANCEL_EVENT_OCCURRENCE = """
            UPDATE comms.event_occurrence
               SET is_cancelled = true,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.event_id IN (SELECT p0.id FROM comms.event p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public EventOccurrenceRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<EventOccurrenceEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(EventOccurrenceRepository::map).optional();
    }

    public Page<EventOccurrenceEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<EventOccurrenceEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(EventOccurrenceRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), EventOccurrenceEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public EventOccurrenceEntity insert(EventOccurrenceEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("eventId", e.getEventId())
                .param("userId", e.getUserId())
                .param("cohortId", e.getCohortId())
                .param("title", e.getTitle())
                .param("color", e.getColor())
                .param("description", e.getDescription())
                .param("startAt", e.getStartAt())
                .param("endAt", e.getEndAt())
                .param("isCancelled", e.getIsCancelled())
                .param("responseStatus", e.getResponseStatus())
                .param("importance", e.getImportance())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(EventOccurrenceRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(EventOccurrenceEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("cohortId", e.getCohortId())
                .param("title", e.getTitle())
                .param("color", e.getColor())
                .param("description", e.getDescription())
                .param("startAt", e.getStartAt())
                .param("endAt", e.getEndAt())
                .param("isCancelled", e.getIsCancelled())
                .param("responseStatus", e.getResponseStatus())
                .param("importance", e.getImportance())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(EventOccurrenceEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced comms.event row exists (live). */
    public boolean existsEventId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_EVENT_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through event_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** comms.CancelEventOccurrence; 0 rows means the row changed (or vanished) since it was read. */
    public int cancelEventOccurrence(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_CANCEL_EVENT_OCCURRENCE)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.event_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, EventOccurrenceEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static EventOccurrenceEntity map(ResultSet rs, int row) throws SQLException {
        return EventOccurrenceEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .eventId(rs.getObject("event_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .title(rs.getString("title"))
                .color(rs.getString("color"))
                .description(rs.getString("description"))
                .startAt(rs.getObject("start_at", OffsetDateTime.class))
                .endAt(rs.getObject("end_at", OffsetDateTime.class))
                .isCancelled(rs.getObject("is_cancelled", Boolean.class))
                .responseStatus(rs.getString("response_status"))
                .importance(rs.getString("importance"))
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
