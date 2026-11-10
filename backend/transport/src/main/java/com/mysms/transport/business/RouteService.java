package com.mysms.transport.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.transport.api.model.Route;
import com.mysms.transport.api.model.RouteStop;
import com.mysms.transport.api.model.RouteStopUpdate;
import com.mysms.transport.api.model.RouteUpdate;
import com.mysms.transport.config.TransportDatabaseConfig;
import com.mysms.transport.config.TransportEventsConfig;
import com.mysms.transport.domain.RouteEntity;
import com.mysms.transport.domain.RouteStopEntity;
import com.mysms.transport.events.TransportEvents;
import com.mysms.transport.mapper.RouteMapper;
import com.mysms.transport.mapper.RouteStopMapper;
import com.mysms.transport.repository.RouteRepository;
import com.mysms.transport.repository.RouteStopRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of transport.route (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Route
 * are served here too — RouteStop.
 */
@Service
public class RouteService extends AppTransactionManager {

    public static final String AGGREGATE = RouteEntity.TABLE;
    static final String PERMISSION_LIST = "transport.route.read";
    static final String PERMISSION_GET = "transport.route.read";
    static final String PERMISSION_CREATE = "transport.route.create";
    static final String PERMISSION_UPDATE = "transport.route.update";
    static final String PERMISSION_DELETE = "transport.route.delete";
    /** The contract's RouteUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "name", "vehicleNumber", "startTime", "endTime", "isActive");

    public static final String ROUTE_STOP_AGGREGATE = RouteStopEntity.TABLE;
    static final String ROUTE_STOP_PERMISSION_LIST = "transport.route_stop.read";
    static final String ROUTE_STOP_PERMISSION_GET = "transport.route_stop.read";
    static final String ROUTE_STOP_PERMISSION_CREATE = "transport.route_stop.create";
    static final String ROUTE_STOP_PERMISSION_UPDATE = "transport.route_stop.update";
    static final String ROUTE_STOP_PERMISSION_DELETE = "transport.route_stop.delete";
    /** The contract's RouteStopUpdate fields, in apply order. */
    static final List<String> ROUTE_STOP_WRITABLE = List.of("sequence", "stopName", "pickupTime", "dropTime");

    private final RouteRepository repository;
    private final RouteMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final RouteStopRepository routeStopRepository;
    private final RouteStopMapper routeStopMapper;

    public RouteService(RouteRepository repository,
            RouteMapper mapper,
            @Qualifier(TransportDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(TransportDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(TransportEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(TransportEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            RouteStopRepository routeStopRepository,
            RouteStopMapper routeStopMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.routeStopRepository = routeStopRepository;
        this.routeStopMapper = routeStopMapper;
    }

    public Route getRoute(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Route> listRoutes(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RouteRepository.SORT_FIELDS, RouteRepository.DEFAULT_ORDER,
                RouteRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RouteRepository.FILTER_FIELDS)
                .and(RouteRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<RouteEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Route createRoute(Route request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("endTime", request.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        RouteEntity entity = mapper.toEntity(request);
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
            RouteEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + TransportEvents.CREATED, AGGREGATE, stored.getId(),
                    TransportEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Route updateRoute(UUID id, String ifMatch, RouteUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        WireValues.checkTime("endTime", changes.getEndTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            RouteEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + TransportEvents.UPDATED, AGGREGATE, id, TransportEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRoute(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            RouteEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByRouteStopRouteId(id)) {
                throw new InvalidStateException(
                        "The route is still referenced by transport.route_stop rows; remove those first.");
            }
            if (repository.referencedByStudentTransportRouteId(id)) {
                throw new InvalidStateException(
                        "The route is still referenced by transport.student_transport rows; remove those first.");
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
    private RouteEntity loadVisible(DataScope scope, UUID id) {
        RouteEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(RouteEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected RouteUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(RouteEntity entity, RouteUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "name" -> entity.setName(changes.getName());
                case "vehicleNumber" -> entity.setVehicleNumber(changes.getVehicleNumber());
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "endTime" -> entity.setEndTime(WireValues.time(changes.getEndTime()));
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(RouteEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getVehicleNumber() == null) {
            errors.add(new FieldError("vehicleNumber", "must not be null", "NotNull"));
        }
        if (entity.getStartTime() == null) {
            errors.add(new FieldError("startTime", "must not be null", "NotNull"));
        }
        if (entity.getEndTime() == null) {
            errors.add(new FieldError("endTime", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(RouteEntity entity) {

    }

    // ---- RouteStop: a record of the Route aggregate ----

    public RouteStop getRouteStop(UUID routeId, UUID id) {
        return doInReadTransaction(() -> {
            return routeStopMapper.toDto(loadVisibleRouteStop(DataScope.current(ROUTE_STOP_PERMISSION_GET), routeId, id));
        });
    }

    public Page<RouteStop> listRouteStops(UUID routeId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RouteStopRepository.SORT_FIELDS, RouteStopRepository.DEFAULT_ORDER,
                RouteStopRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RouteStopRepository.FILTER_FIELDS)
                .and(RouteStopRepository.ofParent(routeId))
                        .and(RouteStopRepository.scope(DataScope.current(ROUTE_STOP_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + routeId, clock);
        return doInReadTransaction(() -> {
            Page<RouteStopEntity> rows = routeStopRepository.list(page, where);
            return rows.map(routeStopMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RouteStop createRouteStop(UUID routeId, RouteStop request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("pickupTime", request.getPickupTime(), wire);
        WireValues.checkTime("dropTime", request.getDropTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        RouteStopEntity entity = routeStopMapper.toEntity(request);
        entity.setRouteId(routeId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ROUTE_STOP_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!routeStopRepository.existsRouteId(entity.getRouteId())) {
                throw new ResourceNotFoundException("transport.route", entity.getRouteId());
            }
            if (!scope.canRead(institutionOfRouteStop(entity))) {
                throw new ResourceNotFoundException("transport.route", entity.getRouteId());
            }
            assertValidRouteStop(entity, true);
            scope.requireWritable(institutionOfRouteStop(entity));
            assertUniqueRouteStop(entity);
            if (validateOnly) {
                return routeStopMapper.toDto(entity);
            }
            RouteStopEntity stored = routeStopRepository.insert(entity);
            audit.recordCreate(ROUTE_STOP_AGGREGATE, stored.getId());
            outbox.publish(ROUTE_STOP_AGGREGATE + TransportEvents.CREATED, ROUTE_STOP_AGGREGATE, stored.getId(),
                    TransportEvents.payload(stored.getId()));
            return routeStopMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public RouteStop updateRouteStop(UUID routeId, UUID id, String ifMatch, RouteStopUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ROUTE_STOP_WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("pickupTime", changes.getPickupTime(), wire);
        WireValues.checkTime("dropTime", changes.getDropTime(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ROUTE_STOP_PERMISSION_UPDATE);
            RouteStopEntity entity = loadVisibleRouteStop(scope, routeId, id);
            scope.requireWritable(institutionOfRouteStop(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyRouteStop(entity, changes, fields);
            validator.validate(routeStopMapper.toDto(entity));
            assertValidRouteStop(entity, false);
            scope.requireWritable(institutionOfRouteStop(entity));
            assertUniqueRouteStop(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return routeStopMapper.toDto(entity);
            }
            if (routeStopRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ROUTE_STOP_AGGREGATE, id, fields);
            outbox.publish(ROUTE_STOP_AGGREGATE + TransportEvents.UPDATED, ROUTE_STOP_AGGREGATE, id, TransportEvents.payload(id));
            return routeStopMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRouteStop(UUID routeId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ROUTE_STOP_PERMISSION_DELETE);
            RouteStopEntity entity = loadVisibleRouteStop(scope, routeId, id);
            scope.requireWritable(institutionOfRouteStop(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (routeStopRepository.referencedByStudentTransportRouteStopId(id)) {
                throw new InvalidStateException(
                        "The route stop is still referenced by transport.student_transport rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (routeStopRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ROUTE_STOP_AGGREGATE, id);
            outbox.publish(ROUTE_STOP_AGGREGATE + TransportEvents.DELETED, ROUTE_STOP_AGGREGATE, id, TransportEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RouteStopEntity loadVisibleRouteStop(DataScope scope, UUID routeId, UUID id) {
        RouteStopEntity entity = routeStopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ROUTE_STOP_AGGREGATE, id));
        if (!scope.canRead(institutionOfRouteStop(entity)) || !routeId.equals(entity.getRouteId())) {
            throw new ResourceNotFoundException(ROUTE_STOP_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the transport.route it belongs to). */
    private Optional<UUID> institutionOfRouteStop(RouteStopEntity entity) {
        return routeStopRepository.institutionOf(entity.getRouteId());
    }

    /** Applies the selected RouteStopUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void applyRouteStop(RouteStopEntity entity, RouteStopUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "sequence" -> entity.setSequence(changes.getSequence());
                case "stopName" -> entity.setStopName(changes.getStopName());
                case "pickupTime" -> entity.setPickupTime(WireValues.time(changes.getPickupTime()));
                case "dropTime" -> entity.setDropTime(WireValues.time(changes.getDropTime()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRouteStop(RouteStopEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRouteId() == null) {
            errors.add(new FieldError("routeId", "must not be null", "NotNull"));
        }
        if (entity.getSequence() == null) {
            errors.add(new FieldError("sequence", "must not be null", "NotNull"));
        }
        if (entity.getStopName() == null) {
            errors.add(new FieldError("stopName", "must not be null", "NotNull"));
        }
        if (entity.getRouteId() != null && !routeStopRepository.existsRouteId(entity.getRouteId())) {
            errors.add(new FieldError("routeId", "route does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRouteStop(RouteStopEntity entity) {
        if (routeStopRepository.takenRouteIdSequence1(entity)) {
            throw new DuplicateResourceException("A route stop with the same route id and sequence already exists.");
        }
    }
}
