package com.mysms.comms.grpc;

import com.mysms.comms.api.model.AudienceGroup;
import com.mysms.comms.api.model.AudienceGroupMember;
import com.mysms.comms.api.model.AudienceGroupMemberList;
import com.mysms.comms.business.AudienceGroupService;
import com.mysms.comms.v1.AddGroupMembersRequest;
import com.mysms.comms.v1.AddGroupMembersResponse;
import com.mysms.comms.v1.AudienceGroupMemberUpdate;
import com.mysms.comms.v1.AudienceGroupServiceGrpc;
import com.mysms.comms.v1.AudienceGroupUpdate;
import com.mysms.comms.v1.CreateAudienceGroupMemberRequest;
import com.mysms.comms.v1.CreateAudienceGroupMemberResponse;
import com.mysms.comms.v1.CreateAudienceGroupRequest;
import com.mysms.comms.v1.CreateAudienceGroupResponse;
import com.mysms.comms.v1.DeleteAudienceGroupMemberRequest;
import com.mysms.comms.v1.DeleteAudienceGroupMemberResponse;
import com.mysms.comms.v1.DeleteAudienceGroupRequest;
import com.mysms.comms.v1.DeleteAudienceGroupResponse;
import com.mysms.comms.v1.GetAudienceGroupMemberRequest;
import com.mysms.comms.v1.GetAudienceGroupMemberResponse;
import com.mysms.comms.v1.GetAudienceGroupRequest;
import com.mysms.comms.v1.GetAudienceGroupResponse;
import com.mysms.comms.v1.ListAudienceGroupMembersRequest;
import com.mysms.comms.v1.ListAudienceGroupMembersResponse;
import com.mysms.comms.v1.ListAudienceGroupsRequest;
import com.mysms.comms.v1.ListAudienceGroupsResponse;
import com.mysms.comms.v1.UpdateAudienceGroupMemberResponse;
import com.mysms.comms.v1.UpdateAudienceGroupResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.AudienceGroupService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AudienceGroupResolver extends AudienceGroupServiceGrpc.AudienceGroupServiceImplBase {

    private final AudienceGroupService service;
    private final ProtoBridge bridge;

    public AudienceGroupResolver(AudienceGroupService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listAudienceGroups(ListAudienceGroupsRequest request, StreamObserver<ListAudienceGroupsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AudienceGroup> rows = service.listAudienceGroups(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAudienceGroupsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAudienceGroup(GetAudienceGroupRequest request, StreamObserver<GetAudienceGroupResponse> out) {
        AudienceGroup found = service.getAudienceGroup(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAudienceGroupResponse.newBuilder(), "audience_group", found).build());
    }

    @Override
    public void createAudienceGroup(CreateAudienceGroupRequest request, StreamObserver<CreateAudienceGroupResponse> out) {
        AudienceGroup created = service.createAudienceGroup(
                bridge.body(request.getAudienceGroup(), AudienceGroup.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAudienceGroupResponse.newBuilder(), "audience_group", created).build());
    }

    @Override
    public void updateAudienceGroup(AudienceGroupUpdate request, StreamObserver<UpdateAudienceGroupResponse> out) {
        AudienceGroup updated = service.updateAudienceGroup(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                AudienceGroupProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAudienceGroupResponse.newBuilder(), "audience_group", updated).build());
    }

    @Override
    public void deleteAudienceGroup(DeleteAudienceGroupRequest request, StreamObserver<DeleteAudienceGroupResponse> out) {
        service.deleteAudienceGroup(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAudienceGroupResponse.getDefaultInstance());
    }

    @Override
    public void listAudienceGroupMembers(ListAudienceGroupMembersRequest request,
            StreamObserver<ListAudienceGroupMembersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AudienceGroupMember> rows = service.listAudienceGroupMembers(
                bridge.id(request.getAudienceGroupId(), "audienceGroupId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAudienceGroupMembersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAudienceGroupMember(GetAudienceGroupMemberRequest request,
            StreamObserver<GetAudienceGroupMemberResponse> out) {
        AudienceGroupMember found = service.getAudienceGroupMember(
                bridge.id(request.getAudienceGroupId(), "audienceGroupId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAudienceGroupMemberResponse.newBuilder(), "audience_group_member", found).build());
    }

    @Override
    public void createAudienceGroupMember(CreateAudienceGroupMemberRequest request,
            StreamObserver<CreateAudienceGroupMemberResponse> out) {
        AudienceGroupMember created = service.createAudienceGroupMember(
                bridge.id(request.getAudienceGroupId(), "audienceGroupId"),
                bridge.body(request.getAudienceGroupMember(), AudienceGroupMember.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateAudienceGroupMemberResponse.newBuilder(), "audience_group_member", created).build());
    }

    @Override
    public void updateAudienceGroupMember(AudienceGroupMemberUpdate request,
            StreamObserver<UpdateAudienceGroupMemberResponse> out) {
        AudienceGroupMember updated = service.updateAudienceGroupMember(
                bridge.id(request.getAudienceGroupId(), "audienceGroupId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                AudienceGroupMemberProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateAudienceGroupMemberResponse.newBuilder(), "audience_group_member", updated).build());
    }

    @Override
    public void deleteAudienceGroupMember(DeleteAudienceGroupMemberRequest request,
            StreamObserver<DeleteAudienceGroupMemberResponse> out) {
        service.deleteAudienceGroupMember(
                bridge.id(request.getAudienceGroupId(), "audienceGroupId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAudienceGroupMemberResponse.getDefaultInstance());
    }

    @Override
    public void addGroupMembers(AddGroupMembersRequest request, StreamObserver<AddGroupMembersResponse> out) {
        AudienceGroupMemberList added = service.addGroupMembers(
                bridge.id(request.getAudienceGroupId(), "audienceGroupId"),
                bridge.body(request, com.mysms.comms.api.model.AddGroupMembersRequest.class, "audience_group_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(AddGroupMembersResponse.newBuilder(), "items", added.getItems()).build());
    }
}
