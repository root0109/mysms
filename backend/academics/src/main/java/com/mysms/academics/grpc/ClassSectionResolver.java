package com.mysms.academics.grpc;

import com.mysms.academics.api.model.ClassSection;
import com.mysms.academics.api.model.ClassSectionUpdate;
import com.mysms.academics.api.model.ClassSubject;
import com.mysms.academics.api.model.ClassSubjectUpdate;
import com.mysms.academics.business.ClassSectionService;
import com.mysms.academics.v1.ClassSectionServiceGrpc;
import com.mysms.academics.v1.CreateClassSectionRequest;
import com.mysms.academics.v1.CreateClassSectionResponse;
import com.mysms.academics.v1.CreateClassSubjectRequest;
import com.mysms.academics.v1.CreateClassSubjectResponse;
import com.mysms.academics.v1.DeleteClassSectionRequest;
import com.mysms.academics.v1.DeleteClassSectionResponse;
import com.mysms.academics.v1.DeleteClassSubjectRequest;
import com.mysms.academics.v1.DeleteClassSubjectResponse;
import com.mysms.academics.v1.GetClassSectionRequest;
import com.mysms.academics.v1.GetClassSectionResponse;
import com.mysms.academics.v1.GetClassSubjectRequest;
import com.mysms.academics.v1.GetClassSubjectResponse;
import com.mysms.academics.v1.ListClassSectionsRequest;
import com.mysms.academics.v1.ListClassSectionsResponse;
import com.mysms.academics.v1.ListClassSubjectsRequest;
import com.mysms.academics.v1.ListClassSubjectsResponse;
import com.mysms.academics.v1.UpdateClassSectionRequest;
import com.mysms.academics.v1.UpdateClassSectionResponse;
import com.mysms.academics.v1.UpdateClassSubjectRequest;
import com.mysms.academics.v1.UpdateClassSubjectResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.ClassSectionService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ClassSectionResolver extends ClassSectionServiceGrpc.ClassSectionServiceImplBase {

    private final ClassSectionService service;
    private final ProtoBridge bridge;

    public ClassSectionResolver(ClassSectionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listClassSections(ListClassSectionsRequest request, StreamObserver<ListClassSectionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ClassSection> rows = service.listClassSections(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListClassSectionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getClassSection(GetClassSectionRequest request, StreamObserver<GetClassSectionResponse> out) {
        ClassSection found = service.getClassSection(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetClassSectionResponse.newBuilder(), "class_section", found).build());
    }

    @Override
    public void createClassSection(CreateClassSectionRequest request, StreamObserver<CreateClassSectionResponse> out) {
        ClassSection created = service.createClassSection(bridge.body(request.getClassSection(), ClassSection.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateClassSectionResponse.newBuilder(), "class_section", created).build());
    }

    @Override
    public void updateClassSection(UpdateClassSectionRequest request, StreamObserver<UpdateClassSectionResponse> out) {
        ClassSection updated = service.updateClassSection(bridge.id(request.getClassSection().getId(), "id"),
                bridge.requiredIfMatch(request.getClassSection().getEtag()),
                bridge.changes(request.getClassSection(), request.getUpdateMask(), ClassSectionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateClassSectionResponse.newBuilder(), "class_section", updated).build());
    }

    @Override
    public void deleteClassSection(DeleteClassSectionRequest request, StreamObserver<DeleteClassSectionResponse> out) {
        service.deleteClassSection(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteClassSectionResponse.getDefaultInstance());
    }

    @Override
    public void listClassSubjects(ListClassSubjectsRequest request, StreamObserver<ListClassSubjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ClassSubject> rows = service.listClassSubjects(bridge.id(request.getClassSectionId(), "classSectionId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListClassSubjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getClassSubject(GetClassSubjectRequest request, StreamObserver<GetClassSubjectResponse> out) {
        ClassSubject found = service.getClassSubject(bridge.id(request.getClassSectionId(), "classSectionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetClassSubjectResponse.newBuilder(), "class_subject", found).build());
    }

    @Override
    public void createClassSubject(CreateClassSubjectRequest request, StreamObserver<CreateClassSubjectResponse> out) {
        ClassSubject created = service.createClassSubject(bridge.id(request.getClassSectionId(), "classSectionId"),
                bridge.body(request.getClassSubject(), ClassSubject.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateClassSubjectResponse.newBuilder(), "class_subject", created).build());
    }

    @Override
    public void updateClassSubject(UpdateClassSubjectRequest request, StreamObserver<UpdateClassSubjectResponse> out) {
        ClassSubject updated = service.updateClassSubject(bridge.id(request.getClassSectionId(), "classSectionId"),
                bridge.id(request.getClassSubject().getId(), "id"), bridge.requiredIfMatch(request.getClassSubject().getEtag()),
                bridge.changes(request.getClassSubject(), request.getUpdateMask(), ClassSubjectUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateClassSubjectResponse.newBuilder(), "class_subject", updated).build());
    }

    @Override
    public void deleteClassSubject(DeleteClassSubjectRequest request, StreamObserver<DeleteClassSubjectResponse> out) {
        service.deleteClassSubject(bridge.id(request.getClassSectionId(), "classSectionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteClassSubjectResponse.getDefaultInstance());
    }
}
