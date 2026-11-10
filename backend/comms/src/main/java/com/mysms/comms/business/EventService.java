package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.ActivateEventRequest;
import com.mysms.comms.api.model.CancelEventOccurrenceRequest;
import com.mysms.comms.api.model.DeactivateEventRequest;
import com.mysms.comms.api.model.Event;
import com.mysms.comms.api.model.EventAttendee;
import com.mysms.comms.api.model.EventAttendeeUpdate;
import com.mysms.comms.api.model.EventGalleryItem;
import com.mysms.comms.api.model.EventGalleryItemUpdate;
import com.mysms.comms.api.model.EventInstitution;
import com.mysms.comms.api.model.EventInstitutionUpdate;
import com.mysms.comms.api.model.EventOccurrence;
import com.mysms.comms.api.model.EventOccurrenceUpdate;
import com.mysms.comms.api.model.EventUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.EventAttendeeEntity;
import com.mysms.comms.domain.EventEntity;
import com.mysms.comms.domain.EventGalleryItemEntity;
import com.mysms.comms.domain.EventInstitutionEntity;
import com.mysms.comms.domain.EventOccurrenceEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.EventAttendeeMapper;
import com.mysms.comms.mapper.EventGalleryItemMapper;
import com.mysms.comms.mapper.EventInstitutionMapper;
import com.mysms.comms.mapper.EventMapper;
import com.mysms.comms.mapper.EventOccurrenceMapper;
import com.mysms.comms.repository.EventAttendeeRepository;
import com.mysms.comms.repository.EventGalleryItemRepository;
import com.mysms.comms.repository.EventInstitutionRepository;
import com.mysms.comms.repository.EventOccurrenceRepository;
import com.mysms.comms.repository.EventRepository;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
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
 * Business service of comms.event (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Event
 * are served here too — EventAttendee, EventGalleryItem, EventInstitution, EventOccurrence.
 */
@Service
public class EventService extends AppTransactionManager {

    public static final String AGGREGATE = EventEntity.TABLE;
    static final String PERMISSION_LIST = "comms.event.read";
    static final String PERMISSION_GET = "comms.event.read";
    static final String PERMISSION_CREATE = "comms.event.create";
    static final String PERMISSION_UPDATE = "comms.event.update";
    static final String PERMISSION_DELETE = "comms.event.delete";
    static final String PERMISSION_ACTIVATE_EVENT = "comms.event.activate_event";
    static final String PERMISSION_DEACTIVATE_EVENT = "comms.event.deactivate_event";
    /** The contract's EventUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "audience", "classSectionId", "audienceLabel", "title",
                    "description",
            "category", "location", "organisedBy", "imageFileId", "color", "startAt", "endAt", "isAllDay",
            "repeatCount", "repeatInterval", "repeatUnit", "url", "cohortId", "ownerUserId", "recurrenceRule",
            "forAllMembers", "staffCanEdit", "importance");

    public static final String EVENT_ATTENDEE_AGGREGATE = EventAttendeeEntity.TABLE;
    static final String EVENT_ATTENDEE_PERMISSION_LIST = "comms.event_attendee.read";
    static final String EVENT_ATTENDEE_PERMISSION_GET = "comms.event_attendee.read";
    static final String EVENT_ATTENDEE_PERMISSION_CREATE = "comms.event_attendee.create";
    static final String EVENT_ATTENDEE_PERMISSION_UPDATE = "comms.event_attendee.update";
    static final String EVENT_ATTENDEE_PERMISSION_DELETE = "comms.event_attendee.delete";
    /** The contract's EventAttendeeUpdate fields, in apply order. */
    static final List<String> EVENT_ATTENDEE_WRITABLE = List.of("userId");

    public static final String EVENT_GALLERY_ITEM_AGGREGATE = EventGalleryItemEntity.TABLE;
    static final String EVENT_GALLERY_ITEM_PERMISSION_LIST = "comms.event_gallery_item.read";
    static final String EVENT_GALLERY_ITEM_PERMISSION_GET = "comms.event_gallery_item.read";
    static final String EVENT_GALLERY_ITEM_PERMISSION_CREATE = "comms.event_gallery_item.create";
    static final String EVENT_GALLERY_ITEM_PERMISSION_UPDATE = "comms.event_gallery_item.update";
    static final String EVENT_GALLERY_ITEM_PERMISSION_DELETE = "comms.event_gallery_item.delete";
    /** The contract's EventGalleryItemUpdate fields, in apply order. */
    static final List<String> EVENT_GALLERY_ITEM_WRITABLE = List.of("fileId", "sortOrder");

    public static final String EVENT_INSTITUTION_AGGREGATE = EventInstitutionEntity.TABLE;
    static final String EVENT_INSTITUTION_PERMISSION_LIST = "comms.event_institution.read";
    static final String EVENT_INSTITUTION_PERMISSION_GET = "comms.event_institution.read";
    static final String EVENT_INSTITUTION_PERMISSION_CREATE = "comms.event_institution.create";
    static final String EVENT_INSTITUTION_PERMISSION_UPDATE = "comms.event_institution.update";
    static final String EVENT_INSTITUTION_PERMISSION_DELETE = "comms.event_institution.delete";
    /** The contract's EventInstitutionUpdate fields, in apply order. */
    static final List<String> EVENT_INSTITUTION_WRITABLE = List.of("institutionId");

    public static final String EVENT_OCCURRENCE_AGGREGATE = EventOccurrenceEntity.TABLE;
    static final String EVENT_OCCURRENCE_PERMISSION_LIST = "comms.event_occurrence.read";
    static final String EVENT_OCCURRENCE_PERMISSION_GET = "comms.event_occurrence.read";
    static final String EVENT_OCCURRENCE_PERMISSION_CREATE = "comms.event_occurrence.create";
    static final String EVENT_OCCURRENCE_PERMISSION_UPDATE = "comms.event_occurrence.update";
    static final String EVENT_OCCURRENCE_PERMISSION_DELETE = "comms.event_occurrence.delete";
    static final String PERMISSION_CANCEL_EVENT_OCCURRENCE = "comms.event_occurrence.cancel_event_occurrence";
    /** The contract's EventOccurrenceUpdate fields, in apply order. */
    static final List<String> EVENT_OCCURRENCE_WRITABLE =
            List.of("userId", "cohortId", "title", "color", "description", "startAt", "endAt", "isCancelled",
                    "responseStatus",
            "importance");

    private final EventRepository repository;
    private final EventMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final EventAttendeeRepository eventAttendeeRepository;
    private final EventAttendeeMapper eventAttendeeMapper;
    private final EventGalleryItemRepository eventGalleryItemRepository;
    private final EventGalleryItemMapper eventGalleryItemMapper;
    private final EventInstitutionRepository eventInstitutionRepository;
    private final EventInstitutionMapper eventInstitutionMapper;
    private final EventOccurrenceRepository eventOccurrenceRepository;
    private final EventOccurrenceMapper eventOccurrenceMapper;

    public EventService(EventRepository repository,
            EventMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            EventAttendeeRepository eventAttendeeRepository,
            EventAttendeeMapper eventAttendeeMapper,
            EventGalleryItemRepository eventGalleryItemRepository,
            EventGalleryItemMapper eventGalleryItemMapper,
            EventInstitutionRepository eventInstitutionRepository,
            EventInstitutionMapper eventInstitutionMapper,
            EventOccurrenceRepository eventOccurrenceRepository,
            EventOccurrenceMapper eventOccurrenceMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.eventAttendeeRepository = eventAttendeeRepository;
        this.eventAttendeeMapper = eventAttendeeMapper;
        this.eventGalleryItemRepository = eventGalleryItemRepository;
        this.eventGalleryItemMapper = eventGalleryItemMapper;
        this.eventInstitutionRepository = eventInstitutionRepository;
        this.eventInstitutionMapper = eventInstitutionMapper;
        this.eventOccurrenceRepository = eventOccurrenceRepository;
        this.eventOccurrenceMapper = eventOccurrenceMapper;
    }

    public Event getEvent(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Event> listEvents(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, EventRepository.SORT_FIELDS, EventRepository.DEFAULT_ORDER,
                EventRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EventRepository.FILTER_FIELDS)
                .and(EventRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<EventEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Event createEvent(Event request, boolean validateOnly) {
        validator.validate(request);
        EventEntity entity = mapper.toEntity(request);
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
            EventEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Event updateEvent(UUID id, String ifMatch, EventUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            EventEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEvent(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            EventEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByEventAttendeeEventId(id)) {
                throw new InvalidStateException(
                        "The event is still referenced by comms.event_attendee rows; remove those first.");
            }
            if (repository.referencedByEventGalleryItemEventId(id)) {
                throw new InvalidStateException(
                        "The event is still referenced by comms.event_gallery_item rows; remove those first.");
            }
            if (repository.referencedByEventInstitutionEventId(id)) {
                throw new InvalidStateException(
                        "The event is still referenced by comms.event_institution rows; remove those first.");
            }
            if (repository.referencedByEventOccurrenceEventId(id)) {
                throw new InvalidStateException(
                        "The event is still referenced by comms.event_occurrence rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /**
     * comms.ActivateEvent: Approve / show. The row must be visible and writable for the caller, and the step is allowed
     * while status is inactive; in any other state
     * the answer is 409 and nothing changes.
     */
    public Event activateEvent(UUID id, ActivateEventRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ACTIVATE_EVENT);
        return doInWriteTransaction(() -> {
            EventEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("inactive").contains(entity.getStatus()))) {
                throw new InvalidStateException("The event cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.activateEvent(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "activate", List.of("status"), null);
            outbox.publish(AGGREGATE + ".activate", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.DeactivateEvent: Hide. The row must be visible and writable for the caller, and the step is allowed while
     * status is active; in any other state
     * the answer is 409 and nothing changes.
     */
    public Event deactivateEvent(UUID id, DeactivateEventRequest request) {
        DataScope scope = DataScope.current(PERMISSION_DEACTIVATE_EVENT);
        return doInWriteTransaction(() -> {
            EventEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("active").contains(entity.getStatus()))) {
                throw new InvalidStateException("The event cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.deactivateEvent(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "deactivate", List.of("status"), null);
            outbox.publish(AGGREGATE + ".deactivate", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EventEntity loadVisible(DataScope scope, UUID id) {
        EventEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(EventEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected EventUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(EventEntity entity, EventUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "audience" -> entity.setAudience(Optional.ofNullable(changes.getAudience()).map(v -> v.getValue())
                        .orElse(null));
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "audienceLabel" -> entity.setAudienceLabel(changes.getAudienceLabel());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "category" -> entity.setCategory(Optional.ofNullable(changes.getCategory()).map(v -> v.getValue())
                        .orElse(null));
                case "location" -> entity.setLocation(changes.getLocation());
                case "organisedBy" -> entity.setOrganisedBy(changes.getOrganisedBy());
                case "imageFileId" -> entity.setImageFileId(changes.getImageFileId());
                case "color" -> entity.setColor(changes.getColor());
                case "startAt" -> entity.setStartAt(changes.getStartAt());
                case "endAt" -> entity.setEndAt(changes.getEndAt());
                case "isAllDay" -> entity.setIsAllDay(changes.getIsAllDay());
                case "repeatCount" -> entity.setRepeatCount(changes.getRepeatCount());
                case "repeatInterval" -> entity.setRepeatInterval(changes.getRepeatInterval());
                case "repeatUnit" -> entity.setRepeatUnit(Optional.ofNullable(changes.getRepeatUnit())
                        .map(v -> v.getValue()).orElse(null));
                case "url" -> entity.setUrl(changes.getUrl());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "ownerUserId" -> entity.setOwnerUserId(changes.getOwnerUserId());
                case "recurrenceRule" -> entity.setRecurrenceRule(changes.getRecurrenceRule());
                case "forAllMembers" -> entity.setForAllMembers(changes.getForAllMembers());
                case "staffCanEdit" -> entity.setStaffCanEdit(changes.getStaffCanEdit());
                case "importance" -> entity.setImportance(Optional.ofNullable(changes.getImportance())
                        .map(v -> v.getValue()).orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(EventEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getAudience() == null) {
            errors.add(new FieldError("audience", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCategory() == null) {
            errors.add(new FieldError("category", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsAllDay() == null) {
            errors.add(new FieldError("isAllDay", "must not be null", "NotNull"));
        }
        if (!creating && entity.getRepeatCount() == null) {
            errors.add(new FieldError("repeatCount", "must not be null", "NotNull"));
        }
        if (!creating && entity.getRepeatInterval() == null) {
            errors.add(new FieldError("repeatInterval", "must not be null", "NotNull"));
        }
        if (!creating && entity.getForAllMembers() == null) {
            errors.add(new FieldError("forAllMembers", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStaffCanEdit() == null) {
            errors.add(new FieldError("staffCanEdit", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(EventEntity entity) {

    }

    // ---- EventAttendee: a record of the Event aggregate ----

    public EventAttendee getEventAttendee(UUID eventId, UUID id) {
        return doInReadTransaction(() -> {
            return eventAttendeeMapper.toDto(loadVisibleEventAttendee(DataScope.current(EVENT_ATTENDEE_PERMISSION_GET),
                    eventId, id));
        });
    }

    public Page<EventAttendee> listEventAttendees(UUID eventId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, EventAttendeeRepository.SORT_FIELDS, EventAttendeeRepository.DEFAULT_ORDER,
                EventAttendeeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EventAttendeeRepository.FILTER_FIELDS)
                .and(EventAttendeeRepository.ofParent(eventId))
                        .and(EventAttendeeRepository.scope(DataScope.current(EVENT_ATTENDEE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + eventId, clock);
        return doInReadTransaction(() -> {
            Page<EventAttendeeEntity> rows = eventAttendeeRepository.list(page, where);
            return rows.map(eventAttendeeMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EventAttendee createEventAttendee(UUID eventId, EventAttendee request, boolean validateOnly) {
        validator.validate(request);
        EventAttendeeEntity entity = eventAttendeeMapper.toEntity(request);
        entity.setEventId(eventId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EVENT_ATTENDEE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!eventAttendeeRepository.existsEventId(entity.getEventId())) {
                throw new ResourceNotFoundException("comms.event", entity.getEventId());
            }
            if (!scope.canRead(institutionOfEventAttendee(entity))) {
                throw new ResourceNotFoundException("comms.event", entity.getEventId());
            }
            assertValidEventAttendee(entity, true);
            scope.requireWritable(institutionOfEventAttendee(entity));
            assertUniqueEventAttendee(entity);
            if (validateOnly) {
                return eventAttendeeMapper.toDto(entity);
            }
            EventAttendeeEntity stored = eventAttendeeRepository.insert(entity);
            audit.recordCreate(EVENT_ATTENDEE_AGGREGATE, stored.getId());
            outbox.publish(EVENT_ATTENDEE_AGGREGATE + CommsEvents.CREATED, EVENT_ATTENDEE_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return eventAttendeeMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EventAttendee updateEventAttendee(UUID eventId, UUID id, String ifMatch, EventAttendeeUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EVENT_ATTENDEE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVENT_ATTENDEE_PERMISSION_UPDATE);
            EventAttendeeEntity entity = loadVisibleEventAttendee(scope, eventId, id);
            scope.requireWritable(institutionOfEventAttendee(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyEventAttendee(entity, changes, fields);
            validator.validate(eventAttendeeMapper.toDto(entity));
            assertValidEventAttendee(entity, false);
            scope.requireWritable(institutionOfEventAttendee(entity));
            assertUniqueEventAttendee(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return eventAttendeeMapper.toDto(entity);
            }
            if (eventAttendeeRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EVENT_ATTENDEE_AGGREGATE, id, fields);
            outbox.publish(EVENT_ATTENDEE_AGGREGATE + CommsEvents.UPDATED, EVENT_ATTENDEE_AGGREGATE, id, CommsEvents.payload(id));
            return eventAttendeeMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEventAttendee(UUID eventId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVENT_ATTENDEE_PERMISSION_DELETE);
            EventAttendeeEntity entity = loadVisibleEventAttendee(scope, eventId, id);
            scope.requireWritable(institutionOfEventAttendee(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (eventAttendeeRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EVENT_ATTENDEE_AGGREGATE, id);
            outbox.publish(EVENT_ATTENDEE_AGGREGATE + CommsEvents.DELETED, EVENT_ATTENDEE_AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EventAttendeeEntity loadVisibleEventAttendee(DataScope scope, UUID eventId, UUID id) {
        EventAttendeeEntity entity = eventAttendeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_ATTENDEE_AGGREGATE, id));
        if (!scope.canRead(institutionOfEventAttendee(entity)) || !eventId.equals(entity.getEventId())) {
            throw new ResourceNotFoundException(EVENT_ATTENDEE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the comms.event it belongs to). */
    private Optional<UUID> institutionOfEventAttendee(EventAttendeeEntity entity) {
        return eventAttendeeRepository.institutionOf(entity.getEventId());
    }

    /**
     * Applies the selected EventAttendeeUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyEventAttendee(EventAttendeeEntity entity, EventAttendeeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidEventAttendee(EventAttendeeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getEventId() == null) {
            errors.add(new FieldError("eventId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getEventId() != null && !eventAttendeeRepository.existsEventId(entity.getEventId())) {
            errors.add(new FieldError("eventId", "event does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueEventAttendee(EventAttendeeEntity entity) {
        if (eventAttendeeRepository.takenEventIdUserId1(entity)) {
            throw new DuplicateResourceException("A event attendee with the same event id and user id already exists.");
        }
    }

    // ---- EventGalleryItem: a record of the Event aggregate ----

    public EventGalleryItem getEventGalleryItem(UUID eventId, UUID id) {
        return doInReadTransaction(() -> {
            return eventGalleryItemMapper.toDto(loadVisibleEventGalleryItem(DataScope.current(
                    EVENT_GALLERY_ITEM_PERMISSION_GET), eventId, id));
        });
    }

    public Page<EventGalleryItem> listEventGalleryItems(UUID eventId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, EventGalleryItemRepository.SORT_FIELDS,
                        EventGalleryItemRepository.DEFAULT_ORDER,
                EventGalleryItemRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EventGalleryItemRepository.FILTER_FIELDS)
                .and(EventGalleryItemRepository.ofParent(eventId))
                        .and(EventGalleryItemRepository.scope(DataScope.current(EVENT_GALLERY_ITEM_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + eventId, clock);
        return doInReadTransaction(() -> {
            Page<EventGalleryItemEntity> rows = eventGalleryItemRepository.list(page, where);
            return rows.map(eventGalleryItemMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EventGalleryItem createEventGalleryItem(UUID eventId, EventGalleryItem request, boolean validateOnly) {
        validator.validate(request);
        EventGalleryItemEntity entity = eventGalleryItemMapper.toEntity(request);
        entity.setEventId(eventId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EVENT_GALLERY_ITEM_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!eventGalleryItemRepository.existsEventId(entity.getEventId())) {
                throw new ResourceNotFoundException("comms.event", entity.getEventId());
            }
            if (!scope.canRead(institutionOfEventGalleryItem(entity))) {
                throw new ResourceNotFoundException("comms.event", entity.getEventId());
            }
            assertValidEventGalleryItem(entity, true);
            scope.requireWritable(institutionOfEventGalleryItem(entity));
            assertUniqueEventGalleryItem(entity);
            if (validateOnly) {
                return eventGalleryItemMapper.toDto(entity);
            }
            EventGalleryItemEntity stored = eventGalleryItemRepository.insert(entity);
            audit.recordCreate(EVENT_GALLERY_ITEM_AGGREGATE, stored.getId());
            outbox.publish(EVENT_GALLERY_ITEM_AGGREGATE + CommsEvents.CREATED, EVENT_GALLERY_ITEM_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return eventGalleryItemMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EventGalleryItem updateEventGalleryItem(UUID eventId, UUID id, String ifMatch,
            EventGalleryItemUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EVENT_GALLERY_ITEM_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVENT_GALLERY_ITEM_PERMISSION_UPDATE);
            EventGalleryItemEntity entity = loadVisibleEventGalleryItem(scope, eventId, id);
            scope.requireWritable(institutionOfEventGalleryItem(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyEventGalleryItem(entity, changes, fields);
            validator.validate(eventGalleryItemMapper.toDto(entity));
            assertValidEventGalleryItem(entity, false);
            scope.requireWritable(institutionOfEventGalleryItem(entity));
            assertUniqueEventGalleryItem(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return eventGalleryItemMapper.toDto(entity);
            }
            if (eventGalleryItemRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EVENT_GALLERY_ITEM_AGGREGATE, id, fields);
            outbox.publish(EVENT_GALLERY_ITEM_AGGREGATE + CommsEvents.UPDATED, EVENT_GALLERY_ITEM_AGGREGATE, id,
                    CommsEvents.payload(id));
            return eventGalleryItemMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEventGalleryItem(UUID eventId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVENT_GALLERY_ITEM_PERMISSION_DELETE);
            EventGalleryItemEntity entity = loadVisibleEventGalleryItem(scope, eventId, id);
            scope.requireWritable(institutionOfEventGalleryItem(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (eventGalleryItemRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EVENT_GALLERY_ITEM_AGGREGATE, id);
            outbox.publish(EVENT_GALLERY_ITEM_AGGREGATE + CommsEvents.DELETED, EVENT_GALLERY_ITEM_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EventGalleryItemEntity loadVisibleEventGalleryItem(DataScope scope, UUID eventId, UUID id) {
        EventGalleryItemEntity entity = eventGalleryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_GALLERY_ITEM_AGGREGATE, id));
        if (!scope.canRead(institutionOfEventGalleryItem(entity)) || !eventId.equals(entity.getEventId())) {
            throw new ResourceNotFoundException(EVENT_GALLERY_ITEM_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the comms.event it belongs to). */
    private Optional<UUID> institutionOfEventGalleryItem(EventGalleryItemEntity entity) {
        return eventGalleryItemRepository.institutionOf(entity.getEventId());
    }

    /**
     * Applies the selected EventGalleryItemUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyEventGalleryItem(EventGalleryItemEntity entity, EventGalleryItemUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "fileId" -> entity.setFileId(changes.getFileId());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidEventGalleryItem(EventGalleryItemEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getEventId() == null) {
            errors.add(new FieldError("eventId", "must not be null", "NotNull"));
        }
        if (entity.getFileId() == null) {
            errors.add(new FieldError("fileId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getEventId() != null && !eventGalleryItemRepository.existsEventId(entity.getEventId())) {
            errors.add(new FieldError("eventId", "event does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueEventGalleryItem(EventGalleryItemEntity entity) {

    }

    // ---- EventInstitution: a record of the Event aggregate ----

    public EventInstitution getEventInstitution(UUID eventId, UUID id) {
        return doInReadTransaction(() -> {
            return eventInstitutionMapper.toDto(loadVisibleEventInstitution(DataScope.current(
                    EVENT_INSTITUTION_PERMISSION_GET), eventId, id));
        });
    }

    public Page<EventInstitution> listEventInstitutions(UUID eventId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, EventInstitutionRepository.SORT_FIELDS,
                        EventInstitutionRepository.DEFAULT_ORDER,
                EventInstitutionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EventInstitutionRepository.FILTER_FIELDS)
                .and(EventInstitutionRepository.ofParent(eventId))
                        .and(EventInstitutionRepository.scope(DataScope.current(EVENT_INSTITUTION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + eventId, clock);
        return doInReadTransaction(() -> {
            Page<EventInstitutionEntity> rows = eventInstitutionRepository.list(page, where);
            return rows.map(eventInstitutionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EventInstitution createEventInstitution(UUID eventId, EventInstitution request, boolean validateOnly) {
        validator.validate(request);
        EventInstitutionEntity entity = eventInstitutionMapper.toEntity(request);
        entity.setEventId(eventId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EVENT_INSTITUTION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!eventInstitutionRepository.existsEventId(entity.getEventId())) {
                throw new ResourceNotFoundException("comms.event", entity.getEventId());
            }
            assertValidEventInstitution(entity, true);
            scope.requireWritable(institutionOfEventInstitution(entity));
            assertUniqueEventInstitution(entity);
            if (validateOnly) {
                return eventInstitutionMapper.toDto(entity);
            }
            EventInstitutionEntity stored = eventInstitutionRepository.insert(entity);
            audit.recordCreate(EVENT_INSTITUTION_AGGREGATE, stored.getId());
            outbox.publish(EVENT_INSTITUTION_AGGREGATE + CommsEvents.CREATED, EVENT_INSTITUTION_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return eventInstitutionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EventInstitution updateEventInstitution(UUID eventId, UUID id, String ifMatch,
            EventInstitutionUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EVENT_INSTITUTION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVENT_INSTITUTION_PERMISSION_UPDATE);
            EventInstitutionEntity entity = loadVisibleEventInstitution(scope, eventId, id);
            scope.requireWritable(institutionOfEventInstitution(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyEventInstitution(entity, changes, fields);
            validator.validate(eventInstitutionMapper.toDto(entity));
            assertValidEventInstitution(entity, false);
            scope.requireWritable(institutionOfEventInstitution(entity));
            assertUniqueEventInstitution(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return eventInstitutionMapper.toDto(entity);
            }
            if (eventInstitutionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EVENT_INSTITUTION_AGGREGATE, id, fields);
            outbox.publish(EVENT_INSTITUTION_AGGREGATE + CommsEvents.UPDATED, EVENT_INSTITUTION_AGGREGATE, id,
                    CommsEvents.payload(id));
            return eventInstitutionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEventInstitution(UUID eventId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVENT_INSTITUTION_PERMISSION_DELETE);
            EventInstitutionEntity entity = loadVisibleEventInstitution(scope, eventId, id);
            scope.requireWritable(institutionOfEventInstitution(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (eventInstitutionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EVENT_INSTITUTION_AGGREGATE, id);
            outbox.publish(EVENT_INSTITUTION_AGGREGATE + CommsEvents.DELETED, EVENT_INSTITUTION_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EventInstitutionEntity loadVisibleEventInstitution(DataScope scope, UUID eventId, UUID id) {
        EventInstitutionEntity entity = eventInstitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_INSTITUTION_AGGREGATE, id));
        if (!scope.canRead(institutionOfEventInstitution(entity)) || !eventId.equals(entity.getEventId())) {
            throw new ResourceNotFoundException(EVENT_INSTITUTION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOfEventInstitution(EventInstitutionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected EventInstitutionUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyEventInstitution(EventInstitutionEntity entity, EventInstitutionUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidEventInstitution(EventInstitutionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getEventId() == null) {
            errors.add(new FieldError("eventId", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getEventId() != null && !eventInstitutionRepository.existsEventId(entity.getEventId())) {
            errors.add(new FieldError("eventId", "event does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueEventInstitution(EventInstitutionEntity entity) {
        if (eventInstitutionRepository.takenEventIdInstitutionId1(entity)) {
            throw new DuplicateResourceException(
                    "A event institution with the same event id and institution id already exists.");
        }
    }

    // ---- EventOccurrence: a record of the Event aggregate ----

    public EventOccurrence getEventOccurrence(UUID eventId, UUID id) {
        return doInReadTransaction(() -> {
            return eventOccurrenceMapper.toDto(loadVisibleEventOccurrence(DataScope.current(EVENT_OCCURRENCE_PERMISSION_GET),
                    eventId, id));
        });
    }

    public Page<EventOccurrence> listEventOccurrences(UUID eventId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, EventOccurrenceRepository.SORT_FIELDS, EventOccurrenceRepository.DEFAULT_ORDER,
                EventOccurrenceRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EventOccurrenceRepository.FILTER_FIELDS)
                .and(EventOccurrenceRepository.ofParent(eventId))
                        .and(EventOccurrenceRepository.scope(DataScope.current(EVENT_OCCURRENCE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + eventId, clock);
        return doInReadTransaction(() -> {
            Page<EventOccurrenceEntity> rows = eventOccurrenceRepository.list(page, where);
            return rows.map(eventOccurrenceMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public EventOccurrence createEventOccurrence(UUID eventId, EventOccurrence request, boolean validateOnly) {
        validator.validate(request);
        EventOccurrenceEntity entity = eventOccurrenceMapper.toEntity(request);
        entity.setEventId(eventId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EVENT_OCCURRENCE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!eventOccurrenceRepository.existsEventId(entity.getEventId())) {
                throw new ResourceNotFoundException("comms.event", entity.getEventId());
            }
            if (!scope.canRead(institutionOfEventOccurrence(entity))) {
                throw new ResourceNotFoundException("comms.event", entity.getEventId());
            }
            assertValidEventOccurrence(entity, true);
            scope.requireWritable(institutionOfEventOccurrence(entity));
            assertUniqueEventOccurrence(entity);
            if (validateOnly) {
                return eventOccurrenceMapper.toDto(entity);
            }
            EventOccurrenceEntity stored = eventOccurrenceRepository.insert(entity);
            audit.recordCreate(EVENT_OCCURRENCE_AGGREGATE, stored.getId());
            outbox.publish(EVENT_OCCURRENCE_AGGREGATE + CommsEvents.CREATED, EVENT_OCCURRENCE_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return eventOccurrenceMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public EventOccurrence updateEventOccurrence(UUID eventId, UUID id, String ifMatch, EventOccurrenceUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EVENT_OCCURRENCE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVENT_OCCURRENCE_PERMISSION_UPDATE);
            EventOccurrenceEntity entity = loadVisibleEventOccurrence(scope, eventId, id);
            scope.requireWritable(institutionOfEventOccurrence(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyEventOccurrence(entity, changes, fields);
            validator.validate(eventOccurrenceMapper.toDto(entity));
            assertValidEventOccurrence(entity, false);
            scope.requireWritable(institutionOfEventOccurrence(entity));
            assertUniqueEventOccurrence(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return eventOccurrenceMapper.toDto(entity);
            }
            if (eventOccurrenceRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EVENT_OCCURRENCE_AGGREGATE, id, fields);
            outbox.publish(EVENT_OCCURRENCE_AGGREGATE + CommsEvents.UPDATED, EVENT_OCCURRENCE_AGGREGATE, id,
                    CommsEvents.payload(id));
            return eventOccurrenceMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEventOccurrence(UUID eventId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EVENT_OCCURRENCE_PERMISSION_DELETE);
            EventOccurrenceEntity entity = loadVisibleEventOccurrence(scope, eventId, id);
            scope.requireWritable(institutionOfEventOccurrence(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (eventOccurrenceRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EVENT_OCCURRENCE_AGGREGATE, id);
            outbox.publish(EVENT_OCCURRENCE_AGGREGATE + CommsEvents.DELETED, EVENT_OCCURRENCE_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /**
     * comms.CancelEventOccurrence: Cancel one occurrence. The row must be visible and writable for the caller, and the
     * step is allowed while is_cancelled is false; in any other state
     * the answer is 409 and nothing changes.
     */
    public EventOccurrence cancelEventOccurrence(UUID eventId, UUID id, CancelEventOccurrenceRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_EVENT_OCCURRENCE);
        return doInWriteTransaction(() -> {
            EventOccurrenceEntity entity = eventOccurrenceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(EVENT_OCCURRENCE_AGGREGATE, id));
            if (!scope.canRead(institutionOfEventOccurrence(entity)) || !eventId.equals(entity.getEventId())) {
                throw new ResourceNotFoundException(EVENT_OCCURRENCE_AGGREGATE, id);
            }
            scope.requireWritable(institutionOfEventOccurrence(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(!Boolean.TRUE.equals(entity.getIsCancelled()))) {
                throw new InvalidStateException(
                        "The event occurrence cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (eventOccurrenceRepository.cancelEventOccurrence(id, entity.getRowVersion(), now,
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(EVENT_OCCURRENCE_AGGREGATE, id, "cancel", List.of("isCancelled"), request == null ? null
                    : request.getReason());
            outbox.publish(EVENT_OCCURRENCE_AGGREGATE + ".cancel", EVENT_OCCURRENCE_AGGREGATE, id, CommsEvents.payload(id));
            return eventOccurrenceMapper.toDto(eventOccurrenceRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(EVENT_OCCURRENCE_AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EventOccurrenceEntity loadVisibleEventOccurrence(DataScope scope, UUID eventId, UUID id) {
        EventOccurrenceEntity entity = eventOccurrenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EVENT_OCCURRENCE_AGGREGATE, id));
        if (!scope.canRead(institutionOfEventOccurrence(entity)) || !eventId.equals(entity.getEventId())) {
            throw new ResourceNotFoundException(EVENT_OCCURRENCE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the comms.event it belongs to). */
    private Optional<UUID> institutionOfEventOccurrence(EventOccurrenceEntity entity) {
        return eventOccurrenceRepository.institutionOf(entity.getEventId());
    }

    /**
     * Applies the selected EventOccurrenceUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyEventOccurrence(EventOccurrenceEntity entity, EventOccurrenceUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "color" -> entity.setColor(changes.getColor());
                case "description" -> entity.setDescription(changes.getDescription());
                case "startAt" -> entity.setStartAt(changes.getStartAt());
                case "endAt" -> entity.setEndAt(changes.getEndAt());
                case "isCancelled" -> entity.setIsCancelled(changes.getIsCancelled());
                case "responseStatus" -> entity.setResponseStatus(Optional.ofNullable(changes.getResponseStatus())
                        .map(v -> v.getValue()).orElse(null));
                case "importance" -> entity.setImportance(Optional.ofNullable(changes.getImportance())
                        .map(v -> v.getValue()).orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidEventOccurrence(EventOccurrenceEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getEventId() == null) {
            errors.add(new FieldError("eventId", "must not be null", "NotNull"));
        }
        if (entity.getStartAt() == null) {
            errors.add(new FieldError("startAt", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsCancelled() == null) {
            errors.add(new FieldError("isCancelled", "must not be null", "NotNull"));
        }
        if (entity.getEventId() != null && !eventOccurrenceRepository.existsEventId(entity.getEventId())) {
            errors.add(new FieldError("eventId", "event does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueEventOccurrence(EventOccurrenceEntity entity) {

    }
}
