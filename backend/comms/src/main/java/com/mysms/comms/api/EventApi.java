// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api;

import com.mysms.comms.api.model.CalendarItem;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.ActivateEventRequest;
import com.mysms.comms.api.model.CancelEventOccurrenceRequest;
import com.mysms.comms.api.model.CompletePersonalEventRequest;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Validated
public interface EventApi {

    String PATH_ACTIVATE_EVENT = "/v1/comms/events/{eventId}:activate";
    /**
     * POST /v1/comms/events/{eventId}:activate : Approve / show
     * Approve / show.
     *
     * @param eventId  (required)
     * @param activateEventRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_ACTIVATE_EVENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Event> activateEvent(
         @PathVariable("eventId") UUID eventId,
         @Valid @RequestBody ActivateEventRequest activateEventRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CANCEL_EVENT_OCCURRENCE = "/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}:cancel";
    /**
     * POST /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}:cancel : Cancel one occurrence
     * Cancel one occurrence.
     *
     * @param eventId  (required)
     * @param eventOccurrenceId  (required)
     * @param cancelEventOccurrenceRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_CANCEL_EVENT_OCCURRENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventOccurrence> cancelEventOccurrence(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventOccurrenceId") UUID eventOccurrenceId,
         @Valid @RequestBody CancelEventOccurrenceRequest cancelEventOccurrenceRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_COMPLETE_PERSONAL_EVENT = "/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}:complete";
    /**
     * POST /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}:complete : Mark a personal to-do event complete
     * Mark a personal to-do event complete.
     *
     * @param eventId  (required)
     * @param eventOccurrenceId  (required)
     * @param completePersonalEventRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_COMPLETE_PERSONAL_EVENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventOccurrence> completePersonalEvent(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventOccurrenceId") UUID eventOccurrenceId,
         @Valid @RequestBody CompletePersonalEventRequest completePersonalEventRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_EVENT = "/v1/comms/events";
    /**
     * POST /v1/comms/events : Create Event
     * Create a Event. Supports Idempotency-Key and validateOnly.
     *
     * @param event  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_CREATE_EVENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Event> createEvent(
         @Valid @RequestBody Event event,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EVENT_ATTENDEE = "/v1/comms/events/{eventId}/attendees";
    /**
     * POST /v1/comms/events/{eventId}/attendees : Create EventAttendee
     * Create a EventAttendee. Supports Idempotency-Key and validateOnly.
     *
     * @param eventId Event id. (required)
     * @param eventAttendee  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_CREATE_EVENT_ATTENDEE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventAttendee> createEventAttendee(
         @PathVariable("eventId") UUID eventId,
         @Valid @RequestBody EventAttendee eventAttendee,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EVENT_GALLERY_ITEM = "/v1/comms/events/{eventId}/gallery-items";
    /**
     * POST /v1/comms/events/{eventId}/gallery-items : Create EventGalleryItem
     * Create a EventGalleryItem. Supports Idempotency-Key and validateOnly.
     *
     * @param eventId Event id. (required)
     * @param eventGalleryItem  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_CREATE_EVENT_GALLERY_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventGalleryItem> createEventGalleryItem(
         @PathVariable("eventId") UUID eventId,
         @Valid @RequestBody EventGalleryItem eventGalleryItem,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EVENT_INSTITUTION = "/v1/comms/events/{eventId}/institutions";
    /**
     * POST /v1/comms/events/{eventId}/institutions : Create EventInstitution
     * Create a EventInstitution. Supports Idempotency-Key and validateOnly.
     *
     * @param eventId Event id. (required)
     * @param eventInstitution  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_CREATE_EVENT_INSTITUTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventInstitution> createEventInstitution(
         @PathVariable("eventId") UUID eventId,
         @Valid @RequestBody EventInstitution eventInstitution,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EVENT_OCCURRENCE = "/v1/comms/events/{eventId}/occurrences";
    /**
     * POST /v1/comms/events/{eventId}/occurrences : Create EventOccurrence
     * Create a EventOccurrence. Supports Idempotency-Key and validateOnly.
     *
     * @param eventId Event id. (required)
     * @param eventOccurrence  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_CREATE_EVENT_OCCURRENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventOccurrence> createEventOccurrence(
         @PathVariable("eventId") UUID eventId,
         @Valid @RequestBody EventOccurrence eventOccurrence,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DEACTIVATE_EVENT = "/v1/comms/events/{eventId}:deactivate";
    /**
     * POST /v1/comms/events/{eventId}:deactivate : Hide
     * Hide.
     *
     * @param eventId  (required)
     * @param deactivateEventRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = EventApi.PATH_DEACTIVATE_EVENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Event> deactivateEvent(
         @PathVariable("eventId") UUID eventId,
         @Valid @RequestBody DeactivateEventRequest deactivateEventRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DELETE_EVENT = "/v1/comms/events/{eventId}";
    /**
     * DELETE /v1/comms/events/{eventId} : Delete Event
     * Soft delete (sets deleted_at).
     *
     * @param eventId Event id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = EventApi.PATH_DELETE_EVENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEvent(
         @PathVariable("eventId") UUID eventId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EVENT_ATTENDEE = "/v1/comms/events/{eventId}/attendees/{eventAttendeeId}";
    /**
     * DELETE /v1/comms/events/{eventId}/attendees/{eventAttendeeId} : Delete EventAttendee
     * Soft delete (sets deleted_at).
     *
     * @param eventId Event id. (required)
     * @param eventAttendeeId EventAttendee id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = EventApi.PATH_DELETE_EVENT_ATTENDEE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEventAttendee(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventAttendeeId") UUID eventAttendeeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EVENT_GALLERY_ITEM = "/v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}";
    /**
     * DELETE /v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId} : Delete EventGalleryItem
     * Soft delete (sets deleted_at).
     *
     * @param eventId Event id. (required)
     * @param eventGalleryItemId EventGalleryItem id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = EventApi.PATH_DELETE_EVENT_GALLERY_ITEM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEventGalleryItem(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventGalleryItemId") UUID eventGalleryItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EVENT_INSTITUTION = "/v1/comms/events/{eventId}/institutions/{eventInstitutionId}";
    /**
     * DELETE /v1/comms/events/{eventId}/institutions/{eventInstitutionId} : Delete EventInstitution
     * Soft delete (sets deleted_at).
     *
     * @param eventId Event id. (required)
     * @param eventInstitutionId EventInstitution id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = EventApi.PATH_DELETE_EVENT_INSTITUTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEventInstitution(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventInstitutionId") UUID eventInstitutionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EVENT_OCCURRENCE = "/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}";
    /**
     * DELETE /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId} : Delete EventOccurrence
     * Soft delete (sets deleted_at).
     *
     * @param eventId Event id. (required)
     * @param eventOccurrenceId EventOccurrence id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = EventApi.PATH_DELETE_EVENT_OCCURRENCE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEventOccurrence(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventOccurrenceId") UUID eventOccurrenceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_EVENT = "/v1/comms/events/{eventId}";
    /**
     * GET /v1/comms/events/{eventId} : Get Event
     * Get one Event by id. The ETag header carries the row version.
     *
     * @param eventId Event id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_GET_EVENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Event> getEvent(
         @PathVariable("eventId") UUID eventId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EVENT_ATTENDEE = "/v1/comms/events/{eventId}/attendees/{eventAttendeeId}";
    /**
     * GET /v1/comms/events/{eventId}/attendees/{eventAttendeeId} : Get EventAttendee
     * Get one EventAttendee by id. The ETag header carries the row version.
     *
     * @param eventId Event id. (required)
     * @param eventAttendeeId EventAttendee id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_GET_EVENT_ATTENDEE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EventAttendee> getEventAttendee(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventAttendeeId") UUID eventAttendeeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EVENT_GALLERY_ITEM = "/v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}";
    /**
     * GET /v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId} : Get EventGalleryItem
     * Get one EventGalleryItem by id. The ETag header carries the row version.
     *
     * @param eventId Event id. (required)
     * @param eventGalleryItemId EventGalleryItem id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_GET_EVENT_GALLERY_ITEM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EventGalleryItem> getEventGalleryItem(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventGalleryItemId") UUID eventGalleryItemId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EVENT_INSTITUTION = "/v1/comms/events/{eventId}/institutions/{eventInstitutionId}";
    /**
     * GET /v1/comms/events/{eventId}/institutions/{eventInstitutionId} : Get EventInstitution
     * Get one EventInstitution by id. The ETag header carries the row version.
     *
     * @param eventId Event id. (required)
     * @param eventInstitutionId EventInstitution id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_GET_EVENT_INSTITUTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EventInstitution> getEventInstitution(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventInstitutionId") UUID eventInstitutionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EVENT_OCCURRENCE = "/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}";
    /**
     * GET /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId} : Get EventOccurrence
     * Get one EventOccurrence by id. The ETag header carries the row version.
     *
     * @param eventId Event id. (required)
     * @param eventOccurrenceId EventOccurrence id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_GET_EVENT_OCCURRENCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EventOccurrence> getEventOccurrence(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventOccurrenceId") UUID eventOccurrenceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_MY_CALENDAR = "/v1/comms/me/calendar";
    /**
     * GET /v1/comms/me/calendar : Merged calendar of the caller: institution and cohort events, personal events, item due dates, live classes, cohort sessions and holidays
     * Merged calendar of the caller: institution and cohort events, personal events, item due dates, live classes, cohort sessions and holidays.
     *
     * @param from Window start. (required)
     * @param to Window end. (required)
     * @param cohortId Only one cohort. (optional)
     * @param itemTypes Filter kinds. (optional)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_GET_MY_CALENDAR,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CalendarItem>> getMyCalendar(
        @NotNull  @Valid @RequestParam(value = "from", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @NotNull  @Valid @RequestParam(value = "to", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
         @Valid @RequestParam(value = "cohortId", required = false) @Nullable UUID cohortId,
         @RequestParam(value = "itemTypes", required = false) @Nullable List<String> itemTypes,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_LIST_EVENT_ATTENDEES = "/v1/comms/events/{eventId}/attendees";
    /**
     * GET /v1/comms/events/{eventId}/attendees : List EventAttendees
     * List EventAttendees visible to the caller, page by page, with filtering and sorting.
     *
     * @param eventId Event id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_LIST_EVENT_ATTENDEES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EventAttendee>> listEventAttendees(
         @PathVariable("eventId") UUID eventId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EVENT_GALLERY_ITEMS = "/v1/comms/events/{eventId}/gallery-items";
    /**
     * GET /v1/comms/events/{eventId}/gallery-items : List EventGalleryItems
     * List EventGalleryItems visible to the caller, page by page, with filtering and sorting.
     *
     * @param eventId Event id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_LIST_EVENT_GALLERY_ITEMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EventGalleryItem>> listEventGalleryItems(
         @PathVariable("eventId") UUID eventId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EVENT_INSTITUTIONS = "/v1/comms/events/{eventId}/institutions";
    /**
     * GET /v1/comms/events/{eventId}/institutions : List EventInstitutions
     * List EventInstitutions visible to the caller, page by page, with filtering and sorting.
     *
     * @param eventId Event id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_LIST_EVENT_INSTITUTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EventInstitution>> listEventInstitutions(
         @PathVariable("eventId") UUID eventId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EVENT_OCCURRENCES = "/v1/comms/events/{eventId}/occurrences";
    /**
     * GET /v1/comms/events/{eventId}/occurrences : List EventOccurrences
     * List EventOccurrences visible to the caller, page by page, with filtering and sorting.
     *
     * @param eventId Event id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_LIST_EVENT_OCCURRENCES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EventOccurrence>> listEventOccurrences(
         @PathVariable("eventId") UUID eventId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EVENTS = "/v1/comms/events";
    /**
     * GET /v1/comms/events : List Events
     * List Events visible to the caller, page by page, with filtering and sorting.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = EventApi.PATH_LIST_EVENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Event>> listEvents(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_EVENT = "/v1/comms/events/{eventId}";
    /**
     * PATCH /v1/comms/events/{eventId} : Update Event
     * Partially update a Event (updateMask). Requires If-Match with the current ETag.
     *
     * @param eventId Event id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param eventUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = EventApi.PATH_UPDATE_EVENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Event> updateEvent(
         @PathVariable("eventId") UUID eventId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EventUpdate eventUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EVENT_ATTENDEE = "/v1/comms/events/{eventId}/attendees/{eventAttendeeId}";
    /**
     * PATCH /v1/comms/events/{eventId}/attendees/{eventAttendeeId} : Update EventAttendee
     * Partially update a EventAttendee (updateMask). Requires If-Match with the current ETag.
     *
     * @param eventId Event id. (required)
     * @param eventAttendeeId EventAttendee id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param eventAttendeeUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = EventApi.PATH_UPDATE_EVENT_ATTENDEE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventAttendee> updateEventAttendee(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventAttendeeId") UUID eventAttendeeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EventAttendeeUpdate eventAttendeeUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EVENT_GALLERY_ITEM = "/v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId}";
    /**
     * PATCH /v1/comms/events/{eventId}/gallery-items/{eventGalleryItemId} : Update EventGalleryItem
     * Partially update a EventGalleryItem (updateMask). Requires If-Match with the current ETag.
     *
     * @param eventId Event id. (required)
     * @param eventGalleryItemId EventGalleryItem id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param eventGalleryItemUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = EventApi.PATH_UPDATE_EVENT_GALLERY_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventGalleryItem> updateEventGalleryItem(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventGalleryItemId") UUID eventGalleryItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EventGalleryItemUpdate eventGalleryItemUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EVENT_INSTITUTION = "/v1/comms/events/{eventId}/institutions/{eventInstitutionId}";
    /**
     * PATCH /v1/comms/events/{eventId}/institutions/{eventInstitutionId} : Update EventInstitution
     * Partially update a EventInstitution (updateMask). Requires If-Match with the current ETag.
     *
     * @param eventId Event id. (required)
     * @param eventInstitutionId EventInstitution id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param eventInstitutionUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = EventApi.PATH_UPDATE_EVENT_INSTITUTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventInstitution> updateEventInstitution(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventInstitutionId") UUID eventInstitutionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EventInstitutionUpdate eventInstitutionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EVENT_OCCURRENCE = "/v1/comms/events/{eventId}/occurrences/{eventOccurrenceId}";
    /**
     * PATCH /v1/comms/events/{eventId}/occurrences/{eventOccurrenceId} : Update EventOccurrence
     * Partially update a EventOccurrence (updateMask). Requires If-Match with the current ETag.
     *
     * @param eventId Event id. (required)
     * @param eventOccurrenceId EventOccurrence id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param eventOccurrenceUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = EventApi.PATH_UPDATE_EVENT_OCCURRENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EventOccurrence> updateEventOccurrence(
         @PathVariable("eventId") UUID eventId,
         @PathVariable("eventOccurrenceId") UUID eventOccurrenceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EventOccurrenceUpdate eventOccurrenceUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
