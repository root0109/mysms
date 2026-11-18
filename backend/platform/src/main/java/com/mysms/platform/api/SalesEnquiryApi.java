// Owned API contract of platform.
package com.mysms.platform.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.model.MarkEnquiryContactedRequest;
import org.springframework.lang.Nullable;
import com.mysms.platform.api.model.SalesEnquiry;
import com.mysms.platform.api.model.SalesEnquiryUpdate;
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
public interface SalesEnquiryApi {

    String PATH_CREATE_SALES_ENQUIRY = "/v1/platform/sales-enquiries";
    /**
     * POST /v1/platform/sales-enquiries : Create SalesEnquiry
     * Create a SalesEnquiry. Supports Idempotency-Key and validateOnly.
     *
     * @param salesEnquiry  (required)
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
        value = SalesEnquiryApi.PATH_CREATE_SALES_ENQUIRY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SalesEnquiry> createSalesEnquiry(
         @Valid @RequestBody SalesEnquiry salesEnquiry,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_SALES_ENQUIRY = "/v1/platform/sales-enquiries/{salesEnquiryId}";
    /**
     * DELETE /v1/platform/sales-enquiries/{salesEnquiryId} : Delete SalesEnquiry
     * Soft delete (sets deleted_at).
     *
     * @param salesEnquiryId SalesEnquiry id. (required)
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
        value = SalesEnquiryApi.PATH_DELETE_SALES_ENQUIRY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSalesEnquiry(
         @PathVariable("salesEnquiryId") UUID salesEnquiryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_SALES_ENQUIRY = "/v1/platform/sales-enquiries/{salesEnquiryId}";
    /**
     * GET /v1/platform/sales-enquiries/{salesEnquiryId} : Get SalesEnquiry
     * Get one SalesEnquiry by id. The ETag header carries the row version.
     *
     * @param salesEnquiryId SalesEnquiry id. (required)
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
        value = SalesEnquiryApi.PATH_GET_SALES_ENQUIRY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SalesEnquiry> getSalesEnquiry(
         @PathVariable("salesEnquiryId") UUID salesEnquiryId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SALES_ENQUIRIES = "/v1/platform/sales-enquiries";
    /**
     * GET /v1/platform/sales-enquiries : List SalesEnquiries
     * List SalesEnquiries visible to the caller, page by page, with filtering and sorting.
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
        value = SalesEnquiryApi.PATH_LIST_SALES_ENQUIRIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SalesEnquiry>> listSalesEnquiries(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_ENQUIRY_CONTACTED = "/v1/platform/sales-enquiries/{salesEnquiryId}:markContacted";
    /**
     * POST /v1/platform/sales-enquiries/{salesEnquiryId}:markContacted : Record follow-up
     * Record follow-up.
     *
     * @param salesEnquiryId  (required)
     * @param markEnquiryContactedRequest  (required)
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
        value = SalesEnquiryApi.PATH_MARK_ENQUIRY_CONTACTED,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SalesEnquiry> markEnquiryContacted(
         @PathVariable("salesEnquiryId") UUID salesEnquiryId,
         @Valid @RequestBody MarkEnquiryContactedRequest markEnquiryContactedRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_SALES_ENQUIRY = "/v1/platform/sales-enquiries/{salesEnquiryId}";
    /**
     * PATCH /v1/platform/sales-enquiries/{salesEnquiryId} : Update SalesEnquiry
     * Partially update a SalesEnquiry (updateMask). Requires If-Match with the current ETag.
     *
     * @param salesEnquiryId SalesEnquiry id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param salesEnquiryUpdate  (required)
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
        value = SalesEnquiryApi.PATH_UPDATE_SALES_ENQUIRY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SalesEnquiry> updateSalesEnquiry(
         @PathVariable("salesEnquiryId") UUID salesEnquiryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SalesEnquiryUpdate salesEnquiryUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
