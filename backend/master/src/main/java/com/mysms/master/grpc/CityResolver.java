package com.mysms.master.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.master.api.model.City;
import com.mysms.master.api.model.CityUpdate;
import com.mysms.master.business.CityService;
import com.mysms.master.v1.CityServiceGrpc;
import com.mysms.master.v1.CreateCityRequest;
import com.mysms.master.v1.CreateCityResponse;
import com.mysms.master.v1.DeleteCityRequest;
import com.mysms.master.v1.DeleteCityResponse;
import com.mysms.master.v1.GetCityRequest;
import com.mysms.master.v1.GetCityResponse;
import com.mysms.master.v1.ListCitiesRequest;
import com.mysms.master.v1.ListCitiesResponse;
import com.mysms.master.v1.UpdateCityRequest;
import com.mysms.master.v1.UpdateCityResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of master.CityService: delegates every RPC to the business service REST uses, so the rules exist once.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CityResolver extends CityServiceGrpc.CityServiceImplBase {

    private final CityService service;
    private final ProtoBridge bridge;

    public CityResolver(CityService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listCities(ListCitiesRequest request,
            StreamObserver<ListCitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<City> rows = service.listCities(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCity(GetCityRequest request, StreamObserver<GetCityResponse> out) {
        City found = service.getCity(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCityResponse.newBuilder(), "city", found).build());
    }

    @Override
    public void createCity(CreateCityRequest request, StreamObserver<CreateCityResponse> out) {
        City created = service.createCity(
                bridge.body(request.getCity(), City.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCityResponse.newBuilder(), "city", created).build());
    }

    @Override
    public void updateCity(UpdateCityRequest request, StreamObserver<UpdateCityResponse> out) {
        City updated = service.updateCity(
                bridge.id(request.getCity().getId(), "id"),
                bridge.requiredIfMatch(request.getCity().getEtag()),
                bridge.changes(request.getCity(), request.getUpdateMask(), CityUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCityResponse.newBuilder(), "city", updated).build());
    }

    @Override
    public void deleteCity(DeleteCityRequest request, StreamObserver<DeleteCityResponse> out) {
        service.deleteCity(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCityResponse.getDefaultInstance());
    }
}
