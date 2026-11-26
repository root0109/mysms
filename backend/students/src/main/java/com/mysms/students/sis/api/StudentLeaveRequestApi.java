// Owned API contract of sis.
package com.mysms.students.sis.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.students.sis.api.model.StudentLeaveDecisionRequest;
import com.mysms.students.sis.api.model.StudentLeaveRequest;
import com.mysms.students.sis.api.model.StudentLeaveRequestUpdate;
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
public interface StudentLeaveRequestApi {

    String PATH_APPROVE_STUDENT_LEAVE_REQUEST = "/v1/sis/student-leave-requests/{studentLeaveRequestId}:approve";
    /**
     * POST /v1/sis/student-leave-requests/{studentLeaveRequestId}:approve : Approve a student leave request
     * Approves the request and records who decided and why. Approved leave is linked from attendance records.
     *
     * @param studentLeaveRequestId  (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param studentLeaveDecisionRequest  (optional)
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
        value = StudentLeaveRequestApi.PATH_APPROVE_STUDENT_LEAVE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentLeaveRequest> approveStudentLeaveRequest(
         @PathVariable("studentLeaveRequestId") UUID studentLeaveRequestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestBody(required = false) @Nullable StudentLeaveDecisionRequest studentLeaveDecisionRequest
    );


    String PATH_CREATE_STUDENT_LEAVE_REQUEST = "/v1/sis/student-leave-requests";
    /**
     * POST /v1/sis/student-leave-requests : Create StudentLeaveRequest
     * Create a StudentLeaveRequest. Supports Idempotency-Key and validateOnly.
     *
     * @param studentLeaveRequest  (required)
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
        value = StudentLeaveRequestApi.PATH_CREATE_STUDENT_LEAVE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentLeaveRequest> createStudentLeaveRequest(
         @Valid @RequestBody StudentLeaveRequest studentLeaveRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_STUDENT_LEAVE_REQUEST = "/v1/sis/student-leave-requests/{studentLeaveRequestId}";
    /**
     * DELETE /v1/sis/student-leave-requests/{studentLeaveRequestId} : Delete StudentLeaveRequest
     * Soft delete (sets deleted_at).
     *
     * @param studentLeaveRequestId StudentLeaveRequest id. (required)
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
        value = StudentLeaveRequestApi.PATH_DELETE_STUDENT_LEAVE_REQUEST,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStudentLeaveRequest(
         @PathVariable("studentLeaveRequestId") UUID studentLeaveRequestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_STUDENT_LEAVE_REQUEST = "/v1/sis/student-leave-requests/{studentLeaveRequestId}";
    /**
     * GET /v1/sis/student-leave-requests/{studentLeaveRequestId} : Get StudentLeaveRequest
     * Get one StudentLeaveRequest by id. The ETag header carries the row version.
     *
     * @param studentLeaveRequestId StudentLeaveRequest id. (required)
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
        value = StudentLeaveRequestApi.PATH_GET_STUDENT_LEAVE_REQUEST,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StudentLeaveRequest> getStudentLeaveRequest(
         @PathVariable("studentLeaveRequestId") UUID studentLeaveRequestId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STUDENT_LEAVE_REQUESTS = "/v1/sis/student-leave-requests";
    /**
     * GET /v1/sis/student-leave-requests : List StudentLeaveRequests
     * List StudentLeaveRequests visible to the caller, page by page, with filtering and sorting.
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
        value = StudentLeaveRequestApi.PATH_LIST_STUDENT_LEAVE_REQUESTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StudentLeaveRequest>> listStudentLeaveRequests(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REJECT_STUDENT_LEAVE_REQUEST = "/v1/sis/student-leave-requests/{studentLeaveRequestId}:reject";
    /**
     * POST /v1/sis/student-leave-requests/{studentLeaveRequestId}:reject : Reject a student leave request
     * Rejects the request and records who decided and why. Approved leave is linked from attendance records.
     *
     * @param studentLeaveRequestId  (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param studentLeaveDecisionRequest  (optional)
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
        value = StudentLeaveRequestApi.PATH_REJECT_STUDENT_LEAVE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentLeaveRequest> rejectStudentLeaveRequest(
         @PathVariable("studentLeaveRequestId") UUID studentLeaveRequestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestBody(required = false) @Nullable StudentLeaveDecisionRequest studentLeaveDecisionRequest
    );


    String PATH_UPDATE_STUDENT_LEAVE_REQUEST = "/v1/sis/student-leave-requests/{studentLeaveRequestId}";
    /**
     * PATCH /v1/sis/student-leave-requests/{studentLeaveRequestId} : Update StudentLeaveRequest
     * Partially update a StudentLeaveRequest (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param studentLeaveRequestId StudentLeaveRequest id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param studentLeaveRequestUpdate  (required)
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
        value = StudentLeaveRequestApi.PATH_UPDATE_STUDENT_LEAVE_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentLeaveRequest> updateStudentLeaveRequest(
         @PathVariable("studentLeaveRequestId") UUID studentLeaveRequestId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StudentLeaveRequestUpdate studentLeaveRequestUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
