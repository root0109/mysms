package com.mysms.campus.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.library.api.model.Publisher;
import com.mysms.campus.library.api.model.PublisherUpdate;
import com.mysms.campus.library.business.PublisherService;
import com.mysms.library.v1.CreatePublisherRequest;
import com.mysms.library.v1.CreatePublisherResponse;
import com.mysms.library.v1.DeletePublisherRequest;
import com.mysms.library.v1.DeletePublisherResponse;
import com.mysms.library.v1.GetPublisherRequest;
import com.mysms.library.v1.GetPublisherResponse;
import com.mysms.library.v1.ListPublishersRequest;
import com.mysms.library.v1.ListPublishersResponse;
import com.mysms.library.v1.PublisherServiceGrpc;
import com.mysms.library.v1.UpdatePublisherRequest;
import com.mysms.library.v1.UpdatePublisherResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.PublisherService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PublisherResolver extends PublisherServiceGrpc.PublisherServiceImplBase {

    private final PublisherService service;
    private final ProtoBridge bridge;

    public PublisherResolver(PublisherService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPublishers(ListPublishersRequest request, StreamObserver<ListPublishersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Publisher> rows = service.listPublishers(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPublishersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPublisher(GetPublisherRequest request, StreamObserver<GetPublisherResponse> out) {
        Publisher found = service.getPublisher(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPublisherResponse.newBuilder(), "publisher", found).build());
    }

    @Override
    public void createPublisher(CreatePublisherRequest request, StreamObserver<CreatePublisherResponse> out) {
        Publisher created = service.createPublisher(
                bridge.body(request.getPublisher(), Publisher.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePublisherResponse.newBuilder(), "publisher", created).build());
    }

    @Override
    public void updatePublisher(UpdatePublisherRequest request, StreamObserver<UpdatePublisherResponse> out) {
        Publisher updated = service.updatePublisher(
                bridge.id(request.getPublisher().getId(), "id"),
                bridge.requiredIfMatch(request.getPublisher().getEtag()),
                bridge.changes(request.getPublisher(), request.getUpdateMask(), PublisherUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePublisherResponse.newBuilder(), "publisher", updated).build());
    }

    @Override
    public void deletePublisher(DeletePublisherRequest request, StreamObserver<DeletePublisherResponse> out) {
        service.deletePublisher(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePublisherResponse.getDefaultInstance());
    }
}
