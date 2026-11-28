package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.AssignmentSubmission;
import com.mysms.academics.assessment.business.AssignmentSubmissionService;
import com.mysms.assessment.v1.AcceptAssignmentSubmissionRequest;
import com.mysms.assessment.v1.AcceptAssignmentSubmissionResponse;
import com.mysms.assessment.v1.AssignmentSubmissionServiceGrpc;
import com.mysms.assessment.v1.AssignmentSubmissionUpdate;
import com.mysms.assessment.v1.CreateAssignmentSubmissionRequest;
import com.mysms.assessment.v1.CreateAssignmentSubmissionResponse;
import com.mysms.assessment.v1.DeleteAssignmentSubmissionRequest;
import com.mysms.assessment.v1.DeleteAssignmentSubmissionResponse;
import com.mysms.assessment.v1.GetAssignmentSubmissionRequest;
import com.mysms.assessment.v1.GetAssignmentSubmissionResponse;
import com.mysms.assessment.v1.GradeAssignmentSubmissionRequest;
import com.mysms.assessment.v1.GradeAssignmentSubmissionResponse;
import com.mysms.assessment.v1.ListAssignmentSubmissionsRequest;
import com.mysms.assessment.v1.ListAssignmentSubmissionsResponse;
import com.mysms.assessment.v1.RejectAssignmentSubmissionRequest;
import com.mysms.assessment.v1.RejectAssignmentSubmissionResponse;
import com.mysms.assessment.v1.RequestSubmissionChangesRequest;
import com.mysms.assessment.v1.RequestSubmissionChangesResponse;
import com.mysms.assessment.v1.ResetAssignmentSubmissionRequest;
import com.mysms.assessment.v1.ResetAssignmentSubmissionResponse;
import com.mysms.assessment.v1.SubmitAssignmentSubmissionRequest;
import com.mysms.assessment.v1.SubmitAssignmentSubmissionResponse;
import com.mysms.assessment.v1.UpdateAssignmentSubmissionResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.AssignmentSubmissionService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AssignmentSubmissionResolver extends AssignmentSubmissionServiceGrpc.AssignmentSubmissionServiceImplBase {

    private final AssignmentSubmissionService assignmentSubmissionService;
    private final ProtoBridge bridge;

    public AssignmentSubmissionResolver(AssignmentSubmissionService assignmentSubmissionService, ProtoBridge bridge) {
        this.assignmentSubmissionService = assignmentSubmissionService;
        this.bridge = bridge;
    }

    @Override
    public void listAssignmentSubmissions(ListAssignmentSubmissionsRequest request,
            StreamObserver<ListAssignmentSubmissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AssignmentSubmission> rows = assignmentSubmissionService.listAssignmentSubmissions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAssignmentSubmissionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssignmentSubmission(GetAssignmentSubmissionRequest request,
            StreamObserver<GetAssignmentSubmissionResponse> out) {
        AssignmentSubmission found = assignmentSubmissionService.getAssignmentSubmission(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssignmentSubmissionResponse.newBuilder(),
                "assignment_submission", found).build());
    }

    @Override
    public void createAssignmentSubmission(CreateAssignmentSubmissionRequest request,
            StreamObserver<CreateAssignmentSubmissionResponse> out) {
        AssignmentSubmission created = assignmentSubmissionService.createAssignmentSubmission(
                bridge.body(request.getAssignmentSubmission(), AssignmentSubmission.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssignmentSubmissionResponse.newBuilder(),
                "assignment_submission", created).build());
    }

    @Override
    public void updateAssignmentSubmission(AssignmentSubmissionUpdate request,
            StreamObserver<UpdateAssignmentSubmissionResponse> out) {
        AssignmentSubmission updated = assignmentSubmissionService.updateAssignmentSubmission(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                AssignmentSubmissionProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssignmentSubmissionResponse.newBuilder(),
                "assignment_submission", updated).build());
    }

    @Override
    public void deleteAssignmentSubmission(DeleteAssignmentSubmissionRequest request,
            StreamObserver<DeleteAssignmentSubmissionResponse> out) {
        assignmentSubmissionService.deleteAssignmentSubmission(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssignmentSubmissionResponse.getDefaultInstance());
    }

    @Override
    public void submitAssignmentSubmission(SubmitAssignmentSubmissionRequest request,
            StreamObserver<SubmitAssignmentSubmissionResponse> out) {
        AssignmentSubmission changed = assignmentSubmissionService.submitAssignmentSubmission(
                bridge.id(request.getAssignmentSubmissionId(), "assignmentSubmissionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.SubmitAssignmentSubmissionRequest.class,
                        "assignment_submission_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(SubmitAssignmentSubmissionResponse.newBuilder(),
                "assignment_submission", changed).build());
    }

    @Override
    public void acceptAssignmentSubmission(AcceptAssignmentSubmissionRequest request,
            StreamObserver<AcceptAssignmentSubmissionResponse> out) {
        AssignmentSubmission changed = assignmentSubmissionService.acceptAssignmentSubmission(
                bridge.id(request.getAssignmentSubmissionId(), "assignmentSubmissionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.AcceptAssignmentSubmissionRequest.class,
                        "assignment_submission_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(AcceptAssignmentSubmissionResponse.newBuilder(),
                "assignment_submission", changed).build());
    }

    @Override
    public void requestSubmissionChanges(RequestSubmissionChangesRequest request,
            StreamObserver<RequestSubmissionChangesResponse> out) {
        AssignmentSubmission changed = assignmentSubmissionService.requestSubmissionChanges(
                bridge.id(request.getAssignmentSubmissionId(), "assignmentSubmissionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.RequestSubmissionChangesRequest.class,
                        "assignment_submission_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RequestSubmissionChangesResponse.newBuilder(),
                "assignment_submission", changed).build());
    }

    @Override
    public void rejectAssignmentSubmission(RejectAssignmentSubmissionRequest request,
            StreamObserver<RejectAssignmentSubmissionResponse> out) {
        AssignmentSubmission changed = assignmentSubmissionService.rejectAssignmentSubmission(
                bridge.id(request.getAssignmentSubmissionId(), "assignmentSubmissionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.RejectAssignmentSubmissionRequest.class,
                        "assignment_submission_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectAssignmentSubmissionResponse.newBuilder(),
                "assignment_submission", changed).build());
    }

    @Override
    public void resetAssignmentSubmission(ResetAssignmentSubmissionRequest request,
            StreamObserver<ResetAssignmentSubmissionResponse> out) {
        AssignmentSubmission changed = assignmentSubmissionService.resetAssignmentSubmission(
                bridge.id(request.getAssignmentSubmissionId(), "assignmentSubmissionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ResetAssignmentSubmissionRequest.class,
                        "assignment_submission_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetAssignmentSubmissionResponse.newBuilder(),
                "assignment_submission", changed).build());
    }

    @Override
    public void gradeAssignmentSubmission(GradeAssignmentSubmissionRequest request,
            StreamObserver<GradeAssignmentSubmissionResponse> out) {
        AssignmentSubmission changed = assignmentSubmissionService.gradeAssignmentSubmission(
                bridge.id(request.getAssignmentSubmissionId(), "assignmentSubmissionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.GradeAssignmentSubmissionRequest.class,
                        "assignment_submission_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(GradeAssignmentSubmissionResponse.newBuilder(),
                "assignment_submission", changed).build());
    }
}
