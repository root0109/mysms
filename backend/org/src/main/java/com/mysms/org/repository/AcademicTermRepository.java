package com.mysms.org.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.org.config.OrgDatabaseConfig;
import com.mysms.org.domain.AcademicTermEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Plain-JDBC access to org.academic_term: live rows only, keyset paging over an allowlisted order and the row_version
 * guard on every update and delete. Every statement filters the caller's tenant itself; row-level security is the
 * backstop.
 */
@Repository
public class AcademicTermRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("parentTermId", "r.parent_term_id", ValueType.UUID),
            field("name", "r.name", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE),
            field("endDate", "r.end_date", ValueType.DATE),
            field("sequence", "r.sequence", ValueType.INTEGER),
            field("code", "r.code", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("name", "r.name", ValueType.STRING),
            field("startDate", "r.start_date", ValueType.DATE));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<AcademicTermEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", AcademicTermEntity::getCreatedAt),
            Map.entry("updateTime", AcademicTermEntity::getUpdatedAt),
            Map.entry("name", AcademicTermEntity::getName),
            Map.entry("startDate", AcademicTermEntity::getStartDate));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.academic_year_id, r.parent_term_id, r.name, r.start_date, r.end_date,
                   r.sequence, r.code, r.is_active, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM org.academic_term r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO org.academic_term
            (id, tenant_id, academic_year_id, parent_term_id, name, start_date, end_date, sequence, code, is_active,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :academicYearId, :parentTermId, :name, :startDate, :endDate, :sequence, :code,
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, academic_year_id, parent_term_id, name, start_date, end_date, sequence, code,
                      is_active, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE org.academic_term
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE org.academic_term
               SET parent_term_id = :parentTermId,
                   name = :name,
                   start_date = :startDate,
                   end_date = :endDate,
                   sequence = :sequence,
                   code = :code,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_ACADEMIC_TERM_NAME_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM org.academic_term
                            WHERE tenant_id = :tenantId
                              AND academic_year_id IS NOT DISTINCT FROM :academicYearId AND parent_term_id IS NOT
                                  DISTINCT FROM :parentTermId AND name IS NOT DISTINCT FROM :name AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String UQ_ACADEMIC_TERM_START_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM org.academic_term
                            WHERE tenant_id = :tenantId
                              AND academic_year_id IS NOT DISTINCT FROM :academicYearId AND parent_term_id IS NOT
                                  DISTINCT FROM :parentTermId AND start_date IS NOT DISTINCT FROM :startDate AND id <>
                                  :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_ACADEMIC_YEAR_ID = """
            SELECT EXISTS (SELECT 1 FROM org.academic_year WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_PARENT_TERM_ID = """
            SELECT EXISTS (SELECT 1 FROM org.academic_term WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_ACADEMIC_TERM_PARENT_TERM_ID = """
            SELECT EXISTS (SELECT 1 FROM org.academic_term WHERE tenant_id = :tenantId
                AND parent_term_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM org.academic_year p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.academic_year_id IN (SELECT p0.id FROM org.academic_year p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id =
             ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public AcademicTermRepository(@Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AcademicTermEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(AcademicTermRepository::map).optional();
    }

    public Page<AcademicTermEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<AcademicTermEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(AcademicTermRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), AcademicTermEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public AcademicTermEntity insert(AcademicTermEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("academicYearId", e.getAcademicYearId())
                .param("parentTermId", e.getParentTermId())
                .param("name", e.getName())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("sequence", e.getSequence())
                .param("code", e.getCode())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(AcademicTermRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(AcademicTermEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("parentTermId", e.getParentTermId())
                .param("name", e.getName())
                .param("startDate", e.getStartDate())
                .param("endDate", e.getEndDate())
                .param("sequence", e.getSequence())
                .param("code", e.getCode())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(AcademicTermEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_academic_term_name: academic_year_id, parent_term_id, name where (deleted_at IS NULL). */
    public boolean takenAcademicYearIdParentTermIdNameName(AcademicTermEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ACADEMIC_TERM_NAME_TAKEN)
                .param("academicYearId", e.getAcademicYearId())
                .param("parentTermId", e.getParentTermId())
                .param("name", e.getName())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** uq_academic_term_start: academic_year_id, parent_term_id, start_date where (deleted_at IS NULL). */
    public boolean takenAcademicYearIdParentTermIdStartDateStart(AcademicTermEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_ACADEMIC_TERM_START_TAKEN)
                .param("academicYearId", e.getAcademicYearId())
                .param("parentTermId", e.getParentTermId())
                .param("startDate", e.getStartDate())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced org.academic_year row exists (live). */
    public boolean existsAcademicYearId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_ACADEMIC_YEAR_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced org.academic_term row exists (live). */
    public boolean existsParentTermId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PARENT_TERM_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live org.academic_term rows still reference the row. */
    public boolean referencedByAcademicTermParentTermId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_ACADEMIC_TERM_PARENT_TERM_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through academic_year_id (empty: a tenant-level row, or none). */
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

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.academic_year_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, AcademicTermEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static AcademicTermEntity map(ResultSet rs, int row) throws SQLException {
        return AcademicTermEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .parentTermId(rs.getObject("parent_term_id", UUID.class))
                .name(rs.getString("name"))
                .startDate(rs.getObject("start_date", LocalDate.class))
                .endDate(rs.getObject("end_date", LocalDate.class))
                .sequence(rs.getObject("sequence", Integer.class))
                .code(rs.getString("code"))
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
