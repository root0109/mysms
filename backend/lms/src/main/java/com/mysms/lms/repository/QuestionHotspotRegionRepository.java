package com.mysms.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.QuestionHotspotRegionEntity;
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
 * Plain-JDBC access to lms.question_hotspot_region: live rows only, keyset paging, and the row_version guard on every
 * update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class QuestionHotspotRegionRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("questionHotspotId", "r.question_hotspot_id", ValueType.UUID),
            field("label", "r.label", ValueType.STRING),
            field("shape", "r.shape", ValueType.STRING),
            field("coordinates", "r.coordinates", ValueType.STRING),
            field("isCorrect", "r.is_correct", ValueType.BOOLEAN),
            field("feedback", "r.feedback", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<QuestionHotspotRegionEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", QuestionHotspotRegionEntity::getCreatedAt),
            Map.entry("updateTime", QuestionHotspotRegionEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.question_hotspot_id, r.label, r.shape, r.coordinates, r.is_correct,
                   r.feedback, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.question_hotspot_region r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.question_hotspot_region
            (id, tenant_id, question_hotspot_id, label, shape, coordinates, is_correct, feedback, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :questionHotspotId, :label, :shape, :coordinates, COALESCE(:isCorrect, false),
                    :feedback, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, question_hotspot_id, label, shape, coordinates, is_correct, feedback, created_at,
                      created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.question_hotspot_region
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.question_hotspot_region
               SET question_hotspot_id = :questionHotspotId,
                   label = :label,
                   shape = :shape,
                   coordinates = :coordinates,
                   is_correct = :isCorrect,
                   feedback = :feedback,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_QUESTION_HOTSPOT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_hotspot WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public QuestionHotspotRegionRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<QuestionHotspotRegionEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(QuestionHotspotRegionRepository::map).optional();
    }

    public Page<QuestionHotspotRegionEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<QuestionHotspotRegionEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(QuestionHotspotRegionRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), QuestionHotspotRegionEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public QuestionHotspotRegionEntity insert(QuestionHotspotRegionEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("questionHotspotId", e.getQuestionHotspotId())
                .param("label", e.getLabel())
                .param("shape", e.getShape())
                .param("coordinates", e.getCoordinates())
                .param("isCorrect", e.getIsCorrect())
                .param("feedback", e.getFeedback())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(QuestionHotspotRegionRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(QuestionHotspotRegionEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("questionHotspotId", e.getQuestionHotspotId())
                .param("label", e.getLabel())
                .param("shape", e.getShape())
                .param("coordinates", e.getCoordinates())
                .param("isCorrect", e.getIsCorrect())
                .param("feedback", e.getFeedback())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(QuestionHotspotRegionEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.question_hotspot row exists (live). */
    public boolean existsQuestionHotspotId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUESTION_HOTSPOT_ID).param("id", id).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, QuestionHotspotRegionEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static QuestionHotspotRegionEntity map(ResultSet rs, int row) throws SQLException {
        return QuestionHotspotRegionEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .questionHotspotId(rs.getObject("question_hotspot_id", UUID.class))
                .label(rs.getString("label"))
                .shape(rs.getString("shape"))
                .coordinates(rs.getString("coordinates"))
                .isCorrect(rs.getObject("is_correct", Boolean.class))
                .feedback(rs.getString("feedback"))
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
