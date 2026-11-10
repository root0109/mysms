// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.model.CancelInvoiceRequest;
import com.mysms.finance.api.model.CreateInvoiceFromDuesRequest;
import com.mysms.finance.api.model.FeeStatement;
import com.mysms.finance.api.model.Invoice;
import com.mysms.finance.api.model.InvoiceLine;
import com.mysms.finance.api.model.InvoiceUpdate;
import org.springframework.lang.Nullable;
import com.mysms.finance.api.model.PostInvoiceRequest;
import com.mysms.finance.api.model.SendFeeRemindersRequest;
import com.mysms.finance.api.model.SendResult;
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
public interface InvoiceApi {

    String PATH_CANCEL_INVOICE = "/v1/finance/invoices/{invoiceId}:cancel";
    /**
     * POST /v1/finance/invoices/{invoiceId}:cancel : Cancel
     * Cancel.
     *
     * @param invoiceId  (required)
     * @param cancelInvoiceRequest  (required)
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
        value = InvoiceApi.PATH_CANCEL_INVOICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Invoice> cancelInvoice(
         @PathVariable("invoiceId") UUID invoiceId,
         @Valid @RequestBody CancelInvoiceRequest cancelInvoiceRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_INVOICE = "/v1/finance/invoices";
    /**
     * POST /v1/finance/invoices : Create Invoice
     * Create a Invoice. Supports Idempotency-Key and validateOnly.
     *
     * @param invoice  (required)
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
        value = InvoiceApi.PATH_CREATE_INVOICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Invoice> createInvoice(
         @Valid @RequestBody Invoice invoice,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_INVOICE_FROM_DUES = "/v1/finance/invoices:fromDues";
    /**
     * POST /v1/finance/invoices:fromDues : Raise an invoice for dues
     * Raise an invoice for dues.
     *
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param createInvoiceFromDuesRequest  (required)
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
        value = InvoiceApi.PATH_CREATE_INVOICE_FROM_DUES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Invoice> createInvoiceFromDues(
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody CreateInvoiceFromDuesRequest createInvoiceFromDuesRequest
    );


    String PATH_GET_FEE_STATEMENT = "/v1/finance/students/{studentId}/fee-statement";
    /**
     * GET /v1/finance/students/{studentId}/fee-statement : Student fee statement
     * Student fee statement.
     *
     * @param studentId  (required)
     * @param academicYearId Year. (optional)
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
        value = InvoiceApi.PATH_GET_FEE_STATEMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<FeeStatement> getFeeStatement(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestParam(value = "academicYearId", required = false) @Nullable UUID academicYearId
    );


    String PATH_GET_INVOICE = "/v1/finance/invoices/{invoiceId}";
    /**
     * GET /v1/finance/invoices/{invoiceId} : Get Invoice
     * Get one Invoice by id. The ETag header carries the row version.
     *
     * @param invoiceId Invoice id. (required)
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
        value = InvoiceApi.PATH_GET_INVOICE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Invoice> getInvoice(
         @PathVariable("invoiceId") UUID invoiceId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_INVOICE_LINE = "/v1/finance/invoices/{invoiceId}/lines/{invoiceLineId}";
    /**
     * GET /v1/finance/invoices/{invoiceId}/lines/{invoiceLineId} : Get InvoiceLine
     * Get one InvoiceLine by id. The ETag header carries the row version.
     *
     * @param invoiceId Invoice id. (required)
     * @param invoiceLineId InvoiceLine id. (required)
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
        value = InvoiceApi.PATH_GET_INVOICE_LINE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<InvoiceLine> getInvoiceLine(
         @PathVariable("invoiceId") UUID invoiceId,
         @PathVariable("invoiceLineId") UUID invoiceLineId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_INVOICE_LINES = "/v1/finance/invoices/{invoiceId}/lines";
    /**
     * GET /v1/finance/invoices/{invoiceId}/lines : List InvoiceLines
     * List InvoiceLines visible to the caller, page by page, with filtering and sorting.
     *
     * @param invoiceId Invoice id. (required)
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
        value = InvoiceApi.PATH_LIST_INVOICE_LINES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<InvoiceLine>> listInvoiceLines(
         @PathVariable("invoiceId") UUID invoiceId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_INVOICES = "/v1/finance/invoices";
    /**
     * GET /v1/finance/invoices : List Invoices
     * List Invoices visible to the caller, page by page, with filtering and sorting.
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
        value = InvoiceApi.PATH_LIST_INVOICES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Invoice>> listInvoices(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_POST_INVOICE = "/v1/finance/invoices/{invoiceId}:post";
    /**
     * POST /v1/finance/invoices/{invoiceId}:post : Post (number assigned)
     * Post (number assigned).
     *
     * @param invoiceId  (required)
     * @param postInvoiceRequest  (required)
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
        value = InvoiceApi.PATH_POST_INVOICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Invoice> postInvoice(
         @PathVariable("invoiceId") UUID invoiceId,
         @Valid @RequestBody PostInvoiceRequest postInvoiceRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SEND_FEE_REMINDERS = "/v1/finance/invoices:sendReminders";
    /**
     * POST /v1/finance/invoices:sendReminders : Remind guardians of unpaid fees
     * Remind guardians of unpaid fees.
     *
     * @param sendFeeRemindersRequest  (required)
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
        value = InvoiceApi.PATH_SEND_FEE_REMINDERS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SendResult> sendFeeReminders(
         @Valid @RequestBody SendFeeRemindersRequest sendFeeRemindersRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_INVOICE = "/v1/finance/invoices/{invoiceId}";
    /**
     * PATCH /v1/finance/invoices/{invoiceId} : Update Invoice
     * Partially update a Invoice (updateMask). Requires If-Match with the current ETag.
     *
     * @param invoiceId Invoice id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param invoiceUpdate  (required)
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
        value = InvoiceApi.PATH_UPDATE_INVOICE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Invoice> updateInvoice(
         @PathVariable("invoiceId") UUID invoiceId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody InvoiceUpdate invoiceUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
