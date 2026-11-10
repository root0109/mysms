package com.mysms.comms.controller;

import com.mysms.comms.api.model.CalendarItem;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.EventService;
import com.mysms.comms.api.EventApi;
import com.mysms.comms.api.model.ActivateEventRequest;
import com.mysms.comms.api.model.CancelEventOccurrenceRequest;
import com.mysms.comms.api.model.CompletePersonalEventRequest;
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
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Event operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/comms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class EventController implements EventApi {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public ResponseEntity<Event> activateEvent(UUID eventId, ActivateEventRequest activateEventRequest,
            UUID idempotencyKey) {
        Event changed = eventService.activateEvent(eventId, activateEventRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<EventOccurrence> cancelEventOccurrence(UUID eventId, UUID eventOccurrenceId,
            CancelEventOccurrenceRequest cancelEventOccurrenceRequest, UUID idempotencyKey) {
        EventOccurrence changed =
                eventService.cancelEventOccurrence(eventId, eventOccurrenceId, cancelEventOccurrenceRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<EventOccurrence> completePersonalEvent(UUID eventId, UUID eventOccurrenceId,
            CompletePersonalEventRequest completePersonalEventRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Event> createEvent(Event event, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Event created = eventService.createEvent(event, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/events/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<EventAttendee> createEventAttendee(UUID eventId, EventAttendee eventAttendee,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EventAttendee created = eventService.createEventAttendee(eventId, eventAttendee, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/events/" + eventId + "/attendees/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<EventGalleryItem> createEventGalleryItem(UUID eventId,
            EventGalleryItem eventGalleryItem, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EventGalleryItem created = eventService.createEventGalleryItem(eventId, eventGalleryItem, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/events/" + eventId + "/gallery-items/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<EventInstitution> createEventInstitution(UUID eventId,
            EventInstitution eventInstitution, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EventInstitution created = eventService.createEventInstitution(eventId, eventInstitution, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/events/" + eventId + "/institutions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<EventOccurrence> createEventOccurrence(UUID eventId,
            EventOccurrence eventOccurrence, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EventOccurrence created = eventService.createEventOccurrence(eventId, eventOccurrence, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/events/" + eventId + "/occurrences/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Event> deactivateEvent(UUID eventId,
            DeactivateEventRequest deactivateEventRequest, UUID idempotencyKey) {
        Event changed = eventService.deactivateEvent(eventId, deactivateEventRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> deleteEvent(UUID eventId, String ifMatch) {
        eventService.deleteEvent(eventId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEventAttendee(UUID eventId, UUID eventAttendeeId, String ifMatch) {
        eventService.deleteEventAttendee(eventId, eventAttendeeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEventGalleryItem(UUID eventId, UUID eventGalleryItemId,
            String ifMatch) {
        eventService.deleteEventGalleryItem(eventId, eventGalleryItemId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEventInstitution(UUID eventId, UUID eventInstitutionId,
            String ifMatch) {
        eventService.deleteEventInstitution(eventId, eventInstitutionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteEventOccurrence(UUID eventId, UUID eventOccurrenceId,
            String ifMatch) {
        eventService.deleteEventOccurrence(eventId, eventOccurrenceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Event> getEvent(UUID eventId, String fields) {
        Event found = eventService.getEvent(eventId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<EventAttendee> getEventAttendee(UUID eventId, UUID eventAttendeeId,
            String fields) {
        EventAttendee found = eventService.getEventAttendee(eventId, eventAttendeeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<EventGalleryItem> getEventGalleryItem(UUID eventId, UUID eventGalleryItemId,
            String fields) {
        EventGalleryItem found = eventService.getEventGalleryItem(eventId, eventGalleryItemId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<EventInstitution> getEventInstitution(UUID eventId, UUID eventInstitutionId,
            String fields) {
        EventInstitution found = eventService.getEventInstitution(eventId, eventInstitutionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<EventOccurrence> getEventOccurrence(UUID eventId, UUID eventOccurrenceId,
            String fields) {
        EventOccurrence found = eventService.getEventOccurrence(eventId, eventOccurrenceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CalendarItem>> getMyCalendar(OffsetDateTime from, OffsetDateTime to, UUID cohortId,
            List<String> itemTypes, Integer page, Integer size, String cursor) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<EventAttendee>> listEventAttendees(UUID eventId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<EventAttendee> list =
                PagingUtil.pagingInfo(eventService.listEventAttendees(eventId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<EventGalleryItem>> listEventGalleryItems(UUID eventId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<EventGalleryItem> list =
                PagingUtil.pagingInfo(eventService.listEventGalleryItems(eventId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<EventInstitution>> listEventInstitutions(UUID eventId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<EventInstitution> list =
                PagingUtil.pagingInfo(eventService.listEventInstitutions(eventId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<EventOccurrence>> listEventOccurrences(UUID eventId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<EventOccurrence> list =
                PagingUtil.pagingInfo(eventService.listEventOccurrences(eventId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Event>> listEvents(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Event> list = PagingUtil.pagingInfo(eventService.listEvents(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Event> updateEvent(UUID eventId, String ifMatch, EventUpdate eventUpdate,
            String updateMask, Boolean validateOnly) {
        Event updated = eventService.updateEvent(eventId, ifMatch, eventUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EventAttendee> updateEventAttendee(UUID eventId, UUID eventAttendeeId,
            String ifMatch, EventAttendeeUpdate eventAttendeeUpdate, String updateMask, Boolean validateOnly) {
        EventAttendee updated =
                eventService.updateEventAttendee(eventId, eventAttendeeId, ifMatch, eventAttendeeUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EventGalleryItem> updateEventGalleryItem(UUID eventId, UUID eventGalleryItemId,
            String ifMatch, EventGalleryItemUpdate eventGalleryItemUpdate, String updateMask, Boolean validateOnly) {
        EventGalleryItem updated =
                eventService.updateEventGalleryItem(eventId, eventGalleryItemId, ifMatch,
                        eventGalleryItemUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EventInstitution> updateEventInstitution(UUID eventId, UUID eventInstitutionId,
            String ifMatch, EventInstitutionUpdate eventInstitutionUpdate, String updateMask, Boolean validateOnly) {
        EventInstitution updated =
                eventService.updateEventInstitution(eventId, eventInstitutionId, ifMatch,
                        eventInstitutionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<EventOccurrence> updateEventOccurrence(UUID eventId, UUID eventOccurrenceId,
            String ifMatch, EventOccurrenceUpdate eventOccurrenceUpdate, String updateMask, Boolean validateOnly) {
        EventOccurrence updated =
                eventService.updateEventOccurrence(eventId, eventOccurrenceId, ifMatch, eventOccurrenceUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
