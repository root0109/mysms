package com.mysms.campus.frontoffice.business;

import com.mysms.authclient.DataScope;
import com.mysms.campus.frontoffice.api.model.CheckOutVisitorRequest;
import com.mysms.campus.frontoffice.api.model.VisitorLog;
import com.mysms.campus.frontoffice.api.model.VisitorLogUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
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
import com.mysms.campus.frontoffice.domain.VisitorLogEntity;
import com.mysms.campus.frontoffice.events.FrontofficeEvents;
import com.mysms.campus.frontoffice.mapper.VisitorLogMapper;
import com.mysms.campus.frontoffice.repository.VisitorLogRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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
    static final String PERMISSION_CHECK_OUT = "frontoffice.visitor_log.check_out_visitor";
    static final String STEP_CHECK_OUT = "checkOutVisitor";
    /** The zone whose day starts first: a visit dated after its today has not happened anywhere yet. */
    private static final ZoneOffset LATEST_ZONE = ZoneOffset.ofHours(14);


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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public VisitorLog updateVisitorLog(UUID id, String ifMatch, VisitorLogUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("entryTime", Patches.value(changes.getEntryTime()), wire);
        WireValues.checkTime("exitTime", Patches.value(changes.getExitTime()), wire);
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

    /**
     * Records when a visitor still inside left: once only (409 after), not before the entry time and not for a visit
     * dated after today anywhere on earth (422), under the body etag when one is sent (412). A visit is one day's: one
     * that runs past midnight is checked out at 23:59.
     */
    public VisitorLog checkOutVisitor(UUID id, CheckOutVisitorRequest request) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("exitTime", request.getExitTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        OptionalInt bodyVersion = Etags.bodyVersion(request.getEtag());
        DataScope scope = DataScope.current(PERMISSION_CHECK_OUT);
        return doInWriteTransaction(() -> {
            VisitorLogEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (bodyVersion.isPresent() && bodyVersion.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (entity.getExitTime() != null) {
                throw new InvalidStateException("The visitor has already been checked out.");
            }
            if (entity.getVisitDate().isAfter(LocalDate.now(clock.withZone(LATEST_ZONE)))) {
                throw new ValidationFailedException(
                        List.of(new FieldError("exitTime", "the visit is still to come", "VisitNotStarted")));
            }
            entity.setExitTime(WireValues.time(request.getExitTime()));
            if (entity.getEntryTime() != null && entity.getExitTime().isBefore(entity.getEntryTime())) {
                throw new ValidationFailedException(
                        List.of(new FieldError("exitTime", "must not be before the entry time", "ExitBeforeEntry")));
            }
            int expected = entity.getRowVersion();
            entity.onUpdatingData(Actor.current(), clock);
            if (repository.checkOut(entity, expected) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordTransition(AGGREGATE, id, STEP_CHECK_OUT, List.of("exitTime"), null);
            outbox.publish(AGGREGATE + "." + STEP_CHECK_OUT, AGGREGATE, id, FrontofficeEvents.payload(id));
            return mapper.toDto(entity);
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

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(VisitorLogEntity entity, VisitorLogUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("visitorName", changes.getVisitorName(), entity::setVisitorName);
        patch.apply("visitorType", changes.getVisitorType(), entity::setVisitorType);
        patch.apply("email", changes.getEmail(), entity::setEmail);
        patch.apply("phone", changes.getPhone(), entity::setPhone);
        patch.apply("companyName", changes.getCompanyName(), entity::setCompanyName);
        patch.apply("address", changes.getAddress(), entity::setAddress);
        patch.apply("studentId", changes.getStudentId(), entity::setStudentId);
        patch.apply("relationToStudent", changes.getRelationToStudent(), entity::setRelationToStudent);
        patch.apply("relatedPersonName", changes.getRelatedPersonName(), entity::setRelatedPersonName);
        patch.apply("visitorCount", changes.getVisitorCount(), entity::setVisitorCount);
        patch.apply("purpose", changes.getPurpose(), entity::setPurpose);
        patch.apply("hostStaffId", changes.getHostStaffId(), entity::setHostStaffId);
        patch.apply("visitDate", changes.getVisitDate(), entity::setVisitDate);
        patch.apply("entryTime", changes.getEntryTime(), v -> entity.setEntryTime(WireValues.time(v)));
        patch.apply("exitTime", changes.getExitTime(), v -> entity.setExitTime(WireValues.time(v)));
        patch.apply("remarks", changes.getRemarks(), entity::setRemarks);
        return patch.applied();
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
