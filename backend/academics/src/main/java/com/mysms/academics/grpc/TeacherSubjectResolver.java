package com.mysms.academics.grpc;

import com.mysms.academics.api.model.TeacherSubject;
import com.mysms.academics.api.model.TeacherSubjectUpdate;
import com.mysms.academics.business.TeacherSubjectService;
import com.mysms.academics.v1.CreateTeacherSubjectRequest;
import com.mysms.academics.v1.CreateTeacherSubjectResponse;
import com.mysms.academics.v1.DeleteTeacherSubjectRequest;
import com.mysms.academics.v1.DeleteTeacherSubjectResponse;
import com.mysms.academics.v1.GetTeacherSubjectRequest;
import com.mysms.academics.v1.GetTeacherSubjectResponse;
import com.mysms.academics.v1.ListTeacherSubjectsRequest;
import com.mysms.academics.v1.ListTeacherSubjectsResponse;
import com.mysms.academics.v1.TeacherSubjectServiceGrpc;
import com.mysms.academics.v1.UpdateTeacherSubjectRequest;
import com.mysms.academics.v1.UpdateTeacherSubjectResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.TeacherSubjectService: delegates every RPC to the business service REST uses, so the
 * rules exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class TeacherSubjectResolver extends TeacherSubjectServiceGrpc.TeacherSubjectServiceImplBase {

    private final TeacherSubjectService service;
    private final ProtoBridge bridge;

    public TeacherSubjectResolver(TeacherSubjectService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTeacherSubjects(ListTeacherSubjectsRequest request,
            StreamObserver<ListTeacherSubjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TeacherSubject> rows = service.listTeacherSubjects(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTeacherSubjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTeacherSubject(GetTeacherSubjectRequest request, StreamObserver<GetTeacherSubjectResponse> out) {
        TeacherSubject found = service.getTeacherSubject(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTeacherSubjectResponse.newBuilder(), "teacher_subject", found).build());
    }

    @Override
    public void createTeacherSubject(CreateTeacherSubjectRequest request,
            StreamObserver<CreateTeacherSubjectResponse> out) {
        TeacherSubject created = service.createTeacherSubject(
                bridge.body(request.getTeacherSubject(), TeacherSubject.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateTeacherSubjectResponse.newBuilder(), "teacher_subject", created).build());
    }

    @Override
    public void updateTeacherSubject(UpdateTeacherSubjectRequest request,
            StreamObserver<UpdateTeacherSubjectResponse> out) {
        TeacherSubject updated = service.updateTeacherSubject(bridge.id(request.getTeacherSubject().getId(), "id"),
                bridge.requiredIfMatch(request.getTeacherSubject().getEtag()),
                bridge.changes(request.getTeacherSubject(), request.getUpdateMask(), TeacherSubjectUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateTeacherSubjectResponse.newBuilder(), "teacher_subject", updated).build());
    }

    @Override
    public void deleteTeacherSubject(DeleteTeacherSubjectRequest request,
            StreamObserver<DeleteTeacherSubjectResponse> out) {
        service.deleteTeacherSubject(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTeacherSubjectResponse.getDefaultInstance());
    }
}
