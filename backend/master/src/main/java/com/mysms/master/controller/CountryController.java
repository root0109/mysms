package com.mysms.master.controller;

import com.mysms.contracts.master.api.CountryApi;
import com.mysms.contracts.master.model.Country;
import com.mysms.contracts.master.model.CountryList;
import com.mysms.contracts.master.model.CountryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.master.business.CountryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of master.country: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/master.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter.
 */
@RestController
public class CountryController implements CountryApi {

    private static final String PATH = "/v1/master/countries/";

    private final CountryService service;

    public CountryController(CountryService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<CountryList> listCountries(Integer pageSize, String pageToken, String filter, String orderBy,
                                              String fields) {
        CountryList list = service.listCountries(pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
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
            String updateMask, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Country updated = service.updateCountry(countryId, ifMatch, changes, updateMask, dryRun);
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Void> deleteCountry(UUID countryId, String ifMatch) {
        service.deleteCountry(countryId, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
