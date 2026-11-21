package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.Notice;
import com.mysms.privacy.api.model.NoticeTranslation;
import com.mysms.privacy.api.model.NoticeTranslationUpdate;
import com.mysms.privacy.api.model.NoticeUpdate;
import com.mysms.privacy.api.model.NoticeVersion;
import com.mysms.privacy.api.model.NoticeVersionUpdate;
import com.mysms.privacy.business.NoticeService;
import com.mysms.privacy.v1.CreateNoticeRequest;
import com.mysms.privacy.v1.CreateNoticeResponse;
import com.mysms.privacy.v1.CreateNoticeTranslationRequest;
import com.mysms.privacy.v1.CreateNoticeTranslationResponse;
import com.mysms.privacy.v1.CreateNoticeVersionRequest;
import com.mysms.privacy.v1.CreateNoticeVersionResponse;
import com.mysms.privacy.v1.DeleteNoticeRequest;
import com.mysms.privacy.v1.DeleteNoticeResponse;
import com.mysms.privacy.v1.DeleteNoticeTranslationRequest;
import com.mysms.privacy.v1.DeleteNoticeTranslationResponse;
import com.mysms.privacy.v1.GetNoticeRequest;
import com.mysms.privacy.v1.GetNoticeResponse;
import com.mysms.privacy.v1.GetNoticeTranslationRequest;
import com.mysms.privacy.v1.GetNoticeTranslationResponse;
import com.mysms.privacy.v1.GetNoticeVersionRequest;
import com.mysms.privacy.v1.GetNoticeVersionResponse;
import com.mysms.privacy.v1.ListNoticeTranslationsRequest;
import com.mysms.privacy.v1.ListNoticeTranslationsResponse;
import com.mysms.privacy.v1.ListNoticeVersionsRequest;
import com.mysms.privacy.v1.ListNoticeVersionsResponse;
import com.mysms.privacy.v1.ListNoticesRequest;
import com.mysms.privacy.v1.ListNoticesResponse;
import com.mysms.privacy.v1.NoticeServiceGrpc;
import com.mysms.privacy.v1.PublishNoticeVersionRequest;
import com.mysms.privacy.v1.PublishNoticeVersionResponse;
import com.mysms.privacy.v1.UpdateNoticeRequest;
import com.mysms.privacy.v1.UpdateNoticeResponse;
import com.mysms.privacy.v1.UpdateNoticeTranslationRequest;
import com.mysms.privacy.v1.UpdateNoticeTranslationResponse;
import com.mysms.privacy.v1.UpdateNoticeVersionRequest;
import com.mysms.privacy.v1.UpdateNoticeVersionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.NoticeService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class NoticeResolver extends NoticeServiceGrpc.NoticeServiceImplBase {

    private static final String NOTICE = "notice";
    private static final String NOTICE_VERSION = "notice_version";
    private static final String NOTICE_TRANSLATION = "notice_translation";

    private final NoticeService service;
    private final ProtoBridge bridge;

    public NoticeResolver(NoticeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listNotices(ListNoticesRequest request, StreamObserver<ListNoticesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Notice> rows = service.listNotices(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNoticesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotice(GetNoticeRequest request, StreamObserver<GetNoticeResponse> out) {
        Notice found = service.getNotice(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNoticeResponse.newBuilder(), NOTICE, found).build());
    }

    @Override
    public void createNotice(CreateNoticeRequest request, StreamObserver<CreateNoticeResponse> out) {
        Notice created = service.createNotice(bridge.body(request.getNotice(), Notice.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateNoticeResponse.newBuilder(), NOTICE, created).build());
    }

    @Override
    public void updateNotice(UpdateNoticeRequest request, StreamObserver<UpdateNoticeResponse> out) {
        Notice updated = service.updateNotice(bridge.id(request.getNotice().getId(), "id"),
                bridge.requiredIfMatch(request.getNotice().getEtag()),
                bridge.changes(request.getNotice(), request.getUpdateMask(), NoticeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateNoticeResponse.newBuilder(), NOTICE, updated).build());
    }

    @Override
    public void deleteNotice(DeleteNoticeRequest request, StreamObserver<DeleteNoticeResponse> out) {
        service.deleteNotice(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNoticeResponse.getDefaultInstance());
    }

    @Override
    public void listNoticeVersions(ListNoticeVersionsRequest request, StreamObserver<ListNoticeVersionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NoticeVersion> rows = service.listNoticeVersions(bridge.id(request.getNoticeId(), "noticeId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNoticeVersionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNoticeVersion(GetNoticeVersionRequest request, StreamObserver<GetNoticeVersionResponse> out) {
        NoticeVersion found = service.getNoticeVersion(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNoticeVersionResponse.newBuilder(), NOTICE_VERSION, found).build());
    }

    @Override
    public void createNoticeVersion(CreateNoticeVersionRequest request,
            StreamObserver<CreateNoticeVersionResponse> out) {
        NoticeVersion created = service.createNoticeVersion(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.body(request.getNoticeVersion(), NoticeVersion.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateNoticeVersionResponse.newBuilder(), NOTICE_VERSION, created).build());
    }

    @Override
    public void updateNoticeVersion(UpdateNoticeVersionRequest request,
            StreamObserver<UpdateNoticeVersionResponse> out) {
        NoticeVersion updated = service.updateNoticeVersion(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.id(request.getNoticeVersion().getId(), "id"),
                bridge.requiredIfMatch(request.getNoticeVersion().getEtag()),
                bridge.changes(request.getNoticeVersion(), request.getUpdateMask(), NoticeVersionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateNoticeVersionResponse.newBuilder(), NOTICE_VERSION, updated).build());
    }

    /** The REST body (the workflow etag) is the request without its path ids and idempotency key. */
    @Override
    public void publishNoticeVersion(PublishNoticeVersionRequest request,
            StreamObserver<PublishNoticeVersionResponse> out) {
        NoticeVersion changed = service.publishNoticeVersion(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.id(request.getNoticeVersionId(), "noticeVersionId"),
                bridge.body(request, com.mysms.privacy.api.model.PublishNoticeVersionRequest.class, "notice_id",
                        "notice_version_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(PublishNoticeVersionResponse.newBuilder(), NOTICE_VERSION, changed).build());
    }

    @Override
    public void listNoticeTranslations(ListNoticeTranslationsRequest request,
            StreamObserver<ListNoticeTranslationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NoticeTranslation> rows = service.listNoticeTranslations(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.id(request.getNoticeVersionId(), "noticeVersionId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNoticeTranslationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNoticeTranslation(GetNoticeTranslationRequest request,
            StreamObserver<GetNoticeTranslationResponse> out) {
        NoticeTranslation found = service.getNoticeTranslation(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.id(request.getNoticeVersionId(), "noticeVersionId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNoticeTranslationResponse.newBuilder(), NOTICE_TRANSLATION, found)
                .build());
    }

    @Override
    public void createNoticeTranslation(CreateNoticeTranslationRequest request,
            StreamObserver<CreateNoticeTranslationResponse> out) {
        NoticeTranslation created = service.createNoticeTranslation(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.id(request.getNoticeVersionId(), "noticeVersionId"),
                bridge.body(request.getNoticeTranslation(), NoticeTranslation.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateNoticeTranslationResponse.newBuilder(), NOTICE_TRANSLATION, created)
                .build());
    }

    @Override
    public void updateNoticeTranslation(UpdateNoticeTranslationRequest request,
            StreamObserver<UpdateNoticeTranslationResponse> out) {
        NoticeTranslation updated = service.updateNoticeTranslation(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.id(request.getNoticeVersionId(), "noticeVersionId"),
                bridge.id(request.getNoticeTranslation().getId(), "id"),
                bridge.requiredIfMatch(request.getNoticeTranslation().getEtag()),
                bridge.changes(request.getNoticeTranslation(), request.getUpdateMask(), NoticeTranslationUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateNoticeTranslationResponse.newBuilder(), NOTICE_TRANSLATION, updated)
                .build());
    }

    @Override
    public void deleteNoticeTranslation(DeleteNoticeTranslationRequest request,
            StreamObserver<DeleteNoticeTranslationResponse> out) {
        service.deleteNoticeTranslation(bridge.id(request.getNoticeId(), "noticeId"),
                bridge.id(request.getNoticeVersionId(), "noticeVersionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNoticeTranslationResponse.getDefaultInstance());
    }
}
