package com.mysms.campus.library.repository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.domain.PurchaseRequestSubjectEntity;
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
 * Plain-JDBC access to library.purchase_request_subject: live rows only, keyset paging, and the row_version guard on
 * every update and delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class PurchaseRequestSubjectRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("purchaseRequestId", "r.purchase_request_id", ValueType.UUID),
            field("subjectId", "r.subject_id", ValueType.UUID),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<PurchaseRequestSubjectEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PurchaseRequestSubjectEntity::getCreatedAt),
            Map.entry("updateTime", PurchaseRequestSubjectEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.purchase_request_id, r.subject_id, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM library.purchase_request_subject r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO library.purchase_request_subject
            (id, tenant_id, purchase_request_id, subject_id, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :purchaseRequestId, :subjectId, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, purchase_request_id, subject_id, created_at, created_by, updated_at, updated_by,
                      deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE library.purchase_request_subject
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE library.purchase_request_subject
               SET subject_id = :subjectId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PURCHASE_REQUEST_SUBJECT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM library.purchase_request_subject
                            WHERE tenant_id = :tenantId
                              AND purchase_request_id = :purchaseRequestId AND subject_id = :subjectId AND id <> :id AND
                                  ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_PURCHASE_REQUEST_ID = """
            SELECT EXISTS (SELECT 1 FROM library.purchase_request WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;

    private final JdbcClient jdbc;

    public PurchaseRequestSubjectRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PurchaseRequestSubjectEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PurchaseRequestSubjectRepository::map).optional();
    }

    public Page<PurchaseRequestSubjectEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PurchaseRequestSubjectEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PurchaseRequestSubjectRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PurchaseRequestSubjectEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PurchaseRequestSubjectEntity insert(PurchaseRequestSubjectEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("purchaseRequestId", e.getPurchaseRequestId())
                .param("subjectId", e.getSubjectId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PurchaseRequestSubjectRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PurchaseRequestSubjectEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("subjectId", e.getSubjectId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PurchaseRequestSubjectEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_purchase_request_subject_1: purchase_request_id, subject_id where (deleted_at IS NULL). */
    public boolean takenPurchaseRequestIdSubjectId1(PurchaseRequestSubjectEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PURCHASE_REQUEST_SUBJECT_1_TAKEN)
                .param("purchaseRequestId", e.getPurchaseRequestId())
                .param("subjectId", e.getSubjectId())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced library.purchase_request row exists (live). */
    public boolean existsPurchaseRequestId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PURCHASE_REQUEST_ID).param("id", id).query(Boolean.class).single());
    }

    /** Children of the path's parent row. */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.purchase_request_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, PurchaseRequestSubjectEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PurchaseRequestSubjectEntity map(ResultSet rs, int row) throws SQLException {
        return PurchaseRequestSubjectEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .purchaseRequestId(rs.getObject("purchase_request_id", UUID.class))
                .subjectId(rs.getObject("subject_id", UUID.class))
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
