package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ParticipationActivity;
import com.mysms.learning.lms.business.ParticipationActivityService;
import com.mysms.lms.v1.CreateParticipationActivityRequest;
import com.mysms.lms.v1.CreateParticipationActivityResponse;
import com.mysms.lms.v1.DeleteParticipationActivityRequest;
import com.mysms.lms.v1.DeleteParticipationActivityResponse;
import com.mysms.lms.v1.GetParticipationActivityRequest;
import com.mysms.lms.v1.GetParticipationActivityResponse;
import com.mysms.lms.v1.ListParticipationActivitiesRequest;
import com.mysms.lms.v1.ListParticipationActivitiesResponse;
import com.mysms.lms.v1.ParticipationActivityServiceGrpc;
import com.mysms.lms.v1.ParticipationActivityUpdate;
import com.mysms.lms.v1.UpdateParticipationActivityResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ParticipationActivityService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ParticipationActivityResolver extends ParticipationActivityServiceGrpc.ParticipationActivityServiceImplBase {

    private final ParticipationActivityService service;
    private final ProtoBridge bridge;

    public ParticipationActivityResolver(ParticipationActivityService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listParticipationActivities(ListParticipationActivitiesRequest request,
            StreamObserver<ListParticipationActivitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ParticipationActivity> rows = service.listParticipationActivitys(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListParticipationActivitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getParticipationActivity(GetParticipationActivityRequest request,
            StreamObserver<GetParticipationActivityResponse> out) {
        ParticipationActivity found = service.getParticipationActivity(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetParticipationActivityResponse.newBuilder(), "participation_activity",
                found).build());
    }

    @Override
    public void createParticipationActivity(CreateParticipationActivityRequest request,
            StreamObserver<CreateParticipationActivityResponse> out) {
        ParticipationActivity created = service.createParticipationActivity(
                bridge.body(request.getParticipationActivity(), ParticipationActivity.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateParticipationActivityResponse.newBuilder(), "participation_activity",
                created).build());
    }

    @Override
    public void updateParticipationActivity(ParticipationActivityUpdate request,
            StreamObserver<UpdateParticipationActivityResponse> out) {
        ParticipationActivity updated = service.updateParticipationActivity(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ParticipationActivityProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateParticipationActivityResponse.newBuilder(), "participation_activity",
                updated).build());
    }

    @Override
    public void deleteParticipationActivity(DeleteParticipationActivityRequest request,
            StreamObserver<DeleteParticipationActivityResponse> out) {
        service.deleteParticipationActivity(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteParticipationActivityResponse.getDefaultInstance());
    }
}
