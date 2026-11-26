// Owned API contract of hr.
package com.mysms.hrms.hr.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.hrms.hr.api.model.ApproveLeaveRequest;
import com.mysms.hrms.hr.api.model.CancelLeaveRequest;
import com.mysms.hrms.hr.api.model.LeaveApplication;
import com.mysms.hrms.hr.api.model.LeaveApplicationUpdate;
import org.springframework.lang.Nullable;
import com.mysms.hrms.hr.api.model.RejectLeaveRequest;
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
public interface LeaveApplicationApi {

    String PATH_APPROVE_LEAVE = "/v1/hr/leave-applications/{leaveApplicationId}:approve";
    /**
     * POST /v1/hr/leave-applications/{leaveApplicationId}:approve : Approve
     * Approve.
     *
     * @param leaveApplicationId  (required)
     * @param approveLeaveRequest  (required)
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
        value = LeaveApplicationApi.PATH_APPROVE_LEAVE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LeaveApplication> approveLeave(
         @PathVariable("leaveApplicationId") UUID leaveApplicationId,
         @Valid @RequestBody ApproveLeaveRequest approveLeaveRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CANCEL_LEAVE = "/v1/hr/leave-applications/{leaveApplicationId}:cancel";
    /**
     * POST /v1/hr/leave-applications/{leaveApplicationId}:cancel : Withdraw by applicant
     * Withdraw by applicant.
     *
     * @param leaveApplicationId  (required)
     * @param cancelLeaveRequest  (required)
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
        value = LeaveApplicationApi.PATH_CANCEL_LEAVE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LeaveApplication> cancelLeave(
         @PathVariable("leaveApplicationId") UUID leaveApplicationId,
         @Valid @RequestBody CancelLeaveRequest cancelLeaveRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_LEAVE_APPLICATION = "/v1/hr/leave-applications";
    /**
     * POST /v1/hr/leave-applications : Create LeaveApplication
     * Create a LeaveApplication. Supports Idempotency-Key and validateOnly.
     *
     * @param leaveApplication  (required)
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
        value = LeaveApplicationApi.PATH_CREATE_LEAVE_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LeaveApplication> createLeaveApplication(
         @Valid @RequestBody LeaveApplication leaveApplication,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_LEAVE_APPLICATION = "/v1/hr/leave-applications/{leaveApplicationId}";
    /**
     * DELETE /v1/hr/leave-applications/{leaveApplicationId} : Delete LeaveApplication
     * Soft delete (sets deleted_at).
     *
     * @param leaveApplicationId LeaveApplication id. (required)
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
        value = LeaveApplicationApi.PATH_DELETE_LEAVE_APPLICATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLeaveApplication(
         @PathVariable("leaveApplicationId") UUID leaveApplicationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_LEAVE_APPLICATION = "/v1/hr/leave-applications/{leaveApplicationId}";
    /**
     * GET /v1/hr/leave-applications/{leaveApplicationId} : Get LeaveApplication
     * Get one LeaveApplication by id. The ETag header carries the row version.
     *
     * @param leaveApplicationId LeaveApplication id. (required)
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
        value = LeaveApplicationApi.PATH_GET_LEAVE_APPLICATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LeaveApplication> getLeaveApplication(
         @PathVariable("leaveApplicationId") UUID leaveApplicationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LEAVE_APPLICATIONS = "/v1/hr/leave-applications";
    /**
     * GET /v1/hr/leave-applications : List LeaveApplications
     * List LeaveApplications visible to the caller, page by page, with filtering and sorting.
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
        value = LeaveApplicationApi.PATH_LIST_LEAVE_APPLICATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LeaveApplication>> listLeaveApplications(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REJECT_LEAVE = "/v1/hr/leave-applications/{leaveApplicationId}:reject";
    /**
     * POST /v1/hr/leave-applications/{leaveApplicationId}:reject : Reject
     * Reject.
     *
     * @param leaveApplicationId  (required)
     * @param rejectLeaveRequest  (required)
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
        value = LeaveApplicationApi.PATH_REJECT_LEAVE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LeaveApplication> rejectLeave(
         @PathVariable("leaveApplicationId") UUID leaveApplicationId,
         @Valid @RequestBody RejectLeaveRequest rejectLeaveRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_LEAVE_APPLICATION = "/v1/hr/leave-applications/{leaveApplicationId}";
    /**
     * PATCH /v1/hr/leave-applications/{leaveApplicationId} : Update LeaveApplication
     * Partially update a LeaveApplication (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param leaveApplicationId LeaveApplication id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param leaveApplicationUpdate  (required)
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
        value = LeaveApplicationApi.PATH_UPDATE_LEAVE_APPLICATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LeaveApplication> updateLeaveApplication(
         @PathVariable("leaveApplicationId") UUID leaveApplicationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LeaveApplicationUpdate leaveApplicationUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
