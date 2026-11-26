package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.ExternalToolWorkflowService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ExternalToolLink;
import com.mysms.learning.lms.api.model.ExternalToolLinkUpdate;
import com.mysms.learning.lms.api.model.ExternalToolResource;
import com.mysms.learning.lms.api.model.ExternalToolResourceUpdate;
import com.mysms.learning.lms.business.ExternalToolResourceService;
import com.mysms.lms.v1.CreateExternalToolLinkRequest;
import com.mysms.lms.v1.CreateExternalToolLinkResponse;
import com.mysms.lms.v1.CreateExternalToolResourceRequest;
import com.mysms.lms.v1.CreateExternalToolResourceResponse;
import com.mysms.lms.v1.DeleteExternalToolLinkRequest;
import com.mysms.lms.v1.DeleteExternalToolLinkResponse;
import com.mysms.lms.v1.DeleteExternalToolResourceRequest;
import com.mysms.lms.v1.DeleteExternalToolResourceResponse;
import com.mysms.lms.v1.ExternalToolResourceServiceGrpc;
import com.mysms.lms.v1.GetExternalToolLinkRequest;
import com.mysms.lms.v1.GetExternalToolLinkResponse;
import com.mysms.lms.v1.GetExternalToolResourceRequest;
import com.mysms.lms.v1.GetExternalToolResourceResponse;
import com.mysms.lms.v1.LaunchExternalToolRequest;
import com.mysms.lms.v1.LaunchExternalToolResponse;
import com.mysms.lms.v1.ListExternalToolLinksRequest;
import com.mysms.lms.v1.ListExternalToolLinksResponse;
import com.mysms.lms.v1.ListExternalToolResourcesRequest;
import com.mysms.lms.v1.ListExternalToolResourcesResponse;
import com.mysms.lms.v1.ReceiveToolOutcomeRequest;
import com.mysms.lms.v1.ReceiveToolOutcomeResponse;
import com.mysms.lms.v1.UpdateExternalToolLinkRequest;
import com.mysms.lms.v1.UpdateExternalToolLinkResponse;
import com.mysms.lms.v1.UpdateExternalToolResourceRequest;
import com.mysms.lms.v1.UpdateExternalToolResourceResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ExternalToolResourceService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ExternalToolResourceResolver extends ExternalToolResourceServiceGrpc.ExternalToolResourceServiceImplBase {

    private final ExternalToolResourceService service;
    private final ProtoBridge bridge;
    private final ExternalToolWorkflowService workflows;

    public ExternalToolResourceResolver(ExternalToolResourceService service, ProtoBridge bridge,
            ExternalToolWorkflowService workflows) {
        this.workflows = workflows;
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listExternalToolResources(ListExternalToolResourcesRequest request,
            StreamObserver<ListExternalToolResourcesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExternalToolResource> rows = service.listExternalToolResources(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExternalToolResourcesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExternalToolResource(GetExternalToolResourceRequest request,
            StreamObserver<GetExternalToolResourceResponse> out) {
        ExternalToolResource found = service.getExternalToolResource(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetExternalToolResourceResponse.newBuilder(), "external_tool_resource", found).build());
    }

    @Override
    public void createExternalToolResource(CreateExternalToolResourceRequest request,
            StreamObserver<CreateExternalToolResourceResponse> out) {
        ExternalToolResource created = service.createExternalToolResource(
                bridge.body(request.getExternalToolResource(), ExternalToolResource.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateExternalToolResourceResponse.newBuilder(), "external_tool_resource", created).build());
    }

    @Override
    public void updateExternalToolResource(UpdateExternalToolResourceRequest request,
            StreamObserver<UpdateExternalToolResourceResponse> out) {
        ExternalToolResource updated = service.updateExternalToolResource(
                bridge.id(request.getExternalToolResource().getId(), "id"),
                bridge.requiredIfMatch(request.getExternalToolResource().getEtag()),
                bridge.changes(request.getExternalToolResource(), request.getUpdateMask(),
                        ExternalToolResourceUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateExternalToolResourceResponse.newBuilder(), "external_tool_resource", updated).build());
    }

    @Override
    public void deleteExternalToolResource(DeleteExternalToolResourceRequest request,
            StreamObserver<DeleteExternalToolResourceResponse> out) {
        service.deleteExternalToolResource(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExternalToolResourceResponse.getDefaultInstance());
    }

    @Override
    public void listExternalToolLinks(ListExternalToolLinksRequest request,
            StreamObserver<ListExternalToolLinksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ExternalToolLink> rows = service.listExternalToolLinks(
                bridge.id(request.getExternalToolResourceId(), "externalToolResourceId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListExternalToolLinksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getExternalToolLink(GetExternalToolLinkRequest request,
            StreamObserver<GetExternalToolLinkResponse> out) {
        ExternalToolLink found = service.getExternalToolLink(
                bridge.id(request.getExternalToolResourceId(), "externalToolResourceId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetExternalToolLinkResponse.newBuilder(), "external_tool_link", found).build());
    }

    @Override
    public void createExternalToolLink(CreateExternalToolLinkRequest request,
            StreamObserver<CreateExternalToolLinkResponse> out) {
        ExternalToolLink created = service.createExternalToolLink(
                bridge.id(request.getExternalToolResourceId(), "externalToolResourceId"),
                bridge.body(request.getExternalToolLink(), ExternalToolLink.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateExternalToolLinkResponse.newBuilder(), "external_tool_link", created).build());
    }

    @Override
    public void updateExternalToolLink(UpdateExternalToolLinkRequest request,
            StreamObserver<UpdateExternalToolLinkResponse> out) {
        ExternalToolLink updated = service.updateExternalToolLink(
                bridge.id(request.getExternalToolResourceId(), "externalToolResourceId"),
                bridge.id(request.getExternalToolLink().getId(), "id"),
                bridge.requiredIfMatch(request.getExternalToolLink().getEtag()),
                bridge.changes(request.getExternalToolLink(), request.getUpdateMask(), ExternalToolLinkUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateExternalToolLinkResponse.newBuilder(), "external_tool_link", updated).build());
    }

    @Override
    public void deleteExternalToolLink(DeleteExternalToolLinkRequest request,
            StreamObserver<DeleteExternalToolLinkResponse> out) {
        service.deleteExternalToolLink(
                bridge.id(request.getExternalToolResourceId(), "externalToolResourceId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteExternalToolLinkResponse.getDefaultInstance());
    }

    @Override
    public void launchExternalTool(LaunchExternalToolRequest request, StreamObserver<LaunchExternalToolResponse> out) {
        ProtoBridge.send(out, bridge.reply(LaunchExternalToolResponse.newBuilder(), "result",
                workflows.launchExternalTool(bridge.id(request.getExternalToolResourceId(), "externalToolResourceId"),
                        bridge.id(request.getExternalToolLinkId(), "externalToolLinkId"),
                        bridge.body(request, com.mysms.learning.lms.api.model.LaunchExternalToolRequest.class,
                                "external_tool_resource_id", "external_tool_link_id", "request_id"))).build());
    }

    /** A provider webhook with a signed raw body: REST only; UNIMPLEMENTED over gRPC. */
    @Override
    public void receiveToolOutcome(ReceiveToolOutcomeRequest request, StreamObserver<ReceiveToolOutcomeResponse> out) {
        workflows.receiveToolOutcome(request.getHeadersOrDefault("authorization", null),
                request.getRawBody().toByteArray());
        ProtoBridge.send(out, ReceiveToolOutcomeResponse.getDefaultInstance());
    }
}
