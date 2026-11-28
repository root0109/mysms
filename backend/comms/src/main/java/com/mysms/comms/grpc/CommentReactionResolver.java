package com.mysms.comms.grpc;

import com.mysms.comms.api.model.CommentReaction;
import com.mysms.comms.business.ReactionService;
import com.mysms.comms.v1.CommentReactionServiceGrpc;
import com.mysms.comms.v1.CommentReactionUpdate;
import com.mysms.comms.v1.CreateCommentReactionRequest;
import com.mysms.comms.v1.CreateCommentReactionResponse;
import com.mysms.comms.v1.DeleteCommentReactionRequest;
import com.mysms.comms.v1.DeleteCommentReactionResponse;
import com.mysms.comms.v1.GetCommentReactionRequest;
import com.mysms.comms.v1.GetCommentReactionResponse;
import com.mysms.comms.v1.ListCommentReactionsRequest;
import com.mysms.comms.v1.ListCommentReactionsResponse;
import com.mysms.comms.v1.UpdateCommentReactionResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/** gRPC resolver of comms.CommentReactionService: the same business service as REST. */
@GrpcService
public class CommentReactionResolver extends CommentReactionServiceGrpc.CommentReactionServiceImplBase {

    private final ReactionService service;
    private final ProtoBridge bridge;

    public CommentReactionResolver(ReactionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCommentReactions(ListCommentReactionsRequest request,
            StreamObserver<ListCommentReactionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CommentReaction> rows = service.listCommentReactions(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCommentReactionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCommentReaction(GetCommentReactionRequest request, StreamObserver<GetCommentReactionResponse> out) {
        CommentReaction found = service.getCommentReaction(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCommentReactionResponse.newBuilder(), "comment_reaction", found).build());
    }

    @Override
    public void createCommentReaction(CreateCommentReactionRequest request,
            StreamObserver<CreateCommentReactionResponse> out) {
        CommentReaction created = service.createCommentReaction(
                bridge.body(request.getCommentReaction(), CommentReaction.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCommentReactionResponse.newBuilder(), "comment_reaction", created)
                .build());
    }

    @Override
    public void updateCommentReaction(CommentReactionUpdate request,
            StreamObserver<UpdateCommentReactionResponse> out) {
        CommentReaction updated = service.updateCommentReaction(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                CommentReactionProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCommentReactionResponse.newBuilder(), "comment_reaction", updated)
                .build());
    }

    @Override
    public void deleteCommentReaction(DeleteCommentReactionRequest request,
            StreamObserver<DeleteCommentReactionResponse> out) {
        service.deleteCommentReaction(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCommentReactionResponse.getDefaultInstance());
    }
}
