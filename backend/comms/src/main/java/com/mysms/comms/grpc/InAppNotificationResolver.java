package com.mysms.comms.grpc;

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
import com.mysms.core.error.NotImplementedException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.InAppNotificationService: not served yet (REST answers 501), so every RPC answers
 * UNIMPLEMENTED explicitly rather than relying on the generated stub's default. Authentication and permission are
 * checked at the gRPC edge.
 */
@GrpcService
public class InAppNotificationResolver extends InAppNotificationServiceGrpc.InAppNotificationServiceImplBase {

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void listInAppNotifications(ListInAppNotificationsRequest request,
            StreamObserver<ListInAppNotificationsResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void getInAppNotification(GetInAppNotificationRequest request, StreamObserver<GetInAppNotificationResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void markNotificationRead(MarkNotificationReadRequest request, StreamObserver<MarkNotificationReadResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void markAllNotificationsRead(MarkAllNotificationsReadRequest request,
            StreamObserver<MarkAllNotificationsReadResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void archiveNotifications(ArchiveNotificationsRequest request, StreamObserver<ArchiveNotificationsResponse> out) {
        throw new NotImplementedException();
    }
}
