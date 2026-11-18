package com.mysms.payroll.business;

import com.mysms.authclient.AuthContext;
import com.mysms.payroll.api.model.Operation;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.id.UuidV7;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import com.mysms.payroll.config.PayrollEventsConfig;
import com.mysms.payroll.domain.OperationEntity;
import com.mysms.payroll.events.PayrollEvents;
import com.mysms.payroll.mapper.OperationMapper;
import com.mysms.payroll.repository.OperationRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * payroll's long-running operations (GetOperation, ListOperations, CancelOperation): a caller sees and cancels the
 * operations they started; the permission held tenant-wide covers every operation of the tenant. Anything else is
 * 404, never a hint that it exists.
 */
@Service
public class OperationService extends AppTransactionManager {

    static final String AGGREGATE = "payroll.operation";
    static final String PERMISSION_READ = "payroll.operation.read";
    static final String PERMISSION_CANCEL = "payroll.operation.cancel_operation";
    /** The audited step that ends a run (succeeded or failed). */
    static final String FINISH = "finish";

    private final OperationRepository repository;
    private final OperationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;
    private final JsonMapper json;

    public OperationService(OperationRepository repository, OperationMapper mapper,
            @Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PayrollDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PayrollEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PayrollEventsConfig.AUDIT) AuditWriter audit,
            Clock clock, JsonMapper json) {

        super(database, transactions);
        this.json = json;
        this.repository = repository;
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
            return rows.map(this::toDto);
        });
    }

    /** Cancels a running operation (409 once it has finished); audited, with a domain event. */
    public Operation cancelOperation(UUID id) {
        AuthContext caller = AuthContext.require();
        OffsetDateTime now = now();
        return doInWriteTransaction(() -> {
            OperationEntity current = visible(caller, id, PERMISSION_CANCEL);
            if (!"running".equals(current.getStatus())) {
                throw new InvalidStateException("The operation has already finished.");
            }
            if (repository.cancel(id, current.getRowVersion(), now, caller.userId()) == 0) {
                throw new PreconditionFailedException("The operation changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status", "doneTime"), null);
            outbox.publish(AGGREGATE + ".cancelled", AGGREGATE, id, PayrollEvents.payload(id));
            return toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * Records that the caller started a long-running operation of this kind on a resource; committed at once, so the
     * operation can be polled and cancelled while the work runs. Answers its id.
     */
    public UUID start(String kind, String resourceType, UUID resourceId) {
        AuthContext caller = AuthContext.require();
        UUID id = UuidV7.next(clock);
        runInWriteTransaction(() -> {
            repository.insert(id, kind, resourceType, resourceId, now(), caller.userId());
            audit.recordCreate(AGGREGATE, id);
            outbox.publish(AGGREGATE + PayrollEvents.CREATED, AGGREGATE, id, PayrollEvents.payload(id));
        });
        return id;
    }

    /** Reports progress; false when the operation is no longer running (it was cancelled) and the work should stop. */
    public boolean progress(UUID id, int percent) {
        UUID actor = AuthContext.require().userId();
        return doInWriteTransaction(() -> repository.progress(id, percent, now(), actor) == 1);
    }

    /**
     * Ends the operation as succeeded with its outcome (kept as JSON, answered as the contract's {@code result});
     * a cancelled operation stays cancelled. Audited as its {@code finish} step, with a {@code .succeeded} event.
     * Answers the operation as it stands.
     */
    public Operation succeed(UUID id, Map<String, Object> result) {
        UUID actor = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            if (repository.succeed(id, json.writeValueAsString(result), now(), actor) == 1) {
                audit.recordTransition(AGGREGATE, id, FINISH, List.of("status", "progressPercent", "result",
                        "doneTime"), null);
                outbox.publish(AGGREGATE + ".succeeded", AGGREGATE, id, PayrollEvents.payload(id));
            }
            return toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * Ends the operation as failed; the message is for the user and carries no personal data. Audited as its
     * {@code finish} step, with a {@code .failed} event.
     */
    public void fail(UUID id, String errorCode, String errorMessage) {
        UUID actor = AuthContext.require().userId();
        runInWriteTransaction(() -> {
            if (repository.fail(id, errorCode, errorMessage, now(), actor) == 1) {
                audit.recordTransition(AGGREGATE, id, FINISH, List.of("status", "errorMessage", "doneTime"), null);
                outbox.publish(AGGREGATE + ".failed", AGGREGATE, id, PayrollEvents.payload(id));
            }
        });
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private Operation toDto(OperationEntity entity) {
        Operation operation = mapper.toDto(entity);
        if (entity.getResult() != null) {
            operation.setResult(json.readValue(entity.getResult(), Map.class));
        }
        return operation;
    }

    private OperationEntity visible(AuthContext caller, UUID id, String permission) {
        OperationEntity row = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!caller.hasTenantWidePermission(permission) && !caller.userId().equals(row.getStartedByUserId())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return row;
    }
}
