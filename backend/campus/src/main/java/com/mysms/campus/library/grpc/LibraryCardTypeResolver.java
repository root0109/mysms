package com.mysms.campus.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.library.api.model.LibraryCardType;
import com.mysms.campus.library.business.LibraryCardTypeService;
import com.mysms.library.v1.CreateLibraryCardTypeRequest;
import com.mysms.library.v1.CreateLibraryCardTypeResponse;
import com.mysms.library.v1.DeleteLibraryCardTypeRequest;
import com.mysms.library.v1.DeleteLibraryCardTypeResponse;
import com.mysms.library.v1.GetLibraryCardTypeRequest;
import com.mysms.library.v1.GetLibraryCardTypeResponse;
import com.mysms.library.v1.LibraryCardTypeServiceGrpc;
import com.mysms.library.v1.LibraryCardTypeUpdate;
import com.mysms.library.v1.ListLibraryCardTypesRequest;
import com.mysms.library.v1.ListLibraryCardTypesResponse;
import com.mysms.library.v1.UpdateLibraryCardTypeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.LibraryCardTypeService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class LibraryCardTypeResolver extends LibraryCardTypeServiceGrpc.LibraryCardTypeServiceImplBase {

    private final LibraryCardTypeService service;
    private final ProtoBridge bridge;

    public LibraryCardTypeResolver(LibraryCardTypeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLibraryCardTypes(ListLibraryCardTypesRequest request, StreamObserver<ListLibraryCardTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LibraryCardType> rows = service.listLibraryCardTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLibraryCardTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLibraryCardType(GetLibraryCardTypeRequest request, StreamObserver<GetLibraryCardTypeResponse> out) {
        LibraryCardType found = service.getLibraryCardType(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLibraryCardTypeResponse.newBuilder(), "library_card_type", found).build());
    }

    @Override
    public void createLibraryCardType(CreateLibraryCardTypeRequest request, StreamObserver<CreateLibraryCardTypeResponse> out) {
        LibraryCardType created = service.createLibraryCardType(
                bridge.body(request.getLibraryCardType(), LibraryCardType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLibraryCardTypeResponse.newBuilder(), "library_card_type", created).build());
    }

    @Override
    public void updateLibraryCardType(LibraryCardTypeUpdate request, StreamObserver<UpdateLibraryCardTypeResponse> out) {
        LibraryCardType updated = service.updateLibraryCardType(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                LibraryCardTypeProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLibraryCardTypeResponse.newBuilder(), "library_card_type", updated).build());
    }

    @Override
    public void deleteLibraryCardType(DeleteLibraryCardTypeRequest request, StreamObserver<DeleteLibraryCardTypeResponse> out) {
        service.deleteLibraryCardType(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLibraryCardTypeResponse.getDefaultInstance());
    }
}
