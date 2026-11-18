// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.LearningProgram;
import com.mysms.lms.api.model.LearningProgramCourse;
import com.mysms.lms.api.model.LearningProgramCourseUpdate;
import com.mysms.lms.api.model.LearningProgramMember;
import com.mysms.lms.api.model.LearningProgramMemberUpdate;
import com.mysms.lms.api.model.LearningProgramUpdate;
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
public interface LearningProgramApi {

    String PATH_CREATE_LEARNING_PROGRAM = "/v1/lms/learning-programs";
    /**
     * POST /v1/lms/learning-programs : Create LearningProgram
     * Create a LearningProgram. Supports Idempotency-Key and validateOnly.
     *
     * @param learningProgram  (required)
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
        value = LearningProgramApi.PATH_CREATE_LEARNING_PROGRAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LearningProgram> createLearningProgram(
         @Valid @RequestBody LearningProgram learningProgram,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_LEARNING_PROGRAM_COURSE = "/v1/lms/learning-programs/{learningProgramId}/courses";
    /**
     * POST /v1/lms/learning-programs/{learningProgramId}/courses : Create LearningProgramCourse
     * Create a LearningProgramCourse. Supports Idempotency-Key and validateOnly.
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param learningProgramCourse  (required)
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
        value = LearningProgramApi.PATH_CREATE_LEARNING_PROGRAM_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LearningProgramCourse> createLearningProgramCourse(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @Valid @RequestBody LearningProgramCourse learningProgramCourse,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_LEARNING_PROGRAM_MEMBER = "/v1/lms/learning-programs/{learningProgramId}/members";
    /**
     * POST /v1/lms/learning-programs/{learningProgramId}/members : Create LearningProgramMember
     * Create a LearningProgramMember. Supports Idempotency-Key and validateOnly.
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param learningProgramMember  (required)
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
        value = LearningProgramApi.PATH_CREATE_LEARNING_PROGRAM_MEMBER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LearningProgramMember> createLearningProgramMember(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @Valid @RequestBody LearningProgramMember learningProgramMember,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_LEARNING_PROGRAM = "/v1/lms/learning-programs/{learningProgramId}";
    /**
     * DELETE /v1/lms/learning-programs/{learningProgramId} : Delete LearningProgram
     * Soft delete (sets deleted_at).
     *
     * @param learningProgramId LearningProgram id. (required)
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
        value = LearningProgramApi.PATH_DELETE_LEARNING_PROGRAM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLearningProgram(
         @PathVariable("learningProgramId") UUID learningProgramId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_LEARNING_PROGRAM_COURSE = "/v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}";
    /**
     * DELETE /v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId} : Delete LearningProgramCourse
     * Soft delete (sets deleted_at).
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param learningProgramCourseId LearningProgramCourse id. (required)
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
        value = LearningProgramApi.PATH_DELETE_LEARNING_PROGRAM_COURSE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLearningProgramCourse(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @PathVariable("learningProgramCourseId") UUID learningProgramCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_LEARNING_PROGRAM_MEMBER = "/v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}";
    /**
     * DELETE /v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId} : Delete LearningProgramMember
     * Soft delete (sets deleted_at).
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param learningProgramMemberId LearningProgramMember id. (required)
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
        value = LearningProgramApi.PATH_DELETE_LEARNING_PROGRAM_MEMBER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteLearningProgramMember(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @PathVariable("learningProgramMemberId") UUID learningProgramMemberId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_ENROLL_IN_LEARNING_PROGRAM = "/v1/lms/learning-programs/{learningProgramId}:enroll";
    /**
     * POST /v1/lms/learning-programs/{learningProgramId}:enroll : Join a program
     * Join a program.
     *
     * @param learningProgramId  (required)
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
        value = LearningProgramApi.PATH_ENROLL_IN_LEARNING_PROGRAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LearningProgramMember> enrollInLearningProgram(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_LEARNING_PROGRAM = "/v1/lms/learning-programs/{learningProgramId}";
    /**
     * GET /v1/lms/learning-programs/{learningProgramId} : Get LearningProgram
     * Get one LearningProgram by id. The ETag header carries the row version. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
     *
     * @param learningProgramId LearningProgram id. (required)
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
        value = LearningProgramApi.PATH_GET_LEARNING_PROGRAM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LearningProgram> getLearningProgram(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_LEARNING_PROGRAM_COURSE = "/v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}";
    /**
     * GET /v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId} : Get LearningProgramCourse
     * Get one LearningProgramCourse by id. The ETag header carries the row version.
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param learningProgramCourseId LearningProgramCourse id. (required)
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
        value = LearningProgramApi.PATH_GET_LEARNING_PROGRAM_COURSE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LearningProgramCourse> getLearningProgramCourse(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @PathVariable("learningProgramCourseId") UUID learningProgramCourseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_LEARNING_PROGRAM_MEMBER = "/v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}";
    /**
     * GET /v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId} : Get LearningProgramMember
     * Get one LearningProgramMember by id. The ETag header carries the row version.
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param learningProgramMemberId LearningProgramMember id. (required)
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
        value = LearningProgramApi.PATH_GET_LEARNING_PROGRAM_MEMBER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<LearningProgramMember> getLearningProgramMember(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @PathVariable("learningProgramMemberId") UUID learningProgramMemberId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LEARNING_PROGRAM_COURSES = "/v1/lms/learning-programs/{learningProgramId}/courses";
    /**
     * GET /v1/lms/learning-programs/{learningProgramId}/courses : List LearningProgramCourses
     * List LearningProgramCourses visible to the caller, page by page, with filtering and sorting.
     *
     * @param learningProgramId LearningProgram id. (required)
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
        value = LearningProgramApi.PATH_LIST_LEARNING_PROGRAM_COURSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LearningProgramCourse>> listLearningProgramCourses(
         @PathVariable("learningProgramId") UUID learningProgramId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LEARNING_PROGRAM_MEMBERS = "/v1/lms/learning-programs/{learningProgramId}/members";
    /**
     * GET /v1/lms/learning-programs/{learningProgramId}/members : List LearningProgramMembers
     * List LearningProgramMembers visible to the caller, page by page, with filtering and sorting.
     *
     * @param learningProgramId LearningProgram id. (required)
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
        value = LearningProgramApi.PATH_LIST_LEARNING_PROGRAM_MEMBERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LearningProgramMember>> listLearningProgramMembers(
         @PathVariable("learningProgramId") UUID learningProgramId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_LEARNING_PROGRAMS = "/v1/lms/learning-programs";
    /**
     * GET /v1/lms/learning-programs : List LearningPrograms
     * List LearningPrograms visible to the caller, page by page, with filtering and sorting. Guests (no token) receive published rows only, with internal fields omitted (guest projection).
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
        value = LearningProgramApi.PATH_LIST_LEARNING_PROGRAMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<LearningProgram>> listLearningPrograms(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_UPDATE_LEARNING_PROGRAM = "/v1/lms/learning-programs/{learningProgramId}";
    /**
     * PATCH /v1/lms/learning-programs/{learningProgramId} : Update LearningProgram
     * Partially update a LearningProgram (updateMask). Requires If-Match with the current ETag.
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param learningProgramUpdate  (required)
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
        value = LearningProgramApi.PATH_UPDATE_LEARNING_PROGRAM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LearningProgram> updateLearningProgram(
         @PathVariable("learningProgramId") UUID learningProgramId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LearningProgramUpdate learningProgramUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_LEARNING_PROGRAM_COURSE = "/v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId}";
    /**
     * PATCH /v1/lms/learning-programs/{learningProgramId}/courses/{learningProgramCourseId} : Update LearningProgramCourse
     * Partially update a LearningProgramCourse (updateMask). Requires If-Match with the current ETag.
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param learningProgramCourseId LearningProgramCourse id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param learningProgramCourseUpdate  (required)
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
        value = LearningProgramApi.PATH_UPDATE_LEARNING_PROGRAM_COURSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LearningProgramCourse> updateLearningProgramCourse(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @PathVariable("learningProgramCourseId") UUID learningProgramCourseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LearningProgramCourseUpdate learningProgramCourseUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_LEARNING_PROGRAM_MEMBER = "/v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId}";
    /**
     * PATCH /v1/lms/learning-programs/{learningProgramId}/members/{learningProgramMemberId} : Update LearningProgramMember
     * Partially update a LearningProgramMember (updateMask). Requires If-Match with the current ETag.
     *
     * @param learningProgramId LearningProgram id. (required)
     * @param learningProgramMemberId LearningProgramMember id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param learningProgramMemberUpdate  (required)
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
        value = LearningProgramApi.PATH_UPDATE_LEARNING_PROGRAM_MEMBER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<LearningProgramMember> updateLearningProgramMember(
         @PathVariable("learningProgramId") UUID learningProgramId,
         @PathVariable("learningProgramMemberId") UUID learningProgramMemberId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody LearningProgramMemberUpdate learningProgramMemberUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
