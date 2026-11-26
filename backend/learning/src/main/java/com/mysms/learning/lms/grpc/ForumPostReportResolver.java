package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ForumPostReport;
import com.mysms.learning.lms.api.model.ForumPostReportUpdate;
import com.mysms.learning.lms.business.ForumPostReportService;
import com.mysms.lms.v1.CreateForumPostReportRequest;
import com.mysms.lms.v1.CreateForumPostReportResponse;
import com.mysms.lms.v1.DeleteForumPostReportRequest;
import com.mysms.lms.v1.DeleteForumPostReportResponse;
import com.mysms.lms.v1.ForumPostReportServiceGrpc;
import com.mysms.lms.v1.GetForumPostReportRequest;
import com.mysms.lms.v1.GetForumPostReportResponse;
import com.mysms.lms.v1.ListForumPostReportsRequest;
import com.mysms.lms.v1.ListForumPostReportsResponse;
import com.mysms.lms.v1.UpdateForumPostReportRequest;
import com.mysms.lms.v1.UpdateForumPostReportResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ForumPostReportService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ForumPostReportResolver extends ForumPostReportServiceGrpc.ForumPostReportServiceImplBase {

    private final ForumPostReportService service;
    private final ProtoBridge bridge;

    public ForumPostReportResolver(ForumPostReportService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listForumPostReports(ListForumPostReportsRequest request,
            StreamObserver<ListForumPostReportsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ForumPostReport> rows = service.listForumPostReports(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListForumPostReportsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getForumPostReport(GetForumPostReportRequest request, StreamObserver<GetForumPostReportResponse> out) {
        ForumPostReport found = service.getForumPostReport(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetForumPostReportResponse.newBuilder(), "forum_post_report", found).build());
    }

    @Override
    public void createForumPostReport(CreateForumPostReportRequest request,
            StreamObserver<CreateForumPostReportResponse> out) {
        ForumPostReport created = service.createForumPostReport(
                bridge.body(request.getForumPostReport(), ForumPostReport.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateForumPostReportResponse.newBuilder(), "forum_post_report", created).build());
    }

    @Override
    public void updateForumPostReport(UpdateForumPostReportRequest request,
            StreamObserver<UpdateForumPostReportResponse> out) {
        ForumPostReport updated = service.updateForumPostReport(
                bridge.id(request.getForumPostReport().getId(), "id"),
                bridge.requiredIfMatch(request.getForumPostReport().getEtag()),
                bridge.changes(request.getForumPostReport(), request.getUpdateMask(), ForumPostReportUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateForumPostReportResponse.newBuilder(), "forum_post_report", updated).build());
    }

    @Override
    public void deleteForumPostReport(DeleteForumPostReportRequest request,
            StreamObserver<DeleteForumPostReportResponse> out) {
        service.deleteForumPostReport(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteForumPostReportResponse.getDefaultInstance());
    }
}
