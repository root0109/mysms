// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ForumPostReport;
import com.mysms.lms.api.model.ForumPostReportUpdate;
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
public interface ForumPostReportApi {

    String PATH_CREATE_FORUM_POST_REPORT = "/v1/lms/forum-post-reports";
    /**
     * POST /v1/lms/forum-post-reports : Create ForumPostReport
     * Create a ForumPostReport. Supports Idempotency-Key and validateOnly.
     *
     * @param forumPostReport  (required)
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
        value = ForumPostReportApi.PATH_CREATE_FORUM_POST_REPORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumPostReport> createForumPostReport(
         @Valid @RequestBody ForumPostReport forumPostReport,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_FORUM_POST_REPORT = "/v1/lms/forum-post-reports/{forumPostReportId}";
    /**
     * DELETE /v1/lms/forum-post-reports/{forumPostReportId} : Delete ForumPostReport
     * Soft delete (sets deleted_at).
     *
     * @param forumPostReportId ForumPostReport id. (required)
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
        value = ForumPostReportApi.PATH_DELETE_FORUM_POST_REPORT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteForumPostReport(
         @PathVariable("forumPostReportId") UUID forumPostReportId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_FORUM_POST_REPORT = "/v1/lms/forum-post-reports/{forumPostReportId}";
    /**
     * GET /v1/lms/forum-post-reports/{forumPostReportId} : Get ForumPostReport
     * Get one ForumPostReport by id. The ETag header carries the row version.
     *
     * @param forumPostReportId ForumPostReport id. (required)
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
        value = ForumPostReportApi.PATH_GET_FORUM_POST_REPORT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ForumPostReport> getForumPostReport(
         @PathVariable("forumPostReportId") UUID forumPostReportId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FORUM_POST_REPORTS = "/v1/lms/forum-post-reports";
    /**
     * GET /v1/lms/forum-post-reports : List ForumPostReports
     * List ForumPostReports visible to the caller, page by page, with filtering and sorting.
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
        value = ForumPostReportApi.PATH_LIST_FORUM_POST_REPORTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ForumPostReport>> listForumPostReports(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_FORUM_POST_REPORT = "/v1/lms/forum-post-reports/{forumPostReportId}";
    /**
     * PATCH /v1/lms/forum-post-reports/{forumPostReportId} : Update ForumPostReport
     * Partially update a ForumPostReport (updateMask). Requires If-Match with the current ETag.
     *
     * @param forumPostReportId ForumPostReport id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param forumPostReportUpdate  (required)
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
        value = ForumPostReportApi.PATH_UPDATE_FORUM_POST_REPORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumPostReport> updateForumPostReport(
         @PathVariable("forumPostReportId") UUID forumPostReportId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ForumPostReportUpdate forumPostReportUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
