package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.GradingCategory;
import com.mysms.lms.api.model.GradingCategoryUpdate;
import com.mysms.lms.business.GradingCategoryService;
import com.mysms.lms.v1.CreateGradingCategoryRequest;
import com.mysms.lms.v1.CreateGradingCategoryResponse;
import com.mysms.lms.v1.DeleteGradingCategoryRequest;
import com.mysms.lms.v1.DeleteGradingCategoryResponse;
import com.mysms.lms.v1.GetGradingCategoryRequest;
import com.mysms.lms.v1.GetGradingCategoryResponse;
import com.mysms.lms.v1.GradingCategoryServiceGrpc;
import com.mysms.lms.v1.ListGradingCategoriesRequest;
import com.mysms.lms.v1.ListGradingCategoriesResponse;
import com.mysms.lms.v1.UpdateGradingCategoryRequest;
import com.mysms.lms.v1.UpdateGradingCategoryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.GradingCategoryService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class GradingCategoryResolver extends GradingCategoryServiceGrpc.GradingCategoryServiceImplBase {

    private final GradingCategoryService service;
    private final ProtoBridge bridge;

    public GradingCategoryResolver(GradingCategoryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listGradingCategories(ListGradingCategoriesRequest request, StreamObserver<ListGradingCategoriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<GradingCategory> rows = service.listGradingCategorys(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListGradingCategoriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getGradingCategory(GetGradingCategoryRequest request, StreamObserver<GetGradingCategoryResponse> out) {
        GradingCategory found = service.getGradingCategory(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetGradingCategoryResponse.newBuilder(), "grading_category", found).build());
    }

    @Override
    public void createGradingCategory(CreateGradingCategoryRequest request, StreamObserver<CreateGradingCategoryResponse> out) {
        GradingCategory created = service.createGradingCategory(
                bridge.body(request.getGradingCategory(), GradingCategory.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateGradingCategoryResponse.newBuilder(), "grading_category", created).build());
    }

    @Override
    public void updateGradingCategory(UpdateGradingCategoryRequest request, StreamObserver<UpdateGradingCategoryResponse> out) {
        GradingCategory updated = service.updateGradingCategory(bridge.id(request.getGradingCategory().getId(), "id"),
                bridge.requiredIfMatch(request.getGradingCategory().getEtag()),
                bridge.changes(request.getGradingCategory(), request.getUpdateMask(), GradingCategoryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateGradingCategoryResponse.newBuilder(), "grading_category", updated).build());
    }

    @Override
    public void deleteGradingCategory(DeleteGradingCategoryRequest request, StreamObserver<DeleteGradingCategoryResponse> out) {
        service.deleteGradingCategory(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteGradingCategoryResponse.getDefaultInstance());
    }
}
