// Owned API contract of assessment.
package com.mysms.assessment.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.api.model.CancelExamSessionRequest;
import com.mysms.assessment.api.model.CompleteExamSessionRequest;
import com.mysms.assessment.api.model.ExamSession;
import com.mysms.assessment.api.model.ExamSessionUpdate;
import com.mysms.assessment.api.model.MarkExamSessionHeldRequest;
import org.springframework.lang.Nullable;
import com.mysms.assessment.api.model.ResetExamSessionRequest;
import com.mysms.assessment.api.model.ScheduleExamSessionRequest;
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
public interface ExamSessionApi {

    String PATH_CANCEL_EXAM_SESSION = "/v1/assessment/exam-sessions/{examSessionId}:cancel";
    /**
     * POST /v1/assessment/exam-sessions/{examSessionId}:cancel : Cancel
     * Cancel.
     *
     * @param examSessionId  (required)
     * @param cancelExamSessionRequest  (required)
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
        value = ExamSessionApi.PATH_CANCEL_EXAM_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamSession> cancelExamSession(
         @PathVariable("examSessionId") UUID examSessionId,
         @Valid @RequestBody CancelExamSessionRequest cancelExamSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_COMPLETE_EXAM_SESSION = "/v1/assessment/exam-sessions/{examSessionId}:complete";
    /**
     * POST /v1/assessment/exam-sessions/{examSessionId}:complete : Done
     * Done.
     *
     * @param examSessionId  (required)
     * @param completeExamSessionRequest  (required)
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
        value = ExamSessionApi.PATH_COMPLETE_EXAM_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamSession> completeExamSession(
         @PathVariable("examSessionId") UUID examSessionId,
         @Valid @RequestBody CompleteExamSessionRequest completeExamSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_EXAM_SESSION = "/v1/assessment/exam-sessions";
    /**
     * POST /v1/assessment/exam-sessions : Create ExamSession
     * Create a ExamSession. Supports Idempotency-Key and validateOnly.
     *
     * @param examSession  (required)
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
        value = ExamSessionApi.PATH_CREATE_EXAM_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamSession> createExamSession(
         @Valid @RequestBody ExamSession examSession,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_EXAM_SESSION = "/v1/assessment/exam-sessions/{examSessionId}";
    /**
     * DELETE /v1/assessment/exam-sessions/{examSessionId} : Delete ExamSession
     * Soft delete (sets deleted_at).
     *
     * @param examSessionId ExamSession id. (required)
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
        value = ExamSessionApi.PATH_DELETE_EXAM_SESSION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteExamSession(
         @PathVariable("examSessionId") UUID examSessionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_EXAM_SESSION = "/v1/assessment/exam-sessions/{examSessionId}";
    /**
     * GET /v1/assessment/exam-sessions/{examSessionId} : Get ExamSession
     * Get one ExamSession by id. The ETag header carries the row version.
     *
     * @param examSessionId ExamSession id. (required)
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
        value = ExamSessionApi.PATH_GET_EXAM_SESSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExamSession> getExamSession(
         @PathVariable("examSessionId") UUID examSessionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_EXAM_SESSIONS = "/v1/assessment/exam-sessions";
    /**
     * GET /v1/assessment/exam-sessions : List ExamSessions
     * List ExamSessions visible to the caller, page by page, with filtering and sorting.
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
        value = ExamSessionApi.PATH_LIST_EXAM_SESSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ExamSession>> listExamSessions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_EXAM_SESSION_HELD = "/v1/assessment/exam-sessions/{examSessionId}:markHeld";
    /**
     * POST /v1/assessment/exam-sessions/{examSessionId}:markHeld : Mark held
     * Mark held.
     *
     * @param examSessionId  (required)
     * @param markExamSessionHeldRequest  (required)
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
        value = ExamSessionApi.PATH_MARK_EXAM_SESSION_HELD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamSession> markExamSessionHeld(
         @PathVariable("examSessionId") UUID examSessionId,
         @Valid @RequestBody MarkExamSessionHeldRequest markExamSessionHeldRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_EXAM_SESSION = "/v1/assessment/exam-sessions/{examSessionId}:resetToDraft";
    /**
     * POST /v1/assessment/exam-sessions/{examSessionId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param examSessionId  (required)
     * @param resetExamSessionRequest  (required)
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
        value = ExamSessionApi.PATH_RESET_EXAM_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamSession> resetExamSession(
         @PathVariable("examSessionId") UUID examSessionId,
         @Valid @RequestBody ResetExamSessionRequest resetExamSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SCHEDULE_EXAM_SESSION = "/v1/assessment/exam-sessions/{examSessionId}:schedule";
    /**
     * POST /v1/assessment/exam-sessions/{examSessionId}:schedule : Schedule
     * Schedule.
     *
     * @param examSessionId  (required)
     * @param scheduleExamSessionRequest  (required)
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
        value = ExamSessionApi.PATH_SCHEDULE_EXAM_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamSession> scheduleExamSession(
         @PathVariable("examSessionId") UUID examSessionId,
         @Valid @RequestBody ScheduleExamSessionRequest scheduleExamSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_EXAM_SESSION = "/v1/assessment/exam-sessions/{examSessionId}";
    /**
     * PATCH /v1/assessment/exam-sessions/{examSessionId} : Update ExamSession
     * Partially update a ExamSession (updateMask). Requires If-Match with the current ETag.
     *
     * @param examSessionId ExamSession id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param examSessionUpdate  (required)
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
        value = ExamSessionApi.PATH_UPDATE_EXAM_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ExamSession> updateExamSession(
         @PathVariable("examSessionId") UUID examSessionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ExamSessionUpdate examSessionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
