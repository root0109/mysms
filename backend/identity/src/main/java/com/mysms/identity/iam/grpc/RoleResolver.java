package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.identity.iam.api.model.Role;
import com.mysms.identity.iam.api.model.RolePermission;
import com.mysms.identity.iam.business.RoleService;
import com.mysms.iam.v1.CreateRolePermissionRequest;
import com.mysms.iam.v1.CreateRolePermissionResponse;
import com.mysms.iam.v1.CreateRoleRequest;
import com.mysms.iam.v1.CreateRoleResponse;
import com.mysms.iam.v1.DeleteRolePermissionRequest;
import com.mysms.iam.v1.DeleteRolePermissionResponse;
import com.mysms.iam.v1.DeleteRoleRequest;
import com.mysms.iam.v1.DeleteRoleResponse;
import com.mysms.iam.v1.GetRolePermissionRequest;
import com.mysms.iam.v1.GetRolePermissionResponse;
import com.mysms.iam.v1.GetRoleRequest;
import com.mysms.iam.v1.GetRoleResponse;
import com.mysms.iam.v1.ListRolePermissionsRequest;
import com.mysms.iam.v1.ListRolePermissionsResponse;
import com.mysms.iam.v1.ListRolesRequest;
import com.mysms.iam.v1.ListRolesResponse;
import com.mysms.iam.v1.RolePermissionUpdate;
import com.mysms.iam.v1.RoleServiceGrpc;
import com.mysms.iam.v1.RoleUpdate;
import com.mysms.iam.v1.UpdateRolePermissionResponse;
import com.mysms.iam.v1.UpdateRoleResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of iam.RoleService: roles and their permissions, delegated to the same business service as REST,
 * which owns the rules against privilege escalation.
 */
@GrpcService
public class RoleResolver extends RoleServiceGrpc.RoleServiceImplBase {

    private final RoleService service;
    private final ProtoBridge bridge;

    public RoleResolver(RoleService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listRoles(ListRolesRequest request, StreamObserver<ListRolesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Role> rows = service.listRoles(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListRolesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRole(GetRoleRequest request, StreamObserver<GetRoleResponse> out) {
        Role found = service.getRole(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRoleResponse.newBuilder(), "role", found).build());
    }

    @Override
    public void createRole(CreateRoleRequest request, StreamObserver<CreateRoleResponse> out) {
        Role created = service.createRole(bridge.body(request.getRole(), Role.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRoleResponse.newBuilder(), "role", created).build());
    }

    @Override
    public void updateRole(RoleUpdate request, StreamObserver<UpdateRoleResponse> out) {
        Role updated = service.updateRole(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                RoleProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRoleResponse.newBuilder(), "role", updated).build());
    }

    @Override
    public void deleteRole(DeleteRoleRequest request, StreamObserver<DeleteRoleResponse> out) {
        service.deleteRole(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRoleResponse.getDefaultInstance());
    }

    @Override
    public void listRolePermissions(ListRolePermissionsRequest request,
            StreamObserver<ListRolePermissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RolePermission> rows = service.listRolePermissions(bridge.id(request.getRoleId(), "roleId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRolePermissionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRolePermission(GetRolePermissionRequest request, StreamObserver<GetRolePermissionResponse> out) {
        RolePermission found = service.getRolePermission(bridge.id(request.getRoleId(), "roleId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRolePermissionResponse.newBuilder(), "role_permission", found).build());
    }

    @Override
    public void createRolePermission(CreateRolePermissionRequest request,
            StreamObserver<CreateRolePermissionResponse> out) {
        RolePermission created = service.createRolePermission(bridge.id(request.getRoleId(), "roleId"),
                bridge.body(request.getRolePermission(), RolePermission.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRolePermissionResponse.newBuilder(), "role_permission", created)
                .build());
    }

    @Override
    public void updateRolePermission(RolePermissionUpdate request,
            StreamObserver<UpdateRolePermissionResponse> out) {
        RolePermission updated = service.updateRolePermission(
                bridge.id(request.getRoleId(), "roleId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                RolePermissionProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRolePermissionResponse.newBuilder(), "role_permission", updated)
                .build());
    }

    @Override
    public void deleteRolePermission(DeleteRolePermissionRequest request,
            StreamObserver<DeleteRolePermissionResponse> out) {
        service.deleteRolePermission(bridge.id(request.getRoleId(), "roleId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRolePermissionResponse.getDefaultInstance());
    }
}
