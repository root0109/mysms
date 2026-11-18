package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.AudienceGroupEntity;
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
 * Plain-JDBC access to comms.audience_group: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class AudienceGroupRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("classSectionId", "r.class_section_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("groupScope", "r.group_scope", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AudienceGroupEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AudienceGroupEntity::getCreatedAt),
            Map.entry("updateTime", AudienceGroupEntity::getUpdatedAt),
            Map.entry("name", AudienceGroupEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.class_section_id, r.name, r.group_scope, r.is_active,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM comms.audience_group r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.audience_group
            (id, tenant_id, institution_id, class_section_id, name, group_scope, is_active, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :classSectionId, :name,
                    COALESCE(:groupScope, 'class'::character varying), COALESCE(:isActive, true), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, class_section_id, name, group_scope, is_active, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.audience_group
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.audience_group
               SET institution_id = :institutionId,
                   class_section_id = :classSectionId,
                   name = :name,
                   group_scope = :groupScope,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_AUDIENCE_GROUP_MEMBER_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.audience_group_member WHERE tenant_id = :tenantId
                AND group_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TASK_ASSIGNEE_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.task_assignee WHERE tenant_id = :tenantId
                AND group_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public AudienceGroupRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AudienceGroupEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AudienceGroupRepository::map).optional();
    }

    public Page<AudienceGroupEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AudienceGroupEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AudienceGroupRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AudienceGroupEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AudienceGroupEntity insert(AudienceGroupEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("classSectionId", e.getClassSectionId())
                .param("name", e.getName())
                .param("groupScope", e.getGroupScope())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AudienceGroupRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AudienceGroupEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("classSectionId", e.getClassSectionId())
                .param("name", e.getName())
                .param("groupScope", e.getGroupScope())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AudienceGroupEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live comms.audience_group_member rows still reference the row. */
    public boolean referencedByAudienceGroupMemberGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_AUDIENCE_GROUP_MEMBER_GROUP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live comms.task_assignee rows still reference the row. */
    public boolean referencedByTaskAssigneeGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TASK_ASSIGNEE_GROUP_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, AudienceGroupEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AudienceGroupEntity map(ResultSet rs, int row) throws SQLException {
        return AudienceGroupEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .classSectionId(rs.getObject("class_section_id", UUID.class))
                .name(rs.getString("name"))
                .groupScope(rs.getString("group_scope"))
                .isActive(rs.getObject("is_active", Boolean.class))
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
