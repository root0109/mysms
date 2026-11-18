// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.AnswerCheck;
import com.mysms.lms.api.model.CheckQuizAnswerRequest;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.Quiz;
import com.mysms.lms.api.model.QuizAttempt;
import com.mysms.lms.api.model.QuizQuestion;
import com.mysms.lms.api.model.QuizQuestionHistory;
import com.mysms.lms.api.model.QuizQuestionList;
import com.mysms.lms.api.model.QuizQuestionUpdate;
import com.mysms.lms.api.model.QuizSubmission;
import com.mysms.lms.api.model.QuizUpdate;
import com.mysms.lms.api.model.SetQuizQuestionsRequest;
import com.mysms.lms.api.model.SubmitQuizRequest;
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
public interface QuizApi {

    String PATH_CHECK_QUIZ_ANSWER = "/v1/lms/quizzes/{quizId}:checkAnswer";
    /**
     * POST /v1/lms/quizzes/{quizId}:checkAnswer : Instant feedback for practice quizzes
     * Instant feedback for practice quizzes.
     *
     * @param quizId  (required)
     * @param checkQuizAnswerRequest  (required)
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
        value = QuizApi.PATH_CHECK_QUIZ_ANSWER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AnswerCheck> checkQuizAnswer(
         @PathVariable("quizId") UUID quizId,
         @Valid @RequestBody CheckQuizAnswerRequest checkQuizAnswerRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_QUIZ = "/v1/lms/quizzes";
    /**
     * POST /v1/lms/quizzes : Create Quiz
     * Create a Quiz. Supports Idempotency-Key and validateOnly.
     *
     * @param quiz  (required)
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
        value = QuizApi.PATH_CREATE_QUIZ,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Quiz> createQuiz(
         @Valid @RequestBody Quiz quiz,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_QUIZ_QUESTION = "/v1/lms/quizzes/{quizId}/questions";
    /**
     * POST /v1/lms/quizzes/{quizId}/questions : Create QuizQuestion
     * Create a QuizQuestion. Supports Idempotency-Key and validateOnly.
     *
     * @param quizId Quiz id. (required)
     * @param quizQuestion  (required)
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
        value = QuizApi.PATH_CREATE_QUIZ_QUESTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizQuestion> createQuizQuestion(
         @PathVariable("quizId") UUID quizId,
         @Valid @RequestBody QuizQuestion quizQuestion,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_QUIZ = "/v1/lms/quizzes/{quizId}";
    /**
     * DELETE /v1/lms/quizzes/{quizId} : Delete Quiz
     * Soft delete (sets deleted_at).
     *
     * @param quizId Quiz id. (required)
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
        value = QuizApi.PATH_DELETE_QUIZ,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuiz(
         @PathVariable("quizId") UUID quizId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_QUIZ_QUESTION = "/v1/lms/quizzes/{quizId}/questions/{quizQuestionId}";
    /**
     * DELETE /v1/lms/quizzes/{quizId}/questions/{quizQuestionId} : Delete QuizQuestion
     * Soft delete (sets deleted_at).
     *
     * @param quizId Quiz id. (required)
     * @param quizQuestionId QuizQuestion id. (required)
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
        value = QuizApi.PATH_DELETE_QUIZ_QUESTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuizQuestion(
         @PathVariable("quizId") UUID quizId,
         @PathVariable("quizQuestionId") UUID quizQuestionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_QUIZ = "/v1/lms/quizzes/{quizId}";
    /**
     * GET /v1/lms/quizzes/{quizId} : Get Quiz
     * Get one Quiz by id. The ETag header carries the row version.
     *
     * @param quizId Quiz id. (required)
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
        value = QuizApi.PATH_GET_QUIZ,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Quiz> getQuiz(
         @PathVariable("quizId") UUID quizId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUIZ_QUESTION = "/v1/lms/quizzes/{quizId}/questions/{quizQuestionId}";
    /**
     * GET /v1/lms/quizzes/{quizId}/questions/{quizQuestionId} : Get QuizQuestion
     * Get one QuizQuestion by id. The ETag header carries the row version.
     *
     * @param quizId Quiz id. (required)
     * @param quizQuestionId QuizQuestion id. (required)
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
        value = QuizApi.PATH_GET_QUIZ_QUESTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuizQuestion> getQuizQuestion(
         @PathVariable("quizId") UUID quizId,
         @PathVariable("quizQuestionId") UUID quizQuestionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUIZ_QUESTION_HISTORY = "/v1/lms/quizzes/{quizId}/question-history/{quizQuestionHistoryId}";
    /**
     * GET /v1/lms/quizzes/{quizId}/question-history/{quizQuestionHistoryId} : Get QuizQuestionHistory
     * Get one QuizQuestionHistory by id. The ETag header carries the row version.
     *
     * @param quizId Quiz id. (required)
     * @param quizQuestionHistoryId QuizQuestionHistory id. (required)
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
        value = QuizApi.PATH_GET_QUIZ_QUESTION_HISTORY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuizQuestionHistory> getQuizQuestionHistory(
         @PathVariable("quizId") UUID quizId,
         @PathVariable("quizQuestionHistoryId") UUID quizQuestionHistoryId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUIZ_QUESTION_HISTORY = "/v1/lms/quizzes/{quizId}/question-history";
    /**
     * GET /v1/lms/quizzes/{quizId}/question-history : List QuizQuestionHistory
     * List QuizQuestionHistory visible to the caller, page by page, with filtering and sorting.
     *
     * @param quizId Quiz id. (required)
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
        value = QuizApi.PATH_LIST_QUIZ_QUESTION_HISTORY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuizQuestionHistory>> listQuizQuestionHistory(
         @PathVariable("quizId") UUID quizId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUIZ_QUESTIONS = "/v1/lms/quizzes/{quizId}/questions";
    /**
     * GET /v1/lms/quizzes/{quizId}/questions : List QuizQuestions
     * List QuizQuestions visible to the caller, page by page, with filtering and sorting.
     *
     * @param quizId Quiz id. (required)
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
        value = QuizApi.PATH_LIST_QUIZ_QUESTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuizQuestion>> listQuizQuestions(
         @PathVariable("quizId") UUID quizId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUIZZES = "/v1/lms/quizzes";
    /**
     * GET /v1/lms/quizzes : List Quizzes
     * List Quizzes visible to the caller, page by page, with filtering and sorting.
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
        value = QuizApi.PATH_LIST_QUIZZES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Quiz>> listQuizzes(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_SET_QUIZ_QUESTIONS = "/v1/lms/quizzes/{quizId}:setQuestions";
    /**
     * POST /v1/lms/quizzes/{quizId}:setQuestions : Add / remove quiz questions (earlier marks are kept in the question history when attempts exist)
     * Add / remove quiz questions (earlier marks are kept in the question history when attempts exist).
     *
     * @param quizId  (required)
     * @param setQuizQuestionsRequest  (required)
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
        value = QuizApi.PATH_SET_QUIZ_QUESTIONS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizQuestionList> setQuizQuestions(
         @PathVariable("quizId") UUID quizId,
         @Valid @RequestBody SetQuizQuestionsRequest setQuizQuestionsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_START_QUIZ_ATTEMPT = "/v1/lms/content-items/{contentItemId}:startQuizAttempt";
    /**
     * POST /v1/lms/content-items/{contentItemId}:startQuizAttempt : Start a quiz attempt on its placement
     * Starts (or resumes) an attempt on the quiz placed by this content item, so release rules, overrides and the attempt limit of the placement bind the attempt. Delivers questions (answers hidden, shuffled per settings) and creates the submission row the answers autosave into.
     *
     * @param contentItemId  (required)
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
        value = QuizApi.PATH_START_QUIZ_ATTEMPT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizAttempt> startQuizAttempt(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUBMIT_QUIZ = "/v1/lms/quiz-submissions/{quizSubmissionId}:submit";
    /**
     * POST /v1/lms/quiz-submissions/{quizSubmissionId}:submit : Grade and store the attempt
     * Grades and finalises this submission (created by StartQuizAttempt). Idempotent per attempt; late or duplicate submits are rejected with 409.
     *
     * @param quizSubmissionId  (required)
     * @param submitQuizRequest  (required)
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
        value = QuizApi.PATH_SUBMIT_QUIZ,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizSubmission> submitQuiz(
         @PathVariable("quizSubmissionId") UUID quizSubmissionId,
         @Valid @RequestBody SubmitQuizRequest submitQuizRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_QUIZ = "/v1/lms/quizzes/{quizId}";
    /**
     * PATCH /v1/lms/quizzes/{quizId} : Update Quiz
     * Partially update a Quiz (updateMask). Requires If-Match with the current ETag.
     *
     * @param quizId Quiz id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param quizUpdate  (required)
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
        value = QuizApi.PATH_UPDATE_QUIZ,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Quiz> updateQuiz(
         @PathVariable("quizId") UUID quizId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuizUpdate quizUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_QUIZ_QUESTION = "/v1/lms/quizzes/{quizId}/questions/{quizQuestionId}";
    /**
     * PATCH /v1/lms/quizzes/{quizId}/questions/{quizQuestionId} : Update QuizQuestion
     * Partially update a QuizQuestion (updateMask). Requires If-Match with the current ETag.
     *
     * @param quizId Quiz id. (required)
     * @param quizQuestionId QuizQuestion id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param quizQuestionUpdate  (required)
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
        value = QuizApi.PATH_UPDATE_QUIZ_QUESTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuizQuestion> updateQuizQuestion(
         @PathVariable("quizId") UUID quizId,
         @PathVariable("quizQuestionId") UUID quizQuestionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuizQuestionUpdate quizQuestionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
