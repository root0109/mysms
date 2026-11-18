// Owned API contract of assessment.
package com.mysms.assessment.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.api.model.CancelMarksheetRequest;
import com.mysms.assessment.api.model.Marksheet;
import com.mysms.assessment.api.model.MarksheetUpdate;
import org.springframework.lang.Nullable;
import com.mysms.assessment.api.model.PublishMarksheetRequest;
import com.mysms.assessment.api.model.ResetMarksheetRequest;
import java.util.UUID;
import com.mysms.assessment.api.model.ValidateMarksheetRequest;
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
public interface MarksheetApi {

    String PATH_CANCEL_MARKSHEET = "/v1/assessment/marksheets/{marksheetId}:cancel";
    /**
     * POST /v1/assessment/marksheets/{marksheetId}:cancel : Cancel
     * Cancel.
     *
     * @param marksheetId  (required)
     * @param cancelMarksheetRequest  (required)
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
        value = MarksheetApi.PATH_CANCEL_MARKSHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Marksheet> cancelMarksheet(
         @PathVariable("marksheetId") UUID marksheetId,
         @Valid @RequestBody CancelMarksheetRequest cancelMarksheetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_MARKSHEET = "/v1/assessment/marksheets";
    /**
     * POST /v1/assessment/marksheets : Create Marksheet
     * Create a Marksheet. Supports Idempotency-Key and validateOnly.
     *
     * @param marksheet  (required)
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
        value = MarksheetApi.PATH_CREATE_MARKSHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Marksheet> createMarksheet(
         @Valid @RequestBody Marksheet marksheet,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_MARKSHEET = "/v1/assessment/marksheets/{marksheetId}";
    /**
     * DELETE /v1/assessment/marksheets/{marksheetId} : Delete Marksheet
     * Soft delete (sets deleted_at).
     *
     * @param marksheetId Marksheet id. (required)
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
        value = MarksheetApi.PATH_DELETE_MARKSHEET,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteMarksheet(
         @PathVariable("marksheetId") UUID marksheetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_MARKSHEET = "/v1/assessment/marksheets/{marksheetId}";
    /**
     * GET /v1/assessment/marksheets/{marksheetId} : Get Marksheet
     * Get one Marksheet by id. The ETag header carries the row version.
     *
     * @param marksheetId Marksheet id. (required)
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
        value = MarksheetApi.PATH_GET_MARKSHEET,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Marksheet> getMarksheet(
         @PathVariable("marksheetId") UUID marksheetId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MARKSHEETS = "/v1/assessment/marksheets";
    /**
     * GET /v1/assessment/marksheets : List Marksheets
     * List Marksheets visible to the caller, page by page, with filtering and sorting.
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
        value = MarksheetApi.PATH_LIST_MARKSHEETS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Marksheet>> listMarksheets(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PUBLISH_MARKSHEET = "/v1/assessment/marksheets/{marksheetId}:publish";
    /**
     * POST /v1/assessment/marksheets/{marksheetId}:publish : Release a validated marksheet to students
     * Publishes a validated marksheet so students (and guardians when enabled) can see results. Records who published and when.
     *
     * @param marksheetId  (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param publishMarksheetRequest  (optional)
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
        method = RequestMethod.POST,
        value = MarksheetApi.PATH_PUBLISH_MARKSHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Marksheet> publishMarksheet(
         @PathVariable("marksheetId") UUID marksheetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestBody(required = false) @Nullable PublishMarksheetRequest publishMarksheetRequest
    );


    String PATH_RESET_MARKSHEET = "/v1/assessment/marksheets/{marksheetId}:resetToDraft";
    /**
     * POST /v1/assessment/marksheets/{marksheetId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param marksheetId  (required)
     * @param resetMarksheetRequest  (required)
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
        value = MarksheetApi.PATH_RESET_MARKSHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Marksheet> resetMarksheet(
         @PathVariable("marksheetId") UUID marksheetId,
         @Valid @RequestBody ResetMarksheetRequest resetMarksheetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_MARKSHEET = "/v1/assessment/marksheets/{marksheetId}";
    /**
     * PATCH /v1/assessment/marksheets/{marksheetId} : Update Marksheet
     * Partially update a Marksheet (updateMask). Requires If-Match with the current ETag.
     *
     * @param marksheetId Marksheet id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param marksheetUpdate  (required)
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
        value = MarksheetApi.PATH_UPDATE_MARKSHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Marksheet> updateMarksheet(
         @PathVariable("marksheetId") UUID marksheetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody MarksheetUpdate marksheetUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_VALIDATE_MARKSHEET = "/v1/assessment/marksheets/{marksheetId}:validate";
    /**
     * POST /v1/assessment/marksheets/{marksheetId}:validate : Validate; results become visible
     * Validate; results become visible.
     *
     * @param marksheetId  (required)
     * @param validateMarksheetRequest  (required)
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
        value = MarksheetApi.PATH_VALIDATE_MARKSHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Marksheet> validateMarksheet(
         @PathVariable("marksheetId") UUID marksheetId,
         @Valid @RequestBody ValidateMarksheetRequest validateMarksheetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
