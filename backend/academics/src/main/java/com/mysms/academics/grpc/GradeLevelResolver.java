package com.mysms.academics.grpc;

import com.mysms.academics.api.model.GradeLevel;
import com.mysms.academics.api.model.GradeLevelUpdate;
import com.mysms.academics.business.GradeLevelService;
import com.mysms.academics.v1.CreateGradeLevelRequest;
import com.mysms.academics.v1.CreateGradeLevelResponse;
import com.mysms.academics.v1.DeleteGradeLevelRequest;
import com.mysms.academics.v1.DeleteGradeLevelResponse;
import com.mysms.academics.v1.GetGradeLevelRequest;
import com.mysms.academics.v1.GetGradeLevelResponse;
import com.mysms.academics.v1.GradeLevelServiceGrpc;
import com.mysms.academics.v1.ListGradeLevelsRequest;
import com.mysms.academics.v1.ListGradeLevelsResponse;
import com.mysms.academics.v1.UpdateGradeLevelRequest;
import com.mysms.academics.v1.UpdateGradeLevelResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.GradeLevelService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class GradeLevelResolver extends GradeLevelServiceGrpc.GradeLevelServiceImplBase {

    private final GradeLevelService service;
    private final ProtoBridge bridge;

    public GradeLevelResolver(GradeLevelService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listGradeLevels(ListGradeLevelsRequest request, StreamObserver<ListGradeLevelsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<GradeLevel> rows = service.listGradeLevels(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListGradeLevelsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getGradeLevel(GetGradeLevelRequest request, StreamObserver<GetGradeLevelResponse> out) {
        GradeLevel found = service.getGradeLevel(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetGradeLevelResponse.newBuilder(), "grade_level", found).build());
    }

    @Override
    public void createGradeLevel(CreateGradeLevelRequest request, StreamObserver<CreateGradeLevelResponse> out) {
        GradeLevel created = service.createGradeLevel(bridge.body(request.getGradeLevel(), GradeLevel.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateGradeLevelResponse.newBuilder(), "grade_level", created).build());
    }

    @Override
    public void updateGradeLevel(UpdateGradeLevelRequest request, StreamObserver<UpdateGradeLevelResponse> out) {
        GradeLevel updated = service.updateGradeLevel(bridge.id(request.getGradeLevel().getId(), "id"),
                bridge.requiredIfMatch(request.getGradeLevel().getEtag()),
                bridge.changes(request.getGradeLevel(), request.getUpdateMask(), GradeLevelUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateGradeLevelResponse.newBuilder(), "grade_level", updated).build());
    }

    @Override
    public void deleteGradeLevel(DeleteGradeLevelRequest request, StreamObserver<DeleteGradeLevelResponse> out) {
        service.deleteGradeLevel(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteGradeLevelResponse.getDefaultInstance());
    }
}
