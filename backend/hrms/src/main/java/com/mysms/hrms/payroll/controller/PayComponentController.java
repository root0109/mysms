package com.mysms.hrms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hrms.payroll.api.PayComponentApi;
import com.mysms.hrms.payroll.api.model.PayComponent;
import com.mysms.hrms.payroll.api.model.PayComponentUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.hrms.payroll.business.PayComponentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PayComponent operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class PayComponentController implements PayComponentApi {

    private final PayComponentService payComponentService;

    public PayComponentController(PayComponentService payComponentService) {
        this.payComponentService = payComponentService;
    }

    @Override
    public ResponseEntity<PayComponent> createPayComponent(PayComponent payComponent, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PayComponent created = payComponentService.createPayComponent(payComponent, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/payroll/pay-components/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deletePayComponent(UUID payComponentId, String ifMatch) {
        payComponentService.deletePayComponent(payComponentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PayComponent> getPayComponent(UUID payComponentId, String fields) {
        PayComponent found = payComponentService.getPayComponent(payComponentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PayComponent>> listPayComponents(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PayComponent> list =
                PagingUtil.pagingInfo(payComponentService.listPayComponents(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PayComponent> updatePayComponent(UUID payComponentId, String ifMatch,
            PayComponentUpdate payComponentUpdate, Boolean validateOnly) {
        PayComponent updated =
                payComponentService.updatePayComponent(payComponentId, ifMatch, payComponentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
