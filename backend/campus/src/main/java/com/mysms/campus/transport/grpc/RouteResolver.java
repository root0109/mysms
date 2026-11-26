package com.mysms.campus.transport.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.campus.transport.api.model.Route;
import com.mysms.campus.transport.api.model.RouteStop;
import com.mysms.campus.transport.api.model.RouteStopUpdate;
import com.mysms.campus.transport.api.model.RouteUpdate;
import com.mysms.campus.transport.business.RouteService;
import com.mysms.transport.v1.CreateRouteRequest;
import com.mysms.transport.v1.CreateRouteResponse;
import com.mysms.transport.v1.CreateRouteStopRequest;
import com.mysms.transport.v1.CreateRouteStopResponse;
import com.mysms.transport.v1.DeleteRouteRequest;
import com.mysms.transport.v1.DeleteRouteResponse;
import com.mysms.transport.v1.DeleteRouteStopRequest;
import com.mysms.transport.v1.DeleteRouteStopResponse;
import com.mysms.transport.v1.GetRouteRequest;
import com.mysms.transport.v1.GetRouteResponse;
import com.mysms.transport.v1.GetRouteStopRequest;
import com.mysms.transport.v1.GetRouteStopResponse;
import com.mysms.transport.v1.ListRouteStopsRequest;
import com.mysms.transport.v1.ListRouteStopsResponse;
import com.mysms.transport.v1.ListRoutesRequest;
import com.mysms.transport.v1.ListRoutesResponse;
import com.mysms.transport.v1.RouteServiceGrpc;
import com.mysms.transport.v1.UpdateRouteRequest;
import com.mysms.transport.v1.UpdateRouteResponse;
import com.mysms.transport.v1.UpdateRouteStopRequest;
import com.mysms.transport.v1.UpdateRouteStopResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of transport.RouteService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class RouteResolver extends RouteServiceGrpc.RouteServiceImplBase {

    private final RouteService service;
    private final ProtoBridge bridge;

    public RouteResolver(RouteService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listRoutes(ListRoutesRequest request, StreamObserver<ListRoutesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Route> rows = service.listRoutes(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListRoutesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRoute(GetRouteRequest request, StreamObserver<GetRouteResponse> out) {
        Route found = service.getRoute(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRouteResponse.newBuilder(), "route", found).build());
    }

    @Override
    public void createRoute(CreateRouteRequest request, StreamObserver<CreateRouteResponse> out) {
        Route created = service.createRoute(bridge.body(request.getRoute(), Route.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRouteResponse.newBuilder(), "route", created).build());
    }

    @Override
    public void updateRoute(UpdateRouteRequest request, StreamObserver<UpdateRouteResponse> out) {
        Route updated = service.updateRoute(bridge.id(request.getRoute().getId(), "id"),
                bridge.requiredIfMatch(request.getRoute().getEtag()),
                bridge.changes(request.getRoute(), request.getUpdateMask(), RouteUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRouteResponse.newBuilder(), "route", updated).build());
    }

    @Override
    public void deleteRoute(DeleteRouteRequest request, StreamObserver<DeleteRouteResponse> out) {
        service.deleteRoute(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRouteResponse.getDefaultInstance());
    }

    @Override
    public void listRouteStops(ListRouteStopsRequest request, StreamObserver<ListRouteStopsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RouteStop> rows = service.listRouteStops(bridge.id(request.getRouteId(), "routeId"), page.page(), page.size(),
                page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRouteStopsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRouteStop(GetRouteStopRequest request, StreamObserver<GetRouteStopResponse> out) {
        RouteStop found = service.getRouteStop(bridge.id(request.getRouteId(), "routeId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRouteStopResponse.newBuilder(), "route_stop", found).build());
    }

    @Override
    public void createRouteStop(CreateRouteStopRequest request, StreamObserver<CreateRouteStopResponse> out) {
        RouteStop created = service.createRouteStop(bridge.id(request.getRouteId(), "routeId"),
                bridge.body(request.getRouteStop(), RouteStop.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRouteStopResponse.newBuilder(), "route_stop", created).build());
    }

    @Override
    public void updateRouteStop(UpdateRouteStopRequest request, StreamObserver<UpdateRouteStopResponse> out) {
        RouteStop updated = service.updateRouteStop(bridge.id(request.getRouteId(), "routeId"),
                bridge.id(request.getRouteStop().getId(), "id"), bridge.requiredIfMatch(request.getRouteStop().getEtag()),
                bridge.changes(request.getRouteStop(), request.getUpdateMask(), RouteStopUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRouteStopResponse.newBuilder(), "route_stop", updated).build());
    }

    @Override
    public void deleteRouteStop(DeleteRouteStopRequest request, StreamObserver<DeleteRouteStopResponse> out) {
        service.deleteRouteStop(bridge.id(request.getRouteId(), "routeId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRouteStopResponse.getDefaultInstance());
    }
}
