// Owned API contract of lms.
package com.mysms.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.model.BatchGradeEntriesRequest;
import com.mysms.lms.api.model.GradeEntryRequest;
import com.mysms.lms.api.model.GradeEntryResultList;
import com.mysms.lms.api.model.GradebookEntry;
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
public interface GradebookEntryApi {

    String PATH_BATCH_GRADE_ENTRIES = "/v1/lms/gradebook-entries:batchGrade";
    /**
     * POST /v1/lms/gradebook-entries:batchGrade : Grade many students in one call; each row succeeds or fails on its own (stale etags are reported per row)
     * Grade many students in one call; each row succeeds or fails on its own (stale etags are reported per row).
     *
     * @param batchGradeEntriesRequest  (required)
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
        value = GradebookEntryApi.PATH_BATCH_GRADE_ENTRIES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<GradeEntryResultList> batchGradeEntries(
         @Valid @RequestBody BatchGradeEntriesRequest batchGradeEntriesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_GRADEBOOK_ENTRY = "/v1/lms/gradebook-entries/{gradebookEntryId}";
    /**
     * GET /v1/lms/gradebook-entries/{gradebookEntryId} : Get GradebookEntry
     * Get one GradebookEntry by id. The ETag header carries the row version.
     *
     * @param gradebookEntryId GradebookEntry id. (required)
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
        value = GradebookEntryApi.PATH_GET_GRADEBOOK_ENTRY,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<GradebookEntry> getGradebookEntry(
         @PathVariable("gradebookEntryId") UUID gradebookEntryId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GRADE_ENTRY = "/v1/lms/gradebook-entries:grade";
    /**
     * POST /v1/lms/gradebook-entries:grade : Create or update the grade of a student / group for an item; records grade history and rubric choices
     * Create or update the grade of a student / group for an item; records grade history and rubric choices.
     *
     * @param gradeEntryRequest  (required)
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
        value = GradebookEntryApi.PATH_GRADE_ENTRY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<GradebookEntry> gradeEntry(
         @Valid @RequestBody GradeEntryRequest gradeEntryRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_GRADEBOOK_ENTRIES = "/v1/lms/gradebook-entries";
    /**
     * GET /v1/lms/gradebook-entries : List GradebookEntries
     * List GradebookEntries visible to the caller, page by page, with filtering and sorting.
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
        value = GradebookEntryApi.PATH_LIST_GRADEBOOK_ENTRIES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<GradebookEntry>> listGradebookEntries(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );

}
