// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.sis.api.model.ProgramEnrollment;
import com.mysms.sis.api.model.ProgramEnrollmentSubject;
import com.mysms.sis.api.model.ProgramEnrollmentSubjectUpdate;
import com.mysms.sis.api.model.ProgramEnrollmentUpdate;
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
public interface ProgramEnrollmentApi {

    String PATH_CREATE_PROGRAM_ENROLLMENT = "/v1/sis/program-enrollments";
    /**
     * POST /v1/sis/program-enrollments : Create ProgramEnrollment
     * Create a ProgramEnrollment. Supports Idempotency-Key and validateOnly.
     *
     * @param programEnrollment  (required)
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
        value = ProgramEnrollmentApi.PATH_CREATE_PROGRAM_ENROLLMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ProgramEnrollment> createProgramEnrollment(
         @Valid @RequestBody ProgramEnrollment programEnrollment,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PROGRAM_ENROLLMENT_SUBJECT = "/v1/sis/program-enrollments/{programEnrollmentId}/subjects";
    /**
     * POST /v1/sis/program-enrollments/{programEnrollmentId}/subjects : Create ProgramEnrollmentSubject
     * Create a ProgramEnrollmentSubject. Supports Idempotency-Key and validateOnly.
     *
     * @param programEnrollmentId ProgramEnrollment id. (required)
     * @param programEnrollmentSubject  (required)
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
        value = ProgramEnrollmentApi.PATH_CREATE_PROGRAM_ENROLLMENT_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ProgramEnrollmentSubject> createProgramEnrollmentSubject(
         @PathVariable("programEnrollmentId") UUID programEnrollmentId,
         @Valid @RequestBody ProgramEnrollmentSubject programEnrollmentSubject,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_PROGRAM_ENROLLMENT = "/v1/sis/program-enrollments/{programEnrollmentId}";
    /**
     * DELETE /v1/sis/program-enrollments/{programEnrollmentId} : Delete ProgramEnrollment
     * Soft delete (sets deleted_at).
     *
     * @param programEnrollmentId ProgramEnrollment id. (required)
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
        value = ProgramEnrollmentApi.PATH_DELETE_PROGRAM_ENROLLMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteProgramEnrollment(
         @PathVariable("programEnrollmentId") UUID programEnrollmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PROGRAM_ENROLLMENT_SUBJECT = "/v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}";
    /**
     * DELETE /v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId} : Delete ProgramEnrollmentSubject
     * Soft delete (sets deleted_at).
     *
     * @param programEnrollmentId ProgramEnrollment id. (required)
     * @param programEnrollmentSubjectId ProgramEnrollmentSubject id. (required)
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
        value = ProgramEnrollmentApi.PATH_DELETE_PROGRAM_ENROLLMENT_SUBJECT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteProgramEnrollmentSubject(
         @PathVariable("programEnrollmentId") UUID programEnrollmentId,
         @PathVariable("programEnrollmentSubjectId") UUID programEnrollmentSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_PROGRAM_ENROLLMENT = "/v1/sis/program-enrollments/{programEnrollmentId}";
    /**
     * GET /v1/sis/program-enrollments/{programEnrollmentId} : Get ProgramEnrollment
     * Get one ProgramEnrollment by id. The ETag header carries the row version.
     *
     * @param programEnrollmentId ProgramEnrollment id. (required)
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
        value = ProgramEnrollmentApi.PATH_GET_PROGRAM_ENROLLMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ProgramEnrollment> getProgramEnrollment(
         @PathVariable("programEnrollmentId") UUID programEnrollmentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PROGRAM_ENROLLMENT_SUBJECT = "/v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}";
    /**
     * GET /v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId} : Get ProgramEnrollmentSubject
     * Get one ProgramEnrollmentSubject by id. The ETag header carries the row version.
     *
     * @param programEnrollmentId ProgramEnrollment id. (required)
     * @param programEnrollmentSubjectId ProgramEnrollmentSubject id. (required)
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
        value = ProgramEnrollmentApi.PATH_GET_PROGRAM_ENROLLMENT_SUBJECT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ProgramEnrollmentSubject> getProgramEnrollmentSubject(
         @PathVariable("programEnrollmentId") UUID programEnrollmentId,
         @PathVariable("programEnrollmentSubjectId") UUID programEnrollmentSubjectId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PROGRAM_ENROLLMENT_SUBJECTS = "/v1/sis/program-enrollments/{programEnrollmentId}/subjects";
    /**
     * GET /v1/sis/program-enrollments/{programEnrollmentId}/subjects : List ProgramEnrollmentSubjects
     * List ProgramEnrollmentSubjects visible to the caller, page by page, with filtering and sorting.
     *
     * @param programEnrollmentId ProgramEnrollment id. (required)
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
        value = ProgramEnrollmentApi.PATH_LIST_PROGRAM_ENROLLMENT_SUBJECTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ProgramEnrollmentSubject>> listProgramEnrollmentSubjects(
         @PathVariable("programEnrollmentId") UUID programEnrollmentId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PROGRAM_ENROLLMENTS = "/v1/sis/program-enrollments";
    /**
     * GET /v1/sis/program-enrollments : List ProgramEnrollments
     * List ProgramEnrollments visible to the caller, page by page, with filtering and sorting.
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
        value = ProgramEnrollmentApi.PATH_LIST_PROGRAM_ENROLLMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ProgramEnrollment>> listProgramEnrollments(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_PROGRAM_ENROLLMENT = "/v1/sis/program-enrollments/{programEnrollmentId}";
    /**
     * PATCH /v1/sis/program-enrollments/{programEnrollmentId} : Update ProgramEnrollment
     * Partially update a ProgramEnrollment (updateMask). Requires If-Match with the current ETag.
     *
     * @param programEnrollmentId ProgramEnrollment id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param programEnrollmentUpdate  (required)
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
        value = ProgramEnrollmentApi.PATH_UPDATE_PROGRAM_ENROLLMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ProgramEnrollment> updateProgramEnrollment(
         @PathVariable("programEnrollmentId") UUID programEnrollmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ProgramEnrollmentUpdate programEnrollmentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PROGRAM_ENROLLMENT_SUBJECT = "/v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId}";
    /**
     * PATCH /v1/sis/program-enrollments/{programEnrollmentId}/subjects/{programEnrollmentSubjectId} : Update ProgramEnrollmentSubject
     * Partially update a ProgramEnrollmentSubject (updateMask). Requires If-Match with the current ETag.
     *
     * @param programEnrollmentId ProgramEnrollment id. (required)
     * @param programEnrollmentSubjectId ProgramEnrollmentSubject id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param programEnrollmentSubjectUpdate  (required)
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
        value = ProgramEnrollmentApi.PATH_UPDATE_PROGRAM_ENROLLMENT_SUBJECT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ProgramEnrollmentSubject> updateProgramEnrollmentSubject(
         @PathVariable("programEnrollmentId") UUID programEnrollmentId,
         @PathVariable("programEnrollmentSubjectId") UUID programEnrollmentSubjectId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ProgramEnrollmentSubjectUpdate programEnrollmentSubjectUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
