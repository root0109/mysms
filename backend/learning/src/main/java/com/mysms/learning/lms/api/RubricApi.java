// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.learning.lms.api.model.Rubric;
import com.mysms.learning.lms.api.model.RubricCell;
import com.mysms.learning.lms.api.model.RubricCellUpdate;
import com.mysms.learning.lms.api.model.RubricCriterion;
import com.mysms.learning.lms.api.model.RubricCriterionUpdate;
import com.mysms.learning.lms.api.model.RubricLevel;
import com.mysms.learning.lms.api.model.RubricLevelUpdate;
import com.mysms.learning.lms.api.model.RubricUpdate;
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
public interface RubricApi {

    String PATH_CREATE_RUBRIC = "/v1/lms/rubrics";
    /**
     * POST /v1/lms/rubrics : Create Rubric
     * Create a Rubric. Supports Idempotency-Key and validateOnly.
     *
     * @param rubric  (required)
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
        value = RubricApi.PATH_CREATE_RUBRIC,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Rubric> createRubric(
         @Valid @RequestBody Rubric rubric,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_RUBRIC_CELL = "/v1/lms/rubrics/{rubricId}/cells";
    /**
     * POST /v1/lms/rubrics/{rubricId}/cells : Create RubricCell
     * Create a RubricCell. Supports Idempotency-Key and validateOnly.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricCell  (required)
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
        value = RubricApi.PATH_CREATE_RUBRIC_CELL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RubricCell> createRubricCell(
         @PathVariable("rubricId") UUID rubricId,
         @Valid @RequestBody RubricCell rubricCell,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_RUBRIC_CRITERION = "/v1/lms/rubrics/{rubricId}/criteria";
    /**
     * POST /v1/lms/rubrics/{rubricId}/criteria : Create RubricCriterion
     * Create a RubricCriterion. Supports Idempotency-Key and validateOnly.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricCriterion  (required)
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
        value = RubricApi.PATH_CREATE_RUBRIC_CRITERION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RubricCriterion> createRubricCriterion(
         @PathVariable("rubricId") UUID rubricId,
         @Valid @RequestBody RubricCriterion rubricCriterion,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_RUBRIC_LEVEL = "/v1/lms/rubrics/{rubricId}/levels";
    /**
     * POST /v1/lms/rubrics/{rubricId}/levels : Create RubricLevel
     * Create a RubricLevel. Supports Idempotency-Key and validateOnly.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricLevel  (required)
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
        value = RubricApi.PATH_CREATE_RUBRIC_LEVEL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RubricLevel> createRubricLevel(
         @PathVariable("rubricId") UUID rubricId,
         @Valid @RequestBody RubricLevel rubricLevel,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_RUBRIC = "/v1/lms/rubrics/{rubricId}";
    /**
     * DELETE /v1/lms/rubrics/{rubricId} : Delete Rubric
     * Soft delete (sets deleted_at).
     *
     * @param rubricId Rubric id. (required)
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
        value = RubricApi.PATH_DELETE_RUBRIC,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRubric(
         @PathVariable("rubricId") UUID rubricId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RUBRIC_CELL = "/v1/lms/rubrics/{rubricId}/cells/{rubricCellId}";
    /**
     * DELETE /v1/lms/rubrics/{rubricId}/cells/{rubricCellId} : Delete RubricCell
     * Soft delete (sets deleted_at).
     *
     * @param rubricId Rubric id. (required)
     * @param rubricCellId RubricCell id. (required)
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
        value = RubricApi.PATH_DELETE_RUBRIC_CELL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRubricCell(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricCellId") UUID rubricCellId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RUBRIC_CRITERION = "/v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}";
    /**
     * DELETE /v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId} : Delete RubricCriterion
     * Soft delete (sets deleted_at).
     *
     * @param rubricId Rubric id. (required)
     * @param rubricCriterionId RubricCriterion id. (required)
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
        value = RubricApi.PATH_DELETE_RUBRIC_CRITERION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRubricCriterion(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricCriterionId") UUID rubricCriterionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RUBRIC_LEVEL = "/v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}";
    /**
     * DELETE /v1/lms/rubrics/{rubricId}/levels/{rubricLevelId} : Delete RubricLevel
     * Soft delete (sets deleted_at).
     *
     * @param rubricId Rubric id. (required)
     * @param rubricLevelId RubricLevel id. (required)
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
        value = RubricApi.PATH_DELETE_RUBRIC_LEVEL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRubricLevel(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricLevelId") UUID rubricLevelId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_RUBRIC = "/v1/lms/rubrics/{rubricId}";
    /**
     * GET /v1/lms/rubrics/{rubricId} : Get Rubric
     * Get one Rubric by id. The ETag header carries the row version.
     *
     * @param rubricId Rubric id. (required)
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
        value = RubricApi.PATH_GET_RUBRIC,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Rubric> getRubric(
         @PathVariable("rubricId") UUID rubricId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RUBRIC_CELL = "/v1/lms/rubrics/{rubricId}/cells/{rubricCellId}";
    /**
     * GET /v1/lms/rubrics/{rubricId}/cells/{rubricCellId} : Get RubricCell
     * Get one RubricCell by id. The ETag header carries the row version.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricCellId RubricCell id. (required)
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
        value = RubricApi.PATH_GET_RUBRIC_CELL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RubricCell> getRubricCell(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricCellId") UUID rubricCellId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RUBRIC_CRITERION = "/v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}";
    /**
     * GET /v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId} : Get RubricCriterion
     * Get one RubricCriterion by id. The ETag header carries the row version.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricCriterionId RubricCriterion id. (required)
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
        value = RubricApi.PATH_GET_RUBRIC_CRITERION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RubricCriterion> getRubricCriterion(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricCriterionId") UUID rubricCriterionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RUBRIC_LEVEL = "/v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}";
    /**
     * GET /v1/lms/rubrics/{rubricId}/levels/{rubricLevelId} : Get RubricLevel
     * Get one RubricLevel by id. The ETag header carries the row version.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricLevelId RubricLevel id. (required)
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
        value = RubricApi.PATH_GET_RUBRIC_LEVEL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RubricLevel> getRubricLevel(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricLevelId") UUID rubricLevelId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RUBRIC_CELLS = "/v1/lms/rubrics/{rubricId}/cells";
    /**
     * GET /v1/lms/rubrics/{rubricId}/cells : List RubricCells
     * List RubricCells visible to the caller, page by page, with filtering and sorting.
     *
     * @param rubricId Rubric id. (required)
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
        value = RubricApi.PATH_LIST_RUBRIC_CELLS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RubricCell>> listRubricCells(
         @PathVariable("rubricId") UUID rubricId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RUBRIC_CRITERIA = "/v1/lms/rubrics/{rubricId}/criteria";
    /**
     * GET /v1/lms/rubrics/{rubricId}/criteria : List RubricCriteria
     * List RubricCriteria visible to the caller, page by page, with filtering and sorting.
     *
     * @param rubricId Rubric id. (required)
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
        value = RubricApi.PATH_LIST_RUBRIC_CRITERIA,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RubricCriterion>> listRubricCriteria(
         @PathVariable("rubricId") UUID rubricId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RUBRIC_LEVELS = "/v1/lms/rubrics/{rubricId}/levels";
    /**
     * GET /v1/lms/rubrics/{rubricId}/levels : List RubricLevels
     * List RubricLevels visible to the caller, page by page, with filtering and sorting.
     *
     * @param rubricId Rubric id. (required)
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
        value = RubricApi.PATH_LIST_RUBRIC_LEVELS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RubricLevel>> listRubricLevels(
         @PathVariable("rubricId") UUID rubricId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RUBRICS = "/v1/lms/rubrics";
    /**
     * GET /v1/lms/rubrics : List Rubrics
     * List Rubrics visible to the caller, page by page, with filtering and sorting.
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
        value = RubricApi.PATH_LIST_RUBRICS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Rubric>> listRubrics(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_RUBRIC = "/v1/lms/rubrics/{rubricId}";
    /**
     * PATCH /v1/lms/rubrics/{rubricId} : Update Rubric
     * Partially update a Rubric (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param rubricId Rubric id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param rubricUpdate  (required)
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
        value = RubricApi.PATH_UPDATE_RUBRIC,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Rubric> updateRubric(
         @PathVariable("rubricId") UUID rubricId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RubricUpdate rubricUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_RUBRIC_CELL = "/v1/lms/rubrics/{rubricId}/cells/{rubricCellId}";
    /**
     * PATCH /v1/lms/rubrics/{rubricId}/cells/{rubricCellId} : Update RubricCell
     * Partially update a RubricCell (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricCellId RubricCell id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param rubricCellUpdate  (required)
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
        value = RubricApi.PATH_UPDATE_RUBRIC_CELL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RubricCell> updateRubricCell(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricCellId") UUID rubricCellId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RubricCellUpdate rubricCellUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_RUBRIC_CRITERION = "/v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId}";
    /**
     * PATCH /v1/lms/rubrics/{rubricId}/criteria/{rubricCriterionId} : Update RubricCriterion
     * Partially update a RubricCriterion (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricCriterionId RubricCriterion id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param rubricCriterionUpdate  (required)
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
        value = RubricApi.PATH_UPDATE_RUBRIC_CRITERION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RubricCriterion> updateRubricCriterion(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricCriterionId") UUID rubricCriterionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RubricCriterionUpdate rubricCriterionUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_RUBRIC_LEVEL = "/v1/lms/rubrics/{rubricId}/levels/{rubricLevelId}";
    /**
     * PATCH /v1/lms/rubrics/{rubricId}/levels/{rubricLevelId} : Update RubricLevel
     * Partially update a RubricLevel (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param rubricId Rubric id. (required)
     * @param rubricLevelId RubricLevel id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param rubricLevelUpdate  (required)
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
        value = RubricApi.PATH_UPDATE_RUBRIC_LEVEL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RubricLevel> updateRubricLevel(
         @PathVariable("rubricId") UUID rubricId,
         @PathVariable("rubricLevelId") UUID rubricLevelId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RubricLevelUpdate rubricLevelUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
