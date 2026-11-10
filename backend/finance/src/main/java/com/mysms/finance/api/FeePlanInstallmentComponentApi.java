// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.model.FeePlanInstallmentComponent;
import com.mysms.finance.api.model.FeePlanInstallmentComponentUpdate;
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
public interface FeePlanInstallmentComponentApi {

    String PATH_CREATE_FEE_PLAN_INSTALLMENT_COMPONENT = "/v1/finance/fee-plan-installment-components";
    /**
     * POST /v1/finance/fee-plan-installment-components : Create FeePlanInstallmentComponent
     * Create a FeePlanInstallmentComponent. Supports Idempotency-Key and validateOnly.
     *
     * @param feePlanInstallmentComponent  (required)
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
        value = FeePlanInstallmentComponentApi.PATH_CREATE_FEE_PLAN_INSTALLMENT_COMPONENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeePlanInstallmentComponent> createFeePlanInstallmentComponent(
         @Valid @RequestBody FeePlanInstallmentComponent feePlanInstallmentComponent,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_FEE_PLAN_INSTALLMENT_COMPONENT = "/v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}";
    /**
     * DELETE /v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId} : Delete FeePlanInstallmentComponent
     * Soft delete (sets deleted_at).
     *
     * @param feePlanInstallmentComponentId FeePlanInstallmentComponent id. (required)
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
        value = FeePlanInstallmentComponentApi.PATH_DELETE_FEE_PLAN_INSTALLMENT_COMPONENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteFeePlanInstallmentComponent(
         @PathVariable("feePlanInstallmentComponentId") UUID feePlanInstallmentComponentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_FEE_PLAN_INSTALLMENT_COMPONENT = "/v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}";
    /**
     * GET /v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId} : Get FeePlanInstallmentComponent
     * Get one FeePlanInstallmentComponent by id. The ETag header carries the row version.
     *
     * @param feePlanInstallmentComponentId FeePlanInstallmentComponent id. (required)
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
        value = FeePlanInstallmentComponentApi.PATH_GET_FEE_PLAN_INSTALLMENT_COMPONENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<FeePlanInstallmentComponent> getFeePlanInstallmentComponent(
         @PathVariable("feePlanInstallmentComponentId") UUID feePlanInstallmentComponentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FEE_PLAN_INSTALLMENT_COMPONENTS = "/v1/finance/fee-plan-installment-components";
    /**
     * GET /v1/finance/fee-plan-installment-components : List FeePlanInstallmentComponents
     * List FeePlanInstallmentComponents visible to the caller, page by page, with filtering and sorting.
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
        value = FeePlanInstallmentComponentApi.PATH_LIST_FEE_PLAN_INSTALLMENT_COMPONENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<FeePlanInstallmentComponent>> listFeePlanInstallmentComponents(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_FEE_PLAN_INSTALLMENT_COMPONENT = "/v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId}";
    /**
     * PATCH /v1/finance/fee-plan-installment-components/{feePlanInstallmentComponentId} : Update FeePlanInstallmentComponent
     * Partially update a FeePlanInstallmentComponent (updateMask). Requires If-Match with the current ETag.
     *
     * @param feePlanInstallmentComponentId FeePlanInstallmentComponent id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param feePlanInstallmentComponentUpdate  (required)
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
        value = FeePlanInstallmentComponentApi.PATH_UPDATE_FEE_PLAN_INSTALLMENT_COMPONENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeePlanInstallmentComponent> updateFeePlanInstallmentComponent(
         @PathVariable("feePlanInstallmentComponentId") UUID feePlanInstallmentComponentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody FeePlanInstallmentComponentUpdate feePlanInstallmentComponentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
