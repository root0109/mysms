package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.PlagiarismWorkflowService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.PlagiarismReport;
import com.mysms.learning.lms.business.PlagiarismReportService;
import com.mysms.lms.v1.GetPlagiarismReportLinkRequest;
import com.mysms.lms.v1.GetPlagiarismReportLinkResponse;
import com.mysms.lms.v1.GetPlagiarismReportRequest;
import com.mysms.lms.v1.GetPlagiarismReportResponse;
import com.mysms.lms.v1.ListPlagiarismReportsRequest;
import com.mysms.lms.v1.ListPlagiarismReportsResponse;
import com.mysms.lms.v1.PlagiarismReportServiceGrpc;
import com.mysms.lms.v1.ReceivePlagiarismWebhookRequest;
import com.mysms.lms.v1.ReceivePlagiarismWebhookResponse;
import com.mysms.lms.v1.SubmitForPlagiarismCheckRequest;
import com.mysms.lms.v1.SubmitForPlagiarismCheckResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.PlagiarismReportService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PlagiarismReportResolver extends PlagiarismReportServiceGrpc.PlagiarismReportServiceImplBase {

    private final PlagiarismReportService service;
    private final ProtoBridge bridge;
    private final PlagiarismWorkflowService workflows;

    public PlagiarismReportResolver(PlagiarismReportService service, ProtoBridge bridge,
            PlagiarismWorkflowService workflows) {
        this.workflows = workflows;
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPlagiarismReports(ListPlagiarismReportsRequest request,
            StreamObserver<ListPlagiarismReportsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PlagiarismReport> rows = service.listPlagiarismReports(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPlagiarismReportsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPlagiarismReport(GetPlagiarismReportRequest request,
            StreamObserver<GetPlagiarismReportResponse> out) {
        PlagiarismReport found = service.getPlagiarismReport(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetPlagiarismReportResponse.newBuilder(), "plagiarism_report", found).build());
    }

    @Override
    public void submitForPlagiarismCheck(SubmitForPlagiarismCheckRequest request,
            StreamObserver<SubmitForPlagiarismCheckResponse> out) {
        ProtoBridge.send(out, bridge.reply(SubmitForPlagiarismCheckResponse.newBuilder(), "result",
                workflows.submitForPlagiarismCheck(bridge.body(request,
                        com.mysms.learning.lms.api.model.SubmitForPlagiarismCheckRequest.class, "request_id"))).build());
    }

    @Override
    public void getPlagiarismReportLink(GetPlagiarismReportLinkRequest request,
            StreamObserver<GetPlagiarismReportLinkResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetPlagiarismReportLinkResponse.newBuilder(), "result",
                workflows.getPlagiarismReportLink(bridge.id(request.getPlagiarismReportId(), "plagiarismReportId"),
                        request.hasView() ? request.getView().name().substring("VIEW_".length()).toLowerCase() : null)).build());
    }

    /** A provider webhook with a signed raw body: REST only; UNIMPLEMENTED over gRPC. */
    @Override
    public void receivePlagiarismWebhook(ReceivePlagiarismWebhookRequest request,
            StreamObserver<ReceivePlagiarismWebhookResponse> out) {
        workflows.receivePlagiarismWebhook(request.getProvider(), request.getHeadersOrDefault("x-signature", null),
                request.getHeadersOrDefault("x-timestamp", null), request.getRawBody().toByteArray());
        ProtoBridge.send(out, ReceivePlagiarismWebhookResponse.getDefaultInstance());
    }
}
