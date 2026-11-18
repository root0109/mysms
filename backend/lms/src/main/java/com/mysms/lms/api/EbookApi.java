// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.Ebook;
import com.mysms.lms.api.model.EbookUpdate;
import java.util.Map;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.PublishEbookCurationRequest;
import com.mysms.lms.api.model.RelatedMaterial;
import com.mysms.lms.api.model.RelatedMaterialList;
import com.mysms.lms.api.model.RelatedMaterialRank;
import com.mysms.lms.api.model.RelatedMaterialUpdate;
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
public interface EbookApi {

    String PATH_CONVERT_EBOOK = "/v1/lms/ebooks/{ebookId}:convert";
    /**
     * POST /v1/lms/ebooks/{ebookId}:convert : Send the uploaded source document for conversion (state on the source file&#39;s processing_status)
     * Send the uploaded source document for conversion (state on the source file&#39;s processing_status).
     *
     * @param ebookId  (required)
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
        value = EbookApi.PATH_CONVERT_EBOOK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Ebook> convertEbook(
         @PathVariable("ebookId") UUID ebookId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_EBOOK = "/v1/lms/ebooks";
    /**
     * POST /v1/lms/ebooks : Create Ebook
     * Create a Ebook. Supports Idempotency-Key and validateOnly.
     *
     * @param ebook  (required)
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
        value = EbookApi.PATH_CREATE_EBOOK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Ebook> createEbook(
         @Valid @RequestBody Ebook ebook,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_RELATED_MATERIAL = "/v1/lms/ebooks/{ebookId}/related-materials";
    /**
     * POST /v1/lms/ebooks/{ebookId}/related-materials : Create RelatedMaterial
     * Create a RelatedMaterial. Supports Idempotency-Key and validateOnly.
     *
     * @param ebookId Ebook id. (required)
     * @param relatedMaterial  (required)
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
        value = EbookApi.PATH_CREATE_RELATED_MATERIAL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RelatedMaterial> createRelatedMaterial(
         @PathVariable("ebookId") UUID ebookId,
         @Valid @RequestBody RelatedMaterial relatedMaterial,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_EBOOK = "/v1/lms/ebooks/{ebookId}";
    /**
     * DELETE /v1/lms/ebooks/{ebookId} : Delete Ebook
     * Soft delete (sets deleted_at).
     *
     * @param ebookId Ebook id. (required)
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
        value = EbookApi.PATH_DELETE_EBOOK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteEbook(
         @PathVariable("ebookId") UUID ebookId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RELATED_MATERIAL = "/v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}";
    /**
     * DELETE /v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId} : Delete RelatedMaterial
     * Soft delete (sets deleted_at).
     *
     * @param ebookId Ebook id. (required)
     * @param relatedMaterialId RelatedMaterial id. (required)
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
        value = EbookApi.PATH_DELETE_RELATED_MATERIAL,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteRelatedMaterial(
         @PathVariable("ebookId") UUID ebookId,
         @PathVariable("relatedMaterialId") UUID relatedMaterialId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_EBOOK = "/v1/lms/ebooks/{ebookId}";
    /**
     * GET /v1/lms/ebooks/{ebookId} : Get Ebook
     * Get one Ebook by id. The ETag header carries the row version.
     *
     * @param ebookId Ebook id. (required)
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
        value = EbookApi.PATH_GET_EBOOK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Ebook> getEbook(
         @PathVariable("ebookId") UUID ebookId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_MY_RELATED_MATERIALS = "/v1/lms/ebooks/{ebookId}:myRelatedMaterials";
    /**
     * GET /v1/lms/ebooks/{ebookId}:myRelatedMaterials : Related materials ranked for the caller
     * Related materials ranked for the caller.
     *
     * @param ebookId  (required)
     * @param courseId Course context. (optional)
     * @param fileId Page source context. (optional)
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
        value = EbookApi.PATH_GET_MY_RELATED_MATERIALS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RelatedMaterial>> getMyRelatedMaterials(
         @PathVariable("ebookId") UUID ebookId,
         @Valid @RequestParam(value = "courseId", required = false) @Nullable UUID courseId,
         @Valid @RequestParam(value = "fileId", required = false) @Nullable UUID fileId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_RELATED_MATERIAL = "/v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}";
    /**
     * GET /v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId} : Get RelatedMaterial
     * Get one RelatedMaterial by id. The ETag header carries the row version.
     *
     * @param ebookId Ebook id. (required)
     * @param relatedMaterialId RelatedMaterial id. (required)
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
        value = EbookApi.PATH_GET_RELATED_MATERIAL,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RelatedMaterial> getRelatedMaterial(
         @PathVariable("ebookId") UUID ebookId,
         @PathVariable("relatedMaterialId") UUID relatedMaterialId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RELATED_MATERIAL_RANK = "/v1/lms/ebooks/{ebookId}/related-material-ranks/{relatedMaterialRankId}";
    /**
     * GET /v1/lms/ebooks/{ebookId}/related-material-ranks/{relatedMaterialRankId} : Get RelatedMaterialRank
     * Get one RelatedMaterialRank by id. The ETag header carries the row version.
     *
     * @param ebookId Ebook id. (required)
     * @param relatedMaterialRankId RelatedMaterialRank id. (required)
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
        value = EbookApi.PATH_GET_RELATED_MATERIAL_RANK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<RelatedMaterialRank> getRelatedMaterialRank(
         @PathVariable("ebookId") UUID ebookId,
         @PathVariable("relatedMaterialRankId") UUID relatedMaterialRankId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EBOOKS = "/v1/lms/ebooks";
    /**
     * GET /v1/lms/ebooks : List Ebooks
     * List Ebooks visible to the caller, page by page, with filtering and sorting.
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
        value = EbookApi.PATH_LIST_EBOOKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Ebook>> listEbooks(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RELATED_MATERIAL_RANKS = "/v1/lms/ebooks/{ebookId}/related-material-ranks";
    /**
     * GET /v1/lms/ebooks/{ebookId}/related-material-ranks : List RelatedMaterialRanks
     * List RelatedMaterialRanks visible to the caller, page by page, with filtering and sorting.
     *
     * @param ebookId Ebook id. (required)
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
        value = EbookApi.PATH_LIST_RELATED_MATERIAL_RANKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RelatedMaterialRank>> listRelatedMaterialRanks(
         @PathVariable("ebookId") UUID ebookId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RELATED_MATERIALS = "/v1/lms/ebooks/{ebookId}/related-materials";
    /**
     * GET /v1/lms/ebooks/{ebookId}/related-materials : List RelatedMaterials
     * List RelatedMaterials visible to the caller, page by page, with filtering and sorting.
     *
     * @param ebookId Ebook id. (required)
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
        value = EbookApi.PATH_LIST_RELATED_MATERIALS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<RelatedMaterial>> listRelatedMaterials(
         @PathVariable("ebookId") UUID ebookId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PUBLISH_EBOOK_CURATION = "/v1/lms/ebooks/{ebookId}:publishCuration";
    /**
     * POST /v1/lms/ebooks/{ebookId}:publishCuration : Teacher curates the related materials shown with an e-book
     * Teacher curates the related materials shown with an e-book.
     *
     * @param ebookId  (required)
     * @param publishEbookCurationRequest  (required)
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
        value = EbookApi.PATH_PUBLISH_EBOOK_CURATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RelatedMaterialList> publishEbookCuration(
         @PathVariable("ebookId") UUID ebookId,
         @Valid @RequestBody PublishEbookCurationRequest publishEbookCurationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RECEIVE_EBOOK_CONVERSION = "/v1/lms/integrations/conversion/{provider}:webhook";
    /**
     * POST /v1/lms/integrations/conversion/{provider}:webhook : Conversion-service callback with the converted e-book
     * Conversion-service callback with the converted e-book. Authenticated by the HMAC signature header with the tenant&#39;s conversion secret (tenant resolved from the stored conversion job reference); requests older than 5 minutes or larger than 50 MB are rejected; processed idempotently by job reference.
     *
     * @param provider  (required)
     * @param xSignature Provider signature / authentication header, verified against the raw body. (required)
     * @param xTimestamp Provider signature / authentication header, verified against the raw body. (required)
     * @param body Raw provider payload, passed through unchanged for signature verification. (required)
     * @return Done (status code 204)
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
        value = EbookApi.PATH_RECEIVE_EBOOK_CONVERSION,
        produces = { "application/problem+json" },
        consumes = { "application/json", "multipart/form-data" }
    )
    ResponseEntity<Void> receiveEbookConversion(
         @PathVariable("provider") String provider,
        @NotNull  @RequestHeader(value = "X-Signature", required = true) String xSignature,
        @NotNull  @RequestHeader(value = "X-Timestamp", required = true) String xTimestamp,
        @RequestBody byte[] body
    );


    String PATH_UPDATE_EBOOK = "/v1/lms/ebooks/{ebookId}";
    /**
     * PATCH /v1/lms/ebooks/{ebookId} : Update Ebook
     * Partially update a Ebook (updateMask). Requires If-Match with the current ETag.
     *
     * @param ebookId Ebook id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param ebookUpdate  (required)
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
        value = EbookApi.PATH_UPDATE_EBOOK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Ebook> updateEbook(
         @PathVariable("ebookId") UUID ebookId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody EbookUpdate ebookUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_RELATED_MATERIAL = "/v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId}";
    /**
     * PATCH /v1/lms/ebooks/{ebookId}/related-materials/{relatedMaterialId} : Update RelatedMaterial
     * Partially update a RelatedMaterial (updateMask). Requires If-Match with the current ETag.
     *
     * @param ebookId Ebook id. (required)
     * @param relatedMaterialId RelatedMaterial id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param relatedMaterialUpdate  (required)
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
        value = EbookApi.PATH_UPDATE_RELATED_MATERIAL,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<RelatedMaterial> updateRelatedMaterial(
         @PathVariable("ebookId") UUID ebookId,
         @PathVariable("relatedMaterialId") UUID relatedMaterialId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody RelatedMaterialUpdate relatedMaterialUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
