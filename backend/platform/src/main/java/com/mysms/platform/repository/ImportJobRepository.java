package com.mysms.platform.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.imports.ImportBatchResult.RowError;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.ImportJobEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to an import and its children: platform.import_job, the batches its owner answered
 * (platform.import_batch, one row per batch so a repeated answer changes nothing) and its row errors
 * (platform.import_row_error, written in one statement per batch). The tenant is filtered in every statement.
 */
@Repository
public class ImportJobRepository {

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.operation_id, r.entity_type, r.institution_id, r.file_id, r.dry_run,
                   r.row_count, r.batch_count, r.applied_batch_count, r.created_count, r.error_count, r.created_at,
                   r.created_by, r.updated_at, r.updated_by, r.deleted_at, r.row_version
              FROM platform.import_job r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_OPERATION = SELECT_LIVE + " AND r.operation_id = :operationId";
    /** The import a batch answer updates, locked so concurrent answers add up one after the other. */
    private static final String LOCK_BY_OPERATION = FIND_BY_OPERATION + " FOR UPDATE";
    private static final String FIND_BY_OPERATIONS = SELECT_LIVE + " AND r.operation_id = ANY(:operationIds)";
    private static final String INSERT = """
            INSERT INTO platform.import_job
            (id, tenant_id, operation_id, entity_type, institution_id, file_id, dry_run, row_count, batch_count,
             applied_batch_count, created_count, error_count, created_at, created_by, updated_at, updated_by,
             row_version)
            VALUES (:id, :tenantId, :operationId, :entityType, :institutionId, :fileId, :dryRun, :rowCount,
                    :batchCount, 0, 0, :errorCount, :createdAt, :createdBy, :updatedAt, :updatedBy, :rowVersion)
            """;
    /** Adds one answered batch to the totals (row_version guard: 0 rows → it changed meanwhile). */
    private static final String APPLY_BATCH = """
            UPDATE platform.import_job
               SET applied_batch_count = applied_batch_count + 1, created_count = created_count + :created,
                   error_count = error_count + :errors, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
               AND applied_batch_count < batch_count
            """;
    private static final String INSERT_BATCH = """
            INSERT INTO platform.import_batch
            (id, tenant_id, import_job_id, batch_number, module_code, created_count, error_count, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :jobId, :batchNumber, :module, :created, :errors, :now, :actor, :now, :actor, 1)
            ON CONFLICT (import_job_id, batch_number) DO NOTHING
            """;
    private static final String INSERT_ERRORS = """
            INSERT INTO platform.import_row_error
            (id, tenant_id, import_job_id, batch_number, row_number, field, message, created_at, created_by,
             updated_at, updated_by, row_version)
            SELECT u.id, :tenantId, :jobId, :batchNumber, u.row_number, u.field, u.message, :now, :actor, :now,
                   :actor, 1
              FROM unnest(CAST(:ids AS uuid[]), CAST(:rows AS integer[]), CAST(:fields AS varchar[]),
                          CAST(:messages AS varchar[])) AS u(id, row_number, field, message)
            """;
    private static final String ERRORS = """
            SELECT e.row_number, e.field, e.message
              FROM platform.import_row_error e
             WHERE e.tenant_id = :tenantId
               AND e.deleted_at IS NULL
               AND e.import_job_id = :jobId
             ORDER BY e.row_number, e.batch_number NULLS FIRST, e.field NULLS FIRST, e.id
             LIMIT :limit
            """;

    private final JdbcClient jdbc;

    public ImportJobRepository(@Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public void insert(ImportJobEntity e) {
        jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("operationId", e.getOperationId())
                .param("entityType", e.getEntityType())
                .param("institutionId", e.getInstitutionId())
                .param("fileId", e.getFileId())
                .param("dryRun", e.isDryRun())
                .param("rowCount", e.getRowCount())
                .param("batchCount", e.getBatchCount())
                .param("errorCount", e.getErrorCount())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .update();
    }

    public Optional<ImportJobEntity> findByOperation(UUID operationId) {
        return jdbc.sql(FIND_BY_OPERATION).param("operationId", operationId).query(ImportJobRepository::map)
                .optional();
    }

    /** The import of the operation, locked for the rest of the transaction. */
    public Optional<ImportJobEntity> lockByOperation(UUID operationId) {
        return jdbc.sql(LOCK_BY_OPERATION).param("operationId", operationId).query(ImportJobRepository::map)
                .optional();
    }

    public List<ImportJobEntity> findByOperations(List<UUID> operationIds) {
        if (operationIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(FIND_BY_OPERATIONS).param("operationIds", operationIds.toArray(UUID[]::new))
                .query(ImportJobRepository::map).list();
    }

    /** Records an answered batch; false when it was answered before (nothing is written then). */
    public boolean insertBatch(UUID id, UUID jobId, int batchNumber, String module, int created, int errors,
            OffsetDateTime now, UUID actor) {
        return jdbc.sql(INSERT_BATCH).param("id", id).param("jobId", jobId).param("batchNumber", batchNumber)
                .param("module", module).param("created", created).param("errors", errors).param("now", now)
                .param("actor", actor).update() == 1;
    }

    /** Adds an answered batch to the totals; 0 when the import changed meanwhile or has no batch left. */
    public int applyBatch(UUID id, int expected, int created, int errors, OffsetDateTime now, UUID actor) {
        return jdbc.sql(APPLY_BATCH).param("id", id).param("expected", expected).param("created", created)
                .param("errors", errors).param("now", now).param("actor", actor).update();
    }

    /**
     * Stores row errors in one statement.
     *
     * @param batchNumber the batch whose module refused the rows; null for the platform's template check
     */
    public void insertErrors(UUID jobId, Integer batchNumber, List<UUID> ids, List<RowError> errors,
            OffsetDateTime now, UUID actor) {
        if (errors.isEmpty()) {
            return;
        }
        jdbc.sql(INSERT_ERRORS)
                .param("jobId", jobId)
                .param("batchNumber", batchNumber)
                .param("ids", ids.toArray(UUID[]::new))
                .param("rows", errors.stream().map(RowError::rowNumber).toArray(Integer[]::new))
                .param("fields", errors.stream().map(RowError::field).toArray(String[]::new))
                .param("messages", errors.stream().map(RowError::message).toArray(String[]::new))
                .param("now", now)
                .param("actor", actor)
                .update();
    }

    /** The first {@code limit} row errors of an import, by row. */
    public List<RowError> errors(UUID jobId, int limit) {
        return jdbc.sql(ERRORS).param("jobId", jobId).param("limit", limit)
                .query((rs, row) -> new RowError(rs.getInt("row_number"), rs.getString("field"),
                        rs.getString("message"))).list();
    }

    private static ImportJobEntity map(ResultSet rs, int row) throws SQLException {
        return ImportJobEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .operationId(rs.getObject("operation_id", UUID.class))
                .entityType(rs.getString("entity_type"))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .fileId(rs.getObject("file_id", UUID.class))
                .dryRun(rs.getBoolean("dry_run"))
                .rowCount(rs.getInt("row_count"))
                .batchCount(rs.getInt("batch_count"))
                .appliedBatchCount(rs.getInt("applied_batch_count"))
                .createdCount(rs.getInt("created_count"))
                .errorCount(rs.getInt("error_count"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }
}
