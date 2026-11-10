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
import com.mysms.lms.domain.DriveItemShareEntity;
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
 * Plain-JDBC access to lms.drive_item_share (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class DriveItemShareRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("driveItemId", "r.drive_item_id", ValueType.UUID),
            field("userId", "r.user_id", ValueType.UUID),
            field("reaction", "r.reaction", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<DriveItemShareEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", DriveItemShareEntity::getCreatedAt),
            Map.entry("updateTime", DriveItemShareEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.drive_item_id, r.user_id, r.reaction, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.drive_item_share r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.drive_item_share
            (id, tenant_id, drive_item_id, user_id, reaction, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :driveItemId, :userId, :reaction, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, drive_item_id, user_id, reaction, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.drive_item_share
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.drive_item_share
               SET user_id = :userId,
                   reaction = :reaction,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_DRIVE_ITEM_SHARE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.drive_item_share
                            WHERE tenant_id = :tenantId
                              AND drive_item_id = :driveItemId AND user_id = :userId AND id <> :id AND ((deleted_at IS
                                  NULL)))
            """;
    private static final String EXISTS_DRIVE_ITEM_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.drive_item WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM lms.drive_item p0 JOIN lms.course p1 ON p1.tenant_id = :tenantId AND p1.id =
                p0.course_id WHERE p0.tenant_id = :tenantId
                AND p0.id =
            :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.drive_item_id IN (SELECT p0.id FROM lms.drive_item p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.course_id IS NULL OR
             p0.course_id IN (SELECT p1.id FROM lms.course p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id = ANY(:scopeIds) OR
             p1.institution_id IS NULL)))))
            """;

    private final JdbcClient jdbc;

    public DriveItemShareRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<DriveItemShareEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(DriveItemShareRepository::map).optional();
    }

    public Page<DriveItemShareEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<DriveItemShareEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(DriveItemShareRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), DriveItemShareEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public DriveItemShareEntity insert(DriveItemShareEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("driveItemId", e.getDriveItemId())
                .param("userId", e.getUserId())
                .param("reaction", e.getReaction())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(DriveItemShareRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(DriveItemShareEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("userId", e.getUserId())
                .param("reaction", e.getReaction())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(DriveItemShareEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_drive_item_share_1: drive_item_id, user_id where (deleted_at IS NULL). */
    public boolean takenDriveItemIdUserId1(DriveItemShareEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_DRIVE_ITEM_SHARE_1_TAKEN)
                .param("driveItemId", e.getDriveItemId())
                .param("userId", e.getUserId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.drive_item row exists (live). */
    public boolean existsDriveItemId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_DRIVE_ITEM_ID).param("id", id).query(Boolean.class).single());
    }

    /** The institution owning the row reached through drive_item_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.drive_item_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, DriveItemShareEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static DriveItemShareEntity map(ResultSet rs, int row) throws SQLException {
        return DriveItemShareEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .driveItemId(rs.getObject("drive_item_id", UUID.class))
                .userId(rs.getObject("user_id", UUID.class))
                .reaction(rs.getString("reaction"))
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
