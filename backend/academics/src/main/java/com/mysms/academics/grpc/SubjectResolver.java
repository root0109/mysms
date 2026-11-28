package com.mysms.academics.grpc;

import com.mysms.academics.api.model.Subject;
import com.mysms.academics.business.SubjectService;
import com.mysms.academics.v1.CreateSubjectRequest;
import com.mysms.academics.v1.CreateSubjectResponse;
import com.mysms.academics.v1.DeleteSubjectRequest;
import com.mysms.academics.v1.DeleteSubjectResponse;
import com.mysms.academics.v1.GetSubjectRequest;
import com.mysms.academics.v1.GetSubjectResponse;
import com.mysms.academics.v1.ListSubjectsRequest;
import com.mysms.academics.v1.ListSubjectsResponse;
import com.mysms.academics.v1.SubjectServiceGrpc;
import com.mysms.academics.v1.SubjectUpdate;
import com.mysms.academics.v1.UpdateSubjectResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.SubjectService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SubjectResolver extends SubjectServiceGrpc.SubjectServiceImplBase {

    private final SubjectService service;
    private final ProtoBridge bridge;

    public SubjectResolver(SubjectService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSubjects(ListSubjectsRequest request, StreamObserver<ListSubjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Subject> rows = service.listSubjects(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListSubjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSubject(GetSubjectRequest request, StreamObserver<GetSubjectResponse> out) {
        Subject found = service.getSubject(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSubjectResponse.newBuilder(), "subject", found).build());
    }

    @Override
    public void createSubject(CreateSubjectRequest request, StreamObserver<CreateSubjectResponse> out) {
        Subject created = service.createSubject(bridge.body(request.getSubject(), Subject.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSubjectResponse.newBuilder(), "subject", created).build());
    }

    @Override
    public void updateSubject(SubjectUpdate request, StreamObserver<UpdateSubjectResponse> out) {
        Subject updated = service.updateSubject(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                SubjectProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSubjectResponse.newBuilder(), "subject", updated).build());
    }

    @Override
    public void deleteSubject(DeleteSubjectRequest request, StreamObserver<DeleteSubjectResponse> out) {
        service.deleteSubject(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSubjectResponse.getDefaultInstance());
    }
}
