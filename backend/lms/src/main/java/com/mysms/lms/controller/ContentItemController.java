package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ContentItemApi;
import com.mysms.lms.api.model.AddFromRepositoryRequest;
import com.mysms.lms.api.model.AssignmentGroup;
import com.mysms.lms.api.model.AssignmentGroupMember;
import com.mysms.lms.api.model.AssignmentGroupMemberList;
import com.mysms.lms.api.model.AssignmentGroupUpdate;
import com.mysms.lms.api.model.ContentItem;
import com.mysms.lms.api.model.ContentItemList;
import com.mysms.lms.api.model.ContentItemRating;
import com.mysms.lms.api.model.ContentItemRatingUpdate;
import com.mysms.lms.api.model.ContentItemUpdate;
import com.mysms.lms.api.model.CopyContentItemRequest;
import com.mysms.lms.api.model.ExternalLink;
import com.mysms.lms.api.model.FinalizePeerReviewRequest;
import com.mysms.lms.api.model.GradebookEntryList;
import com.mysms.lms.api.model.GrantItemAccessRequest;
import com.mysms.lms.api.model.ItemOverride;
import com.mysms.lms.api.model.ItemOverrideUpdate;
import com.mysms.lms.api.model.MoveContentItemRequest;
import com.mysms.lms.api.model.Operation;
import com.mysms.lms.api.model.PeerReviewList;
import com.mysms.lms.api.model.PublishItemGradesRequest;
import com.mysms.lms.api.model.ReassignItemRequest;
import com.mysms.lms.api.model.ReassignmentLogList;
import com.mysms.lms.api.model.RecordItemViewRequest;
import com.mysms.lms.api.model.ReleaseGrant;
import com.mysms.lms.api.model.ReleaseGrantList;
import com.mysms.lms.api.model.ReleaseRuleList;
import com.mysms.lms.api.model.ReleaseStatus;
import com.mysms.lms.api.model.ReorderReleaseRulesRequest;
import com.mysms.lms.api.model.SetContentItemVisibilityRequest;
import com.mysms.lms.api.model.SetGroupMembersRequest;
import com.mysms.lms.api.model.SetReleaseRulesEnabledRequest;
import com.mysms.lms.api.model.SubmitPeerReviewRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ContentItemService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ContentItem operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ContentItemController implements ContentItemApi {

    private final ContentItemService contentItemService;

    public ContentItemController(ContentItemService contentItemService) {
        this.contentItemService = contentItemService;
    }

    @Override
    public ResponseEntity<ContentItemList> addFromRepository(
            AddFromRepositoryRequest addFromRepositoryRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ContentItem> copyContentItem(UUID contentItemId,
            CopyContentItemRequest copyContentItemRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AssignmentGroup> createAssignmentGroup(UUID contentItemId,
            AssignmentGroup assignmentGroup, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AssignmentGroup created = contentItemService.createAssignmentGroup(contentItemId, assignmentGroup, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/content-items/" + contentItemId + "/groups/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ContentItem> createContentItem(ContentItem contentItem, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ContentItem created = contentItemService.createContentItem(contentItem, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/content-items/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<ContentItemRating> createContentItemRating(UUID contentItemId,
            ContentItemRating contentItemRating, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ContentItemRating created =
                contentItemService.createContentItemRating(contentItemId, contentItemRating, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/content-items/" + contentItemId + "/ratings/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ItemOverride> createItemOverride(UUID contentItemId, ItemOverride itemOverride,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ItemOverride created = contentItemService.createItemOverride(contentItemId, itemOverride, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/content-items/" + contentItemId + "/overrides/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAssignmentGroup(UUID contentItemId, UUID assignmentGroupId,
            String ifMatch) {
        contentItemService.deleteAssignmentGroup(contentItemId, assignmentGroupId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteContentItem(UUID contentItemId, String ifMatch) {
        contentItemService.deleteContentItem(contentItemId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteContentItemRating(UUID contentItemId, UUID contentItemRatingId,
            String ifMatch) {
        contentItemService.deleteContentItemRating(contentItemId, contentItemRatingId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteItemOverride(UUID contentItemId, UUID itemOverrideId,
            String ifMatch) {
        contentItemService.deleteItemOverride(contentItemId, itemOverrideId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteReleaseGrant(UUID contentItemId, UUID releaseGrantId,
            String ifMatch) {
        contentItemService.deleteReleaseGrant(contentItemId, releaseGrantId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Operation> downloadSubmissions(UUID contentItemId, Object body,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<GradebookEntryList> finalizePeerReview(UUID contentItemId,
            UUID assignmentGroupId, FinalizePeerReviewRequest finalizePeerReviewRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AssignmentGroup> getAssignmentGroup(UUID contentItemId, UUID assignmentGroupId,
            String fields) {
        AssignmentGroup found = contentItemService.getAssignmentGroup(contentItemId, assignmentGroupId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ContentItem> getContentItem(UUID contentItemId, String fields) {
        ContentItem found = contentItemService.getContentItem(contentItemId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ContentItemRating> getContentItemRating(UUID contentItemId,
            UUID contentItemRatingId, String fields) {
        ContentItemRating found = contentItemService.getContentItemRating(contentItemId, contentItemRatingId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ItemOverride> getItemOverride(UUID contentItemId, UUID itemOverrideId,
            String fields) {
        ItemOverride found = contentItemService.getItemOverride(contentItemId, itemOverrideId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ReleaseGrant> getReleaseGrant(UUID contentItemId, UUID releaseGrantId,
            String fields) {
        ReleaseGrant found = contentItemService.getReleaseGrant(contentItemId, releaseGrantId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ReleaseStatus> getReleaseStatus(UUID contentItemId, UUID userId) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ExternalLink> getWebResourceLaunch(UUID contentItemId) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ReleaseGrantList> grantItemAccess(UUID contentItemId,
            GrantItemAccessRequest grantItemAccessRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<AssignmentGroup>> listAssignmentGroups(UUID contentItemId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AssignmentGroup> list =
                PagingUtil.pagingInfo(contentItemService.listAssignmentGroups(contentItemId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ContentItemRating>> listContentItemRatings(UUID contentItemId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ContentItemRating> list =
                PagingUtil.pagingInfo(contentItemService.listContentItemRatings(contentItemId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ContentItem>> listContentItems(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ContentItem> list =
                PagingUtil.pagingInfo(contentItemService.listContentItems(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ItemOverride>> listItemOverrides(UUID contentItemId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ItemOverride> list =
                PagingUtil.pagingInfo(contentItemService.listItemOverrides(contentItemId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ReleaseGrant>> listReleaseGrants(UUID contentItemId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ReleaseGrant> list =
                PagingUtil.pagingInfo(contentItemService.listReleaseGrants(contentItemId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ContentItem> moveContentItem(UUID contentItemId,
            MoveContentItemRequest moveContentItemRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<GradebookEntryList> publishItemGrades(UUID contentItemId,
            PublishItemGradesRequest publishItemGradesRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ReassignmentLogList> reassignItem(UUID contentItemId,
            ReassignItemRequest reassignItemRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ContentItem> recordItemView(UUID contentItemId,
            RecordItemViewRequest recordItemViewRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ReleaseRuleList> reorderReleaseRules(UUID contentItemId,
            ReorderReleaseRulesRequest reorderReleaseRulesRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AssignmentGroupMember> requestToJoinGroup(UUID contentItemId,
            UUID assignmentGroupId, Object body, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ContentItem> setContentItemVisibility(UUID contentItemId,
            SetContentItemVisibilityRequest setContentItemVisibilityRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AssignmentGroupMemberList> setGroupMembers(UUID contentItemId,
            UUID assignmentGroupId, SetGroupMembersRequest setGroupMembersRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ReleaseRuleList> setReleaseRulesEnabled(UUID contentItemId,
            SetReleaseRulesEnabledRequest setReleaseRulesEnabledRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PeerReviewList> submitPeerReview(UUID contentItemId, UUID assignmentGroupId,
            SubmitPeerReviewRequest submitPeerReviewRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AssignmentGroup> updateAssignmentGroup(UUID contentItemId,
            UUID assignmentGroupId, String ifMatch, AssignmentGroupUpdate assignmentGroupUpdate, String updateMask,
            Boolean validateOnly) {
        AssignmentGroup updated =
                contentItemService.updateAssignmentGroup(contentItemId, assignmentGroupId, ifMatch,
                        assignmentGroupUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ContentItem> updateContentItem(UUID contentItemId, String ifMatch,
            ContentItemUpdate contentItemUpdate, String updateMask, Boolean validateOnly) {
        ContentItem updated =
                contentItemService.updateContentItem(contentItemId, ifMatch, contentItemUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ContentItemRating> updateContentItemRating(UUID contentItemId,
            UUID contentItemRatingId, String ifMatch, ContentItemRatingUpdate contentItemRatingUpdate,
            String updateMask, Boolean validateOnly) {
        ContentItemRating updated =
                contentItemService.updateContentItemRating(contentItemId, contentItemRatingId, ifMatch,
                        contentItemRatingUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ItemOverride> updateItemOverride(UUID contentItemId, UUID itemOverrideId,
            String ifMatch, ItemOverrideUpdate itemOverrideUpdate, String updateMask, Boolean validateOnly) {
        ItemOverride updated =
                contentItemService.updateItemOverride(contentItemId, itemOverrideId, ifMatch, itemOverrideUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
