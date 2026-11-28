package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ContentView;
import com.mysms.comms.business.ContentViewService;
import com.mysms.comms.v1.ContentViewServiceGrpc;
import com.mysms.comms.v1.ContentViewUpdate;
import com.mysms.comms.v1.CreateContentViewRequest;
import com.mysms.comms.v1.CreateContentViewResponse;
import com.mysms.comms.v1.DeleteContentViewRequest;
import com.mysms.comms.v1.DeleteContentViewResponse;
import com.mysms.comms.v1.GetContentViewRequest;
import com.mysms.comms.v1.GetContentViewResponse;
import com.mysms.comms.v1.ListContentViewsRequest;
import com.mysms.comms.v1.ListContentViewsResponse;
import com.mysms.comms.v1.UpdateContentViewResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.ContentViewService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ContentViewResolver extends ContentViewServiceGrpc.ContentViewServiceImplBase {

    private final ContentViewService service;
    private final ProtoBridge bridge;

    public ContentViewResolver(ContentViewService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listContentViews(ListContentViewsRequest request, StreamObserver<ListContentViewsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ContentView> rows = service.listContentViews(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListContentViewsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getContentView(GetContentViewRequest request, StreamObserver<GetContentViewResponse> out) {
        ContentView found = service.getContentView(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetContentViewResponse.newBuilder(), "content_view", found).build());
    }

    @Override
    public void createContentView(CreateContentViewRequest request, StreamObserver<CreateContentViewResponse> out) {
        ContentView created = service.createContentView(
                bridge.body(request.getContentView(), ContentView.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateContentViewResponse.newBuilder(), "content_view", created).build());
    }

    @Override
    public void updateContentView(ContentViewUpdate request, StreamObserver<UpdateContentViewResponse> out) {
        ContentView updated = service.updateContentView(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                ContentViewProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateContentViewResponse.newBuilder(), "content_view", updated).build());
    }

    @Override
    public void deleteContentView(DeleteContentViewRequest request, StreamObserver<DeleteContentViewResponse> out) {
        service.deleteContentView(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteContentViewResponse.getDefaultInstance());
    }
}
