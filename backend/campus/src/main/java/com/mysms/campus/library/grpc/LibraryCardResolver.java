package com.mysms.campus.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.library.api.model.LibraryCard;
import com.mysms.campus.library.api.model.LibraryCardUpdate;
import com.mysms.campus.library.business.LibraryCardService;
import com.mysms.library.v1.CreateLibraryCardRequest;
import com.mysms.library.v1.CreateLibraryCardResponse;
import com.mysms.library.v1.DeleteLibraryCardRequest;
import com.mysms.library.v1.DeleteLibraryCardResponse;
import com.mysms.library.v1.GetLibraryCardRequest;
import com.mysms.library.v1.GetLibraryCardResponse;
import com.mysms.library.v1.LibraryCardServiceGrpc;
import com.mysms.library.v1.ListLibraryCardsRequest;
import com.mysms.library.v1.ListLibraryCardsResponse;
import com.mysms.library.v1.UpdateLibraryCardRequest;
import com.mysms.library.v1.UpdateLibraryCardResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.LibraryCardService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class LibraryCardResolver extends LibraryCardServiceGrpc.LibraryCardServiceImplBase {

    private final LibraryCardService service;
    private final ProtoBridge bridge;

    public LibraryCardResolver(LibraryCardService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLibraryCards(ListLibraryCardsRequest request, StreamObserver<ListLibraryCardsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LibraryCard> rows = service.listLibraryCards(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLibraryCardsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLibraryCard(GetLibraryCardRequest request, StreamObserver<GetLibraryCardResponse> out) {
        LibraryCard found = service.getLibraryCard(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLibraryCardResponse.newBuilder(), "library_card", found).build());
    }

    @Override
    public void createLibraryCard(CreateLibraryCardRequest request, StreamObserver<CreateLibraryCardResponse> out) {
        LibraryCard created = service.createLibraryCard(
                bridge.body(request.getLibraryCard(), LibraryCard.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLibraryCardResponse.newBuilder(), "library_card", created).build());
    }

    @Override
    public void updateLibraryCard(UpdateLibraryCardRequest request, StreamObserver<UpdateLibraryCardResponse> out) {
        LibraryCard updated = service.updateLibraryCard(
                bridge.id(request.getLibraryCard().getId(), "id"),
                bridge.requiredIfMatch(request.getLibraryCard().getEtag()),
                bridge.changes(request.getLibraryCard(), request.getUpdateMask(), LibraryCardUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLibraryCardResponse.newBuilder(), "library_card", updated).build());
    }

    @Override
    public void deleteLibraryCard(DeleteLibraryCardRequest request, StreamObserver<DeleteLibraryCardResponse> out) {
        service.deleteLibraryCard(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLibraryCardResponse.getDefaultInstance());
    }
}
