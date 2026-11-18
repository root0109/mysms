package com.mysms.admission.repository;
import com.mysms.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.domain.ApplicationEmergencyContactEntity;
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
 * Plain-JDBC access to admission.application_emergency_contact: live rows only, keyset paging, and the row_version
 * guard on every update and delete. Every statement filters the caller's tenant itself; row-level security is the
 * backstop.
 */
@Repository
public class ApplicationEmergencyContactRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("applicationId", "r.application_id", ValueType.UUID),
            field("priority", "r.priority", ValueType.INTEGER),
            field("contactNumber", "r.contact_number", ValueType.STRING),
            field("relationship", "r.relationship", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<ApplicationEmergencyContactEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", ApplicationEmergencyContactEntity::getCreatedAt),
            Map.entry("updateTime", ApplicationEmergencyContactEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.application_id, r.priority, r.contact_number, r.relationship, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM admission.application_emergency_contact r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO admission.application_emergency_contact
            (id, tenant_id, application_id, priority, contact_number, relationship, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :applicationId, :priority, :contactNumber, :relationship, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, application_id, priority, contact_number, relationship, created_at, created_by,
                      updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE admission.application_emergency_contact
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE admission.application_emergency_contact
               SET priority = :priority,
                   contact_number = :contactNumber,
                   relationship = :relationship,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_APPLICATION_EMERGENCY_CONTACT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM admission.application_emergency_contact
                            WHERE tenant_id = :tenantId
                              AND application_id = :applicationId AND priority = :priority AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_APPLICATION_ID = """
            SELECT EXISTS (SELECT 1 FROM admission.application WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REACH = """
             (:scopeAll OR r.application_id IN (SELECT p0.id FROM admission.application p0
                 WHERE p0.tenant_id = :tenantId
                   AND (p0.institution_id = ANY(:scopeIds)
                        OR (p0.created_by = :applicant AND p0.admission_cycle_id IS NOT NULL))))
            """;

    private final JdbcClient jdbc;

    public ApplicationEmergencyContactRepository(@Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<ApplicationEmergencyContactEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(ApplicationEmergencyContactRepository::map).optional();
    }

    public Page<ApplicationEmergencyContactEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<ApplicationEmergencyContactEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(ApplicationEmergencyContactRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), ApplicationEmergencyContactEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public ApplicationEmergencyContactEntity insert(ApplicationEmergencyContactEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("applicationId", e.getApplicationId())
                .param("priority", e.getPriority())
                .param("contactNumber", e.getContactNumber())
                .param("relationship", e.getRelationship())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(ApplicationEmergencyContactRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(ApplicationEmergencyContactEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("priority", e.getPriority())
                .param("contactNumber", e.getContactNumber())
                .param("relationship", e.getRelationship())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(ApplicationEmergencyContactEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_application_emergency_contact_1: application_id, priority where (deleted_at IS NULL). */
    public boolean takenApplicationIdPriority1(ApplicationEmergencyContactEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_APPLICATION_EMERGENCY_CONTACT_1_TAKEN)
                .param("applicationId", e.getApplicationId())
                .param("priority", e.getPriority())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced admission.application row exists (live). */
    public boolean existsApplicationId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_APPLICATION_ID).param("id", id).query(Boolean.class).single());
    }

    /**
     * Rows of applications in the caller's institutions, and of the applications the caller started themselves in an
     * admission cycle.
     */
    public static SqlFragment reach(DataScope scope, UUID applicant) {
        return new SqlFragment(REACH,
                Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray(), "applicant", applicant));
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.application_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, ApplicationEmergencyContactEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static ApplicationEmergencyContactEntity map(ResultSet rs, int row) throws SQLException {
        return ApplicationEmergencyContactEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .applicationId(rs.getObject("application_id", UUID.class))
                .priority(rs.getObject("priority", Integer.class))
                .contactNumber(rs.getString("contact_number"))
                .relationship(rs.getString("relationship"))
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
