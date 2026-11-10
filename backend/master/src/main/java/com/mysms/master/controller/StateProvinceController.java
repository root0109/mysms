package com.mysms.master.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.master.api.StateProvinceApi;
import com.mysms.master.api.model.StateProvince;
import com.mysms.master.api.model.StateProvinceUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.master.business.StateProvinceService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of master.state_province: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/master.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter.
 */
@RestController
public class StateProvinceController implements StateProvinceApi {

    private static final String PATH = "/v1/master/states/";

    private final StateProvinceService service;

    public StateProvinceController(StateProvinceService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<PagingInfo<StateProvince>> listStateProvinces(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StateProvince> list =
                PagingUtil.pagingInfo(service.listStateProvinces(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StateProvince> getStateProvince(UUID stateProvinceId, String fields) {
        StateProvince found = service.getStateProvince(stateProvinceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    /** 201 + Location + ETag; with validateOnly nothing is created and the would-be resource comes back with 200. */
    @Override
    public ResponseEntity<StateProvince> createStateProvince(StateProvince stateProvince, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StateProvince created = service.createStateProvince(stateProvince, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create(PATH + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<StateProvince> updateStateProvince(UUID stateProvinceId, String ifMatch, StateProvinceUpdate changes,
            String updateMask, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StateProvince updated = service.updateStateProvince(stateProvinceId, ifMatch, changes, updateMask, dryRun);
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Void> deleteStateProvince(UUID stateProvinceId, String ifMatch) {
        service.deleteStateProvince(stateProvinceId, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
