package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.LegalHoldEntity;
import java.sql.Array;
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
 * Plain-JDBC access to privacy.legal_hold (no soft delete: a released hold stays as evidence). Moving the review date
 * and releasing are each one update guarded by the row_version read and by the hold still being active, so a
 * concurrent change never slips past the service's checks. Every statement filters the caller's tenant itself;
 * row-level security is the backstop.
 */
@Repository
public class LegalHoldRepository {

    /** Fields clients may filter on (contract names → columns); review due = active and reviewBy reached. */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "l.id", ValueType.UUID),
            field("reasonCode", "l.reason_code", ValueType.STRING),
            field("scope", "l.scope", ValueType.STRING),
            field("subjectPersonId", "l.subject_person_id", ValueType.UUID),
            field("categoryCode", "l.category_code", ValueType.STRING),
            field("module", "l.module", ValueType.STRING),
            field("active", "(l.released_at IS NULL)", ValueType.BOOLEAN),
            field("reviewBy", "l.review_by", ValueType.DATE),
            field("placedAt", "l.placed_at", ValueType.TIMESTAMP),
            field("releasedAt", "l.released_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("reviewBy", "l.review_by", ValueType.DATE),
            field("placedAt", "l.placed_at", ValueType.TIMESTAMP));

    /** The register: the nearest review date first. */
    public static final String DEFAULT_ORDER = "reviewBy";
    public static final String ID_COLUMN = "l.id";

    private static final Map<String, Function<LegalHoldEntity, Object>> SORT_VALUES = Map.of(
            "reviewBy", LegalHoldEntity::getReviewBy,
            "placedAt", LegalHoldEntity::getPlacedAt);

    private static final String SELECT = """
            SELECT l.id, l.tenant_id, l.reason_code, l.scope, l.subject_person_id, l.category_code, l.module,
                   l.entity_type, l.entity_ids, l.placed_by_user_id, l.placed_at, l.review_by, l.released_at,
                   l.released_by_user_id, l.created_at, l.created_by, l.updated_at, l.updated_by, l.row_version
              FROM privacy.legal_hold l
             WHERE l.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " AND l.id = :id";
    /**
     * The active holds that may cover the subject's data: their own subject-wide holds and every category or
     * record-set hold of the subject or of everyone, record sets of other modules left out when a module is named.
     */
    private static final String ACTIVE_FOR = SELECT + """
               AND l.released_at IS NULL
               AND (l.subject_person_id = :subjectPersonId OR (l.subject_person_id IS NULL AND l.scope <> 'subject'))
               AND (l.scope <> 'records' OR CAST(:module AS varchar) IS NULL OR l.module = :module)
             ORDER BY l.placed_at, l.id
            """;
    private static final String INSERT = """
            INSERT INTO privacy.legal_hold
            (id, tenant_id, reason_code, scope, subject_person_id, category_code, module, entity_type, entity_ids,
             placed_by_user_id, placed_at, review_by, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :reasonCode, :scope, :subjectPersonId, :categoryCode, :module, :entityType,
                    CAST(:entityIds AS uuid[]), :placedByUserId, :placedAt, :reviewBy, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            """;
    private static final String UPDATE = """
            UPDATE privacy.legal_hold
               SET review_by = :reviewBy,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND released_at IS NULL
            """;
    private static final String RELEASE = """
            UPDATE privacy.legal_hold
               SET released_at = :releasedAt,
                   released_by_user_id = :releasedByUserId,
                   updated_at = :releasedAt,
                   updated_by = :releasedByUserId,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND released_at IS NULL
            """;
    private static final String CATEGORY_EXISTS = """
            SELECT EXISTS (SELECT 1 FROM privacy.data_category c WHERE c.code = :code)
            """;

    private final JdbcClient jdbc;

    public LegalHoldRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LegalHoldEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LegalHoldRepository::map).optional();
    }

    public Page<LegalHoldEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LegalHoldEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LegalHoldRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LegalHoldEntity::getId);
    }

    /** The active holds that may cover the subject's data, oldest first ({@link #ACTIVE_FOR}); module may be null. */
    public List<LegalHoldEntity> activeFor(UUID subjectPersonId, String module) {
        return jdbc.sql(ACTIVE_FOR).param("subjectPersonId", subjectPersonId).param("module", module)
                .query(LegalHoldRepository::map).list();
    }

    public void insert(LegalHoldEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("reasonCode", e.getReasonCode())
                .param("scope", e.getScope())
                .param("subjectPersonId", e.getSubjectPersonId())
                .param("categoryCode", e.getCategoryCode())
                .param("module", e.getModule())
                .param("entityType", e.getEntityType())
                .param("entityIds", e.getEntityIds() == null ? null : e.getEntityIds().toArray(UUID[]::new))
                .param("placedByUserId", e.getPlacedByUserId())
                .param("placedAt", e.getPlacedAt())
                .param("reviewBy", e.getReviewBy())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Moves an active hold's review date; 0 rows means it changed or was released since it was read. */
    public int update(LegalHoldEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("reviewBy", e.getReviewBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** active → released; 0 rows means the hold changed or was released since it was read. */
    public int release(UUID id, int expectedVersion, OffsetDateTime at, UUID actor) {
        return jdbc.sql(RELEASE).param("id", id).param("releasedAt", at).param("releasedByUserId", actor)
                .param("expected", expectedVersion).update();
    }

    /** Whether the code names a data category of the catalogue. */
    public boolean categoryExists(String code) {
        return Boolean.TRUE.equals(jdbc.sql(CATEGORY_EXISTS).param("code", code).query(Boolean.class).single());
    }

    private static List<Object> sortValues(SortSpec sort, LegalHoldEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LegalHoldEntity map(ResultSet rs, int row) throws SQLException {
        return LegalHoldEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .reasonCode(rs.getString("reason_code"))
                .scope(rs.getString("scope"))
                .subjectPersonId(rs.getObject("subject_person_id", UUID.class))
                .categoryCode(rs.getString("category_code"))
                .module(rs.getString("module"))
                .entityType(rs.getString("entity_type"))
                .entityIds(ids(rs.getArray("entity_ids")))
                .placedByUserId(rs.getObject("placed_by_user_id", UUID.class))
                .placedAt(rs.getObject("placed_at", OffsetDateTime.class))
                .reviewBy(rs.getObject("review_by", LocalDate.class))
                .releasedAt(rs.getObject("released_at", OffsetDateTime.class))
                .releasedByUserId(rs.getObject("released_by_user_id", UUID.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    /** A uuid[] column; null stays null (the hold names no records). */
    private static List<UUID> ids(Array array) throws SQLException {
        return array == null ? null : List.of((UUID[]) array.getArray());
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
