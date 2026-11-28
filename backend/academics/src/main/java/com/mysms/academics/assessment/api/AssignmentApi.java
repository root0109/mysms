// Owned API contract of assessment.
package com.mysms.academics.assessment.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.assessment.api.model.ApproveAssignmentRequest;
import com.mysms.academics.assessment.api.model.Assignment;
import com.mysms.academics.assessment.api.model.AssignmentAllocation;
import com.mysms.academics.assessment.api.model.AssignmentAllocationUpdate;
import com.mysms.academics.assessment.api.model.AssignmentApproval;
import com.mysms.academics.assessment.api.model.AssignmentSubject;
import com.mysms.academics.assessment.api.model.AssignmentSubjectUpdate;
import com.mysms.academics.assessment.api.model.AssignmentUpdate;
import com.mysms.academics.assessment.api.model.CancelAssignmentRequest;
import com.mysms.academics.assessment.api.model.FinishAssignmentRequest;
import org.springframework.lang.Nullable;
import com.mysms.academics.assessment.api.model.PublishAssignmentRequest;
import com.mysms.academics.assessment.api.model.RejectAssignmentRequest;
import com.mysms.academics.assessment.api.model.RequestAssignmentApprovalRequest;
import com.mysms.academics.assessment.api.model.ResetAssignmentRequest;
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
public interface AssignmentApi {

