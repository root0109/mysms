package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.ForumPostVote;
import com.mysms.lms.api.model.ForumPostVoteUpdate;
import com.mysms.lms.business.ForumPostVoteService;
import com.mysms.lms.v1.CreateForumPostVoteRequest;
import com.mysms.lms.v1.CreateForumPostVoteResponse;
import com.mysms.lms.v1.DeleteForumPostVoteRequest;
import com.mysms.lms.v1.DeleteForumPostVoteResponse;
import com.mysms.lms.v1.ForumPostVoteServiceGrpc;
import com.mysms.lms.v1.ListForumPostVotesRequest;
import com.mysms.lms.v1.ListForumPostVotesResponse;
import com.mysms.lms.v1.UpdateForumPostVoteRequest;
import com.mysms.lms.v1.UpdateForumPostVoteResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ForumPostVoteService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ForumPostVoteResolver extends ForumPostVoteServiceGrpc.ForumPostVoteServiceImplBase {

    private final ForumPostVoteService service;
    private final ProtoBridge bridge;

    public ForumPostVoteResolver(ForumPostVoteService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listForumPostVotes(ListForumPostVotesRequest request, StreamObserver<ListForumPostVotesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ForumPostVote> rows = service.listForumPostVotes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListForumPostVotesResponse.newBuilder(), rows).build());
    }

    @Override
    public void createForumPostVote(CreateForumPostVoteRequest request,
            StreamObserver<CreateForumPostVoteResponse> out) {
        ForumPostVote created = service.createForumPostVote(
                bridge.body(request.getForumPostVote(), ForumPostVote.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateForumPostVoteResponse.newBuilder(), "forum_post_vote", created).build());
    }

    @Override
    public void updateForumPostVote(UpdateForumPostVoteRequest request,
            StreamObserver<UpdateForumPostVoteResponse> out) {
        ForumPostVote updated = service.updateForumPostVote(
                bridge.id(request.getForumPostVote().getId(), "id"),
                bridge.requiredIfMatch(request.getForumPostVote().getEtag()),
                bridge.changes(request.getForumPostVote(), request.getUpdateMask(), ForumPostVoteUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateForumPostVoteResponse.newBuilder(), "forum_post_vote", updated).build());
    }

    @Override
    public void deleteForumPostVote(DeleteForumPostVoteRequest request,
            StreamObserver<DeleteForumPostVoteResponse> out) {
        service.deleteForumPostVote(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteForumPostVoteResponse.getDefaultInstance());
    }
}
