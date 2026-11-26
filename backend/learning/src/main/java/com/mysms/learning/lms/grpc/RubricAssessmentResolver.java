package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.RubricAssessment;
import com.mysms.learning.lms.api.model.RubricAssessmentUpdate;
import com.mysms.learning.lms.business.RubricAssessmentService;
import com.mysms.lms.v1.CreateRubricAssessmentRequest;
import com.mysms.lms.v1.CreateRubricAssessmentResponse;
import com.mysms.lms.v1.DeleteRubricAssessmentRequest;
import com.mysms.lms.v1.DeleteRubricAssessmentResponse;
import com.mysms.lms.v1.GetRubricAssessmentRequest;
import com.mysms.lms.v1.GetRubricAssessmentResponse;
import com.mysms.lms.v1.ListRubricAssessmentsRequest;
import com.mysms.lms.v1.ListRubricAssessmentsResponse;
import com.mysms.lms.v1.RubricAssessmentServiceGrpc;
import com.mysms.lms.v1.UpdateRubricAssessmentRequest;
import com.mysms.lms.v1.UpdateRubricAssessmentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.RubricAssessmentService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class RubricAssessmentResolver extends RubricAssessmentServiceGrpc.RubricAssessmentServiceImplBase {

    private final RubricAssessmentService service;
    private final ProtoBridge bridge;

    public RubricAssessmentResolver(RubricAssessmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listRubricAssessments(ListRubricAssessmentsRequest request, StreamObserver<ListRubricAssessmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RubricAssessment> rows = service.listRubricAssessments(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRubricAssessmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRubricAssessment(GetRubricAssessmentRequest request, StreamObserver<GetRubricAssessmentResponse> out) {
        RubricAssessment found = service.getRubricAssessment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRubricAssessmentResponse.newBuilder(), "rubric_assessment", found).build());
    }

    @Override
    public void createRubricAssessment(CreateRubricAssessmentRequest request,
            StreamObserver<CreateRubricAssessmentResponse> out) {
        RubricAssessment created = service.createRubricAssessment(
                bridge.body(request.getRubricAssessment(), RubricAssessment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRubricAssessmentResponse.newBuilder(), "rubric_assessment", created).build());
    }

    @Override
    public void updateRubricAssessment(UpdateRubricAssessmentRequest request,
            StreamObserver<UpdateRubricAssessmentResponse> out) {
        RubricAssessment updated = service.updateRubricAssessment(bridge.id(request.getRubricAssessment().getId(), "id"),
                bridge.requiredIfMatch(request.getRubricAssessment().getEtag()),
                bridge.changes(request.getRubricAssessment(), request.getUpdateMask(), RubricAssessmentUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRubricAssessmentResponse.newBuilder(), "rubric_assessment", updated).build());
    }

    @Override
    public void deleteRubricAssessment(DeleteRubricAssessmentRequest request,
            StreamObserver<DeleteRubricAssessmentResponse> out) {
        service.deleteRubricAssessment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRubricAssessmentResponse.getDefaultInstance());
    }
}