    String PATH_APPROVE_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}:approve";
    /**
     * POST /v1/assessment/assignments/{assignmentId}:approve : Approve
     * Approve.
     *
     * @param assignmentId  (required)
     * @param approveAssignmentRequest  (required)
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
        value = AssignmentApi.PATH_APPROVE_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> approveAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody ApproveAssignmentRequest approveAssignmentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CANCEL_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}:cancel";
    /**
     * POST /v1/assessment/assignments/{assignmentId}:cancel : Cancel
     * Cancel.
     *
     * @param assignmentId  (required)
     * @param cancelAssignmentRequest  (required)
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
        value = AssignmentApi.PATH_CANCEL_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> cancelAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody CancelAssignmentRequest cancelAssignmentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_ASSIGNMENT = "/v1/assessment/assignments";
    /**
     * POST /v1/assessment/assignments : Create Assignment
     * Create a Assignment. Supports Idempotency-Key and validateOnly.
     *
     * @param assignment  (required)
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
        value = AssignmentApi.PATH_CREATE_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> createAssignment(
         @Valid @RequestBody Assignment assignment,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_ASSIGNMENT_ALLOCATION = "/v1/assessment/assignments/{assignmentId}/allocations";
    /**
     * POST /v1/assessment/assignments/{assignmentId}/allocations : Create AssignmentAllocation
     * Create a AssignmentAllocation. Supports Idempotency-Key and validateOnly.
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentAllocation  (required)
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
        value = AssignmentApi.PATH_CREATE_ASSIGNMENT_ALLOCATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentAllocation> createAssignmentAllocation(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody AssignmentAllocation assignmentAllocation,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_ASSIGNMENT_SUBJECT = "/v1/assessment/assignments/{assignmentId}/subjects";
    /**
     * POST /v1/assessment/assignments/{assignmentId}/subjects : Create AssignmentSubject
     * Create a AssignmentSubject. Supports Idempotency-Key and validateOnly.
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentSubject  (required)
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
        value = AssignmentApi.PATH_CREATE_ASSIGNMENT_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubject> createAssignmentSubject(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody AssignmentSubject assignmentSubject,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}";
    /**
     * DELETE /v1/assessment/assignments/{assignmentId} : Delete Assignment
     * Soft delete (sets deleted_at).
     *
     * @param assignmentId Assignment id. (required)
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
        value = AssignmentApi.PATH_DELETE_ASSIGNMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ASSIGNMENT_ALLOCATION = "/v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}";
    /**
     * DELETE /v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId} : Delete AssignmentAllocation
     * Soft delete (sets deleted_at).
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentAllocationId AssignmentAllocation id. (required)
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
        value = AssignmentApi.PATH_DELETE_ASSIGNMENT_ALLOCATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAssignmentAllocation(
         @PathVariable("assignmentId") UUID assignmentId,
         @PathVariable("assignmentAllocationId") UUID assignmentAllocationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ASSIGNMENT_SUBJECT = "/v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}";
    /**
     * DELETE /v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId} : Delete AssignmentSubject
     * Soft delete (sets deleted_at).
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentSubjectId AssignmentSubject id. (required)
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
        value = AssignmentApi.PATH_DELETE_ASSIGNMENT_SUBJECT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAssignmentSubject(
         @PathVariable("assignmentId") UUID assignmentId,
         @PathVariable("assignmentSubjectId") UUID assignmentSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_FINISH_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}:finish";
    /**
     * POST /v1/assessment/assignments/{assignmentId}:finish : Close for submissions
     * Close for submissions.
     *
     * @param assignmentId  (required)
     * @param finishAssignmentRequest  (required)
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
        value = AssignmentApi.PATH_FINISH_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> finishAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody FinishAssignmentRequest finishAssignmentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}";
    /**
     * GET /v1/assessment/assignments/{assignmentId} : Get Assignment
     * Get one Assignment by id. The ETag header carries the row version.
     *
     * @param assignmentId Assignment id. (required)
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
        value = AssignmentApi.PATH_GET_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Assignment> getAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ASSIGNMENT_ALLOCATION = "/v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}";
    /**
     * GET /v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId} : Get AssignmentAllocation
     * Get one AssignmentAllocation by id. The ETag header carries the row version.
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentAllocationId AssignmentAllocation id. (required)
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
        value = AssignmentApi.PATH_GET_ASSIGNMENT_ALLOCATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AssignmentAllocation> getAssignmentAllocation(
         @PathVariable("assignmentId") UUID assignmentId,
         @PathVariable("assignmentAllocationId") UUID assignmentAllocationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ASSIGNMENT_APPROVAL = "/v1/assessment/assignments/{assignmentId}/approvals/{assignmentApprovalId}";
    /**
     * GET /v1/assessment/assignments/{assignmentId}/approvals/{assignmentApprovalId} : Get AssignmentApproval
     * Get one AssignmentApproval by id. The ETag header carries the row version.
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentApprovalId AssignmentApproval id. (required)
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
        value = AssignmentApi.PATH_GET_ASSIGNMENT_APPROVAL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AssignmentApproval> getAssignmentApproval(
         @PathVariable("assignmentId") UUID assignmentId,
         @PathVariable("assignmentApprovalId") UUID assignmentApprovalId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ASSIGNMENT_SUBJECT = "/v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}";
    /**
     * GET /v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId} : Get AssignmentSubject
     * Get one AssignmentSubject by id. The ETag header carries the row version.
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentSubjectId AssignmentSubject id. (required)
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
        value = AssignmentApi.PATH_GET_ASSIGNMENT_SUBJECT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AssignmentSubject> getAssignmentSubject(
         @PathVariable("assignmentId") UUID assignmentId,
         @PathVariable("assignmentSubjectId") UUID assignmentSubjectId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ASSIGNMENT_ALLOCATIONS = "/v1/assessment/assignments/{assignmentId}/allocations";
    /**
     * GET /v1/assessment/assignments/{assignmentId}/allocations : List AssignmentAllocations
     * List AssignmentAllocations visible to the caller, page by page, with filtering and sorting.
     *
     * @param assignmentId Assignment id. (required)
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
        value = AssignmentApi.PATH_LIST_ASSIGNMENT_ALLOCATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AssignmentAllocation>> listAssignmentAllocations(
         @PathVariable("assignmentId") UUID assignmentId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ASSIGNMENT_APPROVALS = "/v1/assessment/assignments/{assignmentId}/approvals";
    /**
     * GET /v1/assessment/assignments/{assignmentId}/approvals : List AssignmentApprovals
     * List AssignmentApprovals visible to the caller, page by page, with filtering and sorting.
     *
     * @param assignmentId Assignment id. (required)
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
        value = AssignmentApi.PATH_LIST_ASSIGNMENT_APPROVALS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AssignmentApproval>> listAssignmentApprovals(
         @PathVariable("assignmentId") UUID assignmentId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ASSIGNMENT_SUBJECTS = "/v1/assessment/assignments/{assignmentId}/subjects";
    /**
     * GET /v1/assessment/assignments/{assignmentId}/subjects : List AssignmentSubjects
     * List AssignmentSubjects visible to the caller, page by page, with filtering and sorting.
     *
     * @param assignmentId Assignment id. (required)
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
        value = AssignmentApi.PATH_LIST_ASSIGNMENT_SUBJECTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AssignmentSubject>> listAssignmentSubjects(
         @PathVariable("assignmentId") UUID assignmentId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ASSIGNMENTS = "/v1/assessment/assignments";
    /**
     * GET /v1/assessment/assignments : List Assignments
     * List Assignments visible to the caller, page by page, with filtering and sorting.
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
        value = AssignmentApi.PATH_LIST_ASSIGNMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Assignment>> listAssignments(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PUBLISH_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}:publish";
    /**
     * POST /v1/assessment/assignments/{assignmentId}:publish : Publish (status published; becomes ongoing automatically at issued_on)
     * Publish (status published; becomes ongoing automatically at issued_on).
     *
     * @param assignmentId  (required)
     * @param publishAssignmentRequest  (required)
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
        value = AssignmentApi.PATH_PUBLISH_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> publishAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody PublishAssignmentRequest publishAssignmentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REJECT_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}:reject";
    /**
     * POST /v1/assessment/assignments/{assignmentId}:reject : Reject
     * Reject.
     *
     * @param assignmentId  (required)
     * @param rejectAssignmentRequest  (required)
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
        value = AssignmentApi.PATH_REJECT_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> rejectAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody RejectAssignmentRequest rejectAssignmentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REQUEST_ASSIGNMENT_APPROVAL = "/v1/assessment/assignments/{assignmentId}:requestApproval";
    /**
     * POST /v1/assessment/assignments/{assignmentId}:requestApproval : Request approval
     * Send a draft for review: a pending approval is recorded in the caller's name.
     *
     * @param assignmentId  (required)
     * @param requestAssignmentApprovalRequest  (required)
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
        value = AssignmentApi.PATH_REQUEST_ASSIGNMENT_APPROVAL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> requestAssignmentApproval(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody RequestAssignmentApprovalRequest requestAssignmentApprovalRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}:resetToDraft";
    /**
     * POST /v1/assessment/assignments/{assignmentId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param assignmentId  (required)
     * @param resetAssignmentRequest  (required)
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
        value = AssignmentApi.PATH_RESET_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> resetAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
         @Valid @RequestBody ResetAssignmentRequest resetAssignmentRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_ASSIGNMENT = "/v1/assessment/assignments/{assignmentId}";
    /**
     * PATCH /v1/assessment/assignments/{assignmentId} : Update Assignment
     * Partially update a Assignment (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param assignmentId Assignment id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param assignmentUpdate  (required)
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
        value = AssignmentApi.PATH_UPDATE_ASSIGNMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Assignment> updateAssignment(
         @PathVariable("assignmentId") UUID assignmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AssignmentUpdate assignmentUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ASSIGNMENT_ALLOCATION = "/v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId}";
    /**
     * PATCH /v1/assessment/assignments/{assignmentId}/allocations/{assignmentAllocationId} : Update AssignmentAllocation
     * Partially update a AssignmentAllocation (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentAllocationId AssignmentAllocation id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param assignmentAllocationUpdate  (required)
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
        value = AssignmentApi.PATH_UPDATE_ASSIGNMENT_ALLOCATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentAllocation> updateAssignmentAllocation(
         @PathVariable("assignmentId") UUID assignmentId,
         @PathVariable("assignmentAllocationId") UUID assignmentAllocationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AssignmentAllocationUpdate assignmentAllocationUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ASSIGNMENT_SUBJECT = "/v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId}";
    /**
     * PATCH /v1/assessment/assignments/{assignmentId}/subjects/{assignmentSubjectId} : Update AssignmentSubject
     * Partially update a AssignmentSubject (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param assignmentId Assignment id. (required)
     * @param assignmentSubjectId AssignmentSubject id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param assignmentSubjectUpdate  (required)
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
        value = AssignmentApi.PATH_UPDATE_ASSIGNMENT_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentSubject> updateAssignmentSubject(
         @PathVariable("assignmentId") UUID assignmentId,
         @PathVariable("assignmentSubjectId") UUID assignmentSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AssignmentSubjectUpdate assignmentSubjectUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
