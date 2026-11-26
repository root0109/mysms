package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.TimetableEntry;
import com.mysms.learning.lms.api.model.TimetableEntryUpdate;
import com.mysms.learning.lms.business.TimetableEntryService;
import com.mysms.lms.v1.CreateTimetableEntryRequest;
import com.mysms.lms.v1.CreateTimetableEntryResponse;
import com.mysms.lms.v1.DeleteTimetableEntryRequest;
import com.mysms.lms.v1.DeleteTimetableEntryResponse;
import com.mysms.lms.v1.GetTimetableEntryRequest;
import com.mysms.lms.v1.GetTimetableEntryResponse;
import com.mysms.lms.v1.ListTimetableEntriesRequest;
import com.mysms.lms.v1.ListTimetableEntriesResponse;
import com.mysms.lms.v1.TimetableEntryServiceGrpc;
import com.mysms.lms.v1.UpdateTimetableEntryRequest;
import com.mysms.lms.v1.UpdateTimetableEntryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.TimetableEntryService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class TimetableEntryResolver extends TimetableEntryServiceGrpc.TimetableEntryServiceImplBase {

    private final TimetableEntryService service;
    private final ProtoBridge bridge;

    public TimetableEntryResolver(TimetableEntryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTimetableEntries(ListTimetableEntriesRequest request, StreamObserver<ListTimetableEntriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TimetableEntry> rows = service.listTimetableEntrys(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTimetableEntriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTimetableEntry(GetTimetableEntryRequest request, StreamObserver<GetTimetableEntryResponse> out) {
        TimetableEntry found = service.getTimetableEntry(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTimetableEntryResponse.newBuilder(), "timetable_entry", found).build());
    }

    @Override
    public void createTimetableEntry(CreateTimetableEntryRequest request, StreamObserver<CreateTimetableEntryResponse> out) {
        TimetableEntry created = service.createTimetableEntry(bridge.body(request.getTimetableEntry(), TimetableEntry.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateTimetableEntryResponse.newBuilder(), "timetable_entry", created).build());
    }

    @Override
    public void updateTimetableEntry(UpdateTimetableEntryRequest request, StreamObserver<UpdateTimetableEntryResponse> out) {
        TimetableEntry updated = service.updateTimetableEntry(bridge.id(request.getTimetableEntry().getId(), "id"),
                bridge.requiredIfMatch(request.getTimetableEntry().getEtag()),
                bridge.changes(request.getTimetableEntry(), request.getUpdateMask(), TimetableEntryUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateTimetableEntryResponse.newBuilder(), "timetable_entry", updated).build());
    }

    @Override
    public void deleteTimetableEntry(DeleteTimetableEntryRequest request, StreamObserver<DeleteTimetableEntryResponse> out) {
        service.deleteTimetableEntry(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTimetableEntryResponse.getDefaultInstance());
    }
}
