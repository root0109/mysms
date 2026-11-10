// Owned API contract of library (JAVA-CODING-RULES §7); first materialized from api/openapi/library.yaml on 2026-11-10.
package com.mysms.library.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.model.Invoice;
import com.mysms.library.api.model.Loan;
import com.mysms.library.api.model.LoanUpdate;
import com.mysms.library.api.model.MarkLoanLostRequest;
import org.springframework.lang.Nullable;
import com.mysms.library.api.model.RequestLoanReturnRequest;
import com.mysms.library.api.model.ReturnLoanRequest;
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
public interface LoanApi {

    String PATH_CREATE_LOAN = "/v1/library/loans";
    /**
     * POST /v1/library/loans : Create Loan
     * Create a Loan. Supports Idempotency-Key and validateOnly.
     *
     * @param loan  (required)
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
        value = LoanApi.PATH_CREATE_LOAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Loan> createLoan(
         @Valid @RequestBody Loan loan,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_LOAN = "/v1/library/loans/{loanId}";
    /**
     * DELETE /v1/library/loans/{loanId} : Delete Loan
     * Soft delete (sets deleted_at).
     *
     * @param loanId Loan id. (required)
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
        value = LoanApi.PATH_DELETE_LOAN,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLoan(
         @PathVariable("loanId") UUID loanId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_LOAN = "/v1/library/loans/{loanId}";
    /**
     * GET /v1/library/loans/{loanId} : Get Loan
     * Get one Loan by id. The ETag header carries the row version.
     *
     * @param loanId Loan id. (required)
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
        value = LoanApi.PATH_GET_LOAN,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Loan> getLoan(
         @PathVariable("loanId") UUID loanId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_INVOICE_LOAN_FINE = "/v1/library/loans/{loanId}:invoiceFine";
    /**
     * POST /v1/library/loans/{loanId}:invoiceFine : Raise an invoice for the fine
     * Raise an invoice for the fine.
     *
     * @param loanId  (required)
     * @param body  (required)
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
        value = LoanApi.PATH_INVOICE_LOAN_FINE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Invoice> invoiceLoanFine(
         @PathVariable("loanId") UUID loanId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_LOANS = "/v1/library/loans";
    /**
     * GET /v1/library/loans : List Loans
     * List Loans visible to the caller, page by page, with filtering and sorting.
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
        value = LoanApi.PATH_LIST_LOANS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Loan>> listLoans(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_LOAN_LOST = "/v1/library/loans/{loanId}:markLost";
    /**
     * POST /v1/library/loans/{loanId}:markLost : Copy lost
     * Copy lost.
     *
     * @param loanId  (required)
     * @param markLoanLostRequest  (required)
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
        value = LoanApi.PATH_MARK_LOAN_LOST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Loan> markLoanLost(
         @PathVariable("loanId") UUID loanId,
         @Valid @RequestBody MarkLoanLostRequest markLoanLostRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REQUEST_LOAN_RETURN = "/v1/library/loans/{loanId}:requestReturn";
    /**
     * POST /v1/library/loans/{loanId}:requestReturn : Borrower drops the copy for return (status return_pending until checked)
     * Borrower drops the copy for return (status return_pending until checked).
     *
     * @param loanId  (required)
     * @param requestLoanReturnRequest  (required)
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
        value = LoanApi.PATH_REQUEST_LOAN_RETURN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Loan> requestLoanReturn(
         @PathVariable("loanId") UUID loanId,
         @Valid @RequestBody RequestLoanReturnRequest requestLoanReturnRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RETURN_LOAN = "/v1/library/loans/{loanId}:return";
    /**
     * POST /v1/library/loans/{loanId}:return : Librarian checks the copy in (status returned); fine is computed
     * Librarian checks the copy in (status returned); fine is computed.
     *
     * @param loanId  (required)
     * @param returnLoanRequest  (required)
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
        value = LoanApi.PATH_RETURN_LOAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Loan> returnLoan(
         @PathVariable("loanId") UUID loanId,
         @Valid @RequestBody ReturnLoanRequest returnLoanRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_LOAN = "/v1/library/loans/{loanId}";
    /**
     * PATCH /v1/library/loans/{loanId} : Update Loan
     * Partially update a Loan (updateMask). Requires If-Match with the current ETag.
     *
     * @param loanId Loan id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param loanUpdate  (required)
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
        value = LoanApi.PATH_UPDATE_LOAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Loan> updateLoan(
         @PathVariable("loanId") UUID loanId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LoanUpdate loanUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
