// Owned API contract of org (JAVA-CODING-RULES §7); first materialized from api/openapi/org.yaml on 2026-11-10.
package com.mysms.org.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.org.api.model.InstitutionType;
import com.mysms.org.api.model.InstitutionTypeAttribute;
import com.mysms.org.api.model.InstitutionTypeAttributeUpdate;
import com.mysms.org.api.model.InstitutionTypeUpdate;
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
public interface InstitutionTypeApi {

    String PATH_CREATE_INSTITUTION_TYPE = "/v1/org/institution-types";
    /**
     * POST /v1/org/institution-types : Create InstitutionType
     * Create a InstitutionType. Supports Idempotency-Key and validateOnly.
     *
     * @param institutionType  (required)
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
        value = InstitutionTypeApi.PATH_CREATE_INSTITUTION_TYPE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<InstitutionType> createInstitutionType(
         @Valid @RequestBody InstitutionType institutionType,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_INSTITUTION_TYPE_ATTRIBUTE = "/v1/org/institution-types/{institutionTypeId}/attributes";
    /**
     * POST /v1/org/institution-types/{institutionTypeId}/attributes : Create InstitutionTypeAttribute
     * Create a InstitutionTypeAttribute. Supports Idempotency-Key and validateOnly.
     *
     * @param institutionTypeId InstitutionType id. (required)
     * @param institutionTypeAttribute  (required)
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
        value = InstitutionTypeApi.PATH_CREATE_INSTITUTION_TYPE_ATTRIBUTE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<InstitutionTypeAttribute> createInstitutionTypeAttribute(
         @PathVariable("institutionTypeId") UUID institutionTypeId,
         @Valid @RequestBody InstitutionTypeAttribute institutionTypeAttribute,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_INSTITUTION_TYPE = "/v1/org/institution-types/{institutionTypeId}";
    /**
     * DELETE /v1/org/institution-types/{institutionTypeId} : Delete InstitutionType
     * Soft delete (sets deleted_at).
     *
     * @param institutionTypeId InstitutionType id. (required)
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
        value = InstitutionTypeApi.PATH_DELETE_INSTITUTION_TYPE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteInstitutionType(
         @PathVariable("institutionTypeId") UUID institutionTypeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_INSTITUTION_TYPE_ATTRIBUTE = "/v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}";
    /**
     * DELETE /v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId} : Delete InstitutionTypeAttribute
     * Soft delete (sets deleted_at).
     *
     * @param institutionTypeId InstitutionType id. (required)
     * @param institutionTypeAttributeId InstitutionTypeAttribute id. (required)
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
        value = InstitutionTypeApi.PATH_DELETE_INSTITUTION_TYPE_ATTRIBUTE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteInstitutionTypeAttribute(
         @PathVariable("institutionTypeId") UUID institutionTypeId,
         @PathVariable("institutionTypeAttributeId") UUID institutionTypeAttributeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_INSTITUTION_TYPE = "/v1/org/institution-types/{institutionTypeId}";
    /**
     * GET /v1/org/institution-types/{institutionTypeId} : Get InstitutionType
     * Get one InstitutionType by id. The ETag header carries the row version.
     *
     * @param institutionTypeId InstitutionType id. (required)
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
        value = InstitutionTypeApi.PATH_GET_INSTITUTION_TYPE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<InstitutionType> getInstitutionType(
         @PathVariable("institutionTypeId") UUID institutionTypeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_INSTITUTION_TYPE_ATTRIBUTE = "/v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}";
    /**
     * GET /v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId} : Get InstitutionTypeAttribute
     * Get one InstitutionTypeAttribute by id. The ETag header carries the row version.
     *
     * @param institutionTypeId InstitutionType id. (required)
     * @param institutionTypeAttributeId InstitutionTypeAttribute id. (required)
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
        value = InstitutionTypeApi.PATH_GET_INSTITUTION_TYPE_ATTRIBUTE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<InstitutionTypeAttribute> getInstitutionTypeAttribute(
         @PathVariable("institutionTypeId") UUID institutionTypeId,
         @PathVariable("institutionTypeAttributeId") UUID institutionTypeAttributeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_INSTITUTION_TYPE_ATTRIBUTES = "/v1/org/institution-types/{institutionTypeId}/attributes";
    /**
     * GET /v1/org/institution-types/{institutionTypeId}/attributes : List InstitutionTypeAttributes
     * List InstitutionTypeAttributes visible to the caller, page by page, with filtering and sorting.
     *
     * @param institutionTypeId InstitutionType id. (required)
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
        value = InstitutionTypeApi.PATH_LIST_INSTITUTION_TYPE_ATTRIBUTES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<InstitutionTypeAttribute>> listInstitutionTypeAttributes(
         @PathVariable("institutionTypeId") UUID institutionTypeId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_INSTITUTION_TYPES = "/v1/org/institution-types";
    /**
     * GET /v1/org/institution-types : List InstitutionTypes
     * List InstitutionTypes visible to the caller, page by page, with filtering and sorting.
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
        value = InstitutionTypeApi.PATH_LIST_INSTITUTION_TYPES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<InstitutionType>> listInstitutionTypes(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_INSTITUTION_TYPE = "/v1/org/institution-types/{institutionTypeId}";
    /**
     * PATCH /v1/org/institution-types/{institutionTypeId} : Update InstitutionType
     * Partially update a InstitutionType (updateMask). Requires If-Match with the current ETag.
     *
     * @param institutionTypeId InstitutionType id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param institutionTypeUpdate  (required)
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
        value = InstitutionTypeApi.PATH_UPDATE_INSTITUTION_TYPE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<InstitutionType> updateInstitutionType(
         @PathVariable("institutionTypeId") UUID institutionTypeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody InstitutionTypeUpdate institutionTypeUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_INSTITUTION_TYPE_ATTRIBUTE = "/v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId}";
    /**
     * PATCH /v1/org/institution-types/{institutionTypeId}/attributes/{institutionTypeAttributeId} : Update InstitutionTypeAttribute
     * Partially update a InstitutionTypeAttribute (updateMask). Requires If-Match with the current ETag.
     *
     * @param institutionTypeId InstitutionType id. (required)
     * @param institutionTypeAttributeId InstitutionTypeAttribute id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param institutionTypeAttributeUpdate  (required)
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
        value = InstitutionTypeApi.PATH_UPDATE_INSTITUTION_TYPE_ATTRIBUTE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<InstitutionTypeAttribute> updateInstitutionTypeAttribute(
         @PathVariable("institutionTypeId") UUID institutionTypeId,
         @PathVariable("institutionTypeAttributeId") UUID institutionTypeAttributeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody InstitutionTypeAttributeUpdate institutionTypeAttributeUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
