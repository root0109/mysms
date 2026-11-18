package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.LmsAssignmentSubmission;
import com.mysms.lms.api.model.LmsAssignmentSubmissionUpdate;
import com.mysms.lms.business.LmsAssignmentSubmissionService;
import com.mysms.lms.v1.CreateLmsAssignmentSubmissionRequest;
import com.mysms.lms.v1.CreateLmsAssignmentSubmissionResponse;
import com.mysms.lms.v1.DeleteLmsAssignmentSubmissionRequest;
import com.mysms.lms.v1.DeleteLmsAssignmentSubmissionResponse;
import com.mysms.lms.v1.FinalizeSubmissionRequest;
import com.mysms.lms.v1.FinalizeSubmissionResponse;
import com.mysms.lms.v1.GetLmsAssignmentSubmissionRequest;
import com.mysms.lms.v1.GetLmsAssignmentSubmissionResponse;
import com.mysms.lms.v1.GradeLearningSubmissionRequest;
import com.mysms.lms.v1.GradeLearningSubmissionResponse;
import com.mysms.lms.v1.ListLmsAssignmentSubmissionsRequest;
import com.mysms.lms.v1.ListLmsAssignmentSubmissionsResponse;
import com.mysms.lms.v1.LmsAssignmentSubmissionServiceGrpc;
import com.mysms.lms.v1.UpdateLmsAssignmentSubmissionRequest;
import com.mysms.lms.v1.UpdateLmsAssignmentSubmissionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.LmsAssignmentSubmissionService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer
 * UNIMPLEMENTED.
 */
@GrpcService
public class LmsAssignmentSubmissionResolver extends LmsAssignmentSubmissionServiceGrpc.LmsAssignmentSubmissionServiceImplBase {

    private final LmsAssignmentSubmissionService service;
    private final ProtoBridge bridge;

    public LmsAssignmentSubmissionResolver(LmsAssignmentSubmissionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLmsAssignmentSubmissions(ListLmsAssignmentSubmissionsRequest request,
            StreamObserver<ListLmsAssignmentSubmissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LmsAssignmentSubmission> rows = service.listLmsAssignmentSubmissions(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLmsAssignmentSubmissionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLmsAssignmentSubmission(GetLmsAssignmentSubmissionRequest request,
            StreamObserver<GetLmsAssignmentSubmissionResponse> out) {
        LmsAssignmentSubmission found = service.getLmsAssignmentSubmission(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetLmsAssignmentSubmissionResponse.newBuilder(), "assignment_submission", found).build());
    }

    @Override
    public void createLmsAssignmentSubmission(CreateLmsAssignmentSubmissionRequest request,
            StreamObserver<CreateLmsAssignmentSubmissionResponse> out) {
        LmsAssignmentSubmission created = service.createLmsAssignmentSubmission(
                bridge.body(request.getAssignmentSubmission(), LmsAssignmentSubmission.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateLmsAssignmentSubmissionResponse.newBuilder(), "assignment_submission", created).build());
    }

    @Override
    public void updateLmsAssignmentSubmission(UpdateLmsAssignmentSubmissionRequest request,
            StreamObserver<UpdateLmsAssignmentSubmissionResponse> out) {
        LmsAssignmentSubmission updated = service.updateLmsAssignmentSubmission(
                bridge.id(request.getAssignmentSubmission().getId(), "id"),
                bridge.requiredIfMatch(request.getAssignmentSubmission().getEtag()),
                bridge.changes(request.getAssignmentSubmission(), request.getUpdateMask(), LmsAssignmentSubmissionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateLmsAssignmentSubmissionResponse.newBuilder(), "assignment_submission", updated).build());
    }

    @Override
    public void deleteLmsAssignmentSubmission(DeleteLmsAssignmentSubmissionRequest request,
            StreamObserver<DeleteLmsAssignmentSubmissionResponse> out) {
        service.deleteLmsAssignmentSubmission(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLmsAssignmentSubmissionResponse.getDefaultInstance());
    }

    @Override
    public void gradeLearningSubmission(GradeLearningSubmissionRequest request,
            StreamObserver<GradeLearningSubmissionResponse> out) {
        LmsAssignmentSubmission changed = service.gradeLearningSubmission(
                bridge.id(request.getAssignmentSubmissionId(), "assignmentSubmissionId"),
                bridge.body(request, com.mysms.lms.api.model.GradeLearningSubmissionRequest.class, "assignment_submission_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(GradeLearningSubmissionResponse.newBuilder(), "assignment_submission",
                changed).build());
    }

    @Override
    public void finalizeSubmission(FinalizeSubmissionRequest request, StreamObserver<FinalizeSubmissionResponse> out) {
        LmsAssignmentSubmission changed = service.finalizeSubmission(
                bridge.id(request.getAssignmentSubmissionId(), "assignmentSubmissionId"));
        ProtoBridge.send(out, bridge.reply(FinalizeSubmissionResponse.newBuilder(), "assignment_submission", changed)
                .build());
    }
}
