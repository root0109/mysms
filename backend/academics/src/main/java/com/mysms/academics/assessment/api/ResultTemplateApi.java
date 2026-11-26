// Owned API contract of assessment.
package com.mysms.academics.assessment.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.academics.assessment.api.model.Operation;
import com.mysms.academics.assessment.api.model.ResultTemplate;
import com.mysms.academics.assessment.api.model.ResultTemplateBand;
import com.mysms.academics.assessment.api.model.ResultTemplateBandUpdate;
import com.mysms.academics.assessment.api.model.ResultTemplateUpdate;
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
public interface ResultTemplateApi {

    String PATH_CREATE_RESULT_TEMPLATE = "/v1/assessment/result-templates";
    /**
     * POST /v1/assessment/result-templates : Create ResultTemplate
     * Create a ResultTemplate. Supports Idempotency-Key and validateOnly.
     *
     * @param resultTemplate  (required)
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
        value = ResultTemplateApi.PATH_CREATE_RESULT_TEMPLATE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ResultTemplate> createResultTemplate(
         @Valid @RequestBody ResultTemplate resultTemplate,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_RESULT_TEMPLATE_BAND = "/v1/assessment/result-templates/{resultTemplateId}/result-template-bands";
    /**
     * POST /v1/assessment/result-templates/{resultTemplateId}/result-template-bands : Create ResultTemplateBand
     * Create a ResultTemplateBand. Supports Idempotency-Key and validateOnly.
     *
     * @param resultTemplateId ResultTemplate id. (required)
     * @param resultTemplateBand  (required)
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
        value = ResultTemplateApi.PATH_CREATE_RESULT_TEMPLATE_BAND,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ResultTemplateBand> createResultTemplateBand(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
         @Valid @RequestBody ResultTemplateBand resultTemplateBand,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_RESULT_TEMPLATE = "/v1/assessment/result-templates/{resultTemplateId}";
    /**
     * DELETE /v1/assessment/result-templates/{resultTemplateId} : Delete ResultTemplate
     * Soft delete (sets deleted_at).
     *
     * @param resultTemplateId ResultTemplate id. (required)
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
        value = ResultTemplateApi.PATH_DELETE_RESULT_TEMPLATE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteResultTemplate(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RESULT_TEMPLATE_BAND = "/v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}";
    /**
     * DELETE /v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId} : Delete ResultTemplateBand
     * Soft delete (sets deleted_at).
     *
     * @param resultTemplateId ResultTemplate id. (required)
     * @param resultTemplateBandId ResultTemplateBand id. (required)
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
        value = ResultTemplateApi.PATH_DELETE_RESULT_TEMPLATE_BAND,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteResultTemplateBand(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
         @PathVariable("resultTemplateBandId") UUID resultTemplateBandId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GENERATE_RESULTS = "/v1/assessment/result-templates/{resultTemplateId}:generateResults";
    /**
     * POST /v1/assessment/result-templates/{resultTemplateId}:generateResults : Compute result lines and the marksheet for the exam series
     * Compute result lines and the marksheet for the exam series.
     *
     * @param resultTemplateId  (required)
     * @param body  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
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
        value = ResultTemplateApi.PATH_GENERATE_RESULTS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> generateResults(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_RESULT_TEMPLATE = "/v1/assessment/result-templates/{resultTemplateId}";
    /**
     * GET /v1/assessment/result-templates/{resultTemplateId} : Get ResultTemplate
     * Get one ResultTemplate by id. The ETag header carries the row version.
     *
     * @param resultTemplateId ResultTemplate id. (required)
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
        value = ResultTemplateApi.PATH_GET_RESULT_TEMPLATE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ResultTemplate> getResultTemplate(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RESULT_TEMPLATE_BAND = "/v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}";
    /**
     * GET /v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId} : Get ResultTemplateBand
     * Get one ResultTemplateBand by id. The ETag header carries the row version.
     *
     * @param resultTemplateId ResultTemplate id. (required)
     * @param resultTemplateBandId ResultTemplateBand id. (required)
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
        value = ResultTemplateApi.PATH_GET_RESULT_TEMPLATE_BAND,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ResultTemplateBand> getResultTemplateBand(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
         @PathVariable("resultTemplateBandId") UUID resultTemplateBandId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RESULT_TEMPLATE_BANDS = "/v1/assessment/result-templates/{resultTemplateId}/result-template-bands";
    /**
     * GET /v1/assessment/result-templates/{resultTemplateId}/result-template-bands : List ResultTemplateBands
     * List ResultTemplateBands visible to the caller, page by page, with filtering and sorting.
     *
     * @param resultTemplateId ResultTemplate id. (required)
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
        value = ResultTemplateApi.PATH_LIST_RESULT_TEMPLATE_BANDS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ResultTemplateBand>> listResultTemplateBands(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RESULT_TEMPLATES = "/v1/assessment/result-templates";
    /**
     * GET /v1/assessment/result-templates : List ResultTemplates
     * List ResultTemplates visible to the caller, page by page, with filtering and sorting.
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
        value = ResultTemplateApi.PATH_LIST_RESULT_TEMPLATES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ResultTemplate>> listResultTemplates(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_RESULT_TEMPLATE = "/v1/assessment/result-templates/{resultTemplateId}";
    /**
     * PATCH /v1/assessment/result-templates/{resultTemplateId} : Update ResultTemplate
     * Partially update a ResultTemplate (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param resultTemplateId ResultTemplate id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param resultTemplateUpdate  (required)
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
        value = ResultTemplateApi.PATH_UPDATE_RESULT_TEMPLATE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ResultTemplate> updateResultTemplate(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ResultTemplateUpdate resultTemplateUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_RESULT_TEMPLATE_BAND = "/v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId}";
    /**
     * PATCH /v1/assessment/result-templates/{resultTemplateId}/result-template-bands/{resultTemplateBandId} : Update ResultTemplateBand
     * Partially update a ResultTemplateBand (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param resultTemplateId ResultTemplate id. (required)
     * @param resultTemplateBandId ResultTemplateBand id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param resultTemplateBandUpdate  (required)
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
        value = ResultTemplateApi.PATH_UPDATE_RESULT_TEMPLATE_BAND,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ResultTemplateBand> updateResultTemplateBand(
         @PathVariable("resultTemplateId") UUID resultTemplateId,
         @PathVariable("resultTemplateBandId") UUID resultTemplateBandId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ResultTemplateBandUpdate resultTemplateBandUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
