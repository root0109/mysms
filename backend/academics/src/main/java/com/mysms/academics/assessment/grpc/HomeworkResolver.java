package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.Homework;
import com.mysms.academics.assessment.api.model.HomeworkApproval;
import com.mysms.academics.assessment.api.model.HomeworkUpdate;
import com.mysms.academics.assessment.business.HomeworkService;
import com.mysms.academics.assessment.business.HomeworkWorkflowService;
import com.mysms.assessment.v1.ApproveHomeworkRequest;
import com.mysms.assessment.v1.ApproveHomeworkResponse;
import com.mysms.assessment.v1.CreateHomeworkRequest;
import com.mysms.assessment.v1.CreateHomeworkResponse;
import com.mysms.assessment.v1.DeleteHomeworkRequest;
import com.mysms.assessment.v1.DeleteHomeworkResponse;
import com.mysms.assessment.v1.GetHomeworkApprovalRequest;
import com.mysms.assessment.v1.GetHomeworkApprovalResponse;
import com.mysms.assessment.v1.GetHomeworkRequest;
import com.mysms.assessment.v1.GetHomeworkResponse;
import com.mysms.assessment.v1.HomeworkServiceGrpc;
import com.mysms.assessment.v1.ListHomeworkApprovalsRequest;
import com.mysms.assessment.v1.ListHomeworkApprovalsResponse;
import com.mysms.assessment.v1.ListHomeworksRequest;
import com.mysms.assessment.v1.ListHomeworksResponse;
import com.mysms.assessment.v1.PublishHomeworkRequest;
import com.mysms.assessment.v1.PublishHomeworkResponse;
import com.mysms.assessment.v1.RejectHomeworkRequest;
import com.mysms.assessment.v1.RejectHomeworkResponse;
import com.mysms.assessment.v1.UpdateHomeworkRequest;
import com.mysms.assessment.v1.UpdateHomeworkResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.HomeworkService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class HomeworkResolver extends HomeworkServiceGrpc.HomeworkServiceImplBase {

    private final HomeworkService homeworkService;
    private final HomeworkWorkflowService homeworkWorkflowService;
    private final ProtoBridge bridge;

    public HomeworkResolver(HomeworkService homeworkService,
            HomeworkWorkflowService homeworkWorkflowService,
            ProtoBridge bridge) {
        this.homeworkService = homeworkService;
        this.homeworkWorkflowService = homeworkWorkflowService;
        this.bridge = bridge;
    }

    @Override
    public void listHomeworks(ListHomeworksRequest request, StreamObserver<ListHomeworksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Homework> rows = homeworkService.listHomeworks(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListHomeworksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getHomework(GetHomeworkRequest request, StreamObserver<GetHomeworkResponse> out) {
        Homework found = homeworkService.getHomework(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetHomeworkResponse.newBuilder(), "homework", found).build());
    }

    @Override
    public void createHomework(CreateHomeworkRequest request, StreamObserver<CreateHomeworkResponse> out) {
        Homework created = homeworkService.createHomework(
                bridge.body(request.getHomework(), Homework.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateHomeworkResponse.newBuilder(), "homework", created).build());
    }

    @Override
    public void updateHomework(UpdateHomeworkRequest request, StreamObserver<UpdateHomeworkResponse> out) {
        Homework updated = homeworkService.updateHomework(
                bridge.id(request.getHomework().getId(), "id"),
                bridge.requiredIfMatch(request.getHomework().getEtag()),
                bridge.changes(request.getHomework(), request.getUpdateMask(), HomeworkUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateHomeworkResponse.newBuilder(), "homework", updated).build());
    }

    @Override
    public void deleteHomework(DeleteHomeworkRequest request, StreamObserver<DeleteHomeworkResponse> out) {
        homeworkService.deleteHomework(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteHomeworkResponse.getDefaultInstance());
    }

    @Override
    public void listHomeworkApprovals(ListHomeworkApprovalsRequest request,
            StreamObserver<ListHomeworkApprovalsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<HomeworkApproval> rows = homeworkService.listHomeworkApprovals(
                bridge.id(request.getHomeworkId(), "homeworkId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListHomeworkApprovalsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getHomeworkApproval(GetHomeworkApprovalRequest request,
            StreamObserver<GetHomeworkApprovalResponse> out) {
        HomeworkApproval found = homeworkService.getHomeworkApproval(
                bridge.id(request.getHomeworkId(), "homeworkId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetHomeworkApprovalResponse.newBuilder(),
                "homework_approval", found).build());
    }

    @Override
    public void publishHomework(PublishHomeworkRequest request, StreamObserver<PublishHomeworkResponse> out) {
        Homework changed = homeworkWorkflowService.publishHomework(
                bridge.id(request.getHomeworkId(), "homeworkId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.PublishHomeworkRequest.class, "homework_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(PublishHomeworkResponse.newBuilder(), "homework", changed).build());
    }

    @Override
    public void approveHomework(ApproveHomeworkRequest request, StreamObserver<ApproveHomeworkResponse> out) {
        Homework changed = homeworkWorkflowService.approveHomework(
                bridge.id(request.getHomeworkId(), "homeworkId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ApproveHomeworkRequest.class, "homework_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ApproveHomeworkResponse.newBuilder(), "homework", changed).build());
    }

    @Override
    public void rejectHomework(RejectHomeworkRequest request, StreamObserver<RejectHomeworkResponse> out) {
        Homework changed = homeworkWorkflowService.rejectHomework(
                bridge.id(request.getHomeworkId(), "homeworkId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.RejectHomeworkRequest.class, "homework_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectHomeworkResponse.newBuilder(), "homework", changed).build());
    }
}
