// Owned API contract of sis.
package com.mysms.sis.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.model.ChangeStudentStatusRequest;
import com.mysms.sis.api.model.CreateStudentPortalAccountRequest;
import com.mysms.sis.api.model.IssueBonafideCertificateRequest;
import org.springframework.lang.Nullable;
import com.mysms.sis.api.model.Operation;
import com.mysms.sis.api.model.Student;
import com.mysms.sis.api.model.StudentGuardian;
import com.mysms.sis.api.model.StudentGuardianUpdate;
import com.mysms.sis.api.model.StudentSibling;
import com.mysms.sis.api.model.StudentSiblingUpdate;
import com.mysms.sis.api.model.StudentSummary;
import com.mysms.sis.api.model.StudentUpdate;
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
public interface StudentApi {

    String PATH_CHANGE_STUDENT_STATUS = "/v1/sis/students/{studentId}:changeStatus";
    /**
     * POST /v1/sis/students/{studentId}:changeStatus : Change status (active, inactive, withdrawn, transferred, alumni, graduated) with exit details
     * Change status (active, inactive, withdrawn, transferred, alumni, graduated) with exit details.
     *
     * @param studentId  (required)
     * @param changeStudentStatusRequest  (required)
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
        value = StudentApi.PATH_CHANGE_STUDENT_STATUS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Student> changeStudentStatus(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestBody ChangeStudentStatusRequest changeStudentStatusRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_STUDENT = "/v1/sis/students";
    /**
     * POST /v1/sis/students : Create Student
     * Create a Student. Supports Idempotency-Key and validateOnly.
     *
     * @param student  (required)
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
        value = StudentApi.PATH_CREATE_STUDENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Student> createStudent(
         @Valid @RequestBody Student student,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_STUDENT_GUARDIAN = "/v1/sis/students/{studentId}/guardians";
    /**
     * POST /v1/sis/students/{studentId}/guardians : Create StudentGuardian
     * Create a StudentGuardian. Supports Idempotency-Key and validateOnly.
     *
     * @param studentId Student id. (required)
     * @param studentGuardian  (required)
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
        value = StudentApi.PATH_CREATE_STUDENT_GUARDIAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentGuardian> createStudentGuardian(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestBody StudentGuardian studentGuardian,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_STUDENT_PORTAL_ACCOUNT = "/v1/sis/students/{studentId}:createPortalAccount";
    /**
     * POST /v1/sis/students/{studentId}:createPortalAccount : Create the student login account and grant the student role
     * Create the student login account and grant the student role.
     *
     * @param studentId  (required)
     * @param createStudentPortalAccountRequest  (required)
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
        value = StudentApi.PATH_CREATE_STUDENT_PORTAL_ACCOUNT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Student> createStudentPortalAccount(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestBody CreateStudentPortalAccountRequest createStudentPortalAccountRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_STUDENT_SIBLING = "/v1/sis/students/{studentId}/siblings";
    /**
     * POST /v1/sis/students/{studentId}/siblings : Create StudentSibling
     * Create a StudentSibling. Supports Idempotency-Key and validateOnly.
     *
     * @param studentId Student id. (required)
     * @param studentSibling  (required)
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
        value = StudentApi.PATH_CREATE_STUDENT_SIBLING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentSibling> createStudentSibling(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestBody StudentSibling studentSibling,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_STUDENT = "/v1/sis/students/{studentId}";
    /**
     * DELETE /v1/sis/students/{studentId} : Delete Student
     * Soft delete (sets deleted_at).
     *
     * @param studentId Student id. (required)
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
        value = StudentApi.PATH_DELETE_STUDENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStudent(
         @PathVariable("studentId") UUID studentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_STUDENT_GUARDIAN = "/v1/sis/students/{studentId}/guardians/{studentGuardianId}";
    /**
     * DELETE /v1/sis/students/{studentId}/guardians/{studentGuardianId} : Delete StudentGuardian
     * Soft delete (sets deleted_at).
     *
     * @param studentId Student id. (required)
     * @param studentGuardianId StudentGuardian id. (required)
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
        value = StudentApi.PATH_DELETE_STUDENT_GUARDIAN,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStudentGuardian(
         @PathVariable("studentId") UUID studentId,
         @PathVariable("studentGuardianId") UUID studentGuardianId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_STUDENT_SIBLING = "/v1/sis/students/{studentId}/siblings/{studentSiblingId}";
    /**
     * DELETE /v1/sis/students/{studentId}/siblings/{studentSiblingId} : Delete StudentSibling
     * Soft delete (sets deleted_at).
     *
     * @param studentId Student id. (required)
     * @param studentSiblingId StudentSibling id. (required)
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
        value = StudentApi.PATH_DELETE_STUDENT_SIBLING,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteStudentSibling(
         @PathVariable("studentId") UUID studentId,
         @PathVariable("studentSiblingId") UUID studentSiblingId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_STUDENT = "/v1/sis/students/{studentId}";
    /**
     * GET /v1/sis/students/{studentId} : Get Student
     * Get one Student by id. The ETag header carries the row version.
     *
     * @param studentId Student id. (required)
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
        value = StudentApi.PATH_GET_STUDENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Student> getStudent(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_STUDENT_GUARDIAN = "/v1/sis/students/{studentId}/guardians/{studentGuardianId}";
    /**
     * GET /v1/sis/students/{studentId}/guardians/{studentGuardianId} : Get StudentGuardian
     * Get one StudentGuardian by id. The ETag header carries the row version.
     *
     * @param studentId Student id. (required)
     * @param studentGuardianId StudentGuardian id. (required)
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
        value = StudentApi.PATH_GET_STUDENT_GUARDIAN,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StudentGuardian> getStudentGuardian(
         @PathVariable("studentId") UUID studentId,
         @PathVariable("studentGuardianId") UUID studentGuardianId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_STUDENT_SIBLING = "/v1/sis/students/{studentId}/siblings/{studentSiblingId}";
    /**
     * GET /v1/sis/students/{studentId}/siblings/{studentSiblingId} : Get StudentSibling
     * Get one StudentSibling by id. The ETag header carries the row version.
     *
     * @param studentId Student id. (required)
     * @param studentSiblingId StudentSibling id. (required)
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
        value = StudentApi.PATH_GET_STUDENT_SIBLING,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StudentSibling> getStudentSibling(
         @PathVariable("studentId") UUID studentId,
         @PathVariable("studentSiblingId") UUID studentSiblingId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_STUDENT_SUMMARY = "/v1/sis/students/{studentId}:summary";
    /**
     * GET /v1/sis/students/{studentId}:summary : Student 360 overview
     * Student 360 overview.
     *
     * @param studentId  (required)
     * @param academicYearId Year (default current). (optional)
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
        value = StudentApi.PATH_GET_STUDENT_SUMMARY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<StudentSummary> getStudentSummary(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestParam(value = "academicYearId", required = false) @Nullable UUID academicYearId
    );


    String PATH_ISSUE_BONAFIDE_CERTIFICATE = "/v1/sis/students/{studentId}:issueBonafideCertificate";
    /**
     * POST /v1/sis/students/{studentId}:issueBonafideCertificate : Generate a bonafide certificate PDF and record its number
     * Generate a bonafide certificate PDF and record its number.
     *
     * @param studentId  (required)
     * @param issueBonafideCertificateRequest  (required)
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
        value = StudentApi.PATH_ISSUE_BONAFIDE_CERTIFICATE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> issueBonafideCertificate(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestBody IssueBonafideCertificateRequest issueBonafideCertificateRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_STUDENT_GUARDIANS = "/v1/sis/students/{studentId}/guardians";
    /**
     * GET /v1/sis/students/{studentId}/guardians : List StudentGuardians
     * List StudentGuardians visible to the caller, page by page, with filtering and sorting.
     *
     * @param studentId Student id. (required)
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
        value = StudentApi.PATH_LIST_STUDENT_GUARDIANS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StudentGuardian>> listStudentGuardians(
         @PathVariable("studentId") UUID studentId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STUDENT_SIBLINGS = "/v1/sis/students/{studentId}/siblings";
    /**
     * GET /v1/sis/students/{studentId}/siblings : List StudentSiblings
     * List StudentSiblings visible to the caller, page by page, with filtering and sorting.
     *
     * @param studentId Student id. (required)
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
        value = StudentApi.PATH_LIST_STUDENT_SIBLINGS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<StudentSibling>> listStudentSiblings(
         @PathVariable("studentId") UUID studentId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_STUDENTS = "/v1/sis/students";
    /**
     * GET /v1/sis/students : List Students
     * List Students visible to the caller, page by page, with filtering and sorting.
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
        value = StudentApi.PATH_LIST_STUDENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Student>> listStudents(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_STUDENT = "/v1/sis/students/{studentId}";
    /**
     * PATCH /v1/sis/students/{studentId} : Update Student
     * Partially update a Student (updateMask). Requires If-Match with the current ETag.
     *
     * @param studentId Student id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param studentUpdate  (required)
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
        value = StudentApi.PATH_UPDATE_STUDENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Student> updateStudent(
         @PathVariable("studentId") UUID studentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StudentUpdate studentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_STUDENT_GUARDIAN = "/v1/sis/students/{studentId}/guardians/{studentGuardianId}";
    /**
     * PATCH /v1/sis/students/{studentId}/guardians/{studentGuardianId} : Update StudentGuardian
     * Partially update a StudentGuardian (updateMask). Requires If-Match with the current ETag.
     *
     * @param studentId Student id. (required)
     * @param studentGuardianId StudentGuardian id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param studentGuardianUpdate  (required)
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
        value = StudentApi.PATH_UPDATE_STUDENT_GUARDIAN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentGuardian> updateStudentGuardian(
         @PathVariable("studentId") UUID studentId,
         @PathVariable("studentGuardianId") UUID studentGuardianId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StudentGuardianUpdate studentGuardianUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_STUDENT_SIBLING = "/v1/sis/students/{studentId}/siblings/{studentSiblingId}";
    /**
     * PATCH /v1/sis/students/{studentId}/siblings/{studentSiblingId} : Update StudentSibling
     * Partially update a StudentSibling (updateMask). Requires If-Match with the current ETag.
     *
     * @param studentId Student id. (required)
     * @param studentSiblingId StudentSibling id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param studentSiblingUpdate  (required)
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
        value = StudentApi.PATH_UPDATE_STUDENT_SIBLING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<StudentSibling> updateStudentSibling(
         @PathVariable("studentId") UUID studentId,
         @PathVariable("studentSiblingId") UUID studentSiblingId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody StudentSiblingUpdate studentSiblingUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
