package com.mysms.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.people.api.model.Skill;
import com.mysms.people.api.model.SkillUpdate;
import com.mysms.people.business.SkillService;
import com.mysms.people.v1.CreateSkillRequest;
import com.mysms.people.v1.CreateSkillResponse;
import com.mysms.people.v1.DeleteSkillRequest;
import com.mysms.people.v1.DeleteSkillResponse;
import com.mysms.people.v1.GetSkillRequest;
import com.mysms.people.v1.GetSkillResponse;
import com.mysms.people.v1.ListSkillsRequest;
import com.mysms.people.v1.ListSkillsResponse;
import com.mysms.people.v1.SkillServiceGrpc;
import com.mysms.people.v1.UpdateSkillRequest;
import com.mysms.people.v1.UpdateSkillResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of people.SkillService: delegates every RPC to the business service REST uses, so the rules exist once.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SkillResolver extends SkillServiceGrpc.SkillServiceImplBase {

    private final SkillService service;
    private final ProtoBridge bridge;

    public SkillResolver(SkillService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSkills(ListSkillsRequest request, StreamObserver<ListSkillsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Skill> rows = service.listSkills(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSkillsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSkill(GetSkillRequest request, StreamObserver<GetSkillResponse> out) {
        Skill found = service.getSkill(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSkillResponse.newBuilder(), "skill", found).build());
    }

    @Override
    public void createSkill(CreateSkillRequest request, StreamObserver<CreateSkillResponse> out) {
        Skill created = service.createSkill(bridge.body(request.getSkill(), Skill.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSkillResponse.newBuilder(), "skill", created).build());
    }

    @Override
    public void updateSkill(UpdateSkillRequest request, StreamObserver<UpdateSkillResponse> out) {
        Skill updated = service.updateSkill(
                bridge.id(request.getSkill().getId(), "id"), bridge.requiredIfMatch(request.getSkill().getEtag()),
                bridge.changes(request.getSkill(), request.getUpdateMask(), SkillUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSkillResponse.newBuilder(), "skill", updated).build());
    }

    @Override
    public void deleteSkill(DeleteSkillRequest request, StreamObserver<DeleteSkillResponse> out) {
        service.deleteSkill(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSkillResponse.getDefaultInstance());
    }
}
