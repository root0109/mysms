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
import com.mysms.lms.domain.ParticipationActivityEntity;
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
 * Plain-JDBC access to lms.participation_activity: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class ParticipationActivityRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("isInRepository", "r.is_in_repository", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ParticipationActivityEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ParticipationActivityEntity::getCreatedAt),
            Map.entry("updateTime", ParticipationActivityEntity::getUpdatedAt),
            Map.entry("title", ParticipationActivityEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.title, r.description, r.is_in_repository, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.participation_activity r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.participation_activity
            (id, tenant_id, institution_id, title, description, is_in_repository, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :title, :description, COALESCE(:isInRepository, false),
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, title, description, is_in_repository, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.participation_activity
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.participation_activity
               SET institution_id = :institutionId,
                   title = :title,
                   description = :description,
                   is_in_repository = :isInRepository,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_PARTICIPATION_ACTIVITY_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND participation_activity_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public ParticipationActivityRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ParticipationActivityEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ParticipationActivityRepository::map).optional();
    }

    public Page<ParticipationActivityEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ParticipationActivityEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ParticipationActivityRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ParticipationActivityEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ParticipationActivityEntity insert(ParticipationActivityEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("isInRepository", e.getIsInRepository())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ParticipationActivityRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ParticipationActivityEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("isInRepository", e.getIsInRepository())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ParticipationActivityEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemParticipationActivityId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_PARTICIPATION_ACTIVITY_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, ParticipationActivityEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ParticipationActivityEntity map(ResultSet rs, int row) throws SQLException {
        return ParticipationActivityEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .isInRepository(rs.getObject("is_in_repository", Boolean.class))
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
