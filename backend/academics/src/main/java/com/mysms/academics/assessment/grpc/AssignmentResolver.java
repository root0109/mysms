package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.Assignment;
import com.mysms.academics.assessment.api.model.AssignmentAllocation;
import com.mysms.academics.assessment.api.model.AssignmentAllocationUpdate;
import com.mysms.academics.assessment.api.model.AssignmentApproval;
import com.mysms.academics.assessment.api.model.AssignmentApprovalUpdate;
import com.mysms.academics.assessment.api.model.AssignmentSubject;
import com.mysms.academics.assessment.api.model.AssignmentSubjectUpdate;
import com.mysms.academics.assessment.api.model.AssignmentUpdate;
import com.mysms.academics.assessment.business.AssignmentService;
import com.mysms.assessment.v1.ApproveAssignmentRequest;
import com.mysms.assessment.v1.ApproveAssignmentResponse;
import com.mysms.assessment.v1.AssignmentServiceGrpc;
import com.mysms.assessment.v1.CancelAssignmentRequest;
import com.mysms.assessment.v1.CancelAssignmentResponse;
import com.mysms.assessment.v1.CreateAssignmentAllocationRequest;
import com.mysms.assessment.v1.CreateAssignmentAllocationResponse;
import com.mysms.assessment.v1.CreateAssignmentApprovalRequest;
import com.mysms.assessment.v1.CreateAssignmentApprovalResponse;
import com.mysms.assessment.v1.CreateAssignmentRequest;
import com.mysms.assessment.v1.CreateAssignmentResponse;
import com.mysms.assessment.v1.CreateAssignmentSubjectRequest;
import com.mysms.assessment.v1.CreateAssignmentSubjectResponse;
import com.mysms.assessment.v1.DeleteAssignmentAllocationRequest;
import com.mysms.assessment.v1.DeleteAssignmentAllocationResponse;
import com.mysms.assessment.v1.DeleteAssignmentApprovalRequest;
import com.mysms.assessment.v1.DeleteAssignmentApprovalResponse;
import com.mysms.assessment.v1.DeleteAssignmentRequest;
import com.mysms.assessment.v1.DeleteAssignmentResponse;
import com.mysms.assessment.v1.DeleteAssignmentSubjectRequest;
import com.mysms.assessment.v1.DeleteAssignmentSubjectResponse;
import com.mysms.assessment.v1.FinishAssignmentRequest;
import com.mysms.assessment.v1.FinishAssignmentResponse;
import com.mysms.assessment.v1.GetAssignmentAllocationRequest;
import com.mysms.assessment.v1.GetAssignmentAllocationResponse;
import com.mysms.assessment.v1.GetAssignmentApprovalRequest;
import com.mysms.assessment.v1.GetAssignmentApprovalResponse;
import com.mysms.assessment.v1.GetAssignmentRequest;
import com.mysms.assessment.v1.GetAssignmentResponse;
import com.mysms.assessment.v1.GetAssignmentSubjectRequest;
import com.mysms.assessment.v1.GetAssignmentSubjectResponse;
import com.mysms.assessment.v1.ListAssignmentAllocationsRequest;
import com.mysms.assessment.v1.ListAssignmentAllocationsResponse;
import com.mysms.assessment.v1.ListAssignmentApprovalsRequest;
import com.mysms.assessment.v1.ListAssignmentApprovalsResponse;
import com.mysms.assessment.v1.ListAssignmentSubjectsRequest;
import com.mysms.assessment.v1.ListAssignmentSubjectsResponse;
import com.mysms.assessment.v1.ListAssignmentsRequest;
import com.mysms.assessment.v1.ListAssignmentsResponse;
import com.mysms.assessment.v1.PublishAssignmentRequest;
import com.mysms.assessment.v1.PublishAssignmentResponse;
import com.mysms.assessment.v1.RejectAssignmentRequest;
import com.mysms.assessment.v1.RejectAssignmentResponse;
import com.mysms.assessment.v1.ResetAssignmentRequest;
import com.mysms.assessment.v1.ResetAssignmentResponse;
import com.mysms.assessment.v1.UpdateAssignmentAllocationRequest;
import com.mysms.assessment.v1.UpdateAssignmentAllocationResponse;
import com.mysms.assessment.v1.UpdateAssignmentApprovalRequest;
import com.mysms.assessment.v1.UpdateAssignmentApprovalResponse;
import com.mysms.assessment.v1.UpdateAssignmentRequest;
import com.mysms.assessment.v1.UpdateAssignmentResponse;
import com.mysms.assessment.v1.UpdateAssignmentSubjectRequest;
import com.mysms.assessment.v1.UpdateAssignmentSubjectResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.AssignmentService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AssignmentResolver extends AssignmentServiceGrpc.AssignmentServiceImplBase {

    private final AssignmentService assignmentService;
    private final ProtoBridge bridge;

    public AssignmentResolver(AssignmentService assignmentService, ProtoBridge bridge) {
        this.assignmentService = assignmentService;
        this.bridge = bridge;
    }

    @Override
    public void listAssignments(ListAssignmentsRequest request, StreamObserver<ListAssignmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Assignment> rows = assignmentService.listAssignments(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAssignmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssignment(GetAssignmentRequest request, StreamObserver<GetAssignmentResponse> out) {
        Assignment found = assignmentService.getAssignment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssignmentResponse.newBuilder(), "assignment", found).build());
    }

    @Override
    public void createAssignment(CreateAssignmentRequest request, StreamObserver<CreateAssignmentResponse> out) {
        Assignment created = assignmentService.createAssignment(
                bridge.body(request.getAssignment(), Assignment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssignmentResponse.newBuilder(), "assignment", created).build());
    }

    @Override
    public void updateAssignment(UpdateAssignmentRequest request, StreamObserver<UpdateAssignmentResponse> out) {
        Assignment updated = assignmentService.updateAssignment(
                bridge.id(request.getAssignment().getId(), "id"),
                bridge.requiredIfMatch(request.getAssignment().getEtag()),
                bridge.changes(request.getAssignment(), request.getUpdateMask(), AssignmentUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssignmentResponse.newBuilder(), "assignment", updated).build());
    }

    @Override
    public void deleteAssignment(DeleteAssignmentRequest request, StreamObserver<DeleteAssignmentResponse> out) {
        assignmentService.deleteAssignment(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssignmentResponse.getDefaultInstance());
    }

    @Override
    public void listAssignmentSubjects(ListAssignmentSubjectsRequest request,
            StreamObserver<ListAssignmentSubjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AssignmentSubject> rows = assignmentService.listAssignmentSubjects(
                bridge.id(request.getAssignmentId(), "assignmentId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAssignmentSubjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssignmentSubject(GetAssignmentSubjectRequest request,
            StreamObserver<GetAssignmentSubjectResponse> out) {
        AssignmentSubject found = assignmentService.getAssignmentSubject(
                bridge.id(request.getAssignmentId(), "assignmentId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssignmentSubjectResponse.newBuilder(),
                "assignment_subject", found).build());
    }

    @Override
    public void createAssignmentSubject(CreateAssignmentSubjectRequest request,
            StreamObserver<CreateAssignmentSubjectResponse> out) {
        AssignmentSubject created = assignmentService.createAssignmentSubject(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request.getAssignmentSubject(), AssignmentSubject.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssignmentSubjectResponse.newBuilder(),
                "assignment_subject", created).build());
    }

    @Override
    public void updateAssignmentSubject(UpdateAssignmentSubjectRequest request,
            StreamObserver<UpdateAssignmentSubjectResponse> out) {
        AssignmentSubject updated = assignmentService.updateAssignmentSubject(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.id(request.getAssignmentSubject().getId(), "id"),
                bridge.requiredIfMatch(request.getAssignmentSubject().getEtag()),
                bridge.changes(request.getAssignmentSubject(), request.getUpdateMask(),
                        AssignmentSubjectUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssignmentSubjectResponse.newBuilder(),
                "assignment_subject", updated).build());
    }

    @Override
    public void deleteAssignmentSubject(DeleteAssignmentSubjectRequest request,
            StreamObserver<DeleteAssignmentSubjectResponse> out) {
        assignmentService.deleteAssignmentSubject(
                bridge.id(request.getAssignmentId(), "assignmentId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssignmentSubjectResponse.getDefaultInstance());
    }

    @Override
    public void listAssignmentAllocations(ListAssignmentAllocationsRequest request,
            StreamObserver<ListAssignmentAllocationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AssignmentAllocation> rows = assignmentService.listAssignmentAllocations(
                bridge.id(request.getAssignmentId(), "assignmentId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAssignmentAllocationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssignmentAllocation(GetAssignmentAllocationRequest request,
            StreamObserver<GetAssignmentAllocationResponse> out) {
        AssignmentAllocation found = assignmentService.getAssignmentAllocation(
                bridge.id(request.getAssignmentId(), "assignmentId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssignmentAllocationResponse.newBuilder(),
                "assignment_allocation", found).build());
    }

    @Override
    public void createAssignmentAllocation(CreateAssignmentAllocationRequest request,
            StreamObserver<CreateAssignmentAllocationResponse> out) {
        AssignmentAllocation created = assignmentService.createAssignmentAllocation(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request.getAssignmentAllocation(), AssignmentAllocation.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssignmentAllocationResponse.newBuilder(),
                "assignment_allocation", created).build());
    }

    @Override
    public void updateAssignmentAllocation(UpdateAssignmentAllocationRequest request,
            StreamObserver<UpdateAssignmentAllocationResponse> out) {
        AssignmentAllocation updated = assignmentService.updateAssignmentAllocation(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.id(request.getAssignmentAllocation().getId(), "id"),
                bridge.requiredIfMatch(request.getAssignmentAllocation().getEtag()),
                bridge.changes(request.getAssignmentAllocation(), request.getUpdateMask(),
                        AssignmentAllocationUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssignmentAllocationResponse.newBuilder(),
                "assignment_allocation", updated).build());
    }

    @Override
    public void deleteAssignmentAllocation(DeleteAssignmentAllocationRequest request,
            StreamObserver<DeleteAssignmentAllocationResponse> out) {
        assignmentService.deleteAssignmentAllocation(
                bridge.id(request.getAssignmentId(), "assignmentId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssignmentAllocationResponse.getDefaultInstance());
    }

    @Override
    public void listAssignmentApprovals(ListAssignmentApprovalsRequest request,
            StreamObserver<ListAssignmentApprovalsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AssignmentApproval> rows = assignmentService.listAssignmentApprovals(
                bridge.id(request.getAssignmentId(), "assignmentId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAssignmentApprovalsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssignmentApproval(GetAssignmentApprovalRequest request,
            StreamObserver<GetAssignmentApprovalResponse> out) {
        AssignmentApproval found = assignmentService.getAssignmentApproval(
                bridge.id(request.getAssignmentId(), "assignmentId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssignmentApprovalResponse.newBuilder(),
                "assignment_approval", found).build());
    }

    @Override
    public void createAssignmentApproval(CreateAssignmentApprovalRequest request,
            StreamObserver<CreateAssignmentApprovalResponse> out) {
        AssignmentApproval created = assignmentService.createAssignmentApproval(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request.getAssignmentApproval(), AssignmentApproval.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssignmentApprovalResponse.newBuilder(),
                "assignment_approval", created).build());
    }

    @Override
    public void updateAssignmentApproval(UpdateAssignmentApprovalRequest request,
            StreamObserver<UpdateAssignmentApprovalResponse> out) {
        AssignmentApproval updated = assignmentService.updateAssignmentApproval(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.id(request.getAssignmentApproval().getId(), "id"),
                bridge.requiredIfMatch(request.getAssignmentApproval().getEtag()),
                bridge.changes(request.getAssignmentApproval(), request.getUpdateMask(),
                        AssignmentApprovalUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssignmentApprovalResponse.newBuilder(),
                "assignment_approval", updated).build());
    }

    @Override
    public void deleteAssignmentApproval(DeleteAssignmentApprovalRequest request,
            StreamObserver<DeleteAssignmentApprovalResponse> out) {
        assignmentService.deleteAssignmentApproval(
                bridge.id(request.getAssignmentId(), "assignmentId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssignmentApprovalResponse.getDefaultInstance());
    }

    @Override
    public void publishAssignment(PublishAssignmentRequest request, StreamObserver<PublishAssignmentResponse> out) {
        Assignment changed = assignmentService.publishAssignment(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.PublishAssignmentRequest.class,
                        "assignment_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(PublishAssignmentResponse.newBuilder(), "assignment", changed).build());
    }

    @Override
    public void approveAssignment(ApproveAssignmentRequest request, StreamObserver<ApproveAssignmentResponse> out) {
        Assignment changed = assignmentService.approveAssignment(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ApproveAssignmentRequest.class,
                        "assignment_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ApproveAssignmentResponse.newBuilder(), "assignment", changed).build());
    }

    @Override
    public void rejectAssignment(RejectAssignmentRequest request, StreamObserver<RejectAssignmentResponse> out) {
        Assignment changed = assignmentService.rejectAssignment(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.RejectAssignmentRequest.class,
                        "assignment_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectAssignmentResponse.newBuilder(), "assignment", changed).build());
    }

    @Override
    public void finishAssignment(FinishAssignmentRequest request, StreamObserver<FinishAssignmentResponse> out) {
        Assignment changed = assignmentService.finishAssignment(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.FinishAssignmentRequest.class,
                        "assignment_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(FinishAssignmentResponse.newBuilder(), "assignment", changed).build());
    }

    @Override
    public void cancelAssignment(CancelAssignmentRequest request, StreamObserver<CancelAssignmentResponse> out) {
        Assignment changed = assignmentService.cancelAssignment(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.CancelAssignmentRequest.class,
                        "assignment_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelAssignmentResponse.newBuilder(), "assignment", changed).build());
    }

    @Override
    public void resetAssignment(ResetAssignmentRequest request, StreamObserver<ResetAssignmentResponse> out) {
        Assignment changed = assignmentService.resetAssignment(
                bridge.id(request.getAssignmentId(), "assignmentId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ResetAssignmentRequest.class,
                        "assignment_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetAssignmentResponse.newBuilder(), "assignment", changed).build());
    }
}
