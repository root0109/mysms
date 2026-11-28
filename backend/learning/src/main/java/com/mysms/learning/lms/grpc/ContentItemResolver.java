package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.ReleaseRuleService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.AssignmentGroup;
import com.mysms.learning.lms.api.model.ContentItem;
import com.mysms.learning.lms.api.model.ContentItemRating;
import com.mysms.learning.lms.api.model.ItemOverride;
import com.mysms.learning.lms.api.model.ReleaseGrant;
import com.mysms.learning.lms.business.ContentItemService;
import com.mysms.lms.v1.AddFromRepositoryRequest;
import com.mysms.lms.v1.AddFromRepositoryResponse;
import com.mysms.lms.v1.AssignmentGroupUpdate;
import com.mysms.lms.v1.ContentItemRatingUpdate;
import com.mysms.lms.v1.ContentItemServiceGrpc;
import com.mysms.lms.v1.ContentItemUpdate;
import com.mysms.lms.v1.CopyContentItemRequest;
import com.mysms.lms.v1.CopyContentItemResponse;
import com.mysms.lms.v1.CreateAssignmentGroupRequest;
import com.mysms.lms.v1.CreateAssignmentGroupResponse;
import com.mysms.lms.v1.CreateContentItemRatingRequest;
import com.mysms.lms.v1.CreateContentItemRatingResponse;
import com.mysms.lms.v1.CreateContentItemRequest;
import com.mysms.lms.v1.CreateContentItemResponse;
import com.mysms.lms.v1.CreateItemOverrideRequest;
import com.mysms.lms.v1.CreateItemOverrideResponse;
import com.mysms.lms.v1.DeleteAssignmentGroupRequest;
import com.mysms.lms.v1.DeleteAssignmentGroupResponse;
import com.mysms.lms.v1.DeleteContentItemRatingRequest;
import com.mysms.lms.v1.DeleteContentItemRatingResponse;
import com.mysms.lms.v1.DeleteContentItemRequest;
import com.mysms.lms.v1.DeleteContentItemResponse;
import com.mysms.lms.v1.DeleteItemOverrideRequest;
import com.mysms.lms.v1.DeleteItemOverrideResponse;
import com.mysms.lms.v1.DeleteReleaseGrantRequest;
import com.mysms.lms.v1.DeleteReleaseGrantResponse;
import com.mysms.lms.v1.DownloadSubmissionsRequest;
import com.mysms.lms.v1.DownloadSubmissionsResponse;
import com.mysms.lms.v1.FinalizePeerReviewRequest;
import com.mysms.lms.v1.FinalizePeerReviewResponse;
import com.mysms.lms.v1.GetAssignmentGroupRequest;
import com.mysms.lms.v1.GetAssignmentGroupResponse;
import com.mysms.lms.v1.GetContentItemRatingRequest;
import com.mysms.lms.v1.GetContentItemRatingResponse;
import com.mysms.lms.v1.GetContentItemRequest;
import com.mysms.lms.v1.GetContentItemResponse;
import com.mysms.lms.v1.GetItemOverrideRequest;
import com.mysms.lms.v1.GetItemOverrideResponse;
import com.mysms.lms.v1.GetReleaseGrantRequest;
import com.mysms.lms.v1.GetReleaseGrantResponse;
import com.mysms.lms.v1.GetReleaseStatusRequest;
import com.mysms.lms.v1.GetReleaseStatusResponse;
import com.mysms.lms.v1.GetWebResourceLaunchRequest;
import com.mysms.lms.v1.GetWebResourceLaunchResponse;
import com.mysms.lms.v1.GrantItemAccessRequest;
import com.mysms.lms.v1.GrantItemAccessResponse;
import com.mysms.lms.v1.ItemOverrideUpdate;
import com.mysms.lms.v1.ListAssignmentGroupsRequest;
import com.mysms.lms.v1.ListAssignmentGroupsResponse;
import com.mysms.lms.v1.ListContentItemRatingsRequest;
import com.mysms.lms.v1.ListContentItemRatingsResponse;
import com.mysms.lms.v1.ListContentItemsRequest;
import com.mysms.lms.v1.ListContentItemsResponse;
import com.mysms.lms.v1.ListItemOverridesRequest;
import com.mysms.lms.v1.ListItemOverridesResponse;
import com.mysms.lms.v1.ListReleaseGrantsRequest;
import com.mysms.lms.v1.ListReleaseGrantsResponse;
import com.mysms.lms.v1.MoveContentItemRequest;
import com.mysms.lms.v1.MoveContentItemResponse;
import com.mysms.lms.v1.PublishItemGradesRequest;
import com.mysms.lms.v1.PublishItemGradesResponse;
import com.mysms.lms.v1.ReassignItemRequest;
import com.mysms.lms.v1.ReassignItemResponse;
import com.mysms.lms.v1.RecordItemViewRequest;
import com.mysms.lms.v1.RecordItemViewResponse;
import com.mysms.lms.v1.ReorderReleaseRulesRequest;
import com.mysms.lms.v1.ReorderReleaseRulesResponse;
import com.mysms.lms.v1.RequestToJoinGroupRequest;
import com.mysms.lms.v1.RequestToJoinGroupResponse;
import com.mysms.lms.v1.SetContentItemVisibilityRequest;
import com.mysms.lms.v1.SetContentItemVisibilityResponse;
import com.mysms.lms.v1.SetGroupMembersRequest;
import com.mysms.lms.v1.SetGroupMembersResponse;
import com.mysms.lms.v1.SetReleaseRulesEnabledRequest;
import com.mysms.lms.v1.SetReleaseRulesEnabledResponse;
import com.mysms.lms.v1.SubmitPeerReviewRequest;
import com.mysms.lms.v1.SubmitPeerReviewResponse;
import com.mysms.lms.v1.UpdateAssignmentGroupResponse;
import com.mysms.lms.v1.UpdateContentItemRatingResponse;
import com.mysms.lms.v1.UpdateContentItemResponse;
import com.mysms.lms.v1.UpdateItemOverrideResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ContentItemService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ContentItemResolver extends ContentItemServiceGrpc.ContentItemServiceImplBase {

    private final ContentItemService service;
    private final ReleaseRuleService releaseRules;
    private final ProtoBridge bridge;

    public ContentItemResolver(ContentItemService service, ReleaseRuleService releaseRules, ProtoBridge bridge) {
        this.service = service;
        this.releaseRules = releaseRules;
        this.bridge = bridge;
    }

    @Override
    public void listContentItems(ListContentItemsRequest request, StreamObserver<ListContentItemsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ContentItem> rows = service.listContentItems(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListContentItemsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getContentItem(GetContentItemRequest request, StreamObserver<GetContentItemResponse> out) {
        ContentItem found = service.getContentItem(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetContentItemResponse.newBuilder(), "content_item", found).build());
    }

    @Override
    public void createContentItem(CreateContentItemRequest request, StreamObserver<CreateContentItemResponse> out) {
        ContentItem created = service.createContentItem(bridge.body(request.getContentItem(), ContentItem.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateContentItemResponse.newBuilder(), "content_item", created).build());
    }

    @Override
    public void updateContentItem(ContentItemUpdate request, StreamObserver<UpdateContentItemResponse> out) {
        ContentItem updated = service.updateContentItem(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ContentItemProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateContentItemResponse.newBuilder(), "content_item", updated).build());
    }

    @Override
    public void deleteContentItem(DeleteContentItemRequest request, StreamObserver<DeleteContentItemResponse> out) {
        service.deleteContentItem(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteContentItemResponse.getDefaultInstance());
    }

    @Override
    public void listContentItemRatings(ListContentItemRatingsRequest request,
            StreamObserver<ListContentItemRatingsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ContentItemRating> rows = service.listContentItemRatings(bridge.id(request.getContentItemId(), "contentItemId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListContentItemRatingsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getContentItemRating(GetContentItemRatingRequest request, StreamObserver<GetContentItemRatingResponse> out) {
        ContentItemRating found = service.getContentItemRating(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetContentItemRatingResponse.newBuilder(), "content_item_rating", found).build());
    }

    @Override
    public void createContentItemRating(CreateContentItemRatingRequest request,
            StreamObserver<CreateContentItemRatingResponse> out) {
        ContentItemRating created = service.createContentItemRating(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request.getContentItemRating(), ContentItemRating.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateContentItemRatingResponse.newBuilder(), "content_item_rating",
                created).build());
    }

    @Override
    public void updateContentItemRating(ContentItemRatingUpdate request,
            StreamObserver<UpdateContentItemRatingResponse> out) {
        ContentItemRating updated = service.updateContentItemRating(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ContentItemRatingProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateContentItemRatingResponse.newBuilder(), "content_item_rating",
                updated).build());
    }

    @Override
    public void deleteContentItemRating(DeleteContentItemRatingRequest request,
            StreamObserver<DeleteContentItemRatingResponse> out) {
        service.deleteContentItemRating(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteContentItemRatingResponse.getDefaultInstance());
    }

    @Override
    public void listItemOverrides(ListItemOverridesRequest request, StreamObserver<ListItemOverridesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ItemOverride> rows = service.listItemOverrides(bridge.id(request.getContentItemId(), "contentItemId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListItemOverridesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getItemOverride(GetItemOverrideRequest request, StreamObserver<GetItemOverrideResponse> out) {
        ItemOverride found = service.getItemOverride(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetItemOverrideResponse.newBuilder(), "item_override", found).build());
    }

    @Override
    public void createItemOverride(CreateItemOverrideRequest request, StreamObserver<CreateItemOverrideResponse> out) {
        ItemOverride created = service.createItemOverride(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request.getItemOverride(), ItemOverride.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateItemOverrideResponse.newBuilder(), "item_override", created).build());
    }

    @Override
    public void updateItemOverride(ItemOverrideUpdate request, StreamObserver<UpdateItemOverrideResponse> out) {
        ItemOverride updated = service.updateItemOverride(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                ItemOverrideProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateItemOverrideResponse.newBuilder(), "item_override", updated).build());
    }

    @Override
    public void deleteItemOverride(DeleteItemOverrideRequest request, StreamObserver<DeleteItemOverrideResponse> out) {
        service.deleteItemOverride(bridge.id(request.getContentItemId(), "contentItemId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteItemOverrideResponse.getDefaultInstance());
    }

    @Override
    public void listAssignmentGroups(ListAssignmentGroupsRequest request, StreamObserver<ListAssignmentGroupsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AssignmentGroup> rows = service.listAssignmentGroups(bridge.id(request.getContentItemId(), "contentItemId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAssignmentGroupsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssignmentGroup(GetAssignmentGroupRequest request, StreamObserver<GetAssignmentGroupResponse> out) {
        AssignmentGroup found = service.getAssignmentGroup(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssignmentGroupResponse.newBuilder(), "assignment_group", found).build());
    }

    @Override
    public void createAssignmentGroup(CreateAssignmentGroupRequest request, StreamObserver<CreateAssignmentGroupResponse> out) {
        AssignmentGroup created = service.createAssignmentGroup(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request.getAssignmentGroup(), AssignmentGroup.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssignmentGroupResponse.newBuilder(), "assignment_group", created).build());
    }

    @Override
    public void updateAssignmentGroup(AssignmentGroupUpdate request, StreamObserver<UpdateAssignmentGroupResponse> out) {
        AssignmentGroup updated = service.updateAssignmentGroup(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                AssignmentGroupProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssignmentGroupResponse.newBuilder(), "assignment_group", updated).build());
    }

    @Override
    public void deleteAssignmentGroup(DeleteAssignmentGroupRequest request, StreamObserver<DeleteAssignmentGroupResponse> out) {
        service.deleteAssignmentGroup(bridge.id(request.getContentItemId(), "contentItemId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssignmentGroupResponse.getDefaultInstance());
    }

    @Override
    public void listReleaseGrants(ListReleaseGrantsRequest request, StreamObserver<ListReleaseGrantsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ReleaseGrant> rows = service.listReleaseGrants(bridge.id(request.getContentItemId(), "contentItemId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListReleaseGrantsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getReleaseGrant(GetReleaseGrantRequest request, StreamObserver<GetReleaseGrantResponse> out) {
        ReleaseGrant found = service.getReleaseGrant(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetReleaseGrantResponse.newBuilder(), "release_grant", found).build());
    }

    @Override
    public void deleteReleaseGrant(DeleteReleaseGrantRequest request, StreamObserver<DeleteReleaseGrantResponse> out) {
        service.deleteReleaseGrant(bridge.id(request.getContentItemId(), "contentItemId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteReleaseGrantResponse.getDefaultInstance());
    }

    @Override
    public void moveContentItem(MoveContentItemRequest request, StreamObserver<MoveContentItemResponse> out) {
        ContentItem moved = service.moveContentItem(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.MoveContentItemRequest.class, "content_item_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(MoveContentItemResponse.newBuilder(), "content_item", moved).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void copyContentItem(CopyContentItemRequest request, StreamObserver<CopyContentItemResponse> out) {
        ContentItem copied = service.copyContentItem(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.CopyContentItemRequest.class, "content_item_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CopyContentItemResponse.newBuilder(), "content_item", copied).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void addFromRepository(AddFromRepositoryRequest request, StreamObserver<AddFromRepositoryResponse> out) {
        com.mysms.learning.lms.api.model.ContentItemList placed = service.addFromRepository(
                bridge.body(request, com.mysms.learning.lms.api.model.AddFromRepositoryRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(AddFromRepositoryResponse.newBuilder(), "items", placed.getItems())
                .build());
    }

    @Override
    public void setContentItemVisibility(SetContentItemVisibilityRequest request,
            StreamObserver<SetContentItemVisibilityResponse> out) {
        ContentItem changed = service.setContentItemVisibility(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.SetContentItemVisibilityRequest.class,
                        "content_item_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(SetContentItemVisibilityResponse.newBuilder(), "content_item", changed).build());
    }

    @Override
    public void getWebResourceLaunch(GetWebResourceLaunchRequest request,
            StreamObserver<GetWebResourceLaunchResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetWebResourceLaunchResponse.newBuilder(), "result",
                service.getWebResourceLaunch(bridge.id(request.getContentItemId(), "contentItemId"))).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void publishItemGrades(PublishItemGradesRequest request, StreamObserver<PublishItemGradesResponse> out) {
        com.mysms.learning.lms.api.model.GradebookEntryList published = service.publishItemGrades(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.PublishItemGradesRequest.class, "content_item_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(PublishItemGradesResponse.newBuilder(), "items", published.getItems())
                .build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void reassignItem(ReassignItemRequest request, StreamObserver<ReassignItemResponse> out) {
        com.mysms.learning.lms.api.model.ReassignmentLogList logged = service.reassignItem(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ReassignItemRequest.class, "content_item_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ReassignItemResponse.newBuilder(), "items", logged.getItems()).build());
    }

    @Override
    public void downloadSubmissions(DownloadSubmissionsRequest request,
            StreamObserver<DownloadSubmissionsResponse> out) {
        ProtoBridge.send(out, bridge.reply(DownloadSubmissionsResponse.newBuilder(), "result",
                service.downloadSubmissions(bridge.id(request.getContentItemId(), "contentItemId"))).build());
    }

    @Override
    public void getReleaseStatus(GetReleaseStatusRequest request, StreamObserver<GetReleaseStatusResponse> out) {
        com.mysms.learning.lms.api.model.ReleaseStatus status = service.getReleaseStatus(
                bridge.id(request.getContentItemId(), "contentItemId"),
                request.hasUserId() ? bridge.id(request.getUserId(), "userId") : null);
        ProtoBridge.send(out, bridge.reply(GetReleaseStatusResponse.newBuilder(), "result", status).build());
    }

    @Override
    public void grantItemAccess(GrantItemAccessRequest request, StreamObserver<GrantItemAccessResponse> out) {
        com.mysms.learning.lms.api.model.ReleaseGrantList granted = service.grantItemAccess(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.GrantItemAccessRequest.class, "content_item_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(GrantItemAccessResponse.newBuilder(), "items", granted.getItems()).build());
    }

    @Override
    public void setReleaseRulesEnabled(SetReleaseRulesEnabledRequest request,
            StreamObserver<SetReleaseRulesEnabledResponse> out) {
        com.mysms.learning.lms.api.model.ReleaseRuleList rules = releaseRules.setReleaseRulesEnabled(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.SetReleaseRulesEnabledRequest.class,
                        "content_item_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(SetReleaseRulesEnabledResponse.newBuilder(), "items", rules.getItems()).build());
    }

    @Override
    public void reorderReleaseRules(ReorderReleaseRulesRequest request, StreamObserver<ReorderReleaseRulesResponse> out) {
        com.mysms.learning.lms.api.model.ReleaseRuleList rules = releaseRules.reorderReleaseRules(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ReorderReleaseRulesRequest.class, "content_item_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ReorderReleaseRulesResponse.newBuilder(), "items", rules.getItems()).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void requestToJoinGroup(RequestToJoinGroupRequest request, StreamObserver<RequestToJoinGroupResponse> out) {
        com.mysms.learning.lms.api.model.AssignmentGroupMember member = service.requestToJoinGroup(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getAssignmentGroupId(), "assignmentGroupId"));
        ProtoBridge.send(out, bridge.reply(RequestToJoinGroupResponse.newBuilder(), "result", member).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void setGroupMembers(SetGroupMembersRequest request, StreamObserver<SetGroupMembersResponse> out) {
        com.mysms.learning.lms.api.model.AssignmentGroupMemberList members = service.setGroupMembers(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getAssignmentGroupId(), "assignmentGroupId"),
                bridge.body(request, com.mysms.learning.lms.api.model.SetGroupMembersRequest.class, "content_item_id",
                        "assignment_group_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(SetGroupMembersResponse.newBuilder(), "items", members.getItems())
                .build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void submitPeerReview(SubmitPeerReviewRequest request, StreamObserver<SubmitPeerReviewResponse> out) {
        com.mysms.learning.lms.api.model.PeerReviewList reviews = service.submitPeerReview(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getAssignmentGroupId(), "assignmentGroupId"),
                bridge.body(request, com.mysms.learning.lms.api.model.SubmitPeerReviewRequest.class, "content_item_id",
                        "assignment_group_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(SubmitPeerReviewResponse.newBuilder(), "items", reviews.getItems())
                .build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void finalizePeerReview(FinalizePeerReviewRequest request, StreamObserver<FinalizePeerReviewResponse> out) {
        com.mysms.learning.lms.api.model.GradebookEntryList entries = service.finalizePeerReview(
                bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.id(request.getAssignmentGroupId(), "assignmentGroupId"),
                bridge.body(request, com.mysms.learning.lms.api.model.FinalizePeerReviewRequest.class, "content_item_id",
                        "assignment_group_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(FinalizePeerReviewResponse.newBuilder(), "items", entries.getItems())
                .build());
    }

    @Override
    public void recordItemView(RecordItemViewRequest request, StreamObserver<RecordItemViewResponse> out) {
        ContentItem viewed = service.recordItemView(bridge.id(request.getContentItemId(), "contentItemId"),
                bridge.body(request, com.mysms.learning.lms.api.model.RecordItemViewRequest.class, "content_item_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(RecordItemViewResponse.newBuilder(), "content_item", viewed).build());
    }
}
