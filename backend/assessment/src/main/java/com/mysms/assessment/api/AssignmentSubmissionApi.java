// Owned API contract of assessment.
package com.mysms.assessment.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.api.model.AcceptAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.AssignmentSubmission;
import com.mysms.assessment.api.model.AssignmentSubmissionUpdate;
import com.mysms.assessment.api.model.GradeAssignmentSubmissionRequest;
import org.springframework.lang.Nullable;
import com.mysms.assessment.api.model.RejectAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.RequestSubmissionChangesRequest;
import com.mysms.assessment.api.model.ResetAssignmentSubmissionRequest;
import com.mysms.assessment.api.model.SubmitAssignmentSubmissionRequest;
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
public interface AssignmentSubmissionApi {

    String PATH_ACCEPT_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}:accept";
    /**
     * POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:accept : Accept
     * Accept.
     *
     * @param assignmentSubmissionId  (required)
     * @param acceptAssignmentSubmissionRequest  (required)
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
        value = AssignmentSubmissionApi.PATH_ACCEPT_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubmission> acceptAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestBody AcceptAssignmentSubmissionRequest acceptAssignmentSubmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions";
    /**
     * POST /v1/assessment/assignment-submissions : Create AssignmentSubmission
     * Create a AssignmentSubmission. Supports Idempotency-Key and validateOnly.
     *
     * @param assignmentSubmission  (required)
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
        value = AssignmentSubmissionApi.PATH_CREATE_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubmission> createAssignmentSubmission(
         @Valid @RequestBody AssignmentSubmission assignmentSubmission,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}";
    /**
     * DELETE /v1/assessment/assignment-submissions/{assignmentSubmissionId} : Delete AssignmentSubmission
     * Soft delete (sets deleted_at).
     *
     * @param assignmentSubmissionId AssignmentSubmission id. (required)
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
        value = AssignmentSubmissionApi.PATH_DELETE_ASSIGNMENT_SUBMISSION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}";
    /**
     * GET /v1/assessment/assignment-submissions/{assignmentSubmissionId} : Get AssignmentSubmission
     * Get one AssignmentSubmission by id. The ETag header carries the row version.
     *
     * @param assignmentSubmissionId AssignmentSubmission id. (required)
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
        value = AssignmentSubmissionApi.PATH_GET_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AssignmentSubmission> getAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GRADE_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}:grade";
    /**
     * POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:grade : Award marks
     * Award marks.
     *
     * @param assignmentSubmissionId  (required)
     * @param gradeAssignmentSubmissionRequest  (required)
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
        value = AssignmentSubmissionApi.PATH_GRADE_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubmission> gradeAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestBody GradeAssignmentSubmissionRequest gradeAssignmentSubmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_ASSIGNMENT_SUBMISSIONS = "/v1/assessment/assignment-submissions";
    /**
     * GET /v1/assessment/assignment-submissions : List AssignmentSubmissions
     * List AssignmentSubmissions visible to the caller, page by page, with filtering and sorting.
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
        value = AssignmentSubmissionApi.PATH_LIST_ASSIGNMENT_SUBMISSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AssignmentSubmission>> listAssignmentSubmissions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REJECT_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}:reject";
    /**
     * POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:reject : Reject
     * Reject.
     *
     * @param assignmentSubmissionId  (required)
     * @param rejectAssignmentSubmissionRequest  (required)
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
        value = AssignmentSubmissionApi.PATH_REJECT_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubmission> rejectAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestBody RejectAssignmentSubmissionRequest rejectAssignmentSubmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REQUEST_SUBMISSION_CHANGES = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}:requestChanges";
    /**
     * POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:requestChanges : Ask for changes
     * Ask for changes.
     *
     * @param assignmentSubmissionId  (required)
     * @param requestSubmissionChangesRequest  (required)
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
        value = AssignmentSubmissionApi.PATH_REQUEST_SUBMISSION_CHANGES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubmission> requestSubmissionChanges(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestBody RequestSubmissionChangesRequest requestSubmissionChangesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}:resetToDraft";
    /**
     * POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:resetToDraft : Change and resubmit
     * Change and resubmit.
     *
     * @param assignmentSubmissionId  (required)
     * @param resetAssignmentSubmissionRequest  (required)
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
        value = AssignmentSubmissionApi.PATH_RESET_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubmission> resetAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestBody ResetAssignmentSubmissionRequest resetAssignmentSubmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUBMIT_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}:submit";
    /**
     * POST /v1/assessment/assignment-submissions/{assignmentSubmissionId}:submit : Student submits
     * Student submits.
     *
     * @param assignmentSubmissionId  (required)
     * @param submitAssignmentSubmissionRequest  (required)
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
        value = AssignmentSubmissionApi.PATH_SUBMIT_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubmission> submitAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
         @Valid @RequestBody SubmitAssignmentSubmissionRequest submitAssignmentSubmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_ASSIGNMENT_SUBMISSION = "/v1/assessment/assignment-submissions/{assignmentSubmissionId}";
    /**
     * PATCH /v1/assessment/assignment-submissions/{assignmentSubmissionId} : Update AssignmentSubmission
     * Partially update a AssignmentSubmission (updateMask). Requires If-Match with the current ETag.
     *
     * @param assignmentSubmissionId AssignmentSubmission id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param assignmentSubmissionUpdate  (required)
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
        value = AssignmentSubmissionApi.PATH_UPDATE_ASSIGNMENT_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubmission> updateAssignmentSubmission(
         @PathVariable("assignmentSubmissionId") UUID assignmentSubmissionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AssignmentSubmissionUpdate assignmentSubmissionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
