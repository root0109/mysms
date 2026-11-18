package com.mysms.finance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.FeePlanApi;
import com.mysms.finance.api.model.FeePlan;
import com.mysms.finance.api.model.FeePlanInstallment;
import com.mysms.finance.api.model.FeePlanInstallmentUpdate;
import com.mysms.finance.api.model.FeePlanUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.business.FeePlanService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the FeePlan operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class FeePlanController implements FeePlanApi {

    private final FeePlanService feePlanService;

    public FeePlanController(FeePlanService feePlanService) {
        this.feePlanService = feePlanService;
    }

    @Override
    public ResponseEntity<FeePlan> createFeePlan(FeePlan feePlan, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FeePlan created = feePlanService.createFeePlan(feePlan, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/finance/fee-plans/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<FeePlanInstallment> createFeePlanInstallment(UUID feePlanId,
            FeePlanInstallment feePlanInstallment, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FeePlanInstallment created =
                feePlanService.createFeePlanInstallment(feePlanId, feePlanInstallment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/finance/fee-plans/" + feePlanId + "/installments/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteFeePlan(UUID feePlanId, String ifMatch) {
        feePlanService.deleteFeePlan(feePlanId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteFeePlanInstallment(UUID feePlanId, UUID feePlanInstallmentId,
            String ifMatch) {
        feePlanService.deleteFeePlanInstallment(feePlanId, feePlanInstallmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FeePlan> getFeePlan(UUID feePlanId, String fields) {
        FeePlan found = feePlanService.getFeePlan(feePlanId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<FeePlanInstallment> getFeePlanInstallment(UUID feePlanId,
            UUID feePlanInstallmentId, String fields) {
        FeePlanInstallment found = feePlanService.getFeePlanInstallment(feePlanId, feePlanInstallmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<FeePlanInstallment>> listFeePlanInstallments(UUID feePlanId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<FeePlanInstallment> list =
                PagingUtil.pagingInfo(feePlanService.listFeePlanInstallments(feePlanId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<FeePlan>> listFeePlans(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<FeePlan> list = PagingUtil.pagingInfo(feePlanService.listFeePlans(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<FeePlan> updateFeePlan(UUID feePlanId, String ifMatch,
            FeePlanUpdate feePlanUpdate, String updateMask, Boolean validateOnly) {
        FeePlan updated = feePlanService.updateFeePlan(feePlanId, ifMatch, feePlanUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<FeePlanInstallment> updateFeePlanInstallment(UUID feePlanId,
            UUID feePlanInstallmentId, String ifMatch, FeePlanInstallmentUpdate feePlanInstallmentUpdate,
            String updateMask, Boolean validateOnly) {
        FeePlanInstallment updated =
                feePlanService.updateFeePlanInstallment(feePlanId, feePlanInstallmentId, ifMatch,
                        feePlanInstallmentUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
