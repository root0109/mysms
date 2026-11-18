package com.mysms.master.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.master.api.model.Country;
import com.mysms.master.api.model.CountryUpdate;
import com.mysms.master.business.CountryService;
import com.mysms.master.v1.CountryServiceGrpc;
import com.mysms.master.v1.CreateCountryRequest;
import com.mysms.master.v1.CreateCountryResponse;
import com.mysms.master.v1.DeleteCountryRequest;
import com.mysms.master.v1.DeleteCountryResponse;
import com.mysms.master.v1.GetCountryRequest;
import com.mysms.master.v1.GetCountryResponse;
import com.mysms.master.v1.ListCountriesRequest;
import com.mysms.master.v1.ListCountriesResponse;
import com.mysms.master.v1.UpdateCountryRequest;
import com.mysms.master.v1.UpdateCountryResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of master.CountryService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CountryResolver extends CountryServiceGrpc.CountryServiceImplBase {

    private final CountryService service;
    private final ProtoBridge bridge;

    public CountryResolver(CountryService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listCountries(ListCountriesRequest request,
            StreamObserver<ListCountriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Country> rows = service.listCountries(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCountriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCountry(GetCountryRequest request, StreamObserver<GetCountryResponse> out) {
        Country found = service.getCountry(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCountryResponse.newBuilder(), "country", found).build());
    }

    @Override
    public void createCountry(CreateCountryRequest request, StreamObserver<CreateCountryResponse> out) {
        Country created = service.createCountry(
                bridge.body(request.getCountry(), Country.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCountryResponse.newBuilder(), "country", created).build());
    }

    @Override
    public void updateCountry(UpdateCountryRequest request, StreamObserver<UpdateCountryResponse> out) {
        Country updated = service.updateCountry(
                bridge.id(request.getCountry().getId(), "id"),
                bridge.requiredIfMatch(request.getCountry().getEtag()),
                bridge.changes(request.getCountry(), request.getUpdateMask(), CountryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCountryResponse.newBuilder(), "country", updated).build());
    }

    @Override
    public void deleteCountry(DeleteCountryRequest request, StreamObserver<DeleteCountryResponse> out) {
        service.deleteCountry(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCountryResponse.getDefaultInstance());
    }
}
