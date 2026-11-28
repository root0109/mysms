package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.AssignmentGroupMember;
import com.mysms.learning.lms.business.AssignmentGroupMemberService;
import com.mysms.lms.v1.AcceptGroupRequestRequest;
import com.mysms.lms.v1.AcceptGroupRequestResponse;
import com.mysms.lms.v1.AssignmentGroupMemberServiceGrpc;
import com.mysms.lms.v1.AssignmentGroupMemberUpdate;
import com.mysms.lms.v1.CreateAssignmentGroupMemberRequest;
import com.mysms.lms.v1.CreateAssignmentGroupMemberResponse;
import com.mysms.lms.v1.DeleteAssignmentGroupMemberRequest;
import com.mysms.lms.v1.DeleteAssignmentGroupMemberResponse;
import com.mysms.lms.v1.GetAssignmentGroupMemberRequest;
import com.mysms.lms.v1.GetAssignmentGroupMemberResponse;
import com.mysms.lms.v1.ListAssignmentGroupMembersRequest;
import com.mysms.lms.v1.ListAssignmentGroupMembersResponse;
import com.mysms.lms.v1.RejectGroupRequestRequest;
import com.mysms.lms.v1.RejectGroupRequestResponse;
import com.mysms.lms.v1.UpdateAssignmentGroupMemberResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.AssignmentGroupMemberService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AssignmentGroupMemberResolver extends AssignmentGroupMemberServiceGrpc.AssignmentGroupMemberServiceImplBase {

    private final AssignmentGroupMemberService service;
    private final ProtoBridge bridge;

    public AssignmentGroupMemberResolver(AssignmentGroupMemberService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAssignmentGroupMembers(ListAssignmentGroupMembersRequest request,
            StreamObserver<ListAssignmentGroupMembersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AssignmentGroupMember> rows = service.listAssignmentGroupMembers(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAssignmentGroupMembersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAssignmentGroupMember(GetAssignmentGroupMemberRequest request,
            StreamObserver<GetAssignmentGroupMemberResponse> out) {
        AssignmentGroupMember found = service.getAssignmentGroupMember(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAssignmentGroupMemberResponse.newBuilder(), "assignment_group_member",
                found).build());
    }

    @Override
    public void createAssignmentGroupMember(CreateAssignmentGroupMemberRequest request,
            StreamObserver<CreateAssignmentGroupMemberResponse> out) {
        AssignmentGroupMember created = service.createAssignmentGroupMember(
                bridge.body(request.getAssignmentGroupMember(), AssignmentGroupMember.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAssignmentGroupMemberResponse.newBuilder(), "assignment_group_member",
                created).build());
    }

    @Override
    public void updateAssignmentGroupMember(AssignmentGroupMemberUpdate request,
            StreamObserver<UpdateAssignmentGroupMemberResponse> out) {
        AssignmentGroupMember updated = service.updateAssignmentGroupMember(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                AssignmentGroupMemberProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAssignmentGroupMemberResponse.newBuilder(), "assignment_group_member",
                updated).build());
    }

    @Override
    public void deleteAssignmentGroupMember(DeleteAssignmentGroupMemberRequest request,
            StreamObserver<DeleteAssignmentGroupMemberResponse> out) {
        service.deleteAssignmentGroupMember(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAssignmentGroupMemberResponse.getDefaultInstance());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void acceptGroupRequest(AcceptGroupRequestRequest request, StreamObserver<AcceptGroupRequestResponse> out) {
        AssignmentGroupMember changed = service.acceptGroupRequest(
                bridge.id(request.getAssignmentGroupMemberId(), "assignmentGroupMemberId"),
                bridge.body(request, com.mysms.learning.lms.api.model.AcceptGroupRequestRequest.class,
                        "assignment_group_member_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(AcceptGroupRequestResponse.newBuilder(), "assignment_group_member",
                changed).build());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void rejectGroupRequest(RejectGroupRequestRequest request, StreamObserver<RejectGroupRequestResponse> out) {
        AssignmentGroupMember changed = service.rejectGroupRequest(
                bridge.id(request.getAssignmentGroupMemberId(), "assignmentGroupMemberId"),
                bridge.body(request, com.mysms.learning.lms.api.model.RejectGroupRequestRequest.class,
                        "assignment_group_member_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectGroupRequestResponse.newBuilder(), "assignment_group_member",
                changed).build());
    }
}
