package com.mysms.master.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.master.api.model.Currency;
import com.mysms.master.api.model.CurrencyUpdate;
import com.mysms.master.business.CurrencyService;
import com.mysms.master.v1.CreateCurrencyRequest;
import com.mysms.master.v1.CreateCurrencyResponse;
import com.mysms.master.v1.CurrencyServiceGrpc;
import com.mysms.master.v1.DeleteCurrencyRequest;
import com.mysms.master.v1.DeleteCurrencyResponse;
import com.mysms.master.v1.GetCurrencyRequest;
import com.mysms.master.v1.GetCurrencyResponse;
import com.mysms.master.v1.ListCurrenciesRequest;
import com.mysms.master.v1.ListCurrenciesResponse;
import com.mysms.master.v1.UpdateCurrencyRequest;
import com.mysms.master.v1.UpdateCurrencyResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of master.CurrencyService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CurrencyResolver extends CurrencyServiceGrpc.CurrencyServiceImplBase {

    private final CurrencyService service;
    private final ProtoBridge bridge;

    public CurrencyResolver(CurrencyService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listCurrencies(ListCurrenciesRequest request,
            StreamObserver<ListCurrenciesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Currency> rows = service.listCurrencies(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCurrenciesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCurrency(GetCurrencyRequest request, StreamObserver<GetCurrencyResponse> out) {
        Currency found = service.getCurrency(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCurrencyResponse.newBuilder(), "currency", found).build());
    }

    @Override
    public void createCurrency(CreateCurrencyRequest request, StreamObserver<CreateCurrencyResponse> out) {
        Currency created = service.createCurrency(
                bridge.body(request.getCurrency(), Currency.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCurrencyResponse.newBuilder(), "currency", created).build());
    }

    @Override
    public void updateCurrency(UpdateCurrencyRequest request, StreamObserver<UpdateCurrencyResponse> out) {
        Currency updated = service.updateCurrency(
                bridge.id(request.getCurrency().getId(), "id"),
                bridge.requiredIfMatch(request.getCurrency().getEtag()),
                bridge.changes(request.getCurrency(), request.getUpdateMask(), CurrencyUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCurrencyResponse.newBuilder(), "currency", updated).build());
    }

    @Override
    public void deleteCurrency(DeleteCurrencyRequest request, StreamObserver<DeleteCurrencyResponse> out) {
        service.deleteCurrency(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCurrencyResponse.getDefaultInstance());
    }
}
