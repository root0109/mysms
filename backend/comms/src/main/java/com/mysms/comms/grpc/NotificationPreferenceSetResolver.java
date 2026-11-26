package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationPreference;
import com.mysms.comms.api.model.NotificationPreferenceSet;
import com.mysms.comms.api.model.NotificationPreferenceSetUpdate;
import com.mysms.comms.api.model.NotificationPreferenceUpdate;
import com.mysms.comms.business.NotificationPreferenceSetService;
import com.mysms.comms.v1.CreateNotificationPreferenceRequest;
import com.mysms.comms.v1.CreateNotificationPreferenceResponse;
import com.mysms.comms.v1.CreateNotificationPreferenceSetRequest;
import com.mysms.comms.v1.CreateNotificationPreferenceSetResponse;
import com.mysms.comms.v1.DeleteNotificationPreferenceRequest;
import com.mysms.comms.v1.DeleteNotificationPreferenceResponse;
import com.mysms.comms.v1.DeleteNotificationPreferenceSetRequest;
import com.mysms.comms.v1.DeleteNotificationPreferenceSetResponse;
import com.mysms.comms.v1.GetNotificationPreferenceRequest;
import com.mysms.comms.v1.GetNotificationPreferenceResponse;
import com.mysms.comms.v1.GetNotificationPreferenceSetRequest;
import com.mysms.comms.v1.GetNotificationPreferenceSetResponse;
import com.mysms.comms.v1.ListNotificationPreferenceSetsRequest;
import com.mysms.comms.v1.ListNotificationPreferenceSetsResponse;
import com.mysms.comms.v1.ListNotificationPreferencesRequest;
import com.mysms.comms.v1.ListNotificationPreferencesResponse;
import com.mysms.comms.v1.NotificationPreferenceSetServiceGrpc;
import com.mysms.comms.v1.UpdateNotificationPreferenceRequest;
import com.mysms.comms.v1.UpdateNotificationPreferenceResponse;
import com.mysms.comms.v1.UpdateNotificationPreferenceSetRequest;
import com.mysms.comms.v1.UpdateNotificationPreferenceSetResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.NotificationPreferenceSetService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class NotificationPreferenceSetResolver
        extends NotificationPreferenceSetServiceGrpc.NotificationPreferenceSetServiceImplBase {

    private final NotificationPreferenceSetService service;
    private final ProtoBridge bridge;

    public NotificationPreferenceSetResolver(NotificationPreferenceSetService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listNotificationPreferenceSets(ListNotificationPreferenceSetsRequest request,
            StreamObserver<ListNotificationPreferenceSetsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NotificationPreferenceSet> rows = service.listNotificationPreferenceSets(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNotificationPreferenceSetsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotificationPreferenceSet(GetNotificationPreferenceSetRequest request,
            StreamObserver<GetNotificationPreferenceSetResponse> out) {
        NotificationPreferenceSet found = service.getNotificationPreferenceSet(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetNotificationPreferenceSetResponse.newBuilder(), "notification_preference_set", found).build());
    }

    @Override
    public void createNotificationPreferenceSet(CreateNotificationPreferenceSetRequest request,
            StreamObserver<CreateNotificationPreferenceSetResponse> out) {
        NotificationPreferenceSet created = service.createNotificationPreferenceSet(
                bridge.body(request.getNotificationPreferenceSet(), NotificationPreferenceSet.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateNotificationPreferenceSetResponse.newBuilder(), "notification_preference_set",
                created).build());
    }

    @Override
    public void updateNotificationPreferenceSet(UpdateNotificationPreferenceSetRequest request,
            StreamObserver<UpdateNotificationPreferenceSetResponse> out) {
        NotificationPreferenceSet updated = service.updateNotificationPreferenceSet(
                bridge.id(request.getNotificationPreferenceSet().getId(), "id"),
                bridge.requiredIfMatch(request.getNotificationPreferenceSet().getEtag()),
                bridge.changes(request.getNotificationPreferenceSet(), request.getUpdateMask(),
                NotificationPreferenceSetUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateNotificationPreferenceSetResponse.newBuilder(), "notification_preference_set",
                updated).build());
    }

    @Override
    public void deleteNotificationPreferenceSet(DeleteNotificationPreferenceSetRequest request,
            StreamObserver<DeleteNotificationPreferenceSetResponse> out) {
        service.deleteNotificationPreferenceSet(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNotificationPreferenceSetResponse.getDefaultInstance());
    }

    @Override
    public void listNotificationPreferences(ListNotificationPreferencesRequest request,
            StreamObserver<ListNotificationPreferencesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NotificationPreference> rows = service.listNotificationPreferences(
                bridge.id(request.getNotificationPreferenceSetId(), "notificationPreferenceSetId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNotificationPreferencesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotificationPreference(GetNotificationPreferenceRequest request,
            StreamObserver<GetNotificationPreferenceResponse> out) {
        NotificationPreference found = service.getNotificationPreference(
                bridge.id(request.getNotificationPreferenceSetId(), "notificationPreferenceSetId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetNotificationPreferenceResponse.newBuilder(), "notification_preference", found).build());
    }

    @Override
    public void createNotificationPreference(CreateNotificationPreferenceRequest request,
            StreamObserver<CreateNotificationPreferenceResponse> out) {
        NotificationPreference created = service.createNotificationPreference(
                bridge.id(request.getNotificationPreferenceSetId(), "notificationPreferenceSetId"),
                bridge.body(request.getNotificationPreference(), NotificationPreference.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateNotificationPreferenceResponse.newBuilder(), "notification_preference", created).build());
    }

    @Override
    public void updateNotificationPreference(UpdateNotificationPreferenceRequest request,
            StreamObserver<UpdateNotificationPreferenceResponse> out) {
        NotificationPreference updated = service.updateNotificationPreference(
                bridge.id(request.getNotificationPreferenceSetId(), "notificationPreferenceSetId"),
                bridge.id(request.getNotificationPreference().getId(), "id"),
                bridge.requiredIfMatch(request.getNotificationPreference().getEtag()),
                bridge.changes(request.getNotificationPreference(), request.getUpdateMask(),
                NotificationPreferenceUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateNotificationPreferenceResponse.newBuilder(), "notification_preference", updated).build());
    }

    @Override
    public void deleteNotificationPreference(DeleteNotificationPreferenceRequest request,
            StreamObserver<DeleteNotificationPreferenceResponse> out) {
        service.deleteNotificationPreference(
                bridge.id(request.getNotificationPreferenceSetId(), "notificationPreferenceSetId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNotificationPreferenceResponse.getDefaultInstance());
    }
}
