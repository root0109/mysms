package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.Rubric;
import com.mysms.lms.api.model.RubricCell;
import com.mysms.lms.api.model.RubricCellUpdate;
import com.mysms.lms.api.model.RubricCriterion;
import com.mysms.lms.api.model.RubricCriterionUpdate;
import com.mysms.lms.api.model.RubricLevel;
import com.mysms.lms.api.model.RubricLevelUpdate;
import com.mysms.lms.api.model.RubricUpdate;
import com.mysms.lms.business.RubricService;
import com.mysms.lms.v1.CreateRubricCellRequest;
import com.mysms.lms.v1.CreateRubricCellResponse;
import com.mysms.lms.v1.CreateRubricCriterionRequest;
import com.mysms.lms.v1.CreateRubricCriterionResponse;
import com.mysms.lms.v1.CreateRubricLevelRequest;
import com.mysms.lms.v1.CreateRubricLevelResponse;
import com.mysms.lms.v1.CreateRubricRequest;
import com.mysms.lms.v1.CreateRubricResponse;
import com.mysms.lms.v1.DeleteRubricCellRequest;
import com.mysms.lms.v1.DeleteRubricCellResponse;
import com.mysms.lms.v1.DeleteRubricCriterionRequest;
import com.mysms.lms.v1.DeleteRubricCriterionResponse;
import com.mysms.lms.v1.DeleteRubricLevelRequest;
import com.mysms.lms.v1.DeleteRubricLevelResponse;
import com.mysms.lms.v1.DeleteRubricRequest;
import com.mysms.lms.v1.DeleteRubricResponse;
import com.mysms.lms.v1.GetRubricCellRequest;
import com.mysms.lms.v1.GetRubricCellResponse;
import com.mysms.lms.v1.GetRubricCriterionRequest;
import com.mysms.lms.v1.GetRubricCriterionResponse;
import com.mysms.lms.v1.GetRubricLevelRequest;
import com.mysms.lms.v1.GetRubricLevelResponse;
import com.mysms.lms.v1.GetRubricRequest;
import com.mysms.lms.v1.GetRubricResponse;
import com.mysms.lms.v1.ListRubricCellsRequest;
import com.mysms.lms.v1.ListRubricCellsResponse;
import com.mysms.lms.v1.ListRubricCriteriaRequest;
import com.mysms.lms.v1.ListRubricCriteriaResponse;
import com.mysms.lms.v1.ListRubricLevelsRequest;
import com.mysms.lms.v1.ListRubricLevelsResponse;
import com.mysms.lms.v1.ListRubricsRequest;
import com.mysms.lms.v1.ListRubricsResponse;
import com.mysms.lms.v1.RubricServiceGrpc;
import com.mysms.lms.v1.UpdateRubricCellRequest;
import com.mysms.lms.v1.UpdateRubricCellResponse;
import com.mysms.lms.v1.UpdateRubricCriterionRequest;
import com.mysms.lms.v1.UpdateRubricCriterionResponse;
import com.mysms.lms.v1.UpdateRubricLevelRequest;
import com.mysms.lms.v1.UpdateRubricLevelResponse;
import com.mysms.lms.v1.UpdateRubricRequest;
import com.mysms.lms.v1.UpdateRubricResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.RubricService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class RubricResolver extends RubricServiceGrpc.RubricServiceImplBase {

    private final RubricService service;
    private final ProtoBridge bridge;

    public RubricResolver(RubricService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listRubrics(ListRubricsRequest request, StreamObserver<ListRubricsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Rubric> rows = service.listRubrics(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListRubricsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRubric(GetRubricRequest request, StreamObserver<GetRubricResponse> out) {
        Rubric found = service.getRubric(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRubricResponse.newBuilder(), "rubric", found).build());
    }

    @Override
    public void createRubric(CreateRubricRequest request, StreamObserver<CreateRubricResponse> out) {
        Rubric created = service.createRubric(bridge.body(request.getRubric(), Rubric.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRubricResponse.newBuilder(), "rubric", created).build());
    }

    @Override
    public void updateRubric(UpdateRubricRequest request, StreamObserver<UpdateRubricResponse> out) {
        Rubric updated = service.updateRubric(bridge.id(request.getRubric().getId(), "id"),
                bridge.requiredIfMatch(request.getRubric().getEtag()),
                bridge.changes(request.getRubric(), request.getUpdateMask(), RubricUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRubricResponse.newBuilder(), "rubric", updated).build());
    }

    @Override
    public void deleteRubric(DeleteRubricRequest request, StreamObserver<DeleteRubricResponse> out) {
        service.deleteRubric(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRubricResponse.getDefaultInstance());
    }

    @Override
    public void listRubricCriteria(ListRubricCriteriaRequest request, StreamObserver<ListRubricCriteriaResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RubricCriterion> rows = service.listRubricCriterions(bridge.id(request.getRubricId(), "rubricId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRubricCriteriaResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRubricCriterion(GetRubricCriterionRequest request, StreamObserver<GetRubricCriterionResponse> out) {
        RubricCriterion found = service.getRubricCriterion(bridge.id(request.getRubricId(), "rubricId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRubricCriterionResponse.newBuilder(), "rubric_criterion", found).build());
    }

    @Override
    public void createRubricCriterion(CreateRubricCriterionRequest request, StreamObserver<CreateRubricCriterionResponse> out) {
        RubricCriterion created = service.createRubricCriterion(bridge.id(request.getRubricId(), "rubricId"),
                bridge.body(request.getRubricCriterion(), RubricCriterion.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRubricCriterionResponse.newBuilder(), "rubric_criterion", created).build());
    }

    @Override
    public void updateRubricCriterion(UpdateRubricCriterionRequest request, StreamObserver<UpdateRubricCriterionResponse> out) {
        RubricCriterion updated = service.updateRubricCriterion(bridge.id(request.getRubricId(), "rubricId"),
                bridge.id(request.getRubricCriterion().getId(), "id"),
                bridge.requiredIfMatch(request.getRubricCriterion().getEtag()),
                bridge.changes(request.getRubricCriterion(), request.getUpdateMask(), RubricCriterionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRubricCriterionResponse.newBuilder(), "rubric_criterion", updated).build());
    }

    @Override
    public void deleteRubricCriterion(DeleteRubricCriterionRequest request, StreamObserver<DeleteRubricCriterionResponse> out) {
        service.deleteRubricCriterion(bridge.id(request.getRubricId(), "rubricId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRubricCriterionResponse.getDefaultInstance());
    }

    @Override
    public void listRubricLevels(ListRubricLevelsRequest request, StreamObserver<ListRubricLevelsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RubricLevel> rows = service.listRubricLevels(bridge.id(request.getRubricId(), "rubricId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRubricLevelsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRubricLevel(GetRubricLevelRequest request, StreamObserver<GetRubricLevelResponse> out) {
        RubricLevel found = service.getRubricLevel(bridge.id(request.getRubricId(), "rubricId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRubricLevelResponse.newBuilder(), "rubric_level", found).build());
    }

    @Override
    public void createRubricLevel(CreateRubricLevelRequest request, StreamObserver<CreateRubricLevelResponse> out) {
        RubricLevel created = service.createRubricLevel(bridge.id(request.getRubricId(), "rubricId"),
                bridge.body(request.getRubricLevel(), RubricLevel.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRubricLevelResponse.newBuilder(), "rubric_level", created).build());
    }

    @Override
    public void updateRubricLevel(UpdateRubricLevelRequest request, StreamObserver<UpdateRubricLevelResponse> out) {
        RubricLevel updated = service.updateRubricLevel(bridge.id(request.getRubricId(), "rubricId"),
                bridge.id(request.getRubricLevel().getId(), "id"), bridge.requiredIfMatch(request.getRubricLevel().getEtag()),
                bridge.changes(request.getRubricLevel(), request.getUpdateMask(), RubricLevelUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRubricLevelResponse.newBuilder(), "rubric_level", updated).build());
    }

    @Override
    public void deleteRubricLevel(DeleteRubricLevelRequest request, StreamObserver<DeleteRubricLevelResponse> out) {
        service.deleteRubricLevel(bridge.id(request.getRubricId(), "rubricId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRubricLevelResponse.getDefaultInstance());
    }

    @Override
    public void listRubricCells(ListRubricCellsRequest request, StreamObserver<ListRubricCellsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RubricCell> rows = service.listRubricCells(bridge.id(request.getRubricId(), "rubricId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRubricCellsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRubricCell(GetRubricCellRequest request, StreamObserver<GetRubricCellResponse> out) {
        RubricCell found = service.getRubricCell(bridge.id(request.getRubricId(), "rubricId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRubricCellResponse.newBuilder(), "rubric_cell", found).build());
    }

    @Override
    public void createRubricCell(CreateRubricCellRequest request, StreamObserver<CreateRubricCellResponse> out) {
        RubricCell created = service.createRubricCell(bridge.id(request.getRubricId(), "rubricId"),
                bridge.body(request.getRubricCell(), RubricCell.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRubricCellResponse.newBuilder(), "rubric_cell", created).build());
    }

    @Override
    public void updateRubricCell(UpdateRubricCellRequest request, StreamObserver<UpdateRubricCellResponse> out) {
        RubricCell updated = service.updateRubricCell(bridge.id(request.getRubricId(), "rubricId"),
                bridge.id(request.getRubricCell().getId(), "id"), bridge.requiredIfMatch(request.getRubricCell().getEtag()),
                bridge.changes(request.getRubricCell(), request.getUpdateMask(), RubricCellUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRubricCellResponse.newBuilder(), "rubric_cell", updated).build());
    }

    @Override
    public void deleteRubricCell(DeleteRubricCellRequest request, StreamObserver<DeleteRubricCellResponse> out) {
        service.deleteRubricCell(bridge.id(request.getRubricId(), "rubricId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRubricCellResponse.getDefaultInstance());
    }
}
