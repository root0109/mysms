package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.RightsRequestEntity;
import com.mysms.privacy.model.AccessSummarySnapshot;
import com.mysms.privacy.model.RightsRequestDetails;
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
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plain-JDBC access to privacy.rights_request (no soft delete: a request is evidence of how a right was honoured). The
 * details and an access request's summary are jsonb, written through {@code CAST(:x AS jsonb)} and read back as JSON
 * text; the summary only by its own statements, so lists never carry it. Each step is one guarded update: the
 * row_version read and the states the step leaves, so a concurrent change never slips past the service's checks.
 * Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RightsRequestRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("number", "r.number", ValueType.STRING),
            field("type", "r.type", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("subjectPersonId", "r.subject_person_id", ValueType.UUID),
            field("requesterPersonId", "r.requester_person_id", ValueType.UUID),
            field("requesterCapacity", "r.requester_capacity", ValueType.STRING),
            field("identityCheck", "r.identity_check", ValueType.STRING),
            field("assigneeUserId", "r.assignee_user_id", ValueType.UUID),
            field("submittedAt", "r.submitted_at", ValueType.TIMESTAMP),
            field("dueAt", "r.due_at", ValueType.TIMESTAMP),
            field("closedAt", "r.closed_at", ValueType.TIMESTAMP),
            field("outcomeCode", "r.outcome_code", ValueType.STRING));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("number", "r.number", ValueType.STRING),
            field("type", "r.type", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("submittedAt", "r.submitted_at", ValueType.TIMESTAMP),
            field("dueAt", "r.due_at", ValueType.TIMESTAMP));

    /** The staff queue: the nearest deadline first. */
    public static final String DEFAULT_ORDER = "dueAt";
    /** A requester's own requests: the newest first. */
    public static final String NEWEST_FIRST = "submittedAt,desc";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<RightsRequestEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("number", RightsRequestEntity::getNumber),
            Map.entry("type", RightsRequestEntity::getType),
            Map.entry("status", RightsRequestEntity::getStatus),
            Map.entry("submittedAt", RightsRequestEntity::getSubmittedAt),
            Map.entry("dueAt", RightsRequestEntity::getDueAt));

    private static final String SELECT = """
            SELECT r.id, r.tenant_id, r.number, r.type, r.subject_person_id, r.requester_person_id, r.requester_capacity,
                   r.status, r.identity_check, r.details::text AS details, r.assignee_user_id, r.submitted_at, r.due_at,
                   r.extended_reason, r.closed_at, r.outcome_code, r.outcome_note, r.outcome_response, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.row_version
              FROM privacy.rights_request r
             WHERE r.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " AND r.id = :id";
    private static final String LOCK_BY_ID = FIND_BY_ID + " FOR UPDATE";
    private static final String INSERT = """
            INSERT INTO privacy.rights_request
            (id, tenant_id, number, type, subject_person_id, requester_person_id, requester_capacity, status,
             identity_check, details, submitted_at, due_at, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :number, :type, :subjectPersonId, :requesterPersonId, :requesterCapacity, :status,
                    :identityCheck, CAST(:details AS jsonb), :submittedAt, :dueAt, :createdAt, :createdBy, :updatedAt,
                    :updatedBy, :rowVersion)
            """;
    private static final String VERIFY_IDENTITY = """
            UPDATE privacy.rights_request
               SET status = 'verified',
                   identity_check = :identityCheck,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'submitted'
            """;
    private static final String ASSIGN = """
            UPDATE privacy.rights_request
               SET status = 'in_progress',
                   assignee_user_id = :assigneeUserId,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status IN ('verified', 'in_progress')
            """;
    private static final String EXTEND = """
            UPDATE privacy.rights_request
               SET due_at = :dueAt,
                   extended_reason = :extendedReason,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND closed_at IS NULL AND due_at < :dueAt
            """;
    private static final String REJECT = """
            UPDATE privacy.rights_request
               SET status = 'rejected',
                   outcome_code = :outcomeCode,
                   closed_at = :closedAt,
                   updated_at = :closedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status IN ('submitted', 'verified')
            """;
    private static final String CLOSE = """
            UPDATE privacy.rights_request
               SET status = 'closed',
                   outcome_code = :outcomeCode,
                   outcome_note = :outcomeNote,
                   outcome_response = :outcomeResponse,
                   closed_at = :closedAt,
                   updated_at = :closedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'in_progress'
               AND NOT (type IN ('access', 'erasure', 'export', 'withdraw_all')
                        AND :outcomeCode IN ('fulfilled', 'partially_fulfilled'))
            """;
    /**
     * A step of an erasure or export request's fulfilment: started (in progress), waiting for action, back in
     * progress, or closed with its outcome once every task answered.
     */
    private static final String FULFILMENT = """
            UPDATE privacy.rights_request
               SET status = :status,
                   outcome_code = :outcomeCode,
                   closed_at = :closedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND type IN ('erasure', 'export')
               AND status IN ('in_progress', 'action_required')
            """;
    private static final String FULFIL_WITHDRAW_ALL = """
            UPDATE privacy.rights_request
               SET status = 'closed',
                   outcome_code = 'fulfilled',
                   closed_at = :closedAt,
                   updated_at = :closedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND type = 'withdraw_all' AND status = 'in_progress'
            """;

    private static final String FULFIL_ACCESS = """
            UPDATE privacy.rights_request
               SET status = 'closed',
                   outcome_code = 'fulfilled',
                   access_summary = CAST(:accessSummary AS jsonb),
                   closed_at = :closedAt,
                   updated_at = :closedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND type = 'access' AND status = 'in_progress'
            """;
    private static final String FIND_ACCESS_SUMMARY = """
            SELECT r.access_summary::text
              FROM privacy.rights_request r
             WHERE r.tenant_id = :tenantId
               AND r.id = :id AND r.access_summary IS NOT NULL
            """;

    /** The caller's requests about someone they still decide for, or act for as nominee (see {@link #requestedBy}). */
    private static final String STILL_DECIDES = """
            r.requester_person_id = :requesterPersonId
            AND ((r.subject_person_id = :requesterPersonId AND EXISTS (
                 SELECT 1
                   FROM privacy.subject a
                  WHERE a.tenant_id = :tenantId AND a.person_id = r.subject_person_id AND a.status = 'adult'))
                 OR EXISTS (
                 SELECT 1
                   FROM privacy.nominee n
                  WHERE n.tenant_id = :tenantId
                    AND n.principal_person_id = r.subject_person_id AND n.nominee_person_id = :requesterPersonId
                    AND n.status = 'active' AND r.requester_capacity = 'nominee') OR EXISTS (
                 SELECT 1
                   FROM privacy.guardian_link g
                   LEFT JOIN privacy.subject s ON s.tenant_id = :tenantId AND s.person_id = g.student_person_id
                  WHERE g.tenant_id = :tenantId
                    AND g.guardian_person_id = :requesterPersonId AND g.student_person_id = r.subject_person_id
                    AND g.status = 'active' AND g.is_legal_guardian
                    AND (s.status IS NULL OR s.status <> 'adult')))
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;
    /**
     * Reads a kept access summary: properties it does not know are skipped, whatever the process's mapper decides, so
     * a summary kept by a later version (a higher {@link AccessSummarySnapshot#schemaVersion()}) stays readable.
     */
    private final ObjectReader summaryReader;

    public RightsRequestRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper json) {
        this.jdbc = database.jdbc();
        this.json = json;
        this.summaryReader = json.readerFor(AccessSummarySnapshot.class)
                .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public Optional<RightsRequestEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(this::map).optional();
    }

    /** The request, its row locked until the transaction ends (a module's answer to one of its tasks). */
    public Optional<RightsRequestEntity> lockById(UUID id) {
        return jdbc.sql(LOCK_BY_ID).param("id", id).query(this::map).optional();
    }

    public Page<RightsRequestEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<RightsRequestEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(this::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), RightsRequestEntity::getId);
    }

    public void insert(RightsRequestEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("number", e.getNumber())
                .param("type", e.getType())
                .param("subjectPersonId", e.getSubjectPersonId())
                .param("requesterPersonId", e.getRequesterPersonId())
                .param("requesterCapacity", e.getRequesterCapacity())
                .param("status", e.getStatus())
                .param("identityCheck", e.getIdentityCheck())
                .param("details", json.writeValueAsString(e.getDetails()))
                .param("submittedAt", e.getSubmittedAt())
                .param("dueAt", e.getDueAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /** submitted → verified by staff in person or by document; 0 rows means the row changed since it was read. */
    public int verifyIdentity(UUID id, int expectedVersion, String identityCheck, OffsetDateTime at, UUID actor) {
        return jdbc.sql(VERIFY_IDENTITY).param("id", id).param("identityCheck", identityCheck).param("updatedAt", at)
                .param("updatedBy", actor).param("expected", expectedVersion).update();
    }

    /** verified or in_progress → in_progress with this assignee; 0 rows means the row changed since it was read. */
    public int assign(UUID id, int expectedVersion, UUID assigneeUserId, OffsetDateTime at, UUID actor) {
        return jdbc.sql(ASSIGN).param("id", id).param("assigneeUserId", assigneeUserId).param("updatedAt", at)
                .param("updatedBy", actor).param("expected", expectedVersion).update();
    }

    /**
     * Moves an open request's deadline later, keeping the reason; 0 rows means the row changed since it was read.
     */
    public int extend(UUID id, int expectedVersion, OffsetDateTime dueAt, String reason, OffsetDateTime at,
            UUID actor) {
        return jdbc.sql(EXTEND).param("id", id).param("dueAt", dueAt).param("extendedReason", reason)
                .param("updatedAt", at).param("updatedBy", actor).param("expected", expectedVersion).update();
    }

    /** submitted or verified → rejected with its reason code; 0 rows means the row changed since it was read. */
    public int reject(UUID id, int expectedVersion, String reasonCode, OffsetDateTime at, UUID actor) {
        return jdbc.sql(REJECT).param("id", id).param("outcomeCode", reasonCode).param("closedAt", at)
                .param("updatedBy", actor).param("expected", expectedVersion).update();
    }

    /**
     * in_progress → closed with its outcome, the staff's note and the answer the requester sees, never an access,
     * erasure, export or withdraw-all request as (partially) fulfilled — those are fulfilled by their own steps; 0 rows
     * means the row changed since it was read.
     */
    public int close(UUID id, int expectedVersion, String outcomeCode, String note, String response,
            OffsetDateTime at, UUID actor) {
        return jdbc.sql(CLOSE).param("id", id).param("outcomeCode", outcomeCode).param("outcomeNote", note)
                .param("outcomeResponse", response).param("closedAt", at).param("updatedBy", actor)
                .param("expected", expectedVersion).update();
    }

    /**
     * An access request in progress → closed as fulfilled, keeping the summary sent; 0 rows means the row changed since
     * it was read.
     */
    public int fulfilAccess(UUID id, int expectedVersion, AccessSummarySnapshot summary, OffsetDateTime at,
            UUID actor) {
        return jdbc.sql(FULFIL_ACCESS).param("id", id).param("accessSummary", json.writeValueAsString(summary))
                .param("closedAt", at).param("updatedBy", actor).param("expected", expectedVersion).update();
    }

    /**
     * An erasure or export request in progress or waiting for action → the status given (in_progress,
     * action_required, or closed with its outcome and closing time); 0 rows means the row changed since it was read.
     */
    public int fulfilment(RightsRequestEntity e, int expectedVersion) {
        return jdbc.sql(FULFILMENT).param("id", e.getId()).param("status", e.getStatus())
                .param("outcomeCode", e.getOutcomeCode()).param("closedAt", e.getClosedAt())
                .param("updatedAt", e.getUpdatedAt()).param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion).update();
    }

    /** A withdraw-all request in progress → closed as fulfilled; 0 rows means the row changed since it was read. */
    public int fulfilWithdrawAll(UUID id, int expectedVersion, OffsetDateTime at, UUID actor) {
        return jdbc.sql(FULFIL_WITHDRAW_ALL).param("id", id).param("closedAt", at).param("updatedBy", actor)
                .param("expected", expectedVersion).update();
    }

    /** The access summary kept on the request; empty until the access request is fulfilled. */
    public Optional<AccessSummarySnapshot> findAccessSummary(UUID id) {
        return jdbc.sql(FIND_ACCESS_SUMMARY).param("id", id).query(String.class).optional()
                .map(text -> summaryReader.<AccessSummarySnapshot>readValue(text));
    }

    /**
     * The requests the person submitted about someone they still decide for: themself, a minor they are still the active
     * legal guardian of (a person of unknown age counts as a minor), or a principal they are still the active nominee of
     * (requests submitted as nominee). A guardian whose link ended, or whose ward came of age, no longer sees the ward's
     * requests; a nominee whose nomination was revoked no longer sees the principal's.
     */
    public static SqlFragment requestedBy(UUID requesterPersonId) {
        return new SqlFragment(STILL_DECIDES, Map.of("requesterPersonId", requesterPersonId));
    }

    private static List<Object> sortValues(SortSpec sort, RightsRequestEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private RightsRequestEntity map(ResultSet rs, int row) throws SQLException {
        return RightsRequestEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .number(rs.getString("number"))
                .type(rs.getString("type"))
                .subjectPersonId(rs.getObject("subject_person_id", UUID.class))
                .requesterPersonId(rs.getObject("requester_person_id", UUID.class))
                .requesterCapacity(rs.getString("requester_capacity"))
                .status(rs.getString("status"))
                .identityCheck(rs.getString("identity_check"))
                .details(json.readValue(rs.getString("details"), RightsRequestDetails.class))
                .assigneeUserId(rs.getObject("assignee_user_id", UUID.class))
                .submittedAt(rs.getObject("submitted_at", OffsetDateTime.class))
                .dueAt(rs.getObject("due_at", OffsetDateTime.class))
                .extendedReason(rs.getString("extended_reason"))
                .closedAt(rs.getObject("closed_at", OffsetDateTime.class))
                .outcomeCode(rs.getString("outcome_code"))
                .outcomeNote(rs.getString("outcome_note"))
                .outcomeResponse(rs.getString("outcome_response"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
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
