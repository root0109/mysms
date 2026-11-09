package com.mysms.master.controller;

import com.mysms.contracts.master.api.CurrencyApi;
import com.mysms.contracts.master.model.Currency;
import com.mysms.contracts.master.model.CurrencyList;
import com.mysms.contracts.master.model.CurrencyUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.master.business.CurrencyService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of master.currency: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/master.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter.
 */
@RestController
public class CurrencyController implements CurrencyApi {

    private static final String PATH = "/v1/master/currencies/";

    private final CurrencyService service;

    public CurrencyController(CurrencyService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<CurrencyList> listCurrencies(Integer pageSize, String pageToken, String filter, String orderBy,
                                              String fields) {
        CurrencyList list = service.listCurrencies(pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Currency> getCurrency(UUID currencyId, String fields) {
        Currency found = service.getCurrency(currencyId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    /** 201 + Location + ETag; with validateOnly nothing is created and the would-be resource comes back with 200. */
    @Override
    public ResponseEntity<Currency> createCurrency(Currency currency, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Currency created = service.createCurrency(currency, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create(PATH + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Currency> updateCurrency(UUID currencyId, String ifMatch, CurrencyUpdate changes,
            String updateMask, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Currency updated = service.updateCurrency(currencyId, ifMatch, changes, updateMask, dryRun);
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Void> deleteCurrency(UUID currencyId, String ifMatch) {
        service.deleteCurrency(currencyId, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
