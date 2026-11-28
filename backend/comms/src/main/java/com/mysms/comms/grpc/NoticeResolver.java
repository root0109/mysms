package com.mysms.comms.grpc;

import com.mysms.comms.api.model.Notice;
import com.mysms.comms.api.model.NoticeAudience;
import com.mysms.comms.api.model.NoticeVersion;
import com.mysms.comms.business.NoticeService;
import com.mysms.comms.v1.CloseNoticeRepliesRequest;
import com.mysms.comms.v1.CloseNoticeRepliesResponse;
import com.mysms.comms.v1.CreateNoticeAudienceRequest;
import com.mysms.comms.v1.CreateNoticeAudienceResponse;
import com.mysms.comms.v1.CreateNoticeRequest;
import com.mysms.comms.v1.CreateNoticeResponse;
import com.mysms.comms.v1.DeactivateNoticeRequest;
import com.mysms.comms.v1.DeactivateNoticeResponse;
import com.mysms.comms.v1.DeleteNoticeAudienceRequest;
import com.mysms.comms.v1.DeleteNoticeAudienceResponse;
import com.mysms.comms.v1.DeleteNoticeRequest;
import com.mysms.comms.v1.DeleteNoticeResponse;
import com.mysms.comms.v1.GetNoticeAudienceRequest;
import com.mysms.comms.v1.GetNoticeAudienceResponse;
import com.mysms.comms.v1.GetNoticeRequest;
import com.mysms.comms.v1.GetNoticeResponse;
import com.mysms.comms.v1.GetNoticeVersionRequest;
import com.mysms.comms.v1.GetNoticeVersionResponse;
import com.mysms.comms.v1.ListNoticeAudiencesRequest;
import com.mysms.comms.v1.ListNoticeAudiencesResponse;
import com.mysms.comms.v1.ListNoticeVersionsRequest;
import com.mysms.comms.v1.ListNoticeVersionsResponse;
import com.mysms.comms.v1.ListNoticesRequest;
import com.mysms.comms.v1.ListNoticesResponse;
import com.mysms.comms.v1.NoticeAudienceUpdate;
import com.mysms.comms.v1.NoticeServiceGrpc;
import com.mysms.comms.v1.NoticeUpdate;
import com.mysms.comms.v1.PublishNoticeRequest;
import com.mysms.comms.v1.PublishNoticeResponse;
import com.mysms.comms.v1.ReactivateNoticeRequest;
import com.mysms.comms.v1.ReactivateNoticeResponse;
import com.mysms.comms.v1.UnpublishNoticeRequest;
import com.mysms.comms.v1.UnpublishNoticeResponse;
import com.mysms.comms.v1.UpdateNoticeAudienceResponse;
import com.mysms.comms.v1.UpdateNoticeResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.NoticeService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class NoticeResolver extends NoticeServiceGrpc.NoticeServiceImplBase {

    private final NoticeService service;
    private final ProtoBridge bridge;

    public NoticeResolver(NoticeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listNotices(ListNoticesRequest request, StreamObserver<ListNoticesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Notice> rows = service.listNotices(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNoticesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotice(GetNoticeRequest request, StreamObserver<GetNoticeResponse> out) {
        Notice found = service.getNotice(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNoticeResponse.newBuilder(), "notice", found).build());
    }

    @Override
    public void createNotice(CreateNoticeRequest request, StreamObserver<CreateNoticeResponse> out) {
        Notice created = service.createNotice(bridge.body(request.getNotice(), Notice.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateNoticeResponse.newBuilder(), "notice", created).build());
    }

    @Override
    public void updateNotice(NoticeUpdate request, StreamObserver<UpdateNoticeResponse> out) {
        Notice updated = service.updateNotice(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                NoticeProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateNoticeResponse.newBuilder(), "notice", updated).build());
    }

    @Override
    public void deleteNotice(DeleteNoticeRequest request, StreamObserver<DeleteNoticeResponse> out) {
        service.deleteNotice(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNoticeResponse.getDefaultInstance());
    }

    @Override
    public void listNoticeAudiences(ListNoticeAudiencesRequest request, StreamObserver<ListNoticeAudiencesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NoticeAudience> rows = service.listNoticeAudiences(
                bridge.id(request.getNoticeId(), "noticeId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNoticeAudiencesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNoticeAudience(GetNoticeAudienceRequest request, StreamObserver<GetNoticeAudienceResponse> out) {
        NoticeAudience found = service.getNoticeAudience(
                bridge.id(request.getNoticeId(), "noticeId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNoticeAudienceResponse.newBuilder(), "notice_audience", found).build());
    }

    @Override
    public void createNoticeAudience(CreateNoticeAudienceRequest request, StreamObserver<CreateNoticeAudienceResponse> out) {
        NoticeAudience created = service.createNoticeAudience(
                bridge.id(request.getNoticeId(), "noticeId"), bridge.body(request.getNoticeAudience(), NoticeAudience.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateNoticeAudienceResponse.newBuilder(), "notice_audience", created).build());
    }

    @Override
    public void updateNoticeAudience(NoticeAudienceUpdate request, StreamObserver<UpdateNoticeAudienceResponse> out) {
        NoticeAudience updated = service.updateNoticeAudience(
                bridge.id(request.getNoticeId(), "noticeId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                NoticeAudienceProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateNoticeAudienceResponse.newBuilder(), "notice_audience", updated).build());
    }

    @Override
    public void deleteNoticeAudience(DeleteNoticeAudienceRequest request, StreamObserver<DeleteNoticeAudienceResponse> out) {
        service.deleteNoticeAudience(
                bridge.id(request.getNoticeId(), "noticeId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNoticeAudienceResponse.getDefaultInstance());
    }

    @Override
    public void listNoticeVersions(ListNoticeVersionsRequest request, StreamObserver<ListNoticeVersionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NoticeVersion> rows = service.listNoticeVersions(
                bridge.id(request.getNoticeId(), "noticeId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNoticeVersionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNoticeVersion(GetNoticeVersionRequest request, StreamObserver<GetNoticeVersionResponse> out) {
        NoticeVersion found = service.getNoticeVersion(
                bridge.id(request.getNoticeId(), "noticeId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNoticeVersionResponse.newBuilder(), "notice_version", found).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void publishNotice(PublishNoticeRequest request, StreamObserver<PublishNoticeResponse> out) {
        Notice changed = service.publishNotice(
                bridge.id(request.getNoticeId(), "noticeId"),
                bridge.body(request, com.mysms.comms.api.model.PublishNoticeRequest.class, "notice_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(PublishNoticeResponse.newBuilder(), "notice", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void unpublishNotice(UnpublishNoticeRequest request, StreamObserver<UnpublishNoticeResponse> out) {
        Notice changed = service.unpublishNotice(
                bridge.id(request.getNoticeId(), "noticeId"),
                bridge.body(request, com.mysms.comms.api.model.UnpublishNoticeRequest.class, "notice_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(UnpublishNoticeResponse.newBuilder(), "notice", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void closeNoticeReplies(CloseNoticeRepliesRequest request, StreamObserver<CloseNoticeRepliesResponse> out) {
        Notice changed = service.closeNoticeReplies(
                bridge.id(request.getNoticeId(), "noticeId"),
                bridge.body(request, com.mysms.comms.api.model.CloseNoticeRepliesRequest.class, "notice_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CloseNoticeRepliesResponse.newBuilder(), "notice", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void deactivateNotice(DeactivateNoticeRequest request, StreamObserver<DeactivateNoticeResponse> out) {
        Notice changed = service.deactivateNotice(
                bridge.id(request.getNoticeId(), "noticeId"),
                bridge.body(request, com.mysms.comms.api.model.DeactivateNoticeRequest.class, "notice_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(DeactivateNoticeResponse.newBuilder(), "notice", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void reactivateNotice(ReactivateNoticeRequest request, StreamObserver<ReactivateNoticeResponse> out) {
        Notice changed = service.reactivateNotice(
                bridge.id(request.getNoticeId(), "noticeId"),
                bridge.body(request, com.mysms.comms.api.model.ReactivateNoticeRequest.class, "notice_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ReactivateNoticeResponse.newBuilder(), "notice", changed).build());
    }
}
