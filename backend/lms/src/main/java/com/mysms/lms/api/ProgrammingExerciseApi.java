// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ExerciseSubmission;
import com.mysms.lms.api.model.ExerciseTestCase;
import com.mysms.lms.api.model.ExerciseTestCaseUpdate;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.ProgrammingExercise;
import com.mysms.lms.api.model.ProgrammingExerciseUpdate;
import com.mysms.lms.api.model.SubmitExerciseRequest;
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
public interface ProgrammingExerciseApi {

    String PATH_CREATE_EXERCISE_TEST_CASE = "/v1/lms/programming-exercises/{programmingExerciseId}/test-cases";
    /**
     * POST /v1/lms/programming-exercises/{programmingExerciseId}/test-cases : Create ExerciseTestCase
     * Create a ExerciseTestCase. Supports Idempotency-Key and validateOnly.
     *
     * @param programmingExerciseId ProgrammingExercise id. (required)
     * @param exerciseTestCase  (required)
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
        value = ProgrammingExerciseApi.PATH_CREATE_EXERCISE_TEST_CASE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExerciseTestCase> createExerciseTestCase(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
         @Valid @RequestBody ExerciseTestCase exerciseTestCase,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_PROGRAMMING_EXERCISE = "/v1/lms/programming-exercises";
    /**
     * POST /v1/lms/programming-exercises : Create ProgrammingExercise
     * Create a ProgrammingExercise. Supports Idempotency-Key and validateOnly.
     *
     * @param programmingExercise  (required)
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
        value = ProgrammingExerciseApi.PATH_CREATE_PROGRAMMING_EXERCISE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ProgrammingExercise> createProgrammingExercise(
         @Valid @RequestBody ProgrammingExercise programmingExercise,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_EXERCISE_TEST_CASE = "/v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}";
    /**
     * DELETE /v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId} : Delete ExerciseTestCase
     * Soft delete (sets deleted_at).
     *
     * @param programmingExerciseId ProgrammingExercise id. (required)
     * @param exerciseTestCaseId ExerciseTestCase id. (required)
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
        value = ProgrammingExerciseApi.PATH_DELETE_EXERCISE_TEST_CASE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExerciseTestCase(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
         @PathVariable("exerciseTestCaseId") UUID exerciseTestCaseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_PROGRAMMING_EXERCISE = "/v1/lms/programming-exercises/{programmingExerciseId}";
    /**
     * DELETE /v1/lms/programming-exercises/{programmingExerciseId} : Delete ProgrammingExercise
     * Soft delete (sets deleted_at).
     *
     * @param programmingExerciseId ProgrammingExercise id. (required)
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
        value = ProgrammingExerciseApi.PATH_DELETE_PROGRAMMING_EXERCISE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteProgrammingExercise(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_EXERCISE_TEST_CASE = "/v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}";
    /**
     * GET /v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId} : Get ExerciseTestCase
     * Get one ExerciseTestCase by id. The ETag header carries the row version.
     *
     * @param programmingExerciseId ProgrammingExercise id. (required)
     * @param exerciseTestCaseId ExerciseTestCase id. (required)
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
        value = ProgrammingExerciseApi.PATH_GET_EXERCISE_TEST_CASE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExerciseTestCase> getExerciseTestCase(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
         @PathVariable("exerciseTestCaseId") UUID exerciseTestCaseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_PROGRAMMING_EXERCISE = "/v1/lms/programming-exercises/{programmingExerciseId}";
    /**
     * GET /v1/lms/programming-exercises/{programmingExerciseId} : Get ProgrammingExercise
     * Get one ProgrammingExercise by id. The ETag header carries the row version.
     *
     * @param programmingExerciseId ProgrammingExercise id. (required)
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
        value = ProgrammingExerciseApi.PATH_GET_PROGRAMMING_EXERCISE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ProgrammingExercise> getProgrammingExercise(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXERCISE_TEST_CASES = "/v1/lms/programming-exercises/{programmingExerciseId}/test-cases";
    /**
     * GET /v1/lms/programming-exercises/{programmingExerciseId}/test-cases : List ExerciseTestCases
     * List ExerciseTestCases visible to the caller, page by page, with filtering and sorting.
     *
     * @param programmingExerciseId ProgrammingExercise id. (required)
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
        value = ProgrammingExerciseApi.PATH_LIST_EXERCISE_TEST_CASES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExerciseTestCase>> listExerciseTestCases(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_PROGRAMMING_EXERCISES = "/v1/lms/programming-exercises";
    /**
     * GET /v1/lms/programming-exercises : List ProgrammingExercises
     * List ProgrammingExercises visible to the caller, page by page, with filtering and sorting.
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
        value = ProgrammingExerciseApi.PATH_LIST_PROGRAMMING_EXERCISES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ProgrammingExercise>> listProgrammingExercises(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_SUBMIT_EXERCISE = "/v1/lms/programming-exercises/{programmingExerciseId}:submit";
    /**
     * POST /v1/lms/programming-exercises/{programmingExerciseId}:submit : Store a submission and evaluate it against the test cases
     * Store a submission and evaluate it against the test cases.
     *
     * @param programmingExerciseId  (required)
     * @param submitExerciseRequest  (required)
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
        value = ProgrammingExerciseApi.PATH_SUBMIT_EXERCISE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExerciseSubmission> submitExercise(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
         @Valid @RequestBody SubmitExerciseRequest submitExerciseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_EXERCISE_TEST_CASE = "/v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId}";
    /**
     * PATCH /v1/lms/programming-exercises/{programmingExerciseId}/test-cases/{exerciseTestCaseId} : Update ExerciseTestCase
     * Partially update a ExerciseTestCase (updateMask). Requires If-Match with the current ETag.
     *
     * @param programmingExerciseId ProgrammingExercise id. (required)
     * @param exerciseTestCaseId ExerciseTestCase id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param exerciseTestCaseUpdate  (required)
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
        value = ProgrammingExerciseApi.PATH_UPDATE_EXERCISE_TEST_CASE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExerciseTestCase> updateExerciseTestCase(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
         @PathVariable("exerciseTestCaseId") UUID exerciseTestCaseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExerciseTestCaseUpdate exerciseTestCaseUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_PROGRAMMING_EXERCISE = "/v1/lms/programming-exercises/{programmingExerciseId}";
    /**
     * PATCH /v1/lms/programming-exercises/{programmingExerciseId} : Update ProgrammingExercise
     * Partially update a ProgrammingExercise (updateMask). Requires If-Match with the current ETag.
     *
     * @param programmingExerciseId ProgrammingExercise id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param programmingExerciseUpdate  (required)
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
        value = ProgrammingExerciseApi.PATH_UPDATE_PROGRAMMING_EXERCISE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ProgrammingExercise> updateProgrammingExercise(
         @PathVariable("programmingExerciseId") UUID programmingExerciseId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ProgrammingExerciseUpdate programmingExerciseUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
