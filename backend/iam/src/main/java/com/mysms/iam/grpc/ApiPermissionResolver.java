package com.mysms.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.iam.api.model.ApiPermission;
import com.mysms.iam.business.ApiPermissionService;
import com.mysms.iam.v1.ApiPermissionServiceGrpc;
import com.mysms.iam.v1.ListApiPermissionsRequest;
import com.mysms.iam.v1.ListApiPermissionsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of iam.ApiPermissionService: delegates to the business service REST uses, so the rules exist once.
 * Authentication, the platform-staff scheme, the permission and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ApiPermissionResolver extends ApiPermissionServiceGrpc.ApiPermissionServiceImplBase {

    private final ApiPermissionService service;
    private final ProtoBridge bridge;

    public ApiPermissionResolver(ApiPermissionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listApiPermissions(ListApiPermissionsRequest request, StreamObserver<ListApiPermissionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ApiPermission> rows = service.list(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListApiPermissionsResponse.newBuilder(), rows).build());
    }
}
