// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.NotificationCohortSetting;
import com.mysms.comms.api.model.NotificationCohortSettingUpdate;
import com.mysms.comms.api.model.NotificationRule;
import com.mysms.comms.api.model.NotificationRuleUpdate;
import com.mysms.comms.api.model.NotificationType;
import com.mysms.comms.api.model.NotificationTypeProperty;
import com.mysms.comms.api.model.NotificationTypePropertyUpdate;
import com.mysms.comms.api.model.NotificationTypeUpdate;
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
public interface NotificationTypeApi {

    String PATH_CREATE_NOTIFICATION_COHORT_SETTING = "/v1/comms/notification-types/{notificationTypeId}/cohort-settings";
    /**
     * POST /v1/comms/notification-types/{notificationTypeId}/cohort-settings : Create NotificationCohortSetting
     * Create a NotificationCohortSetting. Supports Idempotency-Key and validateOnly.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationCohortSetting  (required)
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
        value = NotificationTypeApi.PATH_CREATE_NOTIFICATION_COHORT_SETTING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationCohortSetting> createNotificationCohortSetting(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @Valid @RequestBody NotificationCohortSetting notificationCohortSetting,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_NOTIFICATION_RULE = "/v1/comms/notification-types/{notificationTypeId}/rules";
    /**
     * POST /v1/comms/notification-types/{notificationTypeId}/rules : Create NotificationRule
     * Create a NotificationRule. Supports Idempotency-Key and validateOnly.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationRule  (required)
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
        value = NotificationTypeApi.PATH_CREATE_NOTIFICATION_RULE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationRule> createNotificationRule(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @Valid @RequestBody NotificationRule notificationRule,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_NOTIFICATION_TYPE = "/v1/comms/notification-types";
    /**
     * POST /v1/comms/notification-types : Create NotificationType
     * Create a NotificationType. Supports Idempotency-Key and validateOnly.
     *
     * @param notificationType  (required)
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
        value = NotificationTypeApi.PATH_CREATE_NOTIFICATION_TYPE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationType> createNotificationType(
         @Valid @RequestBody NotificationType notificationType,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_NOTIFICATION_TYPE_PROPERTY = "/v1/comms/notification-types/{notificationTypeId}/properties";
    /**
     * POST /v1/comms/notification-types/{notificationTypeId}/properties : Create NotificationTypeProperty
     * Create a NotificationTypeProperty. Supports Idempotency-Key and validateOnly.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationTypeProperty  (required)
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
        value = NotificationTypeApi.PATH_CREATE_NOTIFICATION_TYPE_PROPERTY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationTypeProperty> createNotificationTypeProperty(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @Valid @RequestBody NotificationTypeProperty notificationTypeProperty,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_NOTIFICATION_COHORT_SETTING = "/v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}";
    /**
     * DELETE /v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId} : Delete NotificationCohortSetting
     * Soft delete (sets deleted_at).
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationCohortSettingId NotificationCohortSetting id. (required)
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
        value = NotificationTypeApi.PATH_DELETE_NOTIFICATION_COHORT_SETTING,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotificationCohortSetting(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationCohortSettingId") UUID notificationCohortSettingId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_NOTIFICATION_RULE = "/v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}";
    /**
     * DELETE /v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId} : Delete NotificationRule
     * Soft delete (sets deleted_at).
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationRuleId NotificationRule id. (required)
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
        value = NotificationTypeApi.PATH_DELETE_NOTIFICATION_RULE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotificationRule(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationRuleId") UUID notificationRuleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_NOTIFICATION_TYPE = "/v1/comms/notification-types/{notificationTypeId}";
    /**
     * DELETE /v1/comms/notification-types/{notificationTypeId} : Delete NotificationType
     * Soft delete (sets deleted_at).
     *
     * @param notificationTypeId NotificationType id. (required)
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
        value = NotificationTypeApi.PATH_DELETE_NOTIFICATION_TYPE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotificationType(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_NOTIFICATION_TYPE_PROPERTY = "/v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}";
    /**
     * DELETE /v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId} : Delete NotificationTypeProperty
     * Soft delete (sets deleted_at).
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationTypePropertyId NotificationTypeProperty id. (required)
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
        value = NotificationTypeApi.PATH_DELETE_NOTIFICATION_TYPE_PROPERTY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotificationTypeProperty(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationTypePropertyId") UUID notificationTypePropertyId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_NOTIFICATION_COHORT_SETTING = "/v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}";
    /**
     * GET /v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId} : Get NotificationCohortSetting
     * Get one NotificationCohortSetting by id. The ETag header carries the row version.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationCohortSettingId NotificationCohortSetting id. (required)
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
        value = NotificationTypeApi.PATH_GET_NOTIFICATION_COHORT_SETTING,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NotificationCohortSetting> getNotificationCohortSetting(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationCohortSettingId") UUID notificationCohortSettingId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_NOTIFICATION_RULE = "/v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}";
    /**
     * GET /v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId} : Get NotificationRule
     * Get one NotificationRule by id. The ETag header carries the row version.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationRuleId NotificationRule id. (required)
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
        value = NotificationTypeApi.PATH_GET_NOTIFICATION_RULE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NotificationRule> getNotificationRule(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationRuleId") UUID notificationRuleId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_NOTIFICATION_TYPE = "/v1/comms/notification-types/{notificationTypeId}";
    /**
     * GET /v1/comms/notification-types/{notificationTypeId} : Get NotificationType
     * Get one NotificationType by id. The ETag header carries the row version.
     *
     * @param notificationTypeId NotificationType id. (required)
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
        value = NotificationTypeApi.PATH_GET_NOTIFICATION_TYPE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NotificationType> getNotificationType(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_NOTIFICATION_TYPE_PROPERTY = "/v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}";
    /**
     * GET /v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId} : Get NotificationTypeProperty
     * Get one NotificationTypeProperty by id. The ETag header carries the row version.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationTypePropertyId NotificationTypeProperty id. (required)
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
        value = NotificationTypeApi.PATH_GET_NOTIFICATION_TYPE_PROPERTY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NotificationTypeProperty> getNotificationTypeProperty(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationTypePropertyId") UUID notificationTypePropertyId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTIFICATION_COHORT_SETTINGS = "/v1/comms/notification-types/{notificationTypeId}/cohort-settings";
    /**
     * GET /v1/comms/notification-types/{notificationTypeId}/cohort-settings : List NotificationCohortSettings
     * List NotificationCohortSettings visible to the caller, page by page, with filtering and sorting.
     *
     * @param notificationTypeId NotificationType id. (required)
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
        value = NotificationTypeApi.PATH_LIST_NOTIFICATION_COHORT_SETTINGS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NotificationCohortSetting>> listNotificationCohortSettings(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTIFICATION_RULES = "/v1/comms/notification-types/{notificationTypeId}/rules";
    /**
     * GET /v1/comms/notification-types/{notificationTypeId}/rules : List NotificationRules
     * List NotificationRules visible to the caller, page by page, with filtering and sorting.
     *
     * @param notificationTypeId NotificationType id. (required)
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
        value = NotificationTypeApi.PATH_LIST_NOTIFICATION_RULES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NotificationRule>> listNotificationRules(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTIFICATION_TYPE_PROPERTIES = "/v1/comms/notification-types/{notificationTypeId}/properties";
    /**
     * GET /v1/comms/notification-types/{notificationTypeId}/properties : List NotificationTypeProperties
     * List NotificationTypeProperties visible to the caller, page by page, with filtering and sorting.
     *
     * @param notificationTypeId NotificationType id. (required)
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
        value = NotificationTypeApi.PATH_LIST_NOTIFICATION_TYPE_PROPERTIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NotificationTypeProperty>> listNotificationTypeProperties(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTIFICATION_TYPES = "/v1/comms/notification-types";
    /**
     * GET /v1/comms/notification-types : List NotificationTypes
     * List NotificationTypes visible to the caller, page by page, with filtering and sorting.
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
        value = NotificationTypeApi.PATH_LIST_NOTIFICATION_TYPES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NotificationType>> listNotificationTypes(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_NOTIFICATION_COHORT_SETTING = "/v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId}";
    /**
     * PATCH /v1/comms/notification-types/{notificationTypeId}/cohort-settings/{notificationCohortSettingId} : Update NotificationCohortSetting
     * Partially update a NotificationCohortSetting (updateMask). Requires If-Match with the current ETag.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationCohortSettingId NotificationCohortSetting id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param notificationCohortSettingUpdate  (required)
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
        value = NotificationTypeApi.PATH_UPDATE_NOTIFICATION_COHORT_SETTING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationCohortSetting> updateNotificationCohortSetting(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationCohortSettingId") UUID notificationCohortSettingId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NotificationCohortSettingUpdate notificationCohortSettingUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_NOTIFICATION_RULE = "/v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId}";
    /**
     * PATCH /v1/comms/notification-types/{notificationTypeId}/rules/{notificationRuleId} : Update NotificationRule
     * Partially update a NotificationRule (updateMask). Requires If-Match with the current ETag.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationRuleId NotificationRule id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param notificationRuleUpdate  (required)
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
        value = NotificationTypeApi.PATH_UPDATE_NOTIFICATION_RULE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationRule> updateNotificationRule(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationRuleId") UUID notificationRuleId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NotificationRuleUpdate notificationRuleUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_NOTIFICATION_TYPE = "/v1/comms/notification-types/{notificationTypeId}";
    /**
     * PATCH /v1/comms/notification-types/{notificationTypeId} : Update NotificationType
     * Partially update a NotificationType (updateMask). Requires If-Match with the current ETag.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param notificationTypeUpdate  (required)
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
        value = NotificationTypeApi.PATH_UPDATE_NOTIFICATION_TYPE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationType> updateNotificationType(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NotificationTypeUpdate notificationTypeUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_NOTIFICATION_TYPE_PROPERTY = "/v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId}";
    /**
     * PATCH /v1/comms/notification-types/{notificationTypeId}/properties/{notificationTypePropertyId} : Update NotificationTypeProperty
     * Partially update a NotificationTypeProperty (updateMask). Requires If-Match with the current ETag.
     *
     * @param notificationTypeId NotificationType id. (required)
     * @param notificationTypePropertyId NotificationTypeProperty id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param notificationTypePropertyUpdate  (required)
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
        value = NotificationTypeApi.PATH_UPDATE_NOTIFICATION_TYPE_PROPERTY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NotificationTypeProperty> updateNotificationTypeProperty(
         @PathVariable("notificationTypeId") UUID notificationTypeId,
         @PathVariable("notificationTypePropertyId") UUID notificationTypePropertyId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NotificationTypePropertyUpdate notificationTypePropertyUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
