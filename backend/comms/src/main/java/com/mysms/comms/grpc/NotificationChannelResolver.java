package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationChannel;
import com.mysms.comms.api.model.NotificationChannelProperty;
import com.mysms.comms.business.NotificationChannelService;
import com.mysms.comms.v1.CreateNotificationChannelPropertyRequest;
import com.mysms.comms.v1.CreateNotificationChannelPropertyResponse;
import com.mysms.comms.v1.CreateNotificationChannelRequest;
import com.mysms.comms.v1.CreateNotificationChannelResponse;
import com.mysms.comms.v1.DeleteNotificationChannelPropertyRequest;
import com.mysms.comms.v1.DeleteNotificationChannelPropertyResponse;
import com.mysms.comms.v1.DeleteNotificationChannelRequest;
import com.mysms.comms.v1.DeleteNotificationChannelResponse;
import com.mysms.comms.v1.GetNotificationChannelPropertyRequest;
import com.mysms.comms.v1.GetNotificationChannelPropertyResponse;
import com.mysms.comms.v1.GetNotificationChannelRequest;
import com.mysms.comms.v1.GetNotificationChannelResponse;
import com.mysms.comms.v1.ListNotificationChannelPropertiesRequest;
import com.mysms.comms.v1.ListNotificationChannelPropertiesResponse;
import com.mysms.comms.v1.ListNotificationChannelsRequest;
import com.mysms.comms.v1.ListNotificationChannelsResponse;
import com.mysms.comms.v1.NotificationChannelPropertyUpdate;
import com.mysms.comms.v1.NotificationChannelServiceGrpc;
import com.mysms.comms.v1.NotificationChannelUpdate;
import com.mysms.comms.v1.SendTestMessageRequest;
import com.mysms.comms.v1.SendTestMessageResponse;
import com.mysms.comms.v1.UpdateNotificationChannelPropertyResponse;
import com.mysms.comms.v1.UpdateNotificationChannelResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.NotificationChannelService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class NotificationChannelResolver extends NotificationChannelServiceGrpc.NotificationChannelServiceImplBase {

    private final NotificationChannelService service;
    private final ProtoBridge bridge;

    public NotificationChannelResolver(NotificationChannelService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listNotificationChannels(ListNotificationChannelsRequest request,
            StreamObserver<ListNotificationChannelsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NotificationChannel> rows = service.listNotificationChannels(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNotificationChannelsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotificationChannel(GetNotificationChannelRequest request,
            StreamObserver<GetNotificationChannelResponse> out) {
        NotificationChannel found = service.getNotificationChannel(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNotificationChannelResponse.newBuilder(), "notification_channel", found).build());
    }

    @Override
    public void createNotificationChannel(CreateNotificationChannelRequest request,
            StreamObserver<CreateNotificationChannelResponse> out) {
        NotificationChannel created = service.createNotificationChannel(
                bridge.body(request.getNotificationChannel(), NotificationChannel.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateNotificationChannelResponse.newBuilder(), "notification_channel", created).build());
    }

    @Override
    public void updateNotificationChannel(NotificationChannelUpdate request,
            StreamObserver<UpdateNotificationChannelResponse> out) {
        NotificationChannel updated = service.updateNotificationChannel(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                NotificationChannelProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateNotificationChannelResponse.newBuilder(), "notification_channel", updated).build());
    }

    @Override
    public void deleteNotificationChannel(DeleteNotificationChannelRequest request,
            StreamObserver<DeleteNotificationChannelResponse> out) {
        service.deleteNotificationChannel(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNotificationChannelResponse.getDefaultInstance());
    }

    @Override
    public void listNotificationChannelProperties(ListNotificationChannelPropertiesRequest request,
            StreamObserver<ListNotificationChannelPropertiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NotificationChannelProperty> rows = service.listNotificationChannelPropertys(
                bridge.id(request.getNotificationChannelId(), "notificationChannelId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNotificationChannelPropertiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotificationChannelProperty(GetNotificationChannelPropertyRequest request,
            StreamObserver<GetNotificationChannelPropertyResponse> out) {
        NotificationChannelProperty found = service.getNotificationChannelProperty(
                bridge.id(request.getNotificationChannelId(), "notificationChannelId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetNotificationChannelPropertyResponse.newBuilder(), "notification_channel_property",
                found).build());
    }

    @Override
    public void createNotificationChannelProperty(CreateNotificationChannelPropertyRequest request,
            StreamObserver<CreateNotificationChannelPropertyResponse> out) {
        NotificationChannelProperty created = service.createNotificationChannelProperty(
                bridge.id(request.getNotificationChannelId(), "notificationChannelId"),
                bridge.body(request.getNotificationChannelProperty(), NotificationChannelProperty.class),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateNotificationChannelPropertyResponse.newBuilder(), "notification_channel_property",
                created).build());
    }

    @Override
    public void updateNotificationChannelProperty(NotificationChannelPropertyUpdate request,
            StreamObserver<UpdateNotificationChannelPropertyResponse> out) {
        NotificationChannelProperty updated = service.updateNotificationChannelProperty(
                bridge.id(request.getNotificationChannelId(), "notificationChannelId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                NotificationChannelPropertyProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateNotificationChannelPropertyResponse.newBuilder(), "notification_channel_property",
                updated).build());
    }

    @Override
    public void deleteNotificationChannelProperty(DeleteNotificationChannelPropertyRequest request,
            StreamObserver<DeleteNotificationChannelPropertyResponse> out) {
        service.deleteNotificationChannelProperty(
                bridge.id(request.getNotificationChannelId(), "notificationChannelId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNotificationChannelPropertyResponse.getDefaultInstance());
    }

    @Override
    public void sendTestMessage(SendTestMessageRequest request, StreamObserver<SendTestMessageResponse> out) {
        service.sendTestMessage(bridge.id(request.getNotificationChannelId(), "notificationChannelId"),
                bridge.body(request, com.mysms.comms.api.model.SendTestMessageRequest.class, "notification_channel_id",
                        "request_id"));
        ProtoBridge.send(out, SendTestMessageResponse.getDefaultInstance());
    }
}
