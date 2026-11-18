package com.mysms.assessment.grpc;

import com.mysms.assessment.api.model.ExamType;
import com.mysms.assessment.api.model.ExamTypeUpdate;
import com.mysms.assessment.business.ExamTypeService;
import com.mysms.assessment.v1.CreateExamTypeRequest;
import com.mysms.assessment.v1.CreateExamTypeResponse;
import com.mysms.assessment.v1.DeleteExamTypeRequest;
import com.mysms.assessment.v1.DeleteExamTypeResponse;
import com.mysms.assessment.v1.ExamTypeServiceGrpc;
import com.mysms.assessment.v1.GetExamTypeRequest;
import com.mysms.assessment.v1.GetExamTypeResponse;
import com.mysms.assessment.v1.ListExamTypesRequest;
import com.mysms.assessment.v1.ListExamTypesResponse;
import com.mysms.assessment.v1.UpdateExamTypeRequest;
import com.mysms.assessment.v1.UpdateExamTypeResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.ExamTypeService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ExamTypeResolver extends ExamTypeServiceGrpc.ExamTypeServiceImplBase {

    private final ExamTypeService examTypeService;
    private final ProtoBridge bridge;

    public ExamTypeResolver(ExamTypeService examTypeService, ProtoBridge bridge) {
        this.examTypeService = examTypeService;
        this.bridge = bridge;
    }

    @Override
    public void listExamTypes(ListExamTypesRequest request, StreamObserver<ListExamTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExamType> rows = examTypeService.listExamTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExamTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExamType(GetExamTypeRequest request, StreamObserver<GetExamTypeResponse> out) {
        ExamType found = examTypeService.getExamType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetExamTypeResponse.newBuilder(), "exam_type", found).build());
    }

    @Override
    public void createExamType(CreateExamTypeRequest request, StreamObserver<CreateExamTypeResponse> out) {
        ExamType created = examTypeService.createExamType(
                bridge.body(request.getExamType(), ExamType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateExamTypeResponse.newBuilder(), "exam_type", created).build());
    }

    @Override
    public void updateExamType(UpdateExamTypeRequest request, StreamObserver<UpdateExamTypeResponse> out) {
        ExamType updated = examTypeService.updateExamType(
                bridge.id(request.getExamType().getId(), "id"),
                bridge.requiredIfMatch(request.getExamType().getEtag()),
                bridge.changes(request.getExamType(), request.getUpdateMask(), ExamTypeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateExamTypeResponse.newBuilder(), "exam_type", updated).build());
    }

    @Override
    public void deleteExamType(DeleteExamTypeRequest request, StreamObserver<DeleteExamTypeResponse> out) {
        examTypeService.deleteExamType(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExamTypeResponse.getDefaultInstance());
    }
}
