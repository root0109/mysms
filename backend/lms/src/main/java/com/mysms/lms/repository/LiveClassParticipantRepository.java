package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.LiveClassParticipantEntity;
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
 * Plain-JDBC access to lms.live_class_participant: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class LiveClassParticipantRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("liveClassId", "r.live_class_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("joinedAt", "r.joined_at", ValueType.TIMESTAMP),
            field("leftAt", "r.left_at", ValueType.TIMESTAMP),
            field("durationMinutes", "r.duration_minutes", ValueType.INTEGER),
            field("isPresenter", "r.is_presenter", ValueType.BOOLEAN),
            field("attended", "r.attended", ValueType.BOOLEAN),
            field("inviteStatus", "r.invite_status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LiveClassParticipantEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LiveClassParticipantEntity::getCreatedAt),
            Map.entry("updateTime", LiveClassParticipantEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.live_class_id, r.user_id, r.joined_at, r.left_at, r.duration_minutes,
                   r.join_url_enc, r.is_presenter, r.attended, r.invite_status, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.live_class_participant r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.live_class_participant
            (id, tenant_id, live_class_id, user_id, joined_at, left_at, duration_minutes, join_url_enc, is_presenter,
             attended, invite_status, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :liveClassId, :userId, :joinedAt, :leftAt, :durationMinutes, :joinUrlEnc,
                    COALESCE(:isPresenter, false), COALESCE(:attended, false), :inviteStatus, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, live_class_id, user_id, joined_at, left_at, duration_minutes, join_url_enc,
                      is_presenter, attended, invite_status, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.live_class_participant
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.live_class_participant
               SET updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_LIVE_CLASS_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.live_class WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.live_class p0 JOIN lms.cohort p1 ON p1.tenant_id = :tenantId AND p1.id =
                p0.cohort_id WHERE p0.tenant_id = :tenantId
                AND p0.id =
            :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.live_class_id IN (SELECT p0.id FROM lms.live_class p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.cohort_id IS NULL OR
             p0.cohort_id IN (SELECT p1.id FROM lms.cohort p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR
             p1.institution_id IS NULL)))))
            """;

    private final JdbcClient jdbc;

    public LiveClassParticipantRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LiveClassParticipantEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LiveClassParticipantRepository::map).optional();
    }

    public Page<LiveClassParticipantEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LiveClassParticipantEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LiveClassParticipantRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LiveClassParticipantEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LiveClassParticipantEntity insert(LiveClassParticipantEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("liveClassId", e.getLiveClassId())
                .param("userId", e.getUserId())
                .param("joinedAt", e.getJoinedAt())
                .param("leftAt", e.getLeftAt())
                .param("durationMinutes", e.getDurationMinutes())
                .param("joinUrlEnc", e.getJoinUrlEnc())
                .param("isPresenter", e.getIsPresenter())
                .param("attended", e.getAttended())
                .param("inviteStatus", e.getInviteStatus())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LiveClassParticipantRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LiveClassParticipantEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())

                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LiveClassParticipantEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.live_class row exists (live). */
    public boolean existsLiveClassId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_LIVE_CLASS_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through live_class_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.live_class_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, LiveClassParticipantEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LiveClassParticipantEntity map(ResultSet rs, int row) throws SQLException {
        return LiveClassParticipantEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .liveClassId(rs.getObject("live_class_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .joinedAt(rs.getObject("joined_at", OffsetDateTime.class))
                .leftAt(rs.getObject("left_at", OffsetDateTime.class))
                .durationMinutes(rs.getObject("duration_minutes", Integer.class))
                .joinUrlEnc(rs.getString("join_url_enc"))
                .isPresenter(rs.getObject("is_presenter", Boolean.class))
                .attended(rs.getObject("attended", Boolean.class))
                .inviteStatus(rs.getString("invite_status"))
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
