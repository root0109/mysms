package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.ReleaseRule;
import com.mysms.lms.api.model.ReleaseRuleCondition;
import com.mysms.lms.api.model.ReleaseRuleConditionUpdate;
import com.mysms.lms.api.model.ReleaseRuleTarget;
import com.mysms.lms.api.model.ReleaseRuleTargetUpdate;
import com.mysms.lms.api.model.ReleaseRuleUpdate;
import com.mysms.lms.business.ReleaseRuleService;
import com.mysms.lms.v1.CopyReleaseRuleRequest;
import com.mysms.lms.v1.CopyReleaseRuleResponse;
import com.mysms.lms.v1.CreateReleaseRuleConditionRequest;
import com.mysms.lms.v1.CreateReleaseRuleConditionResponse;
import com.mysms.lms.v1.CreateReleaseRuleRequest;
import com.mysms.lms.v1.CreateReleaseRuleResponse;
import com.mysms.lms.v1.CreateReleaseRuleTargetRequest;
import com.mysms.lms.v1.CreateReleaseRuleTargetResponse;
import com.mysms.lms.v1.DeleteReleaseRuleConditionRequest;
import com.mysms.lms.v1.DeleteReleaseRuleConditionResponse;
import com.mysms.lms.v1.DeleteReleaseRuleRequest;
import com.mysms.lms.v1.DeleteReleaseRuleResponse;
import com.mysms.lms.v1.DeleteReleaseRuleTargetRequest;
import com.mysms.lms.v1.DeleteReleaseRuleTargetResponse;
import com.mysms.lms.v1.DisableReleaseRuleRequest;
import com.mysms.lms.v1.DisableReleaseRuleResponse;
import com.mysms.lms.v1.EnableReleaseRuleRequest;
import com.mysms.lms.v1.EnableReleaseRuleResponse;
import com.mysms.lms.v1.GetReleaseRuleConditionRequest;
import com.mysms.lms.v1.GetReleaseRuleConditionResponse;
import com.mysms.lms.v1.GetReleaseRuleRequest;
import com.mysms.lms.v1.GetReleaseRuleResponse;
import com.mysms.lms.v1.GetReleaseRuleTargetRequest;
import com.mysms.lms.v1.GetReleaseRuleTargetResponse;
import com.mysms.lms.v1.ListReleaseRuleConditionsRequest;
import com.mysms.lms.v1.ListReleaseRuleConditionsResponse;
import com.mysms.lms.v1.ListReleaseRuleTargetsRequest;
import com.mysms.lms.v1.ListReleaseRuleTargetsResponse;
import com.mysms.lms.v1.ListReleaseRulesRequest;
import com.mysms.lms.v1.ListReleaseRulesResponse;
import com.mysms.lms.v1.ReleaseRuleServiceGrpc;
import com.mysms.lms.v1.UpdateReleaseRuleConditionRequest;
import com.mysms.lms.v1.UpdateReleaseRuleConditionResponse;
import com.mysms.lms.v1.UpdateReleaseRuleRequest;
import com.mysms.lms.v1.UpdateReleaseRuleResponse;
import com.mysms.lms.v1.UpdateReleaseRuleTargetRequest;
import com.mysms.lms.v1.UpdateReleaseRuleTargetResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ReleaseRuleService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ReleaseRuleResolver extends ReleaseRuleServiceGrpc.ReleaseRuleServiceImplBase {

    private final ReleaseRuleService service;
    private final ProtoBridge bridge;

    public ReleaseRuleResolver(ReleaseRuleService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listReleaseRules(ListReleaseRulesRequest request, StreamObserver<ListReleaseRulesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ReleaseRule> rows = service.listReleaseRules(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListReleaseRulesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getReleaseRule(GetReleaseRuleRequest request, StreamObserver<GetReleaseRuleResponse> out) {
        ReleaseRule found = service.getReleaseRule(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetReleaseRuleResponse.newBuilder(), "release_rule", found).build());
    }

    @Override
    public void createReleaseRule(CreateReleaseRuleRequest request, StreamObserver<CreateReleaseRuleResponse> out) {
        ReleaseRule created = service.createReleaseRule(
                bridge.body(request.getReleaseRule(), ReleaseRule.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateReleaseRuleResponse.newBuilder(), "release_rule", created).build());
    }

    @Override
    public void updateReleaseRule(UpdateReleaseRuleRequest request, StreamObserver<UpdateReleaseRuleResponse> out) {
        ReleaseRule updated = service.updateReleaseRule(
                bridge.id(request.getReleaseRule().getId(), "id"),
                bridge.requiredIfMatch(request.getReleaseRule().getEtag()),
                bridge.changes(request.getReleaseRule(), request.getUpdateMask(), ReleaseRuleUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateReleaseRuleResponse.newBuilder(), "release_rule", updated).build());
    }

    @Override
    public void deleteReleaseRule(DeleteReleaseRuleRequest request, StreamObserver<DeleteReleaseRuleResponse> out) {
        service.deleteReleaseRule(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteReleaseRuleResponse.getDefaultInstance());
    }

    @Override
    public void listReleaseRuleConditions(ListReleaseRuleConditionsRequest request,
            StreamObserver<ListReleaseRuleConditionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ReleaseRuleCondition> rows = service.listReleaseRuleConditions(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListReleaseRuleConditionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getReleaseRuleCondition(GetReleaseRuleConditionRequest request,
            StreamObserver<GetReleaseRuleConditionResponse> out) {
        ReleaseRuleCondition found = service.getReleaseRuleCondition(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetReleaseRuleConditionResponse.newBuilder(), "release_rule_condition", found).build());
    }

    @Override
    public void createReleaseRuleCondition(CreateReleaseRuleConditionRequest request,
            StreamObserver<CreateReleaseRuleConditionResponse> out) {
        ReleaseRuleCondition created = service.createReleaseRuleCondition(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"),
                bridge.body(request.getReleaseRuleCondition(), ReleaseRuleCondition.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateReleaseRuleConditionResponse.newBuilder(), "release_rule_condition", created).build());
    }

    @Override
    public void updateReleaseRuleCondition(UpdateReleaseRuleConditionRequest request,
            StreamObserver<UpdateReleaseRuleConditionResponse> out) {
        ReleaseRuleCondition updated = service.updateReleaseRuleCondition(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"),
                bridge.id(request.getReleaseRuleCondition().getId(), "id"),
                bridge.requiredIfMatch(request.getReleaseRuleCondition().getEtag()),
                bridge.changes(request.getReleaseRuleCondition(), request.getUpdateMask(),
                        ReleaseRuleConditionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateReleaseRuleConditionResponse.newBuilder(), "release_rule_condition", updated).build());
    }

    @Override
    public void deleteReleaseRuleCondition(DeleteReleaseRuleConditionRequest request,
            StreamObserver<DeleteReleaseRuleConditionResponse> out) {
        service.deleteReleaseRuleCondition(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteReleaseRuleConditionResponse.getDefaultInstance());
    }

    @Override
    public void listReleaseRuleTargets(ListReleaseRuleTargetsRequest request,
            StreamObserver<ListReleaseRuleTargetsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ReleaseRuleTarget> rows = service.listReleaseRuleTargets(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListReleaseRuleTargetsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getReleaseRuleTarget(GetReleaseRuleTargetRequest request,
            StreamObserver<GetReleaseRuleTargetResponse> out) {
        ReleaseRuleTarget found = service.getReleaseRuleTarget(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetReleaseRuleTargetResponse.newBuilder(), "release_rule_target", found).build());
    }

    @Override
    public void createReleaseRuleTarget(CreateReleaseRuleTargetRequest request,
            StreamObserver<CreateReleaseRuleTargetResponse> out) {
        ReleaseRuleTarget created = service.createReleaseRuleTarget(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"),
                bridge.body(request.getReleaseRuleTarget(), ReleaseRuleTarget.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateReleaseRuleTargetResponse.newBuilder(), "release_rule_target", created).build());
    }

    @Override
    public void updateReleaseRuleTarget(UpdateReleaseRuleTargetRequest request,
            StreamObserver<UpdateReleaseRuleTargetResponse> out) {
        ReleaseRuleTarget updated = service.updateReleaseRuleTarget(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"),
                bridge.id(request.getReleaseRuleTarget().getId(), "id"),
                bridge.requiredIfMatch(request.getReleaseRuleTarget().getEtag()),
                bridge.changes(request.getReleaseRuleTarget(), request.getUpdateMask(), ReleaseRuleTargetUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateReleaseRuleTargetResponse.newBuilder(), "release_rule_target", updated).build());
    }

    @Override
    public void deleteReleaseRuleTarget(DeleteReleaseRuleTargetRequest request,
            StreamObserver<DeleteReleaseRuleTargetResponse> out) {
        service.deleteReleaseRuleTarget(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteReleaseRuleTargetResponse.getDefaultInstance());
    }

    @Override
    public void enableReleaseRule(EnableReleaseRuleRequest request, StreamObserver<EnableReleaseRuleResponse> out) {
        ReleaseRule changed = service.enableReleaseRule(bridge.id(request.getReleaseRuleId(), "releaseRuleId"),
                bridge.body(request, com.mysms.lms.api.model.EnableReleaseRuleRequest.class,
                        "release_rule_id", "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(EnableReleaseRuleResponse.newBuilder(), "release_rule", changed).build());
    }

    @Override
    public void disableReleaseRule(DisableReleaseRuleRequest request, StreamObserver<DisableReleaseRuleResponse> out) {
        ReleaseRule changed = service.disableReleaseRule(bridge.id(request.getReleaseRuleId(), "releaseRuleId"),
                bridge.body(request, com.mysms.lms.api.model.DisableReleaseRuleRequest.class,
                        "release_rule_id", "request_id").getEtag());
        ProtoBridge.send(out, bridge.reply(DisableReleaseRuleResponse.newBuilder(), "release_rule", changed).build());
    }

    @Override
    public void copyReleaseRule(CopyReleaseRuleRequest request, StreamObserver<CopyReleaseRuleResponse> out) {
        com.mysms.lms.api.model.ReleaseRuleList copies = service.copyReleaseRule(
                bridge.id(request.getReleaseRuleId(), "releaseRuleId"),
                bridge.body(request, com.mysms.lms.api.model.CopyReleaseRuleRequest.class, "release_rule_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CopyReleaseRuleResponse.newBuilder(), "items", copies.getItems()).build());
    }
}
