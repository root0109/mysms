package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.LmsAssignment;
import com.mysms.lms.api.model.LmsAssignmentUpdate;
import com.mysms.lms.business.LmsAssignmentService;
import com.mysms.lms.v1.CreateLmsAssignmentRequest;
import com.mysms.lms.v1.CreateLmsAssignmentResponse;
import com.mysms.lms.v1.DeleteLmsAssignmentRequest;
import com.mysms.lms.v1.DeleteLmsAssignmentResponse;
import com.mysms.lms.v1.GetLmsAssignmentRequest;
import com.mysms.lms.v1.GetLmsAssignmentResponse;
import com.mysms.lms.v1.ListLmsAssignmentsRequest;
import com.mysms.lms.v1.ListLmsAssignmentsResponse;
import com.mysms.lms.v1.LmsAssignmentServiceGrpc;
import com.mysms.lms.v1.UpdateLmsAssignmentRequest;
import com.mysms.lms.v1.UpdateLmsAssignmentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.LmsAssignmentService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class LmsAssignmentResolver extends LmsAssignmentServiceGrpc.LmsAssignmentServiceImplBase {

    private final LmsAssignmentService service;
    private final ProtoBridge bridge;

    public LmsAssignmentResolver(LmsAssignmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLmsAssignments(ListLmsAssignmentsRequest request, StreamObserver<ListLmsAssignmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LmsAssignment> rows = service.listLmsAssignments(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLmsAssignmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLmsAssignment(GetLmsAssignmentRequest request, StreamObserver<GetLmsAssignmentResponse> out) {
        LmsAssignment found = service.getLmsAssignment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLmsAssignmentResponse.newBuilder(), "assignment", found).build());
    }

    @Override
    public void createLmsAssignment(CreateLmsAssignmentRequest request, StreamObserver<CreateLmsAssignmentResponse> out) {
        LmsAssignment created = service.createLmsAssignment(bridge.body(request.getAssignment(), LmsAssignment.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLmsAssignmentResponse.newBuilder(), "assignment", created).build());
    }

    @Override
    public void updateLmsAssignment(UpdateLmsAssignmentRequest request, StreamObserver<UpdateLmsAssignmentResponse> out) {
        LmsAssignment updated = service.updateLmsAssignment(bridge.id(request.getAssignment().getId(), "id"),
                bridge.requiredIfMatch(request.getAssignment().getEtag()),
                bridge.changes(request.getAssignment(), request.getUpdateMask(), LmsAssignmentUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLmsAssignmentResponse.newBuilder(), "assignment", updated).build());
    }

    @Override
    public void deleteLmsAssignment(DeleteLmsAssignmentRequest request, StreamObserver<DeleteLmsAssignmentResponse> out) {
        service.deleteLmsAssignment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLmsAssignmentResponse.getDefaultInstance());
    }
}
