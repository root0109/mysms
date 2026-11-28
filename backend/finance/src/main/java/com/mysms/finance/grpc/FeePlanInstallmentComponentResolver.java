package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.FeePlanInstallmentComponent;
import com.mysms.finance.business.FeePlanInstallmentComponentService;
import com.mysms.finance.v1.CreateFeePlanInstallmentComponentRequest;
import com.mysms.finance.v1.CreateFeePlanInstallmentComponentResponse;
import com.mysms.finance.v1.DeleteFeePlanInstallmentComponentRequest;
import com.mysms.finance.v1.DeleteFeePlanInstallmentComponentResponse;
import com.mysms.finance.v1.FeePlanInstallmentComponentServiceGrpc;
import com.mysms.finance.v1.FeePlanInstallmentComponentUpdate;
import com.mysms.finance.v1.GetFeePlanInstallmentComponentRequest;
import com.mysms.finance.v1.GetFeePlanInstallmentComponentResponse;
import com.mysms.finance.v1.ListFeePlanInstallmentComponentsRequest;
import com.mysms.finance.v1.ListFeePlanInstallmentComponentsResponse;
import com.mysms.finance.v1.UpdateFeePlanInstallmentComponentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.FeePlanInstallmentComponentService: delegates every RPC to the business service REST uses,
 * so the rules exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class FeePlanInstallmentComponentResolver
        extends FeePlanInstallmentComponentServiceGrpc.FeePlanInstallmentComponentServiceImplBase {

    private final FeePlanInstallmentComponentService service;
    private final ProtoBridge bridge;

    public FeePlanInstallmentComponentResolver(FeePlanInstallmentComponentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFeePlanInstallmentComponents(ListFeePlanInstallmentComponentsRequest request,
            StreamObserver<ListFeePlanInstallmentComponentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FeePlanInstallmentComponent> rows = service.listFeePlanInstallmentComponents(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFeePlanInstallmentComponentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFeePlanInstallmentComponent(GetFeePlanInstallmentComponentRequest request,
            StreamObserver<GetFeePlanInstallmentComponentResponse> out) {
        FeePlanInstallmentComponent found = service.getFeePlanInstallmentComponent(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFeePlanInstallmentComponentResponse.newBuilder(),
                "fee_plan_installment_component", found).build());
    }

    @Override
    public void createFeePlanInstallmentComponent(CreateFeePlanInstallmentComponentRequest request,
            StreamObserver<CreateFeePlanInstallmentComponentResponse> out) {
        FeePlanInstallmentComponent created = service.createFeePlanInstallmentComponent(
                bridge.body(request.getFeePlanInstallmentComponent(), FeePlanInstallmentComponent.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFeePlanInstallmentComponentResponse.newBuilder(),
                "fee_plan_installment_component", created).build());
    }

    @Override
    public void updateFeePlanInstallmentComponent(FeePlanInstallmentComponentUpdate request,
            StreamObserver<UpdateFeePlanInstallmentComponentResponse> out) {
        FeePlanInstallmentComponent updated = service.updateFeePlanInstallmentComponent(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                FeePlanInstallmentComponentProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFeePlanInstallmentComponentResponse.newBuilder(),
                "fee_plan_installment_component", updated).build());
    }

    @Override
    public void deleteFeePlanInstallmentComponent(DeleteFeePlanInstallmentComponentRequest request,
            StreamObserver<DeleteFeePlanInstallmentComponentResponse> out) {
        service.deleteFeePlanInstallmentComponent(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFeePlanInstallmentComponentResponse.getDefaultInstance());
    }
}
