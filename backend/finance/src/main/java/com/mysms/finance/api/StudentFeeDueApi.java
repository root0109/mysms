// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.model.GenerateFeeDuesRequest;
import org.springframework.lang.Nullable;
import com.mysms.finance.api.model.Operation;
import com.mysms.finance.api.model.StudentFeeDue;
import com.mysms.finance.api.model.StudentFeeDueUpdate;
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
public interface StudentFeeDueApi {

    String PATH_GENERATE_FEE_DUES = "/v1/finance/student-fee-dues:generate";
    /**
     * POST /v1/finance/student-fee-dues:generate : Create dues for students from a fee plan
     * Create dues for students from a fee plan.
     *
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (required)
     * @param generateFeeDuesRequest  (required)
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
        value = StudentFeeDueApi.PATH_GENERATE_FEE_DUES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> generateFeeDues(
        @NotNull  @RequestHeader(value = "Idempotency-Key", required = true) UUID idempotencyKey,
         @Valid @RequestBody GenerateFeeDuesRequest generateFeeDuesRequest
    );


    String PATH_GET_STUDENT_FEE_DUE = "/v1/finance/student-fee-dues/{studentFeeDueId}";
    /**
     * GET /v1/finance/student-fee-dues/{studentFeeDueId} : Get StudentFeeDue
     * Get one StudentFeeDue by id. The ETag header carries the row version.
     *
     * @param studentFeeDueId StudentFeeDue id. (required)
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
        value = StudentFeeDueApi.PATH_GET_STUDENT_FEE_DUE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StudentFeeDue> getStudentFeeDue(
         @PathVariable("studentFeeDueId") UUID studentFeeDueId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STUDENT_FEE_DUES = "/v1/finance/student-fee-dues";
    /**
     * GET /v1/finance/student-fee-dues : List StudentFeeDues
     * List StudentFeeDues visible to the caller, page by page, with filtering and sorting.
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
        value = StudentFeeDueApi.PATH_LIST_STUDENT_FEE_DUES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StudentFeeDue>> listStudentFeeDues(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_STUDENT_FEE_DUE = "/v1/finance/student-fee-dues/{studentFeeDueId}";
    /**
     * PATCH /v1/finance/student-fee-dues/{studentFeeDueId} : Update StudentFeeDue
     * Partially update a StudentFeeDue (updateMask). Requires If-Match with the current ETag.
     *
     * @param studentFeeDueId StudentFeeDue id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param studentFeeDueUpdate  (required)
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
        value = StudentFeeDueApi.PATH_UPDATE_STUDENT_FEE_DUE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentFeeDue> updateStudentFeeDue(
         @PathVariable("studentFeeDueId") UUID studentFeeDueId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StudentFeeDueUpdate studentFeeDueUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
