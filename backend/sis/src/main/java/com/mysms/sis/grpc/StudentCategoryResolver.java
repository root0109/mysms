package com.mysms.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.sis.api.model.StudentCategory;
import com.mysms.sis.api.model.StudentCategoryUpdate;
import com.mysms.sis.business.StudentCategoryService;
import com.mysms.sis.v1.CreateStudentCategoryRequest;
import com.mysms.sis.v1.CreateStudentCategoryResponse;
import com.mysms.sis.v1.DeleteStudentCategoryRequest;
import com.mysms.sis.v1.DeleteStudentCategoryResponse;
import com.mysms.sis.v1.GetStudentCategoryRequest;
import com.mysms.sis.v1.GetStudentCategoryResponse;
import com.mysms.sis.v1.ListStudentCategoriesRequest;
import com.mysms.sis.v1.ListStudentCategoriesResponse;
import com.mysms.sis.v1.StudentCategoryServiceGrpc;
import com.mysms.sis.v1.UpdateStudentCategoryRequest;
import com.mysms.sis.v1.UpdateStudentCategoryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.StudentCategoryService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StudentCategoryResolver extends StudentCategoryServiceGrpc.StudentCategoryServiceImplBase {

    private final StudentCategoryService service;
    private final ProtoBridge bridge;

    public StudentCategoryResolver(StudentCategoryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStudentCategories(ListStudentCategoriesRequest request, StreamObserver<ListStudentCategoriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentCategory> rows = service.listStudentCategorys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentCategoriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentCategory(GetStudentCategoryRequest request, StreamObserver<GetStudentCategoryResponse> out) {
        StudentCategory found = service.getStudentCategory(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentCategoryResponse.newBuilder(), "student_category", found).build());
    }

    @Override
    public void createStudentCategory(CreateStudentCategoryRequest request, StreamObserver<CreateStudentCategoryResponse> out) {
        StudentCategory created = service.createStudentCategory(
                bridge.body(request.getStudentCategory(), StudentCategory.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStudentCategoryResponse.newBuilder(), "student_category", created).build());
    }

    @Override
    public void updateStudentCategory(UpdateStudentCategoryRequest request, StreamObserver<UpdateStudentCategoryResponse> out) {
        StudentCategory updated = service.updateStudentCategory(
                bridge.id(request.getStudentCategory().getId(), "id"),
                bridge.requiredIfMatch(request.getStudentCategory().getEtag()),
                bridge.changes(request.getStudentCategory(), request.getUpdateMask(), StudentCategoryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStudentCategoryResponse.newBuilder(), "student_category", updated).build());
    }

    @Override
    public void deleteStudentCategory(DeleteStudentCategoryRequest request, StreamObserver<DeleteStudentCategoryResponse> out) {
        service.deleteStudentCategory(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentCategoryResponse.getDefaultInstance());
    }
}
