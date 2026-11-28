package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.hrms.payroll.api.model.PayComponent;
import com.mysms.hrms.payroll.business.PayComponentService;
import com.mysms.payroll.v1.CreatePayComponentRequest;
import com.mysms.payroll.v1.CreatePayComponentResponse;
import com.mysms.payroll.v1.DeletePayComponentRequest;
import com.mysms.payroll.v1.DeletePayComponentResponse;
import com.mysms.payroll.v1.GetPayComponentRequest;
import com.mysms.payroll.v1.GetPayComponentResponse;
import com.mysms.payroll.v1.ListPayComponentsRequest;
import com.mysms.payroll.v1.ListPayComponentsResponse;
import com.mysms.payroll.v1.PayComponentServiceGrpc;
import com.mysms.payroll.v1.PayComponentUpdate;
import com.mysms.payroll.v1.UpdatePayComponentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.PayComponentService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PayComponentResolver extends PayComponentServiceGrpc.PayComponentServiceImplBase {

    private final PayComponentService payComponentService;
    private final ProtoBridge bridge;

    public PayComponentResolver(PayComponentService payComponentService, ProtoBridge bridge) {
        this.payComponentService = payComponentService;
        this.bridge = bridge;
    }

    @Override
    public void listPayComponents(ListPayComponentsRequest request, StreamObserver<ListPayComponentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PayComponent> rows = payComponentService.listPayComponents(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPayComponentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPayComponent(GetPayComponentRequest request, StreamObserver<GetPayComponentResponse> out) {
        PayComponent found = payComponentService.getPayComponent(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPayComponentResponse.newBuilder(), "pay_component", found).build());
    }

    @Override
    public void createPayComponent(CreatePayComponentRequest request,
            StreamObserver<CreatePayComponentResponse> out) {
        PayComponent created = payComponentService.createPayComponent(
                bridge.body(request.getPayComponent(), PayComponent.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePayComponentResponse.newBuilder(),
                "pay_component", created).build());
    }

    @Override
    public void updatePayComponent(PayComponentUpdate request,
            StreamObserver<UpdatePayComponentResponse> out) {
        PayComponent updated = payComponentService.updatePayComponent(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                PayComponentProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePayComponentResponse.newBuilder(),
                "pay_component", updated).build());
    }

    @Override
    public void deletePayComponent(DeletePayComponentRequest request,
            StreamObserver<DeletePayComponentResponse> out) {
        payComponentService.deletePayComponent(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePayComponentResponse.getDefaultInstance());
    }
}
