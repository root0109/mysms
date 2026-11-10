package com.mysms.library.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.domain.PurchaseRequestCourseEntity;
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
 * Plain-JDBC access to library.purchase_request_course (JAVA-CODING-RULES §6): schema-qualified constant SQL, named
 * parameters, live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class PurchaseRequestCourseRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("purchaseRequestId", "r.purchase_request_id", ValueType.UUID),
            field("courseId", "r.course_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PurchaseRequestCourseEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PurchaseRequestCourseEntity::getCreatedAt),
            Map.entry("updateTime", PurchaseRequestCourseEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.purchase_request_id, r.course_id, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM library.purchase_request_course r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO library.purchase_request_course
            (id, tenant_id, purchase_request_id, course_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :purchaseRequestId, :courseId, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, purchase_request_id, course_id, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE library.purchase_request_course
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE library.purchase_request_course
               SET course_id = :courseId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PURCHASE_REQUEST_COURSE_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM library.purchase_request_course
                            WHERE tenant_id = :tenantId
                              AND purchase_request_id = :purchaseRequestId AND course_id = :courseId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_PURCHASE_REQUEST_ID = """
            SELECT EXISTS (SELECT 1 FROM library.purchase_request WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public PurchaseRequestCourseRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PurchaseRequestCourseEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PurchaseRequestCourseRepository::map).optional();
    }

    public Page<PurchaseRequestCourseEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PurchaseRequestCourseEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PurchaseRequestCourseRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PurchaseRequestCourseEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PurchaseRequestCourseEntity insert(PurchaseRequestCourseEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("purchaseRequestId", e.getPurchaseRequestId())
                .param("courseId", e.getCourseId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PurchaseRequestCourseRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PurchaseRequestCourseEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("courseId", e.getCourseId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PurchaseRequestCourseEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_purchase_request_course_1: purchase_request_id, course_id where (deleted_at IS NULL). */
    public boolean takenPurchaseRequestIdCourseId1(PurchaseRequestCourseEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PURCHASE_REQUEST_COURSE_1_TAKEN)
                .param("purchaseRequestId", e.getPurchaseRequestId())
                .param("courseId", e.getCourseId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced library.purchase_request row exists (live). */
    public boolean existsPurchaseRequestId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PURCHASE_REQUEST_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.purchase_request_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, PurchaseRequestCourseEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PurchaseRequestCourseEntity map(ResultSet rs, int row) throws SQLException {
        return PurchaseRequestCourseEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .purchaseRequestId(rs.getObject("purchase_request_id", UUID.class))
                .courseId(rs.getObject("course_id", UUID.class))
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
