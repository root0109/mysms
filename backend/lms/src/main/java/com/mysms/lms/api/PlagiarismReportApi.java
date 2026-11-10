// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ExternalLink;
import java.util.Map;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.PlagiarismReport;
import com.mysms.lms.api.model.SubmitForPlagiarismCheckRequest;
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
public interface PlagiarismReportApi {

    String PATH_GET_PLAGIARISM_REPORT = "/v1/lms/plagiarism-reports/{plagiarismReportId}";
    /**
     * GET /v1/lms/plagiarism-reports/{plagiarismReportId} : Get PlagiarismReport
     * Get one PlagiarismReport by id. The ETag header carries the row version.
     *
     * @param plagiarismReportId PlagiarismReport id. (required)
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
        value = PlagiarismReportApi.PATH_GET_PLAGIARISM_REPORT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PlagiarismReport> getPlagiarismReport(
         @PathVariable("plagiarismReportId") UUID plagiarismReportId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PLAGIARISM_REPORT_LINK = "/v1/lms/plagiarism-reports/{plagiarismReportId}:reportLink";
    /**
     * GET /v1/lms/plagiarism-reports/{plagiarismReportId}:reportLink : Signed link to the provider report viewer
     * Signed link to the provider report viewer.
     *
     * @param plagiarismReportId  (required)
     * @param view Report view. (optional)
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
        value = PlagiarismReportApi.PATH_GET_PLAGIARISM_REPORT_LINK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalLink> getPlagiarismReportLink(
         @PathVariable("plagiarismReportId") UUID plagiarismReportId,
         @Valid @RequestParam(value = "view", required = false) @Nullable String view
    );


    String PATH_LIST_PLAGIARISM_REPORTS = "/v1/lms/plagiarism-reports";
    /**
     * GET /v1/lms/plagiarism-reports : List PlagiarismReports
     * List PlagiarismReports visible to the caller, page by page, with filtering and sorting.
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
        value = PlagiarismReportApi.PATH_LIST_PLAGIARISM_REPORTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PlagiarismReport>> listPlagiarismReports(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RECEIVE_PLAGIARISM_WEBHOOK = "/v1/lms/integrations/plagiarism/{provider}:webhook";
    /**
     * POST /v1/lms/integrations/plagiarism/{provider}:webhook : Provider callback when a similarity report is ready
     * Provider callback when a similarity report is ready. Authenticated by the provider signature over the raw body with the account&#39;s shared secret (tenant and account resolved from the stored provider submission id); requests outside a 5-minute replay window are rejected; processed idempotently.
     *
     * @param provider  (required)
     * @param xSignature Provider signature / authentication header, verified against the raw body. (required)
     * @param xTimestamp Provider signature / authentication header, verified against the raw body. (required)
     * @param body Raw provider payload, passed through unchanged for signature verification. (required)
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
        value = PlagiarismReportApi.PATH_RECEIVE_PLAGIARISM_WEBHOOK,
        produces = { "application/problem+json" },
        consumes = { "application/json", "application/x-www-form-urlencoded" }
    )
    ResponseEntity<Void> receivePlagiarismWebhook(
         @PathVariable("provider") String provider,
        @NotNull  @RequestHeader(value = "X-Signature", required = true) String xSignature,
        @NotNull  @RequestHeader(value = "X-Timestamp", required = true) String xTimestamp,
         @Valid @RequestBody Object body
    );


    String PATH_SUBMIT_FOR_PLAGIARISM_CHECK = "/v1/lms/plagiarism-reports:submit";
    /**
     * POST /v1/lms/plagiarism-reports:submit : Send a submitted file to the plagiarism provider
     * Send a submitted file to the plagiarism provider.
     *
     * @param submitForPlagiarismCheckRequest  (required)
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
        value = PlagiarismReportApi.PATH_SUBMIT_FOR_PLAGIARISM_CHECK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PlagiarismReport> submitForPlagiarismCheck(
         @Valid @RequestBody SubmitForPlagiarismCheckRequest submitForPlagiarismCheckRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
