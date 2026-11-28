package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.students.sis.api.model.StudentEnrollment;
import com.mysms.students.sis.business.StudentEnrollmentService;
import com.mysms.sis.v1.CreateStudentEnrollmentRequest;
import com.mysms.sis.v1.CreateStudentEnrollmentResponse;
import com.mysms.sis.v1.DeleteStudentEnrollmentRequest;
import com.mysms.sis.v1.DeleteStudentEnrollmentResponse;
import com.mysms.sis.v1.GetStudentEnrollmentRequest;
import com.mysms.sis.v1.GetStudentEnrollmentResponse;
import com.mysms.sis.v1.ListClassRosterRequest;
import com.mysms.sis.v1.ListClassRosterResponse;
import com.mysms.sis.v1.ListStudentEnrollmentsRequest;
import com.mysms.sis.v1.ListStudentEnrollmentsResponse;
import com.mysms.sis.v1.StudentEnrollmentServiceGrpc;
import com.mysms.sis.v1.StudentEnrollmentUpdate;
import com.mysms.sis.v1.UpdateStudentEnrollmentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.StudentEnrollmentService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StudentEnrollmentResolver extends StudentEnrollmentServiceGrpc.StudentEnrollmentServiceImplBase {

    private final StudentEnrollmentService service;
    private final ProtoBridge bridge;

    public StudentEnrollmentResolver(StudentEnrollmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listClassRoster(ListClassRosterRequest request, StreamObserver<ListClassRosterResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentEnrollment> rows = service.listClassRoster(
                bridge.id(request.getClassSectionId(), "classSectionId"), page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(ListClassRosterResponse.newBuilder(), rows).build());
    }

    @Override
    public void listStudentEnrollments(ListStudentEnrollmentsRequest request,
            StreamObserver<ListStudentEnrollmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentEnrollment> rows = service.listStudentEnrollments(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentEnrollmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentEnrollment(GetStudentEnrollmentRequest request, StreamObserver<GetStudentEnrollmentResponse> out) {
        StudentEnrollment found = service.getStudentEnrollment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentEnrollmentResponse.newBuilder(), "student_enrollment", found).build());
    }

    @Override
    public void createStudentEnrollment(CreateStudentEnrollmentRequest request,
            StreamObserver<CreateStudentEnrollmentResponse> out) {
        StudentEnrollment created = service.createStudentEnrollment(
                bridge.body(request.getStudentEnrollment(), StudentEnrollment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateStudentEnrollmentResponse.newBuilder(), "student_enrollment", created).build());
    }

    @Override
    public void updateStudentEnrollment(StudentEnrollmentUpdate request,
            StreamObserver<UpdateStudentEnrollmentResponse> out) {
        StudentEnrollment updated = service.updateStudentEnrollment(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                StudentEnrollmentProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateStudentEnrollmentResponse.newBuilder(), "student_enrollment", updated).build());
    }

    @Override
    public void deleteStudentEnrollment(DeleteStudentEnrollmentRequest request,
            StreamObserver<DeleteStudentEnrollmentResponse> out) {
        service.deleteStudentEnrollment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentEnrollmentResponse.getDefaultInstance());
    }
}
