package com.mysms.campus.frontoffice.business;

import com.mysms.authclient.DataScope;
import com.mysms.campus.frontoffice.api.model.CallLog;
import com.mysms.campus.frontoffice.api.model.CallLogUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.campus.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.campus.frontoffice.config.FrontofficeEventsConfig;
import com.mysms.campus.frontoffice.domain.CallLogEntity;
import com.mysms.campus.frontoffice.events.FrontofficeEvents;
import com.mysms.campus.frontoffice.mapper.CallLogMapper;
import com.mysms.campus.frontoffice.repository.CallLogRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of frontoffice.call_log: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class CallLogService extends AppTransactionManager {

    public static final String AGGREGATE = CallLogEntity.TABLE;
    static final String PERMISSION_LIST = "frontoffice.call_log.read";
    static final String PERMISSION_GET = "frontoffice.call_log.read";
    static final String PERMISSION_CREATE = "frontoffice.call_log.create";
    static final String PERMISSION_UPDATE = "frontoffice.call_log.update";
    static final String PERMISSION_DELETE = "frontoffice.call_log.delete";


    private final CallLogRepository repository;
    private final CallLogMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public CallLogService(CallLogRepository repository, CallLogMapper mapper,
            @Qualifier(FrontofficeDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FrontofficeDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(FrontofficeEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(FrontofficeEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public CallLog getCallLog(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<CallLog> listCallLogs(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CallLogRepository.SORT_FIELDS, CallLogRepository.DEFAULT_ORDER,
                CallLogRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CallLogRepository.FILTER_FIELDS)
                .and(CallLogRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CallLogEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CallLog createCallLog(CallLog request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("endTime", request.getEndTime(), wire);
        WireValues.checkDuration("durationSeconds", request.getDurationSeconds(), 1, wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        CallLogEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            CallLogEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + FrontofficeEvents.CREATED, AGGREGATE, stored.getId(),
                    FrontofficeEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public CallLog updateCallLog(UUID id, String ifMatch, CallLogUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", Patches.value(changes.getStartTime()), wire);
        WireValues.checkTime("endTime", Patches.value(changes.getEndTime()), wire);
        WireValues.checkDuration("durationSeconds", Patches.value(changes.getDurationSeconds()), 1, wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            CallLogEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + FrontofficeEvents.UPDATED, AGGREGATE, id, FrontofficeEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCallLog(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CallLogEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + FrontofficeEvents.DELETED, AGGREGATE, id, FrontofficeEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CallLogEntity loadVisible(DataScope scope, UUID id) {
        CallLogEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(CallLogEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(CallLogEntity entity, CallLogUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("direction", changes.getDirection(), v -> entity.setDirection(v == null ? null : v.getValue()));
        patch.apply("purpose", changes.getPurpose(), entity::setPurpose);
        patch.apply("callerName", changes.getCallerName(), entity::setCallerName);
        patch.apply("incomingNumber", changes.getIncomingNumber(), entity::setIncomingNumber);
        patch.apply("outgoingNumber", changes.getOutgoingNumber(), entity::setOutgoingNumber);
        patch.apply("callDate", changes.getCallDate(), entity::setCallDate);
        patch.apply("startTime", changes.getStartTime(), v -> entity.setStartTime(WireValues.time(v)));
        patch.apply("endTime", changes.getEndTime(), v -> entity.setEndTime(WireValues.time(v)));
        patch.apply("durationSeconds", changes.getDurationSeconds(), v -> entity.setDurationSeconds(WireValues.duration(v, 1)));
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("loggedByUserId", changes.getLoggedByUserId(), entity::setLoggedByUserId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CallLogEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getDirection() == null) {
            errors.add(new FieldError("direction", "must not be null", "NotNull"));
        }
        if (entity.getCallerName() == null) {
            errors.add(new FieldError("callerName", "must not be null", "NotNull"));
        }
        if (entity.getCallDate() == null) {
            errors.add(new FieldError("callDate", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CallLogEntity entity) {

    }
}
