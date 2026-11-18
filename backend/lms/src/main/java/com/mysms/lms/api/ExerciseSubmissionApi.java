// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ExerciseSubmission;
import com.mysms.lms.api.model.ExerciseTestResult;
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
public interface ExerciseSubmissionApi {

    String PATH_GET_EXERCISE_SUBMISSION = "/v1/lms/exercise-submissions/{exerciseSubmissionId}";
    /**
     * GET /v1/lms/exercise-submissions/{exerciseSubmissionId} : Get ExerciseSubmission
     * Get one ExerciseSubmission by id. The ETag header carries the row version.
     *
     * @param exerciseSubmissionId ExerciseSubmission id. (required)
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
        value = ExerciseSubmissionApi.PATH_GET_EXERCISE_SUBMISSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExerciseSubmission> getExerciseSubmission(
         @PathVariable("exerciseSubmissionId") UUID exerciseSubmissionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_EXERCISE_TEST_RESULT = "/v1/lms/exercise-submissions/{exerciseSubmissionId}/test-results/{exerciseTestResultId}";
    /**
     * GET /v1/lms/exercise-submissions/{exerciseSubmissionId}/test-results/{exerciseTestResultId} : Get ExerciseTestResult
     * Get one ExerciseTestResult by id. The ETag header carries the row version.
     *
     * @param exerciseSubmissionId ExerciseSubmission id. (required)
     * @param exerciseTestResultId ExerciseTestResult id. (required)
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
        value = ExerciseSubmissionApi.PATH_GET_EXERCISE_TEST_RESULT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExerciseTestResult> getExerciseTestResult(
         @PathVariable("exerciseSubmissionId") UUID exerciseSubmissionId,
         @PathVariable("exerciseTestResultId") UUID exerciseTestResultId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXERCISE_SUBMISSIONS = "/v1/lms/exercise-submissions";
    /**
     * GET /v1/lms/exercise-submissions : List ExerciseSubmissions
     * List ExerciseSubmissions visible to the caller, page by page, with filtering and sorting.
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
        value = ExerciseSubmissionApi.PATH_LIST_EXERCISE_SUBMISSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExerciseSubmission>> listExerciseSubmissions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXERCISE_TEST_RESULTS = "/v1/lms/exercise-submissions/{exerciseSubmissionId}/test-results";
    /**
     * GET /v1/lms/exercise-submissions/{exerciseSubmissionId}/test-results : List ExerciseTestResults
     * List ExerciseTestResults visible to the caller, page by page, with filtering and sorting.
     *
     * @param exerciseSubmissionId ExerciseSubmission id. (required)
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
        value = ExerciseSubmissionApi.PATH_LIST_EXERCISE_TEST_RESULTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExerciseTestResult>> listExerciseTestResults(
         @PathVariable("exerciseSubmissionId") UUID exerciseSubmissionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );

}
