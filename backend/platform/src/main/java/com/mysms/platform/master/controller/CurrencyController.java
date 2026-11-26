package com.mysms.platform.master.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.master.api.CurrencyApi;
import com.mysms.platform.master.api.model.Currency;
import com.mysms.platform.master.api.model.CurrencyUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.master.business.CurrencyService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of master.currency; authentication and the permission check already happened in the auth-client filter. */
@RestController
public class CurrencyController implements CurrencyApi {

    private static final String PATH = "/v1/master/currencies/";

    private final CurrencyService service;

    public CurrencyController(CurrencyService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<PagingInfo<Currency>> listCurrencies(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Currency> list = PagingUtil.pagingInfo(service.listCurrencies(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
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
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Currency updated = service.updateCurrency(currencyId, ifMatch, changes, dryRun);
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Void> deleteCurrency(UUID currencyId, String ifMatch) {
        service.deleteCurrency(currencyId, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
