// Owned API contract of timetable.
package com.mysms.timetable.api;

import com.mysms.timetable.api.model.TimetableView;
import com.mysms.core.paging.PagingInfo;
import com.mysms.timetable.api.model.BatchConfirmSessionsRequest;
import com.mysms.timetable.api.model.CancelSessionRequest;
import com.mysms.timetable.api.model.CompleteSessionRequest;
import com.mysms.timetable.api.model.ConfirmSessionRequest;
import org.springframework.format.annotation.DateTimeFormat;
import com.mysms.timetable.api.model.GenerateSessionsRequest;
import java.time.LocalDate;
import org.springframework.lang.Nullable;
import com.mysms.timetable.api.model.Operation;
import com.mysms.timetable.api.model.ResetSessionRequest;
import com.mysms.timetable.api.model.Session;
import com.mysms.timetable.api.model.SessionList;
import com.mysms.timetable.api.model.SessionStudent;
import com.mysms.timetable.api.model.SessionStudentUpdate;
import com.mysms.timetable.api.model.SessionUpdate;
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
public interface SessionApi {

    String PATH_BATCH_CONFIRM_SESSIONS = "/v1/timetable/sessions:batchConfirm";
    /**
     * POST /v1/timetable/sessions:batchConfirm : Confirm many draft sessions at once
     * Confirm many draft sessions at once.
     *
     * @param batchConfirmSessionsRequest  (required)
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
        value = SessionApi.PATH_BATCH_CONFIRM_SESSIONS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SessionList> batchConfirmSessions(
         @Valid @RequestBody BatchConfirmSessionsRequest batchConfirmSessionsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CANCEL_SESSION = "/v1/timetable/sessions/{sessionId}:cancel";
    /**
     * POST /v1/timetable/sessions/{sessionId}:cancel : Cancel
     * Cancel.
     *
     * @param sessionId  (required)
     * @param cancelSessionRequest  (required)
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
        value = SessionApi.PATH_CANCEL_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Session> cancelSession(
         @PathVariable("sessionId") UUID sessionId,
         @Valid @RequestBody CancelSessionRequest cancelSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_COMPLETE_SESSION = "/v1/timetable/sessions/{sessionId}:complete";
    /**
     * POST /v1/timetable/sessions/{sessionId}:complete : Mark done
     * Mark done.
     *
     * @param sessionId  (required)
     * @param completeSessionRequest  (required)
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
        value = SessionApi.PATH_COMPLETE_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Session> completeSession(
         @PathVariable("sessionId") UUID sessionId,
         @Valid @RequestBody CompleteSessionRequest completeSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CONFIRM_SESSION = "/v1/timetable/sessions/{sessionId}:confirm";
    /**
     * POST /v1/timetable/sessions/{sessionId}:confirm : Confirm
     * Confirm.
     *
     * @param sessionId  (required)
     * @param confirmSessionRequest  (required)
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
        value = SessionApi.PATH_CONFIRM_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Session> confirmSession(
         @PathVariable("sessionId") UUID sessionId,
         @Valid @RequestBody ConfirmSessionRequest confirmSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_SESSION = "/v1/timetable/sessions";
    /**
     * POST /v1/timetable/sessions : Create Session
     * Create a Session. Supports Idempotency-Key and validateOnly.
     *
     * @param session  (required)
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
        value = SessionApi.PATH_CREATE_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Session> createSession(
         @Valid @RequestBody Session session,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_SESSION_STUDENT = "/v1/timetable/sessions/{sessionId}/students";
    /**
     * POST /v1/timetable/sessions/{sessionId}/students : Create SessionStudent
     * Create a SessionStudent. Supports Idempotency-Key and validateOnly.
     *
     * @param sessionId Session id. (required)
     * @param sessionStudent  (required)
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
        value = SessionApi.PATH_CREATE_SESSION_STUDENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SessionStudent> createSessionStudent(
         @PathVariable("sessionId") UUID sessionId,
         @Valid @RequestBody SessionStudent sessionStudent,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_SESSION = "/v1/timetable/sessions/{sessionId}";
    /**
     * DELETE /v1/timetable/sessions/{sessionId} : Delete Session
     * Soft delete (sets deleted_at).
     *
     * @param sessionId Session id. (required)
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
        value = SessionApi.PATH_DELETE_SESSION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSession(
         @PathVariable("sessionId") UUID sessionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_SESSION_STUDENT = "/v1/timetable/sessions/{sessionId}/students/{sessionStudentId}";
    /**
     * DELETE /v1/timetable/sessions/{sessionId}/students/{sessionStudentId} : Delete SessionStudent
     * Soft delete (sets deleted_at).
     *
     * @param sessionId Session id. (required)
     * @param sessionStudentId SessionStudent id. (required)
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
        value = SessionApi.PATH_DELETE_SESSION_STUDENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteSessionStudent(
         @PathVariable("sessionId") UUID sessionId,
         @PathVariable("sessionStudentId") UUID sessionStudentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GENERATE_SESSIONS = "/v1/timetable/sessions:generate";
    /**
     * POST /v1/timetable/sessions:generate : Generate sessions from a weekly pattern; clashes (teacher, room, class) are reported
     * Generate sessions from a weekly pattern; clashes (teacher, room, class) are reported.
     *
     * @param generateSessionsRequest  (required)
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
        value = SessionApi.PATH_GENERATE_SESSIONS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> generateSessions(
         @Valid @RequestBody GenerateSessionsRequest generateSessionsRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_SESSION = "/v1/timetable/sessions/{sessionId}";
    /**
     * GET /v1/timetable/sessions/{sessionId} : Get Session
     * Get one Session by id. The ETag header carries the row version.
     *
     * @param sessionId Session id. (required)
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
        value = SessionApi.PATH_GET_SESSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Session> getSession(
         @PathVariable("sessionId") UUID sessionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_SESSION_STUDENT = "/v1/timetable/sessions/{sessionId}/students/{sessionStudentId}";
    /**
     * GET /v1/timetable/sessions/{sessionId}/students/{sessionStudentId} : Get SessionStudent
     * Get one SessionStudent by id. The ETag header carries the row version.
     *
     * @param sessionId Session id. (required)
     * @param sessionStudentId SessionStudent id. (required)
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
        value = SessionApi.PATH_GET_SESSION_STUDENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<SessionStudent> getSessionStudent(
         @PathVariable("sessionId") UUID sessionId,
         @PathVariable("sessionStudentId") UUID sessionStudentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_TIMETABLE = "/v1/timetable/sessions:timetable";
    /**
     * GET /v1/timetable/sessions:timetable : Timetable view for a class, teacher, student or room
     * Timetable view for a class, teacher, student or room.
     *
     * @param fromDate From. (required)
     * @param toDate To. (required)
     * @param classSectionId Class. (optional)
     * @param batchId Batch. (optional)
     * @param staffId Teacher. (optional)
     * @param studentId Student. (optional)
     * @param roomId Room. (optional)
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
        value = SessionApi.PATH_GET_TIMETABLE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<TimetableView>> getTimetable(
        @NotNull  @Valid @RequestParam(value = "fromDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @NotNull  @Valid @RequestParam(value = "toDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
         @Valid @RequestParam(value = "classSectionId", required = false) @Nullable UUID classSectionId,
         @Valid @RequestParam(value = "batchId", required = false) @Nullable UUID batchId,
         @Valid @RequestParam(value = "staffId", required = false) @Nullable UUID staffId,
         @Valid @RequestParam(value = "studentId", required = false) @Nullable UUID studentId,
         @Valid @RequestParam(value = "roomId", required = false) @Nullable UUID roomId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_LIST_SESSION_STUDENTS = "/v1/timetable/sessions/{sessionId}/students";
    /**
     * GET /v1/timetable/sessions/{sessionId}/students : List SessionStudents
     * List SessionStudents visible to the caller, page by page, with filtering and sorting.
     *
     * @param sessionId Session id. (required)
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
        value = SessionApi.PATH_LIST_SESSION_STUDENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<SessionStudent>> listSessionStudents(
         @PathVariable("sessionId") UUID sessionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_SESSIONS = "/v1/timetable/sessions";
    /**
     * GET /v1/timetable/sessions : List Sessions
     * List Sessions visible to the caller, page by page, with filtering and sorting.
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
        value = SessionApi.PATH_LIST_SESSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Session>> listSessions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RESET_SESSION = "/v1/timetable/sessions/{sessionId}:resetToDraft";
    /**
     * POST /v1/timetable/sessions/{sessionId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param sessionId  (required)
     * @param resetSessionRequest  (required)
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
        value = SessionApi.PATH_RESET_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Session> resetSession(
         @PathVariable("sessionId") UUID sessionId,
         @Valid @RequestBody ResetSessionRequest resetSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_SESSION = "/v1/timetable/sessions/{sessionId}";
    /**
     * PATCH /v1/timetable/sessions/{sessionId} : Update Session
     * Partially update a Session (updateMask). Requires If-Match with the current ETag.
     *
     * @param sessionId Session id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param sessionUpdate  (required)
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
        value = SessionApi.PATH_UPDATE_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Session> updateSession(
         @PathVariable("sessionId") UUID sessionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SessionUpdate sessionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_SESSION_STUDENT = "/v1/timetable/sessions/{sessionId}/students/{sessionStudentId}";
    /**
     * PATCH /v1/timetable/sessions/{sessionId}/students/{sessionStudentId} : Update SessionStudent
     * Partially update a SessionStudent (updateMask). Requires If-Match with the current ETag.
     *
     * @param sessionId Session id. (required)
     * @param sessionStudentId SessionStudent id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param sessionStudentUpdate  (required)
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
        value = SessionApi.PATH_UPDATE_SESSION_STUDENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<SessionStudent> updateSessionStudent(
         @PathVariable("sessionId") UUID sessionId,
         @PathVariable("sessionStudentId") UUID sessionStudentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody SessionStudentUpdate sessionStudentUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
