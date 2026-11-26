package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ContentAuthor;
import com.mysms.learning.lms.api.model.ContentAuthorUpdate;
import com.mysms.learning.lms.business.ContentAuthorService;
import com.mysms.lms.v1.ContentAuthorServiceGrpc;
import com.mysms.lms.v1.CreateContentAuthorRequest;
import com.mysms.lms.v1.CreateContentAuthorResponse;
import com.mysms.lms.v1.DeleteContentAuthorRequest;
import com.mysms.lms.v1.DeleteContentAuthorResponse;
import com.mysms.lms.v1.GetContentAuthorRequest;
import com.mysms.lms.v1.GetContentAuthorResponse;
import com.mysms.lms.v1.ListContentAuthorsRequest;
import com.mysms.lms.v1.ListContentAuthorsResponse;
import com.mysms.lms.v1.UpdateContentAuthorRequest;
import com.mysms.lms.v1.UpdateContentAuthorResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ContentAuthorService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class ContentAuthorResolver extends ContentAuthorServiceGrpc.ContentAuthorServiceImplBase {

    private final ContentAuthorService service;
    private final ProtoBridge bridge;

    public ContentAuthorResolver(ContentAuthorService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listContentAuthors(ListContentAuthorsRequest request, StreamObserver<ListContentAuthorsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ContentAuthor> rows = service.listContentAuthors(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListContentAuthorsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getContentAuthor(GetContentAuthorRequest request, StreamObserver<GetContentAuthorResponse> out) {
        ContentAuthor found = service.getContentAuthor(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetContentAuthorResponse.newBuilder(), "content_author", found).build());
    }

    @Override
    public void createContentAuthor(CreateContentAuthorRequest request, StreamObserver<CreateContentAuthorResponse> out) {
        ContentAuthor created = service.createContentAuthor(bridge.body(request.getContentAuthor(), ContentAuthor.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateContentAuthorResponse.newBuilder(), "content_author", created).build());
    }

    @Override
    public void updateContentAuthor(UpdateContentAuthorRequest request, StreamObserver<UpdateContentAuthorResponse> out) {
        ContentAuthor updated = service.updateContentAuthor(bridge.id(request.getContentAuthor().getId(), "id"),
                bridge.requiredIfMatch(request.getContentAuthor().getEtag()),
                bridge.changes(request.getContentAuthor(), request.getUpdateMask(), ContentAuthorUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateContentAuthorResponse.newBuilder(), "content_author", updated).build());
    }

    @Override
    public void deleteContentAuthor(DeleteContentAuthorRequest request, StreamObserver<DeleteContentAuthorResponse> out) {
        service.deleteContentAuthor(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteContentAuthorResponse.getDefaultInstance());
    }
}
