// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.model.FeePlan;
import com.mysms.finance.api.model.FeePlanInstallment;
import com.mysms.finance.api.model.FeePlanInstallmentUpdate;
import com.mysms.finance.api.model.FeePlanUpdate;
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
public interface FeePlanApi {

    String PATH_CREATE_FEE_PLAN = "/v1/finance/fee-plans";
    /**
     * POST /v1/finance/fee-plans : Create FeePlan
     * Create a FeePlan. Supports Idempotency-Key and validateOnly.
     *
     * @param feePlan  (required)
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
        value = FeePlanApi.PATH_CREATE_FEE_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeePlan> createFeePlan(
         @Valid @RequestBody FeePlan feePlan,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_FEE_PLAN_INSTALLMENT = "/v1/finance/fee-plans/{feePlanId}/installments";
    /**
     * POST /v1/finance/fee-plans/{feePlanId}/installments : Create FeePlanInstallment
     * Create a FeePlanInstallment. Supports Idempotency-Key and validateOnly.
     *
     * @param feePlanId FeePlan id. (required)
     * @param feePlanInstallment  (required)
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
        value = FeePlanApi.PATH_CREATE_FEE_PLAN_INSTALLMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeePlanInstallment> createFeePlanInstallment(
         @PathVariable("feePlanId") UUID feePlanId,
         @Valid @RequestBody FeePlanInstallment feePlanInstallment,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_FEE_PLAN = "/v1/finance/fee-plans/{feePlanId}";
    /**
     * DELETE /v1/finance/fee-plans/{feePlanId} : Delete FeePlan
     * Soft delete (sets deleted_at).
     *
     * @param feePlanId FeePlan id. (required)
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
        value = FeePlanApi.PATH_DELETE_FEE_PLAN,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteFeePlan(
         @PathVariable("feePlanId") UUID feePlanId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_FEE_PLAN_INSTALLMENT = "/v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}";
    /**
     * DELETE /v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId} : Delete FeePlanInstallment
     * Soft delete (sets deleted_at).
     *
     * @param feePlanId FeePlan id. (required)
     * @param feePlanInstallmentId FeePlanInstallment id. (required)
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
        value = FeePlanApi.PATH_DELETE_FEE_PLAN_INSTALLMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteFeePlanInstallment(
         @PathVariable("feePlanId") UUID feePlanId,
         @PathVariable("feePlanInstallmentId") UUID feePlanInstallmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_FEE_PLAN = "/v1/finance/fee-plans/{feePlanId}";
    /**
     * GET /v1/finance/fee-plans/{feePlanId} : Get FeePlan
     * Get one FeePlan by id. The ETag header carries the row version.
     *
     * @param feePlanId FeePlan id. (required)
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
        value = FeePlanApi.PATH_GET_FEE_PLAN,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<FeePlan> getFeePlan(
         @PathVariable("feePlanId") UUID feePlanId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_FEE_PLAN_INSTALLMENT = "/v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}";
    /**
     * GET /v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId} : Get FeePlanInstallment
     * Get one FeePlanInstallment by id. The ETag header carries the row version.
     *
     * @param feePlanId FeePlan id. (required)
     * @param feePlanInstallmentId FeePlanInstallment id. (required)
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
        value = FeePlanApi.PATH_GET_FEE_PLAN_INSTALLMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<FeePlanInstallment> getFeePlanInstallment(
         @PathVariable("feePlanId") UUID feePlanId,
         @PathVariable("feePlanInstallmentId") UUID feePlanInstallmentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FEE_PLAN_INSTALLMENTS = "/v1/finance/fee-plans/{feePlanId}/installments";
    /**
     * GET /v1/finance/fee-plans/{feePlanId}/installments : List FeePlanInstallments
     * List FeePlanInstallments visible to the caller, page by page, with filtering and sorting.
     *
     * @param feePlanId FeePlan id. (required)
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
        value = FeePlanApi.PATH_LIST_FEE_PLAN_INSTALLMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<FeePlanInstallment>> listFeePlanInstallments(
         @PathVariable("feePlanId") UUID feePlanId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FEE_PLANS = "/v1/finance/fee-plans";
    /**
     * GET /v1/finance/fee-plans : List FeePlans
     * List FeePlans visible to the caller, page by page, with filtering and sorting.
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
        value = FeePlanApi.PATH_LIST_FEE_PLANS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<FeePlan>> listFeePlans(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_FEE_PLAN = "/v1/finance/fee-plans/{feePlanId}";
    /**
     * PATCH /v1/finance/fee-plans/{feePlanId} : Update FeePlan
     * Partially update a FeePlan (updateMask). Requires If-Match with the current ETag.
     *
     * @param feePlanId FeePlan id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param feePlanUpdate  (required)
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
        value = FeePlanApi.PATH_UPDATE_FEE_PLAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeePlan> updateFeePlan(
         @PathVariable("feePlanId") UUID feePlanId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody FeePlanUpdate feePlanUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_FEE_PLAN_INSTALLMENT = "/v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId}";
    /**
     * PATCH /v1/finance/fee-plans/{feePlanId}/installments/{feePlanInstallmentId} : Update FeePlanInstallment
     * Partially update a FeePlanInstallment (updateMask). Requires If-Match with the current ETag.
     *
     * @param feePlanId FeePlan id. (required)
     * @param feePlanInstallmentId FeePlanInstallment id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param feePlanInstallmentUpdate  (required)
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
        value = FeePlanApi.PATH_UPDATE_FEE_PLAN_INSTALLMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeePlanInstallment> updateFeePlanInstallment(
         @PathVariable("feePlanId") UUID feePlanId,
         @PathVariable("feePlanInstallmentId") UUID feePlanInstallmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody FeePlanInstallmentUpdate feePlanInstallmentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
