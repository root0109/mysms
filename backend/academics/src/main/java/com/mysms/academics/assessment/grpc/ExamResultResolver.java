package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ExamResult;
import com.mysms.academics.assessment.business.ExamResultService;
import com.mysms.assessment.v1.ExamResultServiceGrpc;
import com.mysms.assessment.v1.ExamResultUpdate;
import com.mysms.assessment.v1.GetExamResultRequest;
import com.mysms.assessment.v1.GetExamResultResponse;
import com.mysms.assessment.v1.ListExamResultsRequest;
import com.mysms.assessment.v1.ListExamResultsResponse;
import com.mysms.assessment.v1.UpdateExamResultResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.ExamResultService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ExamResultResolver extends ExamResultServiceGrpc.ExamResultServiceImplBase {

    private final ExamResultService examResultService;
    private final ProtoBridge bridge;

    public ExamResultResolver(ExamResultService examResultService, ProtoBridge bridge) {
        this.examResultService = examResultService;
        this.bridge = bridge;
    }

    @Override
    public void listExamResults(ListExamResultsRequest request, StreamObserver<ListExamResultsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExamResult> rows = examResultService.listExamResults(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExamResultsResponse.newBuilder(),
                rows).build());
    }

    @Override
    public void getExamResult(GetExamResultRequest request, StreamObserver<GetExamResultResponse> out) {
        ExamResult found = examResultService.getExamResult(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExamResultResponse.newBuilder(), "exam_result", found).build());
    }

    @Override
    public void updateExamResult(ExamResultUpdate request, StreamObserver<UpdateExamResultResponse> out) {
        ExamResult updated = examResultService.updateExamResult(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ExamResultProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateExamResultResponse.newBuilder(), "exam_result",
                updated).build());
    }
}
