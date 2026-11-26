// Owned API contract of org.
package com.mysms.platform.org.api;

import com.mysms.platform.org.api.model.InstitutionCard;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.org.api.model.Institution;
import com.mysms.platform.org.api.model.InstitutionAttribute;
import com.mysms.platform.org.api.model.InstitutionAttributeUpdate;
import com.mysms.platform.org.api.model.InstitutionUpdate;
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
public interface InstitutionApi {

    String PATH_CREATE_INSTITUTION = "/v1/org/institutions";
    /**
     * POST /v1/org/institutions : Create Institution
     * Create a Institution. Supports Idempotency-Key and validateOnly.
     *
     * @param institution  (required)
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
        value = InstitutionApi.PATH_CREATE_INSTITUTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Institution> createInstitution(
         @Valid @RequestBody Institution institution,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_INSTITUTION_ATTRIBUTE = "/v1/org/institutions/{institutionId}/attributes";
    /**
     * POST /v1/org/institutions/{institutionId}/attributes : Create InstitutionAttribute
     * Create a InstitutionAttribute. Supports Idempotency-Key and validateOnly.
     *
     * @param institutionId Institution id. (required)
     * @param institutionAttribute  (required)
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
        value = InstitutionApi.PATH_CREATE_INSTITUTION_ATTRIBUTE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<InstitutionAttribute> createInstitutionAttribute(
         @PathVariable("institutionId") UUID institutionId,
         @Valid @RequestBody InstitutionAttribute institutionAttribute,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_INSTITUTION = "/v1/org/institutions/{institutionId}";
    /**
     * DELETE /v1/org/institutions/{institutionId} : Delete Institution
     * Soft delete (sets deleted_at).
     *
     * @param institutionId Institution id. (required)
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
        value = InstitutionApi.PATH_DELETE_INSTITUTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteInstitution(
         @PathVariable("institutionId") UUID institutionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_INSTITUTION_ATTRIBUTE = "/v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}";
    /**
     * DELETE /v1/org/institutions/{institutionId}/attributes/{institutionAttributeId} : Delete InstitutionAttribute
     * Soft delete (sets deleted_at).
     *
     * @param institutionId Institution id. (required)
     * @param institutionAttributeId InstitutionAttribute id. (required)
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
        value = InstitutionApi.PATH_DELETE_INSTITUTION_ATTRIBUTE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteInstitutionAttribute(
         @PathVariable("institutionId") UUID institutionId,
         @PathVariable("institutionAttributeId") UUID institutionAttributeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_FIND_INSTITUTIONS = "/v1/org/institutions:find";
    /**
     * GET /v1/org/institutions:find : Find a school / college to sign in to (mobile app school picker)
     * Find a school / college to sign in to (mobile app school picker). Public.
     *
     * @param query Name or code (minimum 3 characters). (required)
     * @param countryCode ISO country. (optional)
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
        value = InstitutionApi.PATH_FIND_INSTITUTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<InstitutionCard>> findInstitutions(
        @NotNull  @Valid @RequestParam(value = "query", required = true) String query,
         @Valid @RequestParam(value = "countryCode", required = false) @Nullable String countryCode,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_INSTITUTION = "/v1/org/institutions/{institutionId}";
    /**
     * GET /v1/org/institutions/{institutionId} : Get Institution
     * Get one Institution by id. The ETag header carries the row version.
     *
     * @param institutionId Institution id. (required)
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
        value = InstitutionApi.PATH_GET_INSTITUTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Institution> getInstitution(
         @PathVariable("institutionId") UUID institutionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_INSTITUTION_ATTRIBUTE = "/v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}";
    /**
     * GET /v1/org/institutions/{institutionId}/attributes/{institutionAttributeId} : Get InstitutionAttribute
     * Get one InstitutionAttribute by id. The ETag header carries the row version.
     *
     * @param institutionId Institution id. (required)
     * @param institutionAttributeId InstitutionAttribute id. (required)
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
        value = InstitutionApi.PATH_GET_INSTITUTION_ATTRIBUTE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<InstitutionAttribute> getInstitutionAttribute(
         @PathVariable("institutionId") UUID institutionId,
         @PathVariable("institutionAttributeId") UUID institutionAttributeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_INSTITUTION_ATTRIBUTES = "/v1/org/institutions/{institutionId}/attributes";
    /**
     * GET /v1/org/institutions/{institutionId}/attributes : List InstitutionAttributes
     * List InstitutionAttributes visible to the caller, page by page, with filtering and sorting.
     *
     * @param institutionId Institution id. (required)
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
        value = InstitutionApi.PATH_LIST_INSTITUTION_ATTRIBUTES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<InstitutionAttribute>> listInstitutionAttributes(
         @PathVariable("institutionId") UUID institutionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_INSTITUTIONS = "/v1/org/institutions";
    /**
     * GET /v1/org/institutions : List Institutions
     * List Institutions visible to the caller, page by page, with filtering and sorting.
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
        value = InstitutionApi.PATH_LIST_INSTITUTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Institution>> listInstitutions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ACTIVE_INSTITUTIONS = "/v1/org/institutions:active";
    /**
     * GET /v1/org/institutions:active : the active institutions of the call's tenant, by name. Internal: MySMS services
     * only (iam offers them to self sign-up).
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = InstitutionApi.PATH_LIST_ACTIVE_INSTITUTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Institution>> listActiveInstitutions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter
    );


    String PATH_UPDATE_INSTITUTION = "/v1/org/institutions/{institutionId}";
    /**
     * PATCH /v1/org/institutions/{institutionId} : Update Institution
     * Partially update a Institution (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param institutionId Institution id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param institutionUpdate  (required)
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
        value = InstitutionApi.PATH_UPDATE_INSTITUTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Institution> updateInstitution(
         @PathVariable("institutionId") UUID institutionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody InstitutionUpdate institutionUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_INSTITUTION_ATTRIBUTE = "/v1/org/institutions/{institutionId}/attributes/{institutionAttributeId}";
    /**
     * PATCH /v1/org/institutions/{institutionId}/attributes/{institutionAttributeId} : Update InstitutionAttribute
     * Partially update a InstitutionAttribute (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param institutionId Institution id. (required)
     * @param institutionAttributeId InstitutionAttribute id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param institutionAttributeUpdate  (required)
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
        value = InstitutionApi.PATH_UPDATE_INSTITUTION_ATTRIBUTE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<InstitutionAttribute> updateInstitutionAttribute(
         @PathVariable("institutionId") UUID institutionId,
         @PathVariable("institutionAttributeId") UUID institutionAttributeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody InstitutionAttributeUpdate institutionAttributeUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
