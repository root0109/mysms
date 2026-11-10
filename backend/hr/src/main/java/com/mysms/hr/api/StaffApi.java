// Owned API contract of hr (JAVA-CODING-RULES §7); first materialized from api/openapi/hr.yaml on 2026-11-10.
package com.mysms.hr.api;

import com.mysms.hr.api.model.LeaveBalanceSummary;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hr.api.model.ChangeStaffStatusRequest;
import com.mysms.hr.api.model.CreateStaffPortalAccountRequest;
import org.springframework.lang.Nullable;
import com.mysms.hr.api.model.Staff;
import com.mysms.hr.api.model.StaffDepartment;
import com.mysms.hr.api.model.StaffDepartmentUpdate;
import com.mysms.hr.api.model.StaffSubject;
import com.mysms.hr.api.model.StaffSubjectUpdate;
import com.mysms.hr.api.model.StaffUpdate;
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
public interface StaffApi {

    String PATH_CHANGE_STAFF_STATUS = "/v1/hr/staff/{staffId}:changeStatus";
    /**
     * POST /v1/hr/staff/{staffId}:changeStatus : Activate, deactivate or exit
     * Activate, deactivate or exit.
     *
     * @param staffId  (required)
     * @param changeStaffStatusRequest  (required)
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
        value = StaffApi.PATH_CHANGE_STAFF_STATUS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Staff> changeStaffStatus(
         @PathVariable("staffId") UUID staffId,
         @Valid @RequestBody ChangeStaffStatusRequest changeStaffStatusRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_STAFF = "/v1/hr/staff";
    /**
     * POST /v1/hr/staff : Create Staff
     * Create a Staff. Supports Idempotency-Key and validateOnly.
     *
     * @param staff  (required)
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
        value = StaffApi.PATH_CREATE_STAFF,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Staff> createStaff(
         @Valid @RequestBody Staff staff,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_STAFF_DEPARTMENT = "/v1/hr/staff/{staffId}/departments";
    /**
     * POST /v1/hr/staff/{staffId}/departments : Create StaffDepartment
     * Create a StaffDepartment. Supports Idempotency-Key and validateOnly.
     *
     * @param staffId Staff id. (required)
     * @param staffDepartment  (required)
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
        value = StaffApi.PATH_CREATE_STAFF_DEPARTMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StaffDepartment> createStaffDepartment(
         @PathVariable("staffId") UUID staffId,
         @Valid @RequestBody StaffDepartment staffDepartment,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_STAFF_PORTAL_ACCOUNT = "/v1/hr/staff/{staffId}:createPortalAccount";
    /**
     * POST /v1/hr/staff/{staffId}:createPortalAccount : Create the staff login account
     * Create the staff login account.
     *
     * @param staffId  (required)
     * @param createStaffPortalAccountRequest  (required)
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
        value = StaffApi.PATH_CREATE_STAFF_PORTAL_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Staff> createStaffPortalAccount(
         @PathVariable("staffId") UUID staffId,
         @Valid @RequestBody CreateStaffPortalAccountRequest createStaffPortalAccountRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_STAFF_SUBJECT = "/v1/hr/staff/{staffId}/subjects";
    /**
     * POST /v1/hr/staff/{staffId}/subjects : Create StaffSubject
     * Create a StaffSubject. Supports Idempotency-Key and validateOnly.
     *
     * @param staffId Staff id. (required)
     * @param staffSubject  (required)
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
        value = StaffApi.PATH_CREATE_STAFF_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StaffSubject> createStaffSubject(
         @PathVariable("staffId") UUID staffId,
         @Valid @RequestBody StaffSubject staffSubject,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_STAFF = "/v1/hr/staff/{staffId}";
    /**
     * DELETE /v1/hr/staff/{staffId} : Delete Staff
     * Soft delete (sets deleted_at).
     *
     * @param staffId Staff id. (required)
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
        value = StaffApi.PATH_DELETE_STAFF,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStaff(
         @PathVariable("staffId") UUID staffId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_STAFF_DEPARTMENT = "/v1/hr/staff/{staffId}/departments/{staffDepartmentId}";
    /**
     * DELETE /v1/hr/staff/{staffId}/departments/{staffDepartmentId} : Delete StaffDepartment
     * Soft delete (sets deleted_at).
     *
     * @param staffId Staff id. (required)
     * @param staffDepartmentId StaffDepartment id. (required)
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
        value = StaffApi.PATH_DELETE_STAFF_DEPARTMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStaffDepartment(
         @PathVariable("staffId") UUID staffId,
         @PathVariable("staffDepartmentId") UUID staffDepartmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_STAFF_SUBJECT = "/v1/hr/staff/{staffId}/subjects/{staffSubjectId}";
    /**
     * DELETE /v1/hr/staff/{staffId}/subjects/{staffSubjectId} : Delete StaffSubject
     * Soft delete (sets deleted_at).
     *
     * @param staffId Staff id. (required)
     * @param staffSubjectId StaffSubject id. (required)
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
        value = StaffApi.PATH_DELETE_STAFF_SUBJECT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStaffSubject(
         @PathVariable("staffId") UUID staffId,
         @PathVariable("staffSubjectId") UUID staffSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_STAFF = "/v1/hr/staff/{staffId}";
    /**
     * GET /v1/hr/staff/{staffId} : Get Staff
     * Get one Staff by id. The ETag header carries the row version.
     *
     * @param staffId Staff id. (required)
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
        value = StaffApi.PATH_GET_STAFF,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Staff> getStaff(
         @PathVariable("staffId") UUID staffId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_STAFF_DEPARTMENT = "/v1/hr/staff/{staffId}/departments/{staffDepartmentId}";
    /**
     * GET /v1/hr/staff/{staffId}/departments/{staffDepartmentId} : Get StaffDepartment
     * Get one StaffDepartment by id. The ETag header carries the row version.
     *
     * @param staffId Staff id. (required)
     * @param staffDepartmentId StaffDepartment id. (required)
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
        value = StaffApi.PATH_GET_STAFF_DEPARTMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StaffDepartment> getStaffDepartment(
         @PathVariable("staffId") UUID staffId,
         @PathVariable("staffDepartmentId") UUID staffDepartmentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_STAFF_LEAVE_BALANCE = "/v1/hr/staff/{staffId}:leaveBalance";
    /**
     * GET /v1/hr/staff/{staffId}:leaveBalance : Entitlement, taken, pending and available days per leave type
     * Entitlement, taken, pending and available days per leave type.
     *
     * @param staffId  (required)
     * @param academicYearId Academic year of the position. (required)
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
        value = StaffApi.PATH_GET_STAFF_LEAVE_BALANCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LeaveBalanceSummary>> getStaffLeaveBalance(
         @PathVariable("staffId") UUID staffId,
        @NotNull @Valid @RequestParam(value = "academicYearId", required = true) UUID academicYearId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_STAFF_SUBJECT = "/v1/hr/staff/{staffId}/subjects/{staffSubjectId}";
    /**
     * GET /v1/hr/staff/{staffId}/subjects/{staffSubjectId} : Get StaffSubject
     * Get one StaffSubject by id. The ETag header carries the row version.
     *
     * @param staffId Staff id. (required)
     * @param staffSubjectId StaffSubject id. (required)
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
        value = StaffApi.PATH_GET_STAFF_SUBJECT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StaffSubject> getStaffSubject(
         @PathVariable("staffId") UUID staffId,
         @PathVariable("staffSubjectId") UUID staffSubjectId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STAFF = "/v1/hr/staff";
    /**
     * GET /v1/hr/staff : List Staff
     * List Staff visible to the caller, page by page, with filtering and sorting.
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
        value = StaffApi.PATH_LIST_STAFF,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Staff>> listStaff(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STAFF_DEPARTMENTS = "/v1/hr/staff/{staffId}/departments";
    /**
     * GET /v1/hr/staff/{staffId}/departments : List StaffDepartments
     * List StaffDepartments visible to the caller, page by page, with filtering and sorting.
     *
     * @param staffId Staff id. (required)
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
        value = StaffApi.PATH_LIST_STAFF_DEPARTMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StaffDepartment>> listStaffDepartments(
         @PathVariable("staffId") UUID staffId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STAFF_SUBJECTS = "/v1/hr/staff/{staffId}/subjects";
    /**
     * GET /v1/hr/staff/{staffId}/subjects : List StaffSubjects
     * List StaffSubjects visible to the caller, page by page, with filtering and sorting.
     *
     * @param staffId Staff id. (required)
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
        value = StaffApi.PATH_LIST_STAFF_SUBJECTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StaffSubject>> listStaffSubjects(
         @PathVariable("staffId") UUID staffId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_STAFF = "/v1/hr/staff/{staffId}";
    /**
     * PATCH /v1/hr/staff/{staffId} : Update Staff
     * Partially update a Staff (updateMask). Requires If-Match with the current ETag.
     *
     * @param staffId Staff id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param staffUpdate  (required)
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
        value = StaffApi.PATH_UPDATE_STAFF,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Staff> updateStaff(
         @PathVariable("staffId") UUID staffId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StaffUpdate staffUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_STAFF_DEPARTMENT = "/v1/hr/staff/{staffId}/departments/{staffDepartmentId}";
    /**
     * PATCH /v1/hr/staff/{staffId}/departments/{staffDepartmentId} : Update StaffDepartment
     * Partially update a StaffDepartment (updateMask). Requires If-Match with the current ETag.
     *
     * @param staffId Staff id. (required)
     * @param staffDepartmentId StaffDepartment id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param staffDepartmentUpdate  (required)
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
        value = StaffApi.PATH_UPDATE_STAFF_DEPARTMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StaffDepartment> updateStaffDepartment(
         @PathVariable("staffId") UUID staffId,
         @PathVariable("staffDepartmentId") UUID staffDepartmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StaffDepartmentUpdate staffDepartmentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_STAFF_SUBJECT = "/v1/hr/staff/{staffId}/subjects/{staffSubjectId}";
    /**
     * PATCH /v1/hr/staff/{staffId}/subjects/{staffSubjectId} : Update StaffSubject
     * Partially update a StaffSubject (updateMask). Requires If-Match with the current ETag.
     *
     * @param staffId Staff id. (required)
     * @param staffSubjectId StaffSubject id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param staffSubjectUpdate  (required)
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
        value = StaffApi.PATH_UPDATE_STAFF_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StaffSubject> updateStaffSubject(
         @PathVariable("staffId") UUID staffId,
         @PathVariable("staffSubjectId") UUID staffSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StaffSubjectUpdate staffSubjectUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
