package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.EntityNote;
import com.mysms.platform.api.model.EntityNoteUpdate;
import com.mysms.platform.business.EntityNoteService;
import com.mysms.platform.v1.CreateEntityNoteRequest;
import com.mysms.platform.v1.CreateEntityNoteResponse;
import com.mysms.platform.v1.DeleteEntityNoteRequest;
import com.mysms.platform.v1.DeleteEntityNoteResponse;
import com.mysms.platform.v1.EntityNoteServiceGrpc;
import com.mysms.platform.v1.GetEntityNoteRequest;
import com.mysms.platform.v1.GetEntityNoteResponse;
import com.mysms.platform.v1.ListEntityNotesRequest;
import com.mysms.platform.v1.ListEntityNotesResponse;
import com.mysms.platform.v1.UpdateEntityNoteRequest;
import com.mysms.platform.v1.UpdateEntityNoteResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.EntityNoteService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class EntityNoteResolver extends EntityNoteServiceGrpc.EntityNoteServiceImplBase {

    private final EntityNoteService service;
    private final ProtoBridge bridge;

    public EntityNoteResolver(EntityNoteService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listEntityNotes(ListEntityNotesRequest request, StreamObserver<ListEntityNotesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EntityNote> rows = service.listEntityNotes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEntityNotesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEntityNote(GetEntityNoteRequest request, StreamObserver<GetEntityNoteResponse> out) {
        EntityNote found = service.getEntityNote(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEntityNoteResponse.newBuilder(), "entity_note", found).build());
    }

    @Override
    public void createEntityNote(CreateEntityNoteRequest request, StreamObserver<CreateEntityNoteResponse> out) {
        EntityNote created = service.createEntityNote(
                bridge.body(request.getEntityNote(), EntityNote.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEntityNoteResponse.newBuilder(), "entity_note", created).build());
    }

    @Override
    public void updateEntityNote(UpdateEntityNoteRequest request, StreamObserver<UpdateEntityNoteResponse> out) {
        EntityNote updated = service.updateEntityNote(
                bridge.id(request.getEntityNote().getId(), "id"),
                bridge.requiredIfMatch(request.getEntityNote().getEtag()),
                bridge.changes(request.getEntityNote(), request.getUpdateMask(), EntityNoteUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEntityNoteResponse.newBuilder(), "entity_note", updated).build());
    }

    @Override
    public void deleteEntityNote(DeleteEntityNoteRequest request, StreamObserver<DeleteEntityNoteResponse> out) {
        service.deleteEntityNote(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEntityNoteResponse.getDefaultInstance());
    }
}
