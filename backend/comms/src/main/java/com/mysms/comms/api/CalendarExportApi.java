// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.CalendarExport;
import com.mysms.comms.api.model.ExportCalendarRequest;
import org.springframework.lang.Nullable;
import com.mysms.comms.api.model.Operation;
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
public interface CalendarExportApi {

    String PATH_DELETE_CALENDAR_EXPORT = "/v1/comms/calendar-exports/{calendarExportId}";
    /**
     * DELETE /v1/comms/calendar-exports/{calendarExportId} : Delete CalendarExport
     * Soft delete (sets deleted_at).
     *
     * @param calendarExportId CalendarExport id. (required)
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
        value = CalendarExportApi.PATH_DELETE_CALENDAR_EXPORT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteCalendarExport(
         @PathVariable("calendarExportId") UUID calendarExportId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_EXPORT_CALENDAR = "/v1/comms/me/calendar:export";
    /**
     * POST /v1/comms/me/calendar:export : Push the caller&#39;s calendar items to a connected external calendar (updates and deletions are tracked)
     * Push the caller&#39;s calendar items to a connected external calendar (updates and deletions are tracked).
     *
     * @param exportCalendarRequest  (required)
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
        value = CalendarExportApi.PATH_EXPORT_CALENDAR,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> exportCalendar(
         @Valid @RequestBody ExportCalendarRequest exportCalendarRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_CALENDAR_EXPORT = "/v1/comms/calendar-exports/{calendarExportId}";
    /**
     * GET /v1/comms/calendar-exports/{calendarExportId} : Get CalendarExport
     * Get one CalendarExport by id. The ETag header carries the row version.
     *
     * @param calendarExportId CalendarExport id. (required)
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
        value = CalendarExportApi.PATH_GET_CALENDAR_EXPORT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<CalendarExport> getCalendarExport(
         @PathVariable("calendarExportId") UUID calendarExportId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CALENDAR_EXPORTS = "/v1/comms/calendar-exports";
    /**
     * GET /v1/comms/calendar-exports : List CalendarExports
     * List CalendarExports visible to the caller, page by page, with filtering and sorting.
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
        value = CalendarExportApi.PATH_LIST_CALENDAR_EXPORTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<CalendarExport>> listCalendarExports(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );

}
