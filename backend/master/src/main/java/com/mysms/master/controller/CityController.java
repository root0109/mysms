package com.mysms.master.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.master.api.CityApi;
import com.mysms.master.api.model.City;
import com.mysms.master.api.model.CityUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.master.business.CityService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of master.city: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/master.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter.
 */
@RestController
public class CityController implements CityApi {

    private static final String PATH = "/v1/master/cities/";

    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<PagingInfo<City>> listCities(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<City> list = PagingUtil.pagingInfo(service.listCities(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<City> getCity(UUID cityId, String fields) {
        City found = service.getCity(cityId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    /** 201 + Location + ETag; with validateOnly nothing is created and the would-be resource comes back with 200. */
    @Override
    public ResponseEntity<City> createCity(City city, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        City created = service.createCity(city, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create(PATH + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<City> updateCity(UUID cityId, String ifMatch, CityUpdate changes,
            String updateMask, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        City updated = service.updateCity(cityId, ifMatch, changes, updateMask, dryRun);
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Void> deleteCity(UUID cityId, String ifMatch) {
        service.deleteCity(cityId, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
