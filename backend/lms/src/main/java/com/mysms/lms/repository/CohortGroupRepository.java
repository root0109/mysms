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
import com.mysms.lms.domain.CohortGroupEntity;
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
 * Plain-JDBC access to lms.cohort_group: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class CohortGroupRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("isDisabled", "r.is_disabled", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CohortGroupEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CohortGroupEntity::getCreatedAt),
            Map.entry("updateTime", CohortGroupEntity::getUpdatedAt),
            Map.entry("name", CohortGroupEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.cohort_id, r.name, r.description, r.is_disabled, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.cohort_group r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.cohort_group
            (id, tenant_id, cohort_id, name, description, is_disabled, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :cohortId, :name, :description, COALESCE(:isDisabled, false), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, cohort_id, name, description, is_disabled, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.cohort_group
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.cohort_group
               SET name = :name,
                   description = :description,
                   is_disabled = :isDisabled,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_COHORT_GROUP_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort_group
                            WHERE tenant_id = :tenantId
                              AND cohort_id = :cohortId AND name = :name AND id <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_FORUM_SECTION_COHORT_GROUP_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.forum_section WHERE tenant_id = :tenantId
                AND cohort_group_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)))
            """;

    private final JdbcClient jdbc;

    public CohortGroupRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CohortGroupEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CohortGroupRepository::map).optional();
    }

    public Page<CohortGroupEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CohortGroupEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CohortGroupRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CohortGroupEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CohortGroupEntity insert(CohortGroupEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("cohortId", e.getCohortId())
                .param("name", e.getName())
                .param("description", e.getDescription())
                .param("isDisabled", e.getIsDisabled())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CohortGroupRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CohortGroupEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("name", e.getName())
                .param("description", e.getDescription())
                .param("isDisabled", e.getIsDisabled())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CohortGroupEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_cohort_group_1: cohort_id, name where (deleted_at IS NULL). */
    public boolean takenCohortIdName1(CohortGroupEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_COHORT_GROUP_1_TAKEN)
                .param("cohortId", e.getCohortId())
                .param("name", e.getName())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.forum_section rows still reference the row. */
    public boolean referencedByForumSectionCohortGroupId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_FORUM_SECTION_COHORT_GROUP_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through cohort_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.cohort_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, CohortGroupEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CohortGroupEntity map(ResultSet rs, int row) throws SQLException {
        return CohortGroupEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .isDisabled(rs.getObject("is_disabled", Boolean.class))
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
