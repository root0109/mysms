package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.Badge;
import com.mysms.learning.lms.api.model.BadgeAward;
import com.mysms.learning.lms.api.model.BadgeUpdate;
import com.mysms.learning.lms.business.BadgeService;
import com.mysms.lms.v1.AwardBadgeRequest;
import com.mysms.lms.v1.AwardBadgeResponse;
import com.mysms.lms.v1.BadgeServiceGrpc;
import com.mysms.lms.v1.CreateBadgeRequest;
import com.mysms.lms.v1.CreateBadgeResponse;
import com.mysms.lms.v1.DeleteBadgeRequest;
import com.mysms.lms.v1.DeleteBadgeResponse;
import com.mysms.lms.v1.GetBadgeRequest;
import com.mysms.lms.v1.GetBadgeResponse;
import com.mysms.lms.v1.ListBadgesRequest;
import com.mysms.lms.v1.ListBadgesResponse;
import com.mysms.lms.v1.UpdateBadgeRequest;
import com.mysms.lms.v1.UpdateBadgeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.BadgeService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class BadgeResolver extends BadgeServiceGrpc.BadgeServiceImplBase {

    private final BadgeService service;
    private final ProtoBridge bridge;

    public BadgeResolver(BadgeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listBadges(ListBadgesRequest request, StreamObserver<ListBadgesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Badge> rows = service.listBadges(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListBadgesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getBadge(GetBadgeRequest request, StreamObserver<GetBadgeResponse> out) {
        Badge found = service.getBadge(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetBadgeResponse.newBuilder(), "badge", found).build());
    }

    @Override
    public void createBadge(CreateBadgeRequest request, StreamObserver<CreateBadgeResponse> out) {
        Badge created = service.createBadge(bridge.body(request.getBadge(), Badge.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateBadgeResponse.newBuilder(), "badge", created).build());
    }

    @Override
    public void updateBadge(UpdateBadgeRequest request, StreamObserver<UpdateBadgeResponse> out) {
        Badge updated = service.updateBadge(bridge.id(request.getBadge().getId(), "id"),
                bridge.requiredIfMatch(request.getBadge().getEtag()),
                bridge.changes(request.getBadge(), request.getUpdateMask(), BadgeUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateBadgeResponse.newBuilder(), "badge", updated).build());
    }

    @Override
    public void deleteBadge(DeleteBadgeRequest request, StreamObserver<DeleteBadgeResponse> out) {
        service.deleteBadge(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteBadgeResponse.getDefaultInstance());
    }

    @Override
    public void awardBadge(AwardBadgeRequest request, StreamObserver<AwardBadgeResponse> out) {
        BadgeAward awarded = service.awardBadge(bridge.id(request.getBadgeId(), "badgeId"),
                bridge.id(request.getUserId(), "userId"));
        ProtoBridge.send(out, bridge.reply(AwardBadgeResponse.newBuilder(), "result", awarded).build());
    }
}
