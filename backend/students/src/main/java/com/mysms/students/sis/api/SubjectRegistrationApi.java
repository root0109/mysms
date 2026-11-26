// Owned API contract of sis.
package com.mysms.students.sis.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.students.sis.api.model.ApproveSubjectRegistrationRequest;
import org.springframework.lang.Nullable;
import com.mysms.students.sis.api.model.RejectSubjectRegistrationRequest;
import com.mysms.students.sis.api.model.ResetSubjectRegistrationRequest;
import com.mysms.students.sis.api.model.SubjectRegistration;
import com.mysms.students.sis.api.model.SubjectRegistrationLine;
import com.mysms.students.sis.api.model.SubjectRegistrationLineUpdate;
import com.mysms.students.sis.api.model.SubjectRegistrationUpdate;
import com.mysms.students.sis.api.model.SubmitSubjectRegistrationRequest;
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
public interface SubjectRegistrationApi {

    String PATH_APPROVE_SUBJECT_REGISTRATION = "/v1/sis/subject-registrations/{subjectRegistrationId}:approve";
    /**
     * POST /v1/sis/subject-registrations/{subjectRegistrationId}:approve : Approve; subjects are added to the registration
     * Approve; subjects are added to the registration.
     *
     * @param subjectRegistrationId  (required)
     * @param approveSubjectRegistrationRequest  (required)
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
        value = SubjectRegistrationApi.PATH_APPROVE_SUBJECT_REGISTRATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistration> approveSubjectRegistration(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @Valid @RequestBody ApproveSubjectRegistrationRequest approveSubjectRegistrationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_SUBJECT_REGISTRATION = "/v1/sis/subject-registrations";
    /**
     * POST /v1/sis/subject-registrations : Create SubjectRegistration
     * Create a SubjectRegistration. Supports Idempotency-Key and validateOnly.
     *
     * @param subjectRegistration  (required)
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
        value = SubjectRegistrationApi.PATH_CREATE_SUBJECT_REGISTRATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistration> createSubjectRegistration(
         @Valid @RequestBody SubjectRegistration subjectRegistration,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_SUBJECT_REGISTRATION_LINE = "/v1/sis/subject-registrations/{subjectRegistrationId}/lines";
    /**
     * POST /v1/sis/subject-registrations/{subjectRegistrationId}/lines : Create SubjectRegistrationLine
     * Create a SubjectRegistrationLine. Supports Idempotency-Key and validateOnly.
     *
     * @param subjectRegistrationId SubjectRegistration id. (required)
     * @param subjectRegistrationLine  (required)
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
        value = SubjectRegistrationApi.PATH_CREATE_SUBJECT_REGISTRATION_LINE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistrationLine> createSubjectRegistrationLine(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @Valid @RequestBody SubjectRegistrationLine subjectRegistrationLine,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_SUBJECT_REGISTRATION = "/v1/sis/subject-registrations/{subjectRegistrationId}";
    /**
     * DELETE /v1/sis/subject-registrations/{subjectRegistrationId} : Delete SubjectRegistration
     * Soft delete (sets deleted_at).
     *
     * @param subjectRegistrationId SubjectRegistration id. (required)
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
        value = SubjectRegistrationApi.PATH_DELETE_SUBJECT_REGISTRATION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSubjectRegistration(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_SUBJECT_REGISTRATION_LINE = "/v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}";
    /**
     * DELETE /v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId} : Delete SubjectRegistrationLine
     * Soft delete (sets deleted_at).
     *
     * @param subjectRegistrationId SubjectRegistration id. (required)
     * @param subjectRegistrationLineId SubjectRegistrationLine id. (required)
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
        value = SubjectRegistrationApi.PATH_DELETE_SUBJECT_REGISTRATION_LINE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSubjectRegistrationLine(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @PathVariable("subjectRegistrationLineId") UUID subjectRegistrationLineId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_SUBJECT_REGISTRATION = "/v1/sis/subject-registrations/{subjectRegistrationId}";
    /**
     * GET /v1/sis/subject-registrations/{subjectRegistrationId} : Get SubjectRegistration
     * Get one SubjectRegistration by id. The ETag header carries the row version.
     *
     * @param subjectRegistrationId SubjectRegistration id. (required)
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
        value = SubjectRegistrationApi.PATH_GET_SUBJECT_REGISTRATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SubjectRegistration> getSubjectRegistration(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_SUBJECT_REGISTRATION_LINE = "/v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}";
    /**
     * GET /v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId} : Get SubjectRegistrationLine
     * Get one SubjectRegistrationLine by id. The ETag header carries the row version.
     *
     * @param subjectRegistrationId SubjectRegistration id. (required)
     * @param subjectRegistrationLineId SubjectRegistrationLine id. (required)
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
        value = SubjectRegistrationApi.PATH_GET_SUBJECT_REGISTRATION_LINE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SubjectRegistrationLine> getSubjectRegistrationLine(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @PathVariable("subjectRegistrationLineId") UUID subjectRegistrationLineId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SUBJECT_REGISTRATION_LINES = "/v1/sis/subject-registrations/{subjectRegistrationId}/lines";
    /**
     * GET /v1/sis/subject-registrations/{subjectRegistrationId}/lines : List SubjectRegistrationLines
     * List SubjectRegistrationLines visible to the caller, page by page, with filtering and sorting.
     *
     * @param subjectRegistrationId SubjectRegistration id. (required)
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
        value = SubjectRegistrationApi.PATH_LIST_SUBJECT_REGISTRATION_LINES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SubjectRegistrationLine>> listSubjectRegistrationLines(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SUBJECT_REGISTRATIONS = "/v1/sis/subject-registrations";
    /**
     * GET /v1/sis/subject-registrations : List SubjectRegistrations
     * List SubjectRegistrations visible to the caller, page by page, with filtering and sorting.
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
        value = SubjectRegistrationApi.PATH_LIST_SUBJECT_REGISTRATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SubjectRegistration>> listSubjectRegistrations(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LOAD_COURSE_SUBJECTS = "/v1/sis/subject-registrations/{subjectRegistrationId}:loadCourseSubjects";
    /**
     * POST /v1/sis/subject-registrations/{subjectRegistrationId}:loadCourseSubjects : Pre-fill compulsory/elective lines from the course subjects
     * Pre-fill compulsory/elective lines from the course subjects.
     *
     * @param subjectRegistrationId  (required)
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
        value = SubjectRegistrationApi.PATH_LOAD_COURSE_SUBJECTS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistration> loadCourseSubjects(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REJECT_SUBJECT_REGISTRATION = "/v1/sis/subject-registrations/{subjectRegistrationId}:reject";
    /**
     * POST /v1/sis/subject-registrations/{subjectRegistrationId}:reject : Reject
     * Reject.
     *
     * @param subjectRegistrationId  (required)
     * @param rejectSubjectRegistrationRequest  (required)
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
        value = SubjectRegistrationApi.PATH_REJECT_SUBJECT_REGISTRATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistration> rejectSubjectRegistration(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @Valid @RequestBody RejectSubjectRegistrationRequest rejectSubjectRegistrationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_SUBJECT_REGISTRATION = "/v1/sis/subject-registrations/{subjectRegistrationId}:resetToDraft";
    /**
     * POST /v1/sis/subject-registrations/{subjectRegistrationId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param subjectRegistrationId  (required)
     * @param resetSubjectRegistrationRequest  (required)
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
        value = SubjectRegistrationApi.PATH_RESET_SUBJECT_REGISTRATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistration> resetSubjectRegistration(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @Valid @RequestBody ResetSubjectRegistrationRequest resetSubjectRegistrationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUBMIT_SUBJECT_REGISTRATION = "/v1/sis/subject-registrations/{subjectRegistrationId}:submit";
    /**
     * POST /v1/sis/subject-registrations/{subjectRegistrationId}:submit : Submit for approval
     * Submit for approval.
     *
     * @param subjectRegistrationId  (required)
     * @param submitSubjectRegistrationRequest  (required)
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
        value = SubjectRegistrationApi.PATH_SUBMIT_SUBJECT_REGISTRATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistration> submitSubjectRegistration(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @Valid @RequestBody SubmitSubjectRegistrationRequest submitSubjectRegistrationRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_SUBJECT_REGISTRATION = "/v1/sis/subject-registrations/{subjectRegistrationId}";
    /**
     * PATCH /v1/sis/subject-registrations/{subjectRegistrationId} : Update SubjectRegistration
     * Partially update a SubjectRegistration (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param subjectRegistrationId SubjectRegistration id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param subjectRegistrationUpdate  (required)
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
        value = SubjectRegistrationApi.PATH_UPDATE_SUBJECT_REGISTRATION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistration> updateSubjectRegistration(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SubjectRegistrationUpdate subjectRegistrationUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_SUBJECT_REGISTRATION_LINE = "/v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId}";
    /**
     * PATCH /v1/sis/subject-registrations/{subjectRegistrationId}/lines/{subjectRegistrationLineId} : Update SubjectRegistrationLine
     * Partially update a SubjectRegistrationLine (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param subjectRegistrationId SubjectRegistration id. (required)
     * @param subjectRegistrationLineId SubjectRegistrationLine id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param subjectRegistrationLineUpdate  (required)
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
        value = SubjectRegistrationApi.PATH_UPDATE_SUBJECT_REGISTRATION_LINE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SubjectRegistrationLine> updateSubjectRegistrationLine(
         @PathVariable("subjectRegistrationId") UUID subjectRegistrationId,
         @PathVariable("subjectRegistrationLineId") UUID subjectRegistrationLineId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SubjectRegistrationLineUpdate subjectRegistrationLineUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
