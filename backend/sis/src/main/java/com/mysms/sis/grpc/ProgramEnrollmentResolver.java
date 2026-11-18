package com.mysms.sis.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.sis.api.model.ProgramEnrollment;
import com.mysms.sis.api.model.ProgramEnrollmentSubject;
import com.mysms.sis.api.model.ProgramEnrollmentSubjectUpdate;
import com.mysms.sis.api.model.ProgramEnrollmentUpdate;
import com.mysms.sis.business.ProgramEnrollmentService;
import com.mysms.sis.v1.CreateProgramEnrollmentRequest;
import com.mysms.sis.v1.CreateProgramEnrollmentResponse;
import com.mysms.sis.v1.CreateProgramEnrollmentSubjectRequest;
import com.mysms.sis.v1.CreateProgramEnrollmentSubjectResponse;
import com.mysms.sis.v1.DeleteProgramEnrollmentRequest;
import com.mysms.sis.v1.DeleteProgramEnrollmentResponse;
import com.mysms.sis.v1.DeleteProgramEnrollmentSubjectRequest;
import com.mysms.sis.v1.DeleteProgramEnrollmentSubjectResponse;
import com.mysms.sis.v1.GetProgramEnrollmentRequest;
import com.mysms.sis.v1.GetProgramEnrollmentResponse;
import com.mysms.sis.v1.GetProgramEnrollmentSubjectRequest;
import com.mysms.sis.v1.GetProgramEnrollmentSubjectResponse;
import com.mysms.sis.v1.ListProgramEnrollmentSubjectsRequest;
import com.mysms.sis.v1.ListProgramEnrollmentSubjectsResponse;
import com.mysms.sis.v1.ListProgramEnrollmentsRequest;
import com.mysms.sis.v1.ListProgramEnrollmentsResponse;
import com.mysms.sis.v1.ProgramEnrollmentServiceGrpc;
import com.mysms.sis.v1.UpdateProgramEnrollmentRequest;
import com.mysms.sis.v1.UpdateProgramEnrollmentResponse;
import com.mysms.sis.v1.UpdateProgramEnrollmentSubjectRequest;
import com.mysms.sis.v1.UpdateProgramEnrollmentSubjectResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of sis.ProgramEnrollmentService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ProgramEnrollmentResolver extends ProgramEnrollmentServiceGrpc.ProgramEnrollmentServiceImplBase {

    private final ProgramEnrollmentService service;
    private final ProtoBridge bridge;

    public ProgramEnrollmentResolver(ProgramEnrollmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listProgramEnrollments(ListProgramEnrollmentsRequest request,
            StreamObserver<ListProgramEnrollmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ProgramEnrollment> rows = service.listProgramEnrollments(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListProgramEnrollmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getProgramEnrollment(GetProgramEnrollmentRequest request, StreamObserver<GetProgramEnrollmentResponse> out) {
        ProgramEnrollment found = service.getProgramEnrollment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetProgramEnrollmentResponse.newBuilder(), "program_enrollment", found).build());
    }

    @Override
    public void createProgramEnrollment(CreateProgramEnrollmentRequest request,
            StreamObserver<CreateProgramEnrollmentResponse> out) {
        ProgramEnrollment created = service.createProgramEnrollment(
                bridge.body(request.getProgramEnrollment(), ProgramEnrollment.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateProgramEnrollmentResponse.newBuilder(), "program_enrollment", created).build());
    }

    @Override
    public void updateProgramEnrollment(UpdateProgramEnrollmentRequest request,
            StreamObserver<UpdateProgramEnrollmentResponse> out) {
        ProgramEnrollment updated = service.updateProgramEnrollment(
                bridge.id(request.getProgramEnrollment().getId(), "id"),
                bridge.requiredIfMatch(request.getProgramEnrollment().getEtag()),
                bridge.changes(request.getProgramEnrollment(), request.getUpdateMask(), ProgramEnrollmentUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateProgramEnrollmentResponse.newBuilder(), "program_enrollment", updated).build());
    }

    @Override
    public void deleteProgramEnrollment(DeleteProgramEnrollmentRequest request,
            StreamObserver<DeleteProgramEnrollmentResponse> out) {
        service.deleteProgramEnrollment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteProgramEnrollmentResponse.getDefaultInstance());
    }

    @Override
    public void listProgramEnrollmentSubjects(ListProgramEnrollmentSubjectsRequest request,
            StreamObserver<ListProgramEnrollmentSubjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ProgramEnrollmentSubject> rows = service.listProgramEnrollmentSubjects(
                bridge.id(request.getProgramEnrollmentId(), "programEnrollmentId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListProgramEnrollmentSubjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getProgramEnrollmentSubject(GetProgramEnrollmentSubjectRequest request,
            StreamObserver<GetProgramEnrollmentSubjectResponse> out) {
        ProgramEnrollmentSubject found = service.getProgramEnrollmentSubject(
                bridge.id(request.getProgramEnrollmentId(), "programEnrollmentId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetProgramEnrollmentSubjectResponse.newBuilder(), "program_enrollment_subject", found).build());
    }

    @Override
    public void createProgramEnrollmentSubject(CreateProgramEnrollmentSubjectRequest request,
            StreamObserver<CreateProgramEnrollmentSubjectResponse> out) {
        ProgramEnrollmentSubject created = service.createProgramEnrollmentSubject(
                bridge.id(request.getProgramEnrollmentId(), "programEnrollmentId"),
                bridge.body(request.getProgramEnrollmentSubject(), ProgramEnrollmentSubject.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateProgramEnrollmentSubjectResponse.newBuilder(), "program_enrollment_subject", created).build());
    }

    @Override
    public void updateProgramEnrollmentSubject(UpdateProgramEnrollmentSubjectRequest request,
            StreamObserver<UpdateProgramEnrollmentSubjectResponse> out) {
        ProgramEnrollmentSubject updated = service.updateProgramEnrollmentSubject(
                bridge.id(request.getProgramEnrollmentId(), "programEnrollmentId"),
                bridge.id(request.getProgramEnrollmentSubject().getId(), "id"),
                bridge.requiredIfMatch(request.getProgramEnrollmentSubject().getEtag()),
                bridge.changes(request.getProgramEnrollmentSubject(), request.getUpdateMask(),
                        ProgramEnrollmentSubjectUpdate.class), bridge.updateMask(request.getUpdateMask()),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateProgramEnrollmentSubjectResponse.newBuilder(), "program_enrollment_subject", updated).build());
    }

    @Override
    public void deleteProgramEnrollmentSubject(DeleteProgramEnrollmentSubjectRequest request,
            StreamObserver<DeleteProgramEnrollmentSubjectResponse> out) {
        service.deleteProgramEnrollmentSubject(
                bridge.id(request.getProgramEnrollmentId(), "programEnrollmentId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteProgramEnrollmentSubjectResponse.getDefaultInstance());
    }
}
