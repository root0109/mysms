package com.mysms.assessment.grpc;

import com.mysms.assessment.api.model.AssignmentType;
import com.mysms.assessment.api.model.AssignmentTypeUpdate;
import com.mysms.assessment.business.AssignmentTypeService;
import com.mysms.assessment.v1.AssignmentTypeServiceGrpc;
import com.mysms.assessment.v1.CreateAssignmentTypeRequest;
import com.mysms.assessment.v1.CreateAssignmentTypeResponse;
import com.mysms.assessment.v1.DeleteAssignmentTypeRequest;
import com.mysms.assessment.v1.DeleteAssignmentTypeResponse;
import com.mysms.assessment.v1.GetAssignmentTypeRequest;
import com.mysms.assessment.v1.GetAssignmentTypeResponse;
import com.mysms.assessment.v1.ListAssignmentTypesRequest;
import com.mysms.assessment.v1.ListAssignmentTypesResponse;
import com.mysms.assessment.v1.UpdateAssignmentTypeRequest;
import com.mysms.assessment.v1.UpdateAssignmentTypeResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.AssignmentTypeService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AssignmentTypeResolver extends AssignmentTypeServiceGrpc.AssignmentTypeServiceImplBase {

    private final AssignmentTypeService assignmentTypeService;
    private final ProtoBridge bridge;

    public AssignmentTypeResolver(AssignmentTypeService assignmentTypeService, ProtoBridge bridge) {
        this.assignmentTypeService = assignmentTypeService;
        this.bridge = bridge;
    }

    @Override
    public void listAssignmentTypes(ListAssignmentTypesRequest request,
            StreamObserver<ListAssignmentTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AssignmentType> rows = assignmentTypeService.listAssignmentTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAssignmentTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssignmentType(GetAssignmentTypeRequest request, StreamObserver<GetAssignmentTypeResponse> out) {
        AssignmentType found = assignmentTypeService.getAssignmentType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssignmentTypeResponse.newBuilder(), "assignment_type", found).build());
    }

    @Override
    public void createAssignmentType(CreateAssignmentTypeRequest request,
            StreamObserver<CreateAssignmentTypeResponse> out) {
        AssignmentType created = assignmentTypeService.createAssignmentType(
                bridge.body(request.getAssignmentType(), AssignmentType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssignmentTypeResponse.newBuilder(),
                "assignment_type", created).build());
    }

    @Override
    public void updateAssignmentType(UpdateAssignmentTypeRequest request,
            StreamObserver<UpdateAssignmentTypeResponse> out) {
        AssignmentType updated = assignmentTypeService.updateAssignmentType(
                bridge.id(request.getAssignmentType().getId(), "id"),
                bridge.requiredIfMatch(request.getAssignmentType().getEtag()),
                bridge.changes(request.getAssignmentType(), request.getUpdateMask(), AssignmentTypeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssignmentTypeResponse.newBuilder(),
                "assignment_type", updated).build());
    }

    @Override
    public void deleteAssignmentType(DeleteAssignmentTypeRequest request,
            StreamObserver<DeleteAssignmentTypeResponse> out) {
        assignmentTypeService.deleteAssignmentType(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssignmentTypeResponse.getDefaultInstance());
    }
}
