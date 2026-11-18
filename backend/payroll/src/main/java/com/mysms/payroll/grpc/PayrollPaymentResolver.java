package com.mysms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.payroll.api.model.PayrollPayment;
import com.mysms.payroll.business.PayrollPaymentService;
import com.mysms.payroll.v1.CreatePayrollPaymentRequest;
import com.mysms.payroll.v1.CreatePayrollPaymentResponse;
import com.mysms.payroll.v1.GetPayrollPaymentRequest;
import com.mysms.payroll.v1.GetPayrollPaymentResponse;
import com.mysms.payroll.v1.ListPayrollPaymentsRequest;
import com.mysms.payroll.v1.ListPayrollPaymentsResponse;
import com.mysms.payroll.v1.PayrollPaymentServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.PayrollPaymentService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PayrollPaymentResolver extends PayrollPaymentServiceGrpc.PayrollPaymentServiceImplBase {

    private final PayrollPaymentService payrollPaymentService;
    private final ProtoBridge bridge;

    public PayrollPaymentResolver(PayrollPaymentService payrollPaymentService, ProtoBridge bridge) {
        this.payrollPaymentService = payrollPaymentService;
        this.bridge = bridge;
    }

    @Override
    public void listPayrollPayments(ListPayrollPaymentsRequest request,
            StreamObserver<ListPayrollPaymentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PayrollPayment> rows = payrollPaymentService.listPayrollPayments(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPayrollPaymentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPayrollPayment(GetPayrollPaymentRequest request, StreamObserver<GetPayrollPaymentResponse> out) {
        PayrollPayment found = payrollPaymentService.getPayrollPayment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPayrollPaymentResponse.newBuilder(), "payroll_payment", found).build());
    }

    @Override
    public void createPayrollPayment(CreatePayrollPaymentRequest request,
            StreamObserver<CreatePayrollPaymentResponse> out) {
        PayrollPayment created = payrollPaymentService.createPayrollPayment(
                bridge.body(request.getPayrollPayment(), PayrollPayment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePayrollPaymentResponse.newBuilder(),
                "payroll_payment", created).build());
    }
}
