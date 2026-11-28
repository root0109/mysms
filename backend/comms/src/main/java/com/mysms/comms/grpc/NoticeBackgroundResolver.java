package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NoticeBackground;
import com.mysms.comms.business.NoticeBackgroundService;
import com.mysms.comms.v1.CreateNoticeBackgroundRequest;
import com.mysms.comms.v1.CreateNoticeBackgroundResponse;
import com.mysms.comms.v1.DeleteNoticeBackgroundRequest;
import com.mysms.comms.v1.DeleteNoticeBackgroundResponse;
import com.mysms.comms.v1.GetNoticeBackgroundRequest;
import com.mysms.comms.v1.GetNoticeBackgroundResponse;
import com.mysms.comms.v1.ListNoticeBackgroundsRequest;
import com.mysms.comms.v1.ListNoticeBackgroundsResponse;
import com.mysms.comms.v1.NoticeBackgroundServiceGrpc;
import com.mysms.comms.v1.NoticeBackgroundUpdate;
import com.mysms.comms.v1.UpdateNoticeBackgroundResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.NoticeBackgroundService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class NoticeBackgroundResolver extends NoticeBackgroundServiceGrpc.NoticeBackgroundServiceImplBase {

    private final NoticeBackgroundService service;
    private final ProtoBridge bridge;

    public NoticeBackgroundResolver(NoticeBackgroundService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listNoticeBackgrounds(ListNoticeBackgroundsRequest request, StreamObserver<ListNoticeBackgroundsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NoticeBackground> rows = service.listNoticeBackgrounds(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNoticeBackgroundsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNoticeBackground(GetNoticeBackgroundRequest request, StreamObserver<GetNoticeBackgroundResponse> out) {
        NoticeBackground found = service.getNoticeBackground(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNoticeBackgroundResponse.newBuilder(), "notice_background", found).build());
    }

    @Override
    public void createNoticeBackground(CreateNoticeBackgroundRequest request,
            StreamObserver<CreateNoticeBackgroundResponse> out) {
        NoticeBackground created = service.createNoticeBackground(
                bridge.body(request.getNoticeBackground(), NoticeBackground.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateNoticeBackgroundResponse.newBuilder(), "notice_background", created).build());
    }

    @Override
    public void updateNoticeBackground(NoticeBackgroundUpdate request,
            StreamObserver<UpdateNoticeBackgroundResponse> out) {
        NoticeBackground updated = service.updateNoticeBackground(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                NoticeBackgroundProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateNoticeBackgroundResponse.newBuilder(), "notice_background", updated).build());
    }

    @Override
    public void deleteNoticeBackground(DeleteNoticeBackgroundRequest request,
            StreamObserver<DeleteNoticeBackgroundResponse> out) {
        service.deleteNoticeBackground(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNoticeBackgroundResponse.getDefaultInstance());
    }
}
