package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ForumReportCategory;
import com.mysms.learning.lms.business.ForumReportCategoryService;
import com.mysms.lms.v1.CreateForumReportCategoryRequest;
import com.mysms.lms.v1.CreateForumReportCategoryResponse;
import com.mysms.lms.v1.DeleteForumReportCategoryRequest;
import com.mysms.lms.v1.DeleteForumReportCategoryResponse;
import com.mysms.lms.v1.ForumReportCategoryServiceGrpc;
import com.mysms.lms.v1.ForumReportCategoryUpdate;
import com.mysms.lms.v1.GetForumReportCategoryRequest;
import com.mysms.lms.v1.GetForumReportCategoryResponse;
import com.mysms.lms.v1.ListForumReportCategoriesRequest;
import com.mysms.lms.v1.ListForumReportCategoriesResponse;
import com.mysms.lms.v1.UpdateForumReportCategoryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ForumReportCategoryService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ForumReportCategoryResolver extends ForumReportCategoryServiceGrpc.ForumReportCategoryServiceImplBase {

    private final ForumReportCategoryService service;
    private final ProtoBridge bridge;

    public ForumReportCategoryResolver(ForumReportCategoryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listForumReportCategories(ListForumReportCategoriesRequest request,
            StreamObserver<ListForumReportCategoriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ForumReportCategory> rows = service.listForumReportCategorys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListForumReportCategoriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getForumReportCategory(GetForumReportCategoryRequest request,
            StreamObserver<GetForumReportCategoryResponse> out) {
        ForumReportCategory found = service.getForumReportCategory(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetForumReportCategoryResponse.newBuilder(), "forum_report_category", found).build());
    }

    @Override
    public void createForumReportCategory(CreateForumReportCategoryRequest request,
            StreamObserver<CreateForumReportCategoryResponse> out) {
        ForumReportCategory created = service.createForumReportCategory(
                bridge.body(request.getForumReportCategory(), ForumReportCategory.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateForumReportCategoryResponse.newBuilder(), "forum_report_category", created).build());
    }

    @Override
    public void updateForumReportCategory(ForumReportCategoryUpdate request,
            StreamObserver<UpdateForumReportCategoryResponse> out) {
        ForumReportCategory updated = service.updateForumReportCategory(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ForumReportCategoryProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateForumReportCategoryResponse.newBuilder(), "forum_report_category", updated).build());
    }

    @Override
    public void deleteForumReportCategory(DeleteForumReportCategoryRequest request,
            StreamObserver<DeleteForumReportCategoryResponse> out) {
        service.deleteForumReportCategory(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteForumReportCategoryResponse.getDefaultInstance());
    }
}
