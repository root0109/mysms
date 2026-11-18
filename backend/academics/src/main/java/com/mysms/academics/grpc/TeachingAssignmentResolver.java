package com.mysms.academics.grpc;

import com.mysms.academics.api.model.TeachingAssignment;
import com.mysms.academics.api.model.TeachingAssignmentUpdate;
import com.mysms.academics.business.TeachingAssignmentService;
import com.mysms.academics.v1.CreateTeachingAssignmentRequest;
import com.mysms.academics.v1.CreateTeachingAssignmentResponse;
import com.mysms.academics.v1.DeleteTeachingAssignmentRequest;
import com.mysms.academics.v1.DeleteTeachingAssignmentResponse;
import com.mysms.academics.v1.GetTeachingAssignmentRequest;
import com.mysms.academics.v1.GetTeachingAssignmentResponse;
import com.mysms.academics.v1.ListTeachingAssignmentsRequest;
import com.mysms.academics.v1.ListTeachingAssignmentsResponse;
import com.mysms.academics.v1.TeachingAssignmentServiceGrpc;
import com.mysms.academics.v1.UpdateTeachingAssignmentRequest;
import com.mysms.academics.v1.UpdateTeachingAssignmentResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.TeachingAssignmentService: delegates every RPC to the business service REST uses, so the
 * rules exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class TeachingAssignmentResolver extends TeachingAssignmentServiceGrpc.TeachingAssignmentServiceImplBase {

    private final TeachingAssignmentService service;
    private final ProtoBridge bridge;

    public TeachingAssignmentResolver(TeachingAssignmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTeachingAssignments(ListTeachingAssignmentsRequest request,
            StreamObserver<ListTeachingAssignmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TeachingAssignment> rows = service.listTeachingAssignments(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTeachingAssignmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTeachingAssignment(GetTeachingAssignmentRequest request, StreamObserver<GetTeachingAssignmentResponse> out) {
        TeachingAssignment found = service.getTeachingAssignment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTeachingAssignmentResponse.newBuilder(), "teaching_assignment", found).build());
    }

    @Override
    public void createTeachingAssignment(CreateTeachingAssignmentRequest request,
            StreamObserver<CreateTeachingAssignmentResponse> out) {
        TeachingAssignment created = service.createTeachingAssignment(
                bridge.body(request.getTeachingAssignment(), TeachingAssignment.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateTeachingAssignmentResponse.newBuilder(), "teaching_assignment", created).build());
    }

    @Override
    public void updateTeachingAssignment(UpdateTeachingAssignmentRequest request,
            StreamObserver<UpdateTeachingAssignmentResponse> out) {
        TeachingAssignment updated = service.updateTeachingAssignment(bridge.id(request.getTeachingAssignment().getId(), "id"),
                bridge.requiredIfMatch(request.getTeachingAssignment().getEtag()),
                bridge.changes(request.getTeachingAssignment(), request.getUpdateMask(), TeachingAssignmentUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateTeachingAssignmentResponse.newBuilder(), "teaching_assignment", updated).build());
    }

    @Override
    public void deleteTeachingAssignment(DeleteTeachingAssignmentRequest request,
            StreamObserver<DeleteTeachingAssignmentResponse> out) {
        service.deleteTeachingAssignment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTeachingAssignmentResponse.getDefaultInstance());
    }
}
