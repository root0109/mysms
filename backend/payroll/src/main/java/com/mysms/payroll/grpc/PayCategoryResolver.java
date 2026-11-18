package com.mysms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.payroll.api.model.PayCategory;
import com.mysms.payroll.api.model.PayCategoryUpdate;
import com.mysms.payroll.business.PayCategoryService;
import com.mysms.payroll.v1.CreatePayCategoryRequest;
import com.mysms.payroll.v1.CreatePayCategoryResponse;
import com.mysms.payroll.v1.DeletePayCategoryRequest;
import com.mysms.payroll.v1.DeletePayCategoryResponse;
import com.mysms.payroll.v1.GetPayCategoryRequest;
import com.mysms.payroll.v1.GetPayCategoryResponse;
import com.mysms.payroll.v1.ListPayCategoriesRequest;
import com.mysms.payroll.v1.ListPayCategoriesResponse;
import com.mysms.payroll.v1.PayCategoryServiceGrpc;
import com.mysms.payroll.v1.UpdatePayCategoryRequest;
import com.mysms.payroll.v1.UpdatePayCategoryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of payroll.PayCategoryService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PayCategoryResolver extends PayCategoryServiceGrpc.PayCategoryServiceImplBase {

    private final PayCategoryService payCategoryService;
    private final ProtoBridge bridge;

    public PayCategoryResolver(PayCategoryService payCategoryService, ProtoBridge bridge) {
        this.payCategoryService = payCategoryService;
        this.bridge = bridge;
    }

    @Override
    public void listPayCategories(ListPayCategoriesRequest request, StreamObserver<ListPayCategoriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PayCategory> rows = payCategoryService.listPayCategorys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPayCategoriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPayCategory(GetPayCategoryRequest request, StreamObserver<GetPayCategoryResponse> out) {
        PayCategory found = payCategoryService.getPayCategory(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPayCategoryResponse.newBuilder(), "pay_category", found).build());
    }

    @Override
    public void createPayCategory(CreatePayCategoryRequest request, StreamObserver<CreatePayCategoryResponse> out) {
        PayCategory created = payCategoryService.createPayCategory(
                bridge.body(request.getPayCategory(), PayCategory.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePayCategoryResponse.newBuilder(), "pay_category", created).build());
    }

    @Override
    public void updatePayCategory(UpdatePayCategoryRequest request, StreamObserver<UpdatePayCategoryResponse> out) {
        PayCategory updated = payCategoryService.updatePayCategory(
                bridge.id(request.getPayCategory().getId(), "id"),
                bridge.requiredIfMatch(request.getPayCategory().getEtag()),
                bridge.changes(request.getPayCategory(), request.getUpdateMask(), PayCategoryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePayCategoryResponse.newBuilder(), "pay_category", updated).build());
    }

    @Override
    public void deletePayCategory(DeletePayCategoryRequest request, StreamObserver<DeletePayCategoryResponse> out) {
        payCategoryService.deletePayCategory(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePayCategoryResponse.getDefaultInstance());
    }
}
