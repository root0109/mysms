package com.mysms.platform.master.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.master.api.CountryApi;
import com.mysms.platform.master.api.model.Country;
import com.mysms.platform.master.api.model.CountryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.master.business.CountryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of master.country; authentication and the permission check already happened in the auth-client filter. */
@RestController
public class CountryController implements CountryApi {

    private static final String PATH = "/v1/master/countries/";

    private final CountryService service;

    public CountryController(CountryService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<PagingInfo<Country>> listCountries(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Country> list = PagingUtil.pagingInfo(service.listCountries(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Country> getCountry(UUID countryId, String fields) {
        Country found = service.getCountry(countryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    /** 201 + Location + ETag; with validateOnly nothing is created and the would-be resource comes back with 200. */
    @Override
    public ResponseEntity<Country> createCountry(Country country, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Country created = service.createCountry(country, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create(PATH + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Country> updateCountry(UUID countryId, String ifMatch, CountryUpdate changes,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Country updated = service.updateCountry(countryId, ifMatch, changes, dryRun);
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Void> deleteCountry(UUID countryId, String ifMatch) {
        service.deleteCountry(countryId, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
