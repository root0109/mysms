// Owned API contract of assessment.
package com.mysms.academics.assessment.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.assessment.api.model.ApproveHomeworkRequest;
import com.mysms.academics.assessment.api.model.Homework;
import com.mysms.academics.assessment.api.model.HomeworkApproval;
import com.mysms.academics.assessment.api.model.HomeworkUpdate;
import org.springframework.lang.Nullable;
import com.mysms.academics.assessment.api.model.PublishHomeworkRequest;
import com.mysms.academics.assessment.api.model.RejectHomeworkRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Validated
public interface HomeworkApi {

    String PATH_APPROVE_HOMEWORK = "/v1/assessment/homeworks/{homeworkId}:approve";
    /**
     * POST /v1/assessment/homeworks/{homeworkId}:approve : Approve
     * Approve.
     *
     * @param homeworkId  (required)
     * @param approveHomeworkRequest  (required)
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
        value = HomeworkApi.PATH_APPROVE_HOMEWORK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Homework> approveHomework(
         @PathVariable("homeworkId") UUID homeworkId,
         @Valid @RequestBody ApproveHomeworkRequest approveHomeworkRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_HOMEWORK = "/v1/assessment/homeworks";
    /**
     * POST /v1/assessment/homeworks : Create Homework
     * Create a Homework. Supports Idempotency-Key and validateOnly.
     *
     * @param homework  (required)
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
        value = HomeworkApi.PATH_CREATE_HOMEWORK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Homework> createHomework(
         @Valid @RequestBody Homework homework,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_HOMEWORK = "/v1/assessment/homeworks/{homeworkId}";
    /**
     * DELETE /v1/assessment/homeworks/{homeworkId} : Delete Homework
     * Soft delete (sets deleted_at).
     *
     * @param homeworkId Homework id. (required)
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
        value = HomeworkApi.PATH_DELETE_HOMEWORK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteHomework(
         @PathVariable("homeworkId") UUID homeworkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_HOMEWORK = "/v1/assessment/homeworks/{homeworkId}";
    /**
     * GET /v1/assessment/homeworks/{homeworkId} : Get Homework
     * Get one Homework by id. The ETag header carries the row version.
     *
     * @param homeworkId Homework id. (required)
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
        value = HomeworkApi.PATH_GET_HOMEWORK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Homework> getHomework(
         @PathVariable("homeworkId") UUID homeworkId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_HOMEWORK_APPROVAL = "/v1/assessment/homeworks/{homeworkId}/approvals/{homeworkApprovalId}";
    /**
     * GET /v1/assessment/homeworks/{homeworkId}/approvals/{homeworkApprovalId} : Get HomeworkApproval
     * Get one HomeworkApproval by id. The ETag header carries the row version.
     *
     * @param homeworkId Homework id. (required)
     * @param homeworkApprovalId HomeworkApproval id. (required)
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
        value = HomeworkApi.PATH_GET_HOMEWORK_APPROVAL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<HomeworkApproval> getHomeworkApproval(
         @PathVariable("homeworkId") UUID homeworkId,
         @PathVariable("homeworkApprovalId") UUID homeworkApprovalId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_HOMEWORK_APPROVALS = "/v1/assessment/homeworks/{homeworkId}/approvals";
    /**
     * GET /v1/assessment/homeworks/{homeworkId}/approvals : List HomeworkApprovals
     * List HomeworkApprovals visible to the caller, page by page, with filtering and sorting.
     *
     * @param homeworkId Homework id. (required)
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
        value = HomeworkApi.PATH_LIST_HOMEWORK_APPROVALS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<HomeworkApproval>> listHomeworkApprovals(
         @PathVariable("homeworkId") UUID homeworkId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_HOMEWORKS = "/v1/assessment/homeworks";
    /**
     * GET /v1/assessment/homeworks : List Homeworks
     * List Homeworks visible to the caller, page by page, with filtering and sorting.
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
        value = HomeworkApi.PATH_LIST_HOMEWORKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Homework>> listHomeworks(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PUBLISH_HOMEWORK = "/v1/assessment/homeworks/{homeworkId}:publish";
    /**
     * POST /v1/assessment/homeworks/{homeworkId}:publish : Publish (or send for approval when approval is required)
     * Publish (or send for approval when approval is required).
     *
     * @param homeworkId  (required)
     * @param publishHomeworkRequest  (required)
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
        value = HomeworkApi.PATH_PUBLISH_HOMEWORK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Homework> publishHomework(
         @PathVariable("homeworkId") UUID homeworkId,
         @Valid @RequestBody PublishHomeworkRequest publishHomeworkRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REJECT_HOMEWORK = "/v1/assessment/homeworks/{homeworkId}:reject";
    /**
     * POST /v1/assessment/homeworks/{homeworkId}:reject : Reject
     * Reject.
     *
     * @param homeworkId  (required)
     * @param rejectHomeworkRequest  (required)
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
        value = HomeworkApi.PATH_REJECT_HOMEWORK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Homework> rejectHomework(
         @PathVariable("homeworkId") UUID homeworkId,
         @Valid @RequestBody RejectHomeworkRequest rejectHomeworkRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_HOMEWORK = "/v1/assessment/homeworks/{homeworkId}";
    /**
     * PATCH /v1/assessment/homeworks/{homeworkId} : Update Homework
     * Partially update a Homework (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param homeworkId Homework id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param homeworkUpdate  (required)
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
        value = HomeworkApi.PATH_UPDATE_HOMEWORK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Homework> updateHomework(
         @PathVariable("homeworkId") UUID homeworkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody HomeworkUpdate homeworkUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );



}
