package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.Marksheet;
import com.mysms.academics.assessment.business.MarksheetService;
import com.mysms.assessment.v1.CancelMarksheetRequest;
import com.mysms.assessment.v1.CancelMarksheetResponse;
import com.mysms.assessment.v1.CreateMarksheetRequest;
import com.mysms.assessment.v1.CreateMarksheetResponse;
import com.mysms.assessment.v1.DeleteMarksheetRequest;
import com.mysms.assessment.v1.DeleteMarksheetResponse;
import com.mysms.assessment.v1.GetMarksheetRequest;
import com.mysms.assessment.v1.GetMarksheetResponse;
import com.mysms.assessment.v1.ListMarksheetsRequest;
import com.mysms.assessment.v1.ListMarksheetsResponse;
import com.mysms.assessment.v1.MarksheetServiceGrpc;
import com.mysms.assessment.v1.MarksheetUpdate;
import com.mysms.assessment.v1.PublishMarksheetRequest;
import com.mysms.assessment.v1.PublishMarksheetResponse;
import com.mysms.assessment.v1.ResetMarksheetRequest;
import com.mysms.assessment.v1.ResetMarksheetResponse;
import com.mysms.assessment.v1.UpdateMarksheetResponse;
import com.mysms.assessment.v1.ValidateMarksheetRequest;
import com.mysms.assessment.v1.ValidateMarksheetResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.MarksheetService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MarksheetResolver extends MarksheetServiceGrpc.MarksheetServiceImplBase {

    private final MarksheetService marksheetService;
    private final ProtoBridge bridge;

    public MarksheetResolver(MarksheetService marksheetService, ProtoBridge bridge) {
        this.marksheetService = marksheetService;
        this.bridge = bridge;
    }

    /** The etag is REST's If-Match header here (required), not a property of the body. */
    @Override
    public void publishMarksheet(PublishMarksheetRequest request, StreamObserver<PublishMarksheetResponse> out) {
        Marksheet changed = marksheetService.publishMarksheet(
                bridge.id(request.getMarksheetId(), "marksheetId"), bridge.requiredIfMatch(request.getEtag()),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.PublishMarksheetRequest.class, "marksheet_id", "etag"));
        ProtoBridge.send(out, bridge.reply(PublishMarksheetResponse.newBuilder(), "marksheet", changed).build());
    }

    @Override
    public void listMarksheets(ListMarksheetsRequest request, StreamObserver<ListMarksheetsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Marksheet> rows = marksheetService.listMarksheets(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMarksheetsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMarksheet(GetMarksheetRequest request, StreamObserver<GetMarksheetResponse> out) {
        Marksheet found = marksheetService.getMarksheet(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMarksheetResponse.newBuilder(), "marksheet", found).build());
    }

    @Override
    public void createMarksheet(CreateMarksheetRequest request, StreamObserver<CreateMarksheetResponse> out) {
        Marksheet created = marksheetService.createMarksheet(
                bridge.body(request.getMarksheet(), Marksheet.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMarksheetResponse.newBuilder(), "marksheet", created).build());
    }

    @Override
    public void updateMarksheet(MarksheetUpdate request, StreamObserver<UpdateMarksheetResponse> out) {
        Marksheet updated = marksheetService.updateMarksheet(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                MarksheetProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMarksheetResponse.newBuilder(), "marksheet", updated).build());
    }

    @Override
    public void deleteMarksheet(DeleteMarksheetRequest request, StreamObserver<DeleteMarksheetResponse> out) {
        marksheetService.deleteMarksheet(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMarksheetResponse.getDefaultInstance());
    }

    @Override
    public void validateMarksheet(ValidateMarksheetRequest request, StreamObserver<ValidateMarksheetResponse> out) {
        Marksheet changed = marksheetService.validateMarksheet(
                bridge.id(request.getMarksheetId(), "marksheetId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ValidateMarksheetRequest.class,
                        "marksheet_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ValidateMarksheetResponse.newBuilder(), "marksheet", changed).build());
    }

    @Override
    public void cancelMarksheet(CancelMarksheetRequest request, StreamObserver<CancelMarksheetResponse> out) {
        Marksheet changed = marksheetService.cancelMarksheet(
                bridge.id(request.getMarksheetId(), "marksheetId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.CancelMarksheetRequest.class, "marksheet_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelMarksheetResponse.newBuilder(), "marksheet", changed).build());
    }

    @Override
    public void resetMarksheet(ResetMarksheetRequest request, StreamObserver<ResetMarksheetResponse> out) {
        Marksheet changed = marksheetService.resetMarksheet(
                bridge.id(request.getMarksheetId(), "marksheetId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ResetMarksheetRequest.class, "marksheet_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetMarksheetResponse.newBuilder(), "marksheet", changed).build());
    }
}
