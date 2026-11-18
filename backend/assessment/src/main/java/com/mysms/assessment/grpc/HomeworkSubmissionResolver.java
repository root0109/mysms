package com.mysms.assessment.grpc;

import com.mysms.assessment.api.model.HomeworkSubmission;
import com.mysms.assessment.api.model.HomeworkSubmissionUpdate;
import com.mysms.assessment.business.HomeworkSubmissionService;
import com.mysms.assessment.business.HomeworkWorkflowService;
import com.mysms.assessment.v1.CheckHomeworkSubmissionRequest;
import com.mysms.assessment.v1.CheckHomeworkSubmissionResponse;
import com.mysms.assessment.v1.CreateHomeworkSubmissionRequest;
import com.mysms.assessment.v1.CreateHomeworkSubmissionResponse;
import com.mysms.assessment.v1.DeleteHomeworkSubmissionRequest;
import com.mysms.assessment.v1.DeleteHomeworkSubmissionResponse;
import com.mysms.assessment.v1.GetHomeworkSubmissionRequest;
import com.mysms.assessment.v1.GetHomeworkSubmissionResponse;
import com.mysms.assessment.v1.HomeworkSubmissionServiceGrpc;
import com.mysms.assessment.v1.ListHomeworkSubmissionsRequest;
import com.mysms.assessment.v1.ListHomeworkSubmissionsResponse;
import com.mysms.assessment.v1.ReplyToHomeworkFeedbackRequest;
import com.mysms.assessment.v1.ReplyToHomeworkFeedbackResponse;
import com.mysms.assessment.v1.UpdateHomeworkSubmissionRequest;
import com.mysms.assessment.v1.UpdateHomeworkSubmissionResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.HomeworkSubmissionService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class HomeworkSubmissionResolver extends HomeworkSubmissionServiceGrpc.HomeworkSubmissionServiceImplBase {

    private final HomeworkSubmissionService homeworkSubmissionService;
    private final HomeworkWorkflowService homeworkWorkflowService;
    private final ProtoBridge bridge;

    public HomeworkSubmissionResolver(HomeworkSubmissionService homeworkSubmissionService,
            HomeworkWorkflowService homeworkWorkflowService,
            ProtoBridge bridge) {
        this.homeworkSubmissionService = homeworkSubmissionService;
        this.homeworkWorkflowService = homeworkWorkflowService;
        this.bridge = bridge;
    }

    @Override
    public void listHomeworkSubmissions(ListHomeworkSubmissionsRequest request,
            StreamObserver<ListHomeworkSubmissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<HomeworkSubmission> rows = homeworkSubmissionService.listHomeworkSubmissions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListHomeworkSubmissionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getHomeworkSubmission(GetHomeworkSubmissionRequest request,
            StreamObserver<GetHomeworkSubmissionResponse> out) {
        HomeworkSubmission found = homeworkSubmissionService.getHomeworkSubmission(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetHomeworkSubmissionResponse.newBuilder(),
                "homework_submission", found).build());
    }

    @Override
    public void createHomeworkSubmission(CreateHomeworkSubmissionRequest request,
            StreamObserver<CreateHomeworkSubmissionResponse> out) {
        HomeworkSubmission created = homeworkSubmissionService.createHomeworkSubmission(
                bridge.body(request.getHomeworkSubmission(), HomeworkSubmission.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateHomeworkSubmissionResponse.newBuilder(),
                "homework_submission", created).build());
    }

    @Override
    public void updateHomeworkSubmission(UpdateHomeworkSubmissionRequest request,
            StreamObserver<UpdateHomeworkSubmissionResponse> out) {
        HomeworkSubmission updated = homeworkSubmissionService.updateHomeworkSubmission(
                bridge.id(request.getHomeworkSubmission().getId(), "id"),
                bridge.requiredIfMatch(request.getHomeworkSubmission().getEtag()),
                bridge.changes(request.getHomeworkSubmission(), request.getUpdateMask(),
                        HomeworkSubmissionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateHomeworkSubmissionResponse.newBuilder(),
                "homework_submission", updated).build());
    }

    @Override
    public void deleteHomeworkSubmission(DeleteHomeworkSubmissionRequest request,
            StreamObserver<DeleteHomeworkSubmissionResponse> out) {
        homeworkSubmissionService.deleteHomeworkSubmission(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteHomeworkSubmissionResponse.getDefaultInstance());
    }

    @Override
    public void checkHomeworkSubmission(CheckHomeworkSubmissionRequest request,
            StreamObserver<CheckHomeworkSubmissionResponse> out) {
        HomeworkSubmission changed = homeworkWorkflowService.checkHomeworkSubmission(
                bridge.id(request.getHomeworkSubmissionId(), "homeworkSubmissionId"),
                bridge.body(request,
                        com.mysms.assessment.api.model.CheckHomeworkSubmissionRequest.class,
                        "homework_submission_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CheckHomeworkSubmissionResponse.newBuilder(),
                "homework_submission", changed).build());
    }

    @Override
    public void replyToHomeworkFeedback(ReplyToHomeworkFeedbackRequest request,
            StreamObserver<ReplyToHomeworkFeedbackResponse> out) {
        HomeworkSubmission changed = homeworkWorkflowService.replyToHomeworkFeedback(
                bridge.id(request.getHomeworkSubmissionId(), "homeworkSubmissionId"),
                bridge.body(request,
                        com.mysms.assessment.api.model.ReplyToHomeworkFeedbackRequest.class,
                        "homework_submission_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ReplyToHomeworkFeedbackResponse.newBuilder(),
                "homework_submission", changed).build());
    }
}
