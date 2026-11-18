package com.mysms.academics.repository;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.domain.ClassSubjectEntity;
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
 * Plain-JDBC access to academics.class_subject: live rows only, keyset paging over an allowlisted order and the
 * row_version guard on every update and delete. Every statement filters the caller's tenant itself; row-level security
 * is the backstop.
 */
@Repository
public class ClassSubjectRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("classSectionId", "r.class_section_id", ValueType.UUID),
            field("subjectId", "r.subject_id", ValueType.UUID),
            field("offeringType", "r.offering_type", ValueType.STRING),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ClassSubjectEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ClassSubjectEntity::getCreatedAt),
            Map.entry("updateTime", ClassSubjectEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.class_section_id, r.subject_id, r.offering_type, r.is_active, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM academics.class_subject r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO academics.class_subject
            (id, tenant_id, class_section_id, subject_id, offering_type, is_active, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :classSectionId, :subjectId, COALESCE(:offeringType, 'core'::character varying),
                    COALESCE(:isActive, true), :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, class_section_id, subject_id, offering_type, is_active, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE academics.class_subject
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE academics.class_subject
               SET subject_id = :subjectId,
                   offering_type = :offeringType,
                   is_active = :isActive,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_CLASS_SUBJECT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM academics.class_subject
                            WHERE tenant_id = :tenantId
                              AND class_section_id = :classSectionId AND subject_id = :subjectId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_CLASS_SECTION_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.class_section WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_SUBJECT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.subject WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CURRICULUM_UNIT_CLASS_SUBJECT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.curriculum_unit WHERE tenant_id = :tenantId
                AND class_subject_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TEACHING_ASSIGNMENT_CLASS_SUBJECT_ID = """
            SELECT EXISTS (SELECT 1 FROM academics.teaching_assignment WHERE tenant_id = :tenantId
                AND class_subject_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM academics.class_section p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.class_section_id IN (SELECT p0.id FROM academics.class_section p0 WHERE p0.tenant_id =
                 :tenantId
                 AND (p0.institution_id
             = ANY(:scopeIds))))
            """;

    private final JdbcClient jdbc;

    public ClassSubjectRepository(@Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ClassSubjectEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ClassSubjectRepository::map).optional();
    }

    public Page<ClassSubjectEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ClassSubjectEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ClassSubjectRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ClassSubjectEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ClassSubjectEntity insert(ClassSubjectEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("classSectionId", e.getClassSectionId())
                .param("subjectId", e.getSubjectId())
                .param("offeringType", e.getOfferingType())
                .param("isActive", e.getIsActive())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ClassSubjectRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ClassSubjectEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("subjectId", e.getSubjectId())
                .param("offeringType", e.getOfferingType())
                .param("isActive", e.getIsActive())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ClassSubjectEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_class_subject_1: class_section_id, subject_id where (deleted_at IS NULL). */
    public boolean takenClassSectionIdSubjectId1(ClassSubjectEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_CLASS_SUBJECT_1_TAKEN)
                .param("classSectionId", e.getClassSectionId())
                .param("subjectId", e.getSubjectId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced academics.class_section row exists (live). */
    public boolean existsClassSectionId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CLASS_SECTION_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced academics.subject row exists (live). */
    public boolean existsSubjectId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_SUBJECT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live academics.curriculum_unit rows still reference the row. */
    public boolean referencedByCurriculumUnitClassSubjectId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CURRICULUM_UNIT_CLASS_SUBJECT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live academics.teaching_assignment rows still reference the row. */
    public boolean referencedByTeachingAssignmentClassSubjectId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TEACHING_ASSIGNMENT_CLASS_SUBJECT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through class_section_id (empty: a tenant-level row, or none). */
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
        return new SqlFragment("r.class_section_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ClassSubjectEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ClassSubjectEntity map(ResultSet rs, int row) throws SQLException {
        return ClassSubjectEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .classSectionId(rs.getObject("class_section_id", UUID.class))
                .subjectId(rs.getObject("subject_id", UUID.class))
                .offeringType(rs.getString("offering_type"))
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
