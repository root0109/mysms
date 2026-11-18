package com.mysms.lms.grpc;

import com.mysms.core.paging.Page;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.lms.api.model.Category;
import com.mysms.lms.api.model.CategoryUpdate;
import com.mysms.lms.business.CategoryService;
import com.mysms.lms.v1.CategoryServiceGrpc;
import com.mysms.lms.v1.CreateCategoryRequest;
import com.mysms.lms.v1.CreateCategoryResponse;
import com.mysms.lms.v1.DeleteCategoryRequest;
import com.mysms.lms.v1.DeleteCategoryResponse;
import com.mysms.lms.v1.GetCategoryRequest;
import com.mysms.lms.v1.GetCategoryResponse;
import com.mysms.lms.v1.ListCategoriesRequest;
import com.mysms.lms.v1.ListCategoriesResponse;
import com.mysms.lms.v1.UpdateCategoryRequest;
import com.mysms.lms.v1.UpdateCategoryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.CategoryService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class CategoryResolver extends CategoryServiceGrpc.CategoryServiceImplBase {

    private final CategoryService service;
    private final ProtoBridge bridge;

    public CategoryResolver(CategoryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCategories(ListCategoriesRequest request, StreamObserver<ListCategoriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Category> rows = service.listCategories(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCategoriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCategory(GetCategoryRequest request, StreamObserver<GetCategoryResponse> out) {
        Category found = service.getCategory(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCategoryResponse.newBuilder(), "category", found).build());
    }

    @Override
    public void createCategory(CreateCategoryRequest request, StreamObserver<CreateCategoryResponse> out) {
        Category created = service.createCategory(bridge.body(request.getCategory(), Category.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCategoryResponse.newBuilder(), "category", created).build());
    }

    @Override
    public void updateCategory(UpdateCategoryRequest request, StreamObserver<UpdateCategoryResponse> out) {
        Category updated = service.updateCategory(bridge.id(request.getCategory().getId(), "id"),
                bridge.requiredIfMatch(request.getCategory().getEtag()),
                bridge.changes(request.getCategory(), request.getUpdateMask(), CategoryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCategoryResponse.newBuilder(), "category", updated).build());
    }

    @Override
    public void deleteCategory(DeleteCategoryRequest request, StreamObserver<DeleteCategoryResponse> out) {
        service.deleteCategory(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCategoryResponse.getDefaultInstance());
    }
}
