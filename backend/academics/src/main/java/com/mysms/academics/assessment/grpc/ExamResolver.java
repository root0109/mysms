package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.Exam;
import com.mysms.academics.assessment.api.model.ExamInvigilator;
import com.mysms.academics.assessment.api.model.ExamInvigilatorUpdate;
import com.mysms.academics.assessment.api.model.ExamResultList;
import com.mysms.academics.assessment.api.model.ExamUpdate;
import com.mysms.academics.assessment.business.ExamService;
import com.mysms.assessment.v1.AllocateExamRoomsRequest;
import com.mysms.assessment.v1.AllocateExamRoomsResponse;
import com.mysms.assessment.v1.CancelExamRequest;
import com.mysms.assessment.v1.CancelExamResponse;
import com.mysms.assessment.v1.CompleteExamRequest;
import com.mysms.assessment.v1.CompleteExamResponse;
import com.mysms.assessment.v1.CreateExamInvigilatorRequest;
import com.mysms.assessment.v1.CreateExamInvigilatorResponse;
import com.mysms.assessment.v1.CreateExamRequest;
import com.mysms.assessment.v1.CreateExamResponse;
import com.mysms.assessment.v1.DeleteExamInvigilatorRequest;
import com.mysms.assessment.v1.DeleteExamInvigilatorResponse;
import com.mysms.assessment.v1.DeleteExamRequest;
import com.mysms.assessment.v1.DeleteExamResponse;
import com.mysms.assessment.v1.ExamServiceGrpc;
import com.mysms.assessment.v1.GetExamInvigilatorRequest;
import com.mysms.assessment.v1.GetExamInvigilatorResponse;
import com.mysms.assessment.v1.GetExamRequest;
import com.mysms.assessment.v1.GetExamResponse;
import com.mysms.assessment.v1.ListExamInvigilatorsRequest;
import com.mysms.assessment.v1.ListExamInvigilatorsResponse;
import com.mysms.assessment.v1.ListExamsRequest;
import com.mysms.assessment.v1.ListExamsResponse;
import com.mysms.assessment.v1.MarkExamHeldRequest;
import com.mysms.assessment.v1.MarkExamHeldResponse;
import com.mysms.assessment.v1.MarkExamResultsUpdatedRequest;
import com.mysms.assessment.v1.MarkExamResultsUpdatedResponse;
import com.mysms.assessment.v1.PopulateExamResultsRequest;
import com.mysms.assessment.v1.PopulateExamResultsResponse;
import com.mysms.assessment.v1.RecordExamMarksRequest;
import com.mysms.assessment.v1.RecordExamMarksResponse;
import com.mysms.assessment.v1.ResetExamRequest;
import com.mysms.assessment.v1.ResetExamResponse;
import com.mysms.assessment.v1.ScheduleExamRequest;
import com.mysms.assessment.v1.ScheduleExamResponse;
import com.mysms.assessment.v1.UpdateExamInvigilatorRequest;
import com.mysms.assessment.v1.UpdateExamInvigilatorResponse;
import com.mysms.assessment.v1.UpdateExamRequest;
import com.mysms.assessment.v1.UpdateExamResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.ExamService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ExamResolver extends ExamServiceGrpc.ExamServiceImplBase {

    private final ExamService examService;
    private final ProtoBridge bridge;

    public ExamResolver(ExamService examService, ProtoBridge bridge) {
        this.examService = examService;
        this.bridge = bridge;
    }

    @Override
    public void listExams(ListExamsRequest request, StreamObserver<ListExamsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Exam> rows = examService.listExams(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExamsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExam(GetExamRequest request, StreamObserver<GetExamResponse> out) {
        Exam found = examService.getExam(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExamResponse.newBuilder(), "exam", found).build());
    }

    @Override
    public void createExam(CreateExamRequest request, StreamObserver<CreateExamResponse> out) {
        Exam created = examService.createExam(bridge.body(request.getExam(), Exam.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateExamResponse.newBuilder(), "exam", created).build());
    }

    @Override
    public void updateExam(UpdateExamRequest request, StreamObserver<UpdateExamResponse> out) {
        Exam updated = examService.updateExam(
                bridge.id(request.getExam().getId(), "id"), bridge.requiredIfMatch(request.getExam().getEtag()),
                bridge.changes(request.getExam(), request.getUpdateMask(), ExamUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateExamResponse.newBuilder(), "exam", updated).build());
    }

    @Override
    public void deleteExam(DeleteExamRequest request, StreamObserver<DeleteExamResponse> out) {
        examService.deleteExam(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExamResponse.getDefaultInstance());
    }

    @Override
    public void listExamInvigilators(ListExamInvigilatorsRequest request,
            StreamObserver<ListExamInvigilatorsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExamInvigilator> rows = examService.listExamInvigilators(
                bridge.id(request.getExamId(), "examId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExamInvigilatorsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExamInvigilator(GetExamInvigilatorRequest request,
            StreamObserver<GetExamInvigilatorResponse> out) {
        ExamInvigilator found = examService.getExamInvigilator(
                bridge.id(request.getExamId(), "examId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExamInvigilatorResponse.newBuilder(),
                "exam_invigilator", found).build());
    }

    @Override
    public void createExamInvigilator(CreateExamInvigilatorRequest request,
            StreamObserver<CreateExamInvigilatorResponse> out) {
        ExamInvigilator created = examService.createExamInvigilator(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request.getExamInvigilator(), ExamInvigilator.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateExamInvigilatorResponse.newBuilder(),
                "exam_invigilator", created).build());
    }

    @Override
    public void updateExamInvigilator(UpdateExamInvigilatorRequest request,
            StreamObserver<UpdateExamInvigilatorResponse> out) {
        ExamInvigilator updated = examService.updateExamInvigilator(
                bridge.id(request.getExamId(), "examId"), bridge.id(request.getExamInvigilator().getId(), "id"),
                bridge.requiredIfMatch(request.getExamInvigilator().getEtag()),
                bridge.changes(request.getExamInvigilator(), request.getUpdateMask(), ExamInvigilatorUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateExamInvigilatorResponse.newBuilder(),
                "exam_invigilator", updated).build());
    }

    @Override
    public void deleteExamInvigilator(DeleteExamInvigilatorRequest request,
            StreamObserver<DeleteExamInvigilatorResponse> out) {
        examService.deleteExamInvigilator(
                bridge.id(request.getExamId(), "examId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExamInvigilatorResponse.getDefaultInstance());
    }

    @Override
    public void scheduleExam(ScheduleExamRequest request, StreamObserver<ScheduleExamResponse> out) {
        Exam changed = examService.scheduleExam(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ScheduleExamRequest.class, "exam_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ScheduleExamResponse.newBuilder(), "exam", changed).build());
    }

    @Override
    public void markExamHeld(MarkExamHeldRequest request, StreamObserver<MarkExamHeldResponse> out) {
        Exam changed = examService.markExamHeld(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.MarkExamHeldRequest.class, "exam_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(MarkExamHeldResponse.newBuilder(), "exam", changed).build());
    }

    @Override
    public void markExamResultsUpdated(MarkExamResultsUpdatedRequest request,
            StreamObserver<MarkExamResultsUpdatedResponse> out) {
        Exam changed = examService.markExamResultsUpdated(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.MarkExamResultsUpdatedRequest.class,
                        "exam_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(MarkExamResultsUpdatedResponse.newBuilder(), "exam", changed).build());
    }

    @Override
    public void completeExam(CompleteExamRequest request, StreamObserver<CompleteExamResponse> out) {
        Exam changed = examService.completeExam(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.CompleteExamRequest.class, "exam_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CompleteExamResponse.newBuilder(), "exam", changed).build());
    }

    @Override
    public void cancelExam(CancelExamRequest request, StreamObserver<CancelExamResponse> out) {
        Exam changed = examService.cancelExam(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.CancelExamRequest.class, "exam_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelExamResponse.newBuilder(), "exam", changed).build());
    }

    @Override
    public void resetExam(ResetExamRequest request, StreamObserver<ResetExamResponse> out) {
        Exam changed = examService.resetExam(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.ResetExamRequest.class, "exam_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetExamResponse.newBuilder(), "exam", changed).build());
    }

    @Override
    public void populateExamResults(PopulateExamResultsRequest request,
            StreamObserver<PopulateExamResultsResponse> out) {
        ExamResultList changed = examService.populateExamResults(bridge.id(request.getExamId(), "examId"));
        ProtoBridge.send(out, bridge.reply(PopulateExamResultsResponse.newBuilder(), "items",
                changed.getItems()).build());
    }

    @Override
    public void allocateExamRooms(AllocateExamRoomsRequest request, StreamObserver<AllocateExamRoomsResponse> out) {
        ExamResultList changed = examService.allocateExamRooms(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.AllocateExamRoomsRequest.class, "exam_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(AllocateExamRoomsResponse.newBuilder(), "items",
                changed.getItems()).build());
    }

    @Override
    public void recordExamMarks(RecordExamMarksRequest request, StreamObserver<RecordExamMarksResponse> out) {
        ExamResultList changed = examService.recordExamMarks(
                bridge.id(request.getExamId(), "examId"),
                bridge.body(request,
                        com.mysms.academics.assessment.api.model.RecordExamMarksRequest.class, "exam_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RecordExamMarksResponse.newBuilder(), "items",
                changed.getItems()).build());
    }
}
