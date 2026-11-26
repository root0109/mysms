package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.WebResource;
import com.mysms.learning.lms.api.model.WebResourceUpdate;
import com.mysms.learning.lms.business.WebResourceService;
import com.mysms.lms.v1.CreateWebResourceRequest;
import com.mysms.lms.v1.CreateWebResourceResponse;
import com.mysms.lms.v1.DeleteWebResourceRequest;
import com.mysms.lms.v1.DeleteWebResourceResponse;
import com.mysms.lms.v1.GetWebResourceRequest;
import com.mysms.lms.v1.GetWebResourceResponse;
import com.mysms.lms.v1.ListWebResourcesRequest;
import com.mysms.lms.v1.ListWebResourcesResponse;
import com.mysms.lms.v1.UpdateWebResourceRequest;
import com.mysms.lms.v1.UpdateWebResourceResponse;
import com.mysms.lms.v1.WebResourceServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.WebResourceService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class WebResourceResolver extends WebResourceServiceGrpc.WebResourceServiceImplBase {

    private final WebResourceService service;
    private final ProtoBridge bridge;

    public WebResourceResolver(WebResourceService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listWebResources(ListWebResourcesRequest request, StreamObserver<ListWebResourcesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<WebResource> rows = service.listWebResources(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListWebResourcesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getWebResource(GetWebResourceRequest request, StreamObserver<GetWebResourceResponse> out) {
        WebResource found = service.getWebResource(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetWebResourceResponse.newBuilder(), "web_resource", found).build());
    }

    @Override
    public void createWebResource(CreateWebResourceRequest request, StreamObserver<CreateWebResourceResponse> out) {
        WebResource created = service.createWebResource(bridge.body(request.getWebResource(), WebResource.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateWebResourceResponse.newBuilder(), "web_resource", created).build());
    }

    @Override
    public void updateWebResource(UpdateWebResourceRequest request, StreamObserver<UpdateWebResourceResponse> out) {
        WebResource updated = service.updateWebResource(bridge.id(request.getWebResource().getId(), "id"),
                bridge.requiredIfMatch(request.getWebResource().getEtag()),
                bridge.changes(request.getWebResource(), request.getUpdateMask(), WebResourceUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateWebResourceResponse.newBuilder(), "web_resource", updated).build());
    }

    @Override
    public void deleteWebResource(DeleteWebResourceRequest request, StreamObserver<DeleteWebResourceResponse> out) {
        service.deleteWebResource(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteWebResourceResponse.getDefaultInstance());
    }
}
