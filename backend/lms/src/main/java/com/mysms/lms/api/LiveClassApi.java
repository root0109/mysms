// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ExternalLink;
import com.mysms.lms.api.model.LiveClass;
import com.mysms.lms.api.model.LiveClassParticipant;
import com.mysms.lms.api.model.LiveClassParticipantList;
import com.mysms.lms.api.model.LiveClassUpdate;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.ScheduleLiveClassRequest;
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
public interface LiveClassApi {

    String PATH_CREATE_LIVE_CLASS = "/v1/lms/live-classes";
    /**
     * POST /v1/lms/live-classes : Create LiveClass
     * Create a LiveClass. Supports Idempotency-Key and validateOnly.
     *
     * @param liveClass  (required)
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
        value = LiveClassApi.PATH_CREATE_LIVE_CLASS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LiveClass> createLiveClass(
         @Valid @RequestBody LiveClass liveClass,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_LIVE_CLASS = "/v1/lms/live-classes/{liveClassId}";
    /**
     * DELETE /v1/lms/live-classes/{liveClassId} : Delete LiveClass
     * Soft delete (sets deleted_at).
     *
     * @param liveClassId LiveClass id. (required)
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
        value = LiveClassApi.PATH_DELETE_LIVE_CLASS,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLiveClass(
         @PathVariable("liveClassId") UUID liveClassId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_END_LIVE_CLASS = "/v1/lms/live-classes/{liveClassId}:end";
    /**
     * POST /v1/lms/live-classes/{liveClassId}:end : End the meeting at the provider for everyone
     * End the meeting at the provider for everyone.
     *
     * @param liveClassId  (required)
     * @param body  (required)
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
        value = LiveClassApi.PATH_END_LIVE_CLASS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LiveClass> endLiveClass(
         @PathVariable("liveClassId") UUID liveClassId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_LIVE_CLASS = "/v1/lms/live-classes/{liveClassId}";
    /**
     * GET /v1/lms/live-classes/{liveClassId} : Get LiveClass
     * Get one LiveClass by id. The ETag header carries the row version.
     *
     * @param liveClassId LiveClass id. (required)
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
        value = LiveClassApi.PATH_GET_LIVE_CLASS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LiveClass> getLiveClass(
         @PathVariable("liveClassId") UUID liveClassId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_LIVE_CLASS_PARTICIPANT = "/v1/lms/live-classes/{liveClassId}/participants/{liveClassParticipantId}";
    /**
     * GET /v1/lms/live-classes/{liveClassId}/participants/{liveClassParticipantId} : Get LiveClassParticipant
     * Get one LiveClassParticipant by id. The ETag header carries the row version.
     *
     * @param liveClassId LiveClass id. (required)
     * @param liveClassParticipantId LiveClassParticipant id. (required)
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
        value = LiveClassApi.PATH_GET_LIVE_CLASS_PARTICIPANT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LiveClassParticipant> getLiveClassParticipant(
         @PathVariable("liveClassId") UUID liveClassId,
         @PathVariable("liveClassParticipantId") UUID liveClassParticipantId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_LIVE_CLASS_START_URL = "/v1/lms/live-classes/{liveClassId}:startUrl";
    /**
     * GET /v1/lms/live-classes/{liveClassId}:startUrl : Fresh host start URL
     * Fresh host start URL.
     *
     * @param liveClassId  (required)
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
        value = LiveClassApi.PATH_GET_LIVE_CLASS_START_URL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalLink> getLiveClassStartUrl(
         @PathVariable("liveClassId") UUID liveClassId
    );


    String PATH_JOIN_LIVE_CLASS = "/v1/lms/live-classes/{liveClassId}:join";
    /**
     * POST /v1/lms/live-classes/{liveClassId}:join : Participant join URL (records intent)
     * Participant join URL (records intent).
     *
     * @param liveClassId  (required)
     * @param body  (required)
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
        value = LiveClassApi.PATH_JOIN_LIVE_CLASS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalLink> joinLiveClass(
         @PathVariable("liveClassId") UUID liveClassId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_LIVE_CLASS_PARTICIPANTS = "/v1/lms/live-classes/{liveClassId}/participants";
    /**
     * GET /v1/lms/live-classes/{liveClassId}/participants : List LiveClassParticipants
     * List LiveClassParticipants visible to the caller, page by page, with filtering and sorting.
     *
     * @param liveClassId LiveClass id. (required)
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
        value = LiveClassApi.PATH_LIST_LIVE_CLASS_PARTICIPANTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LiveClassParticipant>> listLiveClassParticipants(
         @PathVariable("liveClassId") UUID liveClassId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LIVE_CLASSES = "/v1/lms/live-classes";
    /**
     * GET /v1/lms/live-classes : List LiveClasses
     * List LiveClasses visible to the caller, page by page, with filtering and sorting.
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
        value = LiveClassApi.PATH_LIST_LIVE_CLASSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LiveClass>> listLiveClasses(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RETRY_LIVE_CLASS_INVITES = "/v1/lms/live-classes/{liveClassId}:retryInvites";
    /**
     * POST /v1/lms/live-classes/{liveClassId}:retryInvites : Resend calendar invites
     * Resend calendar invites.
     *
     * @param liveClassId  (required)
     * @param body  (required)
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
        value = LiveClassApi.PATH_RETRY_LIVE_CLASS_INVITES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LiveClass> retryLiveClassInvites(
         @PathVariable("liveClassId") UUID liveClassId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SCHEDULE_LIVE_CLASS = "/v1/lms/live-classes:schedule";
    /**
     * POST /v1/lms/live-classes:schedule : Create the meeting with the provider, the calendar event and invites
     * Create the meeting with the provider, the calendar event and invites.
     *
     * @param scheduleLiveClassRequest  (required)
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
        value = LiveClassApi.PATH_SCHEDULE_LIVE_CLASS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LiveClass> scheduleLiveClass(
         @Valid @RequestBody ScheduleLiveClassRequest scheduleLiveClassRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SYNC_LIVE_CLASS_ATTENDANCE = "/v1/lms/live-classes/{liveClassId}:syncAttendance";
    /**
     * POST /v1/lms/live-classes/{liveClassId}:syncAttendance : Fetch attendance (join / leave times) from the provider
     * Fetch attendance (join / leave times) from the provider.
     *
     * @param liveClassId  (required)
     * @param body  (required)
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
        value = LiveClassApi.PATH_SYNC_LIVE_CLASS_ATTENDANCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LiveClassParticipantList> syncLiveClassAttendance(
         @PathVariable("liveClassId") UUID liveClassId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_LIVE_CLASS = "/v1/lms/live-classes/{liveClassId}";
    /**
     * PATCH /v1/lms/live-classes/{liveClassId} : Update LiveClass
     * Partially update a LiveClass (updateMask). Requires If-Match with the current ETag.
     *
     * @param liveClassId LiveClass id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param liveClassUpdate  (required)
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
        value = LiveClassApi.PATH_UPDATE_LIVE_CLASS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LiveClass> updateLiveClass(
         @PathVariable("liveClassId") UUID liveClassId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LiveClassUpdate liveClassUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
