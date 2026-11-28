package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.LearningObjective;
import com.mysms.learning.lms.business.LearningObjectiveService;
import com.mysms.lms.v1.CreateLearningObjectiveRequest;
import com.mysms.lms.v1.CreateLearningObjectiveResponse;
import com.mysms.lms.v1.DeleteLearningObjectiveRequest;
import com.mysms.lms.v1.DeleteLearningObjectiveResponse;
import com.mysms.lms.v1.GetLearningObjectiveRequest;
import com.mysms.lms.v1.GetLearningObjectiveResponse;
import com.mysms.lms.v1.LearningObjectiveServiceGrpc;
import com.mysms.lms.v1.LearningObjectiveUpdate;
import com.mysms.lms.v1.ListLearningObjectivesRequest;
import com.mysms.lms.v1.ListLearningObjectivesResponse;
import com.mysms.lms.v1.UpdateLearningObjectiveResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.LearningObjectiveService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class LearningObjectiveResolver extends LearningObjectiveServiceGrpc.LearningObjectiveServiceImplBase {

    private final LearningObjectiveService service;
    private final ProtoBridge bridge;

    public LearningObjectiveResolver(LearningObjectiveService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLearningObjectives(ListLearningObjectivesRequest request,
            StreamObserver<ListLearningObjectivesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LearningObjective> rows = service.listLearningObjectives(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLearningObjectivesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLearningObjective(GetLearningObjectiveRequest request, StreamObserver<GetLearningObjectiveResponse> out) {
        LearningObjective found = service.getLearningObjective(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLearningObjectiveResponse.newBuilder(), "learning_objective", found).build());
    }

    @Override
    public void createLearningObjective(CreateLearningObjectiveRequest request,
            StreamObserver<CreateLearningObjectiveResponse> out) {
        LearningObjective created = service.createLearningObjective(
                bridge.body(request.getLearningObjective(), LearningObjective.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLearningObjectiveResponse.newBuilder(), "learning_objective",
                created).build());
    }

    @Override
    public void updateLearningObjective(LearningObjectiveUpdate request,
            StreamObserver<UpdateLearningObjectiveResponse> out) {
        LearningObjective updated = service.updateLearningObjective(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                LearningObjectiveProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLearningObjectiveResponse.newBuilder(), "learning_objective",
                updated).build());
    }

    @Override
    public void deleteLearningObjective(DeleteLearningObjectiveRequest request,
            StreamObserver<DeleteLearningObjectiveResponse> out) {
        service.deleteLearningObjective(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLearningObjectiveResponse.getDefaultInstance());
    }
}
