// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.PlagiarismAccount;
import com.mysms.lms.api.model.PlagiarismAccountUpdate;
import com.mysms.lms.api.model.PlagiarismUserLink;
import com.mysms.lms.api.model.PlagiarismUserLinkUpdate;
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
public interface PlagiarismAccountApi {

    String PATH_CREATE_PLAGIARISM_ACCOUNT = "/v1/lms/plagiarism-accounts";
    /**
     * POST /v1/lms/plagiarism-accounts : Create PlagiarismAccount
     * Create a PlagiarismAccount. Supports Idempotency-Key and validateOnly.
     *
     * @param plagiarismAccount  (required)
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
        value = PlagiarismAccountApi.PATH_CREATE_PLAGIARISM_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PlagiarismAccount> createPlagiarismAccount(
         @Valid @RequestBody PlagiarismAccount plagiarismAccount,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PLAGIARISM_USER_LINK = "/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links";
    /**
     * POST /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links : Create PlagiarismUserLink
     * Create a PlagiarismUserLink. Supports Idempotency-Key and validateOnly.
     *
     * @param plagiarismAccountId PlagiarismAccount id. (required)
     * @param plagiarismUserLink  (required)
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
        value = PlagiarismAccountApi.PATH_CREATE_PLAGIARISM_USER_LINK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PlagiarismUserLink> createPlagiarismUserLink(
         @PathVariable("plagiarismAccountId") UUID plagiarismAccountId,
         @Valid @RequestBody PlagiarismUserLink plagiarismUserLink,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_PLAGIARISM_ACCOUNT = "/v1/lms/plagiarism-accounts/{plagiarismAccountId}";
    /**
     * DELETE /v1/lms/plagiarism-accounts/{plagiarismAccountId} : Delete PlagiarismAccount
     * Soft delete (sets deleted_at).
     *
     * @param plagiarismAccountId PlagiarismAccount id. (required)
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
        value = PlagiarismAccountApi.PATH_DELETE_PLAGIARISM_ACCOUNT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePlagiarismAccount(
         @PathVariable("plagiarismAccountId") UUID plagiarismAccountId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PLAGIARISM_USER_LINK = "/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}";
    /**
     * DELETE /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId} : Delete PlagiarismUserLink
     * Soft delete (sets deleted_at).
     *
     * @param plagiarismAccountId PlagiarismAccount id. (required)
     * @param plagiarismUserLinkId PlagiarismUserLink id. (required)
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
        value = PlagiarismAccountApi.PATH_DELETE_PLAGIARISM_USER_LINK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePlagiarismUserLink(
         @PathVariable("plagiarismAccountId") UUID plagiarismAccountId,
         @PathVariable("plagiarismUserLinkId") UUID plagiarismUserLinkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_PLAGIARISM_ACCOUNT = "/v1/lms/plagiarism-accounts/{plagiarismAccountId}";
    /**
     * GET /v1/lms/plagiarism-accounts/{plagiarismAccountId} : Get PlagiarismAccount
     * Get one PlagiarismAccount by id. The ETag header carries the row version.
     *
     * @param plagiarismAccountId PlagiarismAccount id. (required)
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
        value = PlagiarismAccountApi.PATH_GET_PLAGIARISM_ACCOUNT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PlagiarismAccount> getPlagiarismAccount(
         @PathVariable("plagiarismAccountId") UUID plagiarismAccountId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PLAGIARISM_USER_LINK = "/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}";
    /**
     * GET /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId} : Get PlagiarismUserLink
     * Get one PlagiarismUserLink by id. The ETag header carries the row version.
     *
     * @param plagiarismAccountId PlagiarismAccount id. (required)
     * @param plagiarismUserLinkId PlagiarismUserLink id. (required)
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
        value = PlagiarismAccountApi.PATH_GET_PLAGIARISM_USER_LINK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PlagiarismUserLink> getPlagiarismUserLink(
         @PathVariable("plagiarismAccountId") UUID plagiarismAccountId,
         @PathVariable("plagiarismUserLinkId") UUID plagiarismUserLinkId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PLAGIARISM_ACCOUNTS = "/v1/lms/plagiarism-accounts";
    /**
     * GET /v1/lms/plagiarism-accounts : List PlagiarismAccounts
     * List PlagiarismAccounts visible to the caller, page by page, with filtering and sorting.
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
        value = PlagiarismAccountApi.PATH_LIST_PLAGIARISM_ACCOUNTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PlagiarismAccount>> listPlagiarismAccounts(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PLAGIARISM_USER_LINKS = "/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links";
    /**
     * GET /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links : List PlagiarismUserLinks
     * List PlagiarismUserLinks visible to the caller, page by page, with filtering and sorting.
     *
     * @param plagiarismAccountId PlagiarismAccount id. (required)
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
        value = PlagiarismAccountApi.PATH_LIST_PLAGIARISM_USER_LINKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PlagiarismUserLink>> listPlagiarismUserLinks(
         @PathVariable("plagiarismAccountId") UUID plagiarismAccountId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_PLAGIARISM_ACCOUNT = "/v1/lms/plagiarism-accounts/{plagiarismAccountId}";
    /**
     * PATCH /v1/lms/plagiarism-accounts/{plagiarismAccountId} : Update PlagiarismAccount
     * Partially update a PlagiarismAccount (updateMask). Requires If-Match with the current ETag.
     *
     * @param plagiarismAccountId PlagiarismAccount id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param plagiarismAccountUpdate  (required)
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
        value = PlagiarismAccountApi.PATH_UPDATE_PLAGIARISM_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PlagiarismAccount> updatePlagiarismAccount(
         @PathVariable("plagiarismAccountId") UUID plagiarismAccountId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PlagiarismAccountUpdate plagiarismAccountUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PLAGIARISM_USER_LINK = "/v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId}";
    /**
     * PATCH /v1/lms/plagiarism-accounts/{plagiarismAccountId}/user-links/{plagiarismUserLinkId} : Update PlagiarismUserLink
     * Partially update a PlagiarismUserLink (updateMask). Requires If-Match with the current ETag.
     *
     * @param plagiarismAccountId PlagiarismAccount id. (required)
     * @param plagiarismUserLinkId PlagiarismUserLink id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param plagiarismUserLinkUpdate  (required)
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
        value = PlagiarismAccountApi.PATH_UPDATE_PLAGIARISM_USER_LINK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PlagiarismUserLink> updatePlagiarismUserLink(
         @PathVariable("plagiarismAccountId") UUID plagiarismAccountId,
         @PathVariable("plagiarismUserLinkId") UUID plagiarismUserLinkId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PlagiarismUserLinkUpdate plagiarismUserLinkUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
