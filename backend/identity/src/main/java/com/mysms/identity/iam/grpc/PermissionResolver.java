package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.identity.iam.api.model.Permission;
import com.mysms.identity.iam.business.PermissionCatalogService;
import com.mysms.iam.v1.GetPermissionRequest;
import com.mysms.iam.v1.GetPermissionResponse;
import com.mysms.iam.v1.ListPermissionsRequest;
import com.mysms.iam.v1.ListPermissionsResponse;
import com.mysms.iam.v1.PermissionServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of iam.PermissionService: the read-only permission catalogue, delegated to the same business service
 * as REST. Authentication, permission and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PermissionResolver extends PermissionServiceGrpc.PermissionServiceImplBase {

    private final PermissionCatalogService service;
    private final ProtoBridge bridge;

    public PermissionResolver(PermissionCatalogService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPermissions(ListPermissionsRequest request, StreamObserver<ListPermissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Permission> rows = service.list(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListPermissionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPermission(GetPermissionRequest request, StreamObserver<GetPermissionResponse> out) {
        Permission found = service.get(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPermissionResponse.newBuilder(), "permission", found).build());
    }
}
