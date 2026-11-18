package com.mysms.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.library.api.model.MediaCategory;
import com.mysms.library.api.model.MediaCategoryUpdate;
import com.mysms.library.business.MediaCategoryService;
import com.mysms.library.v1.CreateMediaCategoryRequest;
import com.mysms.library.v1.CreateMediaCategoryResponse;
import com.mysms.library.v1.DeleteMediaCategoryRequest;
import com.mysms.library.v1.DeleteMediaCategoryResponse;
import com.mysms.library.v1.GetMediaCategoryRequest;
import com.mysms.library.v1.GetMediaCategoryResponse;
import com.mysms.library.v1.ListMediaCategoriesRequest;
import com.mysms.library.v1.ListMediaCategoriesResponse;
import com.mysms.library.v1.MediaCategoryServiceGrpc;
import com.mysms.library.v1.UpdateMediaCategoryRequest;
import com.mysms.library.v1.UpdateMediaCategoryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.MediaCategoryService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MediaCategoryResolver extends MediaCategoryServiceGrpc.MediaCategoryServiceImplBase {

    private final MediaCategoryService service;
    private final ProtoBridge bridge;

    public MediaCategoryResolver(MediaCategoryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listMediaCategories(ListMediaCategoriesRequest request, StreamObserver<ListMediaCategoriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MediaCategory> rows = service.listMediaCategorys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMediaCategoriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMediaCategory(GetMediaCategoryRequest request, StreamObserver<GetMediaCategoryResponse> out) {
        MediaCategory found = service.getMediaCategory(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMediaCategoryResponse.newBuilder(), "media_category", found).build());
    }

    @Override
    public void createMediaCategory(CreateMediaCategoryRequest request, StreamObserver<CreateMediaCategoryResponse> out) {
        MediaCategory created = service.createMediaCategory(
                bridge.body(request.getMediaCategory(), MediaCategory.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateMediaCategoryResponse.newBuilder(), "media_category", created).build());
    }

    @Override
    public void updateMediaCategory(UpdateMediaCategoryRequest request, StreamObserver<UpdateMediaCategoryResponse> out) {
        MediaCategory updated = service.updateMediaCategory(
                bridge.id(request.getMediaCategory().getId(), "id"),
                bridge.requiredIfMatch(request.getMediaCategory().getEtag()),
                bridge.changes(request.getMediaCategory(), request.getUpdateMask(), MediaCategoryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateMediaCategoryResponse.newBuilder(), "media_category", updated).build());
    }

    @Override
    public void deleteMediaCategory(DeleteMediaCategoryRequest request, StreamObserver<DeleteMediaCategoryResponse> out) {
        service.deleteMediaCategory(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteMediaCategoryResponse.getDefaultInstance());
    }
}
