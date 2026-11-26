// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.learning.lms.api.model.ForumReadState;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.model.ConfirmPostAbuseRequest;
import com.mysms.learning.lms.api.model.FlagForumPostRequest;
import com.mysms.learning.lms.api.model.ForumPost;
import com.mysms.learning.lms.api.model.ForumPostUpdate;
import com.mysms.learning.lms.api.model.ForumTopic;
import com.mysms.learning.lms.api.model.ForumTopicUpdate;
import com.mysms.learning.lms.api.model.HideForumPostRequest;
import com.mysms.learning.lms.api.model.HideForumTopicRequest;
import com.mysms.learning.lms.api.model.MarkPostSubstantiveRequest;
import com.mysms.learning.lms.api.model.MarkTopicReadRequest;
import org.springframework.lang.Nullable;
import java.util.UUID;
import com.mysms.learning.lms.api.model.UnhideForumPostRequest;
import com.mysms.learning.lms.api.model.UnhideForumTopicRequest;
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
public interface ForumTopicApi {

    String PATH_CONFIRM_POST_ABUSE = "/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:confirmAbuse";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:confirmAbuse : Confirm reports and hide the post
     * Confirm reports and hide the post.
     *
     * @param forumTopicId  (required)
     * @param forumPostId  (required)
     * @param confirmPostAbuseRequest  (required)
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
        value = ForumTopicApi.PATH_CONFIRM_POST_ABUSE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumPost> confirmPostAbuse(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @PathVariable("forumPostId") UUID forumPostId,
         @Valid @RequestBody ConfirmPostAbuseRequest confirmPostAbuseRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_FORUM_POST = "/v1/lms/forum-topics/{forumTopicId}/posts";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}/posts : Create ForumPost
     * Create a ForumPost. Supports Idempotency-Key and validateOnly.
     *
     * @param forumTopicId ForumTopic id. (required)
     * @param forumPost  (required)
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
        value = ForumTopicApi.PATH_CREATE_FORUM_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumPost> createForumPost(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @Valid @RequestBody ForumPost forumPost,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_FORUM_TOPIC = "/v1/lms/forum-topics";
    /**
     * POST /v1/lms/forum-topics : Create ForumTopic
     * Create a ForumTopic. Supports Idempotency-Key and validateOnly.
     *
     * @param forumTopic  (required)
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
        value = ForumTopicApi.PATH_CREATE_FORUM_TOPIC,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumTopic> createForumTopic(
         @Valid @RequestBody ForumTopic forumTopic,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_FORUM_POST = "/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}";
    /**
     * DELETE /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId} : Delete ForumPost
     * Soft delete (sets deleted_at).
     *
     * @param forumTopicId ForumTopic id. (required)
     * @param forumPostId ForumPost id. (required)
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
        value = ForumTopicApi.PATH_DELETE_FORUM_POST,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteForumPost(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @PathVariable("forumPostId") UUID forumPostId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_FORUM_TOPIC = "/v1/lms/forum-topics/{forumTopicId}";
    /**
     * DELETE /v1/lms/forum-topics/{forumTopicId} : Delete ForumTopic
     * Soft delete (sets deleted_at).
     *
     * @param forumTopicId ForumTopic id. (required)
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
        value = ForumTopicApi.PATH_DELETE_FORUM_TOPIC,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteForumTopic(
         @PathVariable("forumTopicId") UUID forumTopicId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_FLAG_FORUM_POST = "/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:flag";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:flag : Flag a post for yourself
     * Flag a post for yourself.
     *
     * @param forumTopicId  (required)
     * @param forumPostId  (required)
     * @param flagForumPostRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = ForumTopicApi.PATH_FLAG_FORUM_POST,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> flagForumPost(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @PathVariable("forumPostId") UUID forumPostId,
         @Valid @RequestBody FlagForumPostRequest flagForumPostRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_FORUM_POST = "/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}";
    /**
     * GET /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId} : Get ForumPost
     * Get one ForumPost by id. The ETag header carries the row version.
     *
     * @param forumTopicId ForumTopic id. (required)
     * @param forumPostId ForumPost id. (required)
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
        value = ForumTopicApi.PATH_GET_FORUM_POST,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ForumPost> getForumPost(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @PathVariable("forumPostId") UUID forumPostId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_FORUM_TOPIC = "/v1/lms/forum-topics/{forumTopicId}";
    /**
     * GET /v1/lms/forum-topics/{forumTopicId} : Get ForumTopic
     * Get one ForumTopic by id. The ETag header carries the row version.
     *
     * @param forumTopicId ForumTopic id. (required)
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
        value = ForumTopicApi.PATH_GET_FORUM_TOPIC,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ForumTopic> getForumTopic(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_HIDE_FORUM_POST = "/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:hide";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:hide : Hide a post (and its replies)
     * Hide a post (and its replies).
     *
     * @param forumTopicId  (required)
     * @param forumPostId  (required)
     * @param hideForumPostRequest  (required)
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
        value = ForumTopicApi.PATH_HIDE_FORUM_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumPost> hideForumPost(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @PathVariable("forumPostId") UUID forumPostId,
         @Valid @RequestBody HideForumPostRequest hideForumPostRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_HIDE_FORUM_TOPIC = "/v1/lms/forum-topics/{forumTopicId}:hide";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}:hide : Hide from students
     * Hide from students.
     *
     * @param forumTopicId  (required)
     * @param hideForumTopicRequest  (required)
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
        value = ForumTopicApi.PATH_HIDE_FORUM_TOPIC,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumTopic> hideForumTopic(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @Valid @RequestBody HideForumTopicRequest hideForumTopicRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_FORUM_POSTS = "/v1/lms/forum-topics/{forumTopicId}/posts";
    /**
     * GET /v1/lms/forum-topics/{forumTopicId}/posts : List ForumPosts
     * List ForumPosts visible to the caller, page by page, with filtering and sorting.
     *
     * @param forumTopicId ForumTopic id. (required)
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
        value = ForumTopicApi.PATH_LIST_FORUM_POSTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ForumPost>> listForumPosts(
         @PathVariable("forumTopicId") UUID forumTopicId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FORUM_READ_STATES = "/v1/lms/forum-topics/{forumTopicId}/read-states";
    /**
     * GET /v1/lms/forum-topics/{forumTopicId}/read-states : List ForumReadStates
     * List ForumReadStates visible to the caller, page by page, with filtering and sorting.
     *
     * @param forumTopicId ForumTopic id. (required)
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
        value = ForumTopicApi.PATH_LIST_FORUM_READ_STATES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ForumReadState>> listForumReadStates(
         @PathVariable("forumTopicId") UUID forumTopicId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_FORUM_TOPICS = "/v1/lms/forum-topics";
    /**
     * GET /v1/lms/forum-topics : List ForumTopics
     * List ForumTopics visible to the caller, page by page, with filtering and sorting.
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
        value = ForumTopicApi.PATH_LIST_FORUM_TOPICS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ForumTopic>> listForumTopics(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MARK_POST_SUBSTANTIVE = "/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:markSubstantive";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:markSubstantive : Teacher marks a post substantive (counts for discussion grading)
     * Teacher marks a post substantive (counts for discussion grading).
     *
     * @param forumTopicId  (required)
     * @param forumPostId  (required)
     * @param markPostSubstantiveRequest  (required)
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
        value = ForumTopicApi.PATH_MARK_POST_SUBSTANTIVE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumPost> markPostSubstantive(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @PathVariable("forumPostId") UUID forumPostId,
         @Valid @RequestBody MarkPostSubstantiveRequest markPostSubstantiveRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_MARK_TOPIC_READ = "/v1/lms/forum-topics/{forumTopicId}:markRead";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}:markRead : Record read state for unread counts
     * Record read state for unread counts.
     *
     * @param forumTopicId  (required)
     * @param markTopicReadRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return Done (status code 204)
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
        value = ForumTopicApi.PATH_MARK_TOPIC_READ,
        produces = { "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Void> markTopicRead(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @Valid @RequestBody MarkTopicReadRequest markTopicReadRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UNHIDE_FORUM_POST = "/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:unhide";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}:unhide : Show again
     * Show again.
     *
     * @param forumTopicId  (required)
     * @param forumPostId  (required)
     * @param unhideForumPostRequest  (required)
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
        value = ForumTopicApi.PATH_UNHIDE_FORUM_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumPost> unhideForumPost(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @PathVariable("forumPostId") UUID forumPostId,
         @Valid @RequestBody UnhideForumPostRequest unhideForumPostRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UNHIDE_FORUM_TOPIC = "/v1/lms/forum-topics/{forumTopicId}:unhide";
    /**
     * POST /v1/lms/forum-topics/{forumTopicId}:unhide : Show again
     * Show again.
     *
     * @param forumTopicId  (required)
     * @param unhideForumTopicRequest  (required)
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
        value = ForumTopicApi.PATH_UNHIDE_FORUM_TOPIC,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumTopic> unhideForumTopic(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @Valid @RequestBody UnhideForumTopicRequest unhideForumTopicRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_FORUM_POST = "/v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId}";
    /**
     * PATCH /v1/lms/forum-topics/{forumTopicId}/posts/{forumPostId} : Update ForumPost
     * Partially update a ForumPost (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param forumTopicId ForumTopic id. (required)
     * @param forumPostId ForumPost id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param forumPostUpdate  (required)
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
        value = ForumTopicApi.PATH_UPDATE_FORUM_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumPost> updateForumPost(
         @PathVariable("forumTopicId") UUID forumTopicId,
         @PathVariable("forumPostId") UUID forumPostId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ForumPostUpdate forumPostUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_FORUM_TOPIC = "/v1/lms/forum-topics/{forumTopicId}";
    /**
     * PATCH /v1/lms/forum-topics/{forumTopicId} : Update ForumTopic
     * Partially update a ForumTopic (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param forumTopicId ForumTopic id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param forumTopicUpdate  (required)
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
        value = ForumTopicApi.PATH_UPDATE_FORUM_TOPIC,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ForumTopic> updateForumTopic(
         @PathVariable("forumTopicId") UUID forumTopicId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ForumTopicUpdate forumTopicUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
