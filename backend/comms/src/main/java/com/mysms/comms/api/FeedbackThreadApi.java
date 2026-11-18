// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.CloseFeedbackThreadRequest;
import com.mysms.comms.api.model.FeedbackMessage;
import com.mysms.comms.api.model.FeedbackMessageUpdate;
import com.mysms.comms.api.model.FeedbackThread;
import com.mysms.comms.api.model.FeedbackThreadUpdate;
import com.mysms.comms.api.model.MarkFeedbackMessageRequest;
import org.springframework.lang.Nullable;
import com.mysms.comms.api.model.ReopenFeedbackThreadRequest;
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
public interface FeedbackThreadApi {

    String PATH_CLOSE_FEEDBACK_THREAD = "/v1/comms/feedback-threads/{feedbackThreadId}:close";
    /**
     * POST /v1/comms/feedback-threads/{feedbackThreadId}:close : Close
     * Close.
     *
     * @param feedbackThreadId  (required)
     * @param closeFeedbackThreadRequest  (required)
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
        value = FeedbackThreadApi.PATH_CLOSE_FEEDBACK_THREAD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeedbackThread> closeFeedbackThread(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
         @Valid @RequestBody CloseFeedbackThreadRequest closeFeedbackThreadRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_FEEDBACK_MESSAGE = "/v1/comms/feedback-threads/{feedbackThreadId}/messages";
    /**
     * POST /v1/comms/feedback-threads/{feedbackThreadId}/messages : Create FeedbackMessage
     * Create a FeedbackMessage. Supports Idempotency-Key and validateOnly.
     *
     * @param feedbackThreadId FeedbackThread id. (required)
     * @param feedbackMessage  (required)
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
        value = FeedbackThreadApi.PATH_CREATE_FEEDBACK_MESSAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeedbackMessage> createFeedbackMessage(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
         @Valid @RequestBody FeedbackMessage feedbackMessage,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_FEEDBACK_THREAD = "/v1/comms/feedback-threads";
    /**
     * POST /v1/comms/feedback-threads : Create FeedbackThread
     * Create a FeedbackThread. Supports Idempotency-Key and validateOnly.
     *
     * @param feedbackThread  (required)
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
        value = FeedbackThreadApi.PATH_CREATE_FEEDBACK_THREAD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeedbackThread> createFeedbackThread(
         @Valid @RequestBody FeedbackThread feedbackThread,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_FEEDBACK_MESSAGE = "/v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}";
    /**
     * DELETE /v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId} : Delete FeedbackMessage
     * Soft delete (sets deleted_at).
     *
     * @param feedbackThreadId FeedbackThread id. (required)
     * @param feedbackMessageId FeedbackMessage id. (required)
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
        value = FeedbackThreadApi.PATH_DELETE_FEEDBACK_MESSAGE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteFeedbackMessage(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
         @PathVariable("feedbackMessageId") UUID feedbackMessageId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_FEEDBACK_THREAD = "/v1/comms/feedback-threads/{feedbackThreadId}";
    /**
     * DELETE /v1/comms/feedback-threads/{feedbackThreadId} : Delete FeedbackThread
     * Soft delete (sets deleted_at).
     *
     * @param feedbackThreadId FeedbackThread id. (required)
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
        value = FeedbackThreadApi.PATH_DELETE_FEEDBACK_THREAD,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteFeedbackThread(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_FEEDBACK_MESSAGE = "/v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}";
    /**
     * GET /v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId} : Get FeedbackMessage
     * Get one FeedbackMessage by id. The ETag header carries the row version.
     *
     * @param feedbackThreadId FeedbackThread id. (required)
     * @param feedbackMessageId FeedbackMessage id. (required)
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
        value = FeedbackThreadApi.PATH_GET_FEEDBACK_MESSAGE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<FeedbackMessage> getFeedbackMessage(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
         @PathVariable("feedbackMessageId") UUID feedbackMessageId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_FEEDBACK_THREAD = "/v1/comms/feedback-threads/{feedbackThreadId}";
    /**
     * GET /v1/comms/feedback-threads/{feedbackThreadId} : Get FeedbackThread
     * Get one FeedbackThread by id. The ETag header carries the row version.
     *
     * @param feedbackThreadId FeedbackThread id. (required)
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
        value = FeedbackThreadApi.PATH_GET_FEEDBACK_THREAD,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<FeedbackThread> getFeedbackThread(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FEEDBACK_MESSAGES = "/v1/comms/feedback-threads/{feedbackThreadId}/messages";
    /**
     * GET /v1/comms/feedback-threads/{feedbackThreadId}/messages : List FeedbackMessages
     * List FeedbackMessages visible to the caller, page by page, with filtering and sorting.
     *
     * @param feedbackThreadId FeedbackThread id. (required)
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
        value = FeedbackThreadApi.PATH_LIST_FEEDBACK_MESSAGES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<FeedbackMessage>> listFeedbackMessages(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FEEDBACK_THREADS = "/v1/comms/feedback-threads";
    /**
     * GET /v1/comms/feedback-threads : List FeedbackThreads
     * List FeedbackThreads visible to the caller, page by page, with filtering and sorting.
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
        value = FeedbackThreadApi.PATH_LIST_FEEDBACK_THREADS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<FeedbackThread>> listFeedbackThreads(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_FEEDBACK_MESSAGE = "/v1/comms/feedback-messages/{messageId}:markReviewStatus";
    /**
     * POST /v1/comms/feedback-messages/{messageId}:markReviewStatus : Mark seen / action taken
     * Mark seen / action taken.
     *
     * @param messageId  (required)
     * @param markFeedbackMessageRequest  (required)
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
        value = FeedbackThreadApi.PATH_MARK_FEEDBACK_MESSAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeedbackMessage> markFeedbackMessage(
         @PathVariable("messageId") UUID messageId,
         @Valid @RequestBody MarkFeedbackMessageRequest markFeedbackMessageRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REOPEN_FEEDBACK_THREAD = "/v1/comms/feedback-threads/{feedbackThreadId}:reopen";
    /**
     * POST /v1/comms/feedback-threads/{feedbackThreadId}:reopen : Reopen
     * Reopen.
     *
     * @param feedbackThreadId  (required)
     * @param reopenFeedbackThreadRequest  (required)
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
        value = FeedbackThreadApi.PATH_REOPEN_FEEDBACK_THREAD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeedbackThread> reopenFeedbackThread(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
         @Valid @RequestBody ReopenFeedbackThreadRequest reopenFeedbackThreadRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_FEEDBACK_MESSAGE = "/v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId}";
    /**
     * PATCH /v1/comms/feedback-threads/{feedbackThreadId}/messages/{feedbackMessageId} : Update FeedbackMessage
     * Partially update a FeedbackMessage (updateMask). Requires If-Match with the current ETag.
     *
     * @param feedbackThreadId FeedbackThread id. (required)
     * @param feedbackMessageId FeedbackMessage id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param feedbackMessageUpdate  (required)
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
        value = FeedbackThreadApi.PATH_UPDATE_FEEDBACK_MESSAGE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeedbackMessage> updateFeedbackMessage(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
         @PathVariable("feedbackMessageId") UUID feedbackMessageId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody FeedbackMessageUpdate feedbackMessageUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_FEEDBACK_THREAD = "/v1/comms/feedback-threads/{feedbackThreadId}";
    /**
     * PATCH /v1/comms/feedback-threads/{feedbackThreadId} : Update FeedbackThread
     * Partially update a FeedbackThread (updateMask). Requires If-Match with the current ETag.
     *
     * @param feedbackThreadId FeedbackThread id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param feedbackThreadUpdate  (required)
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
        value = FeedbackThreadApi.PATH_UPDATE_FEEDBACK_THREAD,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<FeedbackThread> updateFeedbackThread(
         @PathVariable("feedbackThreadId") UUID feedbackThreadId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody FeedbackThreadUpdate feedbackThreadUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
