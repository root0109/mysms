package com.mysms.learning.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.TimetableLegendEntity;
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
 * Plain-JDBC access to lms.timetable_legend: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class TimetableLegendRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("timetableTemplateId", "r.timetable_template_id", ValueType.UUID),
            field("itemKind", "r.item_kind", ValueType.STRING),
            field("color", "r.color", ValueType.STRING),
            field("label", "r.label", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<TimetableLegendEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", TimetableLegendEntity::getCreatedAt),
            Map.entry("updateTime", TimetableLegendEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.cohort_id, r.timetable_template_id, r.item_kind, r.color, r.label,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.timetable_legend r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.timetable_legend
            (id, tenant_id, cohort_id, timetable_template_id, item_kind, color, label, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :cohortId, :timetableTemplateId, :itemKind, :color, :label, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, cohort_id, timetable_template_id, item_kind, color, label, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.timetable_legend
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.timetable_legend
               SET cohort_id = :cohortId,
                   timetable_template_id = :timetableTemplateId,
                   item_kind = :itemKind,
                   color = :color,
                   label = :label,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_TIMETABLE_TEMPLATE_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_template WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.cohort_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public TimetableLegendRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<TimetableLegendEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(TimetableLegendRepository::map).optional();
    }

    public Page<TimetableLegendEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<TimetableLegendEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(TimetableLegendRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), TimetableLegendEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public TimetableLegendEntity insert(TimetableLegendEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("cohortId", e.getCohortId())
                .param("timetableTemplateId", e.getTimetableTemplateId())
                .param("itemKind", e.getItemKind())
                .param("color", e.getColor())
                .param("label", e.getLabel())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(TimetableLegendRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(TimetableLegendEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("cohortId", e.getCohortId())
                .param("timetableTemplateId", e.getTimetableTemplateId())
                .param("itemKind", e.getItemKind())
                .param("color", e.getColor())
                .param("label", e.getLabel())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(TimetableLegendEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.timetable_template row exists (live). */
    public boolean existsTimetableTemplateId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_TIMETABLE_TEMPLATE_ID).param("id", id).query(Boolean.class)
                .single());
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

    private static List<Object> sortValues(SortSpec sort, TimetableLegendEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static TimetableLegendEntity map(ResultSet rs, int row) throws SQLException {
        return TimetableLegendEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .timetableTemplateId(rs.getObject("timetable_template_id", UUID.class))
                .itemKind(rs.getString("item_kind"))
                .color(rs.getString("color"))
                .label(rs.getString("label"))
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
