// Owned API contract of lms.
package com.mysms.learning.lms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.model.AddFromRepositoryRequest;
import com.mysms.learning.lms.api.model.AssignmentGroup;
import com.mysms.learning.lms.api.model.AssignmentGroupMember;
import com.mysms.learning.lms.api.model.AssignmentGroupMemberList;
import com.mysms.learning.lms.api.model.AssignmentGroupUpdate;
import com.mysms.learning.lms.api.model.ContentItem;
import com.mysms.learning.lms.api.model.ContentItemList;
import com.mysms.learning.lms.api.model.ContentItemRating;
import com.mysms.learning.lms.api.model.ContentItemRatingUpdate;
import com.mysms.learning.lms.api.model.ContentItemUpdate;
import com.mysms.learning.lms.api.model.CopyContentItemRequest;
import com.mysms.learning.lms.api.model.ExternalLink;
import com.mysms.learning.lms.api.model.FinalizePeerReviewRequest;
import com.mysms.learning.lms.api.model.GradebookEntryList;
import com.mysms.learning.lms.api.model.GrantItemAccessRequest;
import com.mysms.learning.lms.api.model.ItemOverride;
import com.mysms.learning.lms.api.model.ItemOverrideUpdate;
import com.mysms.learning.lms.api.model.MoveContentItemRequest;
import org.springframework.lang.Nullable;
import com.mysms.learning.lms.api.model.Operation;
import com.mysms.learning.lms.api.model.PeerReviewList;
import com.mysms.learning.lms.api.model.PublishItemGradesRequest;
import com.mysms.learning.lms.api.model.ReassignItemRequest;
import com.mysms.learning.lms.api.model.ReassignmentLogList;
import com.mysms.learning.lms.api.model.RecordItemViewRequest;
import com.mysms.learning.lms.api.model.ReleaseGrant;
import com.mysms.learning.lms.api.model.ReleaseGrantList;
import com.mysms.learning.lms.api.model.ReleaseRuleList;
import com.mysms.learning.lms.api.model.ReleaseStatus;
import com.mysms.learning.lms.api.model.ReorderReleaseRulesRequest;
import com.mysms.learning.lms.api.model.SetContentItemVisibilityRequest;
import com.mysms.learning.lms.api.model.SetGroupMembersRequest;
import com.mysms.learning.lms.api.model.SetReleaseRulesEnabledRequest;
import com.mysms.learning.lms.api.model.SubmitPeerReviewRequest;
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
public interface ContentItemApi {

