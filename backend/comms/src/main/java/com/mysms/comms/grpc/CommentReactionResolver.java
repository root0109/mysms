package com.mysms.comms.grpc;

import com.mysms.comms.v1.CommentReactionServiceGrpc;
import com.mysms.comms.v1.CreateCommentReactionRequest;
import com.mysms.comms.v1.CreateCommentReactionResponse;
import com.mysms.comms.v1.DeleteCommentReactionRequest;
import com.mysms.comms.v1.DeleteCommentReactionResponse;
import com.mysms.comms.v1.GetCommentReactionRequest;
import com.mysms.comms.v1.GetCommentReactionResponse;
import com.mysms.comms.v1.ListCommentReactionsRequest;
import com.mysms.comms.v1.ListCommentReactionsResponse;
import com.mysms.comms.v1.UpdateCommentReactionRequest;
import com.mysms.comms.v1.UpdateCommentReactionResponse;
import com.mysms.core.error.NotImplementedException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.CommentReactionService: not served yet (REST answers 501), so every RPC answers UNIMPLEMENTED
 * explicitly rather than relying on the generated stub's default. Authentication and permission are checked at the gRPC
 * edge.
 */
@GrpcService
public class CommentReactionResolver extends CommentReactionServiceGrpc.CommentReactionServiceImplBase {

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void listCommentReactions(ListCommentReactionsRequest request, StreamObserver<ListCommentReactionsResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void getCommentReaction(GetCommentReactionRequest request, StreamObserver<GetCommentReactionResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void createCommentReaction(CreateCommentReactionRequest request, StreamObserver<CreateCommentReactionResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void updateCommentReaction(UpdateCommentReactionRequest request, StreamObserver<UpdateCommentReactionResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void deleteCommentReaction(DeleteCommentReactionRequest request, StreamObserver<DeleteCommentReactionResponse> out) {
        throw new NotImplementedException();
    }
}
