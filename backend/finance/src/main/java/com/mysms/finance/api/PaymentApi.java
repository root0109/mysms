// Owned API contract of finance.
package com.mysms.finance.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.model.CheckoutSession;
import java.util.Map;
import org.springframework.lang.Nullable;
import com.mysms.finance.api.model.OrderQuote;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.api.model.QuoteCheckoutRequest;
import com.mysms.finance.api.model.RecordPaymentRequest;
import com.mysms.finance.api.model.Refund;
import com.mysms.finance.api.model.RefundPaymentRequest;
import com.mysms.finance.api.model.StartCheckoutRequest;
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
public interface PaymentApi {

    String PATH_GET_PAYMENT = "/v1/finance/payments/{paymentId}";
    /**
     * GET /v1/finance/payments/{paymentId} : Get Payment
     * Get one Payment by id. The ETag header carries the row version.
     *
     * @param paymentId Payment id. (required)
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
        value = PaymentApi.PATH_GET_PAYMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Payment> getPayment(
         @PathVariable("paymentId") UUID paymentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PAYMENTS = "/v1/finance/payments";
    /**
     * GET /v1/finance/payments : List Payments
     * List Payments visible to the caller, page by page, with filtering and sorting.
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
        value = PaymentApi.PATH_LIST_PAYMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Payment>> listPayments(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_QUOTE_CHECKOUT = "/v1/finance/checkout:quote";
    /**
     * POST /v1/finance/checkout:quote : Price with coupon, tax and currency
     * Price with coupon, tax and currency.
     *
     * @param quoteCheckoutRequest  (required)
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
        value = PaymentApi.PATH_QUOTE_CHECKOUT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<OrderQuote> quoteCheckout(
         @Valid @RequestBody QuoteCheckoutRequest quoteCheckoutRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RECEIVE_GATEWAY_WEBHOOK = "/v1/finance/payment-gateways/{gateway}:webhook";
    /**
     * POST /v1/finance/payment-gateways/{gateway}:webhook : Gateway callback; marks payments received / failed and fulfils enrolments
     * Gateway callback; marks payments received / failed and fulfils enrolments. Authenticated by the gateway signature over the raw body, not by a token; the tenant is resolved from the stored gateway order id. Processed idempotently by gateway event id.
     *
     * @param gateway  (required)
     * @param body Raw provider payload, passed through unchanged for signature verification. (required)
     * @param signature The gateway's signature of the raw body (Razorpay: X-Razorpay-Signature). (optional)
     * @param eventId The gateway's id of the event, the same on every redelivery (Razorpay: X-Razorpay-Event-Id). (optional)
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
        value = PaymentApi.PATH_RECEIVE_GATEWAY_WEBHOOK,
        produces = { "application/problem+json" },
        consumes = { "application/json", "application/x-www-form-urlencoded" }
    )
    ResponseEntity<Void> receiveGatewayWebhook(
         @PathVariable("gateway") String gateway,
         @RequestBody byte[] body,
         @RequestHeader(value = "X-Razorpay-Signature", required = false) @Nullable String signature,
         @RequestHeader(value = "X-Razorpay-Event-Id", required = false) @Nullable String eventId
    );


    String PATH_RECORD_PAYMENT = "/v1/finance/payments:record";
    /**
     * POST /v1/finance/payments:record : Record an offline/counter payment
     * Records money received outside a gateway (cash, cheque, DD, bank transfer, card POS) with its allocations across invoices and dues. Issues a receipt number. Idempotency-Key is required; a duplicate receipt or gateway reference is rejected with 409.
     *
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param recordPaymentRequest  (required)
     * @return Created (status code 201)
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
        value = PaymentApi.PATH_RECORD_PAYMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Payment> recordPayment(
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody RecordPaymentRequest recordPaymentRequest
    );


    String PATH_REFUND_PAYMENT = "/v1/finance/payments/{paymentId}:refund";
    /**
     * POST /v1/finance/payments/{paymentId}:refund : Refund through the gateway
     * Refund through the gateway.
     *
     * @param paymentId  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param refundPaymentRequest  (required)
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
        value = PaymentApi.PATH_REFUND_PAYMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Refund> refundPayment(
         @PathVariable("paymentId") UUID paymentId,
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody RefundPaymentRequest refundPaymentRequest
    );


    String PATH_START_CHECKOUT = "/v1/finance/checkout:start";
    /**
     * POST /v1/finance/checkout:start : Create a payment and gateway order
     * Create a payment and gateway order.
     *
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param startCheckoutRequest  (required)
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
        value = PaymentApi.PATH_START_CHECKOUT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<CheckoutSession> startCheckout(
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody StartCheckoutRequest startCheckoutRequest
    );

}
