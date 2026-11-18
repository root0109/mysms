package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.ResourceCategory;
import com.mysms.lms.api.model.ResourceCategoryUpdate;
import com.mysms.lms.business.ResourceCategoryService;
import com.mysms.lms.v1.CreateResourceCategoryRequest;
import com.mysms.lms.v1.CreateResourceCategoryResponse;
import com.mysms.lms.v1.DeleteResourceCategoryRequest;
import com.mysms.lms.v1.DeleteResourceCategoryResponse;
import com.mysms.lms.v1.GetResourceCategoryRequest;
import com.mysms.lms.v1.GetResourceCategoryResponse;
import com.mysms.lms.v1.ListResourceCategoriesRequest;
import com.mysms.lms.v1.ListResourceCategoriesResponse;
import com.mysms.lms.v1.ResourceCategoryServiceGrpc;
import com.mysms.lms.v1.UpdateResourceCategoryRequest;
import com.mysms.lms.v1.UpdateResourceCategoryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ResourceCategoryService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ResourceCategoryResolver extends ResourceCategoryServiceGrpc.ResourceCategoryServiceImplBase {

    private final ResourceCategoryService service;
    private final ProtoBridge bridge;

    public ResourceCategoryResolver(ResourceCategoryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listResourceCategories(ListResourceCategoriesRequest request,
            StreamObserver<ListResourceCategoriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ResourceCategory> rows = service.listResourceCategorys(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListResourceCategoriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getResourceCategory(GetResourceCategoryRequest request, StreamObserver<GetResourceCategoryResponse> out) {
        ResourceCategory found = service.getResourceCategory(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetResourceCategoryResponse.newBuilder(), "resource_category", found).build());
    }

    @Override
    public void createResourceCategory(CreateResourceCategoryRequest request,
            StreamObserver<CreateResourceCategoryResponse> out) {
        ResourceCategory created = service.createResourceCategory(
                bridge.body(request.getResourceCategory(), ResourceCategory.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateResourceCategoryResponse.newBuilder(), "resource_category", created).build());
    }

    @Override
    public void updateResourceCategory(UpdateResourceCategoryRequest request,
            StreamObserver<UpdateResourceCategoryResponse> out) {
        ResourceCategory updated = service.updateResourceCategory(bridge.id(request.getResourceCategory().getId(), "id"),
                bridge.requiredIfMatch(request.getResourceCategory().getEtag()),
                bridge.changes(request.getResourceCategory(), request.getUpdateMask(), ResourceCategoryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateResourceCategoryResponse.newBuilder(), "resource_category", updated).build());
    }

    @Override
    public void deleteResourceCategory(DeleteResourceCategoryRequest request,
            StreamObserver<DeleteResourceCategoryResponse> out) {
        service.deleteResourceCategory(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteResourceCategoryResponse.getDefaultInstance());
    }
}
