// Owned API contract of finance.
package com.mysms.finance.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.model.Coupon;
import com.mysms.finance.api.model.CouponScope;
import com.mysms.finance.api.model.CouponScopeUpdate;
import com.mysms.finance.api.model.CouponUpdate;
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
public interface CouponApi {

    String PATH_CREATE_COUPON = "/v1/finance/coupons";
    /**
     * POST /v1/finance/coupons : Create Coupon
     * Create a Coupon. Supports Idempotency-Key and validateOnly.
     *
     * @param coupon  (required)
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
        value = CouponApi.PATH_CREATE_COUPON,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Coupon> createCoupon(
         @Valid @RequestBody Coupon coupon,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_COUPON_SCOPE = "/v1/finance/coupons/{couponId}/scopes";
    /**
     * POST /v1/finance/coupons/{couponId}/scopes : Create CouponScope
     * Create a CouponScope. Supports Idempotency-Key and validateOnly.
     *
     * @param couponId Coupon id. (required)
     * @param couponScope  (required)
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
        value = CouponApi.PATH_CREATE_COUPON_SCOPE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CouponScope> createCouponScope(
         @PathVariable("couponId") UUID couponId,
         @Valid @RequestBody CouponScope couponScope,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_COUPON = "/v1/finance/coupons/{couponId}";
    /**
     * DELETE /v1/finance/coupons/{couponId} : Delete Coupon
     * Soft delete (sets deleted_at).
     *
     * @param couponId Coupon id. (required)
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
        value = CouponApi.PATH_DELETE_COUPON,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCoupon(
         @PathVariable("couponId") UUID couponId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_COUPON_SCOPE = "/v1/finance/coupons/{couponId}/scopes/{couponScopeId}";
    /**
     * DELETE /v1/finance/coupons/{couponId}/scopes/{couponScopeId} : Delete CouponScope
     * Soft delete (sets deleted_at).
     *
     * @param couponId Coupon id. (required)
     * @param couponScopeId CouponScope id. (required)
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
        value = CouponApi.PATH_DELETE_COUPON_SCOPE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCouponScope(
         @PathVariable("couponId") UUID couponId,
         @PathVariable("couponScopeId") UUID couponScopeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_COUPON = "/v1/finance/coupons/{couponId}";
    /**
     * GET /v1/finance/coupons/{couponId} : Get Coupon
     * Get one Coupon by id. The ETag header carries the row version.
     *
     * @param couponId Coupon id. (required)
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
        value = CouponApi.PATH_GET_COUPON,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Coupon> getCoupon(
         @PathVariable("couponId") UUID couponId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_COUPON_SCOPE = "/v1/finance/coupons/{couponId}/scopes/{couponScopeId}";
    /**
     * GET /v1/finance/coupons/{couponId}/scopes/{couponScopeId} : Get CouponScope
     * Get one CouponScope by id. The ETag header carries the row version.
     *
     * @param couponId Coupon id. (required)
     * @param couponScopeId CouponScope id. (required)
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
        value = CouponApi.PATH_GET_COUPON_SCOPE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CouponScope> getCouponScope(
         @PathVariable("couponId") UUID couponId,
         @PathVariable("couponScopeId") UUID couponScopeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COUPON_SCOPES = "/v1/finance/coupons/{couponId}/scopes";
    /**
     * GET /v1/finance/coupons/{couponId}/scopes : List CouponScopes
     * List CouponScopes visible to the caller, page by page, with filtering and sorting.
     *
     * @param couponId Coupon id. (required)
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
        value = CouponApi.PATH_LIST_COUPON_SCOPES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CouponScope>> listCouponScopes(
         @PathVariable("couponId") UUID couponId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_COUPONS = "/v1/finance/coupons";
    /**
     * GET /v1/finance/coupons : List Coupons
     * List Coupons visible to the caller, page by page, with filtering and sorting.
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
        value = CouponApi.PATH_LIST_COUPONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Coupon>> listCoupons(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_COUPON = "/v1/finance/coupons/{couponId}";
    /**
     * PATCH /v1/finance/coupons/{couponId} : Update Coupon
     * Partially update a Coupon (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param couponId Coupon id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param couponUpdate  (required)
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
        value = CouponApi.PATH_UPDATE_COUPON,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Coupon> updateCoupon(
         @PathVariable("couponId") UUID couponId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CouponUpdate couponUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_COUPON_SCOPE = "/v1/finance/coupons/{couponId}/scopes/{couponScopeId}";
    /**
     * PATCH /v1/finance/coupons/{couponId}/scopes/{couponScopeId} : Update CouponScope
     * Partially update a CouponScope (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param couponId Coupon id. (required)
     * @param couponScopeId CouponScope id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param couponScopeUpdate  (required)
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
        value = CouponApi.PATH_UPDATE_COUPON_SCOPE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CouponScope> updateCouponScope(
         @PathVariable("couponId") UUID couponId,
         @PathVariable("couponScopeId") UUID couponScopeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody CouponScopeUpdate couponScopeUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
