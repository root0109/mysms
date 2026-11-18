// Owned API contract of payroll.
package com.mysms.payroll.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.payroll.api.model.StaffSalary;
import com.mysms.payroll.api.model.StaffSalaryComponent;
import com.mysms.payroll.api.model.StaffSalaryComponentUpdate;
import com.mysms.payroll.api.model.StaffSalaryUpdate;
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
public interface StaffSalaryApi {

    String PATH_CREATE_STAFF_SALARY = "/v1/payroll/staff-salaries";
    /**
     * POST /v1/payroll/staff-salaries : Create StaffSalary
     * Create a StaffSalary. Supports Idempotency-Key and validateOnly.
     *
     * @param staffSalary  (required)
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
        value = StaffSalaryApi.PATH_CREATE_STAFF_SALARY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StaffSalary> createStaffSalary(
         @Valid @RequestBody StaffSalary staffSalary,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_STAFF_SALARY_COMPONENT = "/v1/payroll/staff-salaries/{staffSalaryId}/components";
    /**
     * POST /v1/payroll/staff-salaries/{staffSalaryId}/components : Create StaffSalaryComponent
     * Create a StaffSalaryComponent. Supports Idempotency-Key and validateOnly.
     *
     * @param staffSalaryId StaffSalary id. (required)
     * @param staffSalaryComponent  (required)
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
        value = StaffSalaryApi.PATH_CREATE_STAFF_SALARY_COMPONENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StaffSalaryComponent> createStaffSalaryComponent(
         @PathVariable("staffSalaryId") UUID staffSalaryId,
         @Valid @RequestBody StaffSalaryComponent staffSalaryComponent,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_STAFF_SALARY = "/v1/payroll/staff-salaries/{staffSalaryId}";
    /**
     * DELETE /v1/payroll/staff-salaries/{staffSalaryId} : Delete StaffSalary
     * Soft delete (sets deleted_at).
     *
     * @param staffSalaryId StaffSalary id. (required)
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
        value = StaffSalaryApi.PATH_DELETE_STAFF_SALARY,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStaffSalary(
         @PathVariable("staffSalaryId") UUID staffSalaryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_STAFF_SALARY_COMPONENT = "/v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}";
    /**
     * DELETE /v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId} : Delete StaffSalaryComponent
     * Soft delete (sets deleted_at).
     *
     * @param staffSalaryId StaffSalary id. (required)
     * @param staffSalaryComponentId StaffSalaryComponent id. (required)
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
        value = StaffSalaryApi.PATH_DELETE_STAFF_SALARY_COMPONENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStaffSalaryComponent(
         @PathVariable("staffSalaryId") UUID staffSalaryId,
         @PathVariable("staffSalaryComponentId") UUID staffSalaryComponentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_STAFF_SALARY = "/v1/payroll/staff-salaries/{staffSalaryId}";
    /**
     * GET /v1/payroll/staff-salaries/{staffSalaryId} : Get StaffSalary
     * Get one StaffSalary by id. The ETag header carries the row version.
     *
     * @param staffSalaryId StaffSalary id. (required)
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
        value = StaffSalaryApi.PATH_GET_STAFF_SALARY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StaffSalary> getStaffSalary(
         @PathVariable("staffSalaryId") UUID staffSalaryId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_STAFF_SALARY_COMPONENT = "/v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}";
    /**
     * GET /v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId} : Get StaffSalaryComponent
     * Get one StaffSalaryComponent by id. The ETag header carries the row version.
     *
     * @param staffSalaryId StaffSalary id. (required)
     * @param staffSalaryComponentId StaffSalaryComponent id. (required)
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
        value = StaffSalaryApi.PATH_GET_STAFF_SALARY_COMPONENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StaffSalaryComponent> getStaffSalaryComponent(
         @PathVariable("staffSalaryId") UUID staffSalaryId,
         @PathVariable("staffSalaryComponentId") UUID staffSalaryComponentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STAFF_SALARIES = "/v1/payroll/staff-salaries";
    /**
     * GET /v1/payroll/staff-salaries : List StaffSalaries
     * List StaffSalaries visible to the caller, page by page, with filtering and sorting.
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
        value = StaffSalaryApi.PATH_LIST_STAFF_SALARIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StaffSalary>> listStaffSalaries(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STAFF_SALARY_COMPONENTS = "/v1/payroll/staff-salaries/{staffSalaryId}/components";
    /**
     * GET /v1/payroll/staff-salaries/{staffSalaryId}/components : List StaffSalaryComponents
     * List StaffSalaryComponents visible to the caller, page by page, with filtering and sorting.
     *
     * @param staffSalaryId StaffSalary id. (required)
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
        value = StaffSalaryApi.PATH_LIST_STAFF_SALARY_COMPONENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StaffSalaryComponent>> listStaffSalaryComponents(
         @PathVariable("staffSalaryId") UUID staffSalaryId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_STAFF_SALARY = "/v1/payroll/staff-salaries/{staffSalaryId}";
    /**
     * PATCH /v1/payroll/staff-salaries/{staffSalaryId} : Update StaffSalary
     * Partially update a StaffSalary (updateMask). Requires If-Match with the current ETag.
     *
     * @param staffSalaryId StaffSalary id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param staffSalaryUpdate  (required)
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
        value = StaffSalaryApi.PATH_UPDATE_STAFF_SALARY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StaffSalary> updateStaffSalary(
         @PathVariable("staffSalaryId") UUID staffSalaryId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StaffSalaryUpdate staffSalaryUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_STAFF_SALARY_COMPONENT = "/v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId}";
    /**
     * PATCH /v1/payroll/staff-salaries/{staffSalaryId}/components/{staffSalaryComponentId} : Update StaffSalaryComponent
     * Partially update a StaffSalaryComponent (updateMask). Requires If-Match with the current ETag.
     *
     * @param staffSalaryId StaffSalary id. (required)
     * @param staffSalaryComponentId StaffSalaryComponent id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param staffSalaryComponentUpdate  (required)
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
        value = StaffSalaryApi.PATH_UPDATE_STAFF_SALARY_COMPONENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StaffSalaryComponent> updateStaffSalaryComponent(
         @PathVariable("staffSalaryId") UUID staffSalaryId,
         @PathVariable("staffSalaryComponentId") UUID staffSalaryComponentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StaffSalaryComponentUpdate staffSalaryComponentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
