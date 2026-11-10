// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.GradeLearningSubmissionRequest;
import com.mysms.lms.api.model.LmsAssignmentSubmission;
import com.mysms.lms.api.model.LmsAssignmentSubmissionUpdate;
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
public interface LmsAssignmentSubmissionApi {

    String PATH_CREATE_LMS_ASSIGNMENT_SUBMISSION = "/v1/lms/assignment-submissions";
    /**
     * POST /v1/lms/assignment-submissions : Create LmsAssignmentSubmission
     * Create a LmsAssignmentSubmission. Supports Idempotency-Key and validateOnly.
     *
     * @param lmsAssignmentSubmission  (required)
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
        value = LmsAssignmentSubmissionApi.PATH_CREATE_LMS_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsAssignmentSubmission> createLmsAssignmentSubmission(
         @Valid @RequestBody LmsAssignmentSubmission lmsAssignmentSubmission,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_LMS_ASSIGNMENT_SUBMISSION = "/v1/lms/assignment-submissions/{assignmentSubmissionId}";
    /**
     * DELETE /v1/lms/assignment-submissions/{assignmentSubmissionId} : Delete LmsAssignmentSubmission
     * Soft delete (sets deleted_at).
     *
     * @param assignmentSubmissionId LmsAssignmentSubmission id. (required)
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
        value = LmsAssignmentSubmissionApi.PATH_DELETE_LMS_ASSIGNMENT_SUBMISSION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLmsAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_FINALIZE_SUBMISSION = "/v1/lms/assignment-submissions/{assignmentSubmissionId}:finalize";
    /**
     * POST /v1/lms/assignment-submissions/{assignmentSubmissionId}:finalize : Mark a (group) submission final; no further changes are accepted
     * Mark a (group) submission final; no further changes are accepted.
     *
     * @param assignmentSubmissionId  (required)
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
        value = LmsAssignmentSubmissionApi.PATH_FINALIZE_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsAssignmentSubmission> finalizeSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_LMS_ASSIGNMENT_SUBMISSION = "/v1/lms/assignment-submissions/{assignmentSubmissionId}";
    /**
     * GET /v1/lms/assignment-submissions/{assignmentSubmissionId} : Get LmsAssignmentSubmission
     * Get one LmsAssignmentSubmission by id. The ETag header carries the row version.
     *
     * @param assignmentSubmissionId LmsAssignmentSubmission id. (required)
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
        value = LmsAssignmentSubmissionApi.PATH_GET_LMS_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LmsAssignmentSubmission> getLmsAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GRADE_LEARNING_SUBMISSION = "/v1/lms/assignment-submissions/{assignmentSubmissionId}:grade";
    /**
     * POST /v1/lms/assignment-submissions/{assignmentSubmissionId}:grade : Evaluate a learning-assignment submission
     * Evaluate a learning-assignment submission.
     *
     * @param assignmentSubmissionId  (required)
     * @param gradeLearningSubmissionRequest  (required)
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
        value = LmsAssignmentSubmissionApi.PATH_GRADE_LEARNING_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsAssignmentSubmission> gradeLearningSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestBody GradeLearningSubmissionRequest gradeLearningSubmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_LMS_ASSIGNMENT_SUBMISSIONS = "/v1/lms/assignment-submissions";
    /**
     * GET /v1/lms/assignment-submissions : List LmsAssignmentSubmissions
     * List LmsAssignmentSubmissions visible to the caller, page by page, with filtering and sorting.
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
        value = LmsAssignmentSubmissionApi.PATH_LIST_LMS_ASSIGNMENT_SUBMISSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LmsAssignmentSubmission>> listLmsAssignmentSubmissions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_LMS_ASSIGNMENT_SUBMISSION = "/v1/lms/assignment-submissions/{assignmentSubmissionId}";
    /**
     * PATCH /v1/lms/assignment-submissions/{assignmentSubmissionId} : Update LmsAssignmentSubmission
     * Partially update a LmsAssignmentSubmission (updateMask). Requires If-Match with the current ETag.
     *
     * @param assignmentSubmissionId LmsAssignmentSubmission id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param lmsAssignmentSubmissionUpdate  (required)
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
        value = LmsAssignmentSubmissionApi.PATH_UPDATE_LMS_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LmsAssignmentSubmission> updateLmsAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LmsAssignmentSubmissionUpdate lmsAssignmentSubmissionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
