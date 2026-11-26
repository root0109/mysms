// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.ApprovePostRequest;
import com.mysms.comms.api.model.CancelPostRequest;
import org.springframework.lang.Nullable;
import com.mysms.comms.api.model.Post;
import com.mysms.comms.api.model.PostAudienceClass;
import com.mysms.comms.api.model.PostAudienceClassUpdate;
import com.mysms.comms.api.model.PostComment;
import com.mysms.comms.api.model.PostCommentUpdate;
import com.mysms.comms.api.model.PostReaction;
import com.mysms.comms.api.model.PostReactionUpdate;
import com.mysms.comms.api.model.PostUpdate;
import com.mysms.comms.api.model.SubmitPostRequest;
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
public interface PostApi {

    String PATH_APPROVE_POST = "/v1/comms/posts/{postId}:approve";
    /**
     * POST /v1/comms/posts/{postId}:approve : Approve and publish
     * Approve and publish.
     *
     * @param postId  (required)
     * @param approvePostRequest  (required)
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
        value = PostApi.PATH_APPROVE_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Post> approvePost(
         @PathVariable("postId") UUID postId,
         @Valid @RequestBody ApprovePostRequest approvePostRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CANCEL_POST = "/v1/comms/posts/{postId}:cancel";
    /**
     * POST /v1/comms/posts/{postId}:cancel : Withdraw
     * Withdraw.
     *
     * @param postId  (required)
     * @param cancelPostRequest  (required)
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
        value = PostApi.PATH_CANCEL_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Post> cancelPost(
         @PathVariable("postId") UUID postId,
         @Valid @RequestBody CancelPostRequest cancelPostRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_POST = "/v1/comms/posts";
    /**
     * POST /v1/comms/posts : Create Post
     * Create a Post. Supports Idempotency-Key and validateOnly.
     *
     * @param post  (required)
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
        value = PostApi.PATH_CREATE_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Post> createPost(
         @Valid @RequestBody Post post,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_POST_AUDIENCE_CLASS = "/v1/comms/posts/{postId}/audience-classes";
    /**
     * POST /v1/comms/posts/{postId}/audience-classes : Create PostAudienceClass
     * Create a PostAudienceClass. Supports Idempotency-Key and validateOnly.
     *
     * @param postId Post id. (required)
     * @param postAudienceClass  (required)
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
        value = PostApi.PATH_CREATE_POST_AUDIENCE_CLASS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PostAudienceClass> createPostAudienceClass(
         @PathVariable("postId") UUID postId,
         @Valid @RequestBody PostAudienceClass postAudienceClass,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_POST_COMMENT = "/v1/comms/posts/{postId}/comments";
    /**
     * POST /v1/comms/posts/{postId}/comments : Create PostComment
     * Create a PostComment. Supports Idempotency-Key and validateOnly.
     *
     * @param postId Post id. (required)
     * @param postComment  (required)
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
        value = PostApi.PATH_CREATE_POST_COMMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PostComment> createPostComment(
         @PathVariable("postId") UUID postId,
         @Valid @RequestBody PostComment postComment,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_POST_REACTION = "/v1/comms/posts/{postId}/reactions";
    /**
     * POST /v1/comms/posts/{postId}/reactions : Create PostReaction
     * Create a PostReaction. Supports Idempotency-Key and validateOnly.
     *
     * @param postId Post id. (required)
     * @param postReaction  (required)
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
        value = PostApi.PATH_CREATE_POST_REACTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PostReaction> createPostReaction(
         @PathVariable("postId") UUID postId,
         @Valid @RequestBody PostReaction postReaction,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_POST = "/v1/comms/posts/{postId}";
    /**
     * DELETE /v1/comms/posts/{postId} : Delete Post
     * Soft delete (sets deleted_at).
     *
     * @param postId Post id. (required)
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
        value = PostApi.PATH_DELETE_POST,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePost(
         @PathVariable("postId") UUID postId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_POST_AUDIENCE_CLASS = "/v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}";
    /**
     * DELETE /v1/comms/posts/{postId}/audience-classes/{postAudienceClassId} : Delete PostAudienceClass
     * Soft delete (sets deleted_at).
     *
     * @param postId Post id. (required)
     * @param postAudienceClassId PostAudienceClass id. (required)
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
        value = PostApi.PATH_DELETE_POST_AUDIENCE_CLASS,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePostAudienceClass(
         @PathVariable("postId") UUID postId,
         @PathVariable("postAudienceClassId") UUID postAudienceClassId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_POST_COMMENT = "/v1/comms/posts/{postId}/comments/{postCommentId}";
    /**
     * DELETE /v1/comms/posts/{postId}/comments/{postCommentId} : Delete PostComment
     * Soft delete (sets deleted_at).
     *
     * @param postId Post id. (required)
     * @param postCommentId PostComment id. (required)
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
        value = PostApi.PATH_DELETE_POST_COMMENT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePostComment(
         @PathVariable("postId") UUID postId,
         @PathVariable("postCommentId") UUID postCommentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_POST_REACTION = "/v1/comms/posts/{postId}/reactions/{postReactionId}";
    /**
     * DELETE /v1/comms/posts/{postId}/reactions/{postReactionId} : Delete PostReaction
     * Soft delete (sets deleted_at).
     *
     * @param postId Post id. (required)
     * @param postReactionId PostReaction id. (required)
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
        value = PostApi.PATH_DELETE_POST_REACTION,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deletePostReaction(
         @PathVariable("postId") UUID postId,
         @PathVariable("postReactionId") UUID postReactionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_FEED = "/v1/comms/posts:feed";
    /**
     * GET /v1/comms/posts:feed : Personalised feed
     * Personalised feed.
     *
     * @param classSectionId Class wall. (optional)
     * @param classPageId Page. (optional)
     * @param savedOnly Saved posts. (optional)
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
        value = PostApi.PATH_GET_FEED,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Post>> getFeed(
         @Valid @RequestParam(value = "classSectionId", required = false) @Nullable UUID classSectionId,
         @Valid @RequestParam(value = "classPageId", required = false) @Nullable UUID classPageId,
         @Valid @RequestParam(value = "savedOnly", required = false) @Nullable Boolean savedOnly,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor
    );


    String PATH_GET_POST = "/v1/comms/posts/{postId}";
    /**
     * GET /v1/comms/posts/{postId} : Get Post
     * Get one Post by id. The ETag header carries the row version.
     *
     * @param postId Post id. (required)
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
        value = PostApi.PATH_GET_POST,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Post> getPost(
         @PathVariable("postId") UUID postId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_POST_AUDIENCE_CLASS = "/v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}";
    /**
     * GET /v1/comms/posts/{postId}/audience-classes/{postAudienceClassId} : Get PostAudienceClass
     * Get one PostAudienceClass by id. The ETag header carries the row version.
     *
     * @param postId Post id. (required)
     * @param postAudienceClassId PostAudienceClass id. (required)
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
        value = PostApi.PATH_GET_POST_AUDIENCE_CLASS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PostAudienceClass> getPostAudienceClass(
         @PathVariable("postId") UUID postId,
         @PathVariable("postAudienceClassId") UUID postAudienceClassId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_POST_COMMENT = "/v1/comms/posts/{postId}/comments/{postCommentId}";
    /**
     * GET /v1/comms/posts/{postId}/comments/{postCommentId} : Get PostComment
     * Get one PostComment by id. The ETag header carries the row version.
     *
     * @param postId Post id. (required)
     * @param postCommentId PostComment id. (required)
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
        value = PostApi.PATH_GET_POST_COMMENT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PostComment> getPostComment(
         @PathVariable("postId") UUID postId,
         @PathVariable("postCommentId") UUID postCommentId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_POST_REACTION = "/v1/comms/posts/{postId}/reactions/{postReactionId}";
    /**
     * GET /v1/comms/posts/{postId}/reactions/{postReactionId} : Get PostReaction
     * Get one PostReaction by id. The ETag header carries the row version.
     *
     * @param postId Post id. (required)
     * @param postReactionId PostReaction id. (required)
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
        value = PostApi.PATH_GET_POST_REACTION,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PostReaction> getPostReaction(
         @PathVariable("postId") UUID postId,
         @PathVariable("postReactionId") UUID postReactionId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_POST_AUDIENCE_CLASSES = "/v1/comms/posts/{postId}/audience-classes";
    /**
     * GET /v1/comms/posts/{postId}/audience-classes : List PostAudienceClasses
     * List PostAudienceClasses visible to the caller, page by page, with filtering and sorting.
     *
     * @param postId Post id. (required)
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
        value = PostApi.PATH_LIST_POST_AUDIENCE_CLASSES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PostAudienceClass>> listPostAudienceClasses(
         @PathVariable("postId") UUID postId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_POST_COMMENTS = "/v1/comms/posts/{postId}/comments";
    /**
     * GET /v1/comms/posts/{postId}/comments : List PostComments
     * List PostComments visible to the caller, page by page, with filtering and sorting.
     *
     * @param postId Post id. (required)
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
        value = PostApi.PATH_LIST_POST_COMMENTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PostComment>> listPostComments(
         @PathVariable("postId") UUID postId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_POST_REACTIONS = "/v1/comms/posts/{postId}/reactions";
    /**
     * GET /v1/comms/posts/{postId}/reactions : List PostReactions
     * List PostReactions visible to the caller, page by page, with filtering and sorting.
     *
     * @param postId Post id. (required)
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
        value = PostApi.PATH_LIST_POST_REACTIONS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<PostReaction>> listPostReactions(
         @PathVariable("postId") UUID postId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_POSTS = "/v1/comms/posts";
    /**
     * GET /v1/comms/posts : List Posts
     * List Posts visible to the caller, page by page, with filtering and sorting.
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
        value = PostApi.PATH_LIST_POSTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Post>> listPosts(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_SUBMIT_POST = "/v1/comms/posts/{postId}:submit";
    /**
     * POST /v1/comms/posts/{postId}:submit : Submit for moderation / publish
     * Submit for moderation / publish.
     *
     * @param postId  (required)
     * @param submitPostRequest  (required)
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
        value = PostApi.PATH_SUBMIT_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Post> submitPost(
         @PathVariable("postId") UUID postId,
         @Valid @RequestBody SubmitPostRequest submitPostRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_POST = "/v1/comms/posts/{postId}";
    /**
     * PATCH /v1/comms/posts/{postId} : Update Post
     * Partially update a Post (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param postId Post id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param postUpdate  (required)
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
        value = PostApi.PATH_UPDATE_POST,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Post> updatePost(
         @PathVariable("postId") UUID postId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PostUpdate postUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_POST_AUDIENCE_CLASS = "/v1/comms/posts/{postId}/audience-classes/{postAudienceClassId}";
    /**
     * PATCH /v1/comms/posts/{postId}/audience-classes/{postAudienceClassId} : Update PostAudienceClass
     * Partially update a PostAudienceClass (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param postId Post id. (required)
     * @param postAudienceClassId PostAudienceClass id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param postAudienceClassUpdate  (required)
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
        value = PostApi.PATH_UPDATE_POST_AUDIENCE_CLASS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PostAudienceClass> updatePostAudienceClass(
         @PathVariable("postId") UUID postId,
         @PathVariable("postAudienceClassId") UUID postAudienceClassId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PostAudienceClassUpdate postAudienceClassUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_POST_COMMENT = "/v1/comms/posts/{postId}/comments/{postCommentId}";
    /**
     * PATCH /v1/comms/posts/{postId}/comments/{postCommentId} : Update PostComment
     * Partially update a PostComment (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param postId Post id. (required)
     * @param postCommentId PostComment id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param postCommentUpdate  (required)
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
        value = PostApi.PATH_UPDATE_POST_COMMENT,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PostComment> updatePostComment(
         @PathVariable("postId") UUID postId,
         @PathVariable("postCommentId") UUID postCommentId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PostCommentUpdate postCommentUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_POST_REACTION = "/v1/comms/posts/{postId}/reactions/{postReactionId}";
    /**
     * PATCH /v1/comms/posts/{postId}/reactions/{postReactionId} : Update PostReaction
     * Partially update a PostReaction (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param postId Post id. (required)
     * @param postReactionId PostReaction id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param postReactionUpdate  (required)
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
        value = PostApi.PATH_UPDATE_POST_REACTION,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PostReaction> updatePostReaction(
         @PathVariable("postId") UUID postId,
         @PathVariable("postReactionId") UUID postReactionId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody PostReactionUpdate postReactionUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
