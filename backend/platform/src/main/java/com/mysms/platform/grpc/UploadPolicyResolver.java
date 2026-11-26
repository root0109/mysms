package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.UploadPolicy;
import com.mysms.platform.api.model.UploadPolicyUpdate;
import com.mysms.platform.business.UploadPolicyService;
import com.mysms.platform.v1.CreateUploadPolicyRequest;
import com.mysms.platform.v1.CreateUploadPolicyResponse;
import com.mysms.platform.v1.DeleteUploadPolicyRequest;
import com.mysms.platform.v1.DeleteUploadPolicyResponse;
import com.mysms.platform.v1.GetUploadPolicyRequest;
import com.mysms.platform.v1.GetUploadPolicyResponse;
import com.mysms.platform.v1.ListUploadPoliciesRequest;
import com.mysms.platform.v1.ListUploadPoliciesResponse;
import com.mysms.platform.v1.UpdateUploadPolicyRequest;
import com.mysms.platform.v1.UpdateUploadPolicyResponse;
import com.mysms.platform.v1.UploadPolicyServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.UploadPolicyService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class UploadPolicyResolver extends UploadPolicyServiceGrpc.UploadPolicyServiceImplBase {

    private final UploadPolicyService service;
    private final ProtoBridge bridge;

    public UploadPolicyResolver(UploadPolicyService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listUploadPolicies(ListUploadPoliciesRequest request, StreamObserver<ListUploadPoliciesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<UploadPolicy> rows = service.listUploadPolicys(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListUploadPoliciesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getUploadPolicy(GetUploadPolicyRequest request, StreamObserver<GetUploadPolicyResponse> out) {
        UploadPolicy found = service.getUploadPolicy(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetUploadPolicyResponse.newBuilder(), "upload_policy", found).build());
    }

    @Override
    public void createUploadPolicy(CreateUploadPolicyRequest request, StreamObserver<CreateUploadPolicyResponse> out) {
        UploadPolicy created = service.createUploadPolicy(
                bridge.body(request.getUploadPolicy(), UploadPolicy.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateUploadPolicyResponse.newBuilder(), "upload_policy", created).build());
    }

    @Override
    public void updateUploadPolicy(UpdateUploadPolicyRequest request, StreamObserver<UpdateUploadPolicyResponse> out) {
        UploadPolicy updated = service.updateUploadPolicy(
                bridge.id(request.getUploadPolicy().getId(), "id"),
                bridge.requiredIfMatch(request.getUploadPolicy().getEtag()),
                bridge.changes(request.getUploadPolicy(), request.getUpdateMask(), UploadPolicyUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateUploadPolicyResponse.newBuilder(), "upload_policy", updated).build());
    }

    @Override
    public void deleteUploadPolicy(DeleteUploadPolicyRequest request, StreamObserver<DeleteUploadPolicyResponse> out) {
        service.deleteUploadPolicy(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteUploadPolicyResponse.getDefaultInstance());
    }
}
