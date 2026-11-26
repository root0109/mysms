package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.hrms.payroll.api.model.Operation;
import com.mysms.hrms.payroll.api.model.Payslip;
import com.mysms.hrms.payroll.api.model.PayslipLine;
import com.mysms.hrms.payroll.business.PayslipService;
import com.mysms.payroll.v1.CancelPayslipRequest;
import com.mysms.payroll.v1.CancelPayslipResponse;
import com.mysms.payroll.v1.GeneratePayslipsRequest;
import com.mysms.payroll.v1.GeneratePayslipsResponse;
import com.mysms.payroll.v1.GetPayslipLineRequest;
import com.mysms.payroll.v1.GetPayslipLineResponse;
import com.mysms.payroll.v1.GetPayslipRequest;
import com.mysms.payroll.v1.GetPayslipResponse;
import com.mysms.payroll.v1.ListPayslipLinesRequest;
import com.mysms.payroll.v1.ListPayslipLinesResponse;
import com.mysms.payroll.v1.ListPayslipsRequest;
import com.mysms.payroll.v1.ListPayslipsResponse;
import com.mysms.payroll.v1.MarkPayslipPaidRequest;
import com.mysms.payroll.v1.MarkPayslipPaidResponse;
import com.mysms.payroll.v1.PayslipServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.PayslipService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PayslipResolver extends PayslipServiceGrpc.PayslipServiceImplBase {

    private final PayslipService payslipService;
    private final ProtoBridge bridge;

    public PayslipResolver(PayslipService payslipService, ProtoBridge bridge) {
        this.payslipService = payslipService;
        this.bridge = bridge;
    }

    @Override
    public void listPayslips(ListPayslipsRequest request, StreamObserver<ListPayslipsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Payslip> rows = payslipService.listPayslips(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPayslipsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPayslip(GetPayslipRequest request, StreamObserver<GetPayslipResponse> out) {
        Payslip found = payslipService.getPayslip(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPayslipResponse.newBuilder(), "payslip", found).build());
    }

    @Override
    public void listPayslipLines(ListPayslipLinesRequest request, StreamObserver<ListPayslipLinesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PayslipLine> rows = payslipService.listPayslipLines(
                bridge.id(request.getPayslipId(), "payslipId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPayslipLinesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPayslipLine(GetPayslipLineRequest request, StreamObserver<GetPayslipLineResponse> out) {
        PayslipLine found = payslipService.getPayslipLine(
                bridge.id(request.getPayslipId(), "payslipId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPayslipLineResponse.newBuilder(), "payslip_line", found).build());
    }

    @Override
    public void generatePayslips(GeneratePayslipsRequest request, StreamObserver<GeneratePayslipsResponse> out) {
        Operation changed = payslipService.generatePayslips(
                bridge.body(request,
                        com.mysms.hrms.payroll.api.model.GeneratePayslipsRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(GeneratePayslipsResponse.newBuilder(), "result", changed).build());
    }

    @Override
    public void markPayslipPaid(MarkPayslipPaidRequest request, StreamObserver<MarkPayslipPaidResponse> out) {
        Payslip changed = payslipService.markPayslipPaid(
                bridge.id(request.getPayslipId(), "payslipId"),
                bridge.body(request,
                        com.mysms.hrms.payroll.api.model.MarkPayslipPaidRequest.class, "payslip_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(MarkPayslipPaidResponse.newBuilder(), "payslip", changed).build());
    }

    @Override
    public void cancelPayslip(CancelPayslipRequest request, StreamObserver<CancelPayslipResponse> out) {
        Payslip changed = payslipService.cancelPayslip(
                bridge.id(request.getPayslipId(), "payslipId"),
                bridge.body(request,
                        com.mysms.hrms.payroll.api.model.CancelPayslipRequest.class, "payslip_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelPayslipResponse.newBuilder(), "payslip", changed).build());
    }
}
