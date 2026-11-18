package com.mysms.academics.repository;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.domain.CurriculumUnitEntity;
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
 * Plain-JDBC access to academics.curriculum_unit: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class CurriculumUnitRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("classSubjectId", "r.class_subject_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("sequence", "r.sequence", ValueType.INTEGER),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<CurriculumUnitEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", CurriculumUnitEntity::getCreatedAt),
            Map.entry("updateTime", CurriculumUnitEntity::getUpdatedAt),
            Map.entry("name", CurriculumUnitEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.class_subject_id, r.name, r.sequence, r.status, r.created_at, r.created_by,
                   r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM academics.curriculum_unit r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO academics.curriculum_unit
            (id, tenant_id, class_subject_id, name, sequence, status, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :classSubjectId, :name, :sequence,
                    COALESCE(:status, 'active'::character varying), :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, class_subject_id, name, sequence, status, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE academics.curriculum_unit
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE academics.curriculum_unit
               SET class_subject_id = :classSubjectId,
                   name = :name,
                   sequence = :sequence,
                   status = :status,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_CLASS_SUBJECT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.class_subject WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LESSON_PLAN_CURRICULUM_UNIT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.lesson_plan WHERE tenant_id = :tenantId
                AND curriculum_unit_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p1.institution_id FROM academics.class_subject p0 JOIN academics.class_section p1 ON p1.tenant_id =
                :tenantId AND p1.id =
            p0.class_section_id WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.class_subject_id IN (SELECT p0.id FROM academics.class_subject p0 WHERE p0.tenant_id =
                 :tenantId
                 AND
             (p0.class_section_id IN (SELECT p1.id FROM academics.class_section p1 WHERE p1.tenant_id = :tenantId
                 AND (p1.institution_id =
             ANY(:scopeIds))))))
            """;

    private final JdbcClient jdbc;

    public CurriculumUnitRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<CurriculumUnitEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(CurriculumUnitRepository::map).optional();
    }

    public Page<CurriculumUnitEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<CurriculumUnitEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(CurriculumUnitRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), CurriculumUnitEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public CurriculumUnitEntity insert(CurriculumUnitEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("classSubjectId", e.getClassSubjectId())
                .param("name", e.getName())
                .param("sequence", e.getSequence())
                .param("status", e.getStatus())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(CurriculumUnitRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(CurriculumUnitEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("classSubjectId", e.getClassSubjectId())
                .param("name", e.getName())
                .param("sequence", e.getSequence())
                .param("status", e.getStatus())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(CurriculumUnitEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced academics.class_subject row exists (live). */
    public boolean existsClassSubjectId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CLASS_SUBJECT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live academics.lesson_plan rows still reference the row. */
    public boolean referencedByLessonPlanCurriculumUnitId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LESSON_PLAN_CURRICULUM_UNIT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through class_subject_id (empty: a tenant-level row, or none). */
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

    private static List<Object> sortValues(SortSpec sort, CurriculumUnitEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static CurriculumUnitEntity map(ResultSet rs, int row) throws SQLException {
        return CurriculumUnitEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .classSubjectId(rs.getObject("class_subject_id", UUID.class))
                .name(rs.getString("name"))
                .sequence(rs.getObject("sequence", Integer.class))
                .status(rs.getString("status"))
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
