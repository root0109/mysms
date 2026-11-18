package com.mysms.sis.business;

import com.mysms.authclient.AuthContext;
import com.mysms.sis.api.model.Operation;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.OperationEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.OperationMapper;
import com.mysms.sis.repository.OperationRepository;
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
 * sis's long-running operations (GetOperation, ListOperations, CancelOperation): a caller sees and cancels the
 * operations they started; the permission held tenant-wide covers every operation of the tenant. Anything else is
 * 404, never a hint that it exists.
 */
@Service
public class OperationService extends AppTransactionManager {

    static final String AGGREGATE = "sis.operation";
    static final String PERMISSION_READ = "sis.operation.read";
    static final String PERMISSION_CANCEL = "sis.operation.cancel_operation";

    private final OperationRepository repository;
    private final OperationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public OperationService(OperationRepository repository, OperationMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
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
            outbox.publish(AGGREGATE + ".cancelled", AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * Records a job that ran to its end inside the caller's write transaction (sis.IssueBonafideCertificate,
     * sis.ApplyPromotions answer 202 with it): a succeeded operation of the caller, audited and announced with the
     * work it reports, so the job centre lists it and GetOperation answers it like any other.
     *
     * @param operationName    the contract operation that ran (sis.ApplyPromotions)
     * @param resourceType     what the job worked on (a table name), with {@code resourceId} when it is one row
     * @param resultEntityType what it produced or changed (a table name), with {@code resultEntityId} when one row
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
            outbox.publish(AGGREGATE + ".succeeded", AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    private OperationEntity visible(AuthContext caller, UUID id, String permission) {
        OperationEntity row = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!caller.hasTenantWidePermission(permission) && !caller.userId().equals(row.getStartedByUserId())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return row;
    }
}
