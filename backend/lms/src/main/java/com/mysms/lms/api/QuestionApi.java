// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.ImportQuestionsRequest;
import org.springframework.lang.Nullable;
import com.mysms.lms.api.model.Operation;
import com.mysms.lms.api.model.Question;
import com.mysms.lms.api.model.QuestionAcceptedAnswer;
import com.mysms.lms.api.model.QuestionAcceptedAnswerUpdate;
import com.mysms.lms.api.model.QuestionBlank;
import com.mysms.lms.api.model.QuestionBlankUpdate;
import com.mysms.lms.api.model.QuestionHotspot;
import com.mysms.lms.api.model.QuestionHotspotUpdate;
import com.mysms.lms.api.model.QuestionMatchPair;
import com.mysms.lms.api.model.QuestionMatchPairUpdate;
import com.mysms.lms.api.model.QuestionOption;
import com.mysms.lms.api.model.QuestionOptionUpdate;
import com.mysms.lms.api.model.QuestionPattern;
import com.mysms.lms.api.model.QuestionPatternUpdate;
import com.mysms.lms.api.model.QuestionUpdate;
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
public interface QuestionApi {

    String PATH_CREATE_QUESTION = "/v1/lms/questions";
    /**
     * POST /v1/lms/questions : Create Question
     * Create a Question. Supports Idempotency-Key and validateOnly.
     *
     * @param question  (required)
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
        value = QuestionApi.PATH_CREATE_QUESTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Question> createQuestion(
         @Valid @RequestBody Question question,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_QUESTION_ACCEPTED_ANSWER = "/v1/lms/questions/{questionId}/accepted-answers";
    /**
     * POST /v1/lms/questions/{questionId}/accepted-answers : Create QuestionAcceptedAnswer
     * Create a QuestionAcceptedAnswer. Supports Idempotency-Key and validateOnly.
     *
     * @param questionId Question id. (required)
     * @param questionAcceptedAnswer  (required)
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
        value = QuestionApi.PATH_CREATE_QUESTION_ACCEPTED_ANSWER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionAcceptedAnswer> createQuestionAcceptedAnswer(
         @PathVariable("questionId") UUID questionId,
         @Valid @RequestBody QuestionAcceptedAnswer questionAcceptedAnswer,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_QUESTION_BLANK = "/v1/lms/questions/{questionId}/blanks";
    /**
     * POST /v1/lms/questions/{questionId}/blanks : Create QuestionBlank
     * Create a QuestionBlank. Supports Idempotency-Key and validateOnly.
     *
     * @param questionId Question id. (required)
     * @param questionBlank  (required)
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
        value = QuestionApi.PATH_CREATE_QUESTION_BLANK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionBlank> createQuestionBlank(
         @PathVariable("questionId") UUID questionId,
         @Valid @RequestBody QuestionBlank questionBlank,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_QUESTION_HOTSPOT = "/v1/lms/questions/{questionId}/hotspots";
    /**
     * POST /v1/lms/questions/{questionId}/hotspots : Create QuestionHotspot
     * Create a QuestionHotspot. Supports Idempotency-Key and validateOnly.
     *
     * @param questionId Question id. (required)
     * @param questionHotspot  (required)
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
        value = QuestionApi.PATH_CREATE_QUESTION_HOTSPOT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionHotspot> createQuestionHotspot(
         @PathVariable("questionId") UUID questionId,
         @Valid @RequestBody QuestionHotspot questionHotspot,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_QUESTION_MATCH_PAIR = "/v1/lms/questions/{questionId}/match-pairs";
    /**
     * POST /v1/lms/questions/{questionId}/match-pairs : Create QuestionMatchPair
     * Create a QuestionMatchPair. Supports Idempotency-Key and validateOnly.
     *
     * @param questionId Question id. (required)
     * @param questionMatchPair  (required)
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
        value = QuestionApi.PATH_CREATE_QUESTION_MATCH_PAIR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionMatchPair> createQuestionMatchPair(
         @PathVariable("questionId") UUID questionId,
         @Valid @RequestBody QuestionMatchPair questionMatchPair,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_QUESTION_OPTION = "/v1/lms/questions/{questionId}/options";
    /**
     * POST /v1/lms/questions/{questionId}/options : Create QuestionOption
     * Create a QuestionOption. Supports Idempotency-Key and validateOnly.
     *
     * @param questionId Question id. (required)
     * @param questionOption  (required)
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
        value = QuestionApi.PATH_CREATE_QUESTION_OPTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionOption> createQuestionOption(
         @PathVariable("questionId") UUID questionId,
         @Valid @RequestBody QuestionOption questionOption,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_QUESTION_PATTERN = "/v1/lms/questions/{questionId}/patterns";
    /**
     * POST /v1/lms/questions/{questionId}/patterns : Create QuestionPattern
     * Create a QuestionPattern. Supports Idempotency-Key and validateOnly.
     *
     * @param questionId Question id. (required)
     * @param questionPattern  (required)
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
        value = QuestionApi.PATH_CREATE_QUESTION_PATTERN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionPattern> createQuestionPattern(
         @PathVariable("questionId") UUID questionId,
         @Valid @RequestBody QuestionPattern questionPattern,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_QUESTION = "/v1/lms/questions/{questionId}";
    /**
     * DELETE /v1/lms/questions/{questionId} : Delete Question
     * Soft delete (sets deleted_at).
     *
     * @param questionId Question id. (required)
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
        value = QuestionApi.PATH_DELETE_QUESTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestion(
         @PathVariable("questionId") UUID questionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_QUESTION_ACCEPTED_ANSWER = "/v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}";
    /**
     * DELETE /v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId} : Delete QuestionAcceptedAnswer
     * Soft delete (sets deleted_at).
     *
     * @param questionId Question id. (required)
     * @param questionAcceptedAnswerId QuestionAcceptedAnswer id. (required)
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
        value = QuestionApi.PATH_DELETE_QUESTION_ACCEPTED_ANSWER,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestionAcceptedAnswer(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionAcceptedAnswerId") UUID questionAcceptedAnswerId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_QUESTION_BLANK = "/v1/lms/questions/{questionId}/blanks/{questionBlankId}";
    /**
     * DELETE /v1/lms/questions/{questionId}/blanks/{questionBlankId} : Delete QuestionBlank
     * Soft delete (sets deleted_at).
     *
     * @param questionId Question id. (required)
     * @param questionBlankId QuestionBlank id. (required)
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
        value = QuestionApi.PATH_DELETE_QUESTION_BLANK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestionBlank(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionBlankId") UUID questionBlankId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_QUESTION_HOTSPOT = "/v1/lms/questions/{questionId}/hotspots/{questionHotspotId}";
    /**
     * DELETE /v1/lms/questions/{questionId}/hotspots/{questionHotspotId} : Delete QuestionHotspot
     * Soft delete (sets deleted_at).
     *
     * @param questionId Question id. (required)
     * @param questionHotspotId QuestionHotspot id. (required)
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
        value = QuestionApi.PATH_DELETE_QUESTION_HOTSPOT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestionHotspot(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionHotspotId") UUID questionHotspotId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_QUESTION_MATCH_PAIR = "/v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}";
    /**
     * DELETE /v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId} : Delete QuestionMatchPair
     * Soft delete (sets deleted_at).
     *
     * @param questionId Question id. (required)
     * @param questionMatchPairId QuestionMatchPair id. (required)
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
        value = QuestionApi.PATH_DELETE_QUESTION_MATCH_PAIR,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestionMatchPair(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionMatchPairId") UUID questionMatchPairId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_QUESTION_OPTION = "/v1/lms/questions/{questionId}/options/{questionOptionId}";
    /**
     * DELETE /v1/lms/questions/{questionId}/options/{questionOptionId} : Delete QuestionOption
     * Soft delete (sets deleted_at).
     *
     * @param questionId Question id. (required)
     * @param questionOptionId QuestionOption id. (required)
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
        value = QuestionApi.PATH_DELETE_QUESTION_OPTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestionOption(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionOptionId") UUID questionOptionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_QUESTION_PATTERN = "/v1/lms/questions/{questionId}/patterns/{questionPatternId}";
    /**
     * DELETE /v1/lms/questions/{questionId}/patterns/{questionPatternId} : Delete QuestionPattern
     * Soft delete (sets deleted_at).
     *
     * @param questionId Question id. (required)
     * @param questionPatternId QuestionPattern id. (required)
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
        value = QuestionApi.PATH_DELETE_QUESTION_PATTERN,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteQuestionPattern(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionPatternId") UUID questionPatternId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_QUESTION = "/v1/lms/questions/{questionId}";
    /**
     * GET /v1/lms/questions/{questionId} : Get Question
     * Get one Question by id. The ETag header carries the row version.
     *
     * @param questionId Question id. (required)
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
        value = QuestionApi.PATH_GET_QUESTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Question> getQuestion(
         @PathVariable("questionId") UUID questionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUESTION_ACCEPTED_ANSWER = "/v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}";
    /**
     * GET /v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId} : Get QuestionAcceptedAnswer
     * Get one QuestionAcceptedAnswer by id. The ETag header carries the row version.
     *
     * @param questionId Question id. (required)
     * @param questionAcceptedAnswerId QuestionAcceptedAnswer id. (required)
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
        value = QuestionApi.PATH_GET_QUESTION_ACCEPTED_ANSWER,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuestionAcceptedAnswer> getQuestionAcceptedAnswer(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionAcceptedAnswerId") UUID questionAcceptedAnswerId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUESTION_BLANK = "/v1/lms/questions/{questionId}/blanks/{questionBlankId}";
    /**
     * GET /v1/lms/questions/{questionId}/blanks/{questionBlankId} : Get QuestionBlank
     * Get one QuestionBlank by id. The ETag header carries the row version.
     *
     * @param questionId Question id. (required)
     * @param questionBlankId QuestionBlank id. (required)
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
        value = QuestionApi.PATH_GET_QUESTION_BLANK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuestionBlank> getQuestionBlank(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionBlankId") UUID questionBlankId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUESTION_HOTSPOT = "/v1/lms/questions/{questionId}/hotspots/{questionHotspotId}";
    /**
     * GET /v1/lms/questions/{questionId}/hotspots/{questionHotspotId} : Get QuestionHotspot
     * Get one QuestionHotspot by id. The ETag header carries the row version.
     *
     * @param questionId Question id. (required)
     * @param questionHotspotId QuestionHotspot id. (required)
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
        value = QuestionApi.PATH_GET_QUESTION_HOTSPOT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuestionHotspot> getQuestionHotspot(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionHotspotId") UUID questionHotspotId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUESTION_MATCH_PAIR = "/v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}";
    /**
     * GET /v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId} : Get QuestionMatchPair
     * Get one QuestionMatchPair by id. The ETag header carries the row version.
     *
     * @param questionId Question id. (required)
     * @param questionMatchPairId QuestionMatchPair id. (required)
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
        value = QuestionApi.PATH_GET_QUESTION_MATCH_PAIR,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuestionMatchPair> getQuestionMatchPair(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionMatchPairId") UUID questionMatchPairId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUESTION_OPTION = "/v1/lms/questions/{questionId}/options/{questionOptionId}";
    /**
     * GET /v1/lms/questions/{questionId}/options/{questionOptionId} : Get QuestionOption
     * Get one QuestionOption by id. The ETag header carries the row version.
     *
     * @param questionId Question id. (required)
     * @param questionOptionId QuestionOption id. (required)
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
        value = QuestionApi.PATH_GET_QUESTION_OPTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuestionOption> getQuestionOption(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionOptionId") UUID questionOptionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_QUESTION_PATTERN = "/v1/lms/questions/{questionId}/patterns/{questionPatternId}";
    /**
     * GET /v1/lms/questions/{questionId}/patterns/{questionPatternId} : Get QuestionPattern
     * Get one QuestionPattern by id. The ETag header carries the row version.
     *
     * @param questionId Question id. (required)
     * @param questionPatternId QuestionPattern id. (required)
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
        value = QuestionApi.PATH_GET_QUESTION_PATTERN,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<QuestionPattern> getQuestionPattern(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionPatternId") UUID questionPatternId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_IMPORT_QUESTIONS = "/v1/lms/questions:import";
    /**
     * POST /v1/lms/questions:import : Import questions into the bank
     * Import questions into the bank.
     *
     * @param importQuestionsRequest  (required)
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
        value = QuestionApi.PATH_IMPORT_QUESTIONS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> importQuestions(
         @Valid @RequestBody ImportQuestionsRequest importQuestionsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_QUESTION_ACCEPTED_ANSWERS = "/v1/lms/questions/{questionId}/accepted-answers";
    /**
     * GET /v1/lms/questions/{questionId}/accepted-answers : List QuestionAcceptedAnswers
     * List QuestionAcceptedAnswers visible to the caller, page by page, with filtering and sorting.
     *
     * @param questionId Question id. (required)
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
        value = QuestionApi.PATH_LIST_QUESTION_ACCEPTED_ANSWERS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuestionAcceptedAnswer>> listQuestionAcceptedAnswers(
         @PathVariable("questionId") UUID questionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUESTION_BLANKS = "/v1/lms/questions/{questionId}/blanks";
    /**
     * GET /v1/lms/questions/{questionId}/blanks : List QuestionBlanks
     * List QuestionBlanks visible to the caller, page by page, with filtering and sorting.
     *
     * @param questionId Question id. (required)
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
        value = QuestionApi.PATH_LIST_QUESTION_BLANKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuestionBlank>> listQuestionBlanks(
         @PathVariable("questionId") UUID questionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUESTION_HOTSPOTS = "/v1/lms/questions/{questionId}/hotspots";
    /**
     * GET /v1/lms/questions/{questionId}/hotspots : List QuestionHotspots
     * List QuestionHotspots visible to the caller, page by page, with filtering and sorting.
     *
     * @param questionId Question id. (required)
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
        value = QuestionApi.PATH_LIST_QUESTION_HOTSPOTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuestionHotspot>> listQuestionHotspots(
         @PathVariable("questionId") UUID questionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUESTION_MATCH_PAIRS = "/v1/lms/questions/{questionId}/match-pairs";
    /**
     * GET /v1/lms/questions/{questionId}/match-pairs : List QuestionMatchPairs
     * List QuestionMatchPairs visible to the caller, page by page, with filtering and sorting.
     *
     * @param questionId Question id. (required)
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
        value = QuestionApi.PATH_LIST_QUESTION_MATCH_PAIRS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuestionMatchPair>> listQuestionMatchPairs(
         @PathVariable("questionId") UUID questionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUESTION_OPTIONS = "/v1/lms/questions/{questionId}/options";
    /**
     * GET /v1/lms/questions/{questionId}/options : List QuestionOptions
     * List QuestionOptions visible to the caller, page by page, with filtering and sorting.
     *
     * @param questionId Question id. (required)
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
        value = QuestionApi.PATH_LIST_QUESTION_OPTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuestionOption>> listQuestionOptions(
         @PathVariable("questionId") UUID questionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUESTION_PATTERNS = "/v1/lms/questions/{questionId}/patterns";
    /**
     * GET /v1/lms/questions/{questionId}/patterns : List QuestionPatterns
     * List QuestionPatterns visible to the caller, page by page, with filtering and sorting.
     *
     * @param questionId Question id. (required)
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
        value = QuestionApi.PATH_LIST_QUESTION_PATTERNS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<QuestionPattern>> listQuestionPatterns(
         @PathVariable("questionId") UUID questionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_QUESTIONS = "/v1/lms/questions";
    /**
     * GET /v1/lms/questions : List Questions
     * List Questions visible to the caller, page by page, with filtering and sorting.
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
        value = QuestionApi.PATH_LIST_QUESTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Question>> listQuestions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_SEARCH_QUESTION_BANK = "/v1/lms/questions:search";
    /**
     * GET /v1/lms/questions:search : Find questions to add to a quiz
     * Find questions to add to a quiz.
     *
     * @param query Text. (optional)
     * @param questionTypes Types. (optional)
     * @param excludeQuizId Exclude questions already in this quiz. (optional)
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
        value = QuestionApi.PATH_SEARCH_QUESTION_BANK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Question>> searchQuestionBank(
         @Valid @RequestParam(value = "query", required = false) @Nullable String query,
         @RequestParam(value = "questionTypes", required = false) @Nullable List<String> questionTypes,
         @Valid @RequestParam(value = "excludeQuizId", required = false) @Nullable UUID excludeQuizId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_UPDATE_QUESTION = "/v1/lms/questions/{questionId}";
    /**
     * PATCH /v1/lms/questions/{questionId} : Update Question
     * Partially update a Question (updateMask). Requires If-Match with the current ETag.
     *
     * @param questionId Question id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionUpdate  (required)
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
        value = QuestionApi.PATH_UPDATE_QUESTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Question> updateQuestion(
         @PathVariable("questionId") UUID questionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionUpdate questionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_QUESTION_ACCEPTED_ANSWER = "/v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId}";
    /**
     * PATCH /v1/lms/questions/{questionId}/accepted-answers/{questionAcceptedAnswerId} : Update QuestionAcceptedAnswer
     * Partially update a QuestionAcceptedAnswer (updateMask). Requires If-Match with the current ETag.
     *
     * @param questionId Question id. (required)
     * @param questionAcceptedAnswerId QuestionAcceptedAnswer id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionAcceptedAnswerUpdate  (required)
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
        value = QuestionApi.PATH_UPDATE_QUESTION_ACCEPTED_ANSWER,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionAcceptedAnswer> updateQuestionAcceptedAnswer(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionAcceptedAnswerId") UUID questionAcceptedAnswerId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionAcceptedAnswerUpdate questionAcceptedAnswerUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_QUESTION_BLANK = "/v1/lms/questions/{questionId}/blanks/{questionBlankId}";
    /**
     * PATCH /v1/lms/questions/{questionId}/blanks/{questionBlankId} : Update QuestionBlank
     * Partially update a QuestionBlank (updateMask). Requires If-Match with the current ETag.
     *
     * @param questionId Question id. (required)
     * @param questionBlankId QuestionBlank id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionBlankUpdate  (required)
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
        value = QuestionApi.PATH_UPDATE_QUESTION_BLANK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionBlank> updateQuestionBlank(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionBlankId") UUID questionBlankId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionBlankUpdate questionBlankUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_QUESTION_HOTSPOT = "/v1/lms/questions/{questionId}/hotspots/{questionHotspotId}";
    /**
     * PATCH /v1/lms/questions/{questionId}/hotspots/{questionHotspotId} : Update QuestionHotspot
     * Partially update a QuestionHotspot (updateMask). Requires If-Match with the current ETag.
     *
     * @param questionId Question id. (required)
     * @param questionHotspotId QuestionHotspot id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionHotspotUpdate  (required)
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
        value = QuestionApi.PATH_UPDATE_QUESTION_HOTSPOT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionHotspot> updateQuestionHotspot(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionHotspotId") UUID questionHotspotId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionHotspotUpdate questionHotspotUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_QUESTION_MATCH_PAIR = "/v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId}";
    /**
     * PATCH /v1/lms/questions/{questionId}/match-pairs/{questionMatchPairId} : Update QuestionMatchPair
     * Partially update a QuestionMatchPair (updateMask). Requires If-Match with the current ETag.
     *
     * @param questionId Question id. (required)
     * @param questionMatchPairId QuestionMatchPair id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionMatchPairUpdate  (required)
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
        value = QuestionApi.PATH_UPDATE_QUESTION_MATCH_PAIR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionMatchPair> updateQuestionMatchPair(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionMatchPairId") UUID questionMatchPairId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionMatchPairUpdate questionMatchPairUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_QUESTION_OPTION = "/v1/lms/questions/{questionId}/options/{questionOptionId}";
    /**
     * PATCH /v1/lms/questions/{questionId}/options/{questionOptionId} : Update QuestionOption
     * Partially update a QuestionOption (updateMask). Requires If-Match with the current ETag.
     *
     * @param questionId Question id. (required)
     * @param questionOptionId QuestionOption id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionOptionUpdate  (required)
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
        value = QuestionApi.PATH_UPDATE_QUESTION_OPTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionOption> updateQuestionOption(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionOptionId") UUID questionOptionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionOptionUpdate questionOptionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_QUESTION_PATTERN = "/v1/lms/questions/{questionId}/patterns/{questionPatternId}";
    /**
     * PATCH /v1/lms/questions/{questionId}/patterns/{questionPatternId} : Update QuestionPattern
     * Partially update a QuestionPattern (updateMask). Requires If-Match with the current ETag.
     *
     * @param questionId Question id. (required)
     * @param questionPatternId QuestionPattern id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param questionPatternUpdate  (required)
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
        value = QuestionApi.PATH_UPDATE_QUESTION_PATTERN,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<QuestionPattern> updateQuestionPattern(
         @PathVariable("questionId") UUID questionId,
         @PathVariable("questionPatternId") UUID questionPatternId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody QuestionPatternUpdate questionPatternUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
