package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.FeeStatement;
import com.mysms.finance.api.model.Invoice;
import com.mysms.finance.api.model.InvoiceLine;
import com.mysms.finance.api.model.InvoiceUpdate;
import com.mysms.finance.api.model.SendResult;
import com.mysms.finance.business.InvoiceService;
import com.mysms.finance.v1.CancelInvoiceRequest;
import com.mysms.finance.v1.CancelInvoiceResponse;
import com.mysms.finance.v1.CreateInvoiceFromDuesRequest;
import com.mysms.finance.v1.CreateInvoiceFromDuesResponse;
import com.mysms.finance.v1.CreateInvoiceRequest;
import com.mysms.finance.v1.CreateInvoiceResponse;
import com.mysms.finance.v1.GetFeeStatementRequest;
import com.mysms.finance.v1.GetFeeStatementResponse;
import com.mysms.finance.v1.GetInvoiceLineRequest;
import com.mysms.finance.v1.GetInvoiceLineResponse;
import com.mysms.finance.v1.GetInvoiceRequest;
import com.mysms.finance.v1.GetInvoiceResponse;
import com.mysms.finance.v1.InvoiceServiceGrpc;
import com.mysms.finance.v1.ListInvoiceLinesRequest;
import com.mysms.finance.v1.ListInvoiceLinesResponse;
import com.mysms.finance.v1.ListInvoicesRequest;
import com.mysms.finance.v1.ListInvoicesResponse;
import com.mysms.finance.v1.PostInvoiceRequest;
import com.mysms.finance.v1.PostInvoiceResponse;
import com.mysms.finance.v1.SendFeeRemindersRequest;
import com.mysms.finance.v1.SendFeeRemindersResponse;
import com.mysms.finance.v1.UpdateInvoiceRequest;
import com.mysms.finance.v1.UpdateInvoiceResponse;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.InvoiceService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge. A workflow's etag travels
 * inside its body DTO, as on REST.
 */
@GrpcService
public class InvoiceResolver extends InvoiceServiceGrpc.InvoiceServiceImplBase {

    private final InvoiceService service;
    private final ProtoBridge bridge;

    public InvoiceResolver(InvoiceService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listInvoices(ListInvoicesRequest request, StreamObserver<ListInvoicesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Invoice> rows = service.listInvoices(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListInvoicesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getInvoice(GetInvoiceRequest request, StreamObserver<GetInvoiceResponse> out) {
        Invoice found = service.getInvoice(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetInvoiceResponse.newBuilder(), "invoice", found).build());
    }

    @Override
    public void createInvoice(CreateInvoiceRequest request, StreamObserver<CreateInvoiceResponse> out) {
        Invoice created = service.createInvoice(
                bridge.body(request.getInvoice(), Invoice.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateInvoiceResponse.newBuilder(), "invoice", created).build());
    }

    @Override
    public void updateInvoice(UpdateInvoiceRequest request, StreamObserver<UpdateInvoiceResponse> out) {
        Invoice updated = service.updateInvoice(
                bridge.id(request.getInvoice().getId(), "id"),
                bridge.requiredIfMatch(request.getInvoice().getEtag()),
                bridge.changes(request.getInvoice(), request.getUpdateMask(), InvoiceUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateInvoiceResponse.newBuilder(), "invoice", updated).build());
    }

    @Override
    public void listInvoiceLines(ListInvoiceLinesRequest request, StreamObserver<ListInvoiceLinesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<InvoiceLine> rows = service.listInvoiceLines(bridge.id(request.getInvoiceId(), "invoiceId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListInvoiceLinesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getInvoiceLine(GetInvoiceLineRequest request, StreamObserver<GetInvoiceLineResponse> out) {
        InvoiceLine found = service.getInvoiceLine(bridge.id(request.getInvoiceId(), "invoiceId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetInvoiceLineResponse.newBuilder(), "invoice_line", found).build());
    }

    @Override
    public void createInvoiceFromDues(CreateInvoiceFromDuesRequest request,
            StreamObserver<CreateInvoiceFromDuesResponse> out) {
        Invoice created = service.createInvoiceFromDues(
                bridge.body(request, com.mysms.finance.api.model.CreateInvoiceFromDuesRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(CreateInvoiceFromDuesResponse.newBuilder(), "invoice", created).build());
    }

    @Override
    public void postInvoice(PostInvoiceRequest request, StreamObserver<PostInvoiceResponse> out) {
        Invoice changed = service.postInvoice(bridge.id(request.getInvoiceId(), "invoiceId"),
                bridge.body(request, com.mysms.finance.api.model.PostInvoiceRequest.class, "invoice_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(PostInvoiceResponse.newBuilder(), "invoice", changed).build());
    }

    @Override
    public void cancelInvoice(CancelInvoiceRequest request, StreamObserver<CancelInvoiceResponse> out) {
        Invoice changed = service.cancelInvoice(bridge.id(request.getInvoiceId(), "invoiceId"),
                bridge.body(request, com.mysms.finance.api.model.CancelInvoiceRequest.class, "invoice_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelInvoiceResponse.newBuilder(), "invoice", changed).build());
    }

    @Override
    public void sendFeeReminders(SendFeeRemindersRequest request, StreamObserver<SendFeeRemindersResponse> out) {
        SendResult sent = service.sendFeeReminders(
                bridge.body(request, com.mysms.finance.api.model.SendFeeRemindersRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(SendFeeRemindersResponse.newBuilder(), "result", sent).build());
    }

    /** finance.GetFeeStatement: the year is REST's optional {@code academicYearId} query parameter. */
    @Override
    public void getFeeStatement(GetFeeStatementRequest request, StreamObserver<GetFeeStatementResponse> out) {
        String year = bridge.orNull(request.getAcademicYearId());
        UUID academicYearId = year == null ? null : bridge.id(year, "academicYearId");
        FeeStatement statement = service.getFeeStatement(bridge.id(request.getStudentId(), "studentId"),
                academicYearId);
        ProtoBridge.send(out, bridge.reply(GetFeeStatementResponse.newBuilder(), "result", statement).build());
    }
}
