package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.LegalHold;
import com.mysms.privacy.business.LegalHoldService;
import com.mysms.privacy.v1.GetLegalHoldRequest;
import com.mysms.privacy.v1.GetLegalHoldResponse;
import com.mysms.privacy.v1.LegalHoldServiceGrpc;
import com.mysms.privacy.v1.LegalHoldUpdate;
import com.mysms.privacy.v1.ListActiveHoldsRequest;
import com.mysms.privacy.v1.ListActiveHoldsResponse;
import com.mysms.privacy.v1.ListLegalHoldsRequest;
import com.mysms.privacy.v1.ListLegalHoldsResponse;
import com.mysms.privacy.v1.PlaceLegalHoldRequest;
import com.mysms.privacy.v1.PlaceLegalHoldResponse;
import com.mysms.privacy.v1.ReleaseLegalHoldRequest;
import com.mysms.privacy.v1.ReleaseLegalHoldResponse;
import com.mysms.privacy.v1.UpdateLegalHoldResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.LegalHoldService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge. A release's REST body is its
 * request without the path id and the idempotency key.
 */
@GrpcService
public class LegalHoldResolver extends LegalHoldServiceGrpc.LegalHoldServiceImplBase {

    private static final String LEGAL_HOLD = "legal_hold";

    private final LegalHoldService service;
    private final ProtoBridge bridge;

    public LegalHoldResolver(LegalHoldService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLegalHolds(ListLegalHoldsRequest request, StreamObserver<ListLegalHoldsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LegalHold> rows = service.listLegalHolds(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLegalHoldsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLegalHold(GetLegalHoldRequest request, StreamObserver<GetLegalHoldResponse> out) {
        LegalHold found = service.getLegalHold(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLegalHoldResponse.newBuilder(), LEGAL_HOLD, found).build());
    }

    @Override
    public void placeLegalHold(PlaceLegalHoldRequest request, StreamObserver<PlaceLegalHoldResponse> out) {
        LegalHold placed = service.placeLegalHold(bridge.body(request.getLegalHold(), LegalHold.class));
        ProtoBridge.send(out, bridge.reply(PlaceLegalHoldResponse.newBuilder(), LEGAL_HOLD, placed).build());
    }

    @Override
    public void updateLegalHold(LegalHoldUpdate request, StreamObserver<UpdateLegalHoldResponse> out) {
        LegalHold updated = service.updateLegalHold(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                LegalHoldProtoBridge.changes(bridge, request));
        ProtoBridge.send(out, bridge.reply(UpdateLegalHoldResponse.newBuilder(), LEGAL_HOLD, updated).build());
    }

    @Override
    public void releaseLegalHold(ReleaseLegalHoldRequest request, StreamObserver<ReleaseLegalHoldResponse> out) {
        LegalHold released = service.releaseLegalHold(bridge.id(request.getLegalHoldId(), "legalHoldId"),
                bridge.body(request, com.mysms.privacy.api.model.ReleaseLegalHoldRequest.class, "legal_hold_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ReleaseLegalHoldResponse.newBuilder(), LEGAL_HOLD, released).build());
    }

    @Override
    public void listActiveHolds(ListActiveHoldsRequest request, StreamObserver<ListActiveHoldsResponse> out) {
        com.mysms.privacy.api.model.ListActiveHoldsResponse active = service.listActiveHolds(
                bridge.id(request.getSubjectPersonId(), "subjectPersonId"),
                request.hasModule() ? request.getModule() : null);
        ProtoBridge.send(out, bridge.reply(ListActiveHoldsResponse.newBuilder(), "holds", active.getHolds()).build());
    }
}
