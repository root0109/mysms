// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.CloseNoticeRepliesRequest;
import com.mysms.comms.api.model.DeactivateNoticeRequest;
import com.mysms.comms.api.model.Notice;
import com.mysms.comms.api.model.NoticeAudience;
import com.mysms.comms.api.model.NoticeAudienceUpdate;
import com.mysms.comms.api.model.NoticeUpdate;
import com.mysms.comms.api.model.NoticeVersion;
import org.springframework.lang.Nullable;
import com.mysms.comms.api.model.PublishNoticeRequest;
import com.mysms.comms.api.model.ReactivateNoticeRequest;
import java.util.UUID;
import com.mysms.comms.api.model.UnpublishNoticeRequest;
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
public interface NoticeApi {

    String PATH_CLOSE_NOTICE_REPLIES = "/v1/comms/notices/{noticeId}:closeReplies";
    /**
     * POST /v1/comms/notices/{noticeId}:closeReplies : Stop accepting replies
     * Stop accepting replies.
     *
     * @param noticeId  (required)
     * @param closeNoticeRepliesRequest  (required)
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
        value = NoticeApi.PATH_CLOSE_NOTICE_REPLIES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Notice> closeNoticeReplies(
         @PathVariable("noticeId") UUID noticeId,
         @Valid @RequestBody CloseNoticeRepliesRequest closeNoticeRepliesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_NOTICE = "/v1/comms/notices";
    /**
     * POST /v1/comms/notices : Create Notice
     * Create a Notice. Supports Idempotency-Key and validateOnly.
     *
     * @param notice  (required)
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
        value = NoticeApi.PATH_CREATE_NOTICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Notice> createNotice(
         @Valid @RequestBody Notice notice,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_NOTICE_AUDIENCE = "/v1/comms/notices/{noticeId}/audiences";
    /**
     * POST /v1/comms/notices/{noticeId}/audiences : Create NoticeAudience
     * Create a NoticeAudience. Supports Idempotency-Key and validateOnly.
     *
     * @param noticeId Notice id. (required)
     * @param noticeAudience  (required)
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
        value = NoticeApi.PATH_CREATE_NOTICE_AUDIENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NoticeAudience> createNoticeAudience(
         @PathVariable("noticeId") UUID noticeId,
         @Valid @RequestBody NoticeAudience noticeAudience,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DEACTIVATE_NOTICE = "/v1/comms/notices/{noticeId}:deactivate";
    /**
     * POST /v1/comms/notices/{noticeId}:deactivate : Withdraw a notice
     * Withdraw a notice.
     *
     * @param noticeId  (required)
     * @param deactivateNoticeRequest  (required)
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
        value = NoticeApi.PATH_DEACTIVATE_NOTICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Notice> deactivateNotice(
         @PathVariable("noticeId") UUID noticeId,
         @Valid @RequestBody DeactivateNoticeRequest deactivateNoticeRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DELETE_NOTICE = "/v1/comms/notices/{noticeId}";
    /**
     * DELETE /v1/comms/notices/{noticeId} : Delete Notice
     * Soft delete (sets deleted_at).
     *
     * @param noticeId Notice id. (required)
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
        value = NoticeApi.PATH_DELETE_NOTICE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNotice(
         @PathVariable("noticeId") UUID noticeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_NOTICE_AUDIENCE = "/v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}";
    /**
     * DELETE /v1/comms/notices/{noticeId}/audiences/{noticeAudienceId} : Delete NoticeAudience
     * Soft delete (sets deleted_at).
     *
     * @param noticeId Notice id. (required)
     * @param noticeAudienceId NoticeAudience id. (required)
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
        value = NoticeApi.PATH_DELETE_NOTICE_AUDIENCE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNoticeAudience(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeAudienceId") UUID noticeAudienceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_NOTICE = "/v1/comms/notices/{noticeId}";
    /**
     * GET /v1/comms/notices/{noticeId} : Get Notice
     * Get one Notice by id. The ETag header carries the row version.
     *
     * @param noticeId Notice id. (required)
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
        value = NoticeApi.PATH_GET_NOTICE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Notice> getNotice(
         @PathVariable("noticeId") UUID noticeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_NOTICE_AUDIENCE = "/v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}";
    /**
     * GET /v1/comms/notices/{noticeId}/audiences/{noticeAudienceId} : Get NoticeAudience
     * Get one NoticeAudience by id. The ETag header carries the row version.
     *
     * @param noticeId Notice id. (required)
     * @param noticeAudienceId NoticeAudience id. (required)
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
        value = NoticeApi.PATH_GET_NOTICE_AUDIENCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NoticeAudience> getNoticeAudience(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeAudienceId") UUID noticeAudienceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_NOTICE_VERSION = "/v1/comms/notices/{noticeId}/versions/{noticeVersionId}";
    /**
     * GET /v1/comms/notices/{noticeId}/versions/{noticeVersionId} : Get NoticeVersion
     * Get one NoticeVersion by id. The ETag header carries the row version.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersionId NoticeVersion id. (required)
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
        value = NoticeApi.PATH_GET_NOTICE_VERSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NoticeVersion> getNoticeVersion(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeVersionId") UUID noticeVersionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTICE_AUDIENCES = "/v1/comms/notices/{noticeId}/audiences";
    /**
     * GET /v1/comms/notices/{noticeId}/audiences : List NoticeAudiences
     * List NoticeAudiences visible to the caller, page by page, with filtering and sorting.
     *
     * @param noticeId Notice id. (required)
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
        value = NoticeApi.PATH_LIST_NOTICE_AUDIENCES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NoticeAudience>> listNoticeAudiences(
         @PathVariable("noticeId") UUID noticeId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTICE_VERSIONS = "/v1/comms/notices/{noticeId}/versions";
    /**
     * GET /v1/comms/notices/{noticeId}/versions : List NoticeVersions
     * List NoticeVersions visible to the caller, page by page, with filtering and sorting.
     *
     * @param noticeId Notice id. (required)
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
        value = NoticeApi.PATH_LIST_NOTICE_VERSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NoticeVersion>> listNoticeVersions(
         @PathVariable("noticeId") UUID noticeId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTICES = "/v1/comms/notices";
    /**
     * GET /v1/comms/notices : List Notices
     * List Notices visible to the caller, page by page, with filtering and sorting.
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
        value = NoticeApi.PATH_LIST_NOTICES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Notice>> listNotices(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PUBLISH_NOTICE = "/v1/comms/notices/{noticeId}:publish";
    /**
     * POST /v1/comms/notices/{noticeId}:publish : Publish
     * Publish.
     *
     * @param noticeId  (required)
     * @param publishNoticeRequest  (required)
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
        value = NoticeApi.PATH_PUBLISH_NOTICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Notice> publishNotice(
         @PathVariable("noticeId") UUID noticeId,
         @Valid @RequestBody PublishNoticeRequest publishNoticeRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REACTIVATE_NOTICE = "/v1/comms/notices/{noticeId}:reactivate";
    /**
     * POST /v1/comms/notices/{noticeId}:reactivate : Restore a withdrawn notice
     * Restore a withdrawn notice.
     *
     * @param noticeId  (required)
     * @param reactivateNoticeRequest  (required)
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
        value = NoticeApi.PATH_REACTIVATE_NOTICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Notice> reactivateNotice(
         @PathVariable("noticeId") UUID noticeId,
         @Valid @RequestBody ReactivateNoticeRequest reactivateNoticeRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UNPUBLISH_NOTICE = "/v1/comms/notices/{noticeId}:unpublish";
    /**
     * POST /v1/comms/notices/{noticeId}:unpublish : Unpublish
     * Unpublish.
     *
     * @param noticeId  (required)
     * @param unpublishNoticeRequest  (required)
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
        value = NoticeApi.PATH_UNPUBLISH_NOTICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Notice> unpublishNotice(
         @PathVariable("noticeId") UUID noticeId,
         @Valid @RequestBody UnpublishNoticeRequest unpublishNoticeRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_NOTICE = "/v1/comms/notices/{noticeId}";
    /**
     * PATCH /v1/comms/notices/{noticeId} : Update Notice
     * Partially update a Notice (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param noticeId Notice id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param noticeUpdate  (required)
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
        value = NoticeApi.PATH_UPDATE_NOTICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Notice> updateNotice(
         @PathVariable("noticeId") UUID noticeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NoticeUpdate noticeUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_NOTICE_AUDIENCE = "/v1/comms/notices/{noticeId}/audiences/{noticeAudienceId}";
    /**
     * PATCH /v1/comms/notices/{noticeId}/audiences/{noticeAudienceId} : Update NoticeAudience
     * Partially update a NoticeAudience (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param noticeId Notice id. (required)
     * @param noticeAudienceId NoticeAudience id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param noticeAudienceUpdate  (required)
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
        value = NoticeApi.PATH_UPDATE_NOTICE_AUDIENCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NoticeAudience> updateNoticeAudience(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeAudienceId") UUID noticeAudienceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NoticeAudienceUpdate noticeAudienceUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
