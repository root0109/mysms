package com.mysms.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.org.api.model.AttributeDefinition;
import com.mysms.org.api.model.AttributeDefinitionUpdate;
import com.mysms.org.business.AttributeDefinitionService;
import com.mysms.org.v1.AttributeDefinitionServiceGrpc;
import com.mysms.org.v1.CreateAttributeDefinitionRequest;
import com.mysms.org.v1.CreateAttributeDefinitionResponse;
import com.mysms.org.v1.DeleteAttributeDefinitionRequest;
import com.mysms.org.v1.DeleteAttributeDefinitionResponse;
import com.mysms.org.v1.GetAttributeDefinitionRequest;
import com.mysms.org.v1.GetAttributeDefinitionResponse;
import com.mysms.org.v1.ListAttributeDefinitionsRequest;
import com.mysms.org.v1.ListAttributeDefinitionsResponse;
import com.mysms.org.v1.UpdateAttributeDefinitionRequest;
import com.mysms.org.v1.UpdateAttributeDefinitionResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of org.AttributeDefinitionService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AttributeDefinitionResolver extends AttributeDefinitionServiceGrpc.AttributeDefinitionServiceImplBase {

    private final AttributeDefinitionService service;
    private final ProtoBridge bridge;

    public AttributeDefinitionResolver(AttributeDefinitionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listAttributeDefinitions(ListAttributeDefinitionsRequest request,
            StreamObserver<ListAttributeDefinitionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AttributeDefinition> rows = service.listAttributeDefinitions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAttributeDefinitionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAttributeDefinition(GetAttributeDefinitionRequest request,
            StreamObserver<GetAttributeDefinitionResponse> out) {
        AttributeDefinition found = service.getAttributeDefinition(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAttributeDefinitionResponse.newBuilder(), "attribute_definition",
                found).build());
    }

    @Override
    public void createAttributeDefinition(CreateAttributeDefinitionRequest request,
            StreamObserver<CreateAttributeDefinitionResponse> out) {
        AttributeDefinition created = service.createAttributeDefinition(
                bridge.body(request.getAttributeDefinition(), AttributeDefinition.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAttributeDefinitionResponse.newBuilder(), "attribute_definition",
                created).build());
    }

    @Override
    public void updateAttributeDefinition(UpdateAttributeDefinitionRequest request,
            StreamObserver<UpdateAttributeDefinitionResponse> out) {
        AttributeDefinition updated = service.updateAttributeDefinition(
                bridge.id(request.getAttributeDefinition().getId(), "id"),
                bridge.requiredIfMatch(request.getAttributeDefinition().getEtag()),
                bridge.changes(request.getAttributeDefinition(), request.getUpdateMask(),
                        AttributeDefinitionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAttributeDefinitionResponse.newBuilder(), "attribute_definition",
                updated).build());
    }

    @Override
    public void deleteAttributeDefinition(DeleteAttributeDefinitionRequest request,
            StreamObserver<DeleteAttributeDefinitionResponse> out) {
        service.deleteAttributeDefinition(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAttributeDefinitionResponse.getDefaultInstance());
    }
}
