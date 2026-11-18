// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ConnectEvaluatorCalendarRequest;
import com.mysms.lms.api.model.Evaluator;
import com.mysms.lms.api.model.EvaluatorAvailability;
import com.mysms.lms.api.model.EvaluatorAvailabilityUpdate;
import com.mysms.lms.api.model.EvaluatorUpdate;
import com.mysms.lms.api.model.ExternalLink;
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
public interface EvaluatorApi {

    String PATH_CONNECT_EVALUATOR_CALENDAR = "/v1/lms/evaluators/{evaluatorId}:connectCalendar";
    /**
     * POST /v1/lms/evaluators/{evaluatorId}:connectCalendar : Start OAuth to link the evaluator&#39;s calendar
     * Start OAuth to link the evaluator&#39;s calendar.
     *
     * @param evaluatorId  (required)
     * @param connectEvaluatorCalendarRequest  (required)
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
        value = EvaluatorApi.PATH_CONNECT_EVALUATOR_CALENDAR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExternalLink> connectEvaluatorCalendar(
         @PathVariable("evaluatorId") UUID evaluatorId,
         @Valid @RequestBody ConnectEvaluatorCalendarRequest connectEvaluatorCalendarRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_EVALUATOR = "/v1/lms/evaluators";
    /**
     * POST /v1/lms/evaluators : Create Evaluator
     * Create a Evaluator. Supports Idempotency-Key and validateOnly.
     *
     * @param evaluator  (required)
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
        value = EvaluatorApi.PATH_CREATE_EVALUATOR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Evaluator> createEvaluator(
         @Valid @RequestBody Evaluator evaluator,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_EVALUATOR_AVAILABILITY = "/v1/lms/evaluators/{evaluatorId}/availability";
    /**
     * POST /v1/lms/evaluators/{evaluatorId}/availability : Create EvaluatorAvailability
     * Create a EvaluatorAvailability. Supports Idempotency-Key and validateOnly.
     *
     * @param evaluatorId Evaluator id. (required)
     * @param evaluatorAvailability  (required)
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
        value = EvaluatorApi.PATH_CREATE_EVALUATOR_AVAILABILITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EvaluatorAvailability> createEvaluatorAvailability(
         @PathVariable("evaluatorId") UUID evaluatorId,
         @Valid @RequestBody EvaluatorAvailability evaluatorAvailability,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_EVALUATOR = "/v1/lms/evaluators/{evaluatorId}";
    /**
     * DELETE /v1/lms/evaluators/{evaluatorId} : Delete Evaluator
     * Soft delete (sets deleted_at).
     *
     * @param evaluatorId Evaluator id. (required)
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
        value = EvaluatorApi.PATH_DELETE_EVALUATOR,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEvaluator(
         @PathVariable("evaluatorId") UUID evaluatorId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_EVALUATOR_AVAILABILITY = "/v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}";
    /**
     * DELETE /v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId} : Delete EvaluatorAvailability
     * Soft delete (sets deleted_at).
     *
     * @param evaluatorId Evaluator id. (required)
     * @param evaluatorAvailabilityId EvaluatorAvailability id. (required)
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
        value = EvaluatorApi.PATH_DELETE_EVALUATOR_AVAILABILITY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEvaluatorAvailability(
         @PathVariable("evaluatorId") UUID evaluatorId,
         @PathVariable("evaluatorAvailabilityId") UUID evaluatorAvailabilityId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DISCONNECT_EVALUATOR_CALENDAR = "/v1/lms/evaluators/{evaluatorId}:disconnectCalendar";
    /**
     * POST /v1/lms/evaluators/{evaluatorId}:disconnectCalendar : Unlink the calendar
     * Unlink the calendar.
     *
     * @param evaluatorId  (required)
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
        value = EvaluatorApi.PATH_DISCONNECT_EVALUATOR_CALENDAR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Evaluator> disconnectEvaluatorCalendar(
         @PathVariable("evaluatorId") UUID evaluatorId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_EVALUATOR = "/v1/lms/evaluators/{evaluatorId}";
    /**
     * GET /v1/lms/evaluators/{evaluatorId} : Get Evaluator
     * Get one Evaluator by id. The ETag header carries the row version.
     *
     * @param evaluatorId Evaluator id. (required)
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
        value = EvaluatorApi.PATH_GET_EVALUATOR,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Evaluator> getEvaluator(
         @PathVariable("evaluatorId") UUID evaluatorId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EVALUATOR_AVAILABILITY = "/v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}";
    /**
     * GET /v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId} : Get EvaluatorAvailability
     * Get one EvaluatorAvailability by id. The ETag header carries the row version.
     *
     * @param evaluatorId Evaluator id. (required)
     * @param evaluatorAvailabilityId EvaluatorAvailability id. (required)
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
        value = EvaluatorApi.PATH_GET_EVALUATOR_AVAILABILITY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<EvaluatorAvailability> getEvaluatorAvailability(
         @PathVariable("evaluatorId") UUID evaluatorId,
         @PathVariable("evaluatorAvailabilityId") UUID evaluatorAvailabilityId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EVALUATOR_AVAILABILITIES = "/v1/lms/evaluators/{evaluatorId}/availability";
    /**
     * GET /v1/lms/evaluators/{evaluatorId}/availability : List EvaluatorAvailabilities
     * List EvaluatorAvailabilities visible to the caller, page by page, with filtering and sorting.
     *
     * @param evaluatorId Evaluator id. (required)
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
        value = EvaluatorApi.PATH_LIST_EVALUATOR_AVAILABILITIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<EvaluatorAvailability>> listEvaluatorAvailabilities(
         @PathVariable("evaluatorId") UUID evaluatorId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EVALUATORS = "/v1/lms/evaluators";
    /**
     * GET /v1/lms/evaluators : List Evaluators
     * List Evaluators visible to the caller, page by page, with filtering and sorting.
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
        value = EvaluatorApi.PATH_LIST_EVALUATORS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Evaluator>> listEvaluators(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_EVALUATOR = "/v1/lms/evaluators/{evaluatorId}";
    /**
     * PATCH /v1/lms/evaluators/{evaluatorId} : Update Evaluator
     * Partially update a Evaluator (updateMask). Requires If-Match with the current ETag.
     *
     * @param evaluatorId Evaluator id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param evaluatorUpdate  (required)
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
        value = EvaluatorApi.PATH_UPDATE_EVALUATOR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Evaluator> updateEvaluator(
         @PathVariable("evaluatorId") UUID evaluatorId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EvaluatorUpdate evaluatorUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_EVALUATOR_AVAILABILITY = "/v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId}";
    /**
     * PATCH /v1/lms/evaluators/{evaluatorId}/availability/{evaluatorAvailabilityId} : Update EvaluatorAvailability
     * Partially update a EvaluatorAvailability (updateMask). Requires If-Match with the current ETag.
     *
     * @param evaluatorId Evaluator id. (required)
     * @param evaluatorAvailabilityId EvaluatorAvailability id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param evaluatorAvailabilityUpdate  (required)
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
        value = EvaluatorApi.PATH_UPDATE_EVALUATOR_AVAILABILITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<EvaluatorAvailability> updateEvaluatorAvailability(
         @PathVariable("evaluatorId") UUID evaluatorId,
         @PathVariable("evaluatorAvailabilityId") UUID evaluatorAvailabilityId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EvaluatorAvailabilityUpdate evaluatorAvailabilityUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
