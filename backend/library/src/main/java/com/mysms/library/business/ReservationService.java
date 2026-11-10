package com.mysms.library.business;

import com.mysms.authclient.DataScope;
import com.mysms.library.api.model.AcceptReservationRequest;
import com.mysms.library.api.model.RejectReservationRequest;
import com.mysms.library.api.model.Reservation;
import com.mysms.library.api.model.ReservationUpdate;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.library.config.LibraryDatabaseConfig;
import com.mysms.library.config.LibraryEventsConfig;
import com.mysms.library.domain.ReservationEntity;
import com.mysms.library.events.LibraryEvents;
import com.mysms.library.mapper.ReservationMapper;
import com.mysms.library.repository.ReservationRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of library.reservation (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class ReservationService extends AppTransactionManager {

    public static final String AGGREGATE = ReservationEntity.TABLE;
    static final String PERMISSION_LIST = "library.reservation.read";
    static final String PERMISSION_GET = "library.reservation.read";
    static final String PERMISSION_CREATE = "library.reservation.create";
    static final String PERMISSION_UPDATE = "library.reservation.update";
    static final String PERMISSION_DELETE = "library.reservation.delete";
    static final String PERMISSION_ACCEPT_RESERVATION = "library.reservation.accept_reservation";
    static final String PERMISSION_REJECT_RESERVATION = "library.reservation.reject_reservation";
    /** The contract's ReservationUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("mediaId", "reference", "requestedByPersonId", "requestedByUserId", "dateFrom", "dateTo");


    private final ReservationRepository repository;
    private final ReservationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ReservationService(ReservationRepository repository, ReservationMapper mapper,
            @Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LibraryDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LibraryEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(LibraryEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Reservation getReservation(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Reservation> listReservations(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ReservationRepository.SORT_FIELDS, ReservationRepository.DEFAULT_ORDER,
                ReservationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ReservationRepository.FILTER_FIELDS)
                .and(ReservationRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ReservationEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Reservation createReservation(Reservation request, boolean validateOnly) {
        validator.validate(request);
        ReservationEntity entity = mapper.toEntity(request);
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
            ReservationEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LibraryEvents.CREATED, AGGREGATE, stored.getId(),
                    LibraryEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Reservation updateReservation(UUID id, String ifMatch, ReservationUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ReservationEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + LibraryEvents.UPDATED, AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteReservation(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ReservationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LibraryEvents.DELETED, AGGREGATE, id, LibraryEvents.payload(id));
        });
    }

    /**
     * library.AcceptReservation: Accept. The row must be visible and writable for the caller, and the step is allowed
     * while status is requested; in any other state
     * the answer is 409 and nothing changes.
     */
    public Reservation acceptReservation(UUID id, AcceptReservationRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ACCEPT_RESERVATION);
        return doInWriteTransaction(() -> {
            ReservationEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("requested").contains(entity.getStatus()))) {
                throw new InvalidStateException("The reservation cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.acceptReservation(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "accept", List.of("status"), null);
            outbox.publish(AGGREGATE + ".accept", AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * library.RejectReservation: Reject. The row must be visible and writable for the caller, and the step is allowed
     * while status is requested; in any other state
     * the answer is 409 and nothing changes.
     */
    public Reservation rejectReservation(UUID id, RejectReservationRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_RESERVATION);
        return doInWriteTransaction(() -> {
            ReservationEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("requested").contains(entity.getStatus()))) {
                throw new InvalidStateException("The reservation cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectReservation(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reject", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".reject", AGGREGATE, id, LibraryEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ReservationEntity loadVisible(DataScope scope, UUID id) {
        ReservationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the library.media it belongs to). */
    private Optional<UUID> institutionOf(ReservationEntity entity) {
        return repository.institutionOf(entity.getMediaId());
    }

    /** Applies the selected ReservationUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(ReservationEntity entity, ReservationUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "mediaId" -> entity.setMediaId(changes.getMediaId());
                case "reference" -> entity.setReference(changes.getReference());
                case "requestedByPersonId" -> entity.setRequestedByPersonId(changes.getRequestedByPersonId());
                case "requestedByUserId" -> entity.setRequestedByUserId(changes.getRequestedByUserId());
                case "dateFrom" -> entity.setDateFrom(changes.getDateFrom());
                case "dateTo" -> entity.setDateTo(changes.getDateTo());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ReservationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getMediaId() == null) {
            errors.add(new FieldError("mediaId", "must not be null", "NotNull"));
        }
        if (entity.getDateFrom() == null) {
            errors.add(new FieldError("dateFrom", "must not be null", "NotNull"));
        }
        if (entity.getDateTo() == null) {
            errors.add(new FieldError("dateTo", "must not be null", "NotNull"));
        }
        if (entity.getMediaId() != null && !repository.existsMediaId(entity.getMediaId())) {
            errors.add(new FieldError("mediaId", "media does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ReservationEntity entity) {

    }
}
