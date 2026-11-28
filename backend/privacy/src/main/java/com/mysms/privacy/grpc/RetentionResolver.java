package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.RetentionPolicy;
import com.mysms.privacy.api.model.RetentionRun;
import com.mysms.privacy.business.RetentionPolicyService;
import com.mysms.privacy.business.RetentionRunService;
import com.mysms.privacy.v1.GetRetentionPolicyRequest;
import com.mysms.privacy.v1.GetRetentionPolicyResponse;
import com.mysms.privacy.v1.ListRetentionPoliciesRequest;
import com.mysms.privacy.v1.ListRetentionPoliciesResponse;
import com.mysms.privacy.v1.ListRetentionRunsRequest;
import com.mysms.privacy.v1.ListRetentionRunsResponse;
import com.mysms.privacy.v1.RetentionPolicyUpdate;
import com.mysms.privacy.v1.RetentionServiceGrpc;
import com.mysms.privacy.v1.UpdateRetentionPolicyResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.RetentionService: delegates every RPC to the same business services as REST (the
 * policies' and the runs'). Authentication, permission and error mapping happen at the gRPC edge.
 */
@GrpcService
public class RetentionResolver extends RetentionServiceGrpc.RetentionServiceImplBase {

    private static final String RETENTION_POLICY = "retention_policy";

    private final RetentionPolicyService policies;
    private final RetentionRunService runs;
    private final ProtoBridge bridge;

    public RetentionResolver(RetentionPolicyService policies, RetentionRunService runs, ProtoBridge bridge) {
        this.policies = policies;
        this.runs = runs;
        this.bridge = bridge;
    }

    @Override
    public void listRetentionPolicies(ListRetentionPoliciesRequest request,
            StreamObserver<ListRetentionPoliciesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RetentionPolicy> rows = policies.listRetentionPolicies(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRetentionPoliciesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRetentionPolicy(GetRetentionPolicyRequest request, StreamObserver<GetRetentionPolicyResponse> out) {
        RetentionPolicy found = policies.getRetentionPolicy(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRetentionPolicyResponse.newBuilder(), RETENTION_POLICY, found).build());
    }

    @Override
    public void updateRetentionPolicy(RetentionPolicyUpdate request,
            StreamObserver<UpdateRetentionPolicyResponse> out) {
        RetentionPolicy updated = policies.updateRetentionPolicy(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                RetentionPolicyProtoBridge.changes(bridge, request));
        ProtoBridge.send(out, bridge.reply(UpdateRetentionPolicyResponse.newBuilder(), RETENTION_POLICY, updated)
                .build());
    }

    @Override
    public void listRetentionRuns(ListRetentionRunsRequest request, StreamObserver<ListRetentionRunsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RetentionRun> rows = runs.listRetentionRuns(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRetentionRunsResponse.newBuilder(), rows).build());
    }
}
