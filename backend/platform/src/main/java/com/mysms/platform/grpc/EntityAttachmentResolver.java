package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.EntityAttachment;
import com.mysms.platform.business.EntityAttachmentService;
import com.mysms.platform.v1.CreateEntityAttachmentRequest;
import com.mysms.platform.v1.CreateEntityAttachmentResponse;
import com.mysms.platform.v1.DeleteEntityAttachmentRequest;
import com.mysms.platform.v1.DeleteEntityAttachmentResponse;
import com.mysms.platform.v1.EntityAttachmentServiceGrpc;
import com.mysms.platform.v1.ListEntityAttachmentsRequest;
import com.mysms.platform.v1.ListEntityAttachmentsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.EntityAttachmentService: delegates every RPC to the business service REST uses, so the
 * rules exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class EntityAttachmentResolver extends EntityAttachmentServiceGrpc.EntityAttachmentServiceImplBase {

    private final EntityAttachmentService service;
    private final ProtoBridge bridge;

    public EntityAttachmentResolver(EntityAttachmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listEntityAttachments(ListEntityAttachmentsRequest request,
            StreamObserver<ListEntityAttachmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EntityAttachment> rows = service.listEntityAttachments(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEntityAttachmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void createEntityAttachment(CreateEntityAttachmentRequest request,
            StreamObserver<CreateEntityAttachmentResponse> out) {
        EntityAttachment created = service.createEntityAttachment(
                bridge.body(request.getEntityAttachment(), EntityAttachment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEntityAttachmentResponse.newBuilder(), "entity_attachment",
                created).build());
    }

    @Override
    public void deleteEntityAttachment(DeleteEntityAttachmentRequest request,
            StreamObserver<DeleteEntityAttachmentResponse> out) {
        service.deleteEntityAttachment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEntityAttachmentResponse.getDefaultInstance());
    }
}
