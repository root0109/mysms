package com.mysms.finance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.FeePlanInstallmentComponentApi;
import com.mysms.finance.api.model.FeePlanInstallmentComponent;
import com.mysms.finance.api.model.FeePlanInstallmentComponentUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.business.FeePlanInstallmentComponentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the FeePlanInstallmentComponent operations; authentication and the permission check already happened in
 * the auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class FeePlanInstallmentComponentController implements FeePlanInstallmentComponentApi {

    private final FeePlanInstallmentComponentService feePlanInstallmentComponentService;

    public FeePlanInstallmentComponentController(
            FeePlanInstallmentComponentService feePlanInstallmentComponentService) {
        this.feePlanInstallmentComponentService = feePlanInstallmentComponentService;
    }

    @Override
    public ResponseEntity<FeePlanInstallmentComponent> createFeePlanInstallmentComponent(
            FeePlanInstallmentComponent feePlanInstallmentComponent, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FeePlanInstallmentComponent created =
                feePlanInstallmentComponentService.createFeePlanInstallmentComponent(feePlanInstallmentComponent,
                        dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/finance/fee-plan-installment-components/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteFeePlanInstallmentComponent(UUID feePlanInstallmentComponentId,
            String ifMatch) {
        feePlanInstallmentComponentService.deleteFeePlanInstallmentComponent(feePlanInstallmentComponentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FeePlanInstallmentComponent> getFeePlanInstallmentComponent(
            UUID feePlanInstallmentComponentId, String fields) {
        FeePlanInstallmentComponent found =
                feePlanInstallmentComponentService.getFeePlanInstallmentComponent(feePlanInstallmentComponentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<FeePlanInstallmentComponent>> listFeePlanInstallmentComponents(Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<FeePlanInstallmentComponent> list =
                PagingUtil.pagingInfo(feePlanInstallmentComponentService.listFeePlanInstallmentComponents(page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<FeePlanInstallmentComponent> updateFeePlanInstallmentComponent(
            UUID feePlanInstallmentComponentId, String ifMatch,
            FeePlanInstallmentComponentUpdate feePlanInstallmentComponentUpdate, Boolean validateOnly) {
        FeePlanInstallmentComponent updated =
                feePlanInstallmentComponentService.updateFeePlanInstallmentComponent(feePlanInstallmentComponentId,
                        ifMatch, feePlanInstallmentComponentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
