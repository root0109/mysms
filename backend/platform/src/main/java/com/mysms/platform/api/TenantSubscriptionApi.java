// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.model.CancelSubscriptionRequest;
import org.springframework.lang.Nullable;
import com.mysms.platform.api.model.StartSubscriptionCheckoutRequest;
import com.mysms.platform.api.model.SubscriptionCheckout;
import com.mysms.platform.api.model.TenantSubscription;
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
public interface TenantSubscriptionApi {

    String PATH_CANCEL_SUBSCRIPTION = "/v1/platform/tenant-subscriptions/{tenantSubscriptionId}:cancel";
    /**
     * POST /v1/platform/tenant-subscriptions/{tenantSubscriptionId}:cancel : Cancel at the end of the paid period
     * Cancel at the end of the paid period.
     *
     * @param tenantSubscriptionId  (required)
     * @param cancelSubscriptionRequest  (required)
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
        value = TenantSubscriptionApi.PATH_CANCEL_SUBSCRIPTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<TenantSubscription> cancelSubscription(
         @PathVariable("tenantSubscriptionId") UUID tenantSubscriptionId,
         @Valid @RequestBody CancelSubscriptionRequest cancelSubscriptionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_TENANT_SUBSCRIPTION = "/v1/platform/tenant-subscriptions/{tenantSubscriptionId}";
    /**
     * GET /v1/platform/tenant-subscriptions/{tenantSubscriptionId} : Get TenantSubscription
     * Get one TenantSubscription by id. The ETag header carries the row version.
     *
     * @param tenantSubscriptionId TenantSubscription id. (required)
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
        value = TenantSubscriptionApi.PATH_GET_TENANT_SUBSCRIPTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<TenantSubscription> getTenantSubscription(
         @PathVariable("tenantSubscriptionId") UUID tenantSubscriptionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_TENANT_SUBSCRIPTIONS = "/v1/platform/tenant-subscriptions";
    /**
     * GET /v1/platform/tenant-subscriptions : List TenantSubscriptions
     * List TenantSubscriptions visible to the caller, page by page, with filtering and sorting.
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
        value = TenantSubscriptionApi.PATH_LIST_TENANT_SUBSCRIPTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<TenantSubscription>> listTenantSubscriptions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_START_SUBSCRIPTION_CHECKOUT = "/v1/platform/tenant-subscriptions:checkout";
    /**
     * POST /v1/platform/tenant-subscriptions:checkout : Start paying for a plan
     * Start paying for a plan.
     *
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param startSubscriptionCheckoutRequest  (required)
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
        value = TenantSubscriptionApi.PATH_START_SUBSCRIPTION_CHECKOUT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubscriptionCheckout> startSubscriptionCheckout(
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody StartSubscriptionCheckoutRequest startSubscriptionCheckoutRequest
    );

}
