// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.GradeQuizAnswersRequest;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.PauseQuizAttemptRequest;
import com.mysms.lms.api.model.QuizAnswer;
import com.mysms.lms.api.model.QuizAutosaveResult;
import com.mysms.lms.api.model.QuizSubmission;
import com.mysms.lms.api.model.QuizViolation;
import com.mysms.lms.api.model.ResumeQuizAttemptRequest;
import com.mysms.lms.api.model.SaveQuizResponsesRequest;
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
public interface QuizSubmissionApi {

    String PATH_GET_QUIZ_ANSWER = "/v1/lms/quiz-submissions/{quizSubmissionId}/answers/{quizAnswerId}";
    /**
     * GET /v1/lms/quiz-submissions/{quizSubmissionId}/answers/{quizAnswerId} : Get QuizAnswer
     * Get one QuizAnswer by id. The ETag header carries the row version.
     *
     * @param quizSubmissionId QuizSubmission id. (required)
     * @param quizAnswerId QuizAnswer id. (required)
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
        value = QuizSubmissionApi.PATH_GET_QUIZ_ANSWER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuizAnswer> getQuizAnswer(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
         @PathVariable("quizAnswerId") UUID quizAnswerId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUIZ_SUBMISSION = "/v1/lms/quiz-submissions/{quizSubmissionId}";
    /**
     * GET /v1/lms/quiz-submissions/{quizSubmissionId} : Get QuizSubmission
     * Get one QuizSubmission by id. The ETag header carries the row version.
     *
     * @param quizSubmissionId QuizSubmission id. (required)
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
        value = QuizSubmissionApi.PATH_GET_QUIZ_SUBMISSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuizSubmission> getQuizSubmission(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUIZ_VIOLATION = "/v1/lms/quiz-submissions/{quizSubmissionId}/violations/{quizViolationId}";
    /**
     * GET /v1/lms/quiz-submissions/{quizSubmissionId}/violations/{quizViolationId} : Get QuizViolation
     * Get one QuizViolation by id. The ETag header carries the row version.
     *
     * @param quizSubmissionId QuizSubmission id. (required)
     * @param quizViolationId QuizViolation id. (required)
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
        value = QuizSubmissionApi.PATH_GET_QUIZ_VIOLATION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuizViolation> getQuizViolation(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
         @PathVariable("quizViolationId") UUID quizViolationId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GRADE_QUIZ_ANSWERS = "/v1/lms/quiz-submissions/{quizSubmissionId}:gradeAnswers";
    /**
     * POST /v1/lms/quiz-submissions/{quizSubmissionId}:gradeAnswers : Manually mark answers that need grading (open-ended, pattern...)
     * Manually mark answers that need grading (open-ended, pattern...).
     *
     * @param quizSubmissionId  (required)
     * @param gradeQuizAnswersRequest  (required)
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
        value = QuizSubmissionApi.PATH_GRADE_QUIZ_ANSWERS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizSubmission> gradeQuizAnswers(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
         @Valid @RequestBody GradeQuizAnswersRequest gradeQuizAnswersRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_QUIZ_ANSWERS = "/v1/lms/quiz-submissions/{quizSubmissionId}/answers";
    /**
     * GET /v1/lms/quiz-submissions/{quizSubmissionId}/answers : List QuizAnswers
     * List QuizAnswers visible to the caller, page by page, with filtering and sorting.
     *
     * @param quizSubmissionId QuizSubmission id. (required)
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
        value = QuizSubmissionApi.PATH_LIST_QUIZ_ANSWERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuizAnswer>> listQuizAnswers(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUIZ_SUBMISSIONS = "/v1/lms/quiz-submissions";
    /**
     * GET /v1/lms/quiz-submissions : List QuizSubmissions
     * List QuizSubmissions visible to the caller, page by page, with filtering and sorting.
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
        value = QuizSubmissionApi.PATH_LIST_QUIZ_SUBMISSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuizSubmission>> listQuizSubmissions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUIZ_VIOLATIONS = "/v1/lms/quiz-submissions/{quizSubmissionId}/violations";
    /**
     * GET /v1/lms/quiz-submissions/{quizSubmissionId}/violations : List QuizViolations
     * List QuizViolations visible to the caller, page by page, with filtering and sorting.
     *
     * @param quizSubmissionId QuizSubmission id. (required)
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
        value = QuizSubmissionApi.PATH_LIST_QUIZ_VIOLATIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuizViolation>> listQuizViolations(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_PAUSE_QUIZ_ATTEMPT = "/v1/lms/quiz-submissions/{quizSubmissionId}:pause";
    /**
     * POST /v1/lms/quiz-submissions/{quizSubmissionId}:pause : Pause an attempt (quizzes that allow breaks)
     * Pause an attempt (quizzes that allow breaks).
     *
     * @param quizSubmissionId  (required)
     * @param pauseQuizAttemptRequest  (required)
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
        value = QuizSubmissionApi.PATH_PAUSE_QUIZ_ATTEMPT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizSubmission> pauseQuizAttempt(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
         @Valid @RequestBody PauseQuizAttemptRequest pauseQuizAttemptRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESUME_QUIZ_ATTEMPT = "/v1/lms/quiz-submissions/{quizSubmissionId}:resume";
    /**
     * POST /v1/lms/quiz-submissions/{quizSubmissionId}:resume : Resume a paused attempt
     * Resume a paused attempt.
     *
     * @param quizSubmissionId  (required)
     * @param resumeQuizAttemptRequest  (required)
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
        value = QuizSubmissionApi.PATH_RESUME_QUIZ_ATTEMPT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizSubmission> resumeQuizAttempt(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
         @Valid @RequestBody ResumeQuizAttemptRequest resumeQuizAttemptRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SAVE_QUIZ_RESPONSES = "/v1/lms/quiz-submissions/{quizSubmissionId}:saveResponses";
    /**
     * POST /v1/lms/quiz-submissions/{quizSubmissionId}:saveResponses : Autosave answers of an attempt in progress
     * Autosave answers of an attempt in progress. Offline-safe: replays are ordered by clientSequence and per-answer answeredAt, newer stored answers are never overwritten. Returns no score; scores appear on the submission only after SubmitQuiz and only when the quiz releases results.
     *
     * @param quizSubmissionId  (required)
     * @param saveQuizResponsesRequest  (required)
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
        value = QuizSubmissionApi.PATH_SAVE_QUIZ_RESPONSES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizAutosaveResult> saveQuizResponses(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
         @Valid @RequestBody SaveQuizResponsesRequest saveQuizResponsesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );

}
