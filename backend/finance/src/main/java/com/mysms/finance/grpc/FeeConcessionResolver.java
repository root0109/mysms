package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.FeeConcession;
import com.mysms.finance.api.model.FeeConcessionUpdate;
import com.mysms.finance.business.FeeConcessionService;
import com.mysms.finance.v1.CreateFeeConcessionRequest;
import com.mysms.finance.v1.CreateFeeConcessionResponse;
import com.mysms.finance.v1.DeleteFeeConcessionRequest;
import com.mysms.finance.v1.DeleteFeeConcessionResponse;
import com.mysms.finance.v1.FeeConcessionServiceGrpc;
import com.mysms.finance.v1.GetFeeConcessionRequest;
import com.mysms.finance.v1.GetFeeConcessionResponse;
import com.mysms.finance.v1.ListFeeConcessionsRequest;
import com.mysms.finance.v1.ListFeeConcessionsResponse;
import com.mysms.finance.v1.UpdateFeeConcessionRequest;
import com.mysms.finance.v1.UpdateFeeConcessionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.FeeConcessionService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class FeeConcessionResolver extends FeeConcessionServiceGrpc.FeeConcessionServiceImplBase {

    private final FeeConcessionService service;
    private final ProtoBridge bridge;

    public FeeConcessionResolver(FeeConcessionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFeeConcessions(ListFeeConcessionsRequest request, StreamObserver<ListFeeConcessionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FeeConcession> rows = service.listFeeConcessions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFeeConcessionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFeeConcession(GetFeeConcessionRequest request, StreamObserver<GetFeeConcessionResponse> out) {
        FeeConcession found = service.getFeeConcession(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFeeConcessionResponse.newBuilder(), "fee_concession", found).build());
    }

    @Override
    public void createFeeConcession(CreateFeeConcessionRequest request,
            StreamObserver<CreateFeeConcessionResponse> out) {
        FeeConcession created = service.createFeeConcession(
                bridge.body(request.getFeeConcession(), FeeConcession.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFeeConcessionResponse.newBuilder(), "fee_concession",
                created).build());
    }

    @Override
    public void updateFeeConcession(UpdateFeeConcessionRequest request,
            StreamObserver<UpdateFeeConcessionResponse> out) {
        FeeConcession updated = service.updateFeeConcession(
                bridge.id(request.getFeeConcession().getId(), "id"),
                bridge.requiredIfMatch(request.getFeeConcession().getEtag()),
                bridge.changes(request.getFeeConcession(), request.getUpdateMask(), FeeConcessionUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFeeConcessionResponse.newBuilder(), "fee_concession",
                updated).build());
    }

    @Override
    public void deleteFeeConcession(DeleteFeeConcessionRequest request,
            StreamObserver<DeleteFeeConcessionResponse> out) {
        service.deleteFeeConcession(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFeeConcessionResponse.getDefaultInstance());
    }
}
