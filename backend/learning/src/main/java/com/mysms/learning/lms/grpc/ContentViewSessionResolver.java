package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ContentViewSession;
import com.mysms.learning.lms.business.ContentViewSessionService;
import com.mysms.lms.v1.ContentViewSessionServiceGrpc;
import com.mysms.lms.v1.ContentViewSessionUpdate;
import com.mysms.lms.v1.CreateContentViewSessionRequest;
import com.mysms.lms.v1.CreateContentViewSessionResponse;
import com.mysms.lms.v1.GetContentViewSessionRequest;
import com.mysms.lms.v1.GetContentViewSessionResponse;
import com.mysms.lms.v1.ListContentViewSessionsRequest;
import com.mysms.lms.v1.ListContentViewSessionsResponse;
import com.mysms.lms.v1.UpdateContentViewSessionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ContentViewSessionService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ContentViewSessionResolver extends ContentViewSessionServiceGrpc.ContentViewSessionServiceImplBase {

    private final ContentViewSessionService service;
    private final ProtoBridge bridge;

    public ContentViewSessionResolver(ContentViewSessionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listContentViewSessions(ListContentViewSessionsRequest request,
            StreamObserver<ListContentViewSessionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ContentViewSession> rows = service.listContentViewSessions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListContentViewSessionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getContentViewSession(GetContentViewSessionRequest request,
            StreamObserver<GetContentViewSessionResponse> out) {
        ContentViewSession found = service.getContentViewSession(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetContentViewSessionResponse.newBuilder(), "content_view_session", found).build());
    }

    @Override
    public void createContentViewSession(CreateContentViewSessionRequest request,
            StreamObserver<CreateContentViewSessionResponse> out) {
        ContentViewSession created = service.createContentViewSession(
                bridge.body(request.getContentViewSession(), ContentViewSession.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateContentViewSessionResponse.newBuilder(), "content_view_session", created).build());
    }

    @Override
    public void updateContentViewSession(ContentViewSessionUpdate request,
            StreamObserver<UpdateContentViewSessionResponse> out) {
        ContentViewSession updated = service.updateContentViewSession(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ContentViewSessionProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateContentViewSessionResponse.newBuilder(), "content_view_session", updated).build());
    }
}
