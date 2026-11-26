package com.mysms.campus.transport.business;

import com.mysms.authclient.DataScope;
import com.mysms.campus.transport.api.model.StudentTransport;
import com.mysms.campus.transport.api.model.StudentTransportUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.campus.transport.config.TransportDatabaseConfig;
import com.mysms.campus.transport.config.TransportEventsConfig;
import com.mysms.campus.transport.domain.StudentTransportEntity;
import com.mysms.campus.transport.events.TransportEvents;
import com.mysms.campus.transport.mapper.StudentTransportMapper;
import com.mysms.campus.transport.repository.StudentTransportRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of transport.student_transport: audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class StudentTransportService extends AppTransactionManager {

    public static final String AGGREGATE = StudentTransportEntity.TABLE;
    static final String PERMISSION_LIST = "transport.student_transport.read";
    static final String PERMISSION_GET = "transport.student_transport.read";
    static final String PERMISSION_CREATE = "transport.student_transport.create";
    static final String PERMISSION_UPDATE = "transport.student_transport.update";
    static final String PERMISSION_DELETE = "transport.student_transport.delete";


    private final StudentTransportRepository repository;
    private final StudentTransportMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StudentTransportService(StudentTransportRepository repository, StudentTransportMapper mapper,
            @Qualifier(TransportDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(TransportDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(TransportEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(TransportEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public StudentTransport getStudentTransport(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<StudentTransport> listStudentTransports(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StudentTransportRepository.SORT_FIELDS,
                        StudentTransportRepository.DEFAULT_ORDER,
                StudentTransportRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StudentTransportRepository.FILTER_FIELDS)
                .and(StudentTransportRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StudentTransportEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StudentTransport createStudentTransport(StudentTransport request, boolean validateOnly) {
        validator.validate(request);
        StudentTransportEntity entity = mapper.toEntity(request);
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
            StudentTransportEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + TransportEvents.CREATED, AGGREGATE, stored.getId(),
                    TransportEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public StudentTransport updateStudentTransport(UUID id, String ifMatch, StudentTransportUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StudentTransportEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
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
            outbox.publish(AGGREGATE + TransportEvents.UPDATED, AGGREGATE, id, TransportEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStudentTransport(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StudentTransportEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + TransportEvents.DELETED, AGGREGATE, id, TransportEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StudentTransportEntity loadVisible(DataScope scope, UUID id) {
        StudentTransportEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the transport.route the row belongs to. */
    private Optional<UUID> institutionOf(StudentTransportEntity entity) {
        return repository.institutionOf(entity.getRouteId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(StudentTransportEntity entity, StudentTransportUpdate changes) {
        Patches patch = new Patches();
        patch.apply("studentId", changes.getStudentId(), entity::setStudentId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("transportMode", changes.getTransportMode(), v -> entity.setTransportMode(v == null ? null : v.getValue()));
        patch.apply("routeId", changes.getRouteId(), entity::setRouteId);
        patch.apply("routeStopId", changes.getRouteStopId(), entity::setRouteStopId);
        patch.apply("busPassIssued", changes.getBusPassIssued(), entity::setBusPassIssued);
        patch.apply("details", changes.getDetails(), entity::setDetails);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StudentTransportEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getTransportMode() == null) {
            errors.add(new FieldError("transportMode", "must not be null", "NotNull"));
        }
        if (!creating && entity.getBusPassIssued() == null) {
            errors.add(new FieldError("busPassIssued", "must not be null", "NotNull"));
        }
        if (entity.getRouteId() != null && !repository.existsRouteId(entity.getRouteId())) {
            errors.add(new FieldError("routeId", "route does not exist", "NOT_FOUND"));
        }
        if (entity.getRouteStopId() != null && !repository.existsRouteStopId(entity.getRouteStopId())) {
            errors.add(new FieldError("routeStopId", "route stop does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StudentTransportEntity entity) {
        if (repository.takenStudentIdAcademicYearId1(entity)) {
            throw new DuplicateResourceException(
                    "A student transport with the same student id and academic year id already exists.");
        }
    }
}
