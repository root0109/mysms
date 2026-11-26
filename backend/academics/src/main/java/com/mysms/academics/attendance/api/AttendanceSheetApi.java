// Owned API contract of attendance.
package com.mysms.academics.attendance.api;

import com.mysms.academics.attendance.api.model.AttendanceSummary;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.attendance.api.model.AttendanceRecord;
import com.mysms.academics.attendance.api.model.AttendanceRecordList;
import com.mysms.academics.attendance.api.model.AttendanceSheet;
import com.mysms.academics.attendance.api.model.AttendanceSheetUpdate;
import com.mysms.academics.attendance.api.model.CancelAttendanceSheetRequest;
import com.mysms.academics.attendance.api.model.CompleteAttendanceSheetRequest;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import org.springframework.lang.Nullable;
import com.mysms.academics.attendance.api.model.RecordAttendanceRequest;
import com.mysms.academics.attendance.api.model.ResetAttendanceSheetRequest;
import com.mysms.academics.attendance.api.model.StartAttendanceSheetRequest;
import com.mysms.academics.attendance.api.model.TakeAttendanceForSessionRequest;
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
public interface AttendanceSheetApi {

    String PATH_CANCEL_ATTENDANCE_SHEET = "/v1/attendance/attendance-sheets/{attendanceSheetId}:cancel";
    /**
     * POST /v1/attendance/attendance-sheets/{attendanceSheetId}:cancel : Cancel
     * Cancel.
     *
     * @param attendanceSheetId  (required)
     * @param cancelAttendanceSheetRequest  (required)
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
        value = AttendanceSheetApi.PATH_CANCEL_ATTENDANCE_SHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AttendanceSheet> cancelAttendanceSheet(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
         @Valid @RequestBody CancelAttendanceSheetRequest cancelAttendanceSheetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_COMPLETE_ATTENDANCE_SHEET = "/v1/attendance/attendance-sheets/{attendanceSheetId}:complete";
    /**
     * POST /v1/attendance/attendance-sheets/{attendanceSheetId}:complete : Attendance taken; guardians of absentees are notified
     * Attendance taken; guardians of absentees are notified.
     *
     * @param attendanceSheetId  (required)
     * @param completeAttendanceSheetRequest  (required)
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
        value = AttendanceSheetApi.PATH_COMPLETE_ATTENDANCE_SHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AttendanceSheet> completeAttendanceSheet(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
         @Valid @RequestBody CompleteAttendanceSheetRequest completeAttendanceSheetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_ATTENDANCE_SHEET = "/v1/attendance/attendance-sheets";
    /**
     * POST /v1/attendance/attendance-sheets : Create AttendanceSheet
     * Create a AttendanceSheet. Supports Idempotency-Key and validateOnly.
     *
     * @param attendanceSheet  (required)
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
        value = AttendanceSheetApi.PATH_CREATE_ATTENDANCE_SHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AttendanceSheet> createAttendanceSheet(
         @Valid @RequestBody AttendanceSheet attendanceSheet,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ATTENDANCE_SHEET = "/v1/attendance/attendance-sheets/{attendanceSheetId}";
    /**
     * DELETE /v1/attendance/attendance-sheets/{attendanceSheetId} : Delete AttendanceSheet
     * Soft delete (sets deleted_at).
     *
     * @param attendanceSheetId AttendanceSheet id. (required)
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
        value = AttendanceSheetApi.PATH_DELETE_ATTENDANCE_SHEET,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAttendanceSheet(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_ATTENDANCE_RECORD = "/v1/attendance/attendance-sheets/{attendanceSheetId}/records/{attendanceRecordId}";
    /**
     * GET /v1/attendance/attendance-sheets/{attendanceSheetId}/records/{attendanceRecordId} : Get AttendanceRecord
     * Get one AttendanceRecord by id. The ETag header carries the row version.
     *
     * @param attendanceSheetId AttendanceSheet id. (required)
     * @param attendanceRecordId AttendanceRecord id. (required)
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
        value = AttendanceSheetApi.PATH_GET_ATTENDANCE_RECORD,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AttendanceRecord> getAttendanceRecord(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
         @PathVariable("attendanceRecordId") UUID attendanceRecordId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ATTENDANCE_SHEET = "/v1/attendance/attendance-sheets/{attendanceSheetId}";
    /**
     * GET /v1/attendance/attendance-sheets/{attendanceSheetId} : Get AttendanceSheet
     * Get one AttendanceSheet by id. The ETag header carries the row version.
     *
     * @param attendanceSheetId AttendanceSheet id. (required)
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
        value = AttendanceSheetApi.PATH_GET_ATTENDANCE_SHEET,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AttendanceSheet> getAttendanceSheet(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ATTENDANCE_RECORDS = "/v1/attendance/attendance-sheets/{attendanceSheetId}/records";
    /**
     * GET /v1/attendance/attendance-sheets/{attendanceSheetId}/records : List AttendanceRecords
     * List AttendanceRecords visible to the caller, page by page, with filtering and sorting.
     *
     * @param attendanceSheetId AttendanceSheet id. (required)
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
        value = AttendanceSheetApi.PATH_LIST_ATTENDANCE_RECORDS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AttendanceRecord>> listAttendanceRecords(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ATTENDANCE_SHEETS = "/v1/attendance/attendance-sheets";
    /**
     * GET /v1/attendance/attendance-sheets : List AttendanceSheets
     * List AttendanceSheets visible to the caller, page by page, with filtering and sorting.
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
        value = AttendanceSheetApi.PATH_LIST_ATTENDANCE_SHEETS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AttendanceSheet>> listAttendanceSheets(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_RECORD_ATTENDANCE = "/v1/attendance/attendance-sheets/{attendanceSheetId}:recordAttendance";
    /**
     * POST /v1/attendance/attendance-sheets/{attendanceSheetId}:recordAttendance : Record or correct marks for many students in one call
     * Record or correct marks for many students in one call.
     *
     * @param attendanceSheetId  (required)
     * @param recordAttendanceRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or The etag is not the sheet's current one. (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = AttendanceSheetApi.PATH_RECORD_ATTENDANCE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AttendanceRecordList> recordAttendance(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
         @Valid @RequestBody RecordAttendanceRequest recordAttendanceRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RESET_ATTENDANCE_SHEET = "/v1/attendance/attendance-sheets/{attendanceSheetId}:resetToDraft";
    /**
     * POST /v1/attendance/attendance-sheets/{attendanceSheetId}:resetToDraft : Back to draft
     * Back to draft.
     *
     * @param attendanceSheetId  (required)
     * @param resetAttendanceSheetRequest  (required)
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
        value = AttendanceSheetApi.PATH_RESET_ATTENDANCE_SHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AttendanceSheet> resetAttendanceSheet(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
         @Valid @RequestBody ResetAttendanceSheetRequest resetAttendanceSheetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_START_ATTENDANCE_SHEET = "/v1/attendance/attendance-sheets/{attendanceSheetId}:start";
    /**
     * POST /v1/attendance/attendance-sheets/{attendanceSheetId}:start : Start taking attendance
     * Start taking attendance.
     *
     * @param attendanceSheetId  (required)
     * @param startAttendanceSheetRequest  (required)
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
        value = AttendanceSheetApi.PATH_START_ATTENDANCE_SHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AttendanceSheet> startAttendanceSheet(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
         @Valid @RequestBody StartAttendanceSheetRequest startAttendanceSheetRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUMMARIZE_ATTENDANCE = "/v1/attendance/summaries";
    /**
     * GET /v1/attendance/summaries : Attendance counts and percentages
     * Attendance counts and percentages.
     *
     * @param institutionId Institution. (required)
     * @param fromDate From. (required)
     * @param toDate To. (required)
     * @param groupBy Grouping. (required)
     * @param studentId Student. (optional)
     * @param classSectionId Class. (optional)
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
        value = AttendanceSheetApi.PATH_SUMMARIZE_ATTENDANCE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AttendanceSummary>> summarizeAttendance(
        @NotNull  @Valid @RequestParam(value = "institutionId", required = true) UUID institutionId,
        @NotNull  @Valid @RequestParam(value = "fromDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @NotNull  @Valid @RequestParam(value = "toDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @NotNull  @Valid @RequestParam(value = "groupBy", required = true) String groupBy,
         @Valid @RequestParam(value = "studentId", required = false) @Nullable UUID studentId,
         @Valid @RequestParam(value = "classSectionId", required = false) @Nullable UUID classSectionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_TAKE_ATTENDANCE_FOR_SESSION = "/v1/attendance/attendance-sheets:forSession";
    /**
     * POST /v1/attendance/attendance-sheets:forSession : Get or create the sheet for a timetable session (pre-filled with expected students)
     * Get or create the sheet for a timetable session (pre-filled with expected students).
     *
     * @param takeAttendanceForSessionRequest  (required)
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
     *         or Temporarily unavailable: a service the operation reads from could not answer. (status code 503)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = AttendanceSheetApi.PATH_TAKE_ATTENDANCE_FOR_SESSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AttendanceSheet> takeAttendanceForSession(
         @Valid @RequestBody TakeAttendanceForSessionRequest takeAttendanceForSessionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_ATTENDANCE_SHEET = "/v1/attendance/attendance-sheets/{attendanceSheetId}";
    /**
     * PATCH /v1/attendance/attendance-sheets/{attendanceSheetId} : Update AttendanceSheet
     * Partially update a AttendanceSheet (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param attendanceSheetId AttendanceSheet id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param attendanceSheetUpdate  (required)
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
        value = AttendanceSheetApi.PATH_UPDATE_ATTENDANCE_SHEET,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AttendanceSheet> updateAttendanceSheet(
         @PathVariable("attendanceSheetId") UUID attendanceSheetId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AttendanceSheetUpdate attendanceSheetUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
