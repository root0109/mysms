package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ClassPage;
import com.mysms.comms.api.model.ClassPageAttachment;
import com.mysms.comms.api.model.ClassPageAttachmentUpdate;
import com.mysms.comms.api.model.ClassPageMember;
import com.mysms.comms.api.model.ClassPageMemberUpdate;
import com.mysms.comms.api.model.ClassPageUpdate;
import com.mysms.comms.business.ClassPageService;
import com.mysms.comms.v1.ClassPageServiceGrpc;
import com.mysms.comms.v1.CreateClassPageAttachmentRequest;
import com.mysms.comms.v1.CreateClassPageAttachmentResponse;
import com.mysms.comms.v1.CreateClassPageMemberRequest;
import com.mysms.comms.v1.CreateClassPageMemberResponse;
import com.mysms.comms.v1.CreateClassPageRequest;
import com.mysms.comms.v1.CreateClassPageResponse;
import com.mysms.comms.v1.DeleteClassPageAttachmentRequest;
import com.mysms.comms.v1.DeleteClassPageAttachmentResponse;
import com.mysms.comms.v1.DeleteClassPageMemberRequest;
import com.mysms.comms.v1.DeleteClassPageMemberResponse;
import com.mysms.comms.v1.DeleteClassPageRequest;
import com.mysms.comms.v1.DeleteClassPageResponse;
import com.mysms.comms.v1.FollowClassPageRequest;
import com.mysms.comms.v1.FollowClassPageResponse;
import com.mysms.comms.v1.GetClassPageAttachmentRequest;
import com.mysms.comms.v1.GetClassPageAttachmentResponse;
import com.mysms.comms.v1.GetClassPageMemberRequest;
import com.mysms.comms.v1.GetClassPageMemberResponse;
import com.mysms.comms.v1.GetClassPageRequest;
import com.mysms.comms.v1.GetClassPageResponse;
import com.mysms.comms.v1.ListClassPageAttachmentsRequest;
import com.mysms.comms.v1.ListClassPageAttachmentsResponse;
import com.mysms.comms.v1.ListClassPageMembersRequest;
import com.mysms.comms.v1.ListClassPageMembersResponse;
import com.mysms.comms.v1.ListClassPagesRequest;
import com.mysms.comms.v1.ListClassPagesResponse;
import com.mysms.comms.v1.UpdateClassPageAttachmentRequest;
import com.mysms.comms.v1.UpdateClassPageAttachmentResponse;
import com.mysms.comms.v1.UpdateClassPageMemberRequest;
import com.mysms.comms.v1.UpdateClassPageMemberResponse;
import com.mysms.comms.v1.UpdateClassPageRequest;
import com.mysms.comms.v1.UpdateClassPageResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.ClassPageService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ClassPageResolver extends ClassPageServiceGrpc.ClassPageServiceImplBase {

    private final ClassPageService service;
    private final ProtoBridge bridge;

    public ClassPageResolver(ClassPageService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listClassPages(ListClassPagesRequest request, StreamObserver<ListClassPagesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ClassPage> rows = service.listClassPages(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListClassPagesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getClassPage(GetClassPageRequest request, StreamObserver<GetClassPageResponse> out) {
        ClassPage found = service.getClassPage(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetClassPageResponse.newBuilder(), "class_page", found).build());
    }

    @Override
    public void createClassPage(CreateClassPageRequest request, StreamObserver<CreateClassPageResponse> out) {
        ClassPage created = service.createClassPage(
                bridge.body(request.getClassPage(), ClassPage.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateClassPageResponse.newBuilder(), "class_page", created).build());
    }

    @Override
    public void updateClassPage(UpdateClassPageRequest request, StreamObserver<UpdateClassPageResponse> out) {
        ClassPage updated = service.updateClassPage(
                bridge.id(request.getClassPage().getId(), "id"), bridge.requiredIfMatch(request.getClassPage().getEtag()),
                bridge.changes(request.getClassPage(), request.getUpdateMask(), ClassPageUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateClassPageResponse.newBuilder(), "class_page", updated).build());
    }

    @Override
    public void deleteClassPage(DeleteClassPageRequest request, StreamObserver<DeleteClassPageResponse> out) {
        service.deleteClassPage(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteClassPageResponse.getDefaultInstance());
    }

    @Override
    public void listClassPageAttachments(ListClassPageAttachmentsRequest request,
            StreamObserver<ListClassPageAttachmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ClassPageAttachment> rows = service.listClassPageAttachments(
                bridge.id(request.getClassPageId(), "classPageId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListClassPageAttachmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getClassPageAttachment(GetClassPageAttachmentRequest request,
            StreamObserver<GetClassPageAttachmentResponse> out) {
        ClassPageAttachment found = service.getClassPageAttachment(
                bridge.id(request.getClassPageId(), "classPageId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetClassPageAttachmentResponse.newBuilder(), "class_page_attachment", found).build());
    }

    @Override
    public void createClassPageAttachment(CreateClassPageAttachmentRequest request,
            StreamObserver<CreateClassPageAttachmentResponse> out) {
        ClassPageAttachment created = service.createClassPageAttachment(
                bridge.id(request.getClassPageId(), "classPageId"),
                bridge.body(request.getClassPageAttachment(), ClassPageAttachment.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateClassPageAttachmentResponse.newBuilder(), "class_page_attachment", created).build());
    }

    @Override
    public void updateClassPageAttachment(UpdateClassPageAttachmentRequest request,
            StreamObserver<UpdateClassPageAttachmentResponse> out) {
        ClassPageAttachment updated = service.updateClassPageAttachment(
                bridge.id(request.getClassPageId(), "classPageId"), bridge.id(request.getClassPageAttachment().getId(), "id"),
                bridge.requiredIfMatch(request.getClassPageAttachment().getEtag()),
                bridge.changes(request.getClassPageAttachment(), request.getUpdateMask(), ClassPageAttachmentUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateClassPageAttachmentResponse.newBuilder(), "class_page_attachment", updated).build());
    }

    @Override
    public void deleteClassPageAttachment(DeleteClassPageAttachmentRequest request,
            StreamObserver<DeleteClassPageAttachmentResponse> out) {
        service.deleteClassPageAttachment(
                bridge.id(request.getClassPageId(), "classPageId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteClassPageAttachmentResponse.getDefaultInstance());
    }

    @Override
    public void listClassPageMembers(ListClassPageMembersRequest request, StreamObserver<ListClassPageMembersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ClassPageMember> rows = service.listClassPageMembers(
                bridge.id(request.getClassPageId(), "classPageId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListClassPageMembersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getClassPageMember(GetClassPageMemberRequest request, StreamObserver<GetClassPageMemberResponse> out) {
        ClassPageMember found = service.getClassPageMember(
                bridge.id(request.getClassPageId(), "classPageId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetClassPageMemberResponse.newBuilder(), "class_page_member", found).build());
    }

    @Override
    public void createClassPageMember(CreateClassPageMemberRequest request, StreamObserver<CreateClassPageMemberResponse> out) {
        ClassPageMember created = service.createClassPageMember(
                bridge.id(request.getClassPageId(), "classPageId"),
                bridge.body(request.getClassPageMember(), ClassPageMember.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateClassPageMemberResponse.newBuilder(), "class_page_member", created).build());
    }

    @Override
    public void updateClassPageMember(UpdateClassPageMemberRequest request, StreamObserver<UpdateClassPageMemberResponse> out) {
        ClassPageMember updated = service.updateClassPageMember(
                bridge.id(request.getClassPageId(), "classPageId"), bridge.id(request.getClassPageMember().getId(), "id"),
                bridge.requiredIfMatch(request.getClassPageMember().getEtag()),
                bridge.changes(request.getClassPageMember(), request.getUpdateMask(), ClassPageMemberUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateClassPageMemberResponse.newBuilder(), "class_page_member", updated).build());
    }

    @Override
    public void deleteClassPageMember(DeleteClassPageMemberRequest request, StreamObserver<DeleteClassPageMemberResponse> out) {
        service.deleteClassPageMember(
                bridge.id(request.getClassPageId(), "classPageId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteClassPageMemberResponse.getDefaultInstance());
    }

    @Override
    public void followClassPage(FollowClassPageRequest request, StreamObserver<FollowClassPageResponse> out) {
        ClassPageMember member = service.followClassPage(bridge.id(request.getClassPageId(), "classPageId"),
                bridge.body(request, com.mysms.comms.api.model.FollowClassPageRequest.class, "class_page_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(FollowClassPageResponse.newBuilder(), "result", member).build());
    }
}
