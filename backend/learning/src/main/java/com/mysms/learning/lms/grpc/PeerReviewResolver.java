package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.PeerReview;
import com.mysms.learning.lms.api.model.PeerReviewUpdate;
import com.mysms.learning.lms.business.PeerReviewService;
import com.mysms.lms.v1.CreatePeerReviewRequest;
import com.mysms.lms.v1.CreatePeerReviewResponse;
import com.mysms.lms.v1.DeletePeerReviewRequest;
import com.mysms.lms.v1.DeletePeerReviewResponse;
import com.mysms.lms.v1.GetPeerReviewRequest;
import com.mysms.lms.v1.GetPeerReviewResponse;
import com.mysms.lms.v1.ListPeerReviewsRequest;
import com.mysms.lms.v1.ListPeerReviewsResponse;
import com.mysms.lms.v1.PeerReviewServiceGrpc;
import com.mysms.lms.v1.UpdatePeerReviewRequest;
import com.mysms.lms.v1.UpdatePeerReviewResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.PeerReviewService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PeerReviewResolver extends PeerReviewServiceGrpc.PeerReviewServiceImplBase {

    private final PeerReviewService service;
    private final ProtoBridge bridge;

    public PeerReviewResolver(PeerReviewService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPeerReviews(ListPeerReviewsRequest request, StreamObserver<ListPeerReviewsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PeerReview> rows = service.listPeerReviews(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPeerReviewsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPeerReview(GetPeerReviewRequest request, StreamObserver<GetPeerReviewResponse> out) {
        PeerReview found = service.getPeerReview(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPeerReviewResponse.newBuilder(), "peer_review", found).build());
    }

    @Override
    public void createPeerReview(CreatePeerReviewRequest request, StreamObserver<CreatePeerReviewResponse> out) {
        PeerReview created = service.createPeerReview(bridge.body(request.getPeerReview(), PeerReview.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePeerReviewResponse.newBuilder(), "peer_review", created).build());
    }

    @Override
    public void updatePeerReview(UpdatePeerReviewRequest request, StreamObserver<UpdatePeerReviewResponse> out) {
        PeerReview updated = service.updatePeerReview(bridge.id(request.getPeerReview().getId(), "id"),
                bridge.requiredIfMatch(request.getPeerReview().getEtag()),
                bridge.changes(request.getPeerReview(), request.getUpdateMask(), PeerReviewUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePeerReviewResponse.newBuilder(), "peer_review", updated).build());
    }

    @Override
    public void deletePeerReview(DeletePeerReviewRequest request, StreamObserver<DeletePeerReviewResponse> out) {
        service.deletePeerReview(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePeerReviewResponse.getDefaultInstance());
    }
}
