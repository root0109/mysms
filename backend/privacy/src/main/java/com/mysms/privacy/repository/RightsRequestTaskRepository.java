package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.domain.RightsRequestTaskEntity;
import com.mysms.privacy.model.TaskResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plain-JDBC access to privacy.rights_request_task (no soft delete: the results are evidence). The module's answer is
 * jsonb, written through {@code CAST(:result AS jsonb)} and read back as JSON text. Dispatching and recording an
 * answer are each one update guarded by the row_version read and the statuses the change leaves. Every statement
 * filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class RightsRequestTaskRepository {

    private static final String SELECT = """
            SELECT t.id, t.tenant_id, t.request_id, t.module, t.action, t.wave, t.status, t.attempts, t.dispatched_at,
                   t.completed_at, t.result::text AS result, t.last_error_code, t.next_attempt_at, t.created_at,
                   t.created_by, t.updated_at, t.updated_by, t.row_version
              FROM privacy.rights_request_task t
             WHERE t.tenant_id = :tenantId
            """;
    private static final String FIND_BY_ID = SELECT + " AND t.id = :id";
    private static final String OF_REQUESTS = SELECT + """
               AND t.request_id = ANY(CAST(:requestIds AS uuid[]))
             ORDER BY t.request_id, t.wave, t.module, t.action
            """;
    /**
     * The tasks due for another dispatch: dispatched and unanswered since before the cutoff, or failed and waiting
     * for a next attempt that has come; the longest due first.
     */
    private static final String DUE = """
            SELECT t.id
              FROM privacy.rights_request_task t
             WHERE t.tenant_id = :tenantId
               AND ((t.status = 'dispatched' AND t.dispatched_at < :cutoff)
                    OR (t.status = 'failed' AND t.next_attempt_at <= :now))
             ORDER BY CASE WHEN t.status = 'dispatched' THEN t.dispatched_at ELSE t.next_attempt_at END, t.id
             LIMIT :limit
            """;
    private static final String INSERT = """
            INSERT INTO privacy.rights_request_task
            (id, tenant_id, request_id, module, action, wave, status, attempts, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :requestId, :module, :action, :wave, 'pending', 0, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            """;
    private static final String DISPATCH = """
            UPDATE privacy.rights_request_task
               SET status = 'dispatched',
                   attempts = attempts + 1,
                   dispatched_at = :dispatchedAt,
                   completed_at = NULL,
                   result = NULL,
                   next_attempt_at = NULL,
                   updated_at = :dispatchedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status IN ('pending', 'dispatched', 'failed')
            """;
    private static final String ANSWER = """
            UPDATE privacy.rights_request_task
               SET status = :status,
                   completed_at = :completedAt,
                   result = CAST(:result AS jsonb),
                   last_error_code = :lastErrorCode,
                   next_attempt_at = :nextAttemptAt,
                   updated_at = :completedAt,
                   updated_by = NULL,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'dispatched'
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public RightsRequestTaskRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            JsonMapper json) {
        this.jdbc = database.jdbc();
        this.json = json;
    }

    public Optional<RightsRequestTaskEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(this::map).optional();
    }

    /** The tasks of the requests, request by request, wave by wave. */
    public List<RightsRequestTaskEntity> ofRequests(List<UUID> requestIds) {
        return jdbc.sql(OF_REQUESTS).param("requestIds", requestIds.toArray(UUID[]::new)).query(this::map).list();
    }

    /**
     * Up to {@code limit} tasks due for another dispatch: unanswered since before the cutoff, or failed with their
     * next attempt at or before {@code now}; the longest due first.
     */
    public List<UUID> due(OffsetDateTime cutoff, OffsetDateTime now, int limit) {
        return jdbc.sql(DUE).param("cutoff", cutoff).param("now", now).param("limit", limit).query(UUID.class)
                .list();
    }

    public void insert(RightsRequestTaskEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("requestId", e.getRequestId())
                .param("module", e.getModule())
                .param("action", e.getAction())
                .param("wave", e.getWave())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    /**
     * pending, dispatched or failed → dispatched again: one more attempt, its answer cleared; 0 rows means the task
     * changed since it was read.
     */
    public int dispatch(UUID id, int expectedVersion, OffsetDateTime at, UUID actor) {
        return jdbc.sql(DISPATCH).param("id", id).param("dispatchedAt", at).param("updatedBy", actor)
                .param("expected", expectedVersion).update();
    }

    /**
     * dispatched → the module's answer (a failed one with the time of its next attempt, if any); 0 rows means the
     * task changed since it was read.
     */
    public int answer(RightsRequestTaskEntity e, int expectedVersion) {
        return jdbc.sql(ANSWER)
                .param("id", e.getId())
                .param("status", e.getStatus())
                .param("completedAt", e.getCompletedAt())
                .param("result", e.getResult() == null ? null : json.writeValueAsString(e.getResult()))
                .param("lastErrorCode", e.getLastErrorCode())
                .param("nextAttemptAt", e.getNextAttemptAt())
                .param("expected", expectedVersion)
                .update();
    }

    private RightsRequestTaskEntity map(ResultSet rs, int row) throws SQLException {
        String result = rs.getString("result");
        return RightsRequestTaskEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .requestId(rs.getObject("request_id", UUID.class))
                .module(rs.getString("module"))
                .action(rs.getString("action"))
                .wave(rs.getInt("wave"))
                .status(rs.getString("status"))
                .attempts(rs.getInt("attempts"))
                .dispatchedAt(rs.getObject("dispatched_at", OffsetDateTime.class))
                .completedAt(rs.getObject("completed_at", OffsetDateTime.class))
                .result(result == null ? null : json.readValue(result, TaskResult.class))
                .lastErrorCode(rs.getString("last_error_code"))
                .nextAttemptAt(rs.getObject("next_attempt_at", OffsetDateTime.class))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }
}
