package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.domain.OperationEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to lms.operation: the tenant filtered in every statement, live rows only, newest first with keyset
 * paging, the row_version guard on cancel.
 */
@Repository
public class OperationRepository {

    /** Newest first; NOT NULL columns, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = Map.of(
            "createTime", new FieldSpec("createTime", "r.started_at", ValueType.TIMESTAMP));
    public static final String DEFAULT_ORDER = "createTime desc";
    public static final String ID_COLUMN = "r.id";

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.operation_name, r.resource_type, r.resource_id, r.status, r.progress_percent,
                   r.error_code, r.error_message, r.result_entity_type, r.result_entity_id, r.started_by_user_id,
                   r.started_at, r.finished_at, r.result, r.created_at, r.created_by, r.updated_at,
                   r.updated_by, r.deleted_at, r.row_version
              FROM lms.operation r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String CANCEL = """
            UPDATE lms.operation
               SET status = 'cancelled', finished_at = :now, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND status = 'running' AND deleted_at IS NULL
            """;

    private static final String INSERT_COMPLETED = """
            INSERT INTO lms.operation
                (id, tenant_id, operation_name, resource_type, resource_id, status, progress_percent,
                 result_entity_type, result_entity_id, started_by_user_id, started_at, finished_at,
                 created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :operationName, :resourceType, :resourceId, 'succeeded', 100,
                    :resultType, :resultId, :startedBy, :startedAt, :finishedAt,
                    :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, operation_name, resource_type, resource_id, status, progress_percent,
                      error_code, error_message, result_entity_type, result_entity_id, started_by_user_id,
                      started_at, finished_at, result, created_at, created_by, updated_at, updated_by, deleted_at,
                      row_version
            """;

    private static final String INSERT = """
            INSERT INTO lms.operation
            (id, tenant_id, operation_name, resource_type, resource_id, status, progress_percent, started_by_user_id,
             started_at, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :operationName, :resourceType, :resourceId, 'running', 0, :actor, :now, :now,
                    :actor, :now, :actor, 1)
            """;
    private static final String PROGRESS = """
            UPDATE lms.operation
               SET progress_percent = :progressPercent, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status = 'running' AND deleted_at IS NULL
            """;
    private static final String SUCCEED = """
            UPDATE lms.operation
               SET status = 'succeeded', progress_percent = 100, result_entity_type = :resultType,
                   result_entity_id = :resultId, result = :result, finished_at = :now,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status = 'running' AND deleted_at IS NULL
            """;
    private static final String FAIL = """
            UPDATE lms.operation
               SET status = 'failed', error_code = :errorCode, error_message = :errorMessage, finished_at = :now,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND status = 'running' AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public OperationRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Stores the completed record of a synchronous job accepted through an asynchronous operation contract. */
    public OperationEntity insertCompleted(OperationEntity entity) {
        return jdbc.sql(INSERT_COMPLETED).param("id", entity.getId()).param("tenantId", entity.getTenantId())
                .param("operationName", entity.getOperationName()).param("resourceType", entity.getResourceType())
                .param("resourceId", entity.getResourceId()).param("resultType", entity.getResultEntityType())
                .param("resultId", entity.getResultEntityId()).param("startedBy", entity.getStartedByUserId())
                .param("startedAt", entity.getStartedAt()).param("finishedAt", entity.getFinishedAt())
                .param("createdAt", entity.getCreatedAt()).param("createdBy", entity.getCreatedBy())
                .param("updatedAt", entity.getUpdatedAt()).param("updatedBy", entity.getUpdatedBy())
                .param("rowVersion", entity.getRowVersion()).query(OperationRepository::map).single();
    }

    public Optional<OperationEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(OperationRepository::map).optional();
    }

    public Page<OperationEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<OperationEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(OperationRepository::map).list();
        return page.toPage(rows, e -> List.of(e.getStartedAt()), OperationEntity::getId);
    }

    /** Marks a running operation cancelled (the schema has no queued state); 0 when it changed or finished. */
    public int cancel(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(CANCEL).param("id", id).param("expected", expected).param("now", now).param("actor", actor)
                .update();
    }

    /** Records an operation that starts now, running at 0 %. */
    public void insert(UUID id, String operationName, String resourceType, UUID resourceId, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(INSERT)
                .param("id", id)
                .param("operationName", operationName)
                .param("resourceType", resourceType)
                .param("resourceId", resourceId)
                .param("now", now)
                .param("actor", actor)
                .update();
    }

    /** Reports how far a running operation is; 0 when it is no longer running (cancelled meanwhile). */
    public int progress(UUID id, int progressPercent, OffsetDateTime now, UUID actor) {
        return jdbc.sql(PROGRESS).param("id", id).param("progressPercent", progressPercent).param("now", now)
                .param("actor", actor).update();
    }

    /**
     * Ends a running operation as succeeded, with the record it produced and/or its outcome (JSON); 0 when it is no
     * longer running (cancelled).
     */
    public int succeed(UUID id, String resultType, UUID resultId, String result, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SUCCEED).param("id", id).param("resultType", resultType).param("resultId", resultId)
                .param("result", result).param("now", now).param("actor", actor).update();
    }

    /** Ends a running operation as failed; 0 when it is no longer running (cancelled). */
    public int fail(UUID id, String errorCode, String errorMessage, OffsetDateTime now, UUID actor) {
        return jdbc.sql(FAIL).param("id", id).param("errorCode", errorCode).param("errorMessage", errorMessage)
                .param("now", now).param("actor", actor).update();
    }

    /** Only the caller's own operations, unless every operation of the tenant may be seen. */
    public static SqlFragment visibleTo(UUID user, boolean everyone) {
        if (everyone) {
            return SqlFragment.TRUE;
        }
        return new SqlFragment("r.started_by_user_id = :startedBy", Map.of("startedBy", user));
    }

    /** Operations of one kind (exact match). */
    public static SqlFragment ofKind(String kind) {
        return kind == null || kind.isBlank() ? SqlFragment.TRUE
                : new SqlFragment("r.operation_name = :kind", Map.of("kind", kind));
    }

    private static OperationEntity map(ResultSet rs, int row) throws SQLException {
        return OperationEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .operationName(rs.getString("operation_name"))
                .resourceType(rs.getString("resource_type"))
                .resourceId(rs.getObject("resource_id", UUID.class))
                .status(rs.getString("status"))
                .progressPercent(rs.getObject("progress_percent") == null ? null : rs.getInt("progress_percent"))
                .errorCode(rs.getString("error_code"))
                .errorMessage(rs.getString("error_message"))
                .resultEntityType(rs.getString("result_entity_type"))
                .resultEntityId(rs.getObject("result_entity_id", UUID.class))
                .startedByUserId(rs.getObject("started_by_user_id", UUID.class))
                .startedAt(rs.getObject("started_at", OffsetDateTime.class))
                .finishedAt(rs.getObject("finished_at", OffsetDateTime.class))
                .result(rs.getString("result"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }
}
