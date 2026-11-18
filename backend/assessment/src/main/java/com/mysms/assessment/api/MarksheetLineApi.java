// Owned API contract of assessment.
package com.mysms.assessment.api;

import com.mysms.assessment.api.model.ResultCard;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.api.model.MarksheetLine;
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
public interface MarksheetLineApi {

    String PATH_GET_MARKSHEET_LINE = "/v1/assessment/marksheet-lines/{marksheetLineId}";
    /**
     * GET /v1/assessment/marksheet-lines/{marksheetLineId} : Get MarksheetLine
     * Get one MarksheetLine by id. The ETag header carries the row version.
     *
     * @param marksheetLineId MarksheetLine id. (required)
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
        value = MarksheetLineApi.PATH_GET_MARKSHEET_LINE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<MarksheetLine> getMarksheetLine(
         @PathVariable("marksheetLineId") UUID marksheetLineId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_STUDENT_RESULTS = "/v1/assessment/students/{studentId}/results";
    /**
     * GET /v1/assessment/students/{studentId}/results : Report-card data for a student (validated marksheets only for students/guardians)
     * Report-card data for a student (validated marksheets only for students/guardians).
     *
     * @param studentId  (required)
     * @param academicYearId Year. (optional)
     * @param examSessionId Series. (optional)
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
        value = MarksheetLineApi.PATH_GET_STUDENT_RESULTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ResultCard>> getStudentResults(
         @PathVariable("studentId") UUID studentId,
         @Valid @RequestParam(value = "academicYearId", required = false) @Nullable UUID academicYearId,
         @Valid @RequestParam(value = "examSessionId", required = false) @Nullable UUID examSessionId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_LIST_MARKSHEET_LINES = "/v1/assessment/marksheet-lines";
    /**
     * GET /v1/assessment/marksheet-lines : List MarksheetLines
     * List MarksheetLines visible to the caller, page by page, with filtering and sorting.
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
        value = MarksheetLineApi.PATH_LIST_MARKSHEET_LINES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<MarksheetLine>> listMarksheetLines(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );

}
