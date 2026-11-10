// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.model.CopyFileRequest;
import com.mysms.platform.api.model.CreateUploadUrlRequest;
import com.mysms.platform.api.model.DownloadLink;
import com.mysms.platform.api.model.FileObject;
import org.springframework.lang.Nullable;
import java.util.UUID;
import com.mysms.platform.api.model.UploadTarget;
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
public interface FileObjectApi {

    String PATH_COMPLETE_UPLOAD = "/v1/platform/files/{fileObjectId}:complete";
    /**
     * POST /v1/platform/files/{fileObjectId}:complete : Confirm the bytes were uploaded; the file is scanned and becomes available
     * Confirm the bytes were uploaded; the file is scanned and becomes available.
     *
     * @param fileObjectId  (required)
     * @param body  (required)
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
        value = FileObjectApi.PATH_COMPLETE_UPLOAD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FileObject> completeUpload(
         @PathVariable("fileObjectId") UUID fileObjectId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_COPY_FILE = "/v1/platform/files/{fileObjectId}:copy";
    /**
     * POST /v1/platform/files/{fileObjectId}:copy : Server-side copy of a stored file into a new file record (no download / upload round trip)
     * Server-side copy of a stored file into a new file record (no download / upload round trip).
     *
     * @param fileObjectId  (required)
     * @param copyFileRequest  (required)
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
        value = FileObjectApi.PATH_COPY_FILE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FileObject> copyFile(
         @PathVariable("fileObjectId") UUID fileObjectId,
         @Valid @RequestBody CopyFileRequest copyFileRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_UPLOAD_URL = "/v1/platform/files:createUploadUrl";
    /**
     * POST /v1/platform/files:createUploadUrl : Create a pending file and a pre-signed upload URL
     * Create a pending file and a pre-signed upload URL.
     *
     * @param createUploadUrlRequest  (required)
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
        value = FileObjectApi.PATH_CREATE_UPLOAD_URL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<UploadTarget> createUploadUrl(
         @Valid @RequestBody CreateUploadUrlRequest createUploadUrlRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_DELETE_FILE_OBJECT = "/v1/platform/files/{fileObjectId}";
    /**
     * DELETE /v1/platform/files/{fileObjectId} : Delete FileObject
     * Soft delete (sets deleted_at).
     *
     * @param fileObjectId FileObject id. (required)
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
        value = FileObjectApi.PATH_DELETE_FILE_OBJECT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteFileObject(
         @PathVariable("fileObjectId") UUID fileObjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_DOWNLOAD_URL = "/v1/platform/files/{fileObjectId}:downloadUrl";
    /**
     * GET /v1/platform/files/{fileObjectId}:downloadUrl : Pre-signed download URL
     * Pre-signed download URL. Signed-in users are checked against the owner record; guests can only resolve files owned by published catalogue content (course / cohort images, preview lessons, public profiles). Public.
     *
     * @param fileObjectId  (required)
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
        value = FileObjectApi.PATH_GET_DOWNLOAD_URL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<DownloadLink> getDownloadUrl(
         @PathVariable("fileObjectId") UUID fileObjectId
    );


    String PATH_GET_FILE_OBJECT = "/v1/platform/files/{fileObjectId}";
    /**
     * GET /v1/platform/files/{fileObjectId} : Get FileObject
     * Get one FileObject by id. The ETag header carries the row version.
     *
     * @param fileObjectId FileObject id. (required)
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
        value = FileObjectApi.PATH_GET_FILE_OBJECT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<FileObject> getFileObject(
         @PathVariable("fileObjectId") UUID fileObjectId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FILE_OBJECTS = "/v1/platform/files";
    /**
     * GET /v1/platform/files : List FileObjects
     * List FileObjects visible to the caller, page by page, with filtering and sorting.
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
        value = FileObjectApi.PATH_LIST_FILE_OBJECTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<FileObject>> listFileObjects(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );

}
