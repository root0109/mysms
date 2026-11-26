package com.mysms.learning.lms.controller;

import com.mysms.learning.lms.business.ReleaseRuleService;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ContentItemApi;
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
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ContentItemService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ContentItem operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ContentItemController implements ContentItemApi {

    private final ContentItemService contentItemService;
    private final ReleaseRuleService releaseRuleService;

    public ContentItemController(ContentItemService contentItemService, ReleaseRuleService releaseRuleService) {
        this.contentItemService = contentItemService;
        this.releaseRuleService = releaseRuleService;
    }

    @Override
    public ResponseEntity<ContentItemList> addFromRepository(
            AddFromRepositoryRequest addFromRepositoryRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(contentItemService.addFromRepository(addFromRepositoryRequest));
    }

    @Override
    public ResponseEntity<ContentItem> copyContentItem(UUID contentItemId,
            CopyContentItemRequest copyContentItemRequest, UUID idempotencyKey) {
        ContentItem copied = contentItemService.copyContentItem(contentItemId, copyContentItemRequest);
        return ResponseEntity.created(URI.create("/v1/lms/content-items/" + copied.getId()))
                .eTag(copied.getEtag()).body(copied);
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
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(contentItemService.downloadSubmissions(contentItemId));
    }

    @Override
    public ResponseEntity<GradebookEntryList> finalizePeerReview(UUID contentItemId,
            UUID assignmentGroupId, FinalizePeerReviewRequest finalizePeerReviewRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(contentItemService.finalizePeerReview(contentItemId, assignmentGroupId,
                finalizePeerReviewRequest));
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
        return ResponseEntity.ok(contentItemService.getReleaseStatus(contentItemId, userId));
    }

    @Override
    public ResponseEntity<ExternalLink> getWebResourceLaunch(UUID contentItemId) {
        return ResponseEntity.ok(contentItemService.getWebResourceLaunch(contentItemId));
    }

    @Override
    public ResponseEntity<ReleaseGrantList> grantItemAccess(UUID contentItemId,
            GrantItemAccessRequest grantItemAccessRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(contentItemService.grantItemAccess(contentItemId, grantItemAccessRequest));
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
        ContentItem moved = contentItemService.moveContentItem(contentItemId, moveContentItemRequest);
        return ResponseEntity.ok().eTag(moved.getEtag()).body(moved);
    }

    @Override
    public ResponseEntity<GradebookEntryList> publishItemGrades(UUID contentItemId,
            PublishItemGradesRequest publishItemGradesRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(contentItemService.publishItemGrades(contentItemId, publishItemGradesRequest));
    }

    @Override
    public ResponseEntity<ReassignmentLogList> reassignItem(UUID contentItemId,
            ReassignItemRequest reassignItemRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(contentItemService.reassignItem(contentItemId, reassignItemRequest));
    }

    @Override
    public ResponseEntity<ContentItem> recordItemView(UUID contentItemId,
            RecordItemViewRequest recordItemViewRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(contentItemService.recordItemView(contentItemId, recordItemViewRequest));
    }

    @Override
    public ResponseEntity<ReleaseRuleList> reorderReleaseRules(UUID contentItemId,
            ReorderReleaseRulesRequest reorderReleaseRulesRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(releaseRuleService.reorderReleaseRules(contentItemId, reorderReleaseRulesRequest));
    }

    @Override
    public ResponseEntity<AssignmentGroupMember> requestToJoinGroup(UUID contentItemId,
            UUID assignmentGroupId, Object body, UUID idempotencyKey) {
        AssignmentGroupMember member = contentItemService.requestToJoinGroup(contentItemId, assignmentGroupId);
        return ResponseEntity.ok().eTag(member.getEtag()).body(member);
    }

    @Override
    public ResponseEntity<ContentItem> setContentItemVisibility(UUID contentItemId,
            SetContentItemVisibilityRequest setContentItemVisibilityRequest, UUID idempotencyKey) {
        ContentItem changed = contentItemService.setContentItemVisibility(contentItemId, setContentItemVisibilityRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AssignmentGroupMemberList> setGroupMembers(UUID contentItemId,
            UUID assignmentGroupId, SetGroupMembersRequest setGroupMembersRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(contentItemService.setGroupMembers(contentItemId, assignmentGroupId,
                setGroupMembersRequest));
    }

    @Override
    public ResponseEntity<ReleaseRuleList> setReleaseRulesEnabled(UUID contentItemId,
            SetReleaseRulesEnabledRequest setReleaseRulesEnabledRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(releaseRuleService.setReleaseRulesEnabled(contentItemId,
                setReleaseRulesEnabledRequest));
    }

    @Override
    public ResponseEntity<PeerReviewList> submitPeerReview(UUID contentItemId, UUID assignmentGroupId,
            SubmitPeerReviewRequest submitPeerReviewRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(contentItemService.submitPeerReview(contentItemId, assignmentGroupId,
                submitPeerReviewRequest));
    }

    @Override
    public ResponseEntity<AssignmentGroup> updateAssignmentGroup(UUID contentItemId,
            UUID assignmentGroupId, String ifMatch, AssignmentGroupUpdate assignmentGroupUpdate, Boolean validateOnly) {
        AssignmentGroup updated =
                contentItemService.updateAssignmentGroup(contentItemId, assignmentGroupId, ifMatch,
                        assignmentGroupUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ContentItem> updateContentItem(UUID contentItemId, String ifMatch,
            ContentItemUpdate contentItemUpdate, Boolean validateOnly) {
        ContentItem updated =
                contentItemService.updateContentItem(contentItemId, ifMatch, contentItemUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ContentItemRating> updateContentItemRating(UUID contentItemId,
            UUID contentItemRatingId, String ifMatch, ContentItemRatingUpdate contentItemRatingUpdate,
            Boolean validateOnly) {
        ContentItemRating updated =
                contentItemService.updateContentItemRating(contentItemId, contentItemRatingId, ifMatch,
                        contentItemRatingUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ItemOverride> updateItemOverride(UUID contentItemId, UUID itemOverrideId,
            String ifMatch, ItemOverrideUpdate itemOverrideUpdate, Boolean validateOnly) {
        ItemOverride updated =
                contentItemService.updateItemOverride(contentItemId, itemOverrideId, ifMatch, itemOverrideUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
