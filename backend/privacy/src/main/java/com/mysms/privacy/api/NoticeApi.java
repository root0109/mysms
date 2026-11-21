// Owned API contract of privacy.
package com.mysms.privacy.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.privacy.api.model.Notice;
import com.mysms.privacy.api.model.NoticeTranslation;
import com.mysms.privacy.api.model.NoticeTranslationUpdate;
import com.mysms.privacy.api.model.NoticeUpdate;
import com.mysms.privacy.api.model.NoticeVersion;
import com.mysms.privacy.api.model.NoticeVersionUpdate;
import com.mysms.privacy.api.model.PublishNoticeVersionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
public interface NoticeApi {

    String PATH_CREATE_NOTICE = "/v1/privacy/notices";
    /**
     * POST /v1/privacy/notices : Create Notice
     * Create a notice of the tenant. Supports Idempotency-Key and validateOnly.
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


    String PATH_CREATE_NOTICE_TRANSLATION = "/v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations";
    /**
     * POST /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations : Create NoticeTranslation
     * Create a translation of a draft notice version. Supports Idempotency-Key and validateOnly.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersionId NoticeVersion id. (required)
     * @param noticeTranslation  (required)
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
        value = NoticeApi.PATH_CREATE_NOTICE_TRANSLATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NoticeTranslation> createNoticeTranslation(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeVersionId") UUID noticeVersionId,
         @Valid @RequestBody NoticeTranslation noticeTranslation,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_NOTICE_VERSION = "/v1/privacy/notices/{noticeId}/versions";
    /**
     * POST /v1/privacy/notices/{noticeId}/versions : Create NoticeVersion
     * Create the next version of a notice, as a draft. Supports Idempotency-Key and validateOnly.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersion  (required)
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
        value = NoticeApi.PATH_CREATE_NOTICE_VERSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NoticeVersion> createNoticeVersion(
         @PathVariable("noticeId") UUID noticeId,
         @Valid @RequestBody NoticeVersion noticeVersion,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_NOTICE = "/v1/privacy/notices/{noticeId}";
    /**
     * DELETE /v1/privacy/notices/{noticeId} : Delete Notice
     * Soft delete (sets deleted_at); refused while a purpose setting names the notice or a version of it is active.
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


    String PATH_DELETE_NOTICE_TRANSLATION = "/v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations/{noticeTranslationId}";
    /**
     * DELETE /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations/{noticeTranslationId} : Delete NoticeTranslation
     * Soft delete (sets deleted_at); only while the version is a draft.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersionId NoticeVersion id. (required)
     * @param noticeTranslationId NoticeTranslation id. (required)
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
        value = NoticeApi.PATH_DELETE_NOTICE_TRANSLATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteNoticeTranslation(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeVersionId") UUID noticeVersionId,
         @PathVariable("noticeTranslationId") UUID noticeTranslationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_NOTICE = "/v1/privacy/notices/{noticeId}";
    /**
     * GET /v1/privacy/notices/{noticeId} : Get Notice
     * Get one notice by id. The ETag header carries the row version.
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


    String PATH_GET_NOTICE_TRANSLATION = "/v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations/{noticeTranslationId}";
    /**
     * GET /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations/{noticeTranslationId} : Get NoticeTranslation
     * Get one translation by id. The ETag header carries the row version.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersionId NoticeVersion id. (required)
     * @param noticeTranslationId NoticeTranslation id. (required)
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
        value = NoticeApi.PATH_GET_NOTICE_TRANSLATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<NoticeTranslation> getNoticeTranslation(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeVersionId") UUID noticeVersionId,
         @PathVariable("noticeTranslationId") UUID noticeTranslationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_NOTICE_VERSION = "/v1/privacy/notices/{noticeId}/versions/{noticeVersionId}";
    /**
     * GET /v1/privacy/notices/{noticeId}/versions/{noticeVersionId} : Get NoticeVersion
     * Get one version by id. The ETag header carries the row version.
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


    String PATH_LIST_NOTICE_TRANSLATIONS = "/v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations";
    /**
     * GET /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations : List NoticeTranslations
     * List the translations of a notice version, page by page, with filtering and sorting.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersionId NoticeVersion id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields. (optional)
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
        value = NoticeApi.PATH_LIST_NOTICE_TRANSLATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<NoticeTranslation>> listNoticeTranslations(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeVersionId") UUID noticeVersionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTICE_VERSIONS = "/v1/privacy/notices/{noticeId}/versions";
    /**
     * GET /v1/privacy/notices/{noticeId}/versions : List NoticeVersions
     * List the versions of a notice, page by page, with filtering and sorting.
     *
     * @param noticeId Notice id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields. (optional)
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
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_NOTICES = "/v1/privacy/notices";
    /**
     * GET /v1/privacy/notices : List Notices
     * List the notices of the tenant, page by page, with filtering and sorting.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
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
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PUBLISH_NOTICE_VERSION = "/v1/privacy/notices/{noticeId}/versions/{noticeVersionId}:publish";
    /**
     * POST /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}:publish : Publish NoticeVersion
     * Publish a draft version that has a translation: it becomes the notice's active version and the previous active version is retired.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersionId NoticeVersion id. (required)
     * @param publishNoticeVersionRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = NoticeApi.PATH_PUBLISH_NOTICE_VERSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NoticeVersion> publishNoticeVersion(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeVersionId") UUID noticeVersionId,
         @Valid @RequestBody PublishNoticeVersionRequest publishNoticeVersionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_NOTICE = "/v1/privacy/notices/{noticeId}";
    /**
     * PATCH /v1/privacy/notices/{noticeId} : Update Notice
     * Partially update a notice (updateMask). Requires If-Match with the current ETag.
     *
     * @param noticeId Notice id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param noticeUpdate  (required)
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
        value = NoticeApi.PATH_UPDATE_NOTICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Notice> updateNotice(
         @PathVariable("noticeId") UUID noticeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NoticeUpdate noticeUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_NOTICE_TRANSLATION = "/v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations/{noticeTranslationId}";
    /**
     * PATCH /v1/privacy/notices/{noticeId}/versions/{noticeVersionId}/translations/{noticeTranslationId} : Update NoticeTranslation
     * Partially update a translation of a draft version (updateMask). Requires If-Match with the current ETag.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersionId NoticeVersion id. (required)
     * @param noticeTranslationId NoticeTranslation id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param noticeTranslationUpdate  (required)
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
        value = NoticeApi.PATH_UPDATE_NOTICE_TRANSLATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NoticeTranslation> updateNoticeTranslation(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeVersionId") UUID noticeVersionId,
         @PathVariable("noticeTranslationId") UUID noticeTranslationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NoticeTranslationUpdate noticeTranslationUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_NOTICE_VERSION = "/v1/privacy/notices/{noticeId}/versions/{noticeVersionId}";
    /**
     * PATCH /v1/privacy/notices/{noticeId}/versions/{noticeVersionId} : Update NoticeVersion
     * Partially update a draft version (updateMask). Requires If-Match with the current ETag.
     *
     * @param noticeId Notice id. (required)
     * @param noticeVersionId NoticeVersion id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param noticeVersionUpdate  (required)
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
        value = NoticeApi.PATH_UPDATE_NOTICE_VERSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<NoticeVersion> updateNoticeVersion(
         @PathVariable("noticeId") UUID noticeId,
         @PathVariable("noticeVersionId") UUID noticeVersionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody NoticeVersionUpdate noticeVersionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
