package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ExamSession;
import com.mysms.academics.assessment.api.model.ExamSessionUpdate;
import com.mysms.academics.assessment.business.ExamSessionService;
import com.mysms.assessment.v1.CancelExamSessionRequest;
import com.mysms.assessment.v1.CancelExamSessionResponse;
import com.mysms.assessment.v1.CompleteExamSessionRequest;
import com.mysms.assessment.v1.CompleteExamSessionResponse;
import com.mysms.assessment.v1.CreateExamSessionRequest;
import com.mysms.assessment.v1.CreateExamSessionResponse;
import com.mysms.assessment.v1.DeleteExamSessionRequest;
import com.mysms.assessment.v1.DeleteExamSessionResponse;
import com.mysms.assessment.v1.ExamSessionServiceGrpc;
import com.mysms.assessment.v1.GetExamSessionRequest;
import com.mysms.assessment.v1.GetExamSessionResponse;
import com.mysms.assessment.v1.ListExamSessionsRequest;
import com.mysms.assessment.v1.ListExamSessionsResponse;
import com.mysms.assessment.v1.MarkExamSessionHeldRequest;
import com.mysms.assessment.v1.MarkExamSessionHeldResponse;
import com.mysms.assessment.v1.ResetExamSessionRequest;
import com.mysms.assessment.v1.ResetExamSessionResponse;
import com.mysms.assessment.v1.ScheduleExamSessionRequest;
import com.mysms.assessment.v1.ScheduleExamSessionResponse;
import com.mysms.assessment.v1.UpdateExamSessionRequest;
import com.mysms.assessment.v1.UpdateExamSessionResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.ExamSessionService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ExamSessionResolver extends ExamSessionServiceGrpc.ExamSessionServiceImplBase {

    private final ExamSessionService examSessionService;
    private final ProtoBridge bridge;

    public ExamSessionResolver(ExamSessionService examSessionService, ProtoBridge bridge) {
        this.examSessionService = examSessionService;
        this.bridge = bridge;
    }

    @Override
    public void listExamSessions(ListExamSessionsRequest request, StreamObserver<ListExamSessionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExamSession> rows = examSessionService.listExamSessions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExamSessionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExamSession(GetExamSessionRequest request, StreamObserver<GetExamSessionResponse> out) {
        ExamSession found = examSessionService.getExamSession(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExamSessionResponse.newBuilder(), "exam_session", found).build());
    }

    @Override
    public void createExamSession(CreateExamSessionRequest request, StreamObserver<CreateExamSessionResponse> out) {
        ExamSession created = examSessionService.createExamSession(
                bridge.body(request.getExamSession(), ExamSession.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateExamSessionResponse.newBuilder(), "exam_session", created).build());
    }

    @Override
    public void updateExamSession(UpdateExamSessionRequest request, StreamObserver<UpdateExamSessionResponse> out) {
        ExamSession updated = examSessionService.updateExamSession(
                bridge.id(request.getExamSession().getId(), "id"),
                bridge.requiredIfMatch(request.getExamSession().getEtag()),
                bridge.changes(request.getExamSession(), request.getUpdateMask(), ExamSessionUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateExamSessionResponse.newBuilder(), "exam_session", updated).build());
    }

    @Override
    public void deleteExamSession(DeleteExamSessionRequest request, StreamObserver<DeleteExamSessionResponse> out) {
        examSessionService.deleteExamSession(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExamSessionResponse.getDefaultInstance());
    }

    @Override
    public void scheduleExamSession(ScheduleExamSessionRequest request,
            StreamObserver<ScheduleExamSessionResponse> out) {
        ExamSession changed = examSessionService.scheduleExamSession(
                bridge.id(request.getExamSessionId(), "examSessionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ScheduleExamSessionRequest.class,
                        "exam_session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ScheduleExamSessionResponse.newBuilder(),
                "exam_session", changed).build());
    }

    @Override
    public void markExamSessionHeld(MarkExamSessionHeldRequest request,
            StreamObserver<MarkExamSessionHeldResponse> out) {
        ExamSession changed = examSessionService.markExamSessionHeld(
                bridge.id(request.getExamSessionId(), "examSessionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.MarkExamSessionHeldRequest.class,
                        "exam_session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(MarkExamSessionHeldResponse.newBuilder(),
                "exam_session", changed).build());
    }

    @Override
    public void completeExamSession(CompleteExamSessionRequest request,
            StreamObserver<CompleteExamSessionResponse> out) {
        ExamSession changed = examSessionService.completeExamSession(
                bridge.id(request.getExamSessionId(), "examSessionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.CompleteExamSessionRequest.class,
                        "exam_session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CompleteExamSessionResponse.newBuilder(),
                "exam_session", changed).build());
    }

    @Override
    public void cancelExamSession(CancelExamSessionRequest request, StreamObserver<CancelExamSessionResponse> out) {
        ExamSession changed = examSessionService.cancelExamSession(
                bridge.id(request.getExamSessionId(), "examSessionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.CancelExamSessionRequest.class,
                        "exam_session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelExamSessionResponse.newBuilder(), "exam_session", changed).build());
    }

    @Override
    public void resetExamSession(ResetExamSessionRequest request, StreamObserver<ResetExamSessionResponse> out) {
        ExamSession changed = examSessionService.resetExamSession(
                bridge.id(request.getExamSessionId(), "examSessionId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ResetExamSessionRequest.class,
                        "exam_session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetExamSessionResponse.newBuilder(), "exam_session", changed).build());
    }
}
