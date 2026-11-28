package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.Purpose;
import com.mysms.privacy.business.PurposeService;
import com.mysms.privacy.v1.GetPurposeRequest;
import com.mysms.privacy.v1.GetPurposeResponse;
import com.mysms.privacy.v1.ListPurposesRequest;
import com.mysms.privacy.v1.ListPurposesResponse;
import com.mysms.privacy.v1.PurposeServiceGrpc;
import com.mysms.privacy.v1.PurposeUpdate;
import com.mysms.privacy.v1.UpdatePurposeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.PurposeService: delegates every RPC to the same business service as REST. Authentication,
 * permission and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PurposeResolver extends PurposeServiceGrpc.PurposeServiceImplBase {

    private final PurposeService service;
    private final ProtoBridge bridge;

    public PurposeResolver(PurposeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPurposes(ListPurposesRequest request, StreamObserver<ListPurposesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Purpose> rows = service.listPurposes(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPurposesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPurpose(GetPurposeRequest request, StreamObserver<GetPurposeResponse> out) {
        Purpose found = service.getPurpose(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPurposeResponse.newBuilder(), "purpose", found).build());
    }

    @Override
    public void updatePurpose(PurposeUpdate request, StreamObserver<UpdatePurposeResponse> out) {
        Purpose updated = service.updatePurpose(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PurposeProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePurposeResponse.newBuilder(), "purpose", updated).build());
    }
}
