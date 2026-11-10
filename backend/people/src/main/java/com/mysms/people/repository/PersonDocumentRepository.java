package com.mysms.people.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.people.config.PeopleDatabaseConfig;
import com.mysms.people.domain.PersonDocumentEntity;
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
 * Plain-JDBC access to people.person_document (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters,
 * live rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class PersonDocumentRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("personId", "r.person_id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("documentType", "r.document_type", ValueType.STRING),
            field("name", "r.name", ValueType.STRING),
            field("fileId", "r.file_id", ValueType.UUID),
            field("documentVersion", "r.document_version", ValueType.INTEGER),
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

    private static final Map<String, Function<PersonDocumentEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", PersonDocumentEntity::getCreatedAt),
            Map.entry("updateTime", PersonDocumentEntity::getUpdatedAt),
            Map.entry("name", PersonDocumentEntity::getName));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.person_id, r.institution_id, r.document_type, r.name, r.file_id,
                   r.document_version, r.status, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM people.person_document r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO people.person_document
            (id, tenant_id, person_id, institution_id, document_type, name, file_id, document_version, status,
             created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :personId, :institutionId, :documentType, :name, :fileId,
                    COALESCE(:documentVersion, 1), COALESCE(:status, 'active'::character varying), :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, person_id, institution_id, document_type, name, file_id, document_version, status,
                      created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE people.person_document
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE people.person_document
               SET institution_id = :institutionId,
                   document_type = :documentType,
                   name = :name,
                   file_id = :fileId,
                   document_version = :documentVersion,
                   status = :status,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UQ_PERSON_DOCUMENT_1_TAKEN = """
            SELECT EXISTS (SELECT 1 FROM people.person_document
                            WHERE tenant_id = :tenantId
                              AND person_id = :personId AND name = :name AND document_version = :documentVersion AND id
                                  <> :id AND ((deleted_at IS NULL)))
            """;
    private static final String EXISTS_PERSON_ID = """
            SELECT EXISTS (SELECT 1 FROM people.person WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public PersonDocumentRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PersonDocumentEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(PersonDocumentRepository::map).optional();
    }

    public Page<PersonDocumentEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<PersonDocumentEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(PersonDocumentRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), PersonDocumentEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public PersonDocumentEntity insert(PersonDocumentEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("personId", e.getPersonId())
                .param("institutionId", e.getInstitutionId())
                .param("documentType", e.getDocumentType())
                .param("name", e.getName())
                .param("fileId", e.getFileId())
                .param("documentVersion", e.getDocumentVersion())
                .param("status", e.getStatus())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(PersonDocumentRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(PersonDocumentEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("documentType", e.getDocumentType())
                .param("name", e.getName())
                .param("fileId", e.getFileId())
                .param("documentVersion", e.getDocumentVersion())
                .param("status", e.getStatus())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(PersonDocumentEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** uq_person_document_1: person_id, name, document_version where (deleted_at IS NULL). */
    public boolean takenPersonIdNameDocumentVersion1(PersonDocumentEntity e) {
        return Boolean.TRUE.equals(jdbc.sql(UQ_PERSON_DOCUMENT_1_TAKEN)
                .param("personId", e.getPersonId())
                .param("name", e.getName())
                .param("documentVersion", e.getDocumentVersion())
               .param("id", e.getId())
                .query(Boolean.class).single());
    }

    /** Whether the referenced people.person row exists (live). */
    public boolean existsPersonId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_PERSON_ID).param("id", id).query(Boolean.class).single());
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    /** Children of one parent row (the parent id of the path). */
    public static SqlFragment ofParent(UUID parentId) {
        return new SqlFragment("r.person_id = :parentId", Map.of("parentId", parentId));
    }

    private static List<Object> sortValues(SortSpec sort, PersonDocumentEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static PersonDocumentEntity map(ResultSet rs, int row) throws SQLException {
        return PersonDocumentEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .personId(rs.getObject("person_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .documentType(rs.getString("document_type"))
                .name(rs.getString("name"))
                .fileId(rs.getObject("file_id", UUID.class))
                .documentVersion(rs.getObject("document_version", Integer.class))
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
