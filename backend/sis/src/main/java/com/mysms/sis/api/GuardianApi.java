// Owned API contract of sis.
package com.mysms.sis.api;

import com.mysms.sis.api.model.ChildCard;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.model.CreatePortalAccountRequest;
import com.mysms.sis.api.model.Guardian;
import com.mysms.sis.api.model.GuardianUpdate;
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
public interface GuardianApi {

    String PATH_CREATE_GUARDIAN = "/v1/sis/guardians";
    /**
     * POST /v1/sis/guardians : Create Guardian
     * Create a Guardian. Supports Idempotency-Key and validateOnly.
     *
     * @param guardian  (required)
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
        value = GuardianApi.PATH_CREATE_GUARDIAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Guardian> createGuardian(
         @Valid @RequestBody Guardian guardian,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PORTAL_ACCOUNT = "/v1/sis/guardians/{guardianId}:createPortalAccount";
    /**
     * POST /v1/sis/guardians/{guardianId}:createPortalAccount : Create the guardian&#39;s login account and grant the guardian role scoped to their children
     * Create the guardian&#39;s login account and grant the guardian role scoped to their children.
     *
     * @param guardianId  (required)
     * @param createPortalAccountRequest  (required)
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
        value = GuardianApi.PATH_CREATE_PORTAL_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Guardian> createPortalAccount(
         @PathVariable("guardianId") UUID guardianId,
         @Valid @RequestBody CreatePortalAccountRequest createPortalAccountRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DELETE_GUARDIAN = "/v1/sis/guardians/{guardianId}";
    /**
     * DELETE /v1/sis/guardians/{guardianId} : Delete Guardian
     * Soft delete (sets deleted_at).
     *
     * @param guardianId Guardian id. (required)
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
        value = GuardianApi.PATH_DELETE_GUARDIAN,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteGuardian(
         @PathVariable("guardianId") UUID guardianId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_GUARDIAN = "/v1/sis/guardians/{guardianId}";
    /**
     * GET /v1/sis/guardians/{guardianId} : Get Guardian
     * Get one Guardian by id. The ETag header carries the row version.
     *
     * @param guardianId Guardian id. (required)
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
        value = GuardianApi.PATH_GET_GUARDIAN,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Guardian> getGuardian(
         @PathVariable("guardianId") UUID guardianId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_GUARDIANS = "/v1/sis/guardians";
    /**
     * GET /v1/sis/guardians : List Guardians
     * List Guardians visible to the caller, page by page, with filtering and sorting.
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
        value = GuardianApi.PATH_LIST_GUARDIANS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Guardian>> listGuardians(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_MY_CHILDREN = "/v1/sis/me/children";
    /**
     * GET /v1/sis/me/children : Children of the calling guardian
     * Children of the calling guardian.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
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
        method = RequestMethod.GET,
        value = GuardianApi.PATH_LIST_MY_CHILDREN,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ChildCard>> listMyChildren(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_UPDATE_GUARDIAN = "/v1/sis/guardians/{guardianId}";
    /**
     * PATCH /v1/sis/guardians/{guardianId} : Update Guardian
     * Partially update a Guardian (updateMask). Requires If-Match with the current ETag.
     *
     * @param guardianId Guardian id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param guardianUpdate  (required)
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
        value = GuardianApi.PATH_UPDATE_GUARDIAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Guardian> updateGuardian(
         @PathVariable("guardianId") UUID guardianId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody GuardianUpdate guardianUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
