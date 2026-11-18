package com.mysms.academics.grpc;

import com.mysms.academics.api.model.LessonPlan;
import com.mysms.academics.api.model.LessonPlanApproval;
import com.mysms.academics.api.model.LessonPlanApprovalUpdate;
import com.mysms.academics.api.model.LessonPlanUpdate;
import com.mysms.academics.business.LessonPlanService;
import com.mysms.academics.v1.ApproveLessonPlanRequest;
import com.mysms.academics.v1.ApproveLessonPlanResponse;
import com.mysms.academics.v1.ArchiveLessonPlanRequest;
import com.mysms.academics.v1.ArchiveLessonPlanResponse;
import com.mysms.academics.v1.CreateLessonPlanApprovalRequest;
import com.mysms.academics.v1.CreateLessonPlanApprovalResponse;
import com.mysms.academics.v1.CreateLessonPlanRequest;
import com.mysms.academics.v1.CreateLessonPlanResponse;
import com.mysms.academics.v1.DeleteLessonPlanApprovalRequest;
import com.mysms.academics.v1.DeleteLessonPlanApprovalResponse;
import com.mysms.academics.v1.DeleteLessonPlanRequest;
import com.mysms.academics.v1.DeleteLessonPlanResponse;
import com.mysms.academics.v1.GetLessonPlanApprovalRequest;
import com.mysms.academics.v1.GetLessonPlanApprovalResponse;
import com.mysms.academics.v1.GetLessonPlanRequest;
import com.mysms.academics.v1.GetLessonPlanResponse;
import com.mysms.academics.v1.LessonPlanServiceGrpc;
import com.mysms.academics.v1.ListLessonPlanApprovalsRequest;
import com.mysms.academics.v1.ListLessonPlanApprovalsResponse;
import com.mysms.academics.v1.ListLessonPlansRequest;
import com.mysms.academics.v1.ListLessonPlansResponse;
import com.mysms.academics.v1.PublishLessonPlanRequest;
import com.mysms.academics.v1.PublishLessonPlanResponse;
import com.mysms.academics.v1.RejectLessonPlanRequest;
import com.mysms.academics.v1.RejectLessonPlanResponse;
import com.mysms.academics.v1.SubmitLessonPlanRequest;
import com.mysms.academics.v1.SubmitLessonPlanResponse;
import com.mysms.academics.v1.UpdateLessonPlanApprovalRequest;
import com.mysms.academics.v1.UpdateLessonPlanApprovalResponse;
import com.mysms.academics.v1.UpdateLessonPlanRequest;
import com.mysms.academics.v1.UpdateLessonPlanResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.LessonPlanService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class LessonPlanResolver extends LessonPlanServiceGrpc.LessonPlanServiceImplBase {

    private final LessonPlanService service;
    private final ProtoBridge bridge;

    public LessonPlanResolver(LessonPlanService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLessonPlans(ListLessonPlansRequest request, StreamObserver<ListLessonPlansResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LessonPlan> rows = service.listLessonPlans(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLessonPlansResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLessonPlan(GetLessonPlanRequest request, StreamObserver<GetLessonPlanResponse> out) {
        LessonPlan found = service.getLessonPlan(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLessonPlanResponse.newBuilder(), "lesson_plan", found).build());
    }

    @Override
    public void createLessonPlan(CreateLessonPlanRequest request, StreamObserver<CreateLessonPlanResponse> out) {
        LessonPlan created = service.createLessonPlan(bridge.body(request.getLessonPlan(), LessonPlan.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLessonPlanResponse.newBuilder(), "lesson_plan", created).build());
    }

    @Override
    public void updateLessonPlan(UpdateLessonPlanRequest request, StreamObserver<UpdateLessonPlanResponse> out) {
        LessonPlan updated = service.updateLessonPlan(bridge.id(request.getLessonPlan().getId(), "id"),
                bridge.requiredIfMatch(request.getLessonPlan().getEtag()),
                bridge.changes(request.getLessonPlan(), request.getUpdateMask(), LessonPlanUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLessonPlanResponse.newBuilder(), "lesson_plan", updated).build());
    }

    @Override
    public void deleteLessonPlan(DeleteLessonPlanRequest request, StreamObserver<DeleteLessonPlanResponse> out) {
        service.deleteLessonPlan(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLessonPlanResponse.getDefaultInstance());
    }

    @Override
    public void listLessonPlanApprovals(ListLessonPlanApprovalsRequest request,
            StreamObserver<ListLessonPlanApprovalsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LessonPlanApproval> rows = service.listLessonPlanApprovals(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLessonPlanApprovalsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLessonPlanApproval(GetLessonPlanApprovalRequest request, StreamObserver<GetLessonPlanApprovalResponse> out) {
        LessonPlanApproval found = service.getLessonPlanApproval(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLessonPlanApprovalResponse.newBuilder(), "lesson_plan_approval", found).build());
    }

    @Override
    public void createLessonPlanApproval(CreateLessonPlanApprovalRequest request,
            StreamObserver<CreateLessonPlanApprovalResponse> out) {
        LessonPlanApproval created = service.createLessonPlanApproval(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                bridge.body(request.getLessonPlanApproval(), LessonPlanApproval.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateLessonPlanApprovalResponse.newBuilder(), "lesson_plan_approval", created).build());
    }

    @Override
    public void updateLessonPlanApproval(UpdateLessonPlanApprovalRequest request,
            StreamObserver<UpdateLessonPlanApprovalResponse> out) {
        LessonPlanApproval updated = service.updateLessonPlanApproval(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                bridge.id(request.getLessonPlanApproval().getId(), "id"),
                bridge.requiredIfMatch(request.getLessonPlanApproval().getEtag()),
                bridge.changes(request.getLessonPlanApproval(), request.getUpdateMask(), LessonPlanApprovalUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateLessonPlanApprovalResponse.newBuilder(), "lesson_plan_approval", updated).build());
    }

    @Override
    public void deleteLessonPlanApproval(DeleteLessonPlanApprovalRequest request,
            StreamObserver<DeleteLessonPlanApprovalResponse> out) {
        service.deleteLessonPlanApproval(bridge.id(request.getLessonPlanId(), "lessonPlanId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLessonPlanApprovalResponse.getDefaultInstance());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void submitLessonPlan(SubmitLessonPlanRequest request, StreamObserver<SubmitLessonPlanResponse> out) {
        LessonPlan changed = service.submitLessonPlan(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                bridge.body(request, com.mysms.academics.api.model.SubmitLessonPlanRequest.class, "lesson_plan_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(SubmitLessonPlanResponse.newBuilder(), "lesson_plan", changed).build());
    }

    /** The etag (optional) and the reason are the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void approveLessonPlan(ApproveLessonPlanRequest request, StreamObserver<ApproveLessonPlanResponse> out) {
        LessonPlan changed = service.approveLessonPlan(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                bridge.body(request, com.mysms.academics.api.model.ApproveLessonPlanRequest.class, "lesson_plan_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ApproveLessonPlanResponse.newBuilder(), "lesson_plan", changed).build());
    }

    /** The etag (optional) and the reason are the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void rejectLessonPlan(RejectLessonPlanRequest request, StreamObserver<RejectLessonPlanResponse> out) {
        LessonPlan changed = service.rejectLessonPlan(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                bridge.body(request, com.mysms.academics.api.model.RejectLessonPlanRequest.class, "lesson_plan_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectLessonPlanResponse.newBuilder(), "lesson_plan", changed).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void publishLessonPlan(PublishLessonPlanRequest request, StreamObserver<PublishLessonPlanResponse> out) {
        LessonPlan changed = service.publishLessonPlan(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                bridge.body(request, com.mysms.academics.api.model.PublishLessonPlanRequest.class, "lesson_plan_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(PublishLessonPlanResponse.newBuilder(), "lesson_plan", changed).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void archiveLessonPlan(ArchiveLessonPlanRequest request, StreamObserver<ArchiveLessonPlanResponse> out) {
        LessonPlan changed = service.archiveLessonPlan(bridge.id(request.getLessonPlanId(), "lessonPlanId"),
                bridge.body(request, com.mysms.academics.api.model.ArchiveLessonPlanRequest.class, "lesson_plan_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ArchiveLessonPlanResponse.newBuilder(), "lesson_plan", changed).build());
    }
}
