// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api;

import com.mysms.platform.api.model.StockImage;
import com.mysms.platform.api.model.ReportDefinition;
import com.mysms.platform.api.model.SearchHit;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.model.Branding;
import com.mysms.platform.api.model.DownloadLink;
import org.springframework.lang.Nullable;
import com.mysms.platform.api.model.Operation;
import com.mysms.platform.api.model.RunReportRequest;
import com.mysms.platform.api.model.StartImportRequest;
import com.mysms.platform.api.model.Tenant;
import com.mysms.platform.api.model.TenantUpdate;
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
public interface TenantApi {

    String PATH_CLEAR_DEMO_DATA = "/v1/platform/tenants/{tenantId}:clearDemoData";
    /**
     * POST /v1/platform/tenants/{tenantId}:clearDemoData : Remove demo data loaded for a trial tenant
     * Remove demo data loaded for a trial tenant.
     *
     * @param tenantId  (required)
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
        value = TenantApi.PATH_CLEAR_DEMO_DATA,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> clearDemoData(
         @PathVariable("tenantId") UUID tenantId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_TENANT = "/v1/platform/tenants";
    /**
     * POST /v1/platform/tenants : Create Tenant
     * Create a Tenant. Supports Idempotency-Key and validateOnly.
     *
     * @param tenant  (required)
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
        value = TenantApi.PATH_CREATE_TENANT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Tenant> createTenant(
         @Valid @RequestBody Tenant tenant,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_TENANT = "/v1/platform/tenants/{tenantId}";
    /**
     * DELETE /v1/platform/tenants/{tenantId} : Delete Tenant
     * Soft delete (sets deleted_at).
     *
     * @param tenantId Tenant id. (required)
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
        value = TenantApi.PATH_DELETE_TENANT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteTenant(
         @PathVariable("tenantId") UUID tenantId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_BRANDING = "/v1/platform/branding";
    /**
     * GET /v1/platform/branding : Public branding and locale for login pages and the PWA manifest
     * Public branding and locale for login pages and the PWA manifest. Public.
     *
     * @param tenantCode Tenant code. (required)
     * @param institutionId Institution. (optional)
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
        value = TenantApi.PATH_GET_BRANDING,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Branding> getBranding(
        @NotNull  @Valid @RequestParam(value = "tenantCode", required = true) String tenantCode,
         @Valid @RequestParam(value = "institutionId", required = false) @Nullable UUID institutionId
    );


    String PATH_GET_IMPORT_TEMPLATE = "/v1/platform/imports/templates/{entityType}";
    /**
     * GET /v1/platform/imports/templates/{entityType} : Download the CSV/XLSX template for an importable entity
     * Download the CSV/XLSX template for an importable entity.
     *
     * @param entityType  (required)
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
        value = TenantApi.PATH_GET_IMPORT_TEMPLATE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<DownloadLink> getImportTemplate(
         @PathVariable("entityType") String entityType
    );


    String PATH_GET_TENANT = "/v1/platform/tenants/{tenantId}";
    /**
     * GET /v1/platform/tenants/{tenantId} : Get Tenant
     * Get one Tenant by id. The ETag header carries the row version.
     *
     * @param tenantId Tenant id. (required)
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
        value = TenantApi.PATH_GET_TENANT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Tenant> getTenant(
         @PathVariable("tenantId") UUID tenantId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GLOBAL_SEARCH = "/v1/platform/search";
    /**
     * GET /v1/platform/search : Permission-aware search across students, staff, guardians, courses, media
     * Permission-aware search across students, staff, guardians, courses, media...
     *
     * @param query Search text. (required)
     * @param entityTypes Restrict to these resource types. (optional)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
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
        value = TenantApi.PATH_GLOBAL_SEARCH,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SearchHit>> globalSearch(
        @NotNull  @Valid @RequestParam(value = "query", required = true) String query,
         @RequestParam(value = "entityTypes", required = false) @Nullable List<String> entityTypes,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_LIST_REPORT_DEFINITIONS = "/v1/platform/reports";
    /**
     * GET /v1/platform/reports : Reports the caller may run
     * Reports the caller may run.
     *
     * @param module Filter by module. (optional)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
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
        value = TenantApi.PATH_LIST_REPORT_DEFINITIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ReportDefinition>> listReportDefinitions(
         @Valid @RequestParam(value = "module", required = false) @Nullable String module,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_LIST_TENANTS = "/v1/platform/tenants";
    /**
     * GET /v1/platform/tenants : List Tenants
     * List Tenants visible to the caller, page by page, with filtering and sorting.
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
        value = TenantApi.PATH_LIST_TENANTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Tenant>> listTenants(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RUN_REPORT = "/v1/platform/reports:run";
    /**
     * POST /v1/platform/reports:run : Run a report or printable document asynchronously; the operation result holds the output file
     * Run a report or printable document asynchronously; the operation result holds the output file.
     *
     * @param runReportRequest  (required)
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
        value = TenantApi.PATH_RUN_REPORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> runReport(
         @Valid @RequestBody RunReportRequest runReportRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SEARCH_STOCK_IMAGES = "/v1/platform/stock-images";
    /**
     * GET /v1/platform/stock-images : Search a stock-photo provider for cover images
     * Search a stock-photo provider for cover images.
     *
     * @param query Keyword. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
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
        value = TenantApi.PATH_SEARCH_STOCK_IMAGES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StockImage>> searchStockImages(
        @NotNull  @Valid @RequestParam(value = "query", required = true) String query,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_START_IMPORT = "/v1/platform/imports";
    /**
     * POST /v1/platform/imports : Bulk import from a file; the operation result lists row errors (ImportRowError)
     * Bulk import from a file; the operation result lists row errors (ImportRowError).
     *
     * @param startImportRequest  (required)
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
        value = TenantApi.PATH_START_IMPORT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> startImport(
         @Valid @RequestBody StartImportRequest startImportRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_TENANT = "/v1/platform/tenants/{tenantId}";
    /**
     * PATCH /v1/platform/tenants/{tenantId} : Update Tenant
     * Partially update a Tenant (updateMask). Requires If-Match with the current ETag.
     *
     * @param tenantId Tenant id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param tenantUpdate  (required)
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
        value = TenantApi.PATH_UPDATE_TENANT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Tenant> updateTenant(
         @PathVariable("tenantId") UUID tenantId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody TenantUpdate tenantUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
