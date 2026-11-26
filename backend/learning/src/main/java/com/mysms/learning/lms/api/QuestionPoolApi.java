// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.learning.lms.api.model.QuestionPool;
import com.mysms.learning.lms.api.model.QuestionPoolItem;
import com.mysms.learning.lms.api.model.QuestionPoolItemUpdate;
import com.mysms.learning.lms.api.model.QuestionPoolUpdate;
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
public interface QuestionPoolApi {

    String PATH_CREATE_QUESTION_POOL = "/v1/lms/question-pools";
    /**
     * POST /v1/lms/question-pools : Create QuestionPool
     * Create a QuestionPool. Supports Idempotency-Key and validateOnly.
     *
     * @param questionPool  (required)
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
        value = QuestionPoolApi.PATH_CREATE_QUESTION_POOL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionPool> createQuestionPool(
         @Valid @RequestBody QuestionPool questionPool,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_QUESTION_POOL_ITEM = "/v1/lms/question-pools/{questionPoolId}/items";
    /**
     * POST /v1/lms/question-pools/{questionPoolId}/items : Create QuestionPoolItem
     * Create a QuestionPoolItem. Supports Idempotency-Key and validateOnly.
     *
     * @param questionPoolId QuestionPool id. (required)
     * @param questionPoolItem  (required)
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
        value = QuestionPoolApi.PATH_CREATE_QUESTION_POOL_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionPoolItem> createQuestionPoolItem(
         @PathVariable("questionPoolId") UUID questionPoolId,
         @Valid @RequestBody QuestionPoolItem questionPoolItem,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_QUESTION_POOL = "/v1/lms/question-pools/{questionPoolId}";
    /**
     * DELETE /v1/lms/question-pools/{questionPoolId} : Delete QuestionPool
     * Soft delete (sets deleted_at).
     *
     * @param questionPoolId QuestionPool id. (required)
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
        value = QuestionPoolApi.PATH_DELETE_QUESTION_POOL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestionPool(
         @PathVariable("questionPoolId") UUID questionPoolId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_QUESTION_POOL_ITEM = "/v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}";
    /**
     * DELETE /v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId} : Delete QuestionPoolItem
     * Soft delete (sets deleted_at).
     *
     * @param questionPoolId QuestionPool id. (required)
     * @param questionPoolItemId QuestionPoolItem id. (required)
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
        value = QuestionPoolApi.PATH_DELETE_QUESTION_POOL_ITEM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestionPoolItem(
         @PathVariable("questionPoolId") UUID questionPoolId,
         @PathVariable("questionPoolItemId") UUID questionPoolItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_QUESTION_POOL = "/v1/lms/question-pools/{questionPoolId}";
    /**
     * GET /v1/lms/question-pools/{questionPoolId} : Get QuestionPool
     * Get one QuestionPool by id. The ETag header carries the row version.
     *
     * @param questionPoolId QuestionPool id. (required)
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
        value = QuestionPoolApi.PATH_GET_QUESTION_POOL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuestionPool> getQuestionPool(
         @PathVariable("questionPoolId") UUID questionPoolId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUESTION_POOL_ITEM = "/v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}";
    /**
     * GET /v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId} : Get QuestionPoolItem
     * Get one QuestionPoolItem by id. The ETag header carries the row version.
     *
     * @param questionPoolId QuestionPool id. (required)
     * @param questionPoolItemId QuestionPoolItem id. (required)
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
        value = QuestionPoolApi.PATH_GET_QUESTION_POOL_ITEM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuestionPoolItem> getQuestionPoolItem(
         @PathVariable("questionPoolId") UUID questionPoolId,
         @PathVariable("questionPoolItemId") UUID questionPoolItemId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUESTION_POOL_ITEMS = "/v1/lms/question-pools/{questionPoolId}/items";
    /**
     * GET /v1/lms/question-pools/{questionPoolId}/items : List QuestionPoolItems
     * List QuestionPoolItems visible to the caller, page by page, with filtering and sorting.
     *
     * @param questionPoolId QuestionPool id. (required)
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
        value = QuestionPoolApi.PATH_LIST_QUESTION_POOL_ITEMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuestionPoolItem>> listQuestionPoolItems(
         @PathVariable("questionPoolId") UUID questionPoolId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUESTION_POOLS = "/v1/lms/question-pools";
    /**
     * GET /v1/lms/question-pools : List QuestionPools
     * List QuestionPools visible to the caller, page by page, with filtering and sorting.
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
        value = QuestionPoolApi.PATH_LIST_QUESTION_POOLS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuestionPool>> listQuestionPools(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_QUESTION_POOL = "/v1/lms/question-pools/{questionPoolId}";
    /**
     * PATCH /v1/lms/question-pools/{questionPoolId} : Update QuestionPool
     * Partially update a QuestionPool (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param questionPoolId QuestionPool id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionPoolUpdate  (required)
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
        value = QuestionPoolApi.PATH_UPDATE_QUESTION_POOL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionPool> updateQuestionPool(
         @PathVariable("questionPoolId") UUID questionPoolId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionPoolUpdate questionPoolUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_QUESTION_POOL_ITEM = "/v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId}";
    /**
     * PATCH /v1/lms/question-pools/{questionPoolId}/items/{questionPoolItemId} : Update QuestionPoolItem
     * Partially update a QuestionPoolItem (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param questionPoolId QuestionPool id. (required)
     * @param questionPoolItemId QuestionPoolItem id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionPoolItemUpdate  (required)
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
        value = QuestionPoolApi.PATH_UPDATE_QUESTION_POOL_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionPoolItem> updateQuestionPoolItem(
         @PathVariable("questionPoolId") UUID questionPoolId,
         @PathVariable("questionPoolItemId") UUID questionPoolItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionPoolItemUpdate questionPoolItemUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
