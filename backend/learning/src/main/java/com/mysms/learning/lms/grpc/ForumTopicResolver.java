package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ForumPost;
import com.mysms.learning.lms.api.model.ForumPostUpdate;
import com.mysms.learning.lms.api.model.ForumReadState;
import com.mysms.learning.lms.api.model.ForumTopic;
import com.mysms.learning.lms.api.model.ForumTopicUpdate;
import com.mysms.learning.lms.business.ForumTopicService;
import com.mysms.lms.v1.ConfirmPostAbuseRequest;
import com.mysms.lms.v1.ConfirmPostAbuseResponse;
import com.mysms.lms.v1.CreateForumPostRequest;
import com.mysms.lms.v1.CreateForumPostResponse;
import com.mysms.lms.v1.CreateForumTopicRequest;
import com.mysms.lms.v1.CreateForumTopicResponse;
import com.mysms.lms.v1.DeleteForumPostRequest;
import com.mysms.lms.v1.DeleteForumPostResponse;
import com.mysms.lms.v1.DeleteForumTopicRequest;
import com.mysms.lms.v1.DeleteForumTopicResponse;
import com.mysms.lms.v1.FlagForumPostRequest;
import com.mysms.lms.v1.FlagForumPostResponse;
import com.mysms.lms.v1.ForumTopicServiceGrpc;
import com.mysms.lms.v1.GetForumPostRequest;
import com.mysms.lms.v1.GetForumPostResponse;
import com.mysms.lms.v1.GetForumTopicRequest;
import com.mysms.lms.v1.GetForumTopicResponse;
import com.mysms.lms.v1.HideForumPostRequest;
import com.mysms.lms.v1.HideForumPostResponse;
import com.mysms.lms.v1.HideForumTopicRequest;
import com.mysms.lms.v1.HideForumTopicResponse;
import com.mysms.lms.v1.ListForumPostsRequest;
import com.mysms.lms.v1.ListForumPostsResponse;
import com.mysms.lms.v1.ListForumReadStatesRequest;
import com.mysms.lms.v1.ListForumReadStatesResponse;
import com.mysms.lms.v1.ListForumTopicsRequest;
import com.mysms.lms.v1.ListForumTopicsResponse;
import com.mysms.lms.v1.MarkPostSubstantiveRequest;
import com.mysms.lms.v1.MarkPostSubstantiveResponse;
import com.mysms.lms.v1.MarkTopicReadRequest;
import com.mysms.lms.v1.MarkTopicReadResponse;
import com.mysms.lms.v1.UnhideForumPostRequest;
import com.mysms.lms.v1.UnhideForumPostResponse;
import com.mysms.lms.v1.UnhideForumTopicRequest;
import com.mysms.lms.v1.UnhideForumTopicResponse;
import com.mysms.lms.v1.UpdateForumPostRequest;
import com.mysms.lms.v1.UpdateForumPostResponse;
import com.mysms.lms.v1.UpdateForumTopicRequest;
import com.mysms.lms.v1.UpdateForumTopicResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ForumTopicService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ForumTopicResolver extends ForumTopicServiceGrpc.ForumTopicServiceImplBase {

    private final ForumTopicService service;
    private final ProtoBridge bridge;

    public ForumTopicResolver(ForumTopicService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listForumTopics(ListForumTopicsRequest request, StreamObserver<ListForumTopicsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ForumTopic> rows = service.listForumTopics(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListForumTopicsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getForumTopic(GetForumTopicRequest request, StreamObserver<GetForumTopicResponse> out) {
        ForumTopic found = service.getForumTopic(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetForumTopicResponse.newBuilder(), "forum_topic", found).build());
    }

    @Override
    public void createForumTopic(CreateForumTopicRequest request, StreamObserver<CreateForumTopicResponse> out) {
        ForumTopic created = service.createForumTopic(
                bridge.body(request.getForumTopic(), ForumTopic.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateForumTopicResponse.newBuilder(), "forum_topic", created).build());
    }

    @Override
    public void updateForumTopic(UpdateForumTopicRequest request, StreamObserver<UpdateForumTopicResponse> out) {
        ForumTopic updated = service.updateForumTopic(
                bridge.id(request.getForumTopic().getId(), "id"),
                bridge.requiredIfMatch(request.getForumTopic().getEtag()),
                bridge.changes(request.getForumTopic(), request.getUpdateMask(), ForumTopicUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateForumTopicResponse.newBuilder(), "forum_topic", updated).build());
    }

    @Override
    public void deleteForumTopic(DeleteForumTopicRequest request, StreamObserver<DeleteForumTopicResponse> out) {
        service.deleteForumTopic(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteForumTopicResponse.getDefaultInstance());
    }

    @Override
    public void listForumPosts(ListForumPostsRequest request, StreamObserver<ListForumPostsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ForumPost> rows = service.listForumPosts(
                bridge.id(request.getForumTopicId(), "forumTopicId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListForumPostsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getForumPost(GetForumPostRequest request, StreamObserver<GetForumPostResponse> out) {
        ForumPost found = service.getForumPost(
                bridge.id(request.getForumTopicId(), "forumTopicId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetForumPostResponse.newBuilder(), "forum_post", found).build());
    }

    @Override
    public void createForumPost(CreateForumPostRequest request, StreamObserver<CreateForumPostResponse> out) {
        ForumPost created = service.createForumPost(
                bridge.id(request.getForumTopicId(), "forumTopicId"),
                bridge.body(request.getForumPost(), ForumPost.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateForumPostResponse.newBuilder(), "forum_post", created).build());
    }

    @Override
    public void updateForumPost(UpdateForumPostRequest request, StreamObserver<UpdateForumPostResponse> out) {
        ForumPost updated = service.updateForumPost(
                bridge.id(request.getForumTopicId(), "forumTopicId"), bridge.id(request.getForumPost().getId(), "id"),
                bridge.requiredIfMatch(request.getForumPost().getEtag()),
                bridge.changes(request.getForumPost(), request.getUpdateMask(), ForumPostUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateForumPostResponse.newBuilder(), "forum_post", updated).build());
    }

    @Override
    public void deleteForumPost(DeleteForumPostRequest request, StreamObserver<DeleteForumPostResponse> out) {
        service.deleteForumPost(
                bridge.id(request.getForumTopicId(), "forumTopicId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteForumPostResponse.getDefaultInstance());
    }

    @Override
    public void listForumReadStates(ListForumReadStatesRequest request,
            StreamObserver<ListForumReadStatesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ForumReadState> rows = service.listForumReadStates(
                bridge.id(request.getForumTopicId(), "forumTopicId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListForumReadStatesResponse.newBuilder(), rows).build());
    }

    @Override
    public void markTopicRead(MarkTopicReadRequest request, StreamObserver<MarkTopicReadResponse> out) {
        service.markTopicRead(bridge.id(request.getForumTopicId(), "forumTopicId"),
                request.getForumPostIdsList().stream().map(id -> bridge.id(id, "forumPostIds")).toList());
        ProtoBridge.send(out, MarkTopicReadResponse.getDefaultInstance());
    }

    @Override
    public void hideForumTopic(HideForumTopicRequest request, StreamObserver<HideForumTopicResponse> out) {
        ForumTopic changed = service.hideForumTopic(
                bridge.id(request.getForumTopicId(), "forumTopicId"),
                bridge.body(request, com.mysms.learning.lms.api.model.HideForumTopicRequest.class, "forum_topic_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(HideForumTopicResponse.newBuilder(), "forum_topic", changed).build());
    }

    @Override
    public void unhideForumTopic(UnhideForumTopicRequest request, StreamObserver<UnhideForumTopicResponse> out) {
        ForumTopic changed = service.unhideForumTopic(
                bridge.id(request.getForumTopicId(), "forumTopicId"),
                bridge.body(request, com.mysms.learning.lms.api.model.UnhideForumTopicRequest.class, "forum_topic_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(UnhideForumTopicResponse.newBuilder(), "forum_topic", changed).build());
    }

    @Override
    public void hideForumPost(HideForumPostRequest request, StreamObserver<HideForumPostResponse> out) {
        ForumPost changed = service.hideForumPost(bridge.id(request.getForumTopicId(), "forumTopicId"),
                bridge.id(request.getForumPostId(), "forumPostId"), request.hasEtag() ? request.getEtag() : null,
                request.hasReason() ? request.getReason() : null);
        ProtoBridge.send(out, bridge.reply(HideForumPostResponse.newBuilder(), "forum_post", changed).build());
    }

    @Override
    public void unhideForumPost(UnhideForumPostRequest request, StreamObserver<UnhideForumPostResponse> out) {
        ForumPost changed = service.unhideForumPost(bridge.id(request.getForumTopicId(), "forumTopicId"),
                bridge.id(request.getForumPostId(), "forumPostId"), request.hasEtag() ? request.getEtag() : null);
        ProtoBridge.send(out, bridge.reply(UnhideForumPostResponse.newBuilder(), "forum_post", changed).build());
    }

    @Override
    public void confirmPostAbuse(ConfirmPostAbuseRequest request, StreamObserver<ConfirmPostAbuseResponse> out) {
        ForumPost changed = service.confirmPostAbuse(bridge.id(request.getForumTopicId(), "forumTopicId"),
                bridge.id(request.getForumPostId(), "forumPostId"), request.hasEtag() ? request.getEtag() : null,
                request.hasReason() ? request.getReason() : null);
        ProtoBridge.send(out, bridge.reply(ConfirmPostAbuseResponse.newBuilder(), "forum_post", changed).build());
    }

    @Override
    public void flagForumPost(FlagForumPostRequest request, StreamObserver<FlagForumPostResponse> out) {
        service.flagForumPost(bridge.id(request.getForumTopicId(), "forumTopicId"),
                bridge.id(request.getForumPostId(), "forumPostId"), request.getFlagged());
        ProtoBridge.send(out, FlagForumPostResponse.getDefaultInstance());
    }

    @Override
    public void markPostSubstantive(MarkPostSubstantiveRequest request,
            StreamObserver<MarkPostSubstantiveResponse> out) {
        ForumPost changed = service.markPostSubstantive(bridge.id(request.getForumTopicId(), "forumTopicId"),
                bridge.id(request.getForumPostId(), "forumPostId"), request.getSubstantive());
        ProtoBridge.send(out, bridge.reply(MarkPostSubstantiveResponse.newBuilder(), "forum_post", changed).build());
    }
}
