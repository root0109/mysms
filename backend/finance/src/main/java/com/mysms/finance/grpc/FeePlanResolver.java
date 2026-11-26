package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.FeePlan;
import com.mysms.finance.api.model.FeePlanInstallment;
import com.mysms.finance.api.model.FeePlanInstallmentUpdate;
import com.mysms.finance.api.model.FeePlanUpdate;
import com.mysms.finance.business.FeePlanService;
import com.mysms.finance.v1.CreateFeePlanInstallmentRequest;
import com.mysms.finance.v1.CreateFeePlanInstallmentResponse;
import com.mysms.finance.v1.CreateFeePlanRequest;
import com.mysms.finance.v1.CreateFeePlanResponse;
import com.mysms.finance.v1.DeleteFeePlanInstallmentRequest;
import com.mysms.finance.v1.DeleteFeePlanInstallmentResponse;
import com.mysms.finance.v1.DeleteFeePlanRequest;
import com.mysms.finance.v1.DeleteFeePlanResponse;
import com.mysms.finance.v1.FeePlanServiceGrpc;
import com.mysms.finance.v1.GetFeePlanInstallmentRequest;
import com.mysms.finance.v1.GetFeePlanInstallmentResponse;
import com.mysms.finance.v1.GetFeePlanRequest;
import com.mysms.finance.v1.GetFeePlanResponse;
import com.mysms.finance.v1.ListFeePlanInstallmentsRequest;
import com.mysms.finance.v1.ListFeePlanInstallmentsResponse;
import com.mysms.finance.v1.ListFeePlansRequest;
import com.mysms.finance.v1.ListFeePlansResponse;
import com.mysms.finance.v1.UpdateFeePlanInstallmentRequest;
import com.mysms.finance.v1.UpdateFeePlanInstallmentResponse;
import com.mysms.finance.v1.UpdateFeePlanRequest;
import com.mysms.finance.v1.UpdateFeePlanResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.FeePlanService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class FeePlanResolver extends FeePlanServiceGrpc.FeePlanServiceImplBase {

    private final FeePlanService service;
    private final ProtoBridge bridge;

    public FeePlanResolver(FeePlanService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFeePlans(ListFeePlansRequest request, StreamObserver<ListFeePlansResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FeePlan> rows = service.listFeePlans(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFeePlansResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFeePlan(GetFeePlanRequest request, StreamObserver<GetFeePlanResponse> out) {
        FeePlan found = service.getFeePlan(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFeePlanResponse.newBuilder(), "fee_plan", found).build());
    }

    @Override
    public void createFeePlan(CreateFeePlanRequest request, StreamObserver<CreateFeePlanResponse> out) {
        FeePlan created = service.createFeePlan(
                bridge.body(request.getFeePlan(), FeePlan.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFeePlanResponse.newBuilder(), "fee_plan", created).build());
    }

    @Override
    public void updateFeePlan(UpdateFeePlanRequest request, StreamObserver<UpdateFeePlanResponse> out) {
        FeePlan updated = service.updateFeePlan(
                bridge.id(request.getFeePlan().getId(), "id"),
                bridge.requiredIfMatch(request.getFeePlan().getEtag()),
                bridge.changes(request.getFeePlan(), request.getUpdateMask(), FeePlanUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFeePlanResponse.newBuilder(), "fee_plan", updated).build());
    }

    @Override
    public void deleteFeePlan(DeleteFeePlanRequest request, StreamObserver<DeleteFeePlanResponse> out) {
        service.deleteFeePlan(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFeePlanResponse.getDefaultInstance());
    }

    @Override
    public void listFeePlanInstallments(ListFeePlanInstallmentsRequest request,
            StreamObserver<ListFeePlanInstallmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FeePlanInstallment> rows = service.listFeePlanInstallments(bridge.id(request.getFeePlanId(), "feePlanId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFeePlanInstallmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFeePlanInstallment(GetFeePlanInstallmentRequest request,
            StreamObserver<GetFeePlanInstallmentResponse> out) {
        FeePlanInstallment found = service.getFeePlanInstallment(bridge.id(request.getFeePlanId(), "feePlanId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFeePlanInstallmentResponse.newBuilder(), "fee_plan_installment",
                found).build());
    }

    @Override
    public void createFeePlanInstallment(CreateFeePlanInstallmentRequest request,
            StreamObserver<CreateFeePlanInstallmentResponse> out) {
        FeePlanInstallment created = service.createFeePlanInstallment(bridge.id(request.getFeePlanId(), "feePlanId"),
                bridge.body(request.getFeePlanInstallment(), FeePlanInstallment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateFeePlanInstallmentResponse.newBuilder(), "fee_plan_installment",
                created).build());
    }

    @Override
    public void updateFeePlanInstallment(UpdateFeePlanInstallmentRequest request,
            StreamObserver<UpdateFeePlanInstallmentResponse> out) {
        FeePlanInstallment updated = service.updateFeePlanInstallment(
                bridge.id(request.getFeePlanId(), "feePlanId"),
                bridge.id(request.getFeePlanInstallment().getId(), "id"),
                bridge.requiredIfMatch(request.getFeePlanInstallment().getEtag()),
                bridge.changes(request.getFeePlanInstallment(), request.getUpdateMask(),
                        FeePlanInstallmentUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateFeePlanInstallmentResponse.newBuilder(), "fee_plan_installment",
                updated).build());
    }

    @Override
    public void deleteFeePlanInstallment(DeleteFeePlanInstallmentRequest request,
            StreamObserver<DeleteFeePlanInstallmentResponse> out) {
        service.deleteFeePlanInstallment(bridge.id(request.getFeePlanId(), "feePlanId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFeePlanInstallmentResponse.getDefaultInstance());
    }
}
