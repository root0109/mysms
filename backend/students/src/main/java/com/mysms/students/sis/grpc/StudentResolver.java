package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.students.sis.api.model.Operation;
import com.mysms.students.sis.api.model.Student;
import com.mysms.students.sis.api.model.StudentGuardian;
import com.mysms.students.sis.api.model.StudentGuardianUpdate;
import com.mysms.students.sis.api.model.StudentSibling;
import com.mysms.students.sis.api.model.StudentSiblingUpdate;
import com.mysms.students.sis.api.model.StudentSummary;
import com.mysms.students.sis.api.model.StudentUpdate;
import com.mysms.students.sis.business.PortalAccountService;
import com.mysms.students.sis.business.StudentService;
import com.mysms.students.sis.business.StudentSummaryService;
import com.mysms.students.sis.business.StudentWorkflowService;
import com.mysms.sis.v1.ChangeStudentStatusRequest;
import com.mysms.sis.v1.ChangeStudentStatusResponse;
import com.mysms.sis.v1.CreateStudentGuardianRequest;
import com.mysms.sis.v1.CreateStudentGuardianResponse;
import com.mysms.sis.v1.CreateStudentPortalAccountRequest;
import com.mysms.sis.v1.CreateStudentPortalAccountResponse;
import com.mysms.sis.v1.CreateStudentRequest;
import com.mysms.sis.v1.CreateStudentResponse;
import com.mysms.sis.v1.CreateStudentSiblingRequest;
import com.mysms.sis.v1.CreateStudentSiblingResponse;
import com.mysms.sis.v1.DeleteStudentGuardianRequest;
import com.mysms.sis.v1.DeleteStudentGuardianResponse;
import com.mysms.sis.v1.DeleteStudentRequest;
import com.mysms.sis.v1.DeleteStudentResponse;
import com.mysms.sis.v1.DeleteStudentSiblingRequest;
import com.mysms.sis.v1.DeleteStudentSiblingResponse;
import com.mysms.sis.v1.GetStudentGuardianRequest;
import com.mysms.sis.v1.GetStudentGuardianResponse;
import com.mysms.sis.v1.GetStudentRequest;
import com.mysms.sis.v1.GetStudentResponse;
import com.mysms.sis.v1.GetStudentSiblingRequest;
import com.mysms.sis.v1.GetStudentSiblingResponse;
import com.mysms.sis.v1.GetStudentSummaryRequest;
import com.mysms.sis.v1.GetStudentSummaryResponse;
import com.mysms.sis.v1.IssueBonafideCertificateRequest;
import com.mysms.sis.v1.IssueBonafideCertificateResponse;
import com.mysms.sis.v1.ListStudentGuardiansRequest;
import com.mysms.sis.v1.ListStudentGuardiansResponse;
import com.mysms.sis.v1.ListStudentSiblingsRequest;
import com.mysms.sis.v1.ListStudentSiblingsResponse;
import com.mysms.sis.v1.ListStudentsRequest;
import com.mysms.sis.v1.ListStudentsResponse;
import com.mysms.sis.v1.StudentServiceGrpc;
import com.mysms.sis.v1.UpdateStudentGuardianRequest;
import com.mysms.sis.v1.UpdateStudentGuardianResponse;
import com.mysms.sis.v1.UpdateStudentRequest;
import com.mysms.sis.v1.UpdateStudentResponse;
import com.mysms.sis.v1.UpdateStudentSiblingRequest;
import com.mysms.sis.v1.UpdateStudentSiblingResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.StudentService: delegates every RPC to the business service REST uses, so the rules exist once.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class StudentResolver extends StudentServiceGrpc.StudentServiceImplBase {

    private final StudentService studentService;
    private final StudentWorkflowService workflowService;
    private final PortalAccountService portalAccountService;
    private final StudentSummaryService summaryService;
    private final ProtoBridge bridge;

    public StudentResolver(
            StudentService studentService, StudentWorkflowService workflowService, PortalAccountService portalAccountService,
            StudentSummaryService summaryService, ProtoBridge bridge) {
        this.studentService = studentService;
        this.workflowService = workflowService;
        this.portalAccountService = portalAccountService;
        this.summaryService = summaryService;
        this.bridge = bridge;
    }

    @Override
    public void listStudents(ListStudentsRequest request, StreamObserver<ListStudentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Student> rows = studentService.listStudents(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudent(GetStudentRequest request, StreamObserver<GetStudentResponse> out) {
        Student found = studentService.getStudent(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentResponse.newBuilder(), "student", found).build());
    }

    @Override
    public void createStudent(CreateStudentRequest request, StreamObserver<CreateStudentResponse> out) {
        Student created = studentService.createStudent(
                bridge.body(request.getStudent(), Student.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStudentResponse.newBuilder(), "student", created).build());
    }

    @Override
    public void updateStudent(UpdateStudentRequest request, StreamObserver<UpdateStudentResponse> out) {
        Student updated = studentService.updateStudent(
                bridge.id(request.getStudent().getId(), "id"), bridge.requiredIfMatch(request.getStudent().getEtag()),
                bridge.changes(request.getStudent(), request.getUpdateMask(), StudentUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStudentResponse.newBuilder(), "student", updated).build());
    }

    @Override
    public void deleteStudent(DeleteStudentRequest request, StreamObserver<DeleteStudentResponse> out) {
        studentService.deleteStudent(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentResponse.getDefaultInstance());
    }

    @Override
    public void listStudentGuardians(ListStudentGuardiansRequest request, StreamObserver<ListStudentGuardiansResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentGuardian> rows = studentService.listStudentGuardians(
                bridge.id(request.getStudentId(), "studentId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentGuardiansResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentGuardian(GetStudentGuardianRequest request, StreamObserver<GetStudentGuardianResponse> out) {
        StudentGuardian found = studentService.getStudentGuardian(
                bridge.id(request.getStudentId(), "studentId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentGuardianResponse.newBuilder(), "student_guardian", found).build());
    }

    @Override
    public void createStudentGuardian(CreateStudentGuardianRequest request, StreamObserver<CreateStudentGuardianResponse> out) {
        StudentGuardian created = studentService.createStudentGuardian(
                bridge.id(request.getStudentId(), "studentId"),
                bridge.body(request.getStudentGuardian(), StudentGuardian.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStudentGuardianResponse.newBuilder(), "student_guardian", created).build());
    }

    @Override
    public void updateStudentGuardian(UpdateStudentGuardianRequest request, StreamObserver<UpdateStudentGuardianResponse> out) {
        StudentGuardian updated = studentService.updateStudentGuardian(
                bridge.id(request.getStudentId(), "studentId"), bridge.id(request.getStudentGuardian().getId(), "id"),
                bridge.requiredIfMatch(request.getStudentGuardian().getEtag()),
                bridge.changes(request.getStudentGuardian(), request.getUpdateMask(), StudentGuardianUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStudentGuardianResponse.newBuilder(), "student_guardian", updated).build());
    }

    @Override
    public void deleteStudentGuardian(DeleteStudentGuardianRequest request, StreamObserver<DeleteStudentGuardianResponse> out) {
        studentService.deleteStudentGuardian(
                bridge.id(request.getStudentId(), "studentId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentGuardianResponse.getDefaultInstance());
    }

    @Override
    public void listStudentSiblings(ListStudentSiblingsRequest request, StreamObserver<ListStudentSiblingsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StudentSibling> rows = studentService.listStudentSiblings(
                bridge.id(request.getStudentId(), "studentId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListStudentSiblingsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getStudentSibling(GetStudentSiblingRequest request, StreamObserver<GetStudentSiblingResponse> out) {
        StudentSibling found = studentService.getStudentSibling(
                bridge.id(request.getStudentId(), "studentId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetStudentSiblingResponse.newBuilder(), "student_sibling", found).build());
    }

    @Override
    public void createStudentSibling(CreateStudentSiblingRequest request, StreamObserver<CreateStudentSiblingResponse> out) {
        StudentSibling created = studentService.createStudentSibling(
                bridge.id(request.getStudentId(), "studentId"), bridge.body(request.getStudentSibling(), StudentSibling.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateStudentSiblingResponse.newBuilder(), "student_sibling", created).build());
    }

    @Override
    public void updateStudentSibling(UpdateStudentSiblingRequest request, StreamObserver<UpdateStudentSiblingResponse> out) {
        StudentSibling updated = studentService.updateStudentSibling(
                bridge.id(request.getStudentId(), "studentId"), bridge.id(request.getStudentSibling().getId(), "id"),
                bridge.requiredIfMatch(request.getStudentSibling().getEtag()),
                bridge.changes(request.getStudentSibling(), request.getUpdateMask(), StudentSiblingUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateStudentSiblingResponse.newBuilder(), "student_sibling", updated).build());
    }

    @Override
    public void deleteStudentSibling(DeleteStudentSiblingRequest request, StreamObserver<DeleteStudentSiblingResponse> out) {
        studentService.deleteStudentSibling(
                bridge.id(request.getStudentId(), "studentId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteStudentSiblingResponse.getDefaultInstance());
    }

    @Override
    public void changeStudentStatus(ChangeStudentStatusRequest request, StreamObserver<ChangeStudentStatusResponse> out) {
        Student changed = workflowService.changeStudentStatus(
                bridge.id(request.getStudentId(), "studentId"),
                bridge.body(request, com.mysms.students.sis.api.model.ChangeStudentStatusRequest.class, "student_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ChangeStudentStatusResponse.newBuilder(), "student", changed).build());
    }

    @Override
    public void getStudentSummary(GetStudentSummaryRequest request, StreamObserver<GetStudentSummaryResponse> out) {
        String academicYearId = bridge.orNull(request.getAcademicYearId());
        StudentSummary summary = summaryService.getStudentSummary(bridge.id(request.getStudentId(), "studentId"),
                academicYearId == null ? null : bridge.id(academicYearId, "academicYearId"));
        ProtoBridge.send(out, bridge.reply(GetStudentSummaryResponse.newBuilder(), "result", summary).build());
    }

    @Override
    public void issueBonafideCertificate(IssueBonafideCertificateRequest request,
            StreamObserver<IssueBonafideCertificateResponse> out) {
        Operation operation = workflowService.issueBonafideCertificate(
                bridge.id(request.getStudentId(), "studentId"),
                bridge.body(request, com.mysms.students.sis.api.model.IssueBonafideCertificateRequest.class, "student_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(IssueBonafideCertificateResponse.newBuilder(), "result", operation).build());
    }

    @Override
    public void createStudentPortalAccount(CreateStudentPortalAccountRequest request,
            StreamObserver<CreateStudentPortalAccountResponse> out) {
        Student changed = portalAccountService.createStudentPortalAccount(
                bridge.id(request.getStudentId(), "studentId"),
                bridge.body(request, com.mysms.students.sis.api.model.CreateStudentPortalAccountRequest.class, "student_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CreateStudentPortalAccountResponse.newBuilder(), "student", changed).build());
    }
}
