package com.mysms.academics.grpc;

import com.mysms.academics.api.model.CurriculumUnit;
import com.mysms.academics.api.model.CurriculumUnitUpdate;
import com.mysms.academics.business.CurriculumUnitService;
import com.mysms.academics.v1.CreateCurriculumUnitRequest;
import com.mysms.academics.v1.CreateCurriculumUnitResponse;
import com.mysms.academics.v1.CurriculumUnitServiceGrpc;
import com.mysms.academics.v1.DeleteCurriculumUnitRequest;
import com.mysms.academics.v1.DeleteCurriculumUnitResponse;
import com.mysms.academics.v1.GetCurriculumUnitRequest;
import com.mysms.academics.v1.GetCurriculumUnitResponse;
import com.mysms.academics.v1.ListCurriculumUnitsRequest;
import com.mysms.academics.v1.ListCurriculumUnitsResponse;
import com.mysms.academics.v1.UpdateCurriculumUnitRequest;
import com.mysms.academics.v1.UpdateCurriculumUnitResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.CurriculumUnitService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CurriculumUnitResolver extends CurriculumUnitServiceGrpc.CurriculumUnitServiceImplBase {

    private final CurriculumUnitService service;
    private final ProtoBridge bridge;

    public CurriculumUnitResolver(CurriculumUnitService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCurriculumUnits(ListCurriculumUnitsRequest request, StreamObserver<ListCurriculumUnitsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CurriculumUnit> rows = service.listCurriculumUnits(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCurriculumUnitsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCurriculumUnit(GetCurriculumUnitRequest request, StreamObserver<GetCurriculumUnitResponse> out) {
        CurriculumUnit found = service.getCurriculumUnit(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCurriculumUnitResponse.newBuilder(), "curriculum_unit", found).build());
    }

    @Override
    public void createCurriculumUnit(CreateCurriculumUnitRequest request, StreamObserver<CreateCurriculumUnitResponse> out) {
        CurriculumUnit created = service.createCurriculumUnit(bridge.body(request.getCurriculumUnit(), CurriculumUnit.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCurriculumUnitResponse.newBuilder(), "curriculum_unit", created).build());
    }

    @Override
    public void updateCurriculumUnit(UpdateCurriculumUnitRequest request, StreamObserver<UpdateCurriculumUnitResponse> out) {
        CurriculumUnit updated = service.updateCurriculumUnit(bridge.id(request.getCurriculumUnit().getId(), "id"),
                bridge.requiredIfMatch(request.getCurriculumUnit().getEtag()),
                bridge.changes(request.getCurriculumUnit(), request.getUpdateMask(), CurriculumUnitUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCurriculumUnitResponse.newBuilder(), "curriculum_unit", updated).build());
    }

    @Override
    public void deleteCurriculumUnit(DeleteCurriculumUnitRequest request, StreamObserver<DeleteCurriculumUnitResponse> out) {
        service.deleteCurriculumUnit(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCurriculumUnitResponse.getDefaultInstance());
    }
}
