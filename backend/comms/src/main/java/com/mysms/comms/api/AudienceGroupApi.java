// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.AddGroupMembersRequest;
import com.mysms.comms.api.model.AudienceGroup;
import com.mysms.comms.api.model.AudienceGroupMember;
import com.mysms.comms.api.model.AudienceGroupMemberList;
import com.mysms.comms.api.model.AudienceGroupMemberUpdate;
import com.mysms.comms.api.model.AudienceGroupUpdate;
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
public interface AudienceGroupApi {

    String PATH_ADD_GROUP_MEMBERS = "/v1/comms/audience-groups/{audienceGroupId}:addMembers";
    /**
     * POST /v1/comms/audience-groups/{audienceGroupId}:addMembers : Add many members
     * Add many members.
     *
     * @param audienceGroupId  (required)
     * @param addGroupMembersRequest  (required)
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
        value = AudienceGroupApi.PATH_ADD_GROUP_MEMBERS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AudienceGroupMemberList> addGroupMembers(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
         @Valid @RequestBody AddGroupMembersRequest addGroupMembersRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_AUDIENCE_GROUP = "/v1/comms/audience-groups";
    /**
     * POST /v1/comms/audience-groups : Create AudienceGroup
     * Create a AudienceGroup. Supports Idempotency-Key and validateOnly.
     *
     * @param audienceGroup  (required)
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
        value = AudienceGroupApi.PATH_CREATE_AUDIENCE_GROUP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AudienceGroup> createAudienceGroup(
         @Valid @RequestBody AudienceGroup audienceGroup,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_AUDIENCE_GROUP_MEMBER = "/v1/comms/audience-groups/{audienceGroupId}/members";
    /**
     * POST /v1/comms/audience-groups/{audienceGroupId}/members : Create AudienceGroupMember
     * Create a AudienceGroupMember. Supports Idempotency-Key and validateOnly.
     *
     * @param audienceGroupId AudienceGroup id. (required)
     * @param audienceGroupMember  (required)
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
        value = AudienceGroupApi.PATH_CREATE_AUDIENCE_GROUP_MEMBER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AudienceGroupMember> createAudienceGroupMember(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
         @Valid @RequestBody AudienceGroupMember audienceGroupMember,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_AUDIENCE_GROUP = "/v1/comms/audience-groups/{audienceGroupId}";
    /**
     * DELETE /v1/comms/audience-groups/{audienceGroupId} : Delete AudienceGroup
     * Soft delete (sets deleted_at).
     *
     * @param audienceGroupId AudienceGroup id. (required)
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
        value = AudienceGroupApi.PATH_DELETE_AUDIENCE_GROUP,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAudienceGroup(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_AUDIENCE_GROUP_MEMBER = "/v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}";
    /**
     * DELETE /v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId} : Delete AudienceGroupMember
     * Soft delete (sets deleted_at).
     *
     * @param audienceGroupId AudienceGroup id. (required)
     * @param audienceGroupMemberId AudienceGroupMember id. (required)
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
        value = AudienceGroupApi.PATH_DELETE_AUDIENCE_GROUP_MEMBER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAudienceGroupMember(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
         @PathVariable("audienceGroupMemberId") UUID audienceGroupMemberId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_AUDIENCE_GROUP = "/v1/comms/audience-groups/{audienceGroupId}";
    /**
     * GET /v1/comms/audience-groups/{audienceGroupId} : Get AudienceGroup
     * Get one AudienceGroup by id. The ETag header carries the row version.
     *
     * @param audienceGroupId AudienceGroup id. (required)
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
        value = AudienceGroupApi.PATH_GET_AUDIENCE_GROUP,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AudienceGroup> getAudienceGroup(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_AUDIENCE_GROUP_MEMBER = "/v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}";
    /**
     * GET /v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId} : Get AudienceGroupMember
     * Get one AudienceGroupMember by id. The ETag header carries the row version.
     *
     * @param audienceGroupId AudienceGroup id. (required)
     * @param audienceGroupMemberId AudienceGroupMember id. (required)
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
        value = AudienceGroupApi.PATH_GET_AUDIENCE_GROUP_MEMBER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AudienceGroupMember> getAudienceGroupMember(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
         @PathVariable("audienceGroupMemberId") UUID audienceGroupMemberId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_AUDIENCE_GROUP_MEMBERS = "/v1/comms/audience-groups/{audienceGroupId}/members";
    /**
     * GET /v1/comms/audience-groups/{audienceGroupId}/members : List AudienceGroupMembers
     * List AudienceGroupMembers visible to the caller, page by page, with filtering and sorting.
     *
     * @param audienceGroupId AudienceGroup id. (required)
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
        value = AudienceGroupApi.PATH_LIST_AUDIENCE_GROUP_MEMBERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AudienceGroupMember>> listAudienceGroupMembers(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_AUDIENCE_GROUPS = "/v1/comms/audience-groups";
    /**
     * GET /v1/comms/audience-groups : List AudienceGroups
     * List AudienceGroups visible to the caller, page by page, with filtering and sorting.
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
        value = AudienceGroupApi.PATH_LIST_AUDIENCE_GROUPS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AudienceGroup>> listAudienceGroups(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_AUDIENCE_GROUP = "/v1/comms/audience-groups/{audienceGroupId}";
    /**
     * PATCH /v1/comms/audience-groups/{audienceGroupId} : Update AudienceGroup
     * Partially update a AudienceGroup (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param audienceGroupId AudienceGroup id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param audienceGroupUpdate  (required)
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
        value = AudienceGroupApi.PATH_UPDATE_AUDIENCE_GROUP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AudienceGroup> updateAudienceGroup(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AudienceGroupUpdate audienceGroupUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_AUDIENCE_GROUP_MEMBER = "/v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId}";
    /**
     * PATCH /v1/comms/audience-groups/{audienceGroupId}/members/{audienceGroupMemberId} : Update AudienceGroupMember
     * Partially update a AudienceGroupMember (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param audienceGroupId AudienceGroup id. (required)
     * @param audienceGroupMemberId AudienceGroupMember id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param audienceGroupMemberUpdate  (required)
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
        value = AudienceGroupApi.PATH_UPDATE_AUDIENCE_GROUP_MEMBER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AudienceGroupMember> updateAudienceGroupMember(
         @PathVariable("audienceGroupId") UUID audienceGroupId,
         @PathVariable("audienceGroupMemberId") UUID audienceGroupMemberId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AudienceGroupMemberUpdate audienceGroupMemberUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
