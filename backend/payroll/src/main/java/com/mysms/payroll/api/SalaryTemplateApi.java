// Owned API contract of payroll.
package com.mysms.payroll.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.payroll.api.model.SalaryTemplate;
import com.mysms.payroll.api.model.SalaryTemplateComponent;
import com.mysms.payroll.api.model.SalaryTemplateComponentUpdate;
import com.mysms.payroll.api.model.SalaryTemplateUpdate;
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
public interface SalaryTemplateApi {

    String PATH_CREATE_SALARY_TEMPLATE = "/v1/payroll/salary-templates";
    /**
     * POST /v1/payroll/salary-templates : Create SalaryTemplate
     * Create a SalaryTemplate. Supports Idempotency-Key and validateOnly.
     *
     * @param salaryTemplate  (required)
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
        value = SalaryTemplateApi.PATH_CREATE_SALARY_TEMPLATE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SalaryTemplate> createSalaryTemplate(
         @Valid @RequestBody SalaryTemplate salaryTemplate,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_SALARY_TEMPLATE_COMPONENT = "/v1/payroll/salary-templates/{salaryTemplateId}/components";
    /**
     * POST /v1/payroll/salary-templates/{salaryTemplateId}/components : Create SalaryTemplateComponent
     * Create a SalaryTemplateComponent. Supports Idempotency-Key and validateOnly.
     *
     * @param salaryTemplateId SalaryTemplate id. (required)
     * @param salaryTemplateComponent  (required)
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
        value = SalaryTemplateApi.PATH_CREATE_SALARY_TEMPLATE_COMPONENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SalaryTemplateComponent> createSalaryTemplateComponent(
         @PathVariable("salaryTemplateId") UUID salaryTemplateId,
         @Valid @RequestBody SalaryTemplateComponent salaryTemplateComponent,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_SALARY_TEMPLATE = "/v1/payroll/salary-templates/{salaryTemplateId}";
    /**
     * DELETE /v1/payroll/salary-templates/{salaryTemplateId} : Delete SalaryTemplate
     * Soft delete (sets deleted_at).
     *
     * @param salaryTemplateId SalaryTemplate id. (required)
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
        value = SalaryTemplateApi.PATH_DELETE_SALARY_TEMPLATE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSalaryTemplate(
         @PathVariable("salaryTemplateId") UUID salaryTemplateId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_SALARY_TEMPLATE_COMPONENT = "/v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}";
    /**
     * DELETE /v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId} : Delete SalaryTemplateComponent
     * Soft delete (sets deleted_at).
     *
     * @param salaryTemplateId SalaryTemplate id. (required)
     * @param salaryTemplateComponentId SalaryTemplateComponent id. (required)
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
        value = SalaryTemplateApi.PATH_DELETE_SALARY_TEMPLATE_COMPONENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSalaryTemplateComponent(
         @PathVariable("salaryTemplateId") UUID salaryTemplateId,
         @PathVariable("salaryTemplateComponentId") UUID salaryTemplateComponentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_SALARY_TEMPLATE = "/v1/payroll/salary-templates/{salaryTemplateId}";
    /**
     * GET /v1/payroll/salary-templates/{salaryTemplateId} : Get SalaryTemplate
     * Get one SalaryTemplate by id. The ETag header carries the row version.
     *
     * @param salaryTemplateId SalaryTemplate id. (required)
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
        value = SalaryTemplateApi.PATH_GET_SALARY_TEMPLATE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SalaryTemplate> getSalaryTemplate(
         @PathVariable("salaryTemplateId") UUID salaryTemplateId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_SALARY_TEMPLATE_COMPONENT = "/v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}";
    /**
     * GET /v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId} : Get SalaryTemplateComponent
     * Get one SalaryTemplateComponent by id. The ETag header carries the row version.
     *
     * @param salaryTemplateId SalaryTemplate id. (required)
     * @param salaryTemplateComponentId SalaryTemplateComponent id. (required)
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
        value = SalaryTemplateApi.PATH_GET_SALARY_TEMPLATE_COMPONENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SalaryTemplateComponent> getSalaryTemplateComponent(
         @PathVariable("salaryTemplateId") UUID salaryTemplateId,
         @PathVariable("salaryTemplateComponentId") UUID salaryTemplateComponentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SALARY_TEMPLATE_COMPONENTS = "/v1/payroll/salary-templates/{salaryTemplateId}/components";
    /**
     * GET /v1/payroll/salary-templates/{salaryTemplateId}/components : List SalaryTemplateComponents
     * List SalaryTemplateComponents visible to the caller, page by page, with filtering and sorting.
     *
     * @param salaryTemplateId SalaryTemplate id. (required)
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
        value = SalaryTemplateApi.PATH_LIST_SALARY_TEMPLATE_COMPONENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SalaryTemplateComponent>> listSalaryTemplateComponents(
         @PathVariable("salaryTemplateId") UUID salaryTemplateId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SALARY_TEMPLATES = "/v1/payroll/salary-templates";
    /**
     * GET /v1/payroll/salary-templates : List SalaryTemplates
     * List SalaryTemplates visible to the caller, page by page, with filtering and sorting.
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
        value = SalaryTemplateApi.PATH_LIST_SALARY_TEMPLATES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SalaryTemplate>> listSalaryTemplates(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_SALARY_TEMPLATE = "/v1/payroll/salary-templates/{salaryTemplateId}";
    /**
     * PATCH /v1/payroll/salary-templates/{salaryTemplateId} : Update SalaryTemplate
     * Partially update a SalaryTemplate (updateMask). Requires If-Match with the current ETag.
     *
     * @param salaryTemplateId SalaryTemplate id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param salaryTemplateUpdate  (required)
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
        value = SalaryTemplateApi.PATH_UPDATE_SALARY_TEMPLATE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SalaryTemplate> updateSalaryTemplate(
         @PathVariable("salaryTemplateId") UUID salaryTemplateId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SalaryTemplateUpdate salaryTemplateUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_SALARY_TEMPLATE_COMPONENT = "/v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId}";
    /**
     * PATCH /v1/payroll/salary-templates/{salaryTemplateId}/components/{salaryTemplateComponentId} : Update SalaryTemplateComponent
     * Partially update a SalaryTemplateComponent (updateMask). Requires If-Match with the current ETag.
     *
     * @param salaryTemplateId SalaryTemplate id. (required)
     * @param salaryTemplateComponentId SalaryTemplateComponent id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param salaryTemplateComponentUpdate  (required)
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
        value = SalaryTemplateApi.PATH_UPDATE_SALARY_TEMPLATE_COMPONENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SalaryTemplateComponent> updateSalaryTemplateComponent(
         @PathVariable("salaryTemplateId") UUID salaryTemplateId,
         @PathVariable("salaryTemplateComponentId") UUID salaryTemplateComponentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SalaryTemplateComponentUpdate salaryTemplateComponentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
