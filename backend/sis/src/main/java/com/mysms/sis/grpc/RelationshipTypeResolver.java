package com.mysms.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.sis.api.model.RelationshipType;
import com.mysms.sis.api.model.RelationshipTypeUpdate;
import com.mysms.sis.business.RelationshipTypeService;
import com.mysms.sis.v1.CreateRelationshipTypeRequest;
import com.mysms.sis.v1.CreateRelationshipTypeResponse;
import com.mysms.sis.v1.DeleteRelationshipTypeRequest;
import com.mysms.sis.v1.DeleteRelationshipTypeResponse;
import com.mysms.sis.v1.GetRelationshipTypeRequest;
import com.mysms.sis.v1.GetRelationshipTypeResponse;
import com.mysms.sis.v1.ListRelationshipTypesRequest;
import com.mysms.sis.v1.ListRelationshipTypesResponse;
import com.mysms.sis.v1.RelationshipTypeServiceGrpc;
import com.mysms.sis.v1.UpdateRelationshipTypeRequest;
import com.mysms.sis.v1.UpdateRelationshipTypeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.RelationshipTypeService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class RelationshipTypeResolver extends RelationshipTypeServiceGrpc.RelationshipTypeServiceImplBase {

    private final RelationshipTypeService service;
    private final ProtoBridge bridge;

    public RelationshipTypeResolver(RelationshipTypeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listRelationshipTypes(ListRelationshipTypesRequest request, StreamObserver<ListRelationshipTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RelationshipType> rows = service.listRelationshipTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRelationshipTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRelationshipType(GetRelationshipTypeRequest request, StreamObserver<GetRelationshipTypeResponse> out) {
        RelationshipType found = service.getRelationshipType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRelationshipTypeResponse.newBuilder(), "relationship_type", found).build());
    }

    @Override
    public void createRelationshipType(CreateRelationshipTypeRequest request,
            StreamObserver<CreateRelationshipTypeResponse> out) {
        RelationshipType created = service.createRelationshipType(
                bridge.body(request.getRelationshipType(), RelationshipType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRelationshipTypeResponse.newBuilder(), "relationship_type", created).build());
    }

    @Override
    public void updateRelationshipType(UpdateRelationshipTypeRequest request,
            StreamObserver<UpdateRelationshipTypeResponse> out) {
        RelationshipType updated = service.updateRelationshipType(
                bridge.id(request.getRelationshipType().getId(), "id"),
                bridge.requiredIfMatch(request.getRelationshipType().getEtag()),
                bridge.changes(request.getRelationshipType(), request.getUpdateMask(), RelationshipTypeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRelationshipTypeResponse.newBuilder(), "relationship_type", updated).build());
    }

    @Override
    public void deleteRelationshipType(DeleteRelationshipTypeRequest request,
            StreamObserver<DeleteRelationshipTypeResponse> out) {
        service.deleteRelationshipType(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRelationshipTypeResponse.getDefaultInstance());
    }
}
