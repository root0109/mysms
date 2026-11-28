package com.mysms.campus.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.library.api.model.MediaType;
import com.mysms.campus.library.business.MediaTypeService;
import com.mysms.library.v1.CreateMediaTypeRequest;
import com.mysms.library.v1.CreateMediaTypeResponse;
import com.mysms.library.v1.DeleteMediaTypeRequest;
import com.mysms.library.v1.DeleteMediaTypeResponse;
import com.mysms.library.v1.GetMediaTypeRequest;
import com.mysms.library.v1.GetMediaTypeResponse;
import com.mysms.library.v1.ListMediaTypesRequest;
import com.mysms.library.v1.ListMediaTypesResponse;
import com.mysms.library.v1.MediaTypeServiceGrpc;
import com.mysms.library.v1.MediaTypeUpdate;
import com.mysms.library.v1.UpdateMediaTypeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.MediaTypeService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MediaTypeResolver extends MediaTypeServiceGrpc.MediaTypeServiceImplBase {

    private final MediaTypeService service;
    private final ProtoBridge bridge;

    public MediaTypeResolver(MediaTypeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listMediaTypes(ListMediaTypesRequest request, StreamObserver<ListMediaTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MediaType> rows = service.listMediaTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMediaTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMediaType(GetMediaTypeRequest request, StreamObserver<GetMediaTypeResponse> out) {
        MediaType found = service.getMediaType(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMediaTypeResponse.newBuilder(), "media_type", found).build());
    }

    @Override
    public void createMediaType(CreateMediaTypeRequest request, StreamObserver<CreateMediaTypeResponse> out) {
        MediaType created = service.createMediaType(
                bridge.body(request.getMediaType(), MediaType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMediaTypeResponse.newBuilder(), "media_type", created).build());
    }

    @Override
    public void updateMediaType(MediaTypeUpdate request, StreamObserver<UpdateMediaTypeResponse> out) {
        MediaType updated = service.updateMediaType(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                MediaTypeProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMediaTypeResponse.newBuilder(), "media_type", updated).build());
    }

    @Override
    public void deleteMediaType(DeleteMediaTypeRequest request, StreamObserver<DeleteMediaTypeResponse> out) {
        service.deleteMediaType(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMediaTypeResponse.getDefaultInstance());
    }
}
