// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.model.AcceptGroupRequestRequest;
import com.mysms.learning.lms.api.model.AssignmentGroupMember;
import com.mysms.learning.lms.api.model.AssignmentGroupMemberUpdate;
import org.springframework.lang.Nullable;
import com.mysms.learning.lms.api.model.RejectGroupRequestRequest;
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
public interface AssignmentGroupMemberApi {

    String PATH_ACCEPT_GROUP_REQUEST = "/v1/lms/assignment-group-members/{assignmentGroupMemberId}:accept";
    /**
     * POST /v1/lms/assignment-group-members/{assignmentGroupMemberId}:accept : Accept a join request
     * Accept a join request.
     *
     * @param assignmentGroupMemberId  (required)
     * @param acceptGroupRequestRequest  (required)
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
        value = AssignmentGroupMemberApi.PATH_ACCEPT_GROUP_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentGroupMember> acceptGroupRequest(
         @PathVariable("assignmentGroupMemberId") UUID assignmentGroupMemberId,
         @Valid @RequestBody AcceptGroupRequestRequest acceptGroupRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_ASSIGNMENT_GROUP_MEMBER = "/v1/lms/assignment-group-members";
    /**
     * POST /v1/lms/assignment-group-members : Create AssignmentGroupMember
     * Create a AssignmentGroupMember. Supports Idempotency-Key and validateOnly.
     *
     * @param assignmentGroupMember  (required)
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
        value = AssignmentGroupMemberApi.PATH_CREATE_ASSIGNMENT_GROUP_MEMBER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentGroupMember> createAssignmentGroupMember(
         @Valid @RequestBody AssignmentGroupMember assignmentGroupMember,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ASSIGNMENT_GROUP_MEMBER = "/v1/lms/assignment-group-members/{assignmentGroupMemberId}";
    /**
     * DELETE /v1/lms/assignment-group-members/{assignmentGroupMemberId} : Delete AssignmentGroupMember
     * Soft delete (sets deleted_at).
     *
     * @param assignmentGroupMemberId AssignmentGroupMember id. (required)
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
        value = AssignmentGroupMemberApi.PATH_DELETE_ASSIGNMENT_GROUP_MEMBER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAssignmentGroupMember(
         @PathVariable("assignmentGroupMemberId") UUID assignmentGroupMemberId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_ASSIGNMENT_GROUP_MEMBER = "/v1/lms/assignment-group-members/{assignmentGroupMemberId}";
    /**
     * GET /v1/lms/assignment-group-members/{assignmentGroupMemberId} : Get AssignmentGroupMember
     * Get one AssignmentGroupMember by id. The ETag header carries the row version.
     *
     * @param assignmentGroupMemberId AssignmentGroupMember id. (required)
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
        value = AssignmentGroupMemberApi.PATH_GET_ASSIGNMENT_GROUP_MEMBER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AssignmentGroupMember> getAssignmentGroupMember(
         @PathVariable("assignmentGroupMemberId") UUID assignmentGroupMemberId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ASSIGNMENT_GROUP_MEMBERS = "/v1/lms/assignment-group-members";
    /**
     * GET /v1/lms/assignment-group-members : List AssignmentGroupMembers
     * List AssignmentGroupMembers visible to the caller, page by page, with filtering and sorting.
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
        value = AssignmentGroupMemberApi.PATH_LIST_ASSIGNMENT_GROUP_MEMBERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AssignmentGroupMember>> listAssignmentGroupMembers(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REJECT_GROUP_REQUEST = "/v1/lms/assignment-group-members/{assignmentGroupMemberId}:reject";
    /**
     * POST /v1/lms/assignment-group-members/{assignmentGroupMemberId}:reject : Reject a join request
     * Reject a join request.
     *
     * @param assignmentGroupMemberId  (required)
     * @param rejectGroupRequestRequest  (required)
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
        value = AssignmentGroupMemberApi.PATH_REJECT_GROUP_REQUEST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentGroupMember> rejectGroupRequest(
         @PathVariable("assignmentGroupMemberId") UUID assignmentGroupMemberId,
         @Valid @RequestBody RejectGroupRequestRequest rejectGroupRequestRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_ASSIGNMENT_GROUP_MEMBER = "/v1/lms/assignment-group-members/{assignmentGroupMemberId}";
    /**
     * PATCH /v1/lms/assignment-group-members/{assignmentGroupMemberId} : Update AssignmentGroupMember
     * Partially update a AssignmentGroupMember (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param assignmentGroupMemberId AssignmentGroupMember id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param assignmentGroupMemberUpdate  (required)
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
        value = AssignmentGroupMemberApi.PATH_UPDATE_ASSIGNMENT_GROUP_MEMBER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentGroupMember> updateAssignmentGroupMember(
         @PathVariable("assignmentGroupMemberId") UUID assignmentGroupMemberId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AssignmentGroupMemberUpdate assignmentGroupMemberUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
