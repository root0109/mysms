package com.mysms.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.sis.api.model.ActivityType;
import com.mysms.sis.api.model.ActivityTypeUpdate;
import com.mysms.sis.business.ActivityTypeService;
import com.mysms.sis.v1.ActivityTypeServiceGrpc;
import com.mysms.sis.v1.CreateActivityTypeRequest;
import com.mysms.sis.v1.CreateActivityTypeResponse;
import com.mysms.sis.v1.DeleteActivityTypeRequest;
import com.mysms.sis.v1.DeleteActivityTypeResponse;
import com.mysms.sis.v1.GetActivityTypeRequest;
import com.mysms.sis.v1.GetActivityTypeResponse;
import com.mysms.sis.v1.ListActivityTypesRequest;
import com.mysms.sis.v1.ListActivityTypesResponse;
import com.mysms.sis.v1.UpdateActivityTypeRequest;
import com.mysms.sis.v1.UpdateActivityTypeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.ActivityTypeService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ActivityTypeResolver extends ActivityTypeServiceGrpc.ActivityTypeServiceImplBase {

    private final ActivityTypeService service;
    private final ProtoBridge bridge;

    public ActivityTypeResolver(ActivityTypeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listActivityTypes(ListActivityTypesRequest request, StreamObserver<ListActivityTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ActivityType> rows = service.listActivityTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListActivityTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getActivityType(GetActivityTypeRequest request, StreamObserver<GetActivityTypeResponse> out) {
        ActivityType found = service.getActivityType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetActivityTypeResponse.newBuilder(), "activity_type", found).build());
    }

    @Override
    public void createActivityType(CreateActivityTypeRequest request, StreamObserver<CreateActivityTypeResponse> out) {
        ActivityType created = service.createActivityType(
                bridge.body(request.getActivityType(), ActivityType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateActivityTypeResponse.newBuilder(), "activity_type", created).build());
    }

    @Override
    public void updateActivityType(UpdateActivityTypeRequest request, StreamObserver<UpdateActivityTypeResponse> out) {
        ActivityType updated = service.updateActivityType(
                bridge.id(request.getActivityType().getId(), "id"), bridge.requiredIfMatch(request.getActivityType().getEtag()),
                bridge.changes(request.getActivityType(), request.getUpdateMask(), ActivityTypeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateActivityTypeResponse.newBuilder(), "activity_type", updated).build());
    }

    @Override
    public void deleteActivityType(DeleteActivityTypeRequest request, StreamObserver<DeleteActivityTypeResponse> out) {
        service.deleteActivityType(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteActivityTypeResponse.getDefaultInstance());
    }
}
