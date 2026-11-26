package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.SaasPlan;
import com.mysms.platform.api.model.SaasPlanUpdate;
import com.mysms.platform.business.SaasPlanService;
import com.mysms.platform.v1.CreateSaasPlanRequest;
import com.mysms.platform.v1.CreateSaasPlanResponse;
import com.mysms.platform.v1.DeleteSaasPlanRequest;
import com.mysms.platform.v1.DeleteSaasPlanResponse;
import com.mysms.platform.v1.GetSaasPlanRequest;
import com.mysms.platform.v1.GetSaasPlanResponse;
import com.mysms.platform.v1.ListSaasPlansRequest;
import com.mysms.platform.v1.ListSaasPlansResponse;
import com.mysms.platform.v1.SaasPlanServiceGrpc;
import com.mysms.platform.v1.UpdateSaasPlanRequest;
import com.mysms.platform.v1.UpdateSaasPlanResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.SaasPlanService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SaasPlanResolver extends SaasPlanServiceGrpc.SaasPlanServiceImplBase {

    private final SaasPlanService service;
    private final ProtoBridge bridge;

    public SaasPlanResolver(SaasPlanService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSaasPlans(ListSaasPlansRequest request, StreamObserver<ListSaasPlansResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SaasPlan> rows = service.listSaasPlans(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSaasPlansResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSaasPlan(GetSaasPlanRequest request, StreamObserver<GetSaasPlanResponse> out) {
        SaasPlan found = service.getSaasPlan(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSaasPlanResponse.newBuilder(), "saas_plan", found).build());
    }

    @Override
    public void createSaasPlan(CreateSaasPlanRequest request, StreamObserver<CreateSaasPlanResponse> out) {
        SaasPlan created = service.createSaasPlan(
                bridge.body(request.getSaasPlan(), SaasPlan.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSaasPlanResponse.newBuilder(), "saas_plan", created).build());
    }

    @Override
    public void updateSaasPlan(UpdateSaasPlanRequest request, StreamObserver<UpdateSaasPlanResponse> out) {
        SaasPlan updated = service.updateSaasPlan(
                bridge.id(request.getSaasPlan().getId(), "id"),
                bridge.requiredIfMatch(request.getSaasPlan().getEtag()),
                bridge.changes(request.getSaasPlan(), request.getUpdateMask(), SaasPlanUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSaasPlanResponse.newBuilder(), "saas_plan", updated).build());
    }

    @Override
    public void deleteSaasPlan(DeleteSaasPlanRequest request, StreamObserver<DeleteSaasPlanResponse> out) {
        service.deleteSaasPlan(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSaasPlanResponse.getDefaultInstance());
    }
}
