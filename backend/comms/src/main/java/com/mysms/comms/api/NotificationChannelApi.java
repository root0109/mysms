// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.NotificationChannel;
import com.mysms.comms.api.model.NotificationChannelProperty;
import com.mysms.comms.api.model.NotificationChannelPropertyUpdate;
import com.mysms.comms.api.model.NotificationChannelUpdate;
import org.springframework.lang.Nullable;
import com.mysms.comms.api.model.SendTestMessageRequest;
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
public interface NotificationChannelApi {

    String PATH_CREATE_NOTIFICATION_CHANNEL = "/v1/comms/notification-channels";
    /**
     * POST /v1/comms/notification-channels : Create NotificationChannel
     * Create a NotificationChannel. Supports Idempotency-Key and validateOnly.
     *
     * @param notificationChannel  (required)
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
        value = NotificationChannelApi.PATH_CREATE_NOTIFICATION_CHANNEL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationChannel> createNotificationChannel(
         @Valid @RequestBody NotificationChannel notificationChannel,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_NOTIFICATION_CHANNEL_PROPERTY = "/v1/comms/notification-channels/{notificationChannelId}/properties";
    /**
     * POST /v1/comms/notification-channels/{notificationChannelId}/properties : Create NotificationChannelProperty
     * Create a NotificationChannelProperty. Supports Idempotency-Key and validateOnly.
     *
     * @param notificationChannelId NotificationChannel id. (required)
     * @param notificationChannelProperty  (required)
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
        value = NotificationChannelApi.PATH_CREATE_NOTIFICATION_CHANNEL_PROPERTY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationChannelProperty> createNotificationChannelProperty(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
         @Valid @RequestBody NotificationChannelProperty notificationChannelProperty,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_NOTIFICATION_CHANNEL = "/v1/comms/notification-channels/{notificationChannelId}";
    /**
     * DELETE /v1/comms/notification-channels/{notificationChannelId} : Delete NotificationChannel
     * Soft delete (sets deleted_at).
     *
     * @param notificationChannelId NotificationChannel id. (required)
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
        value = NotificationChannelApi.PATH_DELETE_NOTIFICATION_CHANNEL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotificationChannel(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_NOTIFICATION_CHANNEL_PROPERTY = "/v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}";
    /**
     * DELETE /v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId} : Delete NotificationChannelProperty
     * Soft delete (sets deleted_at).
     *
     * @param notificationChannelId NotificationChannel id. (required)
     * @param notificationChannelPropertyId NotificationChannelProperty id. (required)
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
        value = NotificationChannelApi.PATH_DELETE_NOTIFICATION_CHANNEL_PROPERTY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotificationChannelProperty(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
         @PathVariable("notificationChannelPropertyId") UUID notificationChannelPropertyId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_NOTIFICATION_CHANNEL = "/v1/comms/notification-channels/{notificationChannelId}";
    /**
     * GET /v1/comms/notification-channels/{notificationChannelId} : Get NotificationChannel
     * Get one NotificationChannel by id. The ETag header carries the row version.
     *
     * @param notificationChannelId NotificationChannel id. (required)
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
        value = NotificationChannelApi.PATH_GET_NOTIFICATION_CHANNEL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NotificationChannel> getNotificationChannel(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_NOTIFICATION_CHANNEL_PROPERTY = "/v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}";
    /**
     * GET /v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId} : Get NotificationChannelProperty
     * Get one NotificationChannelProperty by id. The ETag header carries the row version.
     *
     * @param notificationChannelId NotificationChannel id. (required)
     * @param notificationChannelPropertyId NotificationChannelProperty id. (required)
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
        value = NotificationChannelApi.PATH_GET_NOTIFICATION_CHANNEL_PROPERTY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NotificationChannelProperty> getNotificationChannelProperty(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
         @PathVariable("notificationChannelPropertyId") UUID notificationChannelPropertyId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTIFICATION_CHANNEL_PROPERTIES = "/v1/comms/notification-channels/{notificationChannelId}/properties";
    /**
     * GET /v1/comms/notification-channels/{notificationChannelId}/properties : List NotificationChannelProperties
     * List NotificationChannelProperties visible to the caller, page by page, with filtering and sorting.
     *
     * @param notificationChannelId NotificationChannel id. (required)
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
        value = NotificationChannelApi.PATH_LIST_NOTIFICATION_CHANNEL_PROPERTIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NotificationChannelProperty>> listNotificationChannelProperties(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTIFICATION_CHANNELS = "/v1/comms/notification-channels";
    /**
     * GET /v1/comms/notification-channels : List NotificationChannels
     * List NotificationChannels visible to the caller, page by page, with filtering and sorting.
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
        value = NotificationChannelApi.PATH_LIST_NOTIFICATION_CHANNELS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NotificationChannel>> listNotificationChannels(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_SEND_TEST_MESSAGE = "/v1/comms/notification-channels/{notificationChannelId}:sendTest";
    /**
     * POST /v1/comms/notification-channels/{notificationChannelId}:sendTest : Send a test message through the channel configuration
     * Send a test message through the channel configuration.
     *
     * @param notificationChannelId  (required)
     * @param sendTestMessageRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = NotificationChannelApi.PATH_SEND_TEST_MESSAGE,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> sendTestMessage(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
         @Valid @RequestBody SendTestMessageRequest sendTestMessageRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_NOTIFICATION_CHANNEL = "/v1/comms/notification-channels/{notificationChannelId}";
    /**
     * PATCH /v1/comms/notification-channels/{notificationChannelId} : Update NotificationChannel
     * Partially update a NotificationChannel (updateMask). Requires If-Match with the current ETag.
     *
     * @param notificationChannelId NotificationChannel id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param notificationChannelUpdate  (required)
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
        value = NotificationChannelApi.PATH_UPDATE_NOTIFICATION_CHANNEL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationChannel> updateNotificationChannel(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NotificationChannelUpdate notificationChannelUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_NOTIFICATION_CHANNEL_PROPERTY = "/v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId}";
    /**
     * PATCH /v1/comms/notification-channels/{notificationChannelId}/properties/{notificationChannelPropertyId} : Update NotificationChannelProperty
     * Partially update a NotificationChannelProperty (updateMask). Requires If-Match with the current ETag.
     *
     * @param notificationChannelId NotificationChannel id. (required)
     * @param notificationChannelPropertyId NotificationChannelProperty id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param notificationChannelPropertyUpdate  (required)
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
        value = NotificationChannelApi.PATH_UPDATE_NOTIFICATION_CHANNEL_PROPERTY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationChannelProperty> updateNotificationChannelProperty(
         @PathVariable("notificationChannelId") UUID notificationChannelId,
         @PathVariable("notificationChannelPropertyId") UUID notificationChannelPropertyId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NotificationChannelPropertyUpdate notificationChannelPropertyUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
