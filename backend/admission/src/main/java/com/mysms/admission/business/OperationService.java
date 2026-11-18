package com.mysms.admission.business;

import com.mysms.admission.config.AdmissionDatabaseConfig;
import com.mysms.admission.config.AdmissionEventsConfig;
import com.mysms.admission.domain.OperationEntity;
import com.mysms.admission.events.AdmissionEvents;
import com.mysms.admission.mapper.OperationMapper;
import com.mysms.admission.repository.OperationRepository;
import com.mysms.authclient.AuthContext;
import com.mysms.admission.api.model.Operation;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * admission's long-running operations (GetOperation, ListOperations, CancelOperation): a caller sees and cancels the
 * operations they started; the permission held tenant-wide covers every operation of the tenant. Anything else is
 * 404, never a hint that it exists.
 */
@Service
public class OperationService extends AppTransactionManager {

    static final String AGGREGATE = "admission.operation";
    static final String PERMISSION_READ = "admission.operation.read";
    static final String PERMISSION_CANCEL = "admission.operation.cancel_operation";

    private final OperationRepository repository;
    private final OperationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public OperationService(OperationRepository repository, OperationMapper mapper,
            @Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AdmissionDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AdmissionEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AdmissionEventsConfig.AUDIT) AuditWriter audit,
            Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    public Operation getOperation(UUID id) {
        AuthContext caller = AuthContext.require();
        return doInReadTransaction(() -> {
            return mapper.toDto(visible(caller, id, PERMISSION_READ));
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
            return rows.map(mapper::toDto);
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
            if (repository.cancel(id, current.getRowVersion(), now, caller.userId()) == 0) {
                throw new PreconditionFailedException("The operation changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status", "doneTime"), null);
            outbox.publish(AGGREGATE + ".cancelled", AGGREGATE, id, AdmissionEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * Records that the caller starts a long-running piece of work on a resource of this module (status running);
     * audited, with a domain event. The work reports its end through {@link #finish}.
     */
    public Operation start(String operationName, String resourceType, UUID resourceId) {
        AuthContext caller = AuthContext.require();
        OperationEntity entity = OperationEntity.builder().operationName(operationName).resourceType(resourceType)
                .resourceId(resourceId).startedByUserId(caller.userId()).build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        entity.setStartedAt(entity.getCreatedAt());
        return doInWriteTransaction(() -> {
            repository.insert(entity);
            audit.recordCreate(AGGREGATE, entity.getId());
            outbox.publish(AGGREGATE + AdmissionEvents.CREATED, AGGREGATE, entity.getId(),
                    AdmissionEvents.payload(entity.getId()));
            return mapper.toDto(repository.findById(entity.getId()).orElseThrow());
        });
    }

    /**
     * Ends a running operation: succeeded with the record it produced, or failed with a machine code and a sentence
     * written on purpose (never another component's message). An operation cancelled meanwhile stays cancelled.
     */
    public Operation finish(UUID id, Outcome outcome) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        UUID actor = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            if (repository.finish(id, outcome.status(), outcome.errorCode(), outcome.errorMessage(),
                    outcome.resultType(), outcome.resultId(), now, actor) > 0) {
                audit.recordTransition(AGGREGATE, id, "finish", List.of("status", "doneTime", "error", "result"),
                        null);
                outbox.publish(AGGREGATE + "." + outcome.status(), AGGREGATE, id, AdmissionEvents.payload(id));
            }
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE,
                    id)));
        });
    }

    /**
     * How a piece of work ended.
     *
     * @param status       succeeded or failed
     * @param errorCode    a machine code when it failed
     * @param errorMessage a sentence for people when it failed (at most 500 characters)
     * @param resultType   the type of the record it produced, if any
     * @param resultId     that record
     */
    public record Outcome(String status, String errorCode, String errorMessage, String resultType, UUID resultId) {

        /** The work is done; what it produced, if anything. */
        public static Outcome succeeded(String resultType, UUID resultId) {
            return new Outcome("succeeded", null, null, resultType, resultId);
        }

        /** The work could not be done. */
        public static Outcome failed(String errorCode, String errorMessage) {
            return new Outcome("failed", errorCode, errorMessage, null, null);
        }
    }

    private OperationEntity visible(AuthContext caller, UUID id, String permission) {
        OperationEntity row = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!caller.hasTenantWidePermission(permission) && !caller.userId().equals(row.getStartedByUserId())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return row;
    }
}
