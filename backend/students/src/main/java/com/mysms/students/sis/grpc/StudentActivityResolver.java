package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.students.sis.api.model.StudentActivity;
import com.mysms.students.sis.api.model.StudentActivityUpdate;
import com.mysms.students.sis.business.StudentActivityService;
import com.mysms.sis.v1.CreateStudentActivityRequest;
import com.mysms.sis.v1.CreateStudentActivityResponse;
import com.mysms.sis.v1.DeleteStudentActivityRequest;
import com.mysms.sis.v1.DeleteStudentActivityResponse;
import com.mysms.sis.v1.GetStudentActivityRequest;
import com.mysms.sis.v1.GetStudentActivityResponse;
import com.mysms.sis.v1.ListStudentActivitiesRequest;
import com.mysms.sis.v1.ListStudentActivitiesResponse;
import com.mysms.sis.v1.StudentActivityServiceGrpc;
import com.mysms.sis.v1.UpdateStudentActivityRequest;
import com.mysms.sis.v1.UpdateStudentActivityResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.StudentActivityService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StudentActivityResolver extends StudentActivityServiceGrpc.StudentActivityServiceImplBase {

    private final StudentActivityService service;
    private final ProtoBridge bridge;

    public StudentActivityResolver(StudentActivityService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listStudentActivities(ListStudentActivitiesRequest request, StreamObserver<ListStudentActivitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentActivity> rows = service.listStudentActivitys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentActivitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentActivity(GetStudentActivityRequest request, StreamObserver<GetStudentActivityResponse> out) {
        StudentActivity found = service.getStudentActivity(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentActivityResponse.newBuilder(), "student_activity", found).build());
    }

    @Override
    public void createStudentActivity(CreateStudentActivityRequest request, StreamObserver<CreateStudentActivityResponse> out) {
        StudentActivity created = service.createStudentActivity(
                bridge.body(request.getStudentActivity(), StudentActivity.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStudentActivityResponse.newBuilder(), "student_activity", created).build());
    }

    @Override
    public void updateStudentActivity(UpdateStudentActivityRequest request, StreamObserver<UpdateStudentActivityResponse> out) {
        StudentActivity updated = service.updateStudentActivity(
                bridge.id(request.getStudentActivity().getId(), "id"),
                bridge.requiredIfMatch(request.getStudentActivity().getEtag()),
                bridge.changes(request.getStudentActivity(), request.getUpdateMask(), StudentActivityUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStudentActivityResponse.newBuilder(), "student_activity", updated).build());
    }

    @Override
    public void deleteStudentActivity(DeleteStudentActivityRequest request, StreamObserver<DeleteStudentActivityResponse> out) {
        service.deleteStudentActivity(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentActivityResponse.getDefaultInstance());
    }
}
