package com.mysms.platform.business;

import com.mysms.authclient.AuthContext;
import com.mysms.platform.api.model.Operation;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.imports.ImportBatchResult;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.ImportJobEntity;
import com.mysms.platform.domain.OperationEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.OperationMapper;
import com.mysms.platform.repository.ImportJobRepository;
import com.mysms.platform.repository.OperationRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * platform's long-running operations (GetOperation, ListOperations, CancelOperation): a caller sees and cancels the
 * operations they started; the permission held tenant-wide covers every operation of the tenant. Anything else is
 * 404, never a hint that it exists.
 */
@Service
public class OperationService extends AppTransactionManager {

    static final String AGGREGATE = "platform.operation";
    static final String PERMISSION_READ = "platform.operation.read";
    static final String PERMISSION_CANCEL = "platform.operation.cancel_operation";
    /** The operation of an import (platform.StartImport). */
    public static final String IMPORT = "platform.StartImport";
    /** The most row errors an import operation lists; the result's errorCount counts them all. */
    static final int MAX_LISTED_ERRORS = 1000;

    private final OperationRepository repository;
    private final ImportJobRepository imports;
    private final OperationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public OperationService(OperationRepository repository, ImportJobRepository imports, OperationMapper mapper,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.imports = imports;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    public Operation getOperation(UUID id) {
        AuthContext caller = AuthContext.require();
        return doInReadTransaction(() -> {
            return toDto(visible(caller, id, PERMISSION_READ));
        });
    }

    public Page<Operation> listOperations(String kind, Integer pageNumber, Integer pageSize, String cursor) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(null, OperationRepository.SORT_FIELDS, OperationRepository.DEFAULT_ORDER,
                OperationRepository.ID_COLUMN);
        SqlFragment where = OperationRepository.ofKind(kind).and(OperationRepository.visibleTo(caller.userId(),
                caller.hasTenantWidePermission(PERMISSION_READ)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, kind == null ? "" : kind, clock);
        return doInReadTransaction(() -> {
            Page<OperationEntity> rows = repository.list(page, where);
            Map<UUID, ImportJobEntity> jobs = imports.findByOperations(rows.items().stream()
                    .filter(OperationService::isImport).map(OperationEntity::getId).toList()).stream()
                    .collect(Collectors.toMap(ImportJobEntity::getOperationId, Function.identity()));
            return rows.map(row -> withResult(mapper.toDto(row), jobs.get(row.getId()), List.of()));
        });
    }

    /** Cancels a running operation (409 once it has finished); audited, with a domain event. */
    public Operation cancelOperation(UUID id) {
        AuthContext caller = AuthContext.require();
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        return doInWriteTransaction(() -> {
            OperationEntity current = visible(caller, id, PERMISSION_CANCEL);
            if (!"running".equals(current.getStatus())) {
                throw new InvalidStateException("The operation has already finished.");
            }
            if (isImport(current)) {
                throw new InvalidStateException("An import's rows are already with the module that creates them; "
                        + "it ends when that module has answered every batch.");
            }
            if (repository.cancel(id, current.getRowVersion(), now, caller.userId()) == 0) {
                throw new PreconditionFailedException("The operation changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status", "doneTime"), null);
            outbox.publish(AGGREGATE + ".cancelled", AGGREGATE, id, PlatformEvents.payload(id));
            return toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * Records a job that ran to its end in the request that started it (platform.RunReport, platform.ClearDemoData
     * answer 202 with it): a succeeded operation of the caller in the bound tenant, audited and announced, so the job
     * centre lists it and GetOperation answers it like any other. Joins the caller's transaction when there is one.
     *
     * @param operationName    the contract operation that ran (platform.RunReport)
     * @param resourceType     what the job worked on (a table name), with {@code resourceId} when it is one row
     * @param resultEntityType what it produced ({@code file}: the output file), with {@code resultEntityId}
     */
    public Operation recordFinished(String operationName, String resourceType, UUID resourceId,
            String resultEntityType, UUID resultEntityId) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        UUID actor = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            OperationEntity stored = repository.insertFinished(UuidV7.next(clock), operationName, resourceType,
                    resourceId, resultEntityType, resultEntityId, now, actor);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + ".succeeded", AGGREGATE, stored.getId(), PlatformEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Records a job of the caller that runs on after the request that started it (an import whose batches are with
     * the module that creates their rows), audited and announced like a finished one. Joins the caller's transaction.
     */
    public UUID recordRunning(String operationName, String resourceType, UUID resourceId) {
        UUID id = UuidV7.next(clock);
        UUID actor = AuthContext.require().userId();
        runInWriteTransaction(() -> {
            repository.insertRunning(id, operationName, resourceType, resourceId, now(), actor);
            audit.recordCreate(AGGREGATE, id);
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, id, PlatformEvents.payload(id));
        });
        return id;
    }

    /**
     * Moves a running job of the bound tenant on (by the caller, or by the platform's own work on an event); at 100 % it has
     * succeeded, which is audited and announced. False when it is no longer running. Joins the caller's transaction.
     */
    public boolean advance(UUID id, int percent) {
        return doInWriteTransaction(() -> {
            if (repository.progress(id, percent, now(), Actor.current().orElse(null)) == 0) {
                return false;
            }
            if (percent == 100) {
                audit.recordTransition(AGGREGATE, id, "succeed", List.of("status", "progressPercent", "doneTime"),
                        null);
                outbox.publish(AGGREGATE + ".succeeded", AGGREGATE, id, PlatformEvents.payload(id));
            }
            return true;
        });
    }

    /** The operation as the contract shows it; an import's carries its result (totals and the first row errors). */
    private Operation toDto(OperationEntity row) {
        if (!isImport(row)) {
            return mapper.toDto(row);
        }
        Optional<ImportJobEntity> job = imports.findByOperation(row.getId());
        return withResult(mapper.toDto(row), job.orElse(null),
                job.map(j -> imports.errors(j.getId(), MAX_LISTED_ERRORS)).orElse(List.of()));
    }

    /**
     * The result of an import operation: what was imported where, the totals, and the row errors given (ImportRowError:
     * rowNumber, field, message; at most {@value MAX_LISTED_ERRORS}, by row — {@code errorCount} tells whether there
     * are more).
     */
    private static Operation withResult(Operation dto, ImportJobEntity job, List<ImportBatchResult.RowError> errors) {
        if (job == null) {
            return dto;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entityType", job.getEntityType());
        if (job.getInstitutionId() != null) {
            result.put("institutionId", job.getInstitutionId().toString());
        }
        result.put("fileId", job.getFileId().toString());
        result.put("dryRun", job.isDryRun());
        result.put("rowCount", job.getRowCount());
        result.put("createdCount", job.getCreatedCount());
        result.put("errorCount", job.getErrorCount());
        List<Map<String, Object>> listed = new ArrayList<>();
        for (ImportBatchResult.RowError error : errors) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rowNumber", error.rowNumber());
            if (error.field() != null) {
                item.put("field", error.field());
            }
            item.put("message", error.message());
            listed.add(item);
        }
        result.put("errors", listed);
        return dto.result(result);
    }

    private static boolean isImport(OperationEntity row) {
        return IMPORT.equals(row.getOperationName());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private OperationEntity visible(AuthContext caller, UUID id, String permission) {
        OperationEntity row = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!caller.hasTenantWidePermission(permission) && !caller.userId().equals(row.getStartedByUserId())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return row;
    }
}
