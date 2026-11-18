package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.GradebookEntry;
import com.mysms.lms.business.GradebookEntryService;
import com.mysms.lms.business.GradebookGradingService;
import com.mysms.lms.v1.BatchGradeEntriesRequest;
import com.mysms.lms.v1.BatchGradeEntriesResponse;
import com.mysms.lms.v1.GetGradebookEntryRequest;
import com.mysms.lms.v1.GetGradebookEntryResponse;
import com.mysms.lms.v1.GradeEntryRequest;
import com.mysms.lms.v1.GradeEntryResponse;
import com.mysms.lms.v1.GradebookEntryServiceGrpc;
import com.mysms.lms.v1.ListGradebookEntriesRequest;
import com.mysms.lms.v1.ListGradebookEntriesResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.GradebookEntryService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class GradebookEntryResolver extends GradebookEntryServiceGrpc.GradebookEntryServiceImplBase {

    private final GradebookEntryService service;
    private final ProtoBridge bridge;
    private final GradebookGradingService grading;

    public GradebookEntryResolver(GradebookEntryService service, ProtoBridge bridge, GradebookGradingService grading) {
        this.grading = grading;
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listGradebookEntries(ListGradebookEntriesRequest request, StreamObserver<ListGradebookEntriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<GradebookEntry> rows = service.listGradebookEntrys(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListGradebookEntriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getGradebookEntry(GetGradebookEntryRequest request, StreamObserver<GetGradebookEntryResponse> out) {
        GradebookEntry found = service.getGradebookEntry(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetGradebookEntryResponse.newBuilder(), "gradebook_entry", found).build());
    }

    @Override
    public void gradeEntry(GradeEntryRequest request, StreamObserver<GradeEntryResponse> out) {
        ProtoBridge.send(out, bridge.reply(GradeEntryResponse.newBuilder(), "result",
                grading.gradeEntry(bridge.body(request, com.mysms.lms.api.model.GradeEntryRequest.class, "request_id"))).build());
    }

    @Override
    public void batchGradeEntries(BatchGradeEntriesRequest request, StreamObserver<BatchGradeEntriesResponse> out) {
        ProtoBridge.send(out, bridge.reply(BatchGradeEntriesResponse.newBuilder(), "items",
                grading.batchGradeEntries(bridge.body(request, com.mysms.lms.api.model.BatchGradeEntriesRequest.class,
                        "request_id")).getItems()).build());
    }
}
