package com.mysms.assessment.repository;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.domain.GradingBandEntity;
import com.mysms.authclient.DataScope;
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
 * Plain-JDBC access to assessment.grading_band (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class GradingBandRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("gradingSchemeId", "r.grading_scheme_id", ValueType.UUID),
            field("minPercent", "r.min_percent", ValueType.DECIMAL),
            field("maxPercent", "r.max_percent", ValueType.DECIMAL),
            field("gradeLabel", "r.grade_label", ValueType.STRING),
            field("gradePoint", "r.grade_point", ValueType.DECIMAL),
            field("isPass", "r.is_pass", ValueType.BOOLEAN),
            field("description", "r.description", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<GradingBandEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", GradingBandEntity::getCreatedAt),
            Map.entry("updateTime", GradingBandEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.grading_scheme_id, r.min_percent, r.max_percent, r.grade_label, r.grade_point,
                   r.is_pass, r.description, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM assessment.grading_band r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String LIST_FOR_SCHEME = SELECT_LIVE + " AND r.grading_scheme_id = :schemeId ORDER BY r.min_percent, r.max_percent";
    private static final String INSERT = """
            INSERT INTO assessment.grading_band
            (id, tenant_id, grading_scheme_id, min_percent, max_percent, grade_label, grade_point, is_pass,
             description, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :gradingSchemeId, :minPercent, :maxPercent, :gradeLabel, :gradePoint,
                    COALESCE(:isPass, true), :description, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, grading_scheme_id, min_percent, max_percent, grade_label, grade_point, is_pass,
                      description, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE assessment.grading_band
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE assessment.grading_band
               SET grading_scheme_id = :gradingSchemeId,
                   min_percent = :minPercent,
                   max_percent = :maxPercent,
                   grade_label = :gradeLabel,
                   grade_point = :gradePoint,
                   is_pass = :isPass,
                   description = :description,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_GRADING_BAND_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM assessment.grading_band
                            WHERE tenant_id = :tenantId
                              AND grading_scheme_id = :gradingSchemeId AND grade_label = :gradeLabel AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_GRADING_SCHEME_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.grading_scheme WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_RESULT_TEMPLATE_BAND_GRADING_BAND_ID = """
            SELECT EXISTS (SELECT 1 FROM assessment.result_template_band WHERE tenant_id = :tenantId
                AND grading_band_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM assessment.grading_scheme p0 WHERE p0.tenant_id = :tenantId
                AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.grading_scheme_id IN (SELECT p0.id FROM assessment.grading_scheme p0 WHERE p0.tenant_id =
                 :tenantId
                 AND
             (p0.institution_id = ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public GradingBandRepository(@Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<GradingBandEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(GradingBandRepository::map).optional();
    }

    public List<GradingBandEntity> listForScheme(UUID schemeId) {
        return jdbc.sql(LIST_FOR_SCHEME).param("schemeId", schemeId).query(GradingBandRepository::map).list();
    }

    public Page<GradingBandEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<GradingBandEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(GradingBandRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), GradingBandEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public GradingBandEntity insert(GradingBandEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("gradingSchemeId", e.getGradingSchemeId())
                .param("minPercent", e.getMinPercent())
                .param("maxPercent", e.getMaxPercent())
                .param("gradeLabel", e.getGradeLabel())
                .param("gradePoint", e.getGradePoint())
                .param("isPass", e.getIsPass())
                .param("description", e.getDescription())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(GradingBandRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(GradingBandEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("gradingSchemeId", e.getGradingSchemeId())
                .param("minPercent", e.getMinPercent())
                .param("maxPercent", e.getMaxPercent())
                .param("gradeLabel", e.getGradeLabel())
                .param("gradePoint", e.getGradePoint())
                .param("isPass", e.getIsPass())
                .param("description", e.getDescription())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(GradingBandEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_grading_band_1: grading_scheme_id, grade_label where (deleted_at IS NULL). */
    public boolean takenGradingSchemeIdGradeLabel1(GradingBandEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_GRADING_BAND_1_TAKEN)
                .param("gradingSchemeId", e.getGradingSchemeId())
                .param("gradeLabel", e.getGradeLabel())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced assessment.grading_scheme row exists (live). */
    public boolean existsGradingSchemeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_GRADING_SCHEME_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live assessment.result_template_band rows still reference the row. */
    public boolean referencedByResultTemplateBandGradingBandId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_RESULT_TEMPLATE_BAND_GRADING_BAND_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through grading_scheme_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, GradingBandEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static GradingBandEntity map(ResultSet rs, int row) throws SQLException {
        return GradingBandEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .gradingSchemeId(rs.getObject("grading_scheme_id", UUID.class))
                .minPercent(rs.getBigDecimal("min_percent"))
                .maxPercent(rs.getBigDecimal("max_percent"))
                .gradeLabel(rs.getString("grade_label"))
                .gradePoint(rs.getBigDecimal("grade_point"))
                .isPass(rs.getObject("is_pass", Boolean.class))
                .description(rs.getString("description"))
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
