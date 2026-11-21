package com.mysms.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.iam.business.AuthzService;
import com.mysms.iam.v1.GetRolePermissionsRequest;
import com.mysms.iam.v1.GetRolePermissionsResponse;
import com.mysms.iam.v1.IamAuthzServiceGrpc;
import com.mysms.iam.v1.RolePermissionCodes;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of iam.IamAuthzService (iam_authz.proto): the role data every service's permissions filter caches.
 * It mirrors no REST operation, so it is in no operation table; the gRPC edge admits it on a tenant user's own valid
 * token alone, and it answers only about that token's tenant (checked by the service) and roles.
 */
@GrpcService
public class IamAuthzResolver extends IamAuthzServiceGrpc.IamAuthzServiceImplBase {

    private final AuthzService service;
    private final ProtoBridge bridge;

    public IamAuthzResolver(AuthzService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void getRolePermissions(GetRolePermissionsRequest request,
            StreamObserver<GetRolePermissionsResponse> out) {
        List<UUID> roleIds = new ArrayList<>(request.getRoleIdsCount());
        for (int i = 0; i < request.getRoleIdsCount(); i++) {
            roleIds.add(bridge.id(request.getRoleIds(i), "role_ids[" + i + "]"));
        }
        AuthzService.RoleAuthz resolved =
                service.callerRolePermissions(bridge.id(request.getTenantId(), "tenant_id"), roleIds);
        GetRolePermissionsResponse.Builder reply =
                GetRolePermissionsResponse.newBuilder().setAuthzVersion(resolved.authzVersion())
                        .addAllCommonPermissionCodes(resolved.commonCodes());
        for (AuthzService.RoleCodes role : resolved.roles()) {
            reply.addRoles(RolePermissionCodes.newBuilder().setRoleId(role.roleId().toString())
                    .addAllPermissionCodes(role.permissionCodes()));
        }
        ProtoBridge.send(out, reply.build());
    }
}
