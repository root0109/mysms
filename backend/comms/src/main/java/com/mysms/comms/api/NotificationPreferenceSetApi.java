// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.NotificationPreference;
import com.mysms.comms.api.model.NotificationPreferenceSet;
import com.mysms.comms.api.model.NotificationPreferenceSetUpdate;
import com.mysms.comms.api.model.NotificationPreferenceUpdate;
import org.springframework.lang.Nullable;
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
public interface NotificationPreferenceSetApi {

    String PATH_CREATE_NOTIFICATION_PREFERENCE = "/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences";
    /**
     * POST /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences : Create NotificationPreference
     * Create a NotificationPreference. Supports Idempotency-Key and validateOnly.
     *
     * @param notificationPreferenceSetId NotificationPreferenceSet id. (required)
     * @param notificationPreference  (required)
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
        value = NotificationPreferenceSetApi.PATH_CREATE_NOTIFICATION_PREFERENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationPreference> createNotificationPreference(
         @PathVariable("notificationPreferenceSetId") UUID notificationPreferenceSetId,
         @Valid @RequestBody NotificationPreference notificationPreference,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_NOTIFICATION_PREFERENCE_SET = "/v1/comms/notification-preference-sets";
    /**
     * POST /v1/comms/notification-preference-sets : Create NotificationPreferenceSet
     * Create a NotificationPreferenceSet. Supports Idempotency-Key and validateOnly.
     *
     * @param notificationPreferenceSet  (required)
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
        value = NotificationPreferenceSetApi.PATH_CREATE_NOTIFICATION_PREFERENCE_SET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationPreferenceSet> createNotificationPreferenceSet(
         @Valid @RequestBody NotificationPreferenceSet notificationPreferenceSet,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_NOTIFICATION_PREFERENCE = "/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}";
    /**
     * DELETE /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId} : Delete NotificationPreference
     * Soft delete (sets deleted_at).
     *
     * @param notificationPreferenceSetId NotificationPreferenceSet id. (required)
     * @param notificationPreferenceId NotificationPreference id. (required)
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
        value = NotificationPreferenceSetApi.PATH_DELETE_NOTIFICATION_PREFERENCE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotificationPreference(
         @PathVariable("notificationPreferenceSetId") UUID notificationPreferenceSetId,
         @PathVariable("notificationPreferenceId") UUID notificationPreferenceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_NOTIFICATION_PREFERENCE_SET = "/v1/comms/notification-preference-sets/{notificationPreferenceSetId}";
    /**
     * DELETE /v1/comms/notification-preference-sets/{notificationPreferenceSetId} : Delete NotificationPreferenceSet
     * Soft delete (sets deleted_at).
     *
     * @param notificationPreferenceSetId NotificationPreferenceSet id. (required)
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
        value = NotificationPreferenceSetApi.PATH_DELETE_NOTIFICATION_PREFERENCE_SET,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotificationPreferenceSet(
         @PathVariable("notificationPreferenceSetId") UUID notificationPreferenceSetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_NOTIFICATION_PREFERENCE = "/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}";
    /**
     * GET /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId} : Get NotificationPreference
     * Get one NotificationPreference by id. The ETag header carries the row version.
     *
     * @param notificationPreferenceSetId NotificationPreferenceSet id. (required)
     * @param notificationPreferenceId NotificationPreference id. (required)
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
        value = NotificationPreferenceSetApi.PATH_GET_NOTIFICATION_PREFERENCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NotificationPreference> getNotificationPreference(
         @PathVariable("notificationPreferenceSetId") UUID notificationPreferenceSetId,
         @PathVariable("notificationPreferenceId") UUID notificationPreferenceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_NOTIFICATION_PREFERENCE_SET = "/v1/comms/notification-preference-sets/{notificationPreferenceSetId}";
    /**
     * GET /v1/comms/notification-preference-sets/{notificationPreferenceSetId} : Get NotificationPreferenceSet
     * Get one NotificationPreferenceSet by id. The ETag header carries the row version.
     *
     * @param notificationPreferenceSetId NotificationPreferenceSet id. (required)
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
        value = NotificationPreferenceSetApi.PATH_GET_NOTIFICATION_PREFERENCE_SET,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NotificationPreferenceSet> getNotificationPreferenceSet(
         @PathVariable("notificationPreferenceSetId") UUID notificationPreferenceSetId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTIFICATION_PREFERENCE_SETS = "/v1/comms/notification-preference-sets";
    /**
     * GET /v1/comms/notification-preference-sets : List NotificationPreferenceSets
     * List NotificationPreferenceSets visible to the caller, page by page, with filtering and sorting.
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
        value = NotificationPreferenceSetApi.PATH_LIST_NOTIFICATION_PREFERENCE_SETS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NotificationPreferenceSet>> listNotificationPreferenceSets(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTIFICATION_PREFERENCES = "/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences";
    /**
     * GET /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences : List NotificationPreferences
     * List NotificationPreferences visible to the caller, page by page, with filtering and sorting.
     *
     * @param notificationPreferenceSetId NotificationPreferenceSet id. (required)
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
        value = NotificationPreferenceSetApi.PATH_LIST_NOTIFICATION_PREFERENCES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NotificationPreference>> listNotificationPreferences(
         @PathVariable("notificationPreferenceSetId") UUID notificationPreferenceSetId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_NOTIFICATION_PREFERENCE = "/v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId}";
    /**
     * PATCH /v1/comms/notification-preference-sets/{notificationPreferenceSetId}/preferences/{notificationPreferenceId} : Update NotificationPreference
     * Partially update a NotificationPreference (updateMask). Requires If-Match with the current ETag.
     *
     * @param notificationPreferenceSetId NotificationPreferenceSet id. (required)
     * @param notificationPreferenceId NotificationPreference id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param notificationPreferenceUpdate  (required)
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
        value = NotificationPreferenceSetApi.PATH_UPDATE_NOTIFICATION_PREFERENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationPreference> updateNotificationPreference(
         @PathVariable("notificationPreferenceSetId") UUID notificationPreferenceSetId,
         @PathVariable("notificationPreferenceId") UUID notificationPreferenceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NotificationPreferenceUpdate notificationPreferenceUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_NOTIFICATION_PREFERENCE_SET = "/v1/comms/notification-preference-sets/{notificationPreferenceSetId}";
    /**
     * PATCH /v1/comms/notification-preference-sets/{notificationPreferenceSetId} : Update NotificationPreferenceSet
     * Partially update a NotificationPreferenceSet (updateMask). Requires If-Match with the current ETag.
     *
     * @param notificationPreferenceSetId NotificationPreferenceSet id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param notificationPreferenceSetUpdate  (required)
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
        value = NotificationPreferenceSetApi.PATH_UPDATE_NOTIFICATION_PREFERENCE_SET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationPreferenceSet> updateNotificationPreferenceSet(
         @PathVariable("notificationPreferenceSetId") UUID notificationPreferenceSetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NotificationPreferenceSetUpdate notificationPreferenceSetUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