    String PATH_ADD_FROM_REPOSITORY = "/v1/lms/content-items:addFromRepository";
    /**
     * POST /v1/lms/content-items:addFromRepository : Place shared repository items in a chapter
     * Place shared repository items in a chapter.
     *
     * @param addFromRepositoryRequest  (required)
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
        value = ContentItemApi.PATH_ADD_FROM_REPOSITORY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItemList> addFromRepository(
         @Valid @RequestBody AddFromRepositoryRequest addFromRepositoryRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_COPY_CONTENT_ITEM = "/v1/lms/content-items/{contentItemId}:copy";
    /**
     * POST /v1/lms/content-items/{contentItemId}:copy : Duplicate an item (and its underlying assignment / quiz / resource when it is not shared)
     * Duplicate an item (and its underlying assignment / quiz / resource when it is not shared).
     *
     * @param contentItemId  (required)
     * @param copyContentItemRequest  (required)
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
        value = ContentItemApi.PATH_COPY_CONTENT_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItem> copyContentItem(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody CopyContentItemRequest copyContentItemRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_ASSIGNMENT_GROUP = "/v1/lms/content-items/{contentItemId}/groups";
    /**
     * POST /v1/lms/content-items/{contentItemId}/groups : Create AssignmentGroup
     * Create a AssignmentGroup. Supports Idempotency-Key and validateOnly.
     *
     * @param contentItemId ContentItem id. (required)
     * @param assignmentGroup  (required)
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
        value = ContentItemApi.PATH_CREATE_ASSIGNMENT_GROUP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentGroup> createAssignmentGroup(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody AssignmentGroup assignmentGroup,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CONTENT_ITEM = "/v1/lms/content-items";
    /**
     * POST /v1/lms/content-items : Create ContentItem
     * Create a ContentItem. Supports Idempotency-Key and validateOnly.
     *
     * @param contentItem  (required)
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
        value = ContentItemApi.PATH_CREATE_CONTENT_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItem> createContentItem(
         @Valid @RequestBody ContentItem contentItem,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_CONTENT_ITEM_RATING = "/v1/lms/content-items/{contentItemId}/ratings";
    /**
     * POST /v1/lms/content-items/{contentItemId}/ratings : Create ContentItemRating
     * Create a ContentItemRating. Supports Idempotency-Key and validateOnly.
     *
     * @param contentItemId ContentItem id. (required)
     * @param contentItemRating  (required)
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
        value = ContentItemApi.PATH_CREATE_CONTENT_ITEM_RATING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItemRating> createContentItemRating(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody ContentItemRating contentItemRating,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_ITEM_OVERRIDE = "/v1/lms/content-items/{contentItemId}/overrides";
    /**
     * POST /v1/lms/content-items/{contentItemId}/overrides : Create ItemOverride
     * Create a ItemOverride. Supports Idempotency-Key and validateOnly.
     *
     * @param contentItemId ContentItem id. (required)
     * @param itemOverride  (required)
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
        value = ContentItemApi.PATH_CREATE_ITEM_OVERRIDE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ItemOverride> createItemOverride(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody ItemOverride itemOverride,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_ASSIGNMENT_GROUP = "/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}";
    /**
     * DELETE /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId} : Delete AssignmentGroup
     * Soft delete (sets deleted_at).
     *
     * @param contentItemId ContentItem id. (required)
     * @param assignmentGroupId AssignmentGroup id. (required)
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
        value = ContentItemApi.PATH_DELETE_ASSIGNMENT_GROUP,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteAssignmentGroup(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("assignmentGroupId") UUID assignmentGroupId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CONTENT_ITEM = "/v1/lms/content-items/{contentItemId}";
    /**
     * DELETE /v1/lms/content-items/{contentItemId} : Delete ContentItem
     * Soft delete (sets deleted_at).
     *
     * @param contentItemId ContentItem id. (required)
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
        value = ContentItemApi.PATH_DELETE_CONTENT_ITEM,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteContentItem(
         @PathVariable("contentItemId") UUID contentItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_CONTENT_ITEM_RATING = "/v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}";
    /**
     * DELETE /v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId} : Delete ContentItemRating
     * Soft delete (sets deleted_at).
     *
     * @param contentItemId ContentItem id. (required)
     * @param contentItemRatingId ContentItemRating id. (required)
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
        value = ContentItemApi.PATH_DELETE_CONTENT_ITEM_RATING,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteContentItemRating(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("contentItemRatingId") UUID contentItemRatingId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_ITEM_OVERRIDE = "/v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}";
    /**
     * DELETE /v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId} : Delete ItemOverride
     * Soft delete (sets deleted_at).
     *
     * @param contentItemId ContentItem id. (required)
     * @param itemOverrideId ItemOverride id. (required)
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
        value = ContentItemApi.PATH_DELETE_ITEM_OVERRIDE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteItemOverride(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("itemOverrideId") UUID itemOverrideId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_RELEASE_GRANT = "/v1/lms/content-items/{contentItemId}/release-grants/{releaseGrantId}";
    /**
     * DELETE /v1/lms/content-items/{contentItemId}/release-grants/{releaseGrantId} : Delete ReleaseGrant
     * Soft delete (sets deleted_at).
     *
     * @param contentItemId ContentItem id. (required)
     * @param releaseGrantId ReleaseGrant id. (required)
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
        value = ContentItemApi.PATH_DELETE_RELEASE_GRANT,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteReleaseGrant(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("releaseGrantId") UUID releaseGrantId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DOWNLOAD_SUBMISSIONS = "/v1/lms/content-items/{contentItemId}:downloadSubmissions";
    /**
     * POST /v1/lms/content-items/{contentItemId}:downloadSubmissions : Zip of all submitted files of an assignment
     * Zip of all submitted files of an assignment.
     *
     * @param contentItemId  (required)
     * @param body  (required)
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
        value = ContentItemApi.PATH_DOWNLOAD_SUBMISSIONS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Operation> downloadSubmissions(
         @PathVariable("contentItemId") UUID contentItemId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_FINALIZE_PEER_REVIEW = "/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:finalizePeerReview";
    /**
     * POST /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:finalizePeerReview : Teacher finalises peer points into the members&#39; gradebook entries (peerPoints)
     * Teacher finalises peer points into the members&#39; gradebook entries (peerPoints).
     *
     * @param contentItemId  (required)
     * @param assignmentGroupId  (required)
     * @param finalizePeerReviewRequest  (required)
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
        value = ContentItemApi.PATH_FINALIZE_PEER_REVIEW,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<GradebookEntryList> finalizePeerReview(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("assignmentGroupId") UUID assignmentGroupId,
         @Valid @RequestBody FinalizePeerReviewRequest finalizePeerReviewRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_GET_ASSIGNMENT_GROUP = "/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}";
    /**
     * GET /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId} : Get AssignmentGroup
     * Get one AssignmentGroup by id. The ETag header carries the row version.
     *
     * @param contentItemId ContentItem id. (required)
     * @param assignmentGroupId AssignmentGroup id. (required)
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
        value = ContentItemApi.PATH_GET_ASSIGNMENT_GROUP,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<AssignmentGroup> getAssignmentGroup(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("assignmentGroupId") UUID assignmentGroupId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CONTENT_ITEM = "/v1/lms/content-items/{contentItemId}";
    /**
     * GET /v1/lms/content-items/{contentItemId} : Get ContentItem
     * Get one ContentItem by id. The ETag header carries the row version.
     *
     * @param contentItemId ContentItem id. (required)
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
        value = ContentItemApi.PATH_GET_CONTENT_ITEM,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ContentItem> getContentItem(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_CONTENT_ITEM_RATING = "/v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}";
    /**
     * GET /v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId} : Get ContentItemRating
     * Get one ContentItemRating by id. The ETag header carries the row version.
     *
     * @param contentItemId ContentItem id. (required)
     * @param contentItemRatingId ContentItemRating id. (required)
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
        value = ContentItemApi.PATH_GET_CONTENT_ITEM_RATING,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ContentItemRating> getContentItemRating(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("contentItemRatingId") UUID contentItemRatingId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_ITEM_OVERRIDE = "/v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}";
    /**
     * GET /v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId} : Get ItemOverride
     * Get one ItemOverride by id. The ETag header carries the row version.
     *
     * @param contentItemId ContentItem id. (required)
     * @param itemOverrideId ItemOverride id. (required)
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
        value = ContentItemApi.PATH_GET_ITEM_OVERRIDE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ItemOverride> getItemOverride(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("itemOverrideId") UUID itemOverrideId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RELEASE_GRANT = "/v1/lms/content-items/{contentItemId}/release-grants/{releaseGrantId}";
    /**
     * GET /v1/lms/content-items/{contentItemId}/release-grants/{releaseGrantId} : Get ReleaseGrant
     * Get one ReleaseGrant by id. The ETag header carries the row version.
     *
     * @param contentItemId ContentItem id. (required)
     * @param releaseGrantId ReleaseGrant id. (required)
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
        value = ContentItemApi.PATH_GET_RELEASE_GRANT,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ReleaseGrant> getReleaseGrant(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("releaseGrantId") UUID releaseGrantId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_RELEASE_STATUS = "/v1/lms/content-items/{contentItemId}:releaseStatus";
    /**
     * GET /v1/lms/content-items/{contentItemId}:releaseStatus : Whether the item is released to a student and which conditions are pending
     * Whether the item is released to a student and which conditions are pending.
     *
     * @param contentItemId  (required)
     * @param userId Student (default caller). (optional)
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
        value = ContentItemApi.PATH_GET_RELEASE_STATUS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ReleaseStatus> getReleaseStatus(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestParam(value = "userId", required = false) @Nullable UUID userId
    );


    String PATH_GET_WEB_RESOURCE_LAUNCH = "/v1/lms/content-items/{contentItemId}:launch";
    /**
     * GET /v1/lms/content-items/{contentItemId}:launch : Launch URL of a web resource, e-book or file item (signed, short-lived)
     * Launch URL of a web resource, e-book or file item (signed, short-lived).
     *
     * @param contentItemId  (required)
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
        value = ContentItemApi.PATH_GET_WEB_RESOURCE_LAUNCH,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<ExternalLink> getWebResourceLaunch(
         @PathVariable("contentItemId") UUID contentItemId
    );


    String PATH_GRANT_ITEM_ACCESS = "/v1/lms/content-items/{contentItemId}:grantAccess";
    /**
     * POST /v1/lms/content-items/{contentItemId}:grantAccess : Manually release an item to students regardless of rules
     * Manually release an item to students regardless of rules.
     *
     * @param contentItemId  (required)
     * @param grantItemAccessRequest  (required)
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
        value = ContentItemApi.PATH_GRANT_ITEM_ACCESS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseGrantList> grantItemAccess(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody GrantItemAccessRequest grantItemAccessRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_LIST_ASSIGNMENT_GROUPS = "/v1/lms/content-items/{contentItemId}/groups";
    /**
     * GET /v1/lms/content-items/{contentItemId}/groups : List AssignmentGroups
     * List AssignmentGroups visible to the caller, page by page, with filtering and sorting.
     *
     * @param contentItemId ContentItem id. (required)
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
        value = ContentItemApi.PATH_LIST_ASSIGNMENT_GROUPS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<AssignmentGroup>> listAssignmentGroups(
         @PathVariable("contentItemId") UUID contentItemId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CONTENT_ITEM_RATINGS = "/v1/lms/content-items/{contentItemId}/ratings";
    /**
     * GET /v1/lms/content-items/{contentItemId}/ratings : List ContentItemRatings
     * List ContentItemRatings visible to the caller, page by page, with filtering and sorting.
     *
     * @param contentItemId ContentItem id. (required)
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
        value = ContentItemApi.PATH_LIST_CONTENT_ITEM_RATINGS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ContentItemRating>> listContentItemRatings(
         @PathVariable("contentItemId") UUID contentItemId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_CONTENT_ITEMS = "/v1/lms/content-items";
    /**
     * GET /v1/lms/content-items : List ContentItems
     * List ContentItems visible to the caller, page by page, with filtering and sorting.
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
        value = ContentItemApi.PATH_LIST_CONTENT_ITEMS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ContentItem>> listContentItems(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_ITEM_OVERRIDES = "/v1/lms/content-items/{contentItemId}/overrides";
    /**
     * GET /v1/lms/content-items/{contentItemId}/overrides : List ItemOverrides
     * List ItemOverrides visible to the caller, page by page, with filtering and sorting.
     *
     * @param contentItemId ContentItem id. (required)
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
        value = ContentItemApi.PATH_LIST_ITEM_OVERRIDES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ItemOverride>> listItemOverrides(
         @PathVariable("contentItemId") UUID contentItemId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_RELEASE_GRANTS = "/v1/lms/content-items/{contentItemId}/release-grants";
    /**
     * GET /v1/lms/content-items/{contentItemId}/release-grants : List ReleaseGrants
     * List ReleaseGrants visible to the caller, page by page, with filtering and sorting.
     *
     * @param contentItemId ContentItem id. (required)
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
        value = ContentItemApi.PATH_LIST_RELEASE_GRANTS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<ReleaseGrant>> listReleaseGrants(
         @PathVariable("contentItemId") UUID contentItemId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_MOVE_CONTENT_ITEM = "/v1/lms/content-items/{contentItemId}:move";
    /**
     * POST /v1/lms/content-items/{contentItemId}:move : Move / reorder an item
     * Move / reorder an item.
     *
     * @param contentItemId  (required)
     * @param moveContentItemRequest  (required)
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
        value = ContentItemApi.PATH_MOVE_CONTENT_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItem> moveContentItem(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody MoveContentItemRequest moveContentItemRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_PUBLISH_ITEM_GRADES = "/v1/lms/content-items/{contentItemId}:publishGrades";
    /**
     * POST /v1/lms/content-items/{contentItemId}:publishGrades : Publish grades of an item to students
     * Publish grades of an item to students.
     *
     * @param contentItemId  (required)
     * @param publishItemGradesRequest  (required)
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
        value = ContentItemApi.PATH_PUBLISH_ITEM_GRADES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<GradebookEntryList> publishItemGrades(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody PublishItemGradesRequest publishItemGradesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REASSIGN_ITEM = "/v1/lms/content-items/{contentItemId}:reassign";
    /**
     * POST /v1/lms/content-items/{contentItemId}:reassign : Give students / groups a fresh attempt (keeps history in the reassignment log)
     * Give students / groups a fresh attempt (keeps history in the reassignment log).
     *
     * @param contentItemId  (required)
     * @param reassignItemRequest  (required)
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
        value = ContentItemApi.PATH_REASSIGN_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReassignmentLogList> reassignItem(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody ReassignItemRequest reassignItemRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_RECORD_ITEM_VIEW = "/v1/lms/content-items/{contentItemId}:recordView";
    /**
     * POST /v1/lms/content-items/{contentItemId}:recordView : Count a view (and optionally a like) of an item
     * Count a view (and optionally a like) of an item.
     *
     * @param contentItemId  (required)
     * @param recordItemViewRequest  (required)
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
        value = ContentItemApi.PATH_RECORD_ITEM_VIEW,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItem> recordItemView(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody RecordItemViewRequest recordItemViewRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REORDER_RELEASE_RULES = "/v1/lms/content-items/{contentItemId}:reorderReleaseRules";
    /**
     * POST /v1/lms/content-items/{contentItemId}:reorderReleaseRules : Change rule evaluation order
     * Change rule evaluation order.
     *
     * @param contentItemId  (required)
     * @param reorderReleaseRulesRequest  (required)
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
        value = ContentItemApi.PATH_REORDER_RELEASE_RULES,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRuleList> reorderReleaseRules(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody ReorderReleaseRulesRequest reorderReleaseRulesRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_REQUEST_TO_JOIN_GROUP = "/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:requestToJoin";
    /**
     * POST /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:requestToJoin : Student asks to join a group (status requested)
     * Student asks to join a group (status requested).
     *
     * @param contentItemId  (required)
     * @param assignmentGroupId  (required)
     * @param body  (required)
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
        value = ContentItemApi.PATH_REQUEST_TO_JOIN_GROUP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentGroupMember> requestToJoinGroup(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("assignmentGroupId") UUID assignmentGroupId,
         @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SET_CONTENT_ITEM_VISIBILITY = "/v1/lms/content-items/{contentItemId}:setVisibility";
    /**
     * POST /v1/lms/content-items/{contentItemId}:setVisibility : Show or hide a cohort item
     * Show or hide a cohort item.
     *
     * @param contentItemId  (required)
     * @param setContentItemVisibilityRequest  (required)
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
        value = ContentItemApi.PATH_SET_CONTENT_ITEM_VISIBILITY,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItem> setContentItemVisibility(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody SetContentItemVisibilityRequest setContentItemVisibilityRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SET_GROUP_MEMBERS = "/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:setMembers";
    /**
     * POST /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:setMembers : Add / remove members (teacher)
     * Add / remove members (teacher).
     *
     * @param contentItemId  (required)
     * @param assignmentGroupId  (required)
     * @param setGroupMembersRequest  (required)
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
        value = ContentItemApi.PATH_SET_GROUP_MEMBERS,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentGroupMemberList> setGroupMembers(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("assignmentGroupId") UUID assignmentGroupId,
         @Valid @RequestBody SetGroupMembersRequest setGroupMembersRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SET_RELEASE_RULES_ENABLED = "/v1/lms/content-items/{contentItemId}:setReleaseRulesEnabled";
    /**
     * POST /v1/lms/content-items/{contentItemId}:setReleaseRulesEnabled : Enable / disable all release rules whose source is this item
     * Enable / disable all release rules whose source is this item.
     *
     * @param contentItemId  (required)
     * @param setReleaseRulesEnabledRequest  (required)
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
        value = ContentItemApi.PATH_SET_RELEASE_RULES_ENABLED,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ReleaseRuleList> setReleaseRulesEnabled(
         @PathVariable("contentItemId") UUID contentItemId,
         @Valid @RequestBody SetReleaseRulesEnabledRequest setReleaseRulesEnabledRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SUBMIT_PEER_REVIEW = "/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:submitPeerReview";
    /**
     * POST /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}:submitPeerReview : Student records peer-review points for the other members of their group
     * Student records peer-review points for the other members of their group.
     *
     * @param contentItemId  (required)
     * @param assignmentGroupId  (required)
     * @param submitPeerReviewRequest  (required)
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
        value = ContentItemApi.PATH_SUBMIT_PEER_REVIEW,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<PeerReviewList> submitPeerReview(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("assignmentGroupId") UUID assignmentGroupId,
         @Valid @RequestBody SubmitPeerReviewRequest submitPeerReviewRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_ASSIGNMENT_GROUP = "/v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId}";
    /**
     * PATCH /v1/lms/content-items/{contentItemId}/groups/{assignmentGroupId} : Update AssignmentGroup
     * Partially update a AssignmentGroup (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param contentItemId ContentItem id. (required)
     * @param assignmentGroupId AssignmentGroup id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param assignmentGroupUpdate  (required)
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
        value = ContentItemApi.PATH_UPDATE_ASSIGNMENT_GROUP,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<AssignmentGroup> updateAssignmentGroup(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("assignmentGroupId") UUID assignmentGroupId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody AssignmentGroupUpdate assignmentGroupUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CONTENT_ITEM = "/v1/lms/content-items/{contentItemId}";
    /**
     * PATCH /v1/lms/content-items/{contentItemId} : Update ContentItem
     * Partially update a ContentItem (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param contentItemId ContentItem id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param contentItemUpdate  (required)
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
        value = ContentItemApi.PATH_UPDATE_CONTENT_ITEM,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItem> updateContentItem(
         @PathVariable("contentItemId") UUID contentItemId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ContentItemUpdate contentItemUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_CONTENT_ITEM_RATING = "/v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId}";
    /**
     * PATCH /v1/lms/content-items/{contentItemId}/ratings/{contentItemRatingId} : Update ContentItemRating
     * Partially update a ContentItemRating (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param contentItemId ContentItem id. (required)
     * @param contentItemRatingId ContentItemRating id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param contentItemRatingUpdate  (required)
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
        value = ContentItemApi.PATH_UPDATE_CONTENT_ITEM_RATING,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ContentItemRating> updateContentItemRating(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("contentItemRatingId") UUID contentItemRatingId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ContentItemRatingUpdate contentItemRatingUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_ITEM_OVERRIDE = "/v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId}";
    /**
     * PATCH /v1/lms/content-items/{contentItemId}/overrides/{itemOverrideId} : Update ItemOverride
     * Partially update a ItemOverride (absent fields are kept, null clears a field). Requires If-Match with the current ETag.
     *
     * @param contentItemId ContentItem id. (required)
     * @param itemOverrideId ItemOverride id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param itemOverrideUpdate  (required)
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
        value = ContentItemApi.PATH_UPDATE_ITEM_OVERRIDE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<ItemOverride> updateItemOverride(
         @PathVariable("contentItemId") UUID contentItemId,
         @PathVariable("itemOverrideId") UUID itemOverrideId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody ItemOverrideUpdate itemOverrideUpdate,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
