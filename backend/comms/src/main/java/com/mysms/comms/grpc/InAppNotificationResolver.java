package com.mysms.comms.grpc;

import com.mysms.comms.api.model.InAppNotification;
import com.mysms.comms.business.InAppNotificationService;
import com.mysms.comms.v1.ArchiveNotificationsRequest;
import com.mysms.comms.v1.ArchiveNotificationsResponse;
import com.mysms.comms.v1.GetInAppNotificationRequest;
import com.mysms.comms.v1.GetInAppNotificationResponse;
import com.mysms.comms.v1.InAppNotificationServiceGrpc;
import com.mysms.comms.v1.ListInAppNotificationsRequest;
import com.mysms.comms.v1.ListInAppNotificationsResponse;
import com.mysms.comms.v1.MarkAllNotificationsReadRequest;
import com.mysms.comms.v1.MarkAllNotificationsReadResponse;
import com.mysms.comms.v1.MarkNotificationReadRequest;
import com.mysms.comms.v1.MarkNotificationReadResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/** gRPC resolver of comms.InAppNotificationService: the same business service as REST. */
@GrpcService
public class InAppNotificationResolver extends InAppNotificationServiceGrpc.InAppNotificationServiceImplBase {

    private final InAppNotificationService service;
    private final ProtoBridge bridge;

    public InAppNotificationResolver(InAppNotificationService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listInAppNotifications(ListInAppNotificationsRequest request,
            StreamObserver<ListInAppNotificationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<InAppNotification> rows = service.listInAppNotifications(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListInAppNotificationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getInAppNotification(GetInAppNotificationRequest request,
            StreamObserver<GetInAppNotificationResponse> out) {
        InAppNotification found = service.getInAppNotification(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetInAppNotificationResponse.newBuilder(), "in_app_notification", found)
                .build());
    }

    @Override
    public void markNotificationRead(MarkNotificationReadRequest request,
            StreamObserver<MarkNotificationReadResponse> out) {
        InAppNotification read = service.markNotificationRead(
                bridge.id(request.getInAppNotificationId(), "inAppNotificationId"));
        ProtoBridge.send(out, bridge.reply(MarkNotificationReadResponse.newBuilder(), "in_app_notification", read)
                .build());
    }

    @Override
    public void markAllNotificationsRead(MarkAllNotificationsReadRequest request,
            StreamObserver<MarkAllNotificationsReadResponse> out) {
        service.markAllNotificationsRead();
        ProtoBridge.send(out, MarkAllNotificationsReadResponse.getDefaultInstance());
    }

    @Override
    public void archiveNotifications(ArchiveNotificationsRequest request,
            StreamObserver<ArchiveNotificationsResponse> out) {
        service.archiveNotifications(bridge.body(request, com.mysms.comms.api.model.ArchiveNotificationsRequest.class,
                "request_id"));
        ProtoBridge.send(out, ArchiveNotificationsResponse.getDefaultInstance());
    }
}
