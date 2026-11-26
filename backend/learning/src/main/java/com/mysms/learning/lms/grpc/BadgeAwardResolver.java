package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.BadgeAward;
import com.mysms.learning.lms.business.BadgeAwardService;
import com.mysms.lms.v1.BadgeAwardServiceGrpc;
import com.mysms.lms.v1.GetBadgeAwardRequest;
import com.mysms.lms.v1.GetBadgeAwardResponse;
import com.mysms.lms.v1.ListBadgeAwardsRequest;
import com.mysms.lms.v1.ListBadgeAwardsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.BadgeAwardService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class BadgeAwardResolver extends BadgeAwardServiceGrpc.BadgeAwardServiceImplBase {

    private final BadgeAwardService service;
    private final ProtoBridge bridge;

    public BadgeAwardResolver(BadgeAwardService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listBadgeAwards(ListBadgeAwardsRequest request, StreamObserver<ListBadgeAwardsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<BadgeAward> rows = service.listBadgeAwards(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListBadgeAwardsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getBadgeAward(GetBadgeAwardRequest request, StreamObserver<GetBadgeAwardResponse> out) {
        BadgeAward found = service.getBadgeAward(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetBadgeAwardResponse.newBuilder(), "badge_award", found).build());
    }
}
