package com.mysms.frontoffice.business;

import com.mysms.authclient.DataScope;
import com.mysms.frontoffice.api.model.CallLog;
import com.mysms.frontoffice.api.model.CallLogUpdate;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.frontoffice.config.FrontofficeDatabaseConfig;
import com.mysms.frontoffice.config.FrontofficeEventsConfig;
import com.mysms.frontoffice.domain.CallLogEntity;
import com.mysms.frontoffice.events.FrontofficeEvents;
import com.mysms.frontoffice.mapper.CallLogMapper;
import com.mysms.frontoffice.repository.CallLogRepository;
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
    /** The contract's CallLogUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "direction", "purpose", "callerName", "incomingNumber",
                    "outgoingNumber",
            "callDate", "startTime", "endTime", "durationSeconds", "description", "loggedByUserId");


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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public CallLog updateCallLog(UUID id, String ifMatch, CallLogUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        WireValues.checkTime("endTime", changes.getEndTime(), wire);
        WireValues.checkDuration("durationSeconds", changes.getDurationSeconds(), 1, wire);
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
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
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

    /** Applies the selected CallLogUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(CallLogEntity entity, CallLogUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "direction" -> entity.setDirection(Optional.ofNullable(changes.getDirection())
                        .map(v -> v.getValue()).orElse(null));
                case "purpose" -> entity.setPurpose(changes.getPurpose());
                case "callerName" -> entity.setCallerName(changes.getCallerName());
                case "incomingNumber" -> entity.setIncomingNumber(changes.getIncomingNumber());
                case "outgoingNumber" -> entity.setOutgoingNumber(changes.getOutgoingNumber());
                case "callDate" -> entity.setCallDate(changes.getCallDate());
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "endTime" -> entity.setEndTime(WireValues.time(changes.getEndTime()));
                case "durationSeconds" -> entity.setDurationSeconds(
                        WireValues.duration(changes.getDurationSeconds(), 1));
                case "description" -> entity.setDescription(changes.getDescription());
                case "loggedByUserId" -> entity.setLoggedByUserId(changes.getLoggedByUserId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
