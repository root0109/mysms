package com.mysms.comms.grpc;

import com.mysms.comms.api.model.Post;
import com.mysms.comms.api.model.PostAudienceClass;
import com.mysms.comms.api.model.PostComment;
import com.mysms.comms.api.model.PostReaction;
import com.mysms.comms.business.PostService;
import com.mysms.comms.business.ReactionService;
import com.mysms.comms.v1.ApprovePostRequest;
import com.mysms.comms.v1.ApprovePostResponse;
import com.mysms.comms.v1.CancelPostRequest;
import com.mysms.comms.v1.CancelPostResponse;
import com.mysms.comms.v1.CreatePostAudienceClassRequest;
import com.mysms.comms.v1.CreatePostAudienceClassResponse;
import com.mysms.comms.v1.CreatePostCommentRequest;
import com.mysms.comms.v1.CreatePostCommentResponse;
import com.mysms.comms.v1.CreatePostReactionRequest;
import com.mysms.comms.v1.CreatePostReactionResponse;
import com.mysms.comms.v1.CreatePostRequest;
import com.mysms.comms.v1.CreatePostResponse;
import com.mysms.comms.v1.DeletePostAudienceClassRequest;
import com.mysms.comms.v1.DeletePostAudienceClassResponse;
import com.mysms.comms.v1.DeletePostCommentRequest;
import com.mysms.comms.v1.DeletePostCommentResponse;
import com.mysms.comms.v1.DeletePostReactionRequest;
import com.mysms.comms.v1.DeletePostReactionResponse;
import com.mysms.comms.v1.DeletePostRequest;
import com.mysms.comms.v1.DeletePostResponse;
import com.mysms.comms.v1.GetFeedRequest;
import com.mysms.comms.v1.GetFeedResponse;
import com.mysms.comms.v1.GetPostAudienceClassRequest;
import com.mysms.comms.v1.GetPostAudienceClassResponse;
import com.mysms.comms.v1.GetPostCommentRequest;
import com.mysms.comms.v1.GetPostCommentResponse;
import com.mysms.comms.v1.GetPostReactionRequest;
import com.mysms.comms.v1.GetPostReactionResponse;
import com.mysms.comms.v1.GetPostRequest;
import com.mysms.comms.v1.GetPostResponse;
import com.mysms.comms.v1.ListPostAudienceClassesRequest;
import com.mysms.comms.v1.ListPostAudienceClassesResponse;
import com.mysms.comms.v1.ListPostCommentsRequest;
import com.mysms.comms.v1.ListPostCommentsResponse;
import com.mysms.comms.v1.ListPostReactionsRequest;
import com.mysms.comms.v1.ListPostReactionsResponse;
import com.mysms.comms.v1.ListPostsRequest;
import com.mysms.comms.v1.ListPostsResponse;
import com.mysms.comms.v1.PostAudienceClassUpdate;
import com.mysms.comms.v1.PostCommentUpdate;
import com.mysms.comms.v1.PostReactionUpdate;
import com.mysms.comms.v1.PostServiceGrpc;
import com.mysms.comms.v1.PostUpdate;
import com.mysms.comms.v1.SubmitPostRequest;
import com.mysms.comms.v1.SubmitPostResponse;
import com.mysms.comms.v1.UpdatePostAudienceClassResponse;
import com.mysms.comms.v1.UpdatePostCommentResponse;
import com.mysms.comms.v1.UpdatePostReactionResponse;
import com.mysms.comms.v1.UpdatePostResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.PostService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PostResolver extends PostServiceGrpc.PostServiceImplBase {

    private final PostService service;
    private final ReactionService reactions;
    private final ProtoBridge bridge;

    public PostResolver(PostService service, ReactionService reactions, ProtoBridge bridge) {
        this.service = service;
        this.reactions = reactions;
        this.bridge = bridge;
    }

    @Override
    public void listPosts(ListPostsRequest request, StreamObserver<ListPostsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Post> rows = service.listPosts(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPostsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPost(GetPostRequest request, StreamObserver<GetPostResponse> out) {
        Post found = service.getPost(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPostResponse.newBuilder(), "post", found).build());
    }

    @Override
    public void createPost(CreatePostRequest request, StreamObserver<CreatePostResponse> out) {
        Post created = service.createPost(bridge.body(request.getPost(), Post.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePostResponse.newBuilder(), "post", created).build());
    }

    @Override
    public void updatePost(PostUpdate request, StreamObserver<UpdatePostResponse> out) {
        Post updated = service.updatePost(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                PostProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePostResponse.newBuilder(), "post", updated).build());
    }

    @Override
    public void deletePost(DeletePostRequest request, StreamObserver<DeletePostResponse> out) {
        service.deletePost(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePostResponse.getDefaultInstance());
    }

    @Override
    public void listPostAudienceClasses(ListPostAudienceClassesRequest request,
            StreamObserver<ListPostAudienceClassesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PostAudienceClass> rows = service.listPostAudienceClasss(
                bridge.id(request.getPostId(), "postId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPostAudienceClassesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPostAudienceClass(GetPostAudienceClassRequest request, StreamObserver<GetPostAudienceClassResponse> out) {
        PostAudienceClass found = service.getPostAudienceClass(
                bridge.id(request.getPostId(), "postId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPostAudienceClassResponse.newBuilder(), "post_audience_class", found).build());
    }

    @Override
    public void createPostAudienceClass(CreatePostAudienceClassRequest request,
            StreamObserver<CreatePostAudienceClassResponse> out) {
        PostAudienceClass created = service.createPostAudienceClass(
                bridge.id(request.getPostId(), "postId"), bridge.body(request.getPostAudienceClass(), PostAudienceClass.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePostAudienceClassResponse.newBuilder(), "post_audience_class", created).build());
    }

    @Override
    public void updatePostAudienceClass(PostAudienceClassUpdate request,
            StreamObserver<UpdatePostAudienceClassResponse> out) {
        PostAudienceClass updated = service.updatePostAudienceClass(
                bridge.id(request.getPostId(), "postId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PostAudienceClassProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePostAudienceClassResponse.newBuilder(), "post_audience_class", updated).build());
    }

    @Override
    public void deletePostAudienceClass(DeletePostAudienceClassRequest request,
            StreamObserver<DeletePostAudienceClassResponse> out) {
        service.deletePostAudienceClass(
                bridge.id(request.getPostId(), "postId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePostAudienceClassResponse.getDefaultInstance());
    }

    @Override
    public void listPostReactions(ListPostReactionsRequest request, StreamObserver<ListPostReactionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PostReaction> rows = reactions.listPostReactions(
                bridge.id(request.getPostId(), "postId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPostReactionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPostReaction(GetPostReactionRequest request, StreamObserver<GetPostReactionResponse> out) {
        PostReaction found = reactions.getPostReaction(bridge.id(request.getPostId(), "postId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPostReactionResponse.newBuilder(), "post_reaction", found).build());
    }

    @Override
    public void createPostReaction(CreatePostReactionRequest request, StreamObserver<CreatePostReactionResponse> out) {
        PostReaction created = reactions.createPostReaction(bridge.id(request.getPostId(), "postId"),
                bridge.body(request.getPostReaction(), PostReaction.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePostReactionResponse.newBuilder(), "post_reaction", created).build());
    }

    @Override
    public void updatePostReaction(PostReactionUpdate request, StreamObserver<UpdatePostReactionResponse> out) {
        PostReaction updated = reactions.updatePostReaction(
                bridge.id(request.getPostId(), "postId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PostReactionProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePostReactionResponse.newBuilder(), "post_reaction", updated).build());
    }

    @Override
    public void deletePostReaction(DeletePostReactionRequest request, StreamObserver<DeletePostReactionResponse> out) {
        reactions.deletePostReaction(bridge.id(request.getPostId(), "postId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePostReactionResponse.getDefaultInstance());
    }

    @Override
    public void listPostComments(ListPostCommentsRequest request, StreamObserver<ListPostCommentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PostComment> rows = service.listPostComments(
                bridge.id(request.getPostId(), "postId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPostCommentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPostComment(GetPostCommentRequest request, StreamObserver<GetPostCommentResponse> out) {
        PostComment found = service.getPostComment(bridge.id(request.getPostId(), "postId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPostCommentResponse.newBuilder(), "post_comment", found).build());
    }

    @Override
    public void createPostComment(CreatePostCommentRequest request, StreamObserver<CreatePostCommentResponse> out) {
        PostComment created = service.createPostComment(
                bridge.id(request.getPostId(), "postId"), bridge.body(request.getPostComment(), PostComment.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePostCommentResponse.newBuilder(), "post_comment", created).build());
    }

    @Override
    public void updatePostComment(PostCommentUpdate request, StreamObserver<UpdatePostCommentResponse> out) {
        PostComment updated = service.updatePostComment(
                bridge.id(request.getPostId(), "postId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PostCommentProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePostCommentResponse.newBuilder(), "post_comment", updated).build());
    }

    @Override
    public void deletePostComment(DeletePostCommentRequest request, StreamObserver<DeletePostCommentResponse> out) {
        service.deletePostComment(
                bridge.id(request.getPostId(), "postId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePostCommentResponse.getDefaultInstance());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void submitPost(SubmitPostRequest request, StreamObserver<SubmitPostResponse> out) {
        Post changed = service.submitPost(
                bridge.id(request.getPostId(), "postId"),
                bridge.body(request, com.mysms.comms.api.model.SubmitPostRequest.class, "post_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(SubmitPostResponse.newBuilder(), "post", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void approvePost(ApprovePostRequest request, StreamObserver<ApprovePostResponse> out) {
        Post changed = service.approvePost(
                bridge.id(request.getPostId(), "postId"),
                bridge.body(request, com.mysms.comms.api.model.ApprovePostRequest.class, "post_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ApprovePostResponse.newBuilder(), "post", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void cancelPost(CancelPostRequest request, StreamObserver<CancelPostResponse> out) {
        Post changed = service.cancelPost(
                bridge.id(request.getPostId(), "postId"),
                bridge.body(request, com.mysms.comms.api.model.CancelPostRequest.class, "post_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelPostResponse.newBuilder(), "post", changed).build());
    }

    @Override
    public void getFeed(GetFeedRequest request, StreamObserver<GetFeedResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Post> rows = service.getFeed(
                request.hasClassSectionId() ? bridge.id(request.getClassSectionId(), "classSectionId") : null,
                request.hasClassPageId() ? bridge.id(request.getClassPageId(), "classPageId") : null,
                request.hasSavedOnly() ? request.getSavedOnly() : null, page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(GetFeedResponse.newBuilder(), rows).build());
    }
}
