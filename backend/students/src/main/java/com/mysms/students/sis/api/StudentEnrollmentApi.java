// Owned API contract of sis.
package com.mysms.students.sis.api;

import com.mysms.core.paging.PagingInfo;
import org.springframework.lang.Nullable;
import com.mysms.students.sis.api.model.StudentEnrollment;
import com.mysms.students.sis.api.model.StudentEnrollmentUpdate;
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
public interface StudentEnrollmentApi {

    String PATH_CREATE_STUDENT_ENROLLMENT = "/v1/sis/student-enrollments";
    /**
     * POST /v1/sis/student-enrollments : Create StudentEnrollment
     * Create a StudentEnrollment. Supports Idempotency-Key and validateOnly.
     *
     * @param studentEnrollment  (required)
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
        value = StudentEnrollmentApi.PATH_CREATE_STUDENT_ENROLLMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentEnrollment> createStudentEnrollment(
         @Valid @RequestBody StudentEnrollment studentEnrollment,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_STUDENT_ENROLLMENT = "/v1/sis/student-enrollments/{studentEnrollmentId}";
    /**
     * DELETE /v1/sis/student-enrollments/{studentEnrollmentId} : Delete StudentEnrollment
     * Soft delete (sets deleted_at).
     *
     * @param studentEnrollmentId StudentEnrollment id. (required)
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
        value = StudentEnrollmentApi.PATH_DELETE_STUDENT_ENROLLMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStudentEnrollment(
         @PathVariable("studentEnrollmentId") UUID studentEnrollmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_STUDENT_ENROLLMENT = "/v1/sis/student-enrollments/{studentEnrollmentId}";
    /**
     * GET /v1/sis/student-enrollments/{studentEnrollmentId} : Get StudentEnrollment
     * Get one StudentEnrollment by id. The ETag header carries the row version.
     *
     * @param studentEnrollmentId StudentEnrollment id. (required)
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
        value = StudentEnrollmentApi.PATH_GET_STUDENT_ENROLLMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StudentEnrollment> getStudentEnrollment(
         @PathVariable("studentEnrollmentId") UUID studentEnrollmentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CLASS_ROSTER = "/v1/sis/class-sections/{classSectionId}:roster";
    /**
     * GET /v1/sis/class-sections/{classSectionId}:roster : Students placed in the class
     * Students placed in the class.
     *
     * @param classSectionId  (required)
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
        value = StudentEnrollmentApi.PATH_LIST_CLASS_ROSTER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StudentEnrollment>> listClassRoster(
         @PathVariable("classSectionId") UUID classSectionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_LIST_STUDENT_ENROLLMENTS = "/v1/sis/student-enrollments";
    /**
     * GET /v1/sis/student-enrollments : List StudentEnrollments
     * List StudentEnrollments visible to the caller, page by page, with filtering and sorting.
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
        value = StudentEnrollmentApi.PATH_LIST_STUDENT_ENROLLMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StudentEnrollment>> listStudentEnrollments(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_STUDENT_ENROLLMENT = "/v1/sis/student-enrollments/{studentEnrollmentId}";
    /**
     * PATCH /v1/sis/student-enrollments/{studentEnrollmentId} : Update StudentEnrollment
     * Partially update a StudentEnrollment (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param studentEnrollmentId StudentEnrollment id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param studentEnrollmentUpdate  (required)
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
        value = StudentEnrollmentApi.PATH_UPDATE_STUDENT_ENROLLMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentEnrollment> updateStudentEnrollment(
         @PathVariable("studentEnrollmentId") UUID studentEnrollmentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StudentEnrollmentUpdate studentEnrollmentUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
