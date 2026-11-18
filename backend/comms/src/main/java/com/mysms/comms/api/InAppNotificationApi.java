// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.ArchiveNotificationsRequest;
import com.mysms.comms.api.model.InAppNotification;
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
public interface InAppNotificationApi {

    String PATH_ARCHIVE_NOTIFICATIONS = "/v1/comms/notifications:archive";
    /**
     * POST /v1/comms/notifications:archive : Hide notifications from the caller&#39;s inbox (archived_at); they stay in the audit history
     * Hide notifications from the caller&#39;s inbox (archived_at); they stay in the audit history.
     *
     * @param archiveNotificationsRequest  (required)
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
        value = InAppNotificationApi.PATH_ARCHIVE_NOTIFICATIONS,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> archiveNotifications(
         @Valid @RequestBody ArchiveNotificationsRequest archiveNotificationsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_IN_APP_NOTIFICATION = "/v1/comms/notifications/{inAppNotificationId}";
    /**
     * GET /v1/comms/notifications/{inAppNotificationId} : Get InAppNotification
     * Get one InAppNotification by id. The ETag header carries the row version.
     *
     * @param inAppNotificationId InAppNotification id. (required)
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
        value = InAppNotificationApi.PATH_GET_IN_APP_NOTIFICATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<InAppNotification> getInAppNotification(
         @PathVariable("inAppNotificationId") UUID inAppNotificationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_IN_APP_NOTIFICATIONS = "/v1/comms/notifications";
    /**
     * GET /v1/comms/notifications : List InAppNotifications
     * List InAppNotifications visible to the caller, page by page, with filtering and sorting.
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
        value = InAppNotificationApi.PATH_LIST_IN_APP_NOTIFICATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<InAppNotification>> listInAppNotifications(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_ALL_NOTIFICATIONS_READ = "/v1/comms/notifications:markAllRead";
    /**
     * POST /v1/comms/notifications:markAllRead : Mark all of the caller&#39;s notifications read
     * Mark all of the caller&#39;s notifications read.
     *
     * @param body  (required)
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
        value = InAppNotificationApi.PATH_MARK_ALL_NOTIFICATIONS_READ,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> markAllNotificationsRead(
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_MARK_NOTIFICATION_READ = "/v1/comms/notifications/{inAppNotificationId}:markRead";
    /**
     * POST /v1/comms/notifications/{inAppNotificationId}:markRead : Mark read
     * Mark read.
     *
     * @param inAppNotificationId  (required)
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
        value = InAppNotificationApi.PATH_MARK_NOTIFICATION_READ,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<InAppNotification> markNotificationRead(
         @PathVariable("inAppNotificationId") UUID inAppNotificationId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
