package com.mysms.frontoffice.business;

import com.mysms.authclient.DataScope;
import com.mysms.frontoffice.api.model.VisitorLog;
import com.mysms.frontoffice.api.model.VisitorLogUpdate;
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
import com.mysms.frontoffice.domain.VisitorLogEntity;
import com.mysms.frontoffice.events.FrontofficeEvents;
import com.mysms.frontoffice.mapper.VisitorLogMapper;
import com.mysms.frontoffice.repository.VisitorLogRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of frontoffice.visitor_log: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class VisitorLogService extends AppTransactionManager {

    public static final String AGGREGATE = VisitorLogEntity.TABLE;
    static final String PERMISSION_LIST = "frontoffice.visitor_log.read";
    static final String PERMISSION_GET = "frontoffice.visitor_log.read";
    static final String PERMISSION_CREATE = "frontoffice.visitor_log.create";
    static final String PERMISSION_UPDATE = "frontoffice.visitor_log.update";
    static final String PERMISSION_DELETE = "frontoffice.visitor_log.delete";
    /** The contract's VisitorLogUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "visitorName", "visitorType", "email", "phone", "companyName",
                    "address",
            "studentId", "relationToStudent", "relatedPersonName", "visitorCount", "purpose", "hostStaffId",
            "visitDate", "entryTime", "exitTime", "remarks");


    private final VisitorLogRepository repository;
    private final VisitorLogMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public VisitorLogService(VisitorLogRepository repository, VisitorLogMapper mapper,
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

    public VisitorLog getVisitorLog(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<VisitorLog> listVisitorLogs(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, VisitorLogRepository.SORT_FIELDS, VisitorLogRepository.DEFAULT_ORDER,
                VisitorLogRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, VisitorLogRepository.FILTER_FIELDS)
                .and(VisitorLogRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<VisitorLogEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public VisitorLog createVisitorLog(VisitorLog request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("entryTime", request.getEntryTime(), wire);
        WireValues.checkTime("exitTime", request.getExitTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        VisitorLogEntity entity = mapper.toEntity(request);
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
            VisitorLogEntity stored = repository.insert(entity);
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
    public VisitorLog updateVisitorLog(UUID id, String ifMatch, VisitorLogUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("entryTime", changes.getEntryTime(), wire);
        WireValues.checkTime("exitTime", changes.getExitTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            VisitorLogEntity entity = loadVisible(scope, id);
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
    public void deleteVisitorLog(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            VisitorLogEntity entity = loadVisible(scope, id);
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
    private VisitorLogEntity loadVisible(DataScope scope, UUID id) {
        VisitorLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(VisitorLogEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected VisitorLogUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(VisitorLogEntity entity, VisitorLogUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "visitorName" -> entity.setVisitorName(changes.getVisitorName());
                case "visitorType" -> entity.setVisitorType(changes.getVisitorType());
                case "email" -> entity.setEmail(changes.getEmail());
                case "phone" -> entity.setPhone(changes.getPhone());
                case "companyName" -> entity.setCompanyName(changes.getCompanyName());
                case "address" -> entity.setAddress(changes.getAddress());
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "relationToStudent" -> entity.setRelationToStudent(changes.getRelationToStudent());
                case "relatedPersonName" -> entity.setRelatedPersonName(changes.getRelatedPersonName());
                case "visitorCount" -> entity.setVisitorCount(changes.getVisitorCount());
                case "purpose" -> entity.setPurpose(changes.getPurpose());
                case "hostStaffId" -> entity.setHostStaffId(changes.getHostStaffId());
                case "visitDate" -> entity.setVisitDate(changes.getVisitDate());
                case "entryTime" -> entity.setEntryTime(WireValues.time(changes.getEntryTime()));
                case "exitTime" -> entity.setExitTime(WireValues.time(changes.getExitTime()));
                case "remarks" -> entity.setRemarks(changes.getRemarks());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(VisitorLogEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getVisitorName() == null) {
            errors.add(new FieldError("visitorName", "must not be null", "NotNull"));
        }
        if (entity.getVisitorType() == null) {
            errors.add(new FieldError("visitorType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getVisitorCount() == null) {
            errors.add(new FieldError("visitorCount", "must not be null", "NotNull"));
        }
        if (entity.getPurpose() == null) {
            errors.add(new FieldError("purpose", "must not be null", "NotNull"));
        }
        if (entity.getVisitDate() == null) {
            errors.add(new FieldError("visitDate", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(VisitorLogEntity entity) {

    }
}
