// Owned API contract of finance.
package com.mysms.finance.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.finance.api.model.PaymentGatewayAccount;
import com.mysms.finance.api.model.PaymentGatewayAccountUpdate;
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
public interface PaymentGatewayAccountApi {

    String PATH_CREATE_PAYMENT_GATEWAY_ACCOUNT = "/v1/finance/payment-gateway-accounts";
    /**
     * POST /v1/finance/payment-gateway-accounts : Create PaymentGatewayAccount
     * Create a PaymentGatewayAccount. Supports Idempotency-Key and validateOnly.
     *
     * @param paymentGatewayAccount  (required)
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
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = PaymentGatewayAccountApi.PATH_CREATE_PAYMENT_GATEWAY_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PaymentGatewayAccount> createPaymentGatewayAccount(
         @Valid @RequestBody PaymentGatewayAccount paymentGatewayAccount,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_PAYMENT_GATEWAY_ACCOUNT = "/v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}";
    /**
     * DELETE /v1/finance/payment-gateway-accounts/{paymentGatewayAccountId} : Delete PaymentGatewayAccount
     * Soft delete (sets deleted_at).
     *
     * @param paymentGatewayAccountId PaymentGatewayAccount id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = PaymentGatewayAccountApi.PATH_DELETE_PAYMENT_GATEWAY_ACCOUNT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePaymentGatewayAccount(
         @PathVariable("paymentGatewayAccountId") UUID paymentGatewayAccountId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_PAYMENT_GATEWAY_ACCOUNT = "/v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}";
    /**
     * GET /v1/finance/payment-gateway-accounts/{paymentGatewayAccountId} : Get PaymentGatewayAccount
     * Get one PaymentGatewayAccount by id. The ETag header carries the row version.
     *
     * @param paymentGatewayAccountId PaymentGatewayAccount id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = PaymentGatewayAccountApi.PATH_GET_PAYMENT_GATEWAY_ACCOUNT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PaymentGatewayAccount> getPaymentGatewayAccount(
         @PathVariable("paymentGatewayAccountId") UUID paymentGatewayAccountId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PAYMENT_GATEWAY_ACCOUNTS = "/v1/finance/payment-gateway-accounts";
    /**
     * GET /v1/finance/payment-gateway-accounts : List PaymentGatewayAccounts
     * List PaymentGatewayAccounts visible to the caller, page by page, with filtering and sorting.
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
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = PaymentGatewayAccountApi.PATH_LIST_PAYMENT_GATEWAY_ACCOUNTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PaymentGatewayAccount>> listPaymentGatewayAccounts(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_PAYMENT_GATEWAY_ACCOUNT = "/v1/finance/payment-gateway-accounts/{paymentGatewayAccountId}";
    /**
     * PATCH /v1/finance/payment-gateway-accounts/{paymentGatewayAccountId} : Update PaymentGatewayAccount
     * Partially update a PaymentGatewayAccount (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param paymentGatewayAccountId PaymentGatewayAccount id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param paymentGatewayAccountUpdate  (required)
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
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = PaymentGatewayAccountApi.PATH_UPDATE_PAYMENT_GATEWAY_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PaymentGatewayAccount> updatePaymentGatewayAccount(
         @PathVariable("paymentGatewayAccountId") UUID paymentGatewayAccountId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PaymentGatewayAccountUpdate paymentGatewayAccountUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
