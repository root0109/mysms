package com.mysms.lms.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.QuestionHotspotEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
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
 * Plain-JDBC access to lms.question_hotspot: live rows only, keyset paging, and the row_version guard on every update
 * and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class QuestionHotspotRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("questionId", "r.question_id", ValueType.UUID),
            field("hotspotType", "r.hotspot_type", ValueType.STRING),
            field("imageFileId", "r.image_file_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<QuestionHotspotEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", QuestionHotspotEntity::getCreatedAt),
            Map.entry("updateTime", QuestionHotspotEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.question_id, r.hotspot_type, r.image_file_id, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM lms.question_hotspot r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.question_hotspot
            (id, tenant_id, question_id, hotspot_type, image_file_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :questionId, :hotspotType, :imageFileId, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            RETURNING id, tenant_id, question_id, hotspot_type, image_file_id, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.question_hotspot
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.question_hotspot
               SET hotspot_type = :hotspotType,
                   image_file_id = :imageFileId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_QUESTION_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_QUESTION_HOTSPOT_REGION_QUESTION_HOTSPOT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.question_hotspot_region WHERE tenant_id = :tenantId
                AND question_hotspot_id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public QuestionHotspotRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<QuestionHotspotEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(QuestionHotspotRepository::map).optional();
    }

    public Page<QuestionHotspotEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<QuestionHotspotEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(QuestionHotspotRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), QuestionHotspotEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public QuestionHotspotEntity insert(QuestionHotspotEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("questionId", e.getQuestionId())
                .param("hotspotType", e.getHotspotType())
                .param("imageFileId", e.getImageFileId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(QuestionHotspotRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(QuestionHotspotEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("hotspotType", e.getHotspotType())
                .param("imageFileId", e.getImageFileId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(QuestionHotspotEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.question row exists (live). */
    public boolean existsQuestionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_QUESTION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live lms.question_hotspot_region rows still reference the row. */
    public boolean referencedByQuestionHotspotRegionQuestionHotspotId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_QUESTION_HOTSPOT_REGION_QUESTION_HOTSPOT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.question_id = :parentId", Map.of("parentId", parentId));
    }

    private static final String OF_QUESTIONS = SELECT_LIVE + " AND r.question_id = ANY(:questionIds)"
            + " ORDER BY r.question_id, r.created_at";

    /** The live rows of several questions in one statement: question id → its rows, in order (absent: none). */
    public Map<UUID, List<QuestionHotspotEntity>> ofQuestions(Collection<UUID> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql(OF_QUESTIONS).param("questionIds", questionIds.stream().distinct().toArray(UUID[]::new))
                .query(QuestionHotspotRepository::map).list().stream()
                .collect(Collectors.groupingBy(QuestionHotspotEntity::getQuestionId, LinkedHashMap::new, Collectors.toList()));
    }

    private static List<Object> sortValues(SortSpec sort, QuestionHotspotEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static QuestionHotspotEntity map(ResultSet rs, int row) throws SQLException {
        return QuestionHotspotEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .questionId(rs.getObject("question_id", UUID.class))
                .hotspotType(rs.getString("hotspot_type"))
                .imageFileId(rs.getObject("image_file_id", UUID.class))
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
