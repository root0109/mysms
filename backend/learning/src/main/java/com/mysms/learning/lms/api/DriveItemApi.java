// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.model.DriveItem;
import com.mysms.learning.lms.api.model.DriveItemShare;
import com.mysms.learning.lms.api.model.DriveItemShareList;
import com.mysms.learning.lms.api.model.DriveItemShareUpdate;
import com.mysms.learning.lms.api.model.DriveItemUpdate;
import org.springframework.lang.Nullable;
import com.mysms.learning.lms.api.model.ShareDriveItemRequest;
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
public interface DriveItemApi {

    String PATH_CREATE_DRIVE_ITEM = "/v1/lms/drive-items";
    /**
     * POST /v1/lms/drive-items : Create DriveItem
     * Create a DriveItem. Supports Idempotency-Key and validateOnly.
     *
     * @param driveItem  (required)
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
        value = DriveItemApi.PATH_CREATE_DRIVE_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DriveItem> createDriveItem(
         @Valid @RequestBody DriveItem driveItem,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_DRIVE_ITEM_SHARE = "/v1/lms/drive-items/{driveItemId}/shares";
    /**
     * POST /v1/lms/drive-items/{driveItemId}/shares : Create DriveItemShare
     * Create a DriveItemShare. Supports Idempotency-Key and validateOnly.
     *
     * @param driveItemId DriveItem id. (required)
     * @param driveItemShare  (required)
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
        value = DriveItemApi.PATH_CREATE_DRIVE_ITEM_SHARE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DriveItemShare> createDriveItemShare(
         @PathVariable("driveItemId") UUID driveItemId,
         @Valid @RequestBody DriveItemShare driveItemShare,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_DRIVE_ITEM = "/v1/lms/drive-items/{driveItemId}";
    /**
     * DELETE /v1/lms/drive-items/{driveItemId} : Delete DriveItem
     * Soft delete (sets deleted_at).
     *
     * @param driveItemId DriveItem id. (required)
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
        value = DriveItemApi.PATH_DELETE_DRIVE_ITEM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteDriveItem(
         @PathVariable("driveItemId") UUID driveItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_DRIVE_ITEM_SHARE = "/v1/lms/drive-items/{driveItemId}/shares/{driveItemShareId}";
    /**
     * DELETE /v1/lms/drive-items/{driveItemId}/shares/{driveItemShareId} : Delete DriveItemShare
     * Soft delete (sets deleted_at).
     *
     * @param driveItemId DriveItem id. (required)
     * @param driveItemShareId DriveItemShare id. (required)
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
        value = DriveItemApi.PATH_DELETE_DRIVE_ITEM_SHARE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteDriveItemShare(
         @PathVariable("driveItemId") UUID driveItemId,
         @PathVariable("driveItemShareId") UUID driveItemShareId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_DRIVE_ITEM = "/v1/lms/drive-items/{driveItemId}";
    /**
     * GET /v1/lms/drive-items/{driveItemId} : Get DriveItem
     * Get one DriveItem by id. The ETag header carries the row version.
     *
     * @param driveItemId DriveItem id. (required)
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
        value = DriveItemApi.PATH_GET_DRIVE_ITEM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<DriveItem> getDriveItem(
         @PathVariable("driveItemId") UUID driveItemId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_DRIVE_ITEM_SHARES = "/v1/lms/drive-items/{driveItemId}/shares";
    /**
     * GET /v1/lms/drive-items/{driveItemId}/shares : List DriveItemShares
     * List DriveItemShares visible to the caller, page by page, with filtering and sorting.
     *
     * @param driveItemId DriveItem id. (required)
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
        value = DriveItemApi.PATH_LIST_DRIVE_ITEM_SHARES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<DriveItemShare>> listDriveItemShares(
         @PathVariable("driveItemId") UUID driveItemId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_DRIVE_ITEMS = "/v1/lms/drive-items";
    /**
     * GET /v1/lms/drive-items : List DriveItems
     * List DriveItems visible to the caller, page by page, with filtering and sorting.
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
        value = DriveItemApi.PATH_LIST_DRIVE_ITEMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<DriveItem>> listDriveItems(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_SHARE_DRIVE_ITEM = "/v1/lms/drive-items/{driveItemId}:share";
    /**
     * POST /v1/lms/drive-items/{driveItemId}:share : Share a drive item with users
     * Share a drive item with users.
     *
     * @param driveItemId  (required)
     * @param shareDriveItemRequest  (required)
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
        value = DriveItemApi.PATH_SHARE_DRIVE_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DriveItemShareList> shareDriveItem(
         @PathVariable("driveItemId") UUID driveItemId,
         @Valid @RequestBody ShareDriveItemRequest shareDriveItemRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_DRIVE_ITEM = "/v1/lms/drive-items/{driveItemId}";
    /**
     * PATCH /v1/lms/drive-items/{driveItemId} : Update DriveItem
     * Partially update a DriveItem (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param driveItemId DriveItem id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param driveItemUpdate  (required)
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
        value = DriveItemApi.PATH_UPDATE_DRIVE_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DriveItem> updateDriveItem(
         @PathVariable("driveItemId") UUID driveItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody DriveItemUpdate driveItemUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_DRIVE_ITEM_SHARE = "/v1/lms/drive-items/{driveItemId}/shares/{driveItemShareId}";
    /**
     * PATCH /v1/lms/drive-items/{driveItemId}/shares/{driveItemShareId} : Update DriveItemShare
     * Partially update a DriveItemShare (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param driveItemId DriveItem id. (required)
     * @param driveItemShareId DriveItemShare id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param driveItemShareUpdate  (required)
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
        value = DriveItemApi.PATH_UPDATE_DRIVE_ITEM_SHARE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<DriveItemShare> updateDriveItemShare(
         @PathVariable("driveItemId") UUID driveItemId,
         @PathVariable("driveItemShareId") UUID driveItemShareId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody DriveItemShareUpdate driveItemShareUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
