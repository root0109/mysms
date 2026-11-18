// Owned API contract of assessment.
package com.mysms.assessment.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.api.model.CheckHomeworkSubmissionRequest;
import com.mysms.assessment.api.model.HomeworkSubmission;
import com.mysms.assessment.api.model.HomeworkSubmissionUpdate;
import org.springframework.lang.Nullable;
import com.mysms.assessment.api.model.ReplyToHomeworkFeedbackRequest;
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
public interface HomeworkSubmissionApi {

    String PATH_CHECK_HOMEWORK_SUBMISSION = "/v1/assessment/homework-submissions/{homeworkSubmissionId}:check";
    /**
     * POST /v1/assessment/homework-submissions/{homeworkSubmissionId}:check : Teacher marks the submission checked
     * Teacher marks the submission checked.
     *
     * @param homeworkSubmissionId  (required)
     * @param checkHomeworkSubmissionRequest  (required)
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
        value = HomeworkSubmissionApi.PATH_CHECK_HOMEWORK_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<HomeworkSubmission> checkHomeworkSubmission(
         @PathVariable("homeworkSubmissionId") UUID homeworkSubmissionId,
         @Valid @RequestBody CheckHomeworkSubmissionRequest checkHomeworkSubmissionRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_HOMEWORK_SUBMISSION = "/v1/assessment/homework-submissions";
    /**
     * POST /v1/assessment/homework-submissions : Create HomeworkSubmission
     * Create a HomeworkSubmission. Supports Idempotency-Key and validateOnly.
     *
     * @param homeworkSubmission  (required)
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
        value = HomeworkSubmissionApi.PATH_CREATE_HOMEWORK_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<HomeworkSubmission> createHomeworkSubmission(
         @Valid @RequestBody HomeworkSubmission homeworkSubmission,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_HOMEWORK_SUBMISSION = "/v1/assessment/homework-submissions/{homeworkSubmissionId}";
    /**
     * DELETE /v1/assessment/homework-submissions/{homeworkSubmissionId} : Delete HomeworkSubmission
     * Soft delete (sets deleted_at).
     *
     * @param homeworkSubmissionId HomeworkSubmission id. (required)
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
        value = HomeworkSubmissionApi.PATH_DELETE_HOMEWORK_SUBMISSION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteHomeworkSubmission(
         @PathVariable("homeworkSubmissionId") UUID homeworkSubmissionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_HOMEWORK_SUBMISSION = "/v1/assessment/homework-submissions/{homeworkSubmissionId}";
    /**
     * GET /v1/assessment/homework-submissions/{homeworkSubmissionId} : Get HomeworkSubmission
     * Get one HomeworkSubmission by id. The ETag header carries the row version.
     *
     * @param homeworkSubmissionId HomeworkSubmission id. (required)
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
        value = HomeworkSubmissionApi.PATH_GET_HOMEWORK_SUBMISSION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<HomeworkSubmission> getHomeworkSubmission(
         @PathVariable("homeworkSubmissionId") UUID homeworkSubmissionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_HOMEWORK_SUBMISSIONS = "/v1/assessment/homework-submissions";
    /**
     * GET /v1/assessment/homework-submissions : List HomeworkSubmissions
     * List HomeworkSubmissions visible to the caller, page by page, with filtering and sorting.
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
        value = HomeworkSubmissionApi.PATH_LIST_HOMEWORK_SUBMISSIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<HomeworkSubmission>> listHomeworkSubmissions(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REPLY_TO_HOMEWORK_FEEDBACK = "/v1/assessment/homework-submissions/{homeworkSubmissionId}:reply";
    /**
     * POST /v1/assessment/homework-submissions/{homeworkSubmissionId}:reply : Student replies to comments
     * Student replies to comments.
     *
     * @param homeworkSubmissionId  (required)
     * @param replyToHomeworkFeedbackRequest  (required)
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
        value = HomeworkSubmissionApi.PATH_REPLY_TO_HOMEWORK_FEEDBACK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<HomeworkSubmission> replyToHomeworkFeedback(
         @PathVariable("homeworkSubmissionId") UUID homeworkSubmissionId,
         @Valid @RequestBody ReplyToHomeworkFeedbackRequest replyToHomeworkFeedbackRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_HOMEWORK_SUBMISSION = "/v1/assessment/homework-submissions/{homeworkSubmissionId}";
    /**
     * PATCH /v1/assessment/homework-submissions/{homeworkSubmissionId} : Update HomeworkSubmission
     * Partially update a HomeworkSubmission (updateMask). Requires If-Match with the current ETag.
     *
     * @param homeworkSubmissionId HomeworkSubmission id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param homeworkSubmissionUpdate  (required)
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
        value = HomeworkSubmissionApi.PATH_UPDATE_HOMEWORK_SUBMISSION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<HomeworkSubmission> updateHomeworkSubmission(
         @PathVariable("homeworkSubmissionId") UUID homeworkSubmissionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody HomeworkSubmissionUpdate homeworkSubmissionUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
