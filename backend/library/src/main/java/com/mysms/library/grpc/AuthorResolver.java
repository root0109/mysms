package com.mysms.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.library.api.model.Author;
import com.mysms.library.api.model.AuthorUpdate;
import com.mysms.library.business.AuthorService;
import com.mysms.library.v1.AuthorServiceGrpc;
import com.mysms.library.v1.CreateAuthorRequest;
import com.mysms.library.v1.CreateAuthorResponse;
import com.mysms.library.v1.DeleteAuthorRequest;
import com.mysms.library.v1.DeleteAuthorResponse;
import com.mysms.library.v1.GetAuthorRequest;
import com.mysms.library.v1.GetAuthorResponse;
import com.mysms.library.v1.ListAuthorsRequest;
import com.mysms.library.v1.ListAuthorsResponse;
import com.mysms.library.v1.UpdateAuthorRequest;
import com.mysms.library.v1.UpdateAuthorResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.AuthorService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AuthorResolver extends AuthorServiceGrpc.AuthorServiceImplBase {

    private final AuthorService service;
    private final ProtoBridge bridge;

    public AuthorResolver(AuthorService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAuthors(ListAuthorsRequest request, StreamObserver<ListAuthorsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Author> rows = service.listAuthors(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAuthorsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAuthor(GetAuthorRequest request, StreamObserver<GetAuthorResponse> out) {
        Author found = service.getAuthor(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAuthorResponse.newBuilder(), "author", found).build());
    }

    @Override
    public void createAuthor(CreateAuthorRequest request, StreamObserver<CreateAuthorResponse> out) {
        Author created = service.createAuthor(
                bridge.body(request.getAuthor(), Author.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAuthorResponse.newBuilder(), "author", created).build());
    }

    @Override
    public void updateAuthor(UpdateAuthorRequest request, StreamObserver<UpdateAuthorResponse> out) {
        Author updated = service.updateAuthor(
                bridge.id(request.getAuthor().getId(), "id"),
                bridge.requiredIfMatch(request.getAuthor().getEtag()),
                bridge.changes(request.getAuthor(), request.getUpdateMask(), AuthorUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAuthorResponse.newBuilder(), "author", updated).build());
    }

    @Override
    public void deleteAuthor(DeleteAuthorRequest request, StreamObserver<DeleteAuthorResponse> out) {
        service.deleteAuthor(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAuthorResponse.getDefaultInstance());
    }
}
