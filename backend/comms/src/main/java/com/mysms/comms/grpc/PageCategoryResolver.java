package com.mysms.comms.grpc;

import com.mysms.comms.api.model.PageCategory;
import com.mysms.comms.api.model.PageCategoryUpdate;
import com.mysms.comms.business.PageCategoryService;
import com.mysms.comms.v1.CreatePageCategoryRequest;
import com.mysms.comms.v1.CreatePageCategoryResponse;
import com.mysms.comms.v1.DeletePageCategoryRequest;
import com.mysms.comms.v1.DeletePageCategoryResponse;
import com.mysms.comms.v1.GetPageCategoryRequest;
import com.mysms.comms.v1.GetPageCategoryResponse;
import com.mysms.comms.v1.ListPageCategoriesRequest;
import com.mysms.comms.v1.ListPageCategoriesResponse;
import com.mysms.comms.v1.PageCategoryServiceGrpc;
import com.mysms.comms.v1.UpdatePageCategoryRequest;
import com.mysms.comms.v1.UpdatePageCategoryResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.PageCategoryService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PageCategoryResolver extends PageCategoryServiceGrpc.PageCategoryServiceImplBase {

    private final PageCategoryService service;
    private final ProtoBridge bridge;

    public PageCategoryResolver(PageCategoryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPageCategories(ListPageCategoriesRequest request, StreamObserver<ListPageCategoriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PageCategory> rows = service.listPageCategorys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPageCategoriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPageCategory(GetPageCategoryRequest request, StreamObserver<GetPageCategoryResponse> out) {
        PageCategory found = service.getPageCategory(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPageCategoryResponse.newBuilder(), "page_category", found).build());
    }

    @Override
    public void createPageCategory(CreatePageCategoryRequest request, StreamObserver<CreatePageCategoryResponse> out) {
        PageCategory created = service.createPageCategory(
                bridge.body(request.getPageCategory(), PageCategory.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePageCategoryResponse.newBuilder(), "page_category", created).build());
    }

    @Override
    public void updatePageCategory(UpdatePageCategoryRequest request, StreamObserver<UpdatePageCategoryResponse> out) {
        PageCategory updated = service.updatePageCategory(
                bridge.id(request.getPageCategory().getId(), "id"), bridge.requiredIfMatch(request.getPageCategory().getEtag()),
                bridge.changes(request.getPageCategory(), request.getUpdateMask(), PageCategoryUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePageCategoryResponse.newBuilder(), "page_category", updated).build());
    }

    @Override
    public void deletePageCategory(DeletePageCategoryRequest request, StreamObserver<DeletePageCategoryResponse> out) {
        service.deletePageCategory(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePageCategoryResponse.getDefaultInstance());
    }
}
