package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ExternalLink;
import com.mysms.learning.lms.api.model.LiveClass;
import com.mysms.learning.lms.api.model.LiveClassParticipant;
import com.mysms.learning.lms.api.model.LiveClassParticipantList;
import com.mysms.learning.lms.api.model.LiveClassUpdate;
import com.mysms.learning.lms.business.LiveClassService;
import com.mysms.lms.v1.CreateLiveClassRequest;
import com.mysms.lms.v1.CreateLiveClassResponse;
import com.mysms.lms.v1.DeleteLiveClassRequest;
import com.mysms.lms.v1.DeleteLiveClassResponse;
import com.mysms.lms.v1.EndLiveClassRequest;
import com.mysms.lms.v1.EndLiveClassResponse;
import com.mysms.lms.v1.GetLiveClassParticipantRequest;
import com.mysms.lms.v1.GetLiveClassParticipantResponse;
import com.mysms.lms.v1.GetLiveClassRequest;
import com.mysms.lms.v1.GetLiveClassResponse;
import com.mysms.lms.v1.GetLiveClassStartUrlRequest;
import com.mysms.lms.v1.GetLiveClassStartUrlResponse;
import com.mysms.lms.v1.JoinLiveClassRequest;
import com.mysms.lms.v1.JoinLiveClassResponse;
import com.mysms.lms.v1.ListLiveClassParticipantsRequest;
import com.mysms.lms.v1.ListLiveClassParticipantsResponse;
import com.mysms.lms.v1.ListLiveClassesRequest;
import com.mysms.lms.v1.ListLiveClassesResponse;
import com.mysms.lms.v1.LiveClassServiceGrpc;
import com.mysms.lms.v1.RetryLiveClassInvitesRequest;
import com.mysms.lms.v1.RetryLiveClassInvitesResponse;
import com.mysms.lms.v1.ScheduleLiveClassRequest;
import com.mysms.lms.v1.ScheduleLiveClassResponse;
import com.mysms.lms.v1.SyncLiveClassAttendanceRequest;
import com.mysms.lms.v1.SyncLiveClassAttendanceResponse;
import com.mysms.lms.v1.UpdateLiveClassRequest;
import com.mysms.lms.v1.UpdateLiveClassResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.LiveClassService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class LiveClassResolver extends LiveClassServiceGrpc.LiveClassServiceImplBase {

    private final LiveClassService service;
    private final ProtoBridge bridge;

    public LiveClassResolver(LiveClassService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLiveClasses(ListLiveClassesRequest request, StreamObserver<ListLiveClassesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LiveClass> rows = service.listLiveClasss(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListLiveClassesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLiveClass(GetLiveClassRequest request, StreamObserver<GetLiveClassResponse> out) {
        LiveClass found = service.getLiveClass(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLiveClassResponse.newBuilder(), "live_class", found).build());
    }

    @Override
    public void createLiveClass(CreateLiveClassRequest request, StreamObserver<CreateLiveClassResponse> out) {
        LiveClass created = service.createLiveClass(bridge.body(request.getLiveClass(), LiveClass.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLiveClassResponse.newBuilder(), "live_class", created).build());
    }

    @Override
    public void updateLiveClass(UpdateLiveClassRequest request, StreamObserver<UpdateLiveClassResponse> out) {
        LiveClass updated = service.updateLiveClass(bridge.id(request.getLiveClass().getId(), "id"),
                bridge.requiredIfMatch(request.getLiveClass().getEtag()),
                bridge.changes(request.getLiveClass(), request.getUpdateMask(), LiveClassUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLiveClassResponse.newBuilder(), "live_class", updated).build());
    }

    @Override
    public void deleteLiveClass(DeleteLiveClassRequest request, StreamObserver<DeleteLiveClassResponse> out) {
        service.deleteLiveClass(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLiveClassResponse.getDefaultInstance());
    }

    @Override
    public void listLiveClassParticipants(ListLiveClassParticipantsRequest request,
            StreamObserver<ListLiveClassParticipantsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LiveClassParticipant> rows = service.listLiveClassParticipants(bridge.id(request.getLiveClassId(), "liveClassId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLiveClassParticipantsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLiveClassParticipant(GetLiveClassParticipantRequest request,
            StreamObserver<GetLiveClassParticipantResponse> out) {
        LiveClassParticipant found = service.getLiveClassParticipant(bridge.id(request.getLiveClassId(), "liveClassId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetLiveClassParticipantResponse.newBuilder(), "live_class_participant", found).build());
    }

    @Override
    public void scheduleLiveClass(ScheduleLiveClassRequest request, StreamObserver<ScheduleLiveClassResponse> out) {
        LiveClass scheduled = service.scheduleLiveClass(bridge.body(request,
                com.mysms.learning.lms.api.model.ScheduleLiveClassRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(ScheduleLiveClassResponse.newBuilder(), "live_class", scheduled).build());
    }

    @Override
    public void getLiveClassStartUrl(GetLiveClassStartUrlRequest request, StreamObserver<GetLiveClassStartUrlResponse> out) {
        ExternalLink link = service.getLiveClassStartUrl(bridge.id(request.getLiveClassId(), "liveClassId"));
        ProtoBridge.send(out, bridge.reply(GetLiveClassStartUrlResponse.newBuilder(), "result", link).build());
    }

    @Override
    public void joinLiveClass(JoinLiveClassRequest request, StreamObserver<JoinLiveClassResponse> out) {
        ExternalLink link = service.joinLiveClass(bridge.id(request.getLiveClassId(), "liveClassId"));
        ProtoBridge.send(out, bridge.reply(JoinLiveClassResponse.newBuilder(), "result", link).build());
    }

    @Override
    public void retryLiveClassInvites(RetryLiveClassInvitesRequest request,
            StreamObserver<RetryLiveClassInvitesResponse> out) {
        LiveClass queued = service.retryLiveClassInvites(bridge.id(request.getLiveClassId(), "liveClassId"));
        ProtoBridge.send(out, bridge.reply(RetryLiveClassInvitesResponse.newBuilder(), "result", queued).build());
    }

    @Override
    public void endLiveClass(EndLiveClassRequest request, StreamObserver<EndLiveClassResponse> out) {
        LiveClass ended = service.endLiveClass(bridge.id(request.getLiveClassId(), "liveClassId"));
        ProtoBridge.send(out, bridge.reply(EndLiveClassResponse.newBuilder(), "live_class", ended).build());
    }

    @Override
    public void syncLiveClassAttendance(SyncLiveClassAttendanceRequest request,
            StreamObserver<SyncLiveClassAttendanceResponse> out) {
        LiveClassParticipantList attended = service.syncLiveClassAttendance(
                bridge.id(request.getLiveClassId(), "liveClassId"));
        ProtoBridge.send(out, bridge.reply(SyncLiveClassAttendanceResponse.newBuilder(), "items", attended.getItems())
                .build());
    }
}
