// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.ClassPage;
import com.mysms.comms.api.model.ClassPageAttachment;
import com.mysms.comms.api.model.ClassPageAttachmentUpdate;
import com.mysms.comms.api.model.ClassPageMember;
import com.mysms.comms.api.model.ClassPageMemberUpdate;
import com.mysms.comms.api.model.ClassPageUpdate;
import com.mysms.comms.api.model.FollowClassPageRequest;
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
public interface ClassPageApi {

    String PATH_CREATE_CLASS_PAGE = "/v1/comms/class-pages";
    /**
     * POST /v1/comms/class-pages : Create ClassPage
     * Create a ClassPage. Supports Idempotency-Key and validateOnly.
     *
     * @param classPage  (required)
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
        value = ClassPageApi.PATH_CREATE_CLASS_PAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassPage> createClassPage(
         @Valid @RequestBody ClassPage classPage,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CLASS_PAGE_ATTACHMENT = "/v1/comms/class-pages/{classPageId}/attachments";
    /**
     * POST /v1/comms/class-pages/{classPageId}/attachments : Create ClassPageAttachment
     * Create a ClassPageAttachment. Supports Idempotency-Key and validateOnly.
     *
     * @param classPageId ClassPage id. (required)
     * @param classPageAttachment  (required)
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
        value = ClassPageApi.PATH_CREATE_CLASS_PAGE_ATTACHMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassPageAttachment> createClassPageAttachment(
         @PathVariable("classPageId") UUID classPageId,
         @Valid @RequestBody ClassPageAttachment classPageAttachment,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CLASS_PAGE_MEMBER = "/v1/comms/class-pages/{classPageId}/members";
    /**
     * POST /v1/comms/class-pages/{classPageId}/members : Create ClassPageMember
     * Create a ClassPageMember. Supports Idempotency-Key and validateOnly.
     *
     * @param classPageId ClassPage id. (required)
     * @param classPageMember  (required)
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
        value = ClassPageApi.PATH_CREATE_CLASS_PAGE_MEMBER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassPageMember> createClassPageMember(
         @PathVariable("classPageId") UUID classPageId,
         @Valid @RequestBody ClassPageMember classPageMember,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_CLASS_PAGE = "/v1/comms/class-pages/{classPageId}";
    /**
     * DELETE /v1/comms/class-pages/{classPageId} : Delete ClassPage
     * Soft delete (sets deleted_at).
     *
     * @param classPageId ClassPage id. (required)
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
        value = ClassPageApi.PATH_DELETE_CLASS_PAGE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteClassPage(
         @PathVariable("classPageId") UUID classPageId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CLASS_PAGE_ATTACHMENT = "/v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}";
    /**
     * DELETE /v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId} : Delete ClassPageAttachment
     * Soft delete (sets deleted_at).
     *
     * @param classPageId ClassPage id. (required)
     * @param classPageAttachmentId ClassPageAttachment id. (required)
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
        value = ClassPageApi.PATH_DELETE_CLASS_PAGE_ATTACHMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteClassPageAttachment(
         @PathVariable("classPageId") UUID classPageId,
         @PathVariable("classPageAttachmentId") UUID classPageAttachmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CLASS_PAGE_MEMBER = "/v1/comms/class-pages/{classPageId}/members/{classPageMemberId}";
    /**
     * DELETE /v1/comms/class-pages/{classPageId}/members/{classPageMemberId} : Delete ClassPageMember
     * Soft delete (sets deleted_at).
     *
     * @param classPageId ClassPage id. (required)
     * @param classPageMemberId ClassPageMember id. (required)
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
        value = ClassPageApi.PATH_DELETE_CLASS_PAGE_MEMBER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteClassPageMember(
         @PathVariable("classPageId") UUID classPageId,
         @PathVariable("classPageMemberId") UUID classPageMemberId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_FOLLOW_CLASS_PAGE = "/v1/comms/class-pages/{classPageId}:follow";
    /**
     * POST /v1/comms/class-pages/{classPageId}:follow : Follow or unfollow
     * Follow or unfollow.
     *
     * @param classPageId  (required)
     * @param followClassPageRequest  (required)
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
        value = ClassPageApi.PATH_FOLLOW_CLASS_PAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassPageMember> followClassPage(
         @PathVariable("classPageId") UUID classPageId,
         @Valid @RequestBody FollowClassPageRequest followClassPageRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_CLASS_PAGE = "/v1/comms/class-pages/{classPageId}";
    /**
     * GET /v1/comms/class-pages/{classPageId} : Get ClassPage
     * Get one ClassPage by id. The ETag header carries the row version.
     *
     * @param classPageId ClassPage id. (required)
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
        value = ClassPageApi.PATH_GET_CLASS_PAGE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ClassPage> getClassPage(
         @PathVariable("classPageId") UUID classPageId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CLASS_PAGE_ATTACHMENT = "/v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}";
    /**
     * GET /v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId} : Get ClassPageAttachment
     * Get one ClassPageAttachment by id. The ETag header carries the row version.
     *
     * @param classPageId ClassPage id. (required)
     * @param classPageAttachmentId ClassPageAttachment id. (required)
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
        value = ClassPageApi.PATH_GET_CLASS_PAGE_ATTACHMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ClassPageAttachment> getClassPageAttachment(
         @PathVariable("classPageId") UUID classPageId,
         @PathVariable("classPageAttachmentId") UUID classPageAttachmentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CLASS_PAGE_MEMBER = "/v1/comms/class-pages/{classPageId}/members/{classPageMemberId}";
    /**
     * GET /v1/comms/class-pages/{classPageId}/members/{classPageMemberId} : Get ClassPageMember
     * Get one ClassPageMember by id. The ETag header carries the row version.
     *
     * @param classPageId ClassPage id. (required)
     * @param classPageMemberId ClassPageMember id. (required)
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
        value = ClassPageApi.PATH_GET_CLASS_PAGE_MEMBER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ClassPageMember> getClassPageMember(
         @PathVariable("classPageId") UUID classPageId,
         @PathVariable("classPageMemberId") UUID classPageMemberId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CLASS_PAGE_ATTACHMENTS = "/v1/comms/class-pages/{classPageId}/attachments";
    /**
     * GET /v1/comms/class-pages/{classPageId}/attachments : List ClassPageAttachments
     * List ClassPageAttachments visible to the caller, page by page, with filtering and sorting.
     *
     * @param classPageId ClassPage id. (required)
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
        value = ClassPageApi.PATH_LIST_CLASS_PAGE_ATTACHMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ClassPageAttachment>> listClassPageAttachments(
         @PathVariable("classPageId") UUID classPageId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CLASS_PAGE_MEMBERS = "/v1/comms/class-pages/{classPageId}/members";
    /**
     * GET /v1/comms/class-pages/{classPageId}/members : List ClassPageMembers
     * List ClassPageMembers visible to the caller, page by page, with filtering and sorting.
     *
     * @param classPageId ClassPage id. (required)
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
        value = ClassPageApi.PATH_LIST_CLASS_PAGE_MEMBERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ClassPageMember>> listClassPageMembers(
         @PathVariable("classPageId") UUID classPageId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CLASS_PAGES = "/v1/comms/class-pages";
    /**
     * GET /v1/comms/class-pages : List ClassPages
     * List ClassPages visible to the caller, page by page, with filtering and sorting.
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
        value = ClassPageApi.PATH_LIST_CLASS_PAGES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ClassPage>> listClassPages(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_CLASS_PAGE = "/v1/comms/class-pages/{classPageId}";
    /**
     * PATCH /v1/comms/class-pages/{classPageId} : Update ClassPage
     * Partially update a ClassPage (updateMask). Requires If-Match with the current ETag.
     *
     * @param classPageId ClassPage id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param classPageUpdate  (required)
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
        value = ClassPageApi.PATH_UPDATE_CLASS_PAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassPage> updateClassPage(
         @PathVariable("classPageId") UUID classPageId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ClassPageUpdate classPageUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CLASS_PAGE_ATTACHMENT = "/v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId}";
    /**
     * PATCH /v1/comms/class-pages/{classPageId}/attachments/{classPageAttachmentId} : Update ClassPageAttachment
     * Partially update a ClassPageAttachment (updateMask). Requires If-Match with the current ETag.
     *
     * @param classPageId ClassPage id. (required)
     * @param classPageAttachmentId ClassPageAttachment id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param classPageAttachmentUpdate  (required)
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
        value = ClassPageApi.PATH_UPDATE_CLASS_PAGE_ATTACHMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassPageAttachment> updateClassPageAttachment(
         @PathVariable("classPageId") UUID classPageId,
         @PathVariable("classPageAttachmentId") UUID classPageAttachmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ClassPageAttachmentUpdate classPageAttachmentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CLASS_PAGE_MEMBER = "/v1/comms/class-pages/{classPageId}/members/{classPageMemberId}";
    /**
     * PATCH /v1/comms/class-pages/{classPageId}/members/{classPageMemberId} : Update ClassPageMember
     * Partially update a ClassPageMember (updateMask). Requires If-Match with the current ETag.
     *
     * @param classPageId ClassPage id. (required)
     * @param classPageMemberId ClassPageMember id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param classPageMemberUpdate  (required)
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
        value = ClassPageApi.PATH_UPDATE_CLASS_PAGE_MEMBER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ClassPageMember> updateClassPageMember(
         @PathVariable("classPageId") UUID classPageId,
         @PathVariable("classPageMemberId") UUID classPageMemberId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ClassPageMemberUpdate classPageMemberUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
