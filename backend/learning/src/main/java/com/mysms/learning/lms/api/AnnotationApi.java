// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.model.Annotation;
import com.mysms.learning.lms.api.model.AnnotationShare;
import com.mysms.learning.lms.api.model.AnnotationShareUpdate;
import com.mysms.learning.lms.api.model.AnnotationUpdate;
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
public interface AnnotationApi {

    String PATH_CREATE_ANNOTATION = "/v1/lms/annotations";
    /**
     * POST /v1/lms/annotations : Create Annotation
     * Create a Annotation. Supports Idempotency-Key and validateOnly.
     *
     * @param annotation  (required)
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
        value = AnnotationApi.PATH_CREATE_ANNOTATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Annotation> createAnnotation(
         @Valid @RequestBody Annotation annotation,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_ANNOTATION_SHARE = "/v1/lms/annotations/{annotationId}/shares";
    /**
     * POST /v1/lms/annotations/{annotationId}/shares : Create AnnotationShare
     * Create a AnnotationShare. Supports Idempotency-Key and validateOnly.
     *
     * @param annotationId Annotation id. (required)
     * @param annotationShare  (required)
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
        value = AnnotationApi.PATH_CREATE_ANNOTATION_SHARE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AnnotationShare> createAnnotationShare(
         @PathVariable("annotationId") UUID annotationId,
         @Valid @RequestBody AnnotationShare annotationShare,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ANNOTATION = "/v1/lms/annotations/{annotationId}";
    /**
     * DELETE /v1/lms/annotations/{annotationId} : Delete Annotation
     * Soft delete (sets deleted_at).
     *
     * @param annotationId Annotation id. (required)
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
        value = AnnotationApi.PATH_DELETE_ANNOTATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAnnotation(
         @PathVariable("annotationId") UUID annotationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ANNOTATION_SHARE = "/v1/lms/annotations/{annotationId}/shares/{annotationShareId}";
    /**
     * DELETE /v1/lms/annotations/{annotationId}/shares/{annotationShareId} : Delete AnnotationShare
     * Soft delete (sets deleted_at).
     *
     * @param annotationId Annotation id. (required)
     * @param annotationShareId AnnotationShare id. (required)
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
        value = AnnotationApi.PATH_DELETE_ANNOTATION_SHARE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAnnotationShare(
         @PathVariable("annotationId") UUID annotationId,
         @PathVariable("annotationShareId") UUID annotationShareId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_ANNOTATION = "/v1/lms/annotations/{annotationId}";
    /**
     * GET /v1/lms/annotations/{annotationId} : Get Annotation
     * Get one Annotation by id. The ETag header carries the row version.
     *
     * @param annotationId Annotation id. (required)
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
        value = AnnotationApi.PATH_GET_ANNOTATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Annotation> getAnnotation(
         @PathVariable("annotationId") UUID annotationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ANNOTATION_SHARE = "/v1/lms/annotations/{annotationId}/shares/{annotationShareId}";
    /**
     * GET /v1/lms/annotations/{annotationId}/shares/{annotationShareId} : Get AnnotationShare
     * Get one AnnotationShare by id. The ETag header carries the row version.
     *
     * @param annotationId Annotation id. (required)
     * @param annotationShareId AnnotationShare id. (required)
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
        value = AnnotationApi.PATH_GET_ANNOTATION_SHARE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AnnotationShare> getAnnotationShare(
         @PathVariable("annotationId") UUID annotationId,
         @PathVariable("annotationShareId") UUID annotationShareId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ANNOTATION_SHARES = "/v1/lms/annotations/{annotationId}/shares";
    /**
     * GET /v1/lms/annotations/{annotationId}/shares : List AnnotationShares
     * List AnnotationShares visible to the caller, page by page, with filtering and sorting.
     *
     * @param annotationId Annotation id. (required)
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
        value = AnnotationApi.PATH_LIST_ANNOTATION_SHARES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AnnotationShare>> listAnnotationShares(
         @PathVariable("annotationId") UUID annotationId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ANNOTATIONS = "/v1/lms/annotations";
    /**
     * GET /v1/lms/annotations : List Annotations
     * List Annotations visible to the caller, page by page, with filtering and sorting.
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
        value = AnnotationApi.PATH_LIST_ANNOTATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Annotation>> listAnnotations(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_ANNOTATION = "/v1/lms/annotations/{annotationId}";
    /**
     * PATCH /v1/lms/annotations/{annotationId} : Update Annotation
     * Partially update a Annotation (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param annotationId Annotation id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param annotationUpdate  (required)
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
        value = AnnotationApi.PATH_UPDATE_ANNOTATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Annotation> updateAnnotation(
         @PathVariable("annotationId") UUID annotationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AnnotationUpdate annotationUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ANNOTATION_SHARE = "/v1/lms/annotations/{annotationId}/shares/{annotationShareId}";
    /**
     * PATCH /v1/lms/annotations/{annotationId}/shares/{annotationShareId} : Update AnnotationShare
     * Partially update a AnnotationShare (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param annotationId Annotation id. (required)
     * @param annotationShareId AnnotationShare id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param annotationShareUpdate  (required)
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
        value = AnnotationApi.PATH_UPDATE_ANNOTATION_SHARE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AnnotationShare> updateAnnotationShare(
         @PathVariable("annotationId") UUID annotationId,
         @PathVariable("annotationShareId") UUID annotationShareId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AnnotationShareUpdate annotationShareUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
