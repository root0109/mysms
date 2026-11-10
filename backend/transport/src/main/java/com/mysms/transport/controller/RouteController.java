package com.mysms.transport.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.transport.api.RouteApi;
import com.mysms.transport.api.model.Route;
import com.mysms.transport.api.model.RouteStop;
import com.mysms.transport.api.model.RouteStopUpdate;
import com.mysms.transport.api.model.RouteUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.transport.business.RouteService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Route operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/transport.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class RouteController implements RouteApi {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @Override
    public ResponseEntity<Route> createRoute(Route route, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Route created = routeService.createRoute(route, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/transport/routes/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<RouteStop> createRouteStop(UUID routeId, RouteStop routeStop,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RouteStop created = routeService.createRouteStop(routeId, routeStop, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/transport/routes/" + routeId + "/stops/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteRoute(UUID routeId, String ifMatch) {
        routeService.deleteRoute(routeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRouteStop(UUID routeId, UUID routeStopId, String ifMatch) {
        routeService.deleteRouteStop(routeId, routeStopId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Route> getRoute(UUID routeId, String fields) {
        Route found = routeService.getRoute(routeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RouteStop> getRouteStop(UUID routeId, UUID routeStopId, String fields) {
        RouteStop found = routeService.getRouteStop(routeId, routeStopId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RouteStop>> listRouteStops(UUID routeId, Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<RouteStop> list =
                PagingUtil.pagingInfo(routeService.listRouteStops(routeId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Route>> listRoutes(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Route> list = PagingUtil.pagingInfo(routeService.listRoutes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Route> updateRoute(UUID routeId, String ifMatch, RouteUpdate routeUpdate,
            String updateMask, Boolean validateOnly) {
        Route updated = routeService.updateRoute(routeId, ifMatch, routeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RouteStop> updateRouteStop(UUID routeId, UUID routeStopId, String ifMatch,
            RouteStopUpdate routeStopUpdate, String updateMask, Boolean validateOnly) {
        RouteStop updated = routeService.updateRouteStop(routeId, routeStopId, ifMatch, routeStopUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
