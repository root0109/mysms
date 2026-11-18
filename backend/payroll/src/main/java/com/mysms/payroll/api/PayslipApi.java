// Owned API contract of payroll.
package com.mysms.payroll.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.payroll.api.model.CancelPayslipRequest;
import com.mysms.payroll.api.model.GeneratePayslipsRequest;
import com.mysms.payroll.api.model.MarkPayslipPaidRequest;
import org.springframework.lang.Nullable;
import com.mysms.payroll.api.model.Operation;
import com.mysms.payroll.api.model.Payslip;
import com.mysms.payroll.api.model.PayslipLine;
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
public interface PayslipApi {

    String PATH_CANCEL_PAYSLIP = "/v1/payroll/payslips/{payslipId}:cancel";
    /**
     * POST /v1/payroll/payslips/{payslipId}:cancel : Cancel
     * Cancel.
     *
     * @param payslipId  (required)
     * @param cancelPayslipRequest  (required)
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
        value = PayslipApi.PATH_CANCEL_PAYSLIP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Payslip> cancelPayslip(
         @PathVariable("payslipId") UUID payslipId,
         @Valid @RequestBody CancelPayslipRequest cancelPayslipRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GENERATE_PAYSLIPS = "/v1/payroll/payslips:generate";
    /**
     * POST /v1/payroll/payslips:generate : Generate payslips from staff salaries, leave and attendance
     * Generate payslips from staff salaries, leave and attendance.
     *
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param generatePayslipsRequest  (required)
     * @return Accepted; poll the operation. (status code 202)
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
        value = PayslipApi.PATH_GENERATE_PAYSLIPS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> generatePayslips(
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody GeneratePayslipsRequest generatePayslipsRequest
    );


    String PATH_GET_PAYSLIP = "/v1/payroll/payslips/{payslipId}";
    /**
     * GET /v1/payroll/payslips/{payslipId} : Get Payslip
     * Get one Payslip by id. The ETag header carries the row version.
     *
     * @param payslipId Payslip id. (required)
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
        value = PayslipApi.PATH_GET_PAYSLIP,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Payslip> getPayslip(
         @PathVariable("payslipId") UUID payslipId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PAYSLIP_LINE = "/v1/payroll/payslips/{payslipId}/lines/{payslipLineId}";
    /**
     * GET /v1/payroll/payslips/{payslipId}/lines/{payslipLineId} : Get PayslipLine
     * Get one PayslipLine by id. The ETag header carries the row version.
     *
     * @param payslipId Payslip id. (required)
     * @param payslipLineId PayslipLine id. (required)
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
        value = PayslipApi.PATH_GET_PAYSLIP_LINE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PayslipLine> getPayslipLine(
         @PathVariable("payslipId") UUID payslipId,
         @PathVariable("payslipLineId") UUID payslipLineId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PAYSLIP_LINES = "/v1/payroll/payslips/{payslipId}/lines";
    /**
     * GET /v1/payroll/payslips/{payslipId}/lines : List PayslipLines
     * List PayslipLines visible to the caller, page by page, with filtering and sorting.
     *
     * @param payslipId Payslip id. (required)
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
        value = PayslipApi.PATH_LIST_PAYSLIP_LINES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PayslipLine>> listPayslipLines(
         @PathVariable("payslipId") UUID payslipId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PAYSLIPS = "/v1/payroll/payslips";
    /**
     * GET /v1/payroll/payslips : List Payslips
     * List Payslips visible to the caller, page by page, with filtering and sorting.
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
        value = PayslipApi.PATH_LIST_PAYSLIPS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Payslip>> listPayslips(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_PAYSLIP_PAID = "/v1/payroll/payslips/{payslipId}:markPaid";
    /**
     * POST /v1/payroll/payslips/{payslipId}:markPaid : Mark paid and create the payroll payment
     * Mark paid and create the payroll payment.
     *
     * @param payslipId  (required)
     * @param markPayslipPaidRequest  (required)
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
        value = PayslipApi.PATH_MARK_PAYSLIP_PAID,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Payslip> markPayslipPaid(
         @PathVariable("payslipId") UUID payslipId,
         @Valid @RequestBody MarkPayslipPaidRequest markPayslipPaidRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
