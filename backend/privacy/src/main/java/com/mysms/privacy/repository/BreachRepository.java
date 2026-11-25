package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.BreachEntity;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to privacy.breach (no soft delete: the register is evidence). An edit and every step are one
 * update guarded by the row_version read; a step also by the status it leaves, so a concurrent change never slips past
 * the service's checks. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class BreachRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "b.id", ValueType.UUID),
            field("number", "b.number", ValueType.STRING),
            field("status", "b.status", ValueType.STRING),
            field("ownerUserId", "b.owner_user_id", ValueType.UUID),
            field("detectedAt", "b.detected_at", ValueType.TIMESTAMP),
            field("becameAwareAt", "b.became_aware_at", ValueType.TIMESTAMP),
            field("boardReportDueAt", "b.board_report_due_at", ValueType.TIMESTAMP),
            field("closedAt", "b.closed_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("number", "b.number", ValueType.STRING),
            field("status", "b.status", ValueType.STRING),
            field("becameAwareAt", "b.became_aware_at", ValueType.TIMESTAMP),
            field("boardReportDueAt", "b.board_report_due_at", ValueType.TIMESTAMP));

    /** The register: the Board's nearest deadline first. */
    public static final String DEFAULT_ORDER = "boardReportDueAt";
    public static final String ID_COLUMN = "b.id";

    private static final Map<String, Function<BreachEntity, Object>> SORT_VALUES = Map.of(
            "number", BreachEntity::getNumber,
            "status", BreachEntity::getStatus,
            "becameAwareAt", BreachEntity::getBecameAwareAt,
            "boardReportDueAt", BreachEntity::getBoardReportDueAt);

    private static final String SELECT = """
            SELECT b.id, b.tenant_id, b.number, b.status, b.detected_at, b.became_aware_at, b.board_report_due_at,
                   b.description, b.categories, b.modules, b.estimated_principals, b.likely_consequences, b.mitigation,
                   b.owner_user_id, b.board_report_sent_at, b.not_a_breach_reason, b.closed_at, b.created_at,
                   b.created_by, b.updated_at, b.updated_by, b.row_version
              FROM privacy.breach b
             WHERE b.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " AND b.id = :id";
    private static final String INSERT = """
            INSERT INTO privacy.breach
            (id, tenant_id, number, status, detected_at, became_aware_at, board_report_due_at, description, categories,
             modules, estimated_principals, likely_consequences, mitigation, owner_user_id, created_at, created_by,
             updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :number, :status, :detectedAt, :becameAwareAt, :boardReportDueAt, :description,
                    CAST(:categories AS varchar[]), CAST(:modules AS varchar[]), :estimatedPrincipals,
                    :likelyConsequences, :mitigation, :ownerUserId, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            """;
    private static final String UPDATE = """
            UPDATE privacy.breach
               SET description = :description,
                   categories = CAST(:categories AS varchar[]),
                   modules = CAST(:modules AS varchar[]),
                   estimated_principals = :estimatedPrincipals,
                   likely_consequences = :likelyConsequences,
                   mitigation = :mitigation,
                   owner_user_id = :ownerUserId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND closed_at IS NULL
            """;
    private static final String ADVANCE = """
            UPDATE privacy.breach
               SET status = :status,
                   board_report_sent_at = :boardReportSentAt,
                   not_a_breach_reason = :notABreachReason,
                   closed_at = :closedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = :fromStatus
            """;
    private static final String KNOWN_CATEGORIES = """
            SELECT c.code FROM privacy.data_category c WHERE c.code = ANY(CAST(:codes AS varchar[]))
            """;

    private final JdbcClient jdbc;

    public BreachRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<BreachEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(BreachRepository::map).optional();
    }

    public Page<BreachEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<BreachEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(BreachRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), BreachEntity::getId);
    }

    public void insert(BreachEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("number", e.getNumber())
                .param("status", e.getStatus())
                .param("detectedAt", e.getDetectedAt())
                .param("becameAwareAt", e.getBecameAwareAt())
                .param("boardReportDueAt", e.getBoardReportDueAt())
                .param("description", e.getDescription())
                .param("categories", e.getCategories().toArray(String[]::new))
                .param("modules", e.getModules().toArray(String[]::new))
                .param("estimatedPrincipals", e.getEstimatedPrincipals())
                .param("likelyConsequences", e.getLikelyConsequences())
                .param("mitigation", e.getMitigation())
                .param("ownerUserId", e.getOwnerUserId())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** Writes the editable fields of an open breach; 0 rows means it changed or closed since it was read. */
    public int update(BreachEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("description", e.getDescription())
                .param("categories", e.getCategories().toArray(String[]::new))
                .param("modules", e.getModules().toArray(String[]::new))
                .param("estimatedPrincipals", e.getEstimatedPrincipals())
                .param("likelyConsequences", e.getLikelyConsequences())
                .param("mitigation", e.getMitigation())
                .param("ownerUserId", e.getOwnerUserId())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /**
     * Writes a step's columns (status, board report time, not-a-breach reason, closing time) as the entity now holds
     * them; 0 rows means the row changed since it was read or left the status {@code from}.
     */
    public int advance(BreachEntity e, String from, int expectedVersion) {
        return jdbc.sql(ADVANCE)
                .param("id", e.getId())
                .param("status", e.getStatus())
                .param("boardReportSentAt", e.getBoardReportSentAt())
                .param("notABreachReason", e.getNotABreachReason())
                .param("closedAt", e.getClosedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("fromStatus", from)
                .param("expected", expectedVersion)
                .update();
    }

    /** Which of the codes name a data category of the catalogue. */
    public Set<String> knownCategories(List<String> codes) {
        return Set.copyOf(jdbc.sql(KNOWN_CATEGORIES).param("codes", codes.toArray(String[]::new)).query(String.class)
                .list());
    }

    private static List<Object> sortValues(SortSpec sort, BreachEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static BreachEntity map(ResultSet rs, int row) throws SQLException {
        return BreachEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .number(rs.getString("number"))
                .status(rs.getString("status"))
                .detectedAt(rs.getObject("detected_at", OffsetDateTime.class))
                .becameAwareAt(rs.getObject("became_aware_at", OffsetDateTime.class))
                .boardReportDueAt(rs.getObject("board_report_due_at", OffsetDateTime.class))
                .description(rs.getString("description"))
                .categories(strings(rs.getArray("categories")))
                .modules(strings(rs.getArray("modules")))
                .estimatedPrincipals(rs.getObject("estimated_principals", Integer.class))
                .likelyConsequences(rs.getString("likely_consequences"))
                .mitigation(rs.getString("mitigation"))
                .ownerUserId(rs.getObject("owner_user_id", UUID.class))
                .boardReportSentAt(rs.getObject("board_report_sent_at", OffsetDateTime.class))
                .notABreachReason(rs.getString("not_a_breach_reason"))
                .closedAt(rs.getObject("closed_at", OffsetDateTime.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static List<String> strings(Array array) throws SQLException {
        return List.of((String[]) array.getArray());
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
