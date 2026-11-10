package com.mysms.library.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.domain.LibraryCardEntity;
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
 * Plain-JDBC access to library.library_card (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class LibraryCardRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("cardNumber", "r.card_number", ValueType.STRING),
            field("cardTypeId", "r.card_type_id", ValueType.UUID),
            field("holderType", "r.holder_type", ValueType.STRING),
            field("personId", "r.person_id", ValueType.UUID),
            field("studentId", "r.student_id", ValueType.UUID),
            field("staffId", "r.staff_id", ValueType.UUID),
            field("maxItemsOverride", "r.max_items_override", ValueType.INTEGER),
            field("issueDate", "r.issue_date", ValueType.DATE),
            field("expiryDate", "r.expiry_date", ValueType.DATE),
            field("status", "r.status", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LibraryCardEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LibraryCardEntity::getCreatedAt),
            Map.entry("updateTime", LibraryCardEntity::getUpdatedAt));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.card_number, r.card_type_id, r.holder_type, r.person_id,
                   r.student_id, r.staff_id, r.max_items_override, r.issue_date, r.expiry_date, r.status,
                   r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM library.library_card r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO library.library_card
            (id, tenant_id, institution_id, card_number, card_type_id, holder_type, person_id, student_id, staff_id,
             max_items_override, issue_date, expiry_date, status, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :institutionId, :cardNumber, :cardTypeId, :holderType, :personId, :studentId,
                    :staffId, :maxItemsOverride, :issueDate, :expiryDate,
                    COALESCE(:status, 'active'::character varying), :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            RETURNING id, tenant_id, institution_id, card_number, card_type_id, holder_type, person_id, student_id,
                      staff_id, max_items_override, issue_date, expiry_date, status, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE library.library_card
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE library.library_card
               SET institution_id = :institutionId,
                   card_number = :cardNumber,
                   card_type_id = :cardTypeId,
                   holder_type = :holderType,
                   person_id = :personId,
                   student_id = :studentId,
                   staff_id = :staffId,
                   max_items_override = :maxItemsOverride,
                   issue_date = :issueDate,
                   expiry_date = :expiryDate,
                   status = :status,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_LIBRARY_CARD_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM library.library_card
                            WHERE tenant_id = :tenantId
                              AND institution_id = :institutionId AND card_number = :cardNumber AND id <> :id)
            """;
    private static final String EXISTS_CARD_TYPE_ID = """
            SELECT EXISTS (SELECT 1 FROM library.library_card_type WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LOAN_LIBRARY_CARD_ID = """
            SELECT EXISTS (SELECT 1 FROM library.loan WHERE tenant_id = :tenantId
                AND library_card_id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds))
            """;

    private final JdbcClient jdbc;

    public LibraryCardRepository(@Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LibraryCardEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LibraryCardRepository::map).optional();
    }

    public Page<LibraryCardEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LibraryCardEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LibraryCardRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LibraryCardEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LibraryCardEntity insert(LibraryCardEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("cardNumber", e.getCardNumber())
                .param("cardTypeId", e.getCardTypeId())
                .param("holderType", e.getHolderType())
                .param("personId", e.getPersonId())
                .param("studentId", e.getStudentId())
                .param("staffId", e.getStaffId())
                .param("maxItemsOverride", e.getMaxItemsOverride())
                .param("issueDate", e.getIssueDate())
                .param("expiryDate", e.getExpiryDate())
                .param("status", e.getStatus())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LibraryCardRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LibraryCardEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("cardNumber", e.getCardNumber())
                .param("cardTypeId", e.getCardTypeId())
                .param("holderType", e.getHolderType())
                .param("personId", e.getPersonId())
                .param("studentId", e.getStudentId())
                .param("staffId", e.getStaffId())
                .param("maxItemsOverride", e.getMaxItemsOverride())
                .param("issueDate", e.getIssueDate())
                .param("expiryDate", e.getExpiryDate())
                .param("status", e.getStatus())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LibraryCardEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_library_card_1: institution_id, card_number. */
    public boolean takenInstitutionIdCardNumber1(LibraryCardEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_LIBRARY_CARD_1_TAKEN)
                .param("institutionId", e.getInstitutionId())
                .param("cardNumber", e.getCardNumber())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced library.library_card_type row exists (live). */
    public boolean existsCardTypeId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CARD_TYPE_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether live library.loan rows still reference the row. */
    public boolean referencedByLoanLibraryCardId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LOAN_LIBRARY_CARD_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, LibraryCardEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LibraryCardEntity map(ResultSet rs, int row) throws SQLException {
        return LibraryCardEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .cardNumber(rs.getString("card_number"))
                .cardTypeId(rs.getObject("card_type_id", UUID.class))
                .holderType(rs.getString("holder_type"))
                .personId(rs.getObject("person_id", UUID.class))
                .studentId(rs.getObject("student_id", UUID.class))
                .staffId(rs.getObject("staff_id", UUID.class))
                .maxItemsOverride(rs.getObject("max_items_override", Integer.class))
                .issueDate(rs.getObject("issue_date", LocalDate.class))
                .expiryDate(rs.getObject("expiry_date", LocalDate.class))
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
