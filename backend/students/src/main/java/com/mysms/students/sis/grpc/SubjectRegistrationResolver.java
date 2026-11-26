package com.mysms.students.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.students.sis.api.model.SubjectRegistration;
import com.mysms.students.sis.api.model.SubjectRegistrationLine;
import com.mysms.students.sis.api.model.SubjectRegistrationLineUpdate;
import com.mysms.students.sis.api.model.SubjectRegistrationUpdate;
import com.mysms.students.sis.business.SubjectRegistrationService;
import com.mysms.students.sis.business.SubjectRegistrationWorkflowService;
import com.mysms.sis.v1.ApproveSubjectRegistrationRequest;
import com.mysms.sis.v1.ApproveSubjectRegistrationResponse;
import com.mysms.sis.v1.CreateSubjectRegistrationLineRequest;
import com.mysms.sis.v1.CreateSubjectRegistrationLineResponse;
import com.mysms.sis.v1.CreateSubjectRegistrationRequest;
import com.mysms.sis.v1.CreateSubjectRegistrationResponse;
import com.mysms.sis.v1.DeleteSubjectRegistrationLineRequest;
import com.mysms.sis.v1.DeleteSubjectRegistrationLineResponse;
import com.mysms.sis.v1.DeleteSubjectRegistrationRequest;
import com.mysms.sis.v1.DeleteSubjectRegistrationResponse;
import com.mysms.sis.v1.GetSubjectRegistrationLineRequest;
import com.mysms.sis.v1.GetSubjectRegistrationLineResponse;
import com.mysms.sis.v1.GetSubjectRegistrationRequest;
import com.mysms.sis.v1.GetSubjectRegistrationResponse;
import com.mysms.sis.v1.ListSubjectRegistrationLinesRequest;
import com.mysms.sis.v1.ListSubjectRegistrationLinesResponse;
import com.mysms.sis.v1.ListSubjectRegistrationsRequest;
import com.mysms.sis.v1.ListSubjectRegistrationsResponse;
import com.mysms.sis.v1.LoadCourseSubjectsRequest;
import com.mysms.sis.v1.LoadCourseSubjectsResponse;
import com.mysms.sis.v1.RejectSubjectRegistrationRequest;
import com.mysms.sis.v1.RejectSubjectRegistrationResponse;
import com.mysms.sis.v1.ResetSubjectRegistrationRequest;
import com.mysms.sis.v1.ResetSubjectRegistrationResponse;
import com.mysms.sis.v1.SubjectRegistrationServiceGrpc;
import com.mysms.sis.v1.SubmitSubjectRegistrationRequest;
import com.mysms.sis.v1.SubmitSubjectRegistrationResponse;
import com.mysms.sis.v1.UpdateSubjectRegistrationLineRequest;
import com.mysms.sis.v1.UpdateSubjectRegistrationLineResponse;
import com.mysms.sis.v1.UpdateSubjectRegistrationRequest;
import com.mysms.sis.v1.UpdateSubjectRegistrationResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.SubjectRegistrationService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SubjectRegistrationResolver extends SubjectRegistrationServiceGrpc.SubjectRegistrationServiceImplBase {

    private final SubjectRegistrationService subjectRegistrationService;
    private final SubjectRegistrationWorkflowService workflowService;
    private final ProtoBridge bridge;

    public SubjectRegistrationResolver(
            SubjectRegistrationService subjectRegistrationService, SubjectRegistrationWorkflowService workflowService,
            ProtoBridge bridge) {
        this.subjectRegistrationService = subjectRegistrationService;
        this.workflowService = workflowService;
        this.bridge = bridge;
    }

    @Override
    public void listSubjectRegistrations(ListSubjectRegistrationsRequest request,
            StreamObserver<ListSubjectRegistrationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SubjectRegistration> rows = subjectRegistrationService.listSubjectRegistrations(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSubjectRegistrationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSubjectRegistration(GetSubjectRegistrationRequest request,
            StreamObserver<GetSubjectRegistrationResponse> out) {
        SubjectRegistration found = subjectRegistrationService.getSubjectRegistration(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSubjectRegistrationResponse.newBuilder(), "subject_registration", found).build());
    }

    @Override
    public void createSubjectRegistration(CreateSubjectRegistrationRequest request,
            StreamObserver<CreateSubjectRegistrationResponse> out) {
        SubjectRegistration created = subjectRegistrationService.createSubjectRegistration(
                bridge.body(request.getSubjectRegistration(), SubjectRegistration.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateSubjectRegistrationResponse.newBuilder(), "subject_registration", created).build());
    }

    @Override
    public void updateSubjectRegistration(UpdateSubjectRegistrationRequest request,
            StreamObserver<UpdateSubjectRegistrationResponse> out) {
        SubjectRegistration updated = subjectRegistrationService.updateSubjectRegistration(
                bridge.id(request.getSubjectRegistration().getId(), "id"),
                bridge.requiredIfMatch(request.getSubjectRegistration().getEtag()),
                bridge.changes(request.getSubjectRegistration(), request.getUpdateMask(), SubjectRegistrationUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateSubjectRegistrationResponse.newBuilder(), "subject_registration", updated).build());
    }

    @Override
    public void deleteSubjectRegistration(DeleteSubjectRegistrationRequest request,
            StreamObserver<DeleteSubjectRegistrationResponse> out) {
        subjectRegistrationService.deleteSubjectRegistration(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSubjectRegistrationResponse.getDefaultInstance());
    }

    @Override
    public void listSubjectRegistrationLines(ListSubjectRegistrationLinesRequest request,
            StreamObserver<ListSubjectRegistrationLinesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SubjectRegistrationLine> rows = subjectRegistrationService.listSubjectRegistrationLines(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSubjectRegistrationLinesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSubjectRegistrationLine(GetSubjectRegistrationLineRequest request,
            StreamObserver<GetSubjectRegistrationLineResponse> out) {
        SubjectRegistrationLine found = subjectRegistrationService.getSubjectRegistrationLine(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetSubjectRegistrationLineResponse.newBuilder(), "subject_registration_line", found).build());
    }

    @Override
    public void createSubjectRegistrationLine(CreateSubjectRegistrationLineRequest request,
            StreamObserver<CreateSubjectRegistrationLineResponse> out) {
        SubjectRegistrationLine created = subjectRegistrationService.createSubjectRegistrationLine(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"),
                bridge.body(request.getSubjectRegistrationLine(), SubjectRegistrationLine.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateSubjectRegistrationLineResponse.newBuilder(), "subject_registration_line", created).build());
    }

    @Override
    public void updateSubjectRegistrationLine(UpdateSubjectRegistrationLineRequest request,
            StreamObserver<UpdateSubjectRegistrationLineResponse> out) {
        SubjectRegistrationLine updated = subjectRegistrationService.updateSubjectRegistrationLine(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"),
                bridge.id(request.getSubjectRegistrationLine().getId(), "id"),
                bridge.requiredIfMatch(request.getSubjectRegistrationLine().getEtag()),
                bridge.changes(request.getSubjectRegistrationLine(), request.getUpdateMask(),
                        SubjectRegistrationLineUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateSubjectRegistrationLineResponse.newBuilder(), "subject_registration_line", updated).build());
    }

    @Override
    public void deleteSubjectRegistrationLine(DeleteSubjectRegistrationLineRequest request,
            StreamObserver<DeleteSubjectRegistrationLineResponse> out) {
        subjectRegistrationService.deleteSubjectRegistrationLine(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSubjectRegistrationLineResponse.getDefaultInstance());
    }

    @Override
    public void submitSubjectRegistration(SubmitSubjectRegistrationRequest request,
            StreamObserver<SubmitSubjectRegistrationResponse> out) {
        SubjectRegistration changed = subjectRegistrationService.submitSubjectRegistration(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"),
                bridge.body(request, com.mysms.students.sis.api.model.SubmitSubjectRegistrationRequest.class,
                        "subject_registration_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(
                SubmitSubjectRegistrationResponse.newBuilder(), "subject_registration", changed).build());
    }

    @Override
    public void approveSubjectRegistration(ApproveSubjectRegistrationRequest request,
            StreamObserver<ApproveSubjectRegistrationResponse> out) {
        SubjectRegistration changed = workflowService.approveSubjectRegistration(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"),
                bridge.body(request, com.mysms.students.sis.api.model.ApproveSubjectRegistrationRequest.class,
                        "subject_registration_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(
                ApproveSubjectRegistrationResponse.newBuilder(), "subject_registration", changed).build());
    }

    @Override
    public void rejectSubjectRegistration(RejectSubjectRegistrationRequest request,
            StreamObserver<RejectSubjectRegistrationResponse> out) {
        SubjectRegistration changed = subjectRegistrationService.rejectSubjectRegistration(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"),
                bridge.body(request, com.mysms.students.sis.api.model.RejectSubjectRegistrationRequest.class,
                        "subject_registration_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(
                RejectSubjectRegistrationResponse.newBuilder(), "subject_registration", changed).build());
    }

    @Override
    public void resetSubjectRegistration(ResetSubjectRegistrationRequest request,
            StreamObserver<ResetSubjectRegistrationResponse> out) {
        SubjectRegistration changed = subjectRegistrationService.resetSubjectRegistration(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"),
                bridge.body(request, com.mysms.students.sis.api.model.ResetSubjectRegistrationRequest.class,
                        "subject_registration_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(
                ResetSubjectRegistrationResponse.newBuilder(), "subject_registration", changed).build());
    }

    @Override
    public void loadCourseSubjects(LoadCourseSubjectsRequest request, StreamObserver<LoadCourseSubjectsResponse> out) {
        SubjectRegistration changed = workflowService.loadCourseSubjects(
                bridge.id(request.getSubjectRegistrationId(), "subjectRegistrationId"));
        ProtoBridge.send(out, bridge.reply(LoadCourseSubjectsResponse.newBuilder(), "subject_registration", changed).build());
    }
}
