package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.hrms.payroll.api.model.PaymentType;
import com.mysms.hrms.payroll.api.model.PaymentTypeUpdate;
import com.mysms.hrms.payroll.business.PaymentTypeService;
import com.mysms.payroll.v1.CreatePaymentTypeRequest;
import com.mysms.payroll.v1.CreatePaymentTypeResponse;
import com.mysms.payroll.v1.DeletePaymentTypeRequest;
import com.mysms.payroll.v1.DeletePaymentTypeResponse;
import com.mysms.payroll.v1.GetPaymentTypeRequest;
import com.mysms.payroll.v1.GetPaymentTypeResponse;
import com.mysms.payroll.v1.ListPaymentTypesRequest;
import com.mysms.payroll.v1.ListPaymentTypesResponse;
import com.mysms.payroll.v1.PaymentTypeServiceGrpc;
import com.mysms.payroll.v1.UpdatePaymentTypeRequest;
import com.mysms.payroll.v1.UpdatePaymentTypeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.PaymentTypeService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PaymentTypeResolver extends PaymentTypeServiceGrpc.PaymentTypeServiceImplBase {

    private final PaymentTypeService paymentTypeService;
    private final ProtoBridge bridge;

    public PaymentTypeResolver(PaymentTypeService paymentTypeService, ProtoBridge bridge) {
        this.paymentTypeService = paymentTypeService;
        this.bridge = bridge;
    }

    @Override
    public void listPaymentTypes(ListPaymentTypesRequest request, StreamObserver<ListPaymentTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PaymentType> rows = paymentTypeService.listPaymentTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPaymentTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPaymentType(GetPaymentTypeRequest request, StreamObserver<GetPaymentTypeResponse> out) {
        PaymentType found = paymentTypeService.getPaymentType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPaymentTypeResponse.newBuilder(), "payment_type", found).build());
    }

    @Override
    public void createPaymentType(CreatePaymentTypeRequest request, StreamObserver<CreatePaymentTypeResponse> out) {
        PaymentType created = paymentTypeService.createPaymentType(
                bridge.body(request.getPaymentType(), PaymentType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePaymentTypeResponse.newBuilder(), "payment_type", created).build());
    }

    @Override
    public void updatePaymentType(UpdatePaymentTypeRequest request, StreamObserver<UpdatePaymentTypeResponse> out) {
        PaymentType updated = paymentTypeService.updatePaymentType(
                bridge.id(request.getPaymentType().getId(), "id"),
                bridge.requiredIfMatch(request.getPaymentType().getEtag()),
                bridge.changes(request.getPaymentType(), request.getUpdateMask(), PaymentTypeUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePaymentTypeResponse.newBuilder(), "payment_type", updated).build());
    }

    @Override
    public void deletePaymentType(DeletePaymentTypeRequest request, StreamObserver<DeletePaymentTypeResponse> out) {
        paymentTypeService.deletePaymentType(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePaymentTypeResponse.getDefaultInstance());
    }
}
