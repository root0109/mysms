// Owned API contract of org (JAVA-CODING-RULES §7); first materialized from api/openapi/org.yaml on 2026-11-10.
package com.mysms.org.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.org.api.model.AcademicTerm;
import com.mysms.org.api.model.AcademicTermList;
import com.mysms.org.api.model.AcademicTermUpdate;
import com.mysms.org.api.model.AcademicYear;
import com.mysms.org.api.model.AcademicYearUpdate;
import com.mysms.org.api.model.ActivateAcademicYearRequest;
import com.mysms.org.api.model.CloseAcademicYearRequest;
import org.springframework.lang.Nullable;
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
public interface AcademicYearApi {

    String PATH_ACTIVATE_ACADEMIC_YEAR = "/v1/org/academic-years/{academicYearId}:activate";
    /**
     * POST /v1/org/academic-years/{academicYearId}:activate : Make the year active and current for its institution
     * Make the year active and current for its institution.
     *
     * @param academicYearId  (required)
     * @param activateAcademicYearRequest  (required)
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
        value = AcademicYearApi.PATH_ACTIVATE_ACADEMIC_YEAR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AcademicYear> activateAcademicYear(
         @PathVariable("academicYearId") UUID academicYearId,
         @Valid @RequestBody ActivateAcademicYearRequest activateAcademicYearRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CLOSE_ACADEMIC_YEAR = "/v1/org/academic-years/{academicYearId}:close";
    /**
     * POST /v1/org/academic-years/{academicYearId}:close : Close the year (read-only afterwards)
     * Close the year (read-only afterwards).
     *
     * @param academicYearId  (required)
     * @param closeAcademicYearRequest  (required)
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
        value = AcademicYearApi.PATH_CLOSE_ACADEMIC_YEAR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AcademicYear> closeAcademicYear(
         @PathVariable("academicYearId") UUID academicYearId,
         @Valid @RequestBody CloseAcademicYearRequest closeAcademicYearRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_ACADEMIC_TERM = "/v1/org/academic-years/{academicYearId}/terms";
    /**
     * POST /v1/org/academic-years/{academicYearId}/terms : Create AcademicTerm
     * Create a AcademicTerm. Supports Idempotency-Key and validateOnly.
     *
     * @param academicYearId AcademicYear id. (required)
     * @param academicTerm  (required)
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
        value = AcademicYearApi.PATH_CREATE_ACADEMIC_TERM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AcademicTerm> createAcademicTerm(
         @PathVariable("academicYearId") UUID academicYearId,
         @Valid @RequestBody AcademicTerm academicTerm,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_ACADEMIC_YEAR = "/v1/org/academic-years";
    /**
     * POST /v1/org/academic-years : Create AcademicYear
     * Create a AcademicYear. Supports Idempotency-Key and validateOnly.
     *
     * @param academicYear  (required)
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
        value = AcademicYearApi.PATH_CREATE_ACADEMIC_YEAR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AcademicYear> createAcademicYear(
         @Valid @RequestBody AcademicYear academicYear,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ACADEMIC_TERM = "/v1/org/academic-years/{academicYearId}/terms/{academicTermId}";
    /**
     * DELETE /v1/org/academic-years/{academicYearId}/terms/{academicTermId} : Delete AcademicTerm
     * Soft delete (sets deleted_at).
     *
     * @param academicYearId AcademicYear id. (required)
     * @param academicTermId AcademicTerm id. (required)
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
        value = AcademicYearApi.PATH_DELETE_ACADEMIC_TERM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAcademicTerm(
         @PathVariable("academicYearId") UUID academicYearId,
         @PathVariable("academicTermId") UUID academicTermId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ACADEMIC_YEAR = "/v1/org/academic-years/{academicYearId}";
    /**
     * DELETE /v1/org/academic-years/{academicYearId} : Delete AcademicYear
     * Soft delete (sets deleted_at).
     *
     * @param academicYearId AcademicYear id. (required)
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
        value = AcademicYearApi.PATH_DELETE_ACADEMIC_YEAR,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAcademicYear(
         @PathVariable("academicYearId") UUID academicYearId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GENERATE_TERMS = "/v1/org/academic-years/{academicYearId}:generateTerms";
    /**
     * POST /v1/org/academic-years/{academicYearId}:generateTerms : Create terms from term_structure
     * Create terms from term_structure.
     *
     * @param academicYearId  (required)
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
        value = AcademicYearApi.PATH_GENERATE_TERMS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AcademicTermList> generateTerms(
         @PathVariable("academicYearId") UUID academicYearId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_ACADEMIC_TERM = "/v1/org/academic-years/{academicYearId}/terms/{academicTermId}";
    /**
     * GET /v1/org/academic-years/{academicYearId}/terms/{academicTermId} : Get AcademicTerm
     * Get one AcademicTerm by id. The ETag header carries the row version.
     *
     * @param academicYearId AcademicYear id. (required)
     * @param academicTermId AcademicTerm id. (required)
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
        value = AcademicYearApi.PATH_GET_ACADEMIC_TERM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AcademicTerm> getAcademicTerm(
         @PathVariable("academicYearId") UUID academicYearId,
         @PathVariable("academicTermId") UUID academicTermId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ACADEMIC_YEAR = "/v1/org/academic-years/{academicYearId}";
    /**
     * GET /v1/org/academic-years/{academicYearId} : Get AcademicYear
     * Get one AcademicYear by id. The ETag header carries the row version.
     *
     * @param academicYearId AcademicYear id. (required)
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
        value = AcademicYearApi.PATH_GET_ACADEMIC_YEAR,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AcademicYear> getAcademicYear(
         @PathVariable("academicYearId") UUID academicYearId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ACADEMIC_TERMS = "/v1/org/academic-years/{academicYearId}/terms";
    /**
     * GET /v1/org/academic-years/{academicYearId}/terms : List AcademicTerms
     * List AcademicTerms visible to the caller, page by page, with filtering and sorting.
     *
     * @param academicYearId AcademicYear id. (required)
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
        value = AcademicYearApi.PATH_LIST_ACADEMIC_TERMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AcademicTerm>> listAcademicTerms(
         @PathVariable("academicYearId") UUID academicYearId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ACADEMIC_YEARS = "/v1/org/academic-years";
    /**
     * GET /v1/org/academic-years : List AcademicYears
     * List AcademicYears visible to the caller, page by page, with filtering and sorting.
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
        value = AcademicYearApi.PATH_LIST_ACADEMIC_YEARS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AcademicYear>> listAcademicYears(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_ACADEMIC_TERM = "/v1/org/academic-years/{academicYearId}/terms/{academicTermId}";
    /**
     * PATCH /v1/org/academic-years/{academicYearId}/terms/{academicTermId} : Update AcademicTerm
     * Partially update a AcademicTerm (updateMask). Requires If-Match with the current ETag.
     *
     * @param academicYearId AcademicYear id. (required)
     * @param academicTermId AcademicTerm id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param academicTermUpdate  (required)
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
        value = AcademicYearApi.PATH_UPDATE_ACADEMIC_TERM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AcademicTerm> updateAcademicTerm(
         @PathVariable("academicYearId") UUID academicYearId,
         @PathVariable("academicTermId") UUID academicTermId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AcademicTermUpdate academicTermUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ACADEMIC_YEAR = "/v1/org/academic-years/{academicYearId}";
    /**
     * PATCH /v1/org/academic-years/{academicYearId} : Update AcademicYear
     * Partially update a AcademicYear (updateMask). Requires If-Match with the current ETag.
     *
     * @param academicYearId AcademicYear id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param academicYearUpdate  (required)
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
        value = AcademicYearApi.PATH_UPDATE_ACADEMIC_YEAR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AcademicYear> updateAcademicYear(
         @PathVariable("academicYearId") UUID academicYearId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AcademicYearUpdate academicYearUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
